package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.SupplierRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.SupplierResponse;
import com.procurement.entity.CrmSupplier;
import com.procurement.mapper.PurchaseOrderMapper;
import com.procurement.mapper.SupplierMapper;
import com.procurement.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 供应商管理服务实现
 */
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierMapper supplierMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;

    @Override
    public PageResponse<SupplierResponse> listByPage(Long enterpriseId, Integer pageNum,
                                                      Integer pageSize, String keyword) {
        LambdaQueryWrapper<CrmSupplier> wrapper = new LambdaQueryWrapper<CrmSupplier>()
                .eq(CrmSupplier::getEnterpriseId, enterpriseId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CrmSupplier::getName, keyword)
                    .or().like(CrmSupplier::getPhone, keyword));
        }
        wrapper.orderByDesc(CrmSupplier::getId);

        Page<CrmSupplier> page = supplierMapper.selectPage(
                new Page<>(pageNum, pageSize), wrapper);

        List<CrmSupplier> suppliers = page.getRecords();
        if (suppliers.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), page.getTotal(), pageNum, pageSize);
        }

        // SQL 聚合：一次查询所有供应商的采购统计
        Set<Long> supplierIds = new HashSet<>();
        suppliers.forEach(s -> supplierIds.add(s.getId()));
        Map<Long, Map<String, Object>> statsMap = new HashMap<>();
        List<Map<String, Object>> statsList = purchaseOrderMapper.selectSupplierOrderStats(enterpriseId, supplierIds);
        for (Map<String, Object> s : statsList) {
            statsMap.put(((Number) s.get("supplierId")).longValue(), s);
        }

        List<SupplierResponse> records = suppliers.stream()
                .map(s -> toResponse(s, statsMap.get(s.getId()))).toList();

        return PageResponse.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public SupplierResponse getById(Long enterpriseId, Long id) {
        CrmSupplier supplier = supplierMapper.selectById(id);
        if (supplier == null || !supplier.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        List<Map<String, Object>> stats = purchaseOrderMapper.selectSupplierOrderStats(
                enterpriseId, Collections.singleton(id));
        SupplierResponse resp = toResponse(supplier, stats.isEmpty() ? null : stats.get(0));

        // 填充最近采购记录
        List<Map<String, Object>> recentRows = purchaseOrderMapper.selectRecentOrdersBySupplier(
                enterpriseId, id, 10);
        List<SupplierResponse.RecentOrder> recentOrders = recentRows.stream().map(row -> {
            SupplierResponse.RecentOrder ro = new SupplierResponse.RecentOrder();
            ro.setId(((Number) row.get("id")).longValue());
            ro.setOrderNo((String) row.get("orderNo"));
            ro.setTotalAmount((BigDecimal) row.get("totalAmount"));
            ro.setStatus((String) row.get("status"));
            ro.setCreatedAt(row.get("createdAt") != null ? row.get("createdAt").toString() : null);
            return ro;
        }).toList();

        // 批量查询商品明细，构建商品摘要（避免N+1查询）
        if (!recentOrders.isEmpty()) {
            List<Long> orderIds = recentOrders.stream()
                    .map(SupplierResponse.RecentOrder::getId).toList();
            List<Map<String, Object>> itemRows = purchaseOrderMapper.selectOrderItemsByOrderIds(orderIds);

            // 按订单ID分组
            Map<Long, List<Map<String, Object>>> itemsByOrder = itemRows.stream()
                    .collect(Collectors.groupingBy(r -> ((Number) r.get("orderId")).longValue()));

            for (SupplierResponse.RecentOrder ro : recentOrders) {
                ro.setItemSummary(buildItemSummary(itemsByOrder.get(ro.getId())));
            }
        }

        resp.setRecentOrders(recentOrders);
        return resp;
    }

    /**
     * 构建商品摘要：取前2个商品名×数量，总数超过2则追加"等N种"
     * 例：铅笔×100、橡皮×50 等3种
     */
    private String buildItemSummary(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return "无商品";
        }
        int total = items.size();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, total); i++) {
            if (i > 0) sb.append("、");
            Map<String, Object> item = items.get(i);
            sb.append(item.get("productName"))
              .append("×")
              .append(((Number) item.get("quantity")).intValue());
        }
        if (total > 2) {
            sb.append(" 等").append(total).append("种");
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public SupplierResponse create(Long enterpriseId, SupplierRequest request) {
        CrmSupplier supplier = new CrmSupplier();
        supplier.setEnterpriseId(enterpriseId);
        supplier.setName(request.getName());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setMainCategory(request.getMainCategory());
        supplier.setRemark(request.getRemark());
        supplierMapper.insert(supplier);
        return toResponse(supplier, null);
    }

    @Override
    @Transactional
    public SupplierResponse update(Long enterpriseId, Long id, SupplierRequest request) {
        CrmSupplier supplier = supplierMapper.selectById(id);
        if (supplier == null || !supplier.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        supplier.setName(request.getName());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setMainCategory(request.getMainCategory());
        supplier.setRemark(request.getRemark());
        supplierMapper.updateById(supplier);
        return toResponse(supplier, null);
    }

    @Override
    @Transactional
    public void delete(Long enterpriseId, Long id) {
        CrmSupplier supplier = supplierMapper.selectById(id);
        if (supplier == null || !supplier.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        supplierMapper.deleteById(id);
    }

    /**
     * Entity → Response DTO（使用 SQL 聚合统计数据替代全表扫描）
     */
    private SupplierResponse toResponse(CrmSupplier supplier, Map<String, Object> stats) {
        SupplierResponse resp = new SupplierResponse();
        resp.setId(supplier.getId());
        resp.setName(supplier.getName());
        resp.setPhone(supplier.getPhone());
        resp.setAddress(supplier.getAddress());
        resp.setMainCategory(supplier.getMainCategory());
        resp.setRemark(supplier.getRemark());

        if (stats != null) {
            resp.setPurchaseCount(((Number) stats.get("purchaseCount")).intValue());
            resp.setTotalAmount((BigDecimal) stats.get("totalAmount"));
        } else {
            resp.setPurchaseCount(0);
            resp.setTotalAmount(BigDecimal.ZERO);
        }

        return resp;
    }
}
