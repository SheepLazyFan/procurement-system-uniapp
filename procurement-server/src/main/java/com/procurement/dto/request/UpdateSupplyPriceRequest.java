package com.procurement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 更新供货价请求 DTO
 */
@Data
public class UpdateSupplyPriceRequest implements Serializable {

    @NotNull(message = "供货价不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "供货价不能为负数")
    private BigDecimal supplyPrice;
}
