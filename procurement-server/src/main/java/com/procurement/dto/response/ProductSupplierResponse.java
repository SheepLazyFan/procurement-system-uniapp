package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 供应商关联商品响应 DTO
 */
@Data
public class ProductSupplierResponse implements Serializable {
    private Long productId;
    private String productName;
    private String spec;
    private String unit;
    private Long categoryId;
    private String categoryName;
    private BigDecimal supplyPrice;
    private Integer stock;
    /** 该商品绑定的供应商总数（含本条），用于解绑前的友好提示 */
    private Integer supplierCount;
}
