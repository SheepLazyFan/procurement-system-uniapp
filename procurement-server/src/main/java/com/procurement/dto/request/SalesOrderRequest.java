package com.procurement.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 销售订单创建请求 DTO（商家开单）
 */
@Data
public class SalesOrderRequest implements Serializable {

    /** 客户ID（线下开单可为空） */
    private Long customerId;

    @NotEmpty(message = "订单商品不能为空")
    @Valid
    private List<OrderItemRequest> items;

    @Size(max = 500, message = "备注最长500字符")
    private String remark;

    /**
     * 订单商品明细项
     */
    @Data
    public static class OrderItemRequest implements Serializable {

        @NotNull(message = "商品ID不能为空")
        private Long productId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;
    }
}
