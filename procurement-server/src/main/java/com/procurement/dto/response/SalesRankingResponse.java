package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 销售排行响应 DTO
 */
@Data
public class SalesRankingResponse implements Serializable {
    private String productName;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private BigDecimal totalProfit;
}
