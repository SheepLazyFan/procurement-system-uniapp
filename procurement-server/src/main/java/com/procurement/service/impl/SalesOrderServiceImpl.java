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
import com.procurement.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private final StringRedisTemplate redisTemplate;

    @Override
    public PageResponse<SalesOrderResponse> listByPage(Long enterpriseId, Integer pageNum, Integer pageSize,
                                                        String status, String paymentStatus,
                                                        Long customerId, String startDate, String endDate) {
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
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(OmsSalesOrder::getCreatedAt, startDate + " 00:00:00");
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(OmsSalesOrder::getCreatedAt, endDate + " 23:59:59");
        }

        wrapper.orderByDesc(OmsSalesOrder::getId);

        Page<OmsSalesOrder> page = salesOrderMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        List<SalesOrderResponse> records = page.getRecords().stream()
                .map(this::toResponse).toList();

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
        // 生成订单号: SO + 日期 + 4位序号
        String orderNo = generateOrderNo(OrderConstants.SALES_ORDER_PREFIX, enterpriseId);

        // 创建主订单
        OmsSalesOrder order = new OmsSalesOrder();
        order.setOrderNo(orderNo);
        order.setEnterpriseId(enterpriseId);
        order.setCustomerId(request.getCustomerId());
        order.setStatus(OrderConstants.SALES_PENDING);
        order.setPaymentStatus(OrderConstants.PAY_UNPAID);
        order.setRemark(request.getRemark());

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        // 创建订单明细（快照商品信息）
        List<OmsSalesOrderItem> items = new ArrayList<>();
        for (SalesOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            PmsProduct product = productMapper.selectById(itemReq.getProductId());
            if (product == null || !product.getEnterpriseId().equals(enterpriseId)) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        "商品不存在: ID=" + itemReq.getProductId());
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

        // 插入明细
        for (OmsSalesOrderItem item : items) {
            item.setOrderId(order.getId());
            salesOrderItemMapper.insert(item);
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
        order.setStatus(OrderConstants.SALES_SHIPPED);
        salesOrderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void complete(Long enterpriseId, Long id) {
        OmsSalesOrder order = getAndValidate(enterpriseId, id);
        if (!OrderConstants.SALES_SHIPPED.equals(order.getStatus())
                && !OrderConstants.SALES_CONFIRMED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 扣减库存
        List<OmsSalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrderItem>()
                        .eq(OmsSalesOrderItem::getOrderId, id));

        for (OmsSalesOrderItem item : items) {
            int rows = productMapper.adjustStock(item.getProductId(), -item.getQuantity());
            if (rows == 0) {
                PmsProduct product = productMapper.selectById(item.getProductId());
                String name = product != null ? product.getName() : "ID=" + item.getProductId();
                throw new BusinessException(ResultCode.STOCK_INSUFFICIENT.getCode(),
                        "商品 [" + name + "] 库存不足");
            }
        }

        order.setStatus(OrderConstants.SALES_COMPLETED);
        salesOrderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void cancel(Long enterpriseId, Long id) {
        OmsSalesOrder order = getAndValidate(enterpriseId, id);

        // 状态检查必须在库存操作之前，防止并发取消导致库存双倍恢复
        if (OrderConstants.SALES_CANCELLED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 如果订单已完成（已扣减库存），需要恢复库存
        if (OrderConstants.SALES_COMPLETED.equals(order.getStatus())) {
            List<OmsSalesOrderItem> items = salesOrderItemMapper.selectList(
                    new LambdaQueryWrapper<OmsSalesOrderItem>()
                            .eq(OmsSalesOrderItem::getOrderId, id));
            for (OmsSalesOrderItem item : items) {
                productMapper.adjustStock(item.getProductId(), item.getQuantity());
            }
        }

        order.setStatus(OrderConstants.SALES_CANCELLED);
        salesOrderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void pay(Long enterpriseId, Long id) {
        OmsSalesOrder order = getAndValidate(enterpriseId, id);
        if (OrderConstants.PAY_PAID.equals(order.getPaymentStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        order.setPaymentStatus(OrderConstants.PAY_PAID);
        salesOrderMapper.updateById(order);
    }

    // ===================== 私有方法 =====================

    private OmsSalesOrder getAndValidate(Long enterpriseId, Long id) {
        OmsSalesOrder order = salesOrderMapper.selectById(id);
        if (order == null || !order.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return order;
    }

    /**
     * 生成订单编号: 前缀 + 日期(yyyyMMdd) + 4位序号
     * 使用 Redis 原子自增保证并发安全
     */
    private String generateOrderNo(String prefix, Long enterpriseId) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = "order:no:" + prefix + ":" + enterpriseId + ":" + dateStr;
        Long seq = redisTemplate.opsForValue().increment(redisKey);
        // 设置过期时间为2天，避免 Key 堆积
        if (seq != null && seq == 1) {
            redisTemplate.expire(redisKey, 2, TimeUnit.DAYS);
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    /**
     * Entity → Response DTO（含客户信息和订单明细）
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
        resp.setRemark(order.getRemark());
        resp.setCreatedAt(order.getCreatedAt());

        // 客户信息
        if (order.getCustomerId() != null) {
            CrmCustomer customer = customerMapper.selectById(order.getCustomerId());
            if (customer != null) {
                SalesOrderResponse.CustomerInfo ci = new SalesOrderResponse.CustomerInfo();
                ci.setId(customer.getId());
                ci.setName(customer.getName());
                ci.setPhone(customer.getPhone());
                resp.setCustomer(ci);
            }
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
            info.setCostPrice(item.getCostPrice());
            info.setAmount(item.getAmount());
            info.setProfit(item.getProfit());
            return info;
        }).toList();

        resp.setItems(itemInfos);
        return resp;
    }
}
