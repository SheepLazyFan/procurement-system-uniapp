package com.procurement.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.procurement.entity.SysEnterprise;
import com.procurement.entity.SysUser;
import com.procurement.mapper.EnterpriseMapper;
import com.procurement.mapper.UserMapper;
import com.procurement.service.BackupService;
import com.procurement.service.impl.WxSubscribeMessageServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据自动备份定时任务
 * <p>
 * 每天凌晨 3:00 对所有企业执行 JSON 全量备份（应用层）。
 * 若某企业备份失败，捕获异常后继续处理其余企业，并向企业主推送微信告警。
 * <br>
 * 可通过配置项覆盖 cron 表达式：{@code backup.auto.cron}（默认 "0 0 3 * * ?"）
 * 可通过配置项关闭自动备份：{@code backup.auto.enabled=false}
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupAutoScheduler {

    private final BackupService backupService;
    private final EnterpriseMapper enterpriseMapper;
    private final UserMapper userMapper;
    private final WxSubscribeMessageServiceImpl wxSubscribeMessageService;

    /**
     * 每天凌晨 3:00 自动备份所有企业数据。
     * cron 可通过 {@code backup.auto.cron} 配置项覆盖，默认每天 03:00。
     */
    @Scheduled(cron = "${backup.auto.cron:0 0 3 * * ?}")
    public void autoBackupAll() {
        log.info("===== 自动备份任务开始 [{}] =====", LocalDateTime.now());

        List<SysEnterprise> enterprises = enterpriseMapper.selectList(
                new LambdaQueryWrapper<SysEnterprise>()
                        .select(SysEnterprise::getId, SysEnterprise::getName, SysEnterprise::getOwnerId)
        );

        if (enterprises.isEmpty()) {
            log.info("无企业数据，自动备份任务跳过");
            return;
        }

        int success = 0;
        int failed = 0;

        for (SysEnterprise enterprise : enterprises) {
            try {
                backupService.create(enterprise.getId(), "AUTO");
                success++;
                log.info("企业 [{}] (id={}) 自动备份成功", enterprise.getName(), enterprise.getId());
            } catch (Exception e) {
                failed++;
                log.error("企业 [{}] (id={}) 自动备份失败: {}", enterprise.getName(), enterprise.getId(), e.getMessage(), e);
                notifyOwnerOnFailure(enterprise, e.getMessage());
            }
        }

        log.info("===== 自动备份任务结束: 成功={}, 失败={} =====", success, failed);
    }

    /**
     * 备份失败后向企业主推送微信告警通知。
     * 通知发送失败时仅记录日志，不影响其他企业的备份流程。
     */
    private void notifyOwnerOnFailure(SysEnterprise enterprise, String errorMsg) {
        try {
            if (enterprise.getOwnerId() == null) {
                log.warn("企业 [{}] ownerId 为空，无法发送备份失败通知", enterprise.getId());
                return;
            }

            SysUser owner = userMapper.selectById(enterprise.getOwnerId());
            if (owner == null || owner.getWxOpenid() == null || owner.getWxOpenid().isBlank()) {
                log.warn("企业 [{}] 主用户无 openId，跳过备份失败通知推送", enterprise.getId());
                return;
            }

            wxSubscribeMessageService.sendBackupAlert(
                    owner.getWxOpenid(),
                    enterprise.getName(),
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            // 通知失败不中断主流程，仅记录日志
            log.error("企业 [{}] 备份失败通知发送异常: {}", enterprise.getId(), e.getMessage());
        }
    }
}
