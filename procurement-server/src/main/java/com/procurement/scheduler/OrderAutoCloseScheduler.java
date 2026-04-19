package com.procurement.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.procurement.common.constant.OrderConstants;
import com.procurement.entity.OmsSalesOrder;
import com.procurement.entity.OmsSalesOrderItem;
import com.procurement.mapper.ProductMapper;
import com.procurement.mapper.SalesOrderItemMapper;
import com.procurement.mapper.SalesOrderMapper;
import com.procurement.service.StockWarningNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单自动关闭定时任务
 * <p>
 * 每小时扫描一次：将超时未支付（UNPAID）的 PENDING 订单自动取消并恢复库存。
 * 超时阈值默认 24 小时，可通过 order.auto-close-hours 配置覆盖。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCloseScheduler {

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final ProductMapper productMapper;
    private final StockWarningNotificationService notificationService;

    /** 超时小时数，默认 24 小时 */
    @Value("${order.auto-close-hours:24}")
    private int autoCloseHours;

    /**
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void autoCloseExpiredOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(autoCloseHours);

        List<OmsSalesOrder> expiredOrders = salesOrderMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrder>()
                        .eq(OmsSalesOrder::getStatus, OrderConstants.SALES_PENDING)
                        .eq(OmsSalesOrder::getPaymentStatus, OrderConstants.PAY_UNPAID)
                        .lt(OmsSalesOrder::getCreatedAt, deadline));

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("===== 订单自动关闭：发现 {} 笔超时订单 =====", expiredOrders.size());

        for (OmsSalesOrder order : expiredOrders) {
            try {
                cancelExpiredOrder(order);
            } catch (Exception e) {
                log.error("自动关闭订单失败: orderId={}, orderNo={}", order.getId(), order.getOrderNo(), e);
            }
        }

        log.info("===== 订单自动关闭任务结束 =====");
    }

    @Transactional
    public void cancelExpiredOrder(OmsSalesOrder order) {
        // 恢复库存
        List<OmsSalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<OmsSalesOrderItem>()
                        .eq(OmsSalesOrderItem::getOrderId, order.getId()));
        List<Long> restoredIds = new ArrayList<>();
        for (OmsSalesOrderItem item : items) {
            productMapper.adjustStock(item.getProductId(), item.getQuantity());
            restoredIds.add(item.getProductId());
        }

        // 库存恢复后清除预警去重 key
        if (!restoredIds.isEmpty()) {
            final Long eid = order.getEnterpriseId();
            final List<Long> pids = restoredIds;
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationService.clearDedupOnRestock(pids, eid);
                    }
                });
            } else {
                notificationService.clearDedupOnRestock(pids, eid);
            }
        }

        order.setStatus(OrderConstants.SALES_CANCELLED);
        order.setCancelBy(OrderConstants.CANCEL_BY_SYSTEM);
        order.setRemark((order.getRemark() != null ? order.getRemark() + " | " : "") + "系统自动关闭（超时 " + autoCloseHours + " 小时未付款）");
        salesOrderMapper.updateById(order);

        log.info("订单已自动关闭: orderNo={}", order.getOrderNo());
    }
}
