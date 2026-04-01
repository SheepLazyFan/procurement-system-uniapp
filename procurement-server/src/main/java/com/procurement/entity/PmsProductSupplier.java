package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.procurement.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品-供应商关联表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_product_supplier")
public class PmsProductSupplier extends BaseEntity {

    private Long productId;

    private Long supplierId;

    /** 供货价 */
    private BigDecimal supplyPrice;

    /** 是否默认供应商 */
    private Integer isDefault;

    private Long enterpriseId;
}
