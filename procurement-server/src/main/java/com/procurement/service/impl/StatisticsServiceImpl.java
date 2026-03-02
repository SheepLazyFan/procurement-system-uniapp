package com.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.procurement.dto.response.SalesRankingResponse;
import com.procurement.dto.response.SalesTrendResponse;
import com.procurement.dto.response.StatOverviewResponse;
import com.procurement.entity.*;
import com.procurement.mapper.*;
import com.procurement.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计报表服务实现
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final ProductMapper productMapper;
    private final CustomerMapper customerMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public StatOverviewResponse getOverview(Long enterpriseId) {
        StatOverviewResponse resp = new StatOverviewResponse();

        LocalDate today = LocalDate.now();
        String todayStart = today + " 00:00:00";
        String todayEnd = today + " 23:59:59";
        String monthStart = today.withDayOfMonth(1) + " 00:00:00";

        // 今日销售额
        List<OmsSalesOrder> todayOrders = getSalesOrders(enterpriseId, todayStart, todayEnd);
        resp.setTodaySales(sumAmount(todayOrders));

        // 本月销售额
        List<OmsSalesOrder> monthOrders = getSalesOrders(enterpriseId, monthStart, todayEnd);
        resp.setMonthSales(sumAmount(monthOrders));

        // 今日利润
        resp.setTodayProfit(sumProfit(todayOrders));

        // 本月利润
        resp.setMonthProfit(sumProfit(monthOrders));

        // 库存价值和数量
        List<PmsProduct> products = productMapper.selectList(
                new LambdaQueryWrapper<PmsProduct>()
                        .eq(PmsProduct::getEnterpriseId, enterpriseId));

        BigDecimal inventoryValue = products.stream()
                .map(p -> p.getCostPrice().multiply(BigDecimal.valueOf(p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resp.setInventoryValue(inventoryValue);

        int totalStock = products.stream().mapToInt(PmsProduct::getStock).sum();
        resp.setInventoryCount(totalStock);

        // 待处理订单数
        Long pendingCount = salesOrderMapper.selectCount(
                new LambdaQueryWrapper<OmsSalesOrder>()
                        .eq(OmsSalesOrder::getEnterpriseId, enterpriseId)
                        .eq(OmsSalesOrder::getStatus, "PENDING"));
        resp.setPendingOrderCount(pendingCount.intValue());

        // 库存预警数
        long warningCount = products.stream()
                .filter(p -> p.getStockWarning() > 0 && p.getStock() < p.getStockWarning())
                .count();
        resp.setStockWarningCount((int) warningCount);

        return resp;
    }

    @Override
    public List<SalesTrendResponse> getSalesTrend(Long enterpriseId, String period,
                                                   String startDate, String endDate) {
        // 设置默认时间范围
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

        List<OmsSalesOrder> orders = getSalesOrders(enterpriseId,
                start + " 00:00:00", end + " 23:59:59");

        // 按日期分组
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, List<OmsSalesOrder>> grouped = orders.stream()
                .collect(Collectors.groupingBy(o ->
                        o.getCreatedAt().toLocalDate().format(fmt)));

        // 生成连续日期序列
        List<SalesTrendResponse> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String dateStr = date.format(fmt);
            List<OmsSalesOrder> dayOrders = grouped.getOrDefault(dateStr, Collections.emptyList());

            SalesTrendResponse trend = new SalesTrendResponse();
            trend.setDate(dateStr);
            trend.setAmount(sumAmount(dayOrders));
            trend.setProfit(sumProfit(dayOrders));
            trend.setOrderCount(dayOrders.size());
            result.add(trend);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getProfitTrend(Long enterpriseId, String period,
                                                     String startDate, String endDate) {
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(30);

        List<OmsSalesOrder> orders = getSalesOrders(enterpriseId,
                start + " 00:00:00", end + " 23:59:59");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, List<OmsSalesOrder>> grouped = orders.stream()
                .collect(Collectors.groupingBy(o ->
                        o.getCreatedAt().toLocalDate().format(fmt)));

        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String dateStr = date.format(fmt);
            List<OmsSalesOrder> dayOrders = grouped.getOrDefault(dateStr, Collections.emptyList());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateStr);
            item.put("revenue", sumAmount(dayOrders));
            item.put("cost", dayOrders.stream()
                    .map(OmsSalesOrder::getTotalCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            item.put("profit", sumProfit(dayOrders));
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> getInventoryStats(Long enterpriseId) {
        List<PmsProduct> products = productMapper.selectList(
                new LambdaQueryWrapper<PmsProduct>()
                        .eq(PmsProduct::getEnterpriseId, enterpriseId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalProducts", products.size());
        result.put("totalStock", products.stream().mapToInt(PmsProduct::getStock).sum());

        // 按分类统计
        Map<Long, List<PmsProduct>> byCat = products.stream()
                .collect(Collectors.groupingBy(PmsProduct::getCategoryId));

        List<Map<String, Object>> catStats = new ArrayList<>();
        for (Map.Entry<Long, List<PmsProduct>> entry : byCat.entrySet()) {
            PmsCategory cat = categoryMapper.selectById(entry.getKey());
            Map<String, Object> cs = new LinkedHashMap<>();
            cs.put("categoryName", cat != null ? cat.getName() : "未分类");
            cs.put("productCount", entry.getValue().size());
            cs.put("stockCount", entry.getValue().stream().mapToInt(PmsProduct::getStock).sum());
            catStats.add(cs);
        }
        result.put("categoryStats", catStats);

        long warningCount = products.stream()
                .filter(p -> p.getStockWarning() > 0 && p.getStock() < p.getStockWarning())
                .count();
        result.put("warningCount", warningCount);

        return result;
    }

    @Override
    public List<SalesRankingResponse> getProductRanking(Long enterpriseId, String period, Integer limit) {
        String[] range = getPeriodRange(period);

        // 查询时间范围内的已完成订单
        List<OmsSalesOrder> orders = salesOrderMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrder>()
                        .eq(OmsSalesOrder::getEnterpriseId, enterpriseId)
                        .ne(OmsSalesOrder::getStatus, "CANCELLED")
                        .ge(OmsSalesOrder::getCreatedAt, range[0])
                        .le(OmsSalesOrder::getCreatedAt, range[1]));

        if (orders.isEmpty()) return Collections.emptyList();

        List<Long> orderIds = orders.stream().map(OmsSalesOrder::getId).toList();

        // 查询这些订单的所有明细
        List<OmsSalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrderItem>()
                        .in(OmsSalesOrderItem::getOrderId, orderIds));

        // 按商品名称分组汇总
        Map<String, SalesRankingResponse> rankMap = new LinkedHashMap<>();
        for (OmsSalesOrderItem item : items) {
            SalesRankingResponse rank = rankMap.computeIfAbsent(
                    item.getProductName(), k -> {
                        SalesRankingResponse r = new SalesRankingResponse();
                        r.setProductName(k);
                        r.setTotalQuantity(0);
                        r.setTotalAmount(BigDecimal.ZERO);
                        r.setTotalProfit(BigDecimal.ZERO);
                        return r;
                    });
            rank.setTotalQuantity(rank.getTotalQuantity() + item.getQuantity());
            rank.setTotalAmount(rank.getTotalAmount().add(item.getAmount()));
            rank.setTotalProfit(rank.getTotalProfit().add(item.getProfit()));
        }

        return rankMap.values().stream()
                .sorted(Comparator.comparing(SalesRankingResponse::getTotalAmount).reversed())
                .limit(limit != null ? limit : 10)
                .toList();
    }

    @Override
    public List<Map<String, Object>> getCustomerRanking(Long enterpriseId, String period, Integer limit) {
        String[] range = getPeriodRange(period);

        List<OmsSalesOrder> orders = salesOrderMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrder>()
                        .eq(OmsSalesOrder::getEnterpriseId, enterpriseId)
                        .ne(OmsSalesOrder::getStatus, "CANCELLED")
                        .isNotNull(OmsSalesOrder::getCustomerId)
                        .ge(OmsSalesOrder::getCreatedAt, range[0])
                        .le(OmsSalesOrder::getCreatedAt, range[1]));

        // 按客户分组
        Map<Long, List<OmsSalesOrder>> byCustomer = orders.stream()
                .collect(Collectors.groupingBy(OmsSalesOrder::getCustomerId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<OmsSalesOrder>> entry : byCustomer.entrySet()) {
            CrmCustomer customer = customerMapper.selectById(entry.getKey());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("customerName", customer != null ? customer.getName() : "未知客户");
            item.put("orderCount", entry.getValue().size());
            item.put("totalAmount", entry.getValue().stream()
                    .map(OmsSalesOrder::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(item);
        }

        result.sort((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")));

        return result.stream().limit(limit != null ? limit : 10).toList();
    }

    // ===================== 私有方法 =====================

    private List<OmsSalesOrder> getSalesOrders(Long enterpriseId, String start, String end) {
        return salesOrderMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrder>()
                        .eq(OmsSalesOrder::getEnterpriseId, enterpriseId)
                        .ne(OmsSalesOrder::getStatus, "CANCELLED")
                        .ge(OmsSalesOrder::getCreatedAt, start)
                        .le(OmsSalesOrder::getCreatedAt, end));
    }

    private BigDecimal sumAmount(List<OmsSalesOrder> orders) {
        return orders.stream()
                .map(OmsSalesOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumProfit(List<OmsSalesOrder> orders) {
        return orders.stream()
                .map(OmsSalesOrder::getTotalProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String[] getPeriodRange(String period) {
        LocalDate end = LocalDate.now();
        LocalDate start = switch (period) {
            case "week" -> end.minusWeeks(1);
            case "month" -> end.minusMonths(1);
            default -> end.minusDays(1); // day
        };
        return new String[]{start + " 00:00:00", end + " 23:59:59"};
    }
}
