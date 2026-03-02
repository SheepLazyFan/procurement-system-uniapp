package com.procurement.service;

import com.procurement.dto.response.SalesRankingResponse;
import com.procurement.dto.response.SalesTrendResponse;
import com.procurement.dto.response.StatOverviewResponse;

import java.util.List;
import java.util.Map;

/**
 * 统计报表服务接口
 */
public interface StatisticsService {

    /**
     * 经营数据概览
     */
    StatOverviewResponse getOverview(Long enterpriseId);

    /**
     * 销售趋势
     */
    List<SalesTrendResponse> getSalesTrend(Long enterpriseId, String period,
                                           String startDate, String endDate);

    /**
     * 利润趋势
     */
    List<Map<String, Object>> getProfitTrend(Long enterpriseId, String period,
                                             String startDate, String endDate);

    /**
     * 库存统计
     */
    Map<String, Object> getInventoryStats(Long enterpriseId);

    /**
     * 商品销售排行
     */
    List<SalesRankingResponse> getProductRanking(Long enterpriseId, String period, Integer limit);

    /**
     * 客户销售排行
     */
    List<Map<String, Object>> getCustomerRanking(Long enterpriseId, String period, Integer limit);
}
