package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

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
}
