package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.request.TeamPermissionRequest;
import com.procurement.security.LoginUser;
import com.procurement.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 团队管理控制器
 */
@Tag(name = "团队管理")
@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "团队成员列表")
    @GetMapping("/members")
    public R<List<Map<String, Object>>> listMembers(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(teamService.listMembers(loginUser.getEnterpriseId()));
    }

    @Operation(summary = "通过邀请码加入团队")
    @PostMapping("/join")
    public R<Map<String, Object>> join(@AuthenticationPrincipal LoginUser loginUser,
                                       @RequestBody Map<String, String> body) {
        String inviteCode = body.get("inviteCode");
        return R.ok(teamService.joinByInviteCode(loginUser.getUserId(), inviteCode));
    }

    @Operation(summary = "设置成员权限")
    @PutMapping("/members/{id}/permissions")
    public R<Void> setPermissions(@AuthenticationPrincipal LoginUser loginUser,
                                   @PathVariable Long id,
                                   @Valid @RequestBody TeamPermissionRequest request) {
        teamService.setPermissions(loginUser.getEnterpriseId(), id, request);
        return R.ok();
    }

    @Operation(summary = "移除成员")
    @DeleteMapping("/members/{id}")
    public R<Void> removeMember(@AuthenticationPrincipal LoginUser loginUser,
                                 @PathVariable Long id) {
        teamService.removeMember(loginUser.getEnterpriseId(), id);
        return R.ok();
    }
}
