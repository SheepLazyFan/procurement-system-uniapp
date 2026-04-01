package com.procurement.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 订单号生成器 — Redis 优先，DB 降级兜底
 * 格式：前缀 + yyyyMMdd + 4位序号（如 SO202603060001）
 */
@Slf4j
@Component
public class OrderNoGenerator {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    public OrderNoGenerator(StringRedisTemplate redisTemplate, JdbcTemplate jdbcTemplate) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 生成订单编号
     * @param prefix       前缀（如 "SO"、"PO"）
     * @param enterpriseId 企业ID
     * @param tableName    订单表名（如 "oms_sales_order"、"oms_purchase_order"），DB 降级时使用
     * @return 格式化的订单号
     */
    public String generate(String prefix, Long enterpriseId, String tableName) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNoPrefix = prefix + dateStr;

        // 优先使用 Redis 原子自增
        try {
            String redisKey = "order:no:" + prefix + ":" + enterpriseId + ":" + dateStr;
            Long seq = redisTemplate.opsForValue().increment(redisKey);
            if (seq != null && seq == 1) {
                redisTemplate.expire(redisKey, 2, TimeUnit.DAYS);
            }
            return orderNoPrefix + String.format("%04d", seq);
        } catch (Exception e) {
            log.warn("Redis 不可用，订单号生成降级到 DB 方案: {}", e.getMessage());
        }

        // DB 降级：查询当日该企业该前缀的最大订单号，+1
        return generateFromDb(orderNoPrefix, enterpriseId, tableName);
    }

    private synchronized String generateFromDb(String orderNoPrefix, Long enterpriseId, String tableName) {
        String sql = "SELECT MAX(order_no) FROM " + tableName
                + " WHERE enterprise_id = ? AND order_no LIKE ?";
        String maxOrderNo = jdbcTemplate.queryForObject(sql, String.class, enterpriseId, orderNoPrefix + "%");

        long nextSeq = 1;
        if (maxOrderNo != null && maxOrderNo.length() > orderNoPrefix.length()) {
            try {
                long currentSeq = Long.parseLong(maxOrderNo.substring(orderNoPrefix.length()));
                nextSeq = currentSeq + 1;
            } catch (NumberFormatException ex) {
                log.warn("解析订单号序列失败: {}", maxOrderNo);
            }
        }
        return orderNoPrefix + String.format("%04d", nextSeq);
    }
}
