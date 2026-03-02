package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.request.SupplierRequest;
import com.procurement.dto.response.PageResponse;
import com.procurement.dto.response.SupplierResponse;
import com.procurement.security.LoginUser;
import com.procurement.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 供应商管理控制器
 */
@Tag(name = "供应商管理")
@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "供应商列表（分页）")
    @GetMapping
    public R<PageResponse<SupplierResponse>> list(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(supplierService.listByPage(loginUser.getEnterpriseId(),
                pageNum, pageSize, keyword));
    }

    @Operation(summary = "供应商详情")
    @GetMapping("/{id}")
    public R<SupplierResponse> getById(@AuthenticationPrincipal LoginUser loginUser,
                                        @PathVariable Long id) {
        return R.ok(supplierService.getById(loginUser.getEnterpriseId(), id));
    }

    @Operation(summary = "添加供应商")
    @PostMapping
    public R<SupplierResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                       @Valid @RequestBody SupplierRequest request) {
        return R.ok(supplierService.create(loginUser.getEnterpriseId(), request));
    }

    @Operation(summary = "更新供应商")
    @PutMapping("/{id}")
    public R<SupplierResponse> update(@AuthenticationPrincipal LoginUser loginUser,
                                       @PathVariable Long id,
                                       @Valid @RequestBody SupplierRequest request) {
        return R.ok(supplierService.update(loginUser.getEnterpriseId(), id, request));
    }

    @Operation(summary = "删除供应商")
    @DeleteMapping("/{id}")
    public R<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                           @PathVariable Long id) {
        supplierService.delete(loginUser.getEnterpriseId(), id);
        return R.ok();
    }
}
