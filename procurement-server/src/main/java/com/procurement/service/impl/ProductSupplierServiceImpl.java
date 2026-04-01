package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.procurement.common.exception.BusinessException;
import com.procurement.common.result.ResultCode;
import com.procurement.dto.request.BindProductsRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.ProductSupplierResponse;
import com.procurement.entity.CrmSupplier;
import com.procurement.entity.PmsProduct;
import com.procurement.entity.PmsProductSupplier;
import com.procurement.mapper.ProductMapper;
import com.procurement.mapper.ProductSupplierMapper;
import com.procurement.mapper.SupplierMapper;
import com.procurement.service.ProductSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 供应商-商品关联管理服务实现
 */
@Service
@RequiredArgsConstructor
public class ProductSupplierServiceImpl implements ProductSupplierService {

    private final ProductSupplierMapper productSupplierMapper;
    private final ProductMapper productMapper;
    private final SupplierMapper supplierMapper;

    @Override
    public PageResponse<ProductSupplierResponse> listLinkedProducts(
            Long enterpriseId, Long supplierId,
            Integer pageNum, Integer pageSize,
            String keyword, Long categoryId) {

        validateSupplier(enterpriseId, supplierId);

        int offset = (pageNum - 1) * pageSize;
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;

        long total = productSupplierMapper.countLinkedProducts(enterpriseId, supplierId, kw, categoryId);
        List<ProductSupplierResponse> records = new ArrayList<>();

        if (total > 0) {
            List<Map<String, Object>> rows = productSupplierMapper.selectLinkedProducts(
                    enterpriseId, supplierId, kw, categoryId, offset, pageSize);
            for (Map<String, Object> row : rows) {
                ProductSupplierResponse r = new ProductSupplierResponse();
                r.setProductId(((Number) row.get("productId")).longValue());
                r.setProductName((String) row.get("productName"));
                r.setSpec((String) row.get("spec"));
                r.setUnit((String) row.get("unit"));
                r.setCategoryId(row.get("categoryId") != null
                        ? ((Number) row.get("categoryId")).longValue() : null);
                r.setCategoryName((String) row.get("categoryName"));
                r.setSupplyPrice((BigDecimal) row.get("supplyPrice"));
                r.setStock(row.get("stock") != null ? ((Number) row.get("stock")).intValue() : 0);
                r.setSupplierCount(row.get("supplierCount") != null
                        ? ((Number) row.get("supplierCount")).intValue() : 1);
                records.add(r);
            }
        }

        return PageResponse.of(records, total, pageNum, pageSize);
    }

    @Override
    @Transactional
    public void bindProducts(Long enterpriseId, Long supplierId, BindProductsRequest request) {
        validateSupplier(enterpriseId, supplierId);

        // 先全量校验，再统一插入，保证事务原子性
        List<String> errors = new ArrayList<>();
        List<PmsProductSupplier> toInsert = new ArrayList<>();

        for (BindProductsRequest.BindItem item : request.getItems()) {
            // 验证商品存在且属于该企业
            PmsProduct product = productMapper.selectById(item.getProductId());
            if (product == null || !product.getEnterpriseId().equals(enterpriseId)) {
                errors.add("商品ID " + item.getProductId() + " 不存在");
                continue;
            }

            // 检查是否已绑定（含软删除记录已被物理删除，无需额外处理）
            Long existCount = productSupplierMapper.selectCount(
                    new LambdaQueryWrapper<PmsProductSupplier>()
                            .eq(PmsProductSupplier::getEnterpriseId, enterpriseId)
                            .eq(PmsProductSupplier::getSupplierId, supplierId)
                            .eq(PmsProductSupplier::getProductId, item.getProductId()));
            if (existCount > 0) {
                errors.add("商品「" + product.getName() + "」已与该供应商绑定");
                continue;
            }

            PmsProductSupplier ps = new PmsProductSupplier();
            ps.setEnterpriseId(enterpriseId);
            ps.setSupplierId(supplierId);
            ps.setProductId(item.getProductId());
            ps.setSupplyPrice(item.getSupplyPrice());
            ps.setIsDefault(0);
            toInsert.add(ps);
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), String.join("；", errors));
        }

        toInsert.forEach(productSupplierMapper::insert);
    }

    @Override
    @Transactional
    public void unbindProduct(Long enterpriseId, Long supplierId, Long productId) {
        validateSupplier(enterpriseId, supplierId);

        // 使用物理删除，确保 UNIQUE KEY (product_id, supplier_id) 释放，支持将来重新绑定
        int deleted = productSupplierMapper.physicalDelete(enterpriseId, supplierId, productId);
        if (deleted == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public void updateSupplyPrice(Long enterpriseId, Long supplierId, Long productId, BigDecimal supplyPrice) {
        validateSupplier(enterpriseId, supplierId);

        int updated = productSupplierMapper.update(null,
                new LambdaUpdateWrapper<PmsProductSupplier>()
                        .eq(PmsProductSupplier::getEnterpriseId, enterpriseId)
                        .eq(PmsProductSupplier::getSupplierId, supplierId)
                        .eq(PmsProductSupplier::getProductId, productId)
                        .set(PmsProductSupplier::getSupplyPrice, supplyPrice));
        if (updated == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }

    private void validateSupplier(Long enterpriseId, Long supplierId) {
        CrmSupplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null || !supplier.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }
}
