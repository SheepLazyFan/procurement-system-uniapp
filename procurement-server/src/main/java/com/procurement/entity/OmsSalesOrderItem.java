package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 销售订单明细表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_sales_order_item")
public class OmsSalesOrderItem extends BaseEntity {

    /** 所属订单 */
    private Long orderId;

    /** 关联商品 */
    private Long productId;

    /** 商品名称（快照） */
    private String productName;

    /** 规格（快照） */
    private String spec;

    /** 单位（快照） */
    private String unit;

    /** 购买数量 */
    private Integer quantity;

    /** 单价（快照） */
    private BigDecimal price;

    /** 成本价（快照） */
    private BigDecimal costPrice;

    /** 小计金额 */
    private BigDecimal amount;

    /** 小计利润 */
    private BigDecimal profit;
}
