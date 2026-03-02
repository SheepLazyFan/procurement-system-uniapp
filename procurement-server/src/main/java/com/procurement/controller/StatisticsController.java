package com.procurement.controller;

import com.procurement.common.result.R;
import com.procurement.dto.response.SalesRankingResponse;
import com.procurement.dto.response.SalesTrendResponse;
import com.procurement.dto.response.StatOverviewResponse;
import com.procurement.security.LoginUser;
import com.procurement.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统计报表控制器
 */
@Tag(name = "统计报表")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "经营数据概览")
    @GetMapping("/overview")
    public R<StatOverviewResponse> overview(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(statisticsService.getOverview(loginUser.getEnterpriseId()));
    }

    @Operation(summary = "销售趋势")
    @GetMapping("/sales-trend")
    public R<List<SalesTrendResponse>> salesTrend(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return R.ok(statisticsService.getSalesTrend(
                loginUser.getEnterpriseId(), period, startDate, endDate));
    }

    @Operation(summary = "利润趋势")
    @GetMapping("/profit-trend")
    public R<List<Map<String, Object>>> profitTrend(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return R.ok(statisticsService.getProfitTrend(
                loginUser.getEnterpriseId(), period, startDate, endDate));
    }

    @Operation(summary = "库存统计")
    @GetMapping("/inventory")
    public R<Map<String, Object>> inventory(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(statisticsService.getInventoryStats(loginUser.getEnterpriseId()));
    }

    @Operation(summary = "商品销售排行")
    @GetMapping("/sales-ranking/products")
    public R<List<SalesRankingResponse>> productRanking(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(statisticsService.getProductRanking(
                loginUser.getEnterpriseId(), period, limit));
    }

    @Operation(summary = "客户销售排行")
    @GetMapping("/sales-ranking/customers")
    public R<List<Map<String, Object>>> customerRanking(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(statisticsService.getCustomerRanking(
                loginUser.getEnterpriseId(), period, limit));
    }
}
