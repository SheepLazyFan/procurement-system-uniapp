package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购订单响应 DTO
 */
@Data
public class PurchaseOrderResponse implements Serializable {
    private Long id;
    private String orderNo;
    private String status;
    private BigDecimal totalAmount;
    private String remark;
    private LocalDateTime createdAt;

    /** 供应商信息 */
    private SupplierInfo supplier;

    /** 订单商品明细 */
    private List<OrderItemInfo> items;

    @Data
    public static class SupplierInfo implements Serializable {
        private Long id;
        private String name;
        private String phone;
    }

    @Data
    public static class OrderItemInfo implements Serializable {
        private Long id;
        private Long productId;
        private String productName;
        private String spec;
        private String unit;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal amount;
    }
}
