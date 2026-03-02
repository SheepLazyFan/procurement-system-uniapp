package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 供应商响应 DTO
 */
@Data
public class SupplierResponse implements Serializable {
    private Long id;
    private String name;
    private String phone;
    private String address;
    private String mainCategory;
    private String remark;

    /** 采购次数统计 */
    private Integer purchaseCount;

    /** 累计采购金额 */
    private BigDecimal totalAmount;
}
