package com.procurement.service;

import com.procurement.dto.response.DegradableResponse;
import com.procurement.dto.response.SalesRankingResponse;
import com.procurement.dto.response.SalesTrendResponse;
import com.procurement.dto.response.StatOverviewResponse;
import com.procurement.mapper.*;
import com.procurement.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatisticsServiceImpl - \u7edf\u8ba1\u670d\u52a1")
class StatisticsServiceImplTest {

    @Mock private SalesOrderMapper salesOrderMapper;
    @Mock private SalesOrderItemMapper salesOrderItemMapper;
    @Mock private PurchaseOrderMapper purchaseOrderMapper;
    @Mock private ProductMapper productMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    // =================== getOverview ===================

    @Test
    @DisplayName("Should mark overview degraded when part of statistics loading fails")
    void should_markOverviewDegraded_when_partialQueryFails() {
        when(salesOrderMapper.selectSalesOverview(any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("db error"));
        when(productMapper.selectList(any())).thenReturn(List.of());
        when(salesOrderMapper.selectCount(any())).thenReturn(0L);

        StatOverviewResponse response = statisticsService.getOverview(1L);

        assertThat(response.getDegraded()).isTrue();
        assertThat(response.getWarnings()).isNotEmpty();
        assertThat(response.getTodaySales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getMonthSales()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return non-degraded overview when all queries succeed")
    void should_returnNonDegraded_when_allQueriesSucceed() {
        when(salesOrderMapper.selectSalesOverview(any(), anyString(), anyString()))
                .thenReturn(Map.of("totalSales", new BigDecimal("100"), "totalProfit", new BigDecimal("30")));
        when(productMapper.selectList(any())).thenReturn(List.of());
        when(salesOrderMapper.selectCount(any())).thenReturn(5L);

        StatOverviewResponse response = statisticsService.getOverview(1L);

        assertThat(response.getDegraded()).isFalse();
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getPendingOrderCount()).isEqualTo(5);
    }

    // =================== getSalesTrend ===================

    @Test
    @DisplayName("Should degrade sales trend when SQL fails")
    void should_degradeSalesTrend_when_sqlFails() {
        when(salesOrderMapper.selectSalesTrend(anyLong(), anyString(), anyString()))
                .thenThrow(new RuntimeException("db error"));

        DegradableResponse<List<SalesTrendResponse>> response =
                statisticsService.getSalesTrend(1L, "week", null, null);

        assertThat(response.getDegraded()).isTrue();
        assertThat(response.getWarnings()).contains("\u9500\u552e\u8d8b\u52bf\u52a0\u8f7d\u5931\u8d25");
        assertThat(response.getData()).isEmpty();
    }

    @Test
    @DisplayName("Should return normal sales trend with date gap filling")
    void should_returnSalesTrend_with_dateGapFilling() {
        when(salesOrderMapper.selectSalesTrend(anyLong(), anyString(), anyString()))
                .thenReturn(List.of());

        DegradableResponse<List<SalesTrendResponse>> response =
                statisticsService.getSalesTrend(1L, "week", null, null);

        assertThat(response.getDegraded()).isFalse();
        assertThat(response.getData()).isNotEmpty();
        // week period should generate ~8 days (today minus 7 days, inclusive)
        assertThat(response.getData().size()).isGreaterThanOrEqualTo(7);
    }

    // =================== getProfitTrend ===================

    @Test
    @DisplayName("Should degrade profit trend when SQL fails")
    void should_degradeProfitTrend_when_sqlFails() {
        when(salesOrderMapper.selectSalesTrend(anyLong(), anyString(), anyString()))
                .thenThrow(new RuntimeException("db error"));

        DegradableResponse<List<Map<String, Object>>> response =
                statisticsService.getProfitTrend(1L, "day", null, null);

        assertThat(response.getDegraded()).isTrue();
        assertThat(response.getWarnings()).contains("\u5229\u6da6\u8d8b\u52bf\u52a0\u8f7d\u5931\u8d25");
        assertThat(response.getData()).isEmpty();
    }

    // =================== getInventoryStats ===================

    @Test
    @DisplayName("Should degrade inventory stats when SQL fails")
    void should_degradeInventoryStats_when_sqlFails() {
        when(productMapper.selectList(any())).thenThrow(new RuntimeException("db error"));

        DegradableResponse<Map<String, Object>> response =
                statisticsService.getInventoryStats(1L);

        assertThat(response.getDegraded()).isTrue();
        assertThat(response.getWarnings()).contains("\u5e93\u5b58\u7edf\u8ba1\u52a0\u8f7d\u5931\u8d25");
        assertThat(response.getData().get("totalProducts")).isEqualTo(0);
    }

    // Product/Customer ranking tests removed: the underlying implementation was
    // refactored to use RankingAggregateScheduler + StatDaily*Sales tables.
    // These mapper methods (selectProductRanking/selectCustomerRanking) no longer exist.
}
