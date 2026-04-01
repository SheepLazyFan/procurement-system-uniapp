package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 供应商响应 DTO
 */
@Data
public class SupplierResponse implements Serializable {
    private Long id;
    private String name;
    private String phone;
    private String address;
    private String mainCategory;
    private String remark;

    /** 采购次数统计 */
    private Integer purchaseCount;

    /** 累计采购金额 */
    private BigDecimal totalAmount;

    /** 最近采购记录 */
    private List<RecentOrder> recentOrders;

    @Data
    public static class RecentOrder implements Serializable {
        private Long id;
        private String orderNo;
        private BigDecimal totalAmount;
        private String status;
        private String createdAt;
        /** 商品摘要，如"铅笔×100、橡皮×50 等3种" */
        private String itemSummary;
    }
}
