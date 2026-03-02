package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.procurement.common.constant.OrderConstants;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.PurchaseOrderRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.PurchaseOrderResponse;
import com.procurement.entity.*;
import com.procurement.mapper.*;
import com.procurement.service.PurchaseOrderService;
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
 * 采购订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final ProductMapper productMapper;
    private final SupplierMapper supplierMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public PageResponse<PurchaseOrderResponse> listByPage(Long enterpriseId, Integer pageNum, Integer pageSize,
                                                           String status, Long supplierId) {
        LambdaQueryWrapper<OmsPurchaseOrder> wrapper = new LambdaQueryWrapper<OmsPurchaseOrder>()
                .eq(OmsPurchaseOrder::getEnterpriseId, enterpriseId);

        if (StringUtils.hasText(status)) {
            wrapper.eq(OmsPurchaseOrder::getStatus, status);
        }
        if (supplierId != null) {
            wrapper.eq(OmsPurchaseOrder::getSupplierId, supplierId);
        }

        wrapper.orderByDesc(OmsPurchaseOrder::getId);

        Page<OmsPurchaseOrder> page = purchaseOrderMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        List<PurchaseOrderResponse> records = page.getRecords().stream()
                .map(this::toResponse).toList();

        return PageResponse.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public PurchaseOrderResponse getById(Long enterpriseId, Long id) {
        OmsPurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null || !order.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toResponse(order);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse create(Long enterpriseId, PurchaseOrderRequest request) {
        // 生成订单号: PO + 日期 + 4位序号
        String orderNo = generateOrderNo(enterpriseId);

        OmsPurchaseOrder order = new OmsPurchaseOrder();
        order.setOrderNo(orderNo);
        order.setEnterpriseId(enterpriseId);
        order.setSupplierId(request.getSupplierId());
        order.setStatus(OrderConstants.PURCHASE_PENDING);
        order.setRemark(request.getRemark());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OmsPurchaseOrderItem> items = new ArrayList<>();

        for (PurchaseOrderRequest.PurchaseItemRequest itemReq : request.getItems()) {
            PmsProduct product = productMapper.selectById(itemReq.getProductId());
            if (product == null || !product.getEnterpriseId().equals(enterpriseId)) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        "商品不存在: ID=" + itemReq.getProductId());
            }

            OmsPurchaseOrderItem item = new OmsPurchaseOrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setSpec(product.getSpec());
            item.setUnit(product.getUnit());
            item.setQuantity(itemReq.getQuantity());
            item.setPrice(itemReq.getPrice());

            BigDecimal amount = itemReq.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            item.setAmount(amount);
            totalAmount = totalAmount.add(amount);

            items.add(item);
        }

        order.setTotalAmount(totalAmount);
        purchaseOrderMapper.insert(order);

        for (OmsPurchaseOrderItem item : items) {
            item.setOrderId(order.getId());
            purchaseOrderItemMapper.insert(item);
        }

        return toResponse(order);
    }

    @Override
    @Transactional
    public void purchasing(Long enterpriseId, Long id) {
        OmsPurchaseOrder order = getAndValidate(enterpriseId, id);
        if (!OrderConstants.PURCHASE_PENDING.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(OrderConstants.PURCHASE_PURCHASING);
        purchaseOrderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void arrive(Long enterpriseId, Long id) {
        OmsPurchaseOrder order = getAndValidate(enterpriseId, id);
        if (!OrderConstants.PURCHASE_PURCHASING.equals(order.getStatus())
                && !OrderConstants.PURCHASE_PENDING.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 自动增加库存
        List<OmsPurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsPurchaseOrderItem>()
                        .eq(OmsPurchaseOrderItem::getOrderId, id));

        for (OmsPurchaseOrderItem item : items) {
            productMapper.adjustStock(item.getProductId(), item.getQuantity());
        }

        order.setStatus(OrderConstants.PURCHASE_ARRIVED);
        purchaseOrderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void complete(Long enterpriseId, Long id) {
        OmsPurchaseOrder order = getAndValidate(enterpriseId, id);
        if (!OrderConstants.PURCHASE_ARRIVED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(OrderConstants.PURCHASE_COMPLETED);
        purchaseOrderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void cancel(Long enterpriseId, Long id) {
        OmsPurchaseOrder order = getAndValidate(enterpriseId, id);
        if (OrderConstants.PURCHASE_CANCELLED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 如果已到货（已入库），恢复库存
        if (OrderConstants.PURCHASE_ARRIVED.equals(order.getStatus())
                || OrderConstants.PURCHASE_COMPLETED.equals(order.getStatus())) {
            List<OmsPurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                    new LambdaQueryWrapper<OmsPurchaseOrderItem>()
                            .eq(OmsPurchaseOrderItem::getOrderId, id));
            for (OmsPurchaseOrderItem item : items) {
                productMapper.adjustStock(item.getProductId(), -item.getQuantity());
            }
        }

        order.setStatus(OrderConstants.PURCHASE_CANCELLED);
        purchaseOrderMapper.updateById(order);
    }

    // ===================== 私有方法 =====================

    private OmsPurchaseOrder getAndValidate(Long enterpriseId, Long id) {
        OmsPurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null || !order.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return order;
    }

    private String generateOrderNo(Long enterpriseId) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = "order:no:" + OrderConstants.PURCHASE_ORDER_PREFIX + ":" + enterpriseId + ":" + dateStr;
        Long seq = redisTemplate.opsForValue().increment(redisKey);
        if (seq != null && seq == 1) {
            redisTemplate.expire(redisKey, 2, TimeUnit.DAYS);
        }
        return OrderConstants.PURCHASE_ORDER_PREFIX + dateStr + String.format("%04d", seq);
    }

    private PurchaseOrderResponse toResponse(OmsPurchaseOrder order) {
        PurchaseOrderResponse resp = new PurchaseOrderResponse();
        resp.setId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setRemark(order.getRemark());
        resp.setCreatedAt(order.getCreatedAt());

        // 供应商信息
        if (order.getSupplierId() != null) {
            CrmSupplier supplier = supplierMapper.selectById(order.getSupplierId());
            if (supplier != null) {
                PurchaseOrderResponse.SupplierInfo si = new PurchaseOrderResponse.SupplierInfo();
                si.setId(supplier.getId());
                si.setName(supplier.getName());
                si.setPhone(supplier.getPhone());
                resp.setSupplier(si);
            }
        }

        // 订单明细
        List<OmsPurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsPurchaseOrderItem>()
                        .eq(OmsPurchaseOrderItem::getOrderId, order.getId()));

        List<PurchaseOrderResponse.OrderItemInfo> itemInfos = items.stream().map(item -> {
            PurchaseOrderResponse.OrderItemInfo info = new PurchaseOrderResponse.OrderItemInfo();
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
