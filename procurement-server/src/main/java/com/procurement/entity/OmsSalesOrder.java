package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 销售订单主表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_sales_order")
public class OmsSalesOrder extends BaseEntity {

    /** 订单编号（SO+年月日+4位序号） */
    private String orderNo;

    /** 所属企业 */
    private Long enterpriseId;

    /** 客户ID */
    private Long customerId;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 订单总成本 */
    private BigDecimal totalCost;

    /** 订单毛利润 */
    private BigDecimal totalProfit;

    /** 订单状态：PENDING/CONFIRMED/SHIPPED/COMPLETED/CANCELLED */
    private String status;

    /** 支付状态：UNPAID/PAID */
    private String paymentStatus;

    /** 订单备注 */
    private String remark;
}
