package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.procurement.common.constant.OrderConstants;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.SalesOrderRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.SalesOrderResponse;
import com.procurement.entity.*;
import com.procurement.mapper.*;
import com.procurement.scheduler.RankingAggregateScheduler;
import com.procurement.service.SalesOrderService;
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
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 销售订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesOrderServiceImpl implements SalesOrderService {


    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final ProductMapper productMapper;
    private final CustomerMapper customerMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final StockWarningNotificationService notificationService;
    private final RankingAggregateScheduler rankingAggregateScheduler;

    @Override
    public PageResponse<SalesOrderResponse> listByPage(Long enterpriseId, Integer pageNum, Integer pageSize,
                                                        String status, String paymentStatus,
                                                        Long customerId, String keyword,
                                                        String startDate, String endDate,
                                                        BigDecimal minAmount, BigDecimal maxAmount, String sortBy) {
        LambdaQueryWrapper<OmsSalesOrder> wrapper = new LambdaQueryWrapper<OmsSalesOrder>()
                .eq(OmsSalesOrder::getEnterpriseId, enterpriseId);

        if (StringUtils.hasText(status)) {
            wrapper.eq(OmsSalesOrder::getStatus, status);
        }
        if (StringUtils.hasText(paymentStatus)) {
            wrapper.eq(OmsSalesOrder::getPaymentStatus, paymentStatus);
        }
        if (customerId != null) {
            wrapper.eq(OmsSalesOrder::getCustomerId, customerId);
        }

        // 关键词搜索：订单号 LIKE 或 客户名称匹配
        if (StringUtils.hasText(keyword)) {
            List<Long> matchingCustomerIds = customerMapper.selectList(
                    new LambdaQueryWrapper<CrmCustomer>()
                            .eq(CrmCustomer::getEnterpriseId, enterpriseId)
                            .like(CrmCustomer::getName, keyword))
                    .stream().map(CrmCustomer::getId).toList();
            wrapper.and(w -> {
                w.like(OmsSalesOrder::getOrderNo, keyword);
                if (!matchingCustomerIds.isEmpty()) {
                    w.or().in(OmsSalesOrder::getCustomerId, matchingCustomerIds);
                }
            });
        }

        // 时间范围
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(OmsSalesOrder::getCreatedAt, LocalDate.parse(startDate).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(OmsSalesOrder::getCreatedAt, LocalDate.parse(endDate).atTime(23, 59, 59));
        }

        // 金额范围
        if (minAmount != null) {
            wrapper.ge(OmsSalesOrder::getTotalAmount, minAmount);
        }
        if (maxAmount != null) {
            wrapper.le(OmsSalesOrder::getTotalAmount, maxAmount);
        }

        // 排序
        if (StringUtils.hasText(sortBy)) {
            switch (sortBy) {
                case "amount_asc" -> wrapper.orderByAsc(OmsSalesOrder::getTotalAmount);
                case "amount_desc" -> wrapper.orderByDesc(OmsSalesOrder::getTotalAmount);
                case "time_asc" -> wrapper.orderByAsc(OmsSalesOrder::getCreatedAt);
                default -> wrapper.orderByDesc(OmsSalesOrder::getId);
            }
        } else {
            wrapper.orderByDesc(OmsSalesOrder::getId);
        }

        Page<OmsSalesOrder> page = salesOrderMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        List<OmsSalesOrder> orders = page.getRecords();
        if (orders.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), page.getTotal(), pageNum, pageSize);
        }

        // 批量加载客户信息
        Set<Long> customerIds = orders.stream()
                .map(OmsSalesOrder::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CrmCustomer> customerMap = customerIds.isEmpty() ? Collections.emptyMap()
                : customerMapper.selectBatchIds(customerIds).stream()
                        .collect(Collectors.toMap(CrmCustomer::getId, Function.identity()));

        // 批量加载订单明细
        List<Long> orderIds = orders.stream().map(OmsSalesOrder::getId).toList();
        List<OmsSalesOrderItem> allItems = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrderItem>()
                        .in(OmsSalesOrderItem::getOrderId, orderIds));
        Map<Long, List<OmsSalesOrderItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(OmsSalesOrderItem::getOrderId));

        List<SalesOrderResponse> records = orders.stream()
                .map(o -> toResponse(o, customerMap, itemsByOrderId)).toList();

        return PageResponse.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public SalesOrderResponse getById(Long enterpriseId, Long id) {
        OmsSalesOrder order = salesOrderMapper.selectById(id);
        if (order == null || !order.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toResponse(order);
    }

    @Override
    @Transactional
    public SalesOrderResponse create(Long enterpriseId, SalesOrderRequest request) {
        // 生成订单号: SO + 日期 + 4位序号（Redis 优先，DB 降级）
        String orderNo = orderNoGenerator.generate(OrderConstants.SALES_ORDER_PREFIX, enterpriseId, "oms_sales_order");

        // 创建主订单
        OmsSalesOrder order = new OmsSalesOrder();
        order.setOrderNo(orderNo);
        order.setEnterpriseId(enterpriseId);
        order.setCustomerId(request.getCustomerId());
        order.setStatus(OrderConstants.SALES_PENDING);
        order.setPaymentStatus(OrderConstants.PAY_UNPAID);
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setRemark(request.getRemark());
        order.setOrderSource(OrderConstants.ORDER_SOURCE_MERCHANT);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        // 合并相同商品的数量（防止重复 productId 创建多条 item）
        Map<Long, Integer> mergedItems = new LinkedHashMap<>();
        for (SalesOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            mergedItems.merge(itemReq.getProductId(), itemReq.getQuantity(), Integer::sum);
        }

        // 创建订单明细（快照商品信息）
        List<OmsSalesOrderItem> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : mergedItems.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            PmsProduct product = productMapper.selectById(productId);
            if (product == null || !product.getEnterpriseId().equals(enterpriseId)) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        "商品不存在: ID=" + productId);
            }
            // 检查商品上架状态
            if (product.getStatus() != null && product.getStatus() != 1) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                        "商品 [" + product.getName() + "] 已下架，无法开单");
            }

            // 防护 price 为 null
            BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;

            OmsSalesOrderItem item = new OmsSalesOrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setSpec(product.getSpec());
            item.setUnit(product.getUnit());
            item.setQuantity(quantity);
            item.setPrice(price);
            BigDecimal costPrice = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            item.setCostPrice(costPrice);

            BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));
            BigDecimal profit = price.subtract(costPrice)
                    .multiply(BigDecimal.valueOf(quantity));

            item.setAmount(amount);
            item.setProfit(profit);

            totalAmount = totalAmount.add(amount);
            totalCost = totalCost.add(costPrice.multiply(BigDecimal.valueOf(quantity)));
            totalProfit = totalProfit.add(profit);

            items.add(item);
        }

        order.setTotalAmount(totalAmount);
        order.setTotalCost(totalCost);
        order.setTotalProfit(totalProfit);
        salesOrderMapper.insert(order);

        // 插入明细
        for (OmsSalesOrderItem item : items) {
            item.setOrderId(order.getId());
            salesOrderItemMapper.insert(item);
        }

        // 扣减库存（商家开单与买家下单统一在创建时扣减，防止超卖）
        Map<Long, Integer> prevStocks = new HashMap<>();
        for (OmsSalesOrderItem item : items) {
            PmsProduct p = productMapper.selectById(item.getProductId());
            if (p != null) prevStocks.put(p.getId(), p.getStock());
            int rows = productMapper.adjustStock(item.getProductId(), -item.getQuantity());
            if (rows == 0) {
                PmsProduct product = productMapper.selectById(item.getProductId());
                String name = product != null ? product.getName() : "ID=" + item.getProductId();
                throw new BusinessException(ResultCode.STOCK_INSUFFICIENT.getCode(),
                        "商品 [" + name + "] 库存不足");
            }
        }

        // 异步检查库存预警
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

        return toResponse(order);
    }

    @Override
    @Transactional
    public void confirm(Long enterpriseId, Long id) {
        OmsSalesOrder order = getAndValidate(enterpriseId, id);
        if (!OrderConstants.SALES_PENDING.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(OrderConstants.SALES_CONFIRMED);
        salesOrderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void ship(Long enterpriseId, Long id) {
        OmsSalesOrder order = getAndValidate(enterpriseId, id);
        if (!OrderConstants.SALES_CONFIRMED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        if (!OrderConstants.PAY_PAID.equals(order.getPaymentStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR.getCode(), "未确认收款的订单不可发货");
        }
        order.setStatus(OrderConstants.SALES_SHIPPED);
        salesOrderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void complete(Long enterpriseId, Long id) {
        OmsSalesOrder order = getAndValidate(enterpriseId, id);
        if (!OrderConstants.SALES_SHIPPED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        if (!OrderConstants.PAY_PAID.equals(order.getPaymentStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR.getCode(), "未确认收款的订单不可完成");
        }
        order.setStatus(OrderConstants.SALES_COMPLETED);
        salesOrderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void cancel(Long enterpriseId, Long id, Long operatorUserId, String callerMemberRole) {
        OmsSalesOrder order = getAndValidate(enterpriseId, id);

        // 销售员只允许取消未发货订单
        String status = order.getStatus();
        if ("SALES".equals(callerMemberRole)
                && !(OrderConstants.SALES_PENDING.equals(status) || OrderConstants.SALES_CONFIRMED.equals(status))) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (OrderConstants.SALES_SHIPPED.equals(status)
                || OrderConstants.SALES_COMPLETED.equals(status)
                || OrderConstants.SALES_CANCELLED.equals(status)) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 根据订单来源决定是否恢复库存
        // 取消时无条件恢复库存（创建时已统一扣减）
        List<Long> restoredProductIds = new ArrayList<>();
        List<OmsSalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrderItem>()
                        .eq(OmsSalesOrderItem::getOrderId, id));
        for (OmsSalesOrderItem item : items) {
            productMapper.adjustStock(item.getProductId(), item.getQuantity());
            restoredProductIds.add(item.getProductId());
        }

        // 库存恢复后清除预警去重 key，确保下次再跌破阈值时能重新通知
        if (!restoredProductIds.isEmpty()) {
            final Long eid = enterpriseId;
            final List<Long> pids = restoredProductIds;
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationService.clearDedupOnRestock(pids, eid);
                    }
                });
            } else {
                notificationService.clearDedupOnRestock(pids, enterpriseId);
            }
        }

        String previousOrderStatus = order.getStatus();
        String previousPaymentStatus = order.getPaymentStatus();
        order.setCancelBy("SALES".equals(callerMemberRole)
                ? OrderConstants.CANCEL_BY_SALES
                : OrderConstants.CANCEL_BY_MERCHANT);
        order.setStatus(OrderConstants.SALES_CANCELLED);
        order.setRemark(OrderAuditHelper.appendSystemAuditRemark(order.getRemark(), operatorUserId, resolveOperatorRole(callerMemberRole),
                "CANCEL_ORDER", previousPaymentStatus, previousPaymentStatus,
                previousPaymentStatus != null && !OrderConstants.PAY_UNPAID.equals(previousPaymentStatus)
                        ? "取消订单（已提醒线下退款）"
                        : "取消订单"));
        salesOrderMapper.updateById(order);
        log.info("order_audit event=CANCEL_ORDER enterpriseId={} orderId={} orderNo={} operatorId={} operatorRole={} fromPaymentStatus={} toPaymentStatus={} fromOrderStatus={} toOrderStatus={} message={}",
                order.getEnterpriseId(), order.getId(), order.getOrderNo(), operatorUserId, resolveOperatorRole(callerMemberRole),
                previousPaymentStatus, previousPaymentStatus, previousOrderStatus, order.getStatus(),
                previousPaymentStatus != null && !OrderConstants.PAY_UNPAID.equals(previousPaymentStatus)
                        ? "取消订单（已提醒线下退款）"
                        : "取消订单");

        // 取消后触发排行摘要表重新聚合（事务提交后异步执行）
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rankingAggregateScheduler.aggregateToday();
            }
        });
    }

    @Override
    @Transactional
    public void confirmPayment(Long enterpriseId, Long id, Long operatorUserId, String callerMemberRole) {
        OmsSalesOrder order = getAndValidate(enterpriseId, id);
        // 已取消或已完成的订单不允许收款
        if (OrderConstants.SALES_CANCELLED.equals(order.getStatus())
                || OrderConstants.SALES_COMPLETED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        if (OrderConstants.PAY_PAID.equals(order.getPaymentStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 库存已在 create() 统一扣减，收款仅更新支付状态
        String fromPaymentStatus = order.getPaymentStatus();
        order.setPaymentStatus(OrderConstants.PAY_PAID);
        order.setRemark(OrderAuditHelper.appendSystemAuditRemark(order.getRemark(), operatorUserId, resolveOperatorRole(callerMemberRole),
                "CONFIRM_PAYMENT", fromPaymentStatus, OrderConstants.PAY_PAID, "商家侧确认收款"));
        salesOrderMapper.updateById(order);
        log.info("order_audit event=CONFIRM_PAYMENT enterpriseId={} orderId={} orderNo={} operatorId={} operatorRole={} fromPaymentStatus={} toPaymentStatus={} fromOrderStatus={} toOrderStatus={} message={}",
                order.getEnterpriseId(), order.getId(), order.getOrderNo(), operatorUserId, resolveOperatorRole(callerMemberRole),
                fromPaymentStatus, order.getPaymentStatus(), order.getStatus(), order.getStatus(), "商家侧确认收款");
    }

    @Override
    public Map<String, Long> countByStatus(Long enterpriseId, String keyword, String startDate, String endDate,
                                            BigDecimal minAmount, BigDecimal maxAmount,
                                            Long customerId, String paymentStatus) {
        // 预查询关键词匹配的客户 ID
        List<Long> matchingCustomerIds = null;
        if (StringUtils.hasText(keyword)) {
            matchingCustomerIds = customerMapper.selectList(
                    new LambdaQueryWrapper<CrmCustomer>()
                            .eq(CrmCustomer::getEnterpriseId, enterpriseId)
                            .like(CrmCustomer::getName, keyword))
                    .stream().map(CrmCustomer::getId).toList();
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        for (String s : List.of("ALL", "PENDING", "CONFIRMED", "SHIPPED", "COMPLETED")) {
            LambdaQueryWrapper<OmsSalesOrder> w = new LambdaQueryWrapper<OmsSalesOrder>()
                    .eq(OmsSalesOrder::getEnterpriseId, enterpriseId);
            if (!"ALL".equals(s)) {
                w.eq(OmsSalesOrder::getStatus, s);
            }
            if (customerId != null) {
                w.eq(OmsSalesOrder::getCustomerId, customerId);
            }
            if (StringUtils.hasText(paymentStatus)) {
                w.eq(OmsSalesOrder::getPaymentStatus, paymentStatus);
            }
            if (StringUtils.hasText(keyword)) {
                final List<Long> cIds = matchingCustomerIds;
                w.and(ww -> {
                    ww.like(OmsSalesOrder::getOrderNo, keyword);
                    if (cIds != null && !cIds.isEmpty()) {
                        ww.or().in(OmsSalesOrder::getCustomerId, cIds);
                    }
                });
            }
            if (StringUtils.hasText(startDate)) {
                w.ge(OmsSalesOrder::getCreatedAt, LocalDate.parse(startDate).atStartOfDay());
            }
            if (StringUtils.hasText(endDate)) {
                w.le(OmsSalesOrder::getCreatedAt, LocalDate.parse(endDate).atTime(23, 59, 59));
            }
            if (minAmount != null) {
                w.ge(OmsSalesOrder::getTotalAmount, minAmount);
            }
            if (maxAmount != null) {
                w.le(OmsSalesOrder::getTotalAmount, maxAmount);
            }
            counts.put(s, salesOrderMapper.selectCount(w));
        }
        return counts;
    }

    // ===================== 私有方法 =====================

    private OmsSalesOrder getAndValidate(Long enterpriseId, Long id) {
        OmsSalesOrder order = salesOrderMapper.selectById(id);
        if (order == null || !order.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return order;
    }

    private String resolveOperatorRole(String callerMemberRole) {
        return StringUtils.hasText(callerMemberRole) ? callerMemberRole : "SELLER";
    }



    /**
     * Entity → Response DTO（含客户信息和订单明细）— 单条查询版本
     */
    private SalesOrderResponse toResponse(OmsSalesOrder order) {
        SalesOrderResponse resp = new SalesOrderResponse();
        resp.setId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setPaymentStatus(order.getPaymentStatus());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setTotalCost(order.getTotalCost());
        resp.setTotalProfit(order.getTotalProfit());
        resp.setDeliveryAddress(order.getDeliveryAddress());
        resp.setRemark(order.getRemark());
        resp.setOrderSource(order.getOrderSource());
        resp.setCancelBy(order.getCancelBy());
        resp.setCreatedAt(order.getCreatedAt());

        // 客户信息
        if (order.getCustomerId() != null) {
            CrmCustomer customer = customerMapper.selectById(order.getCustomerId());
            if (customer != null) {
                resp.setCustomer(toCustomerInfo(customer));
            }
        }

        // 订单明细
        List<OmsSalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrderItem>()
                        .eq(OmsSalesOrderItem::getOrderId, order.getId()));
        resp.setItems(toItemInfos(items));
        return resp;
    }

    /**
     * Entity → Response DTO — 批量预加载版本（消除 N+1）
     */
    private SalesOrderResponse toResponse(OmsSalesOrder order,
                                           Map<Long, CrmCustomer> customerMap,
                                           Map<Long, List<OmsSalesOrderItem>> itemsByOrderId) {
        SalesOrderResponse resp = new SalesOrderResponse();
        resp.setId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setPaymentStatus(order.getPaymentStatus());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setTotalCost(order.getTotalCost());
        resp.setTotalProfit(order.getTotalProfit());
        resp.setDeliveryAddress(order.getDeliveryAddress());
        resp.setRemark(order.getRemark());
        resp.setOrderSource(order.getOrderSource());
        resp.setCancelBy(order.getCancelBy());
        resp.setCreatedAt(order.getCreatedAt());

        if (order.getCustomerId() != null) {
            CrmCustomer customer = customerMap.get(order.getCustomerId());
            if (customer != null) {
                resp.setCustomer(toCustomerInfo(customer));
            }
        }

        resp.setItems(toItemInfos(itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList())));
        return resp;
    }

    private SalesOrderResponse.CustomerInfo toCustomerInfo(CrmCustomer customer) {
        SalesOrderResponse.CustomerInfo ci = new SalesOrderResponse.CustomerInfo();
        ci.setId(customer.getId());
        ci.setName(customer.getName());
        ci.setPhone(customer.getPhone());
        return ci;
    }

    private List<SalesOrderResponse.OrderItemInfo> toItemInfos(List<OmsSalesOrderItem> items) {
        return items.stream().map(item -> {
            SalesOrderResponse.OrderItemInfo info = new SalesOrderResponse.OrderItemInfo();
            info.setProductId(item.getProductId());
            info.setProductName(item.getProductName());
            info.setSpec(item.getSpec());
            info.setUnit(item.getUnit());
            info.setQuantity(item.getQuantity());
            info.setPrice(item.getPrice());
            info.setCostPrice(item.getCostPrice());
            info.setAmount(item.getAmount());
            info.setProfit(item.getProfit());
            return info;
        }).toList();
    }
}
