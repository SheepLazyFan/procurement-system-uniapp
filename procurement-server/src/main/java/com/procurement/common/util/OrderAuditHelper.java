package com.procurement.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 订单审计备注工具类
 * <p>
 * 统一生成结构化的 [系统流水] 审计备注行，供商家端和买家端共同使用。
 * 格式：[系统流水] 时间 | 角色:ID | 动作 | 状态变化 | 备注
 * </p>
 */
public final class OrderAuditHelper {

    private static final DateTimeFormatter AUDIT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private OrderAuditHelper() {
        // 工具类禁止实例化
    }

    /**
     * 在原始备注后追加一行结构化审计记录。
     *
     * @param originalRemark    原有备注（可为 null/空）
     * @param operatorId        操作者用户 ID
     * @param operatorRole      操作者角色（SELLER/ADMIN/SALES/BUYER 等）
     * @param action            操作动作（CONFIRM_PAYMENT/CANCEL/CLAIM_PAID 等）
     * @param fromPaymentStatus 变更前的付款状态（可为 null）
     * @param toPaymentStatus   变更后的付款状态（可为 null）
     * @param message           附加说明
     * @return 追加后的完整备注文本
     */
    public static String appendSystemAuditRemark(String originalRemark, Long operatorId, String operatorRole,
                                                  String action, String fromPaymentStatus, String toPaymentStatus,
                                                  String message) {
        String line = String.format("[系统流水] %s | %s:%s | %s | %s -> %s | %s",
                LocalDateTime.now().format(AUDIT_TIME_FORMATTER),
                operatorRole, operatorId, action,
                fromPaymentStatus != null ? fromPaymentStatus : "-",
                toPaymentStatus != null ? toPaymentStatus : "-",
                message);
        if (originalRemark == null || originalRemark.isBlank()) {
            return line;
        }
        return originalRemark + System.lineSeparator() + line;
    }
}
