package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_customer")
public class CrmCustomer extends BaseEntity {

    /** 所属企业 */
    private Long enterpriseId;

    /** 客户名称 */
    private String name;

    /** 联系电话 */
    private String phone;

    /** 收货地址 */
    private String address;

    /** 微信 OpenID */
    private String wxOpenid;

    /** 备注 */
    private String remark;
}
