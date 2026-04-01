package com.procurement.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 批量绑定商品到供应商请求 DTO
 */
@Data
public class BindProductsRequest implements Serializable {

    @NotEmpty(message = "商品列表不能为空")
    @Valid
    private List<BindItem> items;

    @Data
    public static class BindItem implements Serializable {

        @NotNull(message = "商品ID不能为空")
        private Long productId;

        @NotNull(message = "供货价不能为空")
        @DecimalMin(value = "0.00", inclusive = true, message = "供货价不能为负数")
        private BigDecimal supplyPrice;
    }
}
