package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 团队成员表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_team_member", autoResultMap = true)
public class SysTeamMember extends BaseEntity {

    /** 所属企业 */
    private Long enterpriseId;

    /** 用户ID */
    private Long userId;

    /** 成员角色 */
    private String role;

    /** 权限配置 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Boolean> permissions;
}
