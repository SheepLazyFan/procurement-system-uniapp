package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 采购订单主表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_purchase_order")
public class OmsPurchaseOrder extends BaseEntity {

    /** 订单编号（PO+年月日+4位序号） */
    private String orderNo;

    /** 所属企业 */
    private Long enterpriseId;

    /** 供应商 */
    private Long supplierId;

    /** 采购总金额 */
    private BigDecimal totalAmount;

    /** 采购状态：PENDING/PURCHASING/ARRIVED/COMPLETED/CANCELLED */
    private String status;

    /** 备注 */
    private String remark;
}
