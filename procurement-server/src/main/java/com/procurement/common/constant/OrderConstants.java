package com.procurement.common.constant;

/**
 * 订单状态常量
 */
public final class OrderConstants {

    private OrderConstants() {}

    // ==================== 销售订单状态 ====================
    public static final String SALES_PENDING    = "PENDING";
    public static final String SALES_CONFIRMED  = "CONFIRMED";
    public static final String SALES_SHIPPED    = "SHIPPED";
    public static final String SALES_COMPLETED  = "COMPLETED";
    public static final String SALES_CANCELLED  = "CANCELLED";

    // ==================== 支付状态 ====================
    public static final String PAY_UNPAID = "UNPAID";
    public static final String PAY_PAID   = "PAID";

    // ==================== 采购订单状态 ====================
    public static final String PURCHASE_PENDING     = "PENDING";
    public static final String PURCHASE_PURCHASING  = "PURCHASING";
    public static final String PURCHASE_ARRIVED     = "ARRIVED";
    public static final String PURCHASE_COMPLETED   = "COMPLETED";
    public static final String PURCHASE_CANCELLED   = "CANCELLED";

    // ==================== 订单号前缀 ====================
    public static final String SALES_ORDER_PREFIX    = "SO";
    public static final String PURCHASE_ORDER_PREFIX = "PO";
}
