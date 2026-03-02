package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 采购订单明细表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_purchase_order_item")
public class OmsPurchaseOrderItem extends BaseEntity {

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

    /** 采购数量 */
    private Integer quantity;

    /** 采购单价 */
    private BigDecimal price;

    /** 小计金额 */
    private BigDecimal amount;
}
