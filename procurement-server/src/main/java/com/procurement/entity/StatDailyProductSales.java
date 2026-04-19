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
 * 商品日销售摘要（预聚合表）
 */
@Data
@TableName("stat_daily_product_sales")
public class StatDailyProductSales implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long enterpriseId;

    private LocalDate statDate;

    private Long productId;

    private String productName;

    private Integer sumQuantity;

    private BigDecimal sumAmount;

    private BigDecimal sumProfit;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
