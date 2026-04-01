package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.request.BindProductsRequest;
import com.procurement.dto.request.UpdateSupplyPriceRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.ProductSupplierResponse;
import com.procurement.security.LoginUser;
import com.procurement.service.ProductSupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 供应商-商品关联管理控制器
 * 路径设计：/suppliers/{supplierId}/products — 资源嵌套，语义清晰
 */
@Tag(name = "供应商-商品关联")
@RestController
@RequestMapping("/suppliers/{supplierId}/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'WAREHOUSE')")
public class ProductSupplierController {

    private final ProductSupplierService productSupplierService;

    @Operation(summary = "查询供应商关联商品（分页）")
    @GetMapping
    public R<PageResponse<ProductSupplierResponse>> listLinkedProducts(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long supplierId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        return R.ok(productSupplierService.listLinkedProducts(
                loginUser.getEnterpriseId(), supplierId, pageNum, pageSize, keyword, categoryId));
    }

    @Operation(summary = "批量绑定商品到供应商（含各自供货价）")
    @PostMapping
    public R<Void> bindProducts(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long supplierId,
            @Valid @RequestBody BindProductsRequest request) {
        productSupplierService.bindProducts(loginUser.getEnterpriseId(), supplierId, request);
        return R.ok();
    }

    @Operation(summary = "解绑供应商与商品的关联")
    @DeleteMapping("/{productId}")
    public R<Void> unbindProduct(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long supplierId,
            @PathVariable Long productId) {
        productSupplierService.unbindProduct(loginUser.getEnterpriseId(), supplierId, productId);
        return R.ok();
    }

    @Operation(summary = "更新供货价")
    @PutMapping("/{productId}/price")
    public R<Void> updateSupplyPrice(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long supplierId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateSupplyPriceRequest request) {
        productSupplierService.updateSupplyPrice(
                loginUser.getEnterpriseId(), supplierId, productId, request.getSupplyPrice());
        return R.ok();
    }
}
