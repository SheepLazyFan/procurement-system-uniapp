package com.procurement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.procurement.entity.PmsProduct;
import com.procurement.entity.SysEnterprise;
import com.procurement.entity.SysUser;
import com.procurement.mapper.ProductMapper;
import com.procurement.mapper.UserMapper;
import com.procurement.service.impl.WxSubscribeMessageServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 库存预警实时通知服务
 * <p>
 * 在库存减少操作的事务提交后异步调用，检查涉及商品是否跌破预警阈值并推送微信订阅消息。
 * <ul>
 *   <li>实时推送去重：使用「跌穿锁」key（无日期），每次库存从阈值以上跌入阈值以下触发一次，
 *       入库恢复到阈值以上后清除，确保下次跌穿仍能推送</li>
 *   <li>多商品汇总通知：一次操作触发多件预警时，合并为一条推送</li>
 *   <li>9 点定时任务使用独立的每日 key 作为兜底，与实时推送互不干扰</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockWarningNotificationService {

    public static final String DAILY_KEY_PREFIX = "stock_warning:sent:";

    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final WxSubscribeMessageServiceImpl wxSubscribeMessageService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 检查指定商品列表，对其中跌破预警阈值且尚未发送过今日通知的商品推送消息。
     * 使用 @Async 在独立线程中执行，不阻塞业务主流程。
     *
     * @param productIds   本次操作涉及的商品 ID 列表
     * @param enterpriseId 所属企业 ID
     */
    @Async("notifyExecutor")
    public void checkAndNotify(Map<Long, Integer> previousStocks, Long enterpriseId) {
        if (previousStocks == null || previousStocks.isEmpty()) {
            return;
        }
        List<Long> productIds = new ArrayList<>(previousStocks.keySet());

        // 查询企业下所有开启了库存预警通知且有 openId 的用户
        List<SysUser> targets = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEnterpriseId, enterpriseId)
                        .eq(SysUser::getNotifyStockWarning, 1)
                        .isNotNull(SysUser::getWxOpenid)
                        .ne(SysUser::getWxOpenid, ""));
        if (targets.isEmpty()) {
            log.debug("[Notify] 企业 {} 无用户开启预警通知或无 openId，跳过推送", enterpriseId);
            return;
        }

        // 查询本次涉及商品的最新库存（事务已提交，数据是最新值）
        List<PmsProduct> products = productMapper.selectList(
                new LambdaQueryWrapper<PmsProduct>()
                        .in(PmsProduct::getId, productIds)
                        .eq(PmsProduct::getEnterpriseId, enterpriseId));

        // 边沿触发：只有从阈值以上【跌入】阈值以内才推送，避免在阈值内连续出库重复骚扰
        // previousStocks 含有操作前库存，不需要 Redis 去重
        List<PmsProduct> toNotify = new ArrayList<>();
        for (PmsProduct p : products) {
            if (p.getStockWarning() == null || p.getStockWarning() <= 0 || p.getStock() > p.getStockWarning()) {
                continue; // 未配置阈值(<=0)或未触发
            }
            int prevStock = previousStocks.getOrDefault(p.getId(), p.getStock() + 1);
            if (prevStock <= p.getStockWarning()) {
                continue; // 操作前就已在阈值内，属于持续减少，不重复通知
            }
            // preStock > threshold AND currentStock <= threshold —— 真正的跌穿事件
            toNotify.add(p);
        }

        if (toNotify.isEmpty()) {
            return;
        }

        // 汇总通知：库存最低的商品作为代表，多件时附件数
        PmsProduct top = toNotify.stream()
                .min((a, b) -> a.getStock() - b.getStock())
                .orElse(toNotify.get(0));
        String productName = toNotify.size() == 1
                ? top.getName()
                : top.getName() + " 等" + toNotify.size() + "件";

        int prevStock = previousStocks.getOrDefault(top.getId(), top.getStock());

        // 向每位开启通知的用户分别推送
        for (SysUser target : targets) {
            boolean sent = wxSubscribeMessageService.sendStockWarning(
                    target.getWxOpenid(),
                    productName,
                    prevStock,
                    top.getStock());
            if (sent) {
                log.info("[Notify] 实时预警推送成功: enterpriseId={}, userId={}, products={}",
                        enterpriseId, target.getId(), productName);
            }
        }
    }

    /**
     * 定时任务专用：检查企业下所有预警商品，跳过今天已推送过的（商品粒度去重）。
     * 返回是否成功推送（供 Scheduler 记录日志）。
     *
     * @param enterprise 企业实体
     * @param force      是否强制推送（跳过 Redis 去重，仅用于测试接口）
     */
    public boolean checkAndNotifyForScheduler(SysEnterprise enterprise, boolean force) {
        Long enterpriseId = enterprise.getId();

        List<PmsProduct> warningProducts = productMapper.selectList(
                new LambdaQueryWrapper<PmsProduct>()
                        .eq(PmsProduct::getEnterpriseId, enterpriseId)
                        .apply("stock <= stock_warning AND stock_warning > 0"));

        if (warningProducts.isEmpty()) {
            return false;
        }

        // 查询企业下所有开启了库存预警通知且有 openId 的用户
        List<SysUser> targets = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEnterpriseId, enterpriseId)
                        .eq(SysUser::getNotifyStockWarning, 1)
                        .isNotNull(SysUser::getWxOpenid)
                        .ne(SysUser::getWxOpenid, ""));
        if (targets.isEmpty()) {
            log.debug("[Scheduler] 企业 {} 无用户开启预警通知或无 openId，跳过推送", enterpriseId);
            return false;
        }

        String today = LocalDate.now().toString();
        boolean anySent = false;

        // 每位用户独立去重：筛选该用户今天尚未推送的商品后汇总推送一条
        for (SysUser target : targets) {
            // Phase 1: 试占 key —— setIfAbsent 保证原子性，防并发重复推送
            List<PmsProduct> toNotify = new ArrayList<>();
            List<String> tentativeKeys = new ArrayList<>();
            for (PmsProduct p : warningProducts) {
                String key = DAILY_KEY_PREFIX + today + ":" + target.getId() + ":" + p.getId();
                if (force) {
                    redisTemplate.delete(key); // 清除旧 key，允许重复推送
                }
                Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "1");
                if (Boolean.TRUE.equals(isNew)) {
                    redisTemplate.expire(key, Duration.ofHours(25));
                    toNotify.add(p);
                    tentativeKeys.add(key);
                }
            }

            if (toNotify.isEmpty()) {
                log.info("[Scheduler] 企业 {} 用户 {} 今日预警商品已全部推送过，跳过",
                        enterpriseId, target.getId());
                continue;
            }

            // 汇总推送：库存最低的商品作为代表
            PmsProduct top = toNotify.stream()
                    .min((a, b) -> a.getStock() - b.getStock())
                    .orElse(toNotify.get(0));
            // [提醒] 前缀区分定时兜底与实时操作触发，避免用户误以为有人操作了库存
            String productName = toNotify.size() == 1
                    ? top.getName()
                    : top.getName() + " 等" + toNotify.size() + "件";
            String displayName = "[提醒] " + productName;

            // Phase 2: 推送；number2=当前库存，number3=预警阈值（与实时通知字段语义不同，用前缀区分）
            boolean sent = wxSubscribeMessageService.sendStockWarning(
                    target.getWxOpenid(),
                    displayName,
                    top.getStock(),
                    top.getStockWarning());

            if (sent) {
                log.info("[Scheduler] 预警推送成功: enterpriseId={}, userId={}, products={}",
                        enterpriseId, target.getId(), productName);
                anySent = true;
            } else {
                // Phase 3: 推送失败 —— 回滚占位 key，确保下次定时任务可以重试
                log.warn("[Scheduler] 预警推送失败，回滚 Redis key: enterpriseId={}, userId={}, keyCount={}",
                        enterpriseId, target.getId(), tentativeKeys.size());
                tentativeKeys.forEach(redisTemplate::delete);
            }
        }
        return anySent;
    }

    /**
     * 入库后若库存已恢复至预警阈值以上，清除今日去重 key，使下次出库跌破阈值时能再次推送。
     * 使用 @Async 在独立线程中执行，不阻塞业务主流程。
     *
     * @param productIds   本次入库涉及的商品 ID 列表
     * @param enterpriseId 所属企业 ID
     */
    @Async("notifyExecutor")
    public void clearDedupOnRestock(List<Long> productIds, Long enterpriseId) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        // 查询企业下所有开启了库存预警通知的用户（需要清除各自的每日锁）
        List<SysUser> targets = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEnterpriseId, enterpriseId)
                        .eq(SysUser::getNotifyStockWarning, 1));
        if (targets.isEmpty()) {
            return;
        }
        List<PmsProduct> products = productMapper.selectList(
                new LambdaQueryWrapper<PmsProduct>()
                        .in(PmsProduct::getId, productIds)
                        .eq(PmsProduct::getEnterpriseId, enterpriseId));
        String today = LocalDate.now().toString();
        for (PmsProduct p : products) {
            // 库存恢复到预警阈值以上 → 清除定时巡检的每日锁，允许当天再次跌穿时定时任务也能推送
            if (p.getStockWarning() != null && p.getStockWarning() > 0 && p.getStock() > p.getStockWarning()) {
                for (SysUser target : targets) {
                    String dailyKey = DAILY_KEY_PREFIX + today + ":" + target.getId() + ":" + p.getId();
                    redisTemplate.delete(dailyKey);
                }
                log.debug("[Notify] 库存恢复预警线以上，已清除每日锁: productId={}", p.getId());
            }
        }
    }
}
