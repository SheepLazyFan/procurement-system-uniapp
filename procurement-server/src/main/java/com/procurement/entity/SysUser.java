package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 手机号（商家登录凭证） */
    private String phone;

    /** 密码哈希（BCrypt，预留字段） */
    private String passwordHash;

    /** 用户角色：SELLER / MEMBER / BUYER */
    private String role;

    /** 所属企业ID */
    private Long enterpriseId;

    /** 微信 OpenID */
    private String wxOpenid;

    /** 微信 UnionID */
    private String wxUnionId;

    /** 昵称 */
    private String nickName;

    /** 头像 URL */
    private String avatarUrl;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    /**
     * 库存预警订阅通知开关：1=已开启，0=已关闭。
     * 默认为 0，用户在小程序中授权后由后端置 1；
     * 关闭时置 0，Scheduler 推送前检查此标志。
     */
    private Integer notifyStockWarning;
}
