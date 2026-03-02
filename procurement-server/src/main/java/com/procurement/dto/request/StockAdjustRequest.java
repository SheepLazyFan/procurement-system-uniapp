package com.procurement.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 库存调整请求 DTO
 */
@Data
public class StockAdjustRequest implements Serializable {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "调整数量不能为空")
    @Min(value = 1, message = "调整数量至少为1")
    private Integer quantity;

    /** 调整类型：IN(入库) / OUT(出库) */
    @NotNull(message = "调整类型不能为空")
    @Pattern(regexp = "^(IN|OUT)$", message = "调整类型只能是 IN 或 OUT")
    private String type;
}
