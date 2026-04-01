package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.request.EnterpriseRequest;
import com.procurement.dto.response.EnterpriseResponse;
import com.procurement.security.LoginUser;
import com.procurement.service.EnterpriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 企业管理控制器
 */
@Tag(name = "企业管理")
@RestController
@RequestMapping("/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @Operation(summary = "创建企业")
    @PostMapping
    public R<EnterpriseResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                         @Valid @RequestBody EnterpriseRequest request) {
        return R.ok(enterpriseService.create(loginUser.getUserId(), request));
    }

    @Operation(summary = "获取当前企业信息")
    @GetMapping
    public R<EnterpriseResponse> get(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(enterpriseService.getByUser(loginUser.getUserId()));
    }

    @Operation(summary = "更新企业信息")
    @PutMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public R<EnterpriseResponse> update(@AuthenticationPrincipal LoginUser loginUser,
                                         @Valid @RequestBody EnterpriseRequest request) {
        return R.ok(enterpriseService.update(loginUser.getEnterpriseId(), request));
    }

    @Operation(summary = "刷新邀请码")
    @PutMapping("/invite-code/refresh")
    @PreAuthorize("hasRole('SELLER')")
    public R<Map<String, String>> refreshInviteCode(@AuthenticationPrincipal LoginUser loginUser) {
        String newCode = enterpriseService.refreshInviteCode(loginUser.getEnterpriseId());
        return R.ok(Map.of("inviteCode", newCode));
    }
}
