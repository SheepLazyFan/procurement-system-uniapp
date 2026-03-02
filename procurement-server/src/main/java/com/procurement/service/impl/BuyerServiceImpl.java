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
import com.procurement.service.BuyerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 买家端服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuyerServiceImpl implements BuyerService {

    private final EnterpriseMapper enterpriseMapper;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

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
        result.put("contactPhone", enterprise.getContactPhone());
        result.put("categoryCount", categoryCount);
        result.put("productCount", productCount);
        return result;
    }

    @Override
    public List<Map<String, Object>> getStoreCategories(Long enterpriseId) {
        List<PmsCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<PmsCategory>()
                        .eq(PmsCategory::getEnterpriseId, enterpriseId)
                        .orderByAsc(PmsCategory::getSortOrder));

        List<Map<String, Object>> result = new ArrayList<>();
        for (PmsCategory cat : categories) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", cat.getId());
            item.put("name", cat.getName());
            result.add(item);
        }
        return result;
    }

    @Override
    public PageResponse<ProductResponse> getStoreProducts(Long enterpriseId, Long categoryId,
                                                           String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<PmsProduct> wrapper = new LambdaQueryWrapper<PmsProduct>()
                .eq(PmsProduct::getEnterpriseId, enterpriseId)
                .eq(PmsProduct::getStatus, 1)        // 仅上架商品
                .gt(PmsProduct::getStock, 0);         // 仅有库存商品

        if (categoryId != null) {
            wrapper.eq(PmsProduct::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(PmsProduct::getName, keyword)
                    .or().like(PmsProduct::getSpec, keyword));
        }

        wrapper.orderByDesc(PmsProduct::getId);

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
            // costPrice 不返回给买家
            resp.setStock(p.getStock());
            resp.setImages(p.getImages());
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
        result.put("stock", product.getStock());
        result.put("images", product.getImages());
        result.put("enterpriseName", enterprise != null ? enterprise.getName() : "");
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

        // 生成订单号（Redis 原子自增）
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = "order:no:" + OrderConstants.SALES_ORDER_PREFIX + ":" + enterpriseId + ":" + dateStr;
        Long seq = redisTemplate.opsForValue().increment(redisKey);
        if (seq != null && seq == 1) {
            redisTemplate.expire(redisKey, 2, TimeUnit.DAYS);
        }
        String orderNo = OrderConstants.SALES_ORDER_PREFIX + dateStr + String.format("%04d", seq);

        // 创建订单
        OmsSalesOrder order = new OmsSalesOrder();
        order.setOrderNo(orderNo);
        order.setEnterpriseId(enterpriseId);
        order.setCustomerId(customer != null ? customer.getId() : null);
        order.setStatus(OrderConstants.SALES_PENDING);
        order.setPaymentStatus(OrderConstants.PAY_UNPAID);
        order.setRemark(request.getRemark());

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        List<OmsSalesOrderItem> items = new ArrayList<>();
        for (BuyerOrderRequest.BuyerItemRequest itemReq : request.getItems()) {
            PmsProduct product = productMapper.selectById(itemReq.getProductId());
            if (product == null || !product.getEnterpriseId().equals(enterpriseId)
                    || product.getStatus() != 1) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        "商品不存在或已下架: ID=" + itemReq.getProductId());
            }
            if (product.getStock() < itemReq.getQuantity()) {
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
            item.setCostPrice(product.getCostPrice());

            BigDecimal amount = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            BigDecimal profit = product.getPrice().subtract(product.getCostPrice())
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            item.setAmount(amount);
            item.setProfit(profit);

            totalAmount = totalAmount.add(amount);
            totalCost = totalCost.add(product.getCostPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
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

        if (OrderConstants.PAY_PAID.equals(order.getPaymentStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        order.setPaymentStatus(OrderConstants.PAY_PAID);
        salesOrderMapper.updateById(order);
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

        List<SalesOrderResponse> records = page.getRecords().stream()
                .map(o -> toOrderResponse(o, null)).toList();

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

    private SalesOrderResponse toOrderResponse(OmsSalesOrder order, CrmCustomer customer) {
        SalesOrderResponse resp = new SalesOrderResponse();
        resp.setId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setPaymentStatus(order.getPaymentStatus());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setRemark(order.getRemark());
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
}
