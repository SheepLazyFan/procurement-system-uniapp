package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企业表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise")
public class SysEnterprise extends BaseEntity {

    /** 企业名称 */
    private String name;

    /** 企业地址 */
    private String address;

    /** 联系电话 */
    private String contactPhone;

    /** 联系人 */
    private String contactName;

    /** 企业主用户ID */
    private Long ownerId;

    /** 团队邀请码 */
    private String inviteCode;

    /** 企业 Logo URL */
    private String logoUrl;
}
