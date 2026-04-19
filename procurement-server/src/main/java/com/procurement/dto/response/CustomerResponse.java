package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 客户响应 DTO
 */
@Data
public class CustomerResponse implements Serializable {
    private Long id;
    private String name;
    private String phone;
    private String address;
    private String remark;

    /** 订单数量统计 */
    private Integer orderCount;

    /** 累计消费金额 */
    private BigDecimal totalAmount;

    /** 最近订单记录 */
    private List<RecentOrder> recentOrders;

    @Data
    public static class RecentOrder implements Serializable {
        private Long id;
        private String orderNo;
        private BigDecimal totalAmount;
        private String status;
        private String createdAt;
        /** 付款状态：UNPAID/CLAIMED/PAID */
        private String paymentStatus;
    }
}
