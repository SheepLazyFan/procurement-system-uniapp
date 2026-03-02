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

    @NotNull(message = "权限配置不能为空")
    private Map<String, Boolean> permissions;
}
