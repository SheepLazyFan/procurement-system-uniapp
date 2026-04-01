-- ============================================================
-- 迁移：oms_sales_order 添加订单来源和取消操作方字段
-- 执行时机：部署新版后端 JAR 之前（或同步执行）
-- ============================================================

ALTER TABLE oms_sales_order
    ADD COLUMN order_source VARCHAR(20) NOT NULL DEFAULT 'MERCHANT'
        COMMENT '订单来源：BUYER=买家线上下单 MERCHANT=商家手动开单',
    ADD COLUMN cancel_by VARCHAR(20) DEFAULT NULL
        COMMENT '取消操作方：BUYER=买家取消 MERCHANT=商家取消';

-- 存量数据：
-- 商家端已存在的订单全部标记为 MERCHANT（默认值已覆盖，此步骤可选）
-- 如有买家历史订单，需按业务实际情况手动 UPDATE
-- UPDATE oms_sales_order SET order_source = 'BUYER' WHERE ... ;
