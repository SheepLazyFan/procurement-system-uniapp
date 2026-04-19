package com.procurement.scheduler;

import com.procurement.entity.StatDailyCustomerSales;
import com.procurement.entity.StatDailyProductSales;
import com.procurement.mapper.SalesOrderMapper;
import com.procurement.mapper.StatDailyCustomerSalesMapper;
import com.procurement.mapper.StatDailyProductSalesMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 排行数据增量聚合定时任务。
 * <p>
 * 每 15 分钟扫描当天的订单明细，按日聚合写入摘要表，
 * 并清除对应的 Redis 排行缓存。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingAggregateScheduler {

    private final SalesOrderMapper salesOrderMapper;
    private final StatDailyProductSalesMapper statProductMapper;
    private final StatDailyCustomerSalesMapper statCustomerMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 应用启动后自动检测：如果摘要表为空则执行全量回填。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        long count = statProductMapper.selectCount(null);
        if (count == 0) {
            log.info("[RankingAggregate] 摘要表为空，自动执行全量回填...");
            backfillAll();
        } else {
            log.info("[RankingAggregate] 摘要表已有 {} 条记录，跳过回填", count);
        }
    }

    /**
     * 每 15 分钟增量聚合当天订单数据。
     * <p>
     * 策略：聚合当天的全部非取消订单，用 ON DUPLICATE KEY UPDATE 幂等覆盖。
     * 相比"只扫增量 + delta 累加"的方案，幂等覆盖更简单、无需维护检查点、
     * 天然处理取消回减（取消后重新聚合即可覆盖）。
     * 当天数据量 = 总量 / 365，千万级时约 2.7 万行，聚合耗时 < 200ms。
     * </p>
     */
    @Scheduled(cron = "0 */15 * * * ?")
    public void aggregateToday() {
        log.info("[RankingAggregate] 开始聚合当天排行数据...");
        long start = System.currentTimeMillis();
        try {
            LocalDate today = LocalDate.now();
            String dayStart = today + " 00:00:00";
            String dayEnd = today + " 23:59:59";

            // 获取所有活跃企业 ID（有当天订单的企业）
            List<Long> enterpriseIds = salesOrderMapper.selectActiveEnterpriseIds(dayStart, dayEnd);

            for (Long eid : enterpriseIds) {
                aggregateProductForDay(eid, today, dayStart, dayEnd);
                aggregateCustomerForDay(eid, today, dayStart, dayEnd);
                evictRankingCache(eid);
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("[RankingAggregate] 聚合完成，企业数={}，耗时={}ms", enterpriseIds.size(), elapsed);
        } catch (Exception e) {
            log.error("[RankingAggregate] 聚合异常", e);
        }
    }

    /**
     * 首次部署时调用，回填全部历史数据到摘要表。
     */
    public void backfillAll() {
        log.info("[RankingAggregate] 开始全量历史回填...");
        long start = System.currentTimeMillis();

        List<Long> allEnterpriseIds = salesOrderMapper.selectAllEnterpriseIds();
        for (Long eid : allEnterpriseIds) {
            // 查询该企业所有日期的商品聚合
            List<Map<String, Object>> productRows = salesOrderMapper.selectProductDailyAgg(eid);
            if (!productRows.isEmpty()) {
                List<StatDailyProductSales> entities = productRows.stream().map(row -> {
                    StatDailyProductSales s = new StatDailyProductSales();
                    s.setEnterpriseId(eid);
                    s.setStatDate(LocalDate.parse(row.get("statDate").toString()));
                    s.setProductId(((Number) row.get("productId")).longValue());
                    s.setProductName((String) row.get("productName"));
                    s.setSumQuantity(((Number) row.get("totalQuantity")).intValue());
                    s.setSumAmount(toBigDecimal(row.get("totalAmount")));
                    s.setSumProfit(toBigDecimal(row.get("totalProfit")));
                    return s;
                }).toList();
                statProductMapper.insertOrUpdate(entities);
            }

            // 客户聚合
            List<Map<String, Object>> customerRows = salesOrderMapper.selectCustomerDailyAgg(eid);
            if (!customerRows.isEmpty()) {
                List<StatDailyCustomerSales> entities = customerRows.stream().map(row -> {
                    StatDailyCustomerSales s = new StatDailyCustomerSales();
                    s.setEnterpriseId(eid);
                    s.setStatDate(LocalDate.parse(row.get("statDate").toString()));
                    s.setCustomerId(((Number) row.get("customerId")).longValue());
                    s.setCustomerName(row.get("customerName") != null ? row.get("customerName").toString() : "");
                    s.setOrderCount(((Number) row.get("orderCount")).intValue());
                    s.setSumAmount(toBigDecimal(row.get("totalAmount")));
                    return s;
                }).toList();
                statCustomerMapper.insertOrUpdate(entities);
            }

            evictRankingCache(eid);
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("[RankingAggregate] 全量回填完成，企业数={}，耗时={}ms", allEnterpriseIds.size(), elapsed);
    }

    // ==================== 私有方法 ====================

    private void aggregateProductForDay(Long enterpriseId, LocalDate day, String dayStart, String dayEnd) {
        List<Map<String, Object>> rows = salesOrderMapper.selectProductRanking(
                enterpriseId, dayStart, dayEnd, Integer.MAX_VALUE);
        if (rows.isEmpty()) return;

        List<StatDailyProductSales> entities = rows.stream().map(row -> {
            StatDailyProductSales s = new StatDailyProductSales();
            s.setEnterpriseId(enterpriseId);
            s.setStatDate(day);
            // productId 可能不在原 ranking SQL 中，需要扩展
            s.setProductId(row.get("productId") != null ? ((Number) row.get("productId")).longValue() : 0L);
            s.setProductName((String) row.get("productName"));
            s.setSumQuantity(((Number) row.get("totalQuantity")).intValue());
            s.setSumAmount(toBigDecimal(row.get("totalAmount")));
            s.setSumProfit(toBigDecimal(row.get("totalProfit")));
            return s;
        }).toList();

        statProductMapper.insertOrUpdate(entities);
    }

    private void aggregateCustomerForDay(Long enterpriseId, LocalDate day, String dayStart, String dayEnd) {
        List<Map<String, Object>> rows = salesOrderMapper.selectCustomerRanking(
                enterpriseId, dayStart, dayEnd, Integer.MAX_VALUE);
        if (rows.isEmpty()) return;

        List<StatDailyCustomerSales> entities = rows.stream().map(row -> {
            StatDailyCustomerSales s = new StatDailyCustomerSales();
            s.setEnterpriseId(enterpriseId);
            s.setStatDate(day);
            s.setCustomerId(row.get("customerId") != null ? ((Number) row.get("customerId")).longValue() : 0L);
            Object cn = row.get("customerName");
            s.setCustomerName(cn != null ? cn.toString() : "");
            s.setOrderCount(((Number) row.get("orderCount")).intValue());
            s.setSumAmount(toBigDecimal(row.get("totalAmount")));
            return s;
        }).toList();

        statCustomerMapper.insertOrUpdate(entities);
    }

    /**
     * 清除指定企业的所有排行缓存。
     */
    private void evictRankingCache(Long enterpriseId) {
        try {
            Set<String> keys = redisTemplate.keys("rk:" + enterpriseId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("[RankingAggregate] 清除缓存 enterpriseId={} keys={}", enterpriseId, keys.size());
            }
        } catch (Exception e) {
            log.warn("[RankingAggregate] 清除缓存失败 enterpriseId={}", enterpriseId, e);
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
