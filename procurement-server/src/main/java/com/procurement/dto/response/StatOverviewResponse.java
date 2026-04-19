package com.procurement.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 经营概览统计响应 DTO
 */
@Data
public class StatOverviewResponse implements Serializable {

    /** 今日销售额 */
    private BigDecimal todaySales;

    /** 本月销售额 */
    private BigDecimal monthSales;

    /** 今日利润 */
    private BigDecimal todayProfit;

    /** 本月利润 */
    private BigDecimal monthProfit;

    /** 库存总值 */
    private BigDecimal inventoryValue;

    /** 库存商品总数 */
    private Integer inventoryCount;

    /** 待处理订单数 */
    private Integer pendingOrderCount;

    /** 库存预警商品数 */
    private Integer stockWarningCount;

    /** 是否存在降级数据 */
    private Boolean degraded;

    /** 降级告警列表 */
    private List<String> warnings;
}
