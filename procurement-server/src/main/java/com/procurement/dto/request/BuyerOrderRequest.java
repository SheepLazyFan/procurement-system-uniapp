package com.procurement.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 买家提交订单请求 DTO
 */
@Data
public class BuyerOrderRequest implements Serializable {

    @NotNull(message = "企业ID不能为空")
    private Long enterpriseId;

    @NotEmpty(message = "订单商品不能为空")
    @Valid
    private List<BuyerItemRequest> items;

    @Size(max = 300, message = "地址最长300字符")
    private String address;

    @Size(max = 500, message = "备注最长500字符")
    private String remark;

    @Data
    public static class BuyerItemRequest implements Serializable {
        @NotNull(message = "商品ID不能为空")
        private Long productId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;
    }
}
