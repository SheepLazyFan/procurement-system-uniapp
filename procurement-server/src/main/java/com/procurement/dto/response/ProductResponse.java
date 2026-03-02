package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品响应 DTO
 */
@Data
public class ProductResponse implements Serializable {
    private Long id;
    private String name;
    private String spec;
    private String unit;
    private BigDecimal price;
    private BigDecimal costPrice;
    private Integer stock;
    private Integer stockWarning;
    private List<String> images;
    private Long categoryId;
    private String categoryName;
    private Integer status;
}
