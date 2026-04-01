package com.procurement.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 采购订单创建请求 DTO
 */
@Data
public class PurchaseOrderRequest implements Serializable {

    /** 供应商ID */
    private Long supplierId;

    @NotEmpty(message = "采购商品不能为空")
    @Valid
    private List<PurchaseItemRequest> items;

    @Size(max = 500, message = "备注最长500字符")
    private String remark;

    /**
     * 采购商品明细项
     */
    @Data
    public static class PurchaseItemRequest implements Serializable {

        @NotNull(message = "商品ID不能为空")
        private Long productId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;

        @NotNull(message = "采购单价不能为空")
        @DecimalMin(value = "0.01", message = "采购单价必须大于0")
        private BigDecimal price;
    }
}
