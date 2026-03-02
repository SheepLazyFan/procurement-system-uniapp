package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 销售趋势响应 DTO
 */
@Data
public class SalesTrendResponse implements Serializable {
    private String date;
    private BigDecimal amount;
    private BigDecimal profit;
    private Integer orderCount;
}
