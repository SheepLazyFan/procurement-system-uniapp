package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 供应商表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_supplier")
public class CrmSupplier extends BaseEntity {

    /** 所属企业 */
    private Long enterpriseId;

    /** 供应商名称 */
    private String name;

    /** 联系电话 */
    private String phone;

    /** 供应商地址 */
    private String address;

    /** 主营品类 */
    private String mainCategory;

    /** 备注 */
    private String remark;
}
