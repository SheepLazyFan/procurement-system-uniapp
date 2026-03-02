package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.request.CustomerRequest;
import com.procurement.dto.response.CustomerResponse;
import com.procurement.dto.response.PageResponse;
import com.procurement.security.LoginUser;
import com.procurement.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 客户管理控制器
 */
@Tag(name = "客户管理")
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "客户列表（分页）")
    @GetMapping
    public R<PageResponse<CustomerResponse>> list(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(customerService.listByPage(loginUser.getEnterpriseId(),
                pageNum, pageSize, keyword));
    }

    @Operation(summary = "客户详情")
    @GetMapping("/{id}")
    public R<CustomerResponse> getById(@AuthenticationPrincipal LoginUser loginUser,
                                        @PathVariable Long id) {
        return R.ok(customerService.getById(loginUser.getEnterpriseId(), id));
    }

    @Operation(summary = "添加客户")
    @PostMapping
    public R<CustomerResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                       @Valid @RequestBody CustomerRequest request) {
        return R.ok(customerService.create(loginUser.getEnterpriseId(), request));
    }

    @Operation(summary = "更新客户")
    @PutMapping("/{id}")
    public R<CustomerResponse> update(@AuthenticationPrincipal LoginUser loginUser,
                                       @PathVariable Long id,
                                       @Valid @RequestBody CustomerRequest request) {
        return R.ok(customerService.update(loginUser.getEnterpriseId(), id, request));
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                           @PathVariable Long id) {
        customerService.delete(loginUser.getEnterpriseId(), id);
        return R.ok();
    }
}
