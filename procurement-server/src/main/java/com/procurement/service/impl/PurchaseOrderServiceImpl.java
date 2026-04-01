package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.procurement.service.StockWarningNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import com.procurement.common.util.OrderNoGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final ProductSupplierMapper productSupplierMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final StockWarningNotificationService notificationService;

    @Override
    public PageResponse<PurchaseOrderResponse> listByPage(Long enterpriseId, Integer pageNum, Integer pageSize,
                                                           String status, Long supplierId,
                                                           String keyword, String startDate, String endDate,
                                                           BigDecimal minAmount, BigDecimal maxAmount, String sortBy) {
        LambdaQueryWrapper<OmsPurchaseOrder> wrapper = new LambdaQueryWrapper<OmsPurchaseOrder>()
                .eq(OmsPurchaseOrder::getEnterpriseId, enterpriseId);

        if (StringUtils.hasText(status)) {
            wrapper.eq(OmsPurchaseOrder::getStatus, status);
        }
        if (supplierId != null) {
            wrapper.eq(OmsPurchaseOrder::getSupplierId, supplierId);
        }

        // 关键词搜索：订单号 LIKE 或 供应商名称匹配
        if (StringUtils.hasText(keyword)) {
            List<Long> matchingSupplierIds = supplierMapper.selectList(
                    new LambdaQueryWrapper<CrmSupplier>()
                            .eq(CrmSupplier::getEnterpriseId, enterpriseId)
                            .like(CrmSupplier::getName, keyword))
                    .stream().map(CrmSupplier::getId).toList();
            wrapper.and(w -> {
                w.like(OmsPurchaseOrder::getOrderNo, keyword);
                if (!matchingSupplierIds.isEmpty()) {
                    w.or().in(OmsPurchaseOrder::getSupplierId, matchingSupplierIds);
                }
            });
        }

        // 时间范围
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(OmsPurchaseOrder::getCreatedAt, LocalDate.parse(startDate).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(OmsPurchaseOrder::getCreatedAt, LocalDate.parse(endDate).atTime(23, 59, 59));
        }

        // 金额范围
        if (minAmount != null) {
            wrapper.ge(OmsPurchaseOrder::getTotalAmount, minAmount);
        }
        if (maxAmount != null) {
            wrapper.le(OmsPurchaseOrder::getTotalAmount, maxAmount);
        }

        // 排序
        if (StringUtils.hasText(sortBy)) {
            switch (sortBy) {
                case "amount_asc" -> wrapper.orderByAsc(OmsPurchaseOrder::getTotalAmount);
                case "amount_desc" -> wrapper.orderByDesc(OmsPurchaseOrder::getTotalAmount);
                case "time_asc" -> wrapper.orderByAsc(OmsPurchaseOrder::getCreatedAt);
                default -> wrapper.orderByDesc(OmsPurchaseOrder::getId);
            }
        } else {
            wrapper.orderByDesc(OmsPurchaseOrder::getId);
        }

        Page<OmsPurchaseOrder> page = purchaseOrderMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        List<OmsPurchaseOrder> orders = page.getRecords();
        if (orders.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), page.getTotal(), pageNum, pageSize);
        }

        // 批量预加载供应商
        Set<Long> supplierIds = orders.stream()
                .map(OmsPurchaseOrder::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CrmSupplier> supplierMap = supplierIds.isEmpty()
                ? Collections.emptyMap()
                : supplierMapper.selectBatchIds(supplierIds).stream()
                        .collect(Collectors.toMap(CrmSupplier::getId, Function.identity()));

        // 批量预加载订单明细
        List<Long> orderIds = orders.stream().map(OmsPurchaseOrder::getId).toList();
        Map<Long, List<OmsPurchaseOrderItem>> itemsByOrderId = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsPurchaseOrderItem>()
                        .in(OmsPurchaseOrderItem::getOrderId, orderIds))
                .stream().collect(Collectors.groupingBy(OmsPurchaseOrderItem::getOrderId));

        List<PurchaseOrderResponse> records = orders.stream()
                .map(o -> toResponse(o, supplierMap, itemsByOrderId)).toList();

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
        // 生成订单号: PO + 日期 + 4位序号（Redis 优先，DB 降级）
        String orderNo = orderNoGenerator.generate(OrderConstants.PURCHASE_ORDER_PREFIX, enterpriseId, "oms_purchase_order");

        OmsPurchaseOrder order = new OmsPurchaseOrder();
        order.setOrderNo(orderNo);
        order.setEnterpriseId(enterpriseId);
        order.setSupplierId(request.getSupplierId());
        order.setStatus(OrderConstants.PURCHASE_DRAFT);
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

        // 供货价回写：将本次采购的实际成交价同步更新到 pms_product_supplier.supply_price
        // 业务意图：supply_price 记录最新一次实际成交价，方便下次快速采购预填
        if (request.getSupplierId() != null) {
            for (OmsPurchaseOrderItem item : items) {
                productSupplierMapper.update(null,
                        new LambdaUpdateWrapper<PmsProductSupplier>()
                                .eq(PmsProductSupplier::getEnterpriseId, enterpriseId)
                                .eq(PmsProductSupplier::getSupplierId, request.getSupplierId())
                                .eq(PmsProductSupplier::getProductId, item.getProductId())
                                .set(PmsProductSupplier::getSupplyPrice, item.getPrice()));
            }
        }

        return toResponse(order);
    }

    @Override
    @Transactional
    public void purchasing(Long enterpriseId, Long id) {
        OmsPurchaseOrder order = getAndValidate(enterpriseId, id);
        if (!OrderConstants.PURCHASE_DRAFT.equals(order.getStatus())
                && !OrderConstants.PURCHASE_PENDING.equals(order.getStatus())) {
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
                && !OrderConstants.PURCHASE_DRAFT.equals(order.getStatus())
                && !OrderConstants.PURCHASE_PENDING.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 自动增加库存
        List<OmsPurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsPurchaseOrderItem>()
                        .eq(OmsPurchaseOrderItem::getOrderId, id));

        List<Long> arrivedProductIds = new ArrayList<>();
        for (OmsPurchaseOrderItem item : items) {
            int affected = productMapper.adjustStock(item.getProductId(), item.getQuantity());
            if (affected == 0) {
                log.warn("采购到货入库失败：商品可能已被删除, productId={}, orderId={}",
                        item.getProductId(), id);
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        "商品不存在，无法入库: productId=" + item.getProductId());
            }
            arrivedProductIds.add(item.getProductId());
        }

        // 采购入库后清除预警去重 key，库存恢复时允许下次跌破再次通知
        if (!arrivedProductIds.isEmpty()) {
            final Long eid = enterpriseId;
            final List<Long> pids = arrivedProductIds;
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationService.clearDedupOnRestock(pids, eid);
                    }
                });
            } else {
                notificationService.clearDedupOnRestock(arrivedProductIds, enterpriseId);
            }
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

        // 仅 DRAFT / PURCHASING 可取消 — 不涉及库存变动
        // ARRIVED（已入库）不可取消 — 应走退货流程
        // COMPLETED / CANCELLED 不可取消
        if (!OrderConstants.PURCHASE_DRAFT.equals(order.getStatus())
                && !OrderConstants.PURCHASE_PURCHASING.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        order.setStatus(OrderConstants.PURCHASE_CANCELLED);
        purchaseOrderMapper.updateById(order);
    }

    @Override
    public Map<String, Long> countByStatus(Long enterpriseId, String keyword, String startDate, String endDate,
                                            BigDecimal minAmount, BigDecimal maxAmount, Long supplierId) {
        // 构建公共筛选条件（不含 status）
        List<Long> matchingSupplierIds = null;
        if (StringUtils.hasText(keyword)) {
            matchingSupplierIds = supplierMapper.selectList(
                    new LambdaQueryWrapper<CrmSupplier>()
                            .eq(CrmSupplier::getEnterpriseId, enterpriseId)
                            .like(CrmSupplier::getName, keyword))
                    .stream().map(CrmSupplier::getId).toList();
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        // ALL + 各状态
        for (String s : List.of("ALL", "DRAFT", "PURCHASING", "ARRIVED", "COMPLETED")) {
            LambdaQueryWrapper<OmsPurchaseOrder> w = new LambdaQueryWrapper<OmsPurchaseOrder>()
                    .eq(OmsPurchaseOrder::getEnterpriseId, enterpriseId);
            if (!"ALL".equals(s)) {
                w.eq(OmsPurchaseOrder::getStatus, s);
            }
            if (supplierId != null) {
                w.eq(OmsPurchaseOrder::getSupplierId, supplierId);
            }
            if (StringUtils.hasText(keyword)) {
                final List<Long> sIds = matchingSupplierIds;
                w.and(ww -> {
                    ww.like(OmsPurchaseOrder::getOrderNo, keyword);
                    if (sIds != null && !sIds.isEmpty()) {
                        ww.or().in(OmsPurchaseOrder::getSupplierId, sIds);
                    }
                });
            }
            if (StringUtils.hasText(startDate)) {
                w.ge(OmsPurchaseOrder::getCreatedAt, LocalDate.parse(startDate).atStartOfDay());
            }
            if (StringUtils.hasText(endDate)) {
                w.le(OmsPurchaseOrder::getCreatedAt, LocalDate.parse(endDate).atTime(23, 59, 59));
            }
            if (minAmount != null) {
                w.ge(OmsPurchaseOrder::getTotalAmount, minAmount);
            }
            if (maxAmount != null) {
                w.le(OmsPurchaseOrder::getTotalAmount, maxAmount);
            }
            counts.put(s, purchaseOrderMapper.selectCount(w));
        }
        return counts;
    }

    // ===================== 私有方法 =====================

    private OmsPurchaseOrder getAndValidate(Long enterpriseId, Long id) {
        OmsPurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null || !order.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return order;
    }

    /** Entity → Response DTO — 单条查询版本 */
    private PurchaseOrderResponse toResponse(OmsPurchaseOrder order) {
        PurchaseOrderResponse resp = buildBaseResponse(order);

        if (order.getSupplierId() != null) {
            CrmSupplier supplier = supplierMapper.selectById(order.getSupplierId());
            if (supplier != null) {
                resp.setSupplier(toSupplierInfo(supplier));
            }
        }

        List<OmsPurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsPurchaseOrderItem>()
                        .eq(OmsPurchaseOrderItem::getOrderId, order.getId()));
        resp.setItems(toItemInfos(items));
        return resp;
    }

    /** Entity → Response DTO — 批量预加载版本（消除 N+1） */
    private PurchaseOrderResponse toResponse(OmsPurchaseOrder order,
                                              Map<Long, CrmSupplier> supplierMap,
                                              Map<Long, List<OmsPurchaseOrderItem>> itemsByOrderId) {
        PurchaseOrderResponse resp = buildBaseResponse(order);

        if (order.getSupplierId() != null) {
            CrmSupplier supplier = supplierMap.get(order.getSupplierId());
            if (supplier != null) {
                resp.setSupplier(toSupplierInfo(supplier));
            }
        }
        resp.setItems(toItemInfos(itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList())));
        return resp;
    }

    private PurchaseOrderResponse buildBaseResponse(OmsPurchaseOrder order) {
        PurchaseOrderResponse resp = new PurchaseOrderResponse();
        resp.setId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setRemark(order.getRemark());
        resp.setCreatedAt(order.getCreatedAt());
        return resp;
    }

    private PurchaseOrderResponse.SupplierInfo toSupplierInfo(CrmSupplier supplier) {
        PurchaseOrderResponse.SupplierInfo si = new PurchaseOrderResponse.SupplierInfo();
        si.setId(supplier.getId());
        si.setName(supplier.getName());
        si.setPhone(supplier.getPhone());
        return si;
    }

    private List<PurchaseOrderResponse.OrderItemInfo> toItemInfos(List<OmsPurchaseOrderItem> items) {
        return items.stream().map(item -> {
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
    }
}
