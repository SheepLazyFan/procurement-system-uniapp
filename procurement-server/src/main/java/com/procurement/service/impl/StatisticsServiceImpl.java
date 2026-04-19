package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.procurement.dto.response.DegradableResponse;
import com.procurement.dto.response.SalesRankingResponse;
import com.procurement.dto.response.SalesTrendResponse;
import com.procurement.dto.response.StatOverviewResponse;
import com.procurement.entity.*;
import com.procurement.mapper.*;
import com.procurement.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统计报表服务实现（使用 SQL 聚合替代内存计算，消除 N+1）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final ProductMapper productMapper;
    private final CustomerMapper customerMapper;
    private final CategoryMapper categoryMapper;
    private final StatDailyProductSalesMapper statProductMapper;
    private final StatDailyCustomerSalesMapper statCustomerMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public StatOverviewResponse getOverview(Long enterpriseId) {
        StatOverviewResponse resp = new StatOverviewResponse();
        resp.setTodaySales(BigDecimal.ZERO);
        resp.setTodayProfit(BigDecimal.ZERO);
        resp.setMonthSales(BigDecimal.ZERO);
        resp.setMonthProfit(BigDecimal.ZERO);
        resp.setInventoryValue(BigDecimal.ZERO);
        resp.setInventoryCount(0);
        resp.setPendingOrderCount(0);
        resp.setStockWarningCount(0);
        resp.setDegraded(false);
        List<String> warnings = new ArrayList<>();
        resp.setWarnings(warnings);

        LocalDate today = LocalDate.now();
        String todayStart = today + " 00:00:00";
        String todayEnd = today + " 23:59:59";
        String monthStart = today.withDayOfMonth(1) + " 00:00:00";

        try {
            Map<String, Object> todayStats = salesOrderMapper.selectSalesOverview(enterpriseId, todayStart, todayEnd);
            if (todayStats != null) {
                resp.setTodaySales(toBigDecimal(todayStats.get("totalSales")));
                resp.setTodayProfit(toBigDecimal(todayStats.get("totalProfit")));
            }
        } catch (Exception e) {
            markOverviewDegraded(warnings, enterpriseId, "todaySales", "今日销售概览加载失败", e);
        }

        try {
            Map<String, Object> monthStats = salesOrderMapper.selectSalesOverview(enterpriseId, monthStart, todayEnd);
            if (monthStats != null) {
                resp.setMonthSales(toBigDecimal(monthStats.get("totalSales")));
                resp.setMonthProfit(toBigDecimal(monthStats.get("totalProfit")));
            }
        } catch (Exception e) {
            markOverviewDegraded(warnings, enterpriseId, "monthSales", "本月销售概览加载失败", e);
        }

        List<PmsProduct> products = List.of();
        try {
            products = productMapper.selectList(
                    new LambdaQueryWrapper<PmsProduct>()
                            .eq(PmsProduct::getEnterpriseId, enterpriseId));
            if (products == null) products = List.of();

            BigDecimal inventoryValue = BigDecimal.ZERO;
            int totalStock = 0;
            for (PmsProduct p : products) {
                int st = p.getStock() != null ? p.getStock() : 0;
                totalStock += st;
                BigDecimal cp = p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO;
                inventoryValue = inventoryValue.add(cp.multiply(BigDecimal.valueOf(st)));
            }
            resp.setInventoryValue(inventoryValue);
            resp.setInventoryCount(totalStock);
        } catch (Exception e) {
            markOverviewDegraded(warnings, enterpriseId, "inventory", "库存总览加载失败", e);
        }

        try {
            Long pendingCount = salesOrderMapper.selectCount(
                    new LambdaQueryWrapper<OmsSalesOrder>()
                            .eq(OmsSalesOrder::getEnterpriseId, enterpriseId)
                            .eq(OmsSalesOrder::getStatus, "PENDING"));
            resp.setPendingOrderCount(pendingCount != null ? pendingCount.intValue() : 0);
        } catch (Exception e) {
            markOverviewDegraded(warnings, enterpriseId, "pendingOrders", "待处理订单统计加载失败", e);
        }

        try {
            int warningCount = 0;
            for (PmsProduct p : products) {
                if (p.getStockWarning() != null && p.getStock() != null && p.getStock() <= p.getStockWarning()) {
                    warningCount++;
                }
            }
            resp.setStockWarningCount(warningCount);
        } catch (Exception e) {
            markOverviewDegraded(warnings, enterpriseId, "stockWarning", "库存预警统计加载失败", e);
        }

        if (!warnings.isEmpty()) {
            resp.setDegraded(true);
        }

        return resp;
    }

    @Override
    public DegradableResponse<List<SalesTrendResponse>> getSalesTrend(Long enterpriseId, String period,
                                                                       String startDate, String endDate) {
        try {
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start;
            if (startDate != null) {
                start = LocalDate.parse(startDate);
            } else {
                start = switch (period) {
                    case "week" -> end.minusWeeks(1);
                    case "month" -> end.minusMonths(1);
                    default -> end.minusDays(7);
                };
            }

            // SQL GROUP BY 聚合
            List<Map<String, Object>> trendData = salesOrderMapper.selectSalesTrend(
                    enterpriseId, start + " 00:00:00", end + " 23:59:59");
            Map<String, Map<String, Object>> dataByDate = trendData.stream()
                    .collect(Collectors.toMap(m -> m.get("date").toString(), Function.identity()));

            // 生成连续日期序列，填充无数据的日期
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            List<SalesTrendResponse> result = new ArrayList<>();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                String dateStr = date.format(fmt);
                Map<String, Object> dayData = dataByDate.get(dateStr);

                SalesTrendResponse trend = new SalesTrendResponse();
                trend.setDate(dateStr);
                if (dayData != null) {
                    trend.setAmount(toBigDecimal(dayData.get("amount")));
                    trend.setProfit(toBigDecimal(dayData.get("profit")));
                    trend.setOrderCount(((Number) dayData.get("orderCount")).intValue());
                } else {
                    trend.setAmount(BigDecimal.ZERO);
                    trend.setProfit(BigDecimal.ZERO);
                    trend.setOrderCount(0);
                }
                result.add(trend);
            }

            return DegradableResponse.ok(result);
        } catch (Exception e) {
            log.error("statistics_degraded enterpriseId={} module=salesTrend message=销售趋势加载失败",
                    enterpriseId, e);
            return DegradableResponse.degraded(List.of(), "销售趋势加载失败");
        }
    }

    @Override
    public DegradableResponse<List<Map<String, Object>>> getProfitTrend(Long enterpriseId, String period,
                                                                         String startDate, String endDate) {
        try {
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(30);

            // SQL GROUP BY 聚合
            List<Map<String, Object>> trendData = salesOrderMapper.selectSalesTrend(
                    enterpriseId, start + " 00:00:00", end + " 23:59:59");
            Map<String, Map<String, Object>> dataByDate = trendData.stream()
                    .collect(Collectors.toMap(m -> m.get("date").toString(), Function.identity()));

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            List<Map<String, Object>> result = new ArrayList<>();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                String dateStr = date.format(fmt);
                Map<String, Object> dayData = dataByDate.get(dateStr);

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("date", dateStr);
                if (dayData != null) {
                    item.put("revenue", toBigDecimal(dayData.get("amount")));
                    item.put("cost", toBigDecimal(dayData.get("cost")));
                    item.put("profit", toBigDecimal(dayData.get("profit")));
                } else {
                    item.put("revenue", BigDecimal.ZERO);
                    item.put("cost", BigDecimal.ZERO);
                    item.put("profit", BigDecimal.ZERO);
                }
                result.add(item);
            }

            return DegradableResponse.ok(result);
        } catch (Exception e) {
            log.error("statistics_degraded enterpriseId={} module=profitTrend message=利润趋势加载失败",
                    enterpriseId, e);
            return DegradableResponse.degraded(List.of(), "利润趋势加载失败");
        }
    }

    @Override
    public DegradableResponse<Map<String, Object>> getInventoryStats(Long enterpriseId) {
        try {
            List<PmsProduct> products = productMapper.selectList(
                    new LambdaQueryWrapper<PmsProduct>()
                            .eq(PmsProduct::getEnterpriseId, enterpriseId));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalProducts", products.size());
            result.put("totalStock", products.stream().mapToInt(p -> p.getStock() != null ? p.getStock() : 0).sum());

            // 批量预加载所有分类（消除 N+1）
            Set<Long> categoryIds = products.stream()
                    .map(PmsProduct::getCategoryId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, PmsCategory> categoryMap = categoryIds.isEmpty()
                    ? Collections.emptyMap()
                    : categoryMapper.selectBatchIds(categoryIds).stream()
                            .collect(Collectors.toMap(PmsCategory::getId, Function.identity()));

            Map<Long, List<PmsProduct>> byCat = products.stream()
                    .filter(p -> p.getCategoryId() != null)   // 过滤 null 防止 groupingBy NPE
                    .collect(Collectors.groupingBy(PmsProduct::getCategoryId));

            List<Map<String, Object>> catStats = new ArrayList<>();
            for (Map.Entry<Long, List<PmsProduct>> entry : byCat.entrySet()) {
                PmsCategory cat = categoryMap.get(entry.getKey());
                Map<String, Object> cs = new LinkedHashMap<>();
                cs.put("categoryName", cat != null ? cat.getName() : "未分类");
                cs.put("productCount", entry.getValue().size());
                cs.put("stockCount", entry.getValue().stream().mapToInt(p -> p.getStock() != null ? p.getStock() : 0).sum());
                catStats.add(cs);
            }
            result.put("categoryStats", catStats);

            long warningCount = products.stream()
                    .filter(p -> p.getStockWarning() != null && p.getStock() != null && p.getStock() <= p.getStockWarning())
                    .count();
            result.put("warningCount", warningCount);

            return DegradableResponse.ok(result);
        } catch (Exception e) {
            log.error("statistics_degraded enterpriseId={} module=inventory message=库存统计加载失败",
                    enterpriseId, e);
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("totalProducts", 0);
            defaults.put("totalStock", 0);
            defaults.put("categoryStats", List.of());
            defaults.put("warningCount", 0);
            return DegradableResponse.degraded(defaults, "库存统计加载失败");
        }
    }

    @Override
    public DegradableResponse<List<SalesRankingResponse>> getProductRanking(Long enterpriseId, String period, Integer limit) {
        try {
            int lim = limit != null ? limit : 10;
            String cacheKey = "rk:" + enterpriseId + ":p:" + period;

            // 1. 查 Redis 缓存
            @SuppressWarnings("unchecked")
            List<SalesRankingResponse> cached = (List<SalesRankingResponse>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return DegradableResponse.ok(cached);
            }

            // 2. 查摘要表
            String[] range = getPeriodDateRange(period);
            List<Map<String, Object>> rows = statProductMapper.selectRanking(
                    enterpriseId, range[0], range[1], lim);

            List<SalesRankingResponse> result = rows.stream().map(row -> {
                SalesRankingResponse r = new SalesRankingResponse();
                r.setProductName((String) row.get("productName"));
                r.setTotalQuantity(((Number) row.get("totalQuantity")).intValue());
                r.setTotalAmount(toBigDecimal(row.get("totalAmount")));
                r.setTotalProfit(toBigDecimal(row.get("totalProfit")));
                return r;
            }).toList();

            // 3. 写入 Redis 缓存
            long ttl = "all".equals(period) ? 15 : 5;
            redisTemplate.opsForValue().set(cacheKey, result, ttl, TimeUnit.MINUTES);

            return DegradableResponse.ok(result);
        } catch (Exception e) {
            log.error("statistics_degraded enterpriseId={} module=productRanking message=商品排行加载失败",
                    enterpriseId, e);
            return DegradableResponse.degraded(List.of(), "商品排行加载失败");
        }
    }

    @Override
    public DegradableResponse<List<Map<String, Object>>> getCustomerRanking(Long enterpriseId, String period, Integer limit) {
        try {
            int lim = limit != null ? limit : 10;
            String cacheKey = "rk:" + enterpriseId + ":c:" + period;

            // 1. 查 Redis 缓存
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cached = (List<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return DegradableResponse.ok(cached);
            }

            // 2. 查摘要表
            String[] range = getPeriodDateRange(period);
            List<Map<String, Object>> rows = statCustomerMapper.selectRanking(
                    enterpriseId, range[0], range[1], lim);

            List<Map<String, Object>> result = rows.stream().map(row -> {
                Map<String, Object> item = new LinkedHashMap<>();
                Object customerName = row.get("customerName");
                item.put("customerName",
                        customerName == null || customerName.toString().isBlank()
                                ? "未知客户"
                                : customerName.toString());
                item.put("orderCount", row.get("orderCount"));
                item.put("totalAmount", toBigDecimal(row.get("totalAmount")));
                return item;
            }).toList();

            // 3. 写入 Redis 缓存
            long ttl = "all".equals(period) ? 15 : 5;
            redisTemplate.opsForValue().set(cacheKey, result, ttl, TimeUnit.MINUTES);

            return DegradableResponse.ok(result);
        } catch (Exception e) {
            log.error("statistics_degraded enterpriseId={} module=customerRanking message=客户排行加载失败",
                    enterpriseId, e);
            return DegradableResponse.degraded(List.of(), "客户排行加载失败");
        }
    }

    // ===================== 私有方法 =====================

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    /**
     * 根据 period 返回 DATE 格式的日期范围（用于摘要表查询，stat_date 是 DATE 类型）。
     *
     * @return [startDate, endDate]，startDate 可能为 null（all 模式）
     */
    private String[] getPeriodDateRange(String period) {
        LocalDate today = LocalDate.now();
        switch (period) {
            case "week" -> {
                return new String[]{today.with(java.time.DayOfWeek.MONDAY).toString(), today.toString()};
            }
            case "month" -> {
                return new String[]{today.withDayOfMonth(1).toString(), today.toString()};
            }
            case "all" -> {
                return new String[]{null, today.minusDays(1).toString()};
            }
            default -> {
                return new String[]{today.toString(), today.toString()};
            }
        }
    }

    private void markOverviewDegraded(List<String> warnings, Long enterpriseId,
                                      String module, String message, Exception e) {
        warnings.add(message);
        log.error("statistics_overview_degraded enterpriseId={} module={} message={}",
                enterpriseId, module, message, e);
    }
}
