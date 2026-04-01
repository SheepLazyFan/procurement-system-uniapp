package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售订单响应 DTO
 */
@Data
public class SalesOrderResponse implements Serializable {
    private Long id;
    private Long enterpriseId;
    private String orderNo;
    private String status;
    private String paymentStatus;
    private BigDecimal totalAmount;
    private BigDecimal totalCost;
    private BigDecimal totalProfit;
    private String deliveryAddress;
    private String remark;
    private String orderSource;
    private String cancelBy;
    private LocalDateTime createdAt;

    /** 客户信息 */
    private CustomerInfo customer;

    /** 订单商品明细 */
    private List<OrderItemInfo> items;

    @Data
    public static class CustomerInfo implements Serializable {
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
        private BigDecimal costPrice;
        private BigDecimal amount;
        private BigDecimal profit;
    }
}
