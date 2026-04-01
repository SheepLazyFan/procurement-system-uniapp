package com.procurement.service;

import com.procurement.dto.request.BindProductsRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.ProductSupplierResponse;

import java.math.BigDecimal;

/**
 * 供应商-商品关联管理服务接口
 */
public interface ProductSupplierService {

    /**
     * 分页查询供应商关联的商品列表
     */
    PageResponse<ProductSupplierResponse> listLinkedProducts(
            Long enterpriseId, Long supplierId,
            Integer pageNum, Integer pageSize,
            String keyword, Long categoryId);

    /**
     * 批量绑定商品到供应商（每个商品填写各自的供货价）
     */
    void bindProducts(Long enterpriseId, Long supplierId, BindProductsRequest request);

    /**
     * 解绑供应商与商品的关联（物理删除，保证 UNIQUE KEY 可重复绑定）
     */
    void unbindProduct(Long enterpriseId, Long supplierId, Long productId);

    /**
     * 更新指定绑定关系的供货价
     */
    void updateSupplyPrice(Long enterpriseId, Long supplierId, Long productId, BigDecimal supplyPrice);
}
