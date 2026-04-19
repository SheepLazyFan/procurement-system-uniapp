package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

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

    /** 收款二维码图片 URL（买家扫码付款用） */
    private String paymentQrUrl;

    /** 企业会话失效时间，恢复等高风险操作后用于让商家和员工重新登录 */
    private LocalDateTime sessionInvalidAfter;
}
