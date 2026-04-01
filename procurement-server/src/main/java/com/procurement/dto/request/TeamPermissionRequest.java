package com.procurement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 团队成员权限配置请求 DTO
 */
@Data
public class TeamPermissionRequest implements Serializable {

    /** 成员角色：ADMIN / SALES / WAREHOUSE */
    @NotNull(message = "角色不能为空")
    private String role;

    /** 权限配置（可选，预留字段） */
    private Map<String, Boolean> permissions;
}
