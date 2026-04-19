package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.procurement.common.constant.OrderConstants;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.BuyerOrderRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.ProductResponse;
import com.procurement.dto.response.SalesOrderResponse;
import com.procurement.entity.*;
import com.procurement.mapper.*;
import com.procurement.scheduler.RankingAggregateScheduler;
import com.procurement.service.BuyerService;
import com.procurement.service.StockWarningNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import com.procurement.common.util.OrderNoGenerator;
import com.procurement.common.util.OrderAuditHelper;

import java.math.BigDecimal;
import java.util.*;

/**
 * 买家端服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuyerServiceImpl implements BuyerService {

    /** 买家端低库存阈值：stock <= 此值时返回精确数量，否则只返回 IN_STOCK */
    private static final int BUYER_LOW_STOCK_THRESHOLD = 20;

    private final EnterpriseMapper enterpriseMapper;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final StockWarningNotificationService notificationService;
    private final RankingAggregateScheduler rankingAggregateScheduler;

    @Override
    public Map<String, Object> getStoreInfo(Long enterpriseId) {
        SysEnterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Long categoryCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<PmsCategory>()
                        .eq(PmsCategory::getEnterpriseId, enterpriseId));

        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<PmsProduct>()
                        .eq(PmsProduct::getEnterpriseId, enterpriseId)
                        .eq(PmsProduct::getStatus, 1));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enterpriseName", enterprise.getName());
        result.put("logoUrl", enterprise.getLogoUrl());
        result.put("paymentQrUrl", enterprise.getPaymentQrUrl());
        result.put("contactPhone", enterprise.getContactPhone());
        result.put("categoryCount", categoryCount);
        result.put("productCount", productCount);
        return result;
    }

    @Override
    public List<Map<String, Object>> getStoreCategories(Long enterpriseId, String stockStatus,
                                                         BigDecimal priceMin, BigDecimal priceMax) {
        List<PmsCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<PmsCategory>()
                        .eq(PmsCategory::getEnterpriseId, enterpriseId)
                        .orderByAsc(PmsCategory::getSortOrder));

        // 单次 SQL 获取各分类的上架商品数（含过滤条件），构建 categoryId -> count 映射
        List<Map<String, Object>> countRows = productMapper.countByCategoryIdFiltered(
                enterpriseId, stockStatus, priceMin, priceMax);
        Map<Long, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : countRows) {
            Object catId = row.get("category_id");
            Object cnt   = row.get("cnt");
            if (catId != null && cnt != null) {
                countMap.put(((Number) catId).longValue(), ((Number) cnt).longValue());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (PmsCategory cat : categories) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", cat.getId());
            item.put("name", cat.getName());
            item.put("productCount", countMap.getOrDefault(cat.getId(), 0L));
            result.add(item);
        }
        return result;
    }

    @Override
    public PageResponse<ProductResponse> getStoreProducts(Long enterpriseId, Long categoryId,
                                                           String keyword,
                                                           String stockStatus, String sortBy,
                                                           BigDecimal priceMin, BigDecimal priceMax,
                                                           Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<PmsProduct> wrapper = new LambdaQueryWrapper<PmsProduct>()
                .eq(PmsProduct::getEnterpriseId, enterpriseId)
                .eq(PmsProduct::getStatus, 1);        // 仅上架商品

        if (categoryId != null) {
            wrapper.eq(PmsProduct::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(PmsProduct::getName, keyword)
                    .or().like(PmsProduct::getSpec, keyword));
        }
        // 库存状态筛选（IN_STOCK: stock>0, OUT_OF_STOCK: stock<=0）
        if ("IN_STOCK".equals(stockStatus)) {
            wrapper.gt(PmsProduct::getStock, 0);
        } else if ("OUT_OF_STOCK".equals(stockStatus)) {
            wrapper.le(PmsProduct::getStock, 0);
        }
        // 价格区间（priceMin/priceMax 均为可选，支持单边）
        if (priceMin != null && priceMin.compareTo(BigDecimal.ZERO) >= 0) {
            wrapper.ge(PmsProduct::getPrice, priceMin);
        }
        if (priceMax != null && priceMax.compareTo(BigDecimal.ZERO) > 0) {
            wrapper.le(PmsProduct::getPrice, priceMax);
        }
        // 排序：价格升/降序；默认按 ID 倒序（新品靠前）
        if ("price_asc".equals(sortBy)) {
            wrapper.orderByAsc(PmsProduct::getPrice);
        } else if ("price_desc".equals(sortBy)) {
            wrapper.orderByDesc(PmsProduct::getPrice);
        } else {
            wrapper.orderByDesc(PmsProduct::getId);
        }

        Page<PmsProduct> page = productMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        // 转换时不包含 costPrice
        List<ProductResponse> records = page.getRecords().stream().map(p -> {
            ProductResponse resp = new ProductResponse();
            resp.setId(p.getId());
            resp.setName(p.getName());
            resp.setSpec(p.getSpec());
            resp.setUnit(p.getUnit());
            resp.setPrice(p.getPrice());
            // 库存策略：<=0 缺货, <=阈值 展示精确数量, >阈值 只显示"有货"
            int stock = p.getStock() != null ? p.getStock() : 0;
            if (stock <= 0) {
                resp.setStockStatus("OUT_OF_STOCK");
                resp.setStock(0);
            } else if (stock <= BUYER_LOW_STOCK_THRESHOLD) {
                resp.setStockStatus("LOW_STOCK");
                resp.setStock(stock);
            } else {
                resp.setStockStatus("IN_STOCK");
                resp.setStock(null); // 不暴露精确数量
            }
            resp.setImages(p.getImages());
            // 主图：images 列表第一张，供列表卡片展示
            resp.setMainImage(p.getImages() != null && !p.getImages().isEmpty() ? p.getImages().get(0) : null);
            resp.setCategoryId(p.getCategoryId());
            resp.setStatus(p.getStatus());
            return resp;
        }).toList();

        return PageResponse.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public Map<String, Object> getProductDetail(Long productId) {
        PmsProduct product = productMapper.selectById(productId);
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        SysEnterprise enterprise = enterpriseMapper.selectById(product.getEnterpriseId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", product.getId());
        result.put("name", product.getName());
        result.put("spec", product.getSpec());
        result.put("unit", product.getUnit());
        result.put("price", product.getPrice());
        int detailStock = product.getStock() != null ? product.getStock() : 0;
        if (detailStock <= 0) {
            result.put("stockStatus", "OUT_OF_STOCK");
            result.put("stock", 0);
        } else if (detailStock <= BUYER_LOW_STOCK_THRESHOLD) {
            result.put("stockStatus", "LOW_STOCK");
            result.put("stock", detailStock);
        } else {
            result.put("stockStatus", "IN_STOCK");
        }
        result.put("images", product.getImages());
        result.put("mainImage", product.getImages() != null && !product.getImages().isEmpty() ? product.getImages().get(0) : null);
        result.put("enterpriseName", enterprise != null ? enterprise.getName() : "");
        result.put("qrcodeImage", product.getQrcodeImage());
        result.put("description", product.getDescription() != null ? product.getDescription() : "");
        return result;
    }

    @Override
    @Transactional
    public SalesOrderResponse createOrder(Long buyerUserId, BuyerOrderRequest request) {
        Long enterpriseId = request.getEnterpriseId();

        // 验证企业存在
        SysEnterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 查找或创建关联买家的客户记录
        SysUser buyer = userMapper.selectById(buyerUserId);
        CrmCustomer customer = null;
        if (buyer != null && buyer.getWxOpenid() != null) {
            customer = customerMapper.selectOne(
                    new LambdaQueryWrapper<CrmCustomer>()
                            .eq(CrmCustomer::getEnterpriseId, enterpriseId)
                            .eq(CrmCustomer::getWxOpenid, buyer.getWxOpenid()));
            if (customer == null) {
                customer = new CrmCustomer();
                customer.setEnterpriseId(enterpriseId);
                customer.setName(buyer.getNickName() != null ? buyer.getNickName() : "微信买家");
                customer.setWxOpenid(buyer.getWxOpenid());
                customer.setAddress(request.getAddress());
                customerMapper.insert(customer);
            }
        }

        // 生成订单号（Redis 优先，DB 降级）
        String orderNo = orderNoGenerator.generate(OrderConstants.SALES_ORDER_PREFIX, enterpriseId, "oms_sales_order");

        // 创建订单
        OmsSalesOrder order = new OmsSalesOrder();
        order.setOrderNo(orderNo);
        order.setEnterpriseId(enterpriseId);
        order.setCustomerId(customer != null ? customer.getId() : null);
        order.setStatus(OrderConstants.SALES_PENDING);
        order.setPaymentStatus(OrderConstants.PAY_UNPAID);
        order.setDeliveryAddress(request.getAddress());
        order.setRemark(request.getRemark());
        order.setOrderSource(OrderConstants.ORDER_SOURCE_BUYER);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        Map<Long, Integer> prevStocks = new HashMap<>();
        List<OmsSalesOrderItem> items = new ArrayList<>();
        for (BuyerOrderRequest.BuyerItemRequest itemReq : request.getItems()) {
            PmsProduct product = productMapper.selectById(itemReq.getProductId());
            if (product == null || !product.getEnterpriseId().equals(enterpriseId)
                    || product.getStatus() != 1) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        "商品不存在或已下架: ID=" + itemReq.getProductId());
            }
            prevStocks.put(product.getId(), product.getStock());
            // 原子扣减库存（乐观锁 — adjustStock 使用 WHERE stock >= quantity 保证并发安全）
            int affected = productMapper.adjustStock(itemReq.getProductId(), -itemReq.getQuantity());
            if (affected == 0) {
                throw new BusinessException(ResultCode.STOCK_INSUFFICIENT.getCode(),
                        "商品 [" + product.getName() + "] 库存不足");
            }

            OmsSalesOrderItem item = new OmsSalesOrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setSpec(product.getSpec());
            item.setUnit(product.getUnit());
            item.setQuantity(itemReq.getQuantity());
            item.setPrice(product.getPrice());
            BigDecimal costPrice = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            item.setCostPrice(costPrice);

            BigDecimal amount = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            BigDecimal profit = product.getPrice().subtract(costPrice)
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            item.setAmount(amount);
            item.setProfit(profit);

            totalAmount = totalAmount.add(amount);
            totalCost = totalCost.add(costPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity())));
            totalProfit = totalProfit.add(profit);

            items.add(item);
        }

        order.setTotalAmount(totalAmount);
        order.setTotalCost(totalCost);
        order.setTotalProfit(totalProfit);
        salesOrderMapper.insert(order);

        for (OmsSalesOrderItem item : items) {
            item.setOrderId(order.getId());
            salesOrderItemMapper.insert(item);
        }

        // 买家下单后异步检查库存预警（事务提交后触发，避免读到旧数据）
        if (!prevStocks.isEmpty()) {
            final Long eid = enterpriseId;
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationService.checkAndNotify(prevStocks, eid);
                    }
                });
            } else {
                notificationService.checkAndNotify(prevStocks, enterpriseId);
            }
        }

        return toOrderResponse(order, customer);
    }

    @Override
    @Transactional
    public void payOrder(Long buyerUserId, Long orderId) {
        OmsSalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 校验是该买家的订单
        validateBuyerOrder(buyerUserId, order);
        ensureBuyerPaymentOperable(order);

        if (!OrderConstants.PAY_UNPAID.equals(order.getPaymentStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR.getCode(), "当前订单无需重复提交付款声明");
        }

        String fromPaymentStatus = order.getPaymentStatus();
        order.setPaymentStatus(OrderConstants.PAY_CLAIMED);
        order.setRemark(OrderAuditHelper.appendSystemAuditRemark(order.getRemark(), buyerUserId, "BUYER",
                "LEGACY_PAY_COMPAT", fromPaymentStatus, OrderConstants.PAY_CLAIMED,
                "旧版 pay 接口兼容降级"));
        salesOrderMapper.updateById(order);
        log.warn("order_audit event=LEGACY_PAY_COMPAT enterpriseId={} orderId={} orderNo={} operatorId={} operatorRole=BUYER fromPaymentStatus={} toPaymentStatus={} fromOrderStatus={} toOrderStatus={} message={}",
                order.getEnterpriseId(), order.getId(), order.getOrderNo(), buyerUserId,
                fromPaymentStatus, order.getPaymentStatus(), order.getStatus(), order.getStatus(),
                "旧版 pay 接口兼容降级为付款声明");
    }

    @Override
    @Transactional
    public void claimPaid(Long buyerUserId, Long orderId) {
        OmsSalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        validateBuyerOrder(buyerUserId, order);
        ensureBuyerPaymentOperable(order);

        // 只允许 UNPAID 状态声明已付款
        if (!OrderConstants.PAY_UNPAID.equals(order.getPaymentStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR.getCode(), "仅未付款状态可声明已付款");
        }

        String fromPaymentStatus = order.getPaymentStatus();
        order.setPaymentStatus(OrderConstants.PAY_CLAIMED);
        order.setRemark(OrderAuditHelper.appendSystemAuditRemark(order.getRemark(), buyerUserId, "BUYER",
                "CLAIM_PAID", fromPaymentStatus, OrderConstants.PAY_CLAIMED,
                "买家提交线下付款声明"));
        salesOrderMapper.updateById(order);
        log.info("order_audit event=CLAIM_PAID enterpriseId={} orderId={} orderNo={} operatorId={} operatorRole=BUYER fromPaymentStatus={} toPaymentStatus={} fromOrderStatus={} toOrderStatus={} message={}",
                order.getEnterpriseId(), order.getId(), order.getOrderNo(), buyerUserId,
                fromPaymentStatus, order.getPaymentStatus(), order.getStatus(), order.getStatus(),
                "买家提交线下付款声明");
    }

    @Override
    public PageResponse<SalesOrderResponse> listOrders(Long buyerUserId, Integer pageNum,
                                                        Integer pageSize, String status) {
        // 找到该买家关联的所有客户 ID
        SysUser buyer = userMapper.selectById(buyerUserId);
        if (buyer == null || buyer.getWxOpenid() == null) {
            return PageResponse.of(Collections.emptyList(), 0L, pageNum, pageSize);
        }

        List<CrmCustomer> customers = customerMapper.selectList(
                new LambdaQueryWrapper<CrmCustomer>()
                        .eq(CrmCustomer::getWxOpenid, buyer.getWxOpenid()));

        if (customers.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), 0L, pageNum, pageSize);
        }

        List<Long> customerIds = customers.stream().map(CrmCustomer::getId).toList();

        LambdaQueryWrapper<OmsSalesOrder> wrapper = new LambdaQueryWrapper<OmsSalesOrder>()
                .in(OmsSalesOrder::getCustomerId, customerIds);

        if (StringUtils.hasText(status)) {
            wrapper.eq(OmsSalesOrder::getStatus, status);
        }
        wrapper.orderByDesc(OmsSalesOrder::getId);

        Page<OmsSalesOrder> page = salesOrderMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        List<OmsSalesOrder> orders = page.getRecords();
        if (orders.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), page.getTotal(), pageNum, pageSize);
        }

        // 批量预加载订单明细（消除 N+1）
        List<Long> orderIds = orders.stream().map(OmsSalesOrder::getId).toList();
        Map<Long, List<OmsSalesOrderItem>> itemsByOrderId = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrderItem>()
                        .in(OmsSalesOrderItem::getOrderId, orderIds))
                .stream().collect(java.util.stream.Collectors.groupingBy(OmsSalesOrderItem::getOrderId));

        // 批量预加载客户信息
        Set<Long> custIds = orders.stream()
                .map(OmsSalesOrder::getCustomerId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, CrmCustomer> customerMap = custIds.isEmpty()
                ? Collections.emptyMap()
                : customerMapper.selectBatchIds(custIds).stream()
                        .collect(java.util.stream.Collectors.toMap(CrmCustomer::getId, c -> c));

        List<SalesOrderResponse> records = orders.stream()
                .map(o -> toBuyerOrderResponse(o, customerMap, itemsByOrderId)).toList();

        return PageResponse.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public SalesOrderResponse getOrderDetail(Long buyerUserId, Long orderId) {
        OmsSalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateBuyerOrder(buyerUserId, order);
        return toOrderResponse(order, null);
    }

    @Override
    @Transactional
    public void cancelOrder(Long buyerUserId, Long orderId) {
        OmsSalesOrder order = salesOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateBuyerOrder(buyerUserId, order);

        // 买家只允许取消 PENDING + UNPAID 订单
        if (!OrderConstants.SALES_PENDING.equals(order.getStatus())
                || !OrderConstants.PAY_UNPAID.equals(order.getPaymentStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR.getCode(), "仅待确认且未付款的订单可取消");
        }

        // 恢复库存
        List<OmsSalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrderItem>()
                        .eq(OmsSalesOrderItem::getOrderId, orderId));
        List<Long> restoredIds = new ArrayList<>();
        for (OmsSalesOrderItem item : items) {
            productMapper.adjustStock(item.getProductId(), item.getQuantity());
            restoredIds.add(item.getProductId());
        }

        // 库存恢复后清除预警去重 key，确保下次再跌破阈值时能重新通知
        if (!restoredIds.isEmpty()) {
            final Long eid = order.getEnterpriseId();
            final List<Long> pids = restoredIds;
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationService.clearDedupOnRestock(pids, eid);
                    }
                });
            } else {
                notificationService.clearDedupOnRestock(pids, eid);
            }
        }

        String previousOrderStatus = order.getStatus();
        String previousPaymentStatus = order.getPaymentStatus();
        order.setCancelBy(OrderConstants.CANCEL_BY_BUYER);
        order.setStatus(OrderConstants.SALES_CANCELLED);
        order.setRemark(OrderAuditHelper.appendSystemAuditRemark(order.getRemark(), buyerUserId, "BUYER",
                "CANCEL_ORDER", previousPaymentStatus, previousPaymentStatus,
                "买家取消订单"));
        salesOrderMapper.updateById(order);
        log.info("order_audit event=CANCEL_ORDER enterpriseId={} orderId={} orderNo={} operatorId={} operatorRole=BUYER fromPaymentStatus={} toPaymentStatus={} fromOrderStatus={} toOrderStatus={} message={}",
                order.getEnterpriseId(), order.getId(), order.getOrderNo(), buyerUserId,
                previousPaymentStatus, previousPaymentStatus, previousOrderStatus, order.getStatus(),
                "买家取消订单");

        // 取消后触发排行摘要表重新聚合（事务提交后异步执行）
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rankingAggregateScheduler.aggregateToday();
            }
        });
    }

    // ===================== 私有方法 =====================

    /**
     * 校验该订单是否属于该买家
     */
    private void validateBuyerOrder(Long buyerUserId, OmsSalesOrder order) {
        if (order.getCustomerId() == null) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        SysUser buyer = userMapper.selectById(buyerUserId);
        if (buyer == null || buyer.getWxOpenid() == null) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        CrmCustomer customer = customerMapper.selectById(order.getCustomerId());
        if (customer == null || !buyer.getWxOpenid().equals(customer.getWxOpenid())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private void ensureBuyerPaymentOperable(OmsSalesOrder order) {
        if (OrderConstants.SALES_CANCELLED.equals(order.getStatus())
                || OrderConstants.SALES_COMPLETED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR.getCode(), "当前订单状态不允许付款操作");
        }
    }

    private SalesOrderResponse toOrderResponse(OmsSalesOrder order, CrmCustomer customer) {
        SalesOrderResponse resp = new SalesOrderResponse();
        resp.setId(order.getId());
        resp.setEnterpriseId(order.getEnterpriseId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setPaymentStatus(order.getPaymentStatus());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setDeliveryAddress(order.getDeliveryAddress());
        resp.setRemark(order.getRemark());
        resp.setOrderSource(order.getOrderSource());
        resp.setCancelBy(order.getCancelBy());
        resp.setCreatedAt(order.getCreatedAt());
        // 买家端不返回利润信息
        resp.setTotalCost(null);
        resp.setTotalProfit(null);

        // 客户信息
        if (customer == null && order.getCustomerId() != null) {
            customer = customerMapper.selectById(order.getCustomerId());
        }
        if (customer != null) {
            SalesOrderResponse.CustomerInfo ci = new SalesOrderResponse.CustomerInfo();
            ci.setId(customer.getId());
            ci.setName(customer.getName());
            ci.setPhone(customer.getPhone());
            resp.setCustomer(ci);
        }

        // 订单明细
        List<OmsSalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrderItem>()
                        .eq(OmsSalesOrderItem::getOrderId, order.getId()));

        List<SalesOrderResponse.OrderItemInfo> itemInfos = items.stream().map(item -> {
            SalesOrderResponse.OrderItemInfo info = new SalesOrderResponse.OrderItemInfo();
            info.setProductId(item.getProductId());
            info.setProductName(item.getProductName());
            info.setSpec(item.getSpec());
            info.setUnit(item.getUnit());
            info.setQuantity(item.getQuantity());
            info.setPrice(item.getPrice());
            // 买家端不返回成本价和利润
            info.setAmount(item.getAmount());
            return info;
        }).toList();

        resp.setItems(itemInfos);
        return resp;
    }

    /** 批量预加载版本（消除 N+1） */
    private SalesOrderResponse toBuyerOrderResponse(OmsSalesOrder order,
                                                     Map<Long, CrmCustomer> customerMap,
                                                     Map<Long, List<OmsSalesOrderItem>> itemsByOrderId) {
        SalesOrderResponse resp = new SalesOrderResponse();
        resp.setId(order.getId());
        resp.setEnterpriseId(order.getEnterpriseId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setPaymentStatus(order.getPaymentStatus());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setDeliveryAddress(order.getDeliveryAddress());
        resp.setRemark(order.getRemark());
        resp.setOrderSource(order.getOrderSource());
        resp.setCancelBy(order.getCancelBy());
        resp.setCreatedAt(order.getCreatedAt());
        resp.setTotalCost(null);
        resp.setTotalProfit(null);

        if (order.getCustomerId() != null) {
            CrmCustomer customer = customerMap.get(order.getCustomerId());
            if (customer != null) {
                SalesOrderResponse.CustomerInfo ci = new SalesOrderResponse.CustomerInfo();
                ci.setId(customer.getId());
                ci.setName(customer.getName());
                ci.setPhone(customer.getPhone());
                resp.setCustomer(ci);
            }
        }

        List<OmsSalesOrderItem> items = itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList());
        List<SalesOrderResponse.OrderItemInfo> itemInfos = items.stream().map(item -> {
            SalesOrderResponse.OrderItemInfo info = new SalesOrderResponse.OrderItemInfo();
            info.setProductId(item.getProductId());
            info.setProductName(item.getProductName());
            info.setSpec(item.getSpec());
            info.setUnit(item.getUnit());
            info.setQuantity(item.getQuantity());
            info.setPrice(item.getPrice());
            info.setAmount(item.getAmount());
            return info;
        }).toList();

        resp.setItems(itemInfos);
        return resp;
    }
}
