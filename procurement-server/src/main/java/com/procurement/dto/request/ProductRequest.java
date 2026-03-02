package com.procurement.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品创建/更新请求 DTO
 */
@Data
public class ProductRequest implements Serializable {

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称最长100字符")
    private String name;

    @Size(max = 200, message = "规格最长200字符")
    private String spec;

    @NotBlank(message = "单位不能为空")
    @Size(max = 20, message = "单位最长20字符")
    private String unit;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.00", message = "售价不能为负数")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "成本价不能为负数")
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Min(value = 0, message = "库存不能为负数")
    private Integer stock = 0;

    @Min(value = 0, message = "库存预警阈值不能为负数")
    private Integer stockWarning = 0;

    /** 商品图片 URL 列表 */
    private List<String> images;
}
