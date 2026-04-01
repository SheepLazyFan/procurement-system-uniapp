package com.procurement.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.procurement.entity.SysEnterprise;
import com.procurement.mapper.EnterpriseMapper;
import com.procurement.service.StockWarningNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 库存预警定时任务
 * <p>
 * 每天早上 9:00 执行：扫描所有企业的商品，找出 stock &le; stockWarning 的商品，
 * 向企业主推送微信订阅消息。与实时触发共享相同的 Redis 商品粒度去重 key，
 * 已被实时推送过的商品不会重复推送。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockWarningScheduler {

    private final EnterpriseMapper enterpriseMapper;
    private final StockWarningNotificationService notificationService;

    /**
     * 每天 09:00 定时兜底推送
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkStockWarning() {
        log.info("===== 库存预警定时任务开始 =====");
        List<SysEnterprise> enterprises = enterpriseMapper.selectList(
                new LambdaQueryWrapper<SysEnterprise>());
        for (SysEnterprise enterprise : enterprises) {
            notificationService.checkAndNotifyForScheduler(enterprise, false);
        }
        log.info("===== 库存预警定时任务结束 =====");
    }

    /**
     * 手动触发（供测试接口调用）
     *
     * @param enterpriseId 目标企业 ID
     * @param force        true 时清除当天 Redis 去重 key，允许重复推送
     */
    public void triggerManually(Long enterpriseId, boolean force) {
        SysEnterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise != null) {
            notificationService.checkAndNotifyForScheduler(enterprise, force);
        }
    }
}
