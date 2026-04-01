package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.request.WxLoginRequest;
import com.procurement.dto.response.LoginResponse;
import com.procurement.entity.SysTeamMember;
import com.procurement.entity.SysUser;
import com.procurement.mapper.TeamMemberMapper;
import com.procurement.security.LoginUser;
import com.procurement.service.AuthService;
import com.procurement.service.EnterpriseService;
import com.procurement.service.FileService;
import com.procurement.common.constant.UserConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证控制器 — 微信授权登录
 */
@Tag(name = "认证模块")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EnterpriseService enterpriseService;
    private final TeamMemberMapper teamMemberMapper;
    private final FileService fileService;

    @Operation(summary = "微信授权登录")
    @PostMapping("/wx-login")
    public R<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return R.ok(authService.wxLogin(request));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/profile")
    public R<Map<String, Object>> getProfile(@AuthenticationPrincipal LoginUser loginUser) {
        SysUser user = loginUser.getUser();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("phone", user.getPhone());
        data.put("role", user.getRole());
        data.put("nickName", user.getNickName());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("enterpriseId", user.getEnterpriseId());
        data.put("wxOpenid", user.getWxOpenid());

        // 附带团队角色
        if (UserConstants.ROLE_MEMBER.equals(user.getRole()) && user.getEnterpriseId() != null) {
            SysTeamMember member = teamMemberMapper.selectOne(
                    new LambdaQueryWrapper<SysTeamMember>()
                            .eq(SysTeamMember::getUserId, user.getId())
                            .eq(SysTeamMember::getEnterpriseId, user.getEnterpriseId()));
            if (member != null) {
                data.put("memberRole", member.getRole());
            }
        }

        // 附带企业信息
        if (user.getEnterpriseId() != null) {
            var enterprise = enterpriseService.getByUser(user.getId());
            data.put("enterprise", enterprise);
            if (enterprise != null) {
                data.put("enterpriseName", enterprise.getName());
            }
        }

        return R.ok(data);
    }

    @Operation(summary = "上传并更新用户头像")
    @PostMapping("/avatar")
    public R<Map<String, Object>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal LoginUser loginUser) {
        Map<String, String> uploadResult = fileService.upload(file, "avatar");
        String avatarUrl = uploadResult.get("url");
        authService.updateAvatar(loginUser.getUser().getId(), avatarUrl);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("avatarUrl", avatarUrl);
        return R.ok(data);
    }

    @Operation(summary = "修改用户昵称")
    @PutMapping("/nickname")
    public R<Void> updateNickName(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal LoginUser loginUser) {
        String nickName = body.get("nickName");
        if (nickName == null || nickName.isBlank()) {
            return R.fail("昵称不能为空");
        }
        if (nickName.length() > 20) {
            return R.fail("昵称不能超过20个字符");
        }
        authService.updateNickName(loginUser.getUser().getId(), nickName.trim());
        return R.ok();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization);
        return R.ok();
    }
}
