package com.procurement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户日销售摘要（预聚合表）
 */
@Data
@TableName("stat_daily_customer_sales")
public class StatDailyCustomerSales implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long enterpriseId;

    private LocalDate statDate;

    private Long customerId;

    private String customerName;

    private Integer orderCount;

    private BigDecimal sumAmount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
