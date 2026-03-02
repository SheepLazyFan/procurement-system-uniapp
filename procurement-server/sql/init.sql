-- ============================================================
-- 采购系统微信小程序 — 数据库初始化脚本
-- 数据库: procurement_db
-- 字符集: utf8mb4
-- 引擎:   InnoDB
-- 版本:   v1.0  |  2026-03-01
-- ============================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `procurement_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `procurement_db`;

-- ============================================================
-- 1. sys_user — 用户表
-- 说明：存储商家、团队成员、买家的账户信息
-- ============================================================
CREATE TABLE `sys_user` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `phone`         VARCHAR(20)   DEFAULT NULL             COMMENT '手机号（商家登录凭证）',
  `password_hash` VARCHAR(255)  DEFAULT NULL             COMMENT '密码哈希（BCrypt，预留字段）',
  `role`          VARCHAR(20)   NOT NULL DEFAULT 'SELLER' COMMENT '用户角色：SELLER / MEMBER / BUYER',
  `enterprise_id` BIGINT        DEFAULT NULL             COMMENT '所属企业ID（买家无此关联）',
  `wx_openid`     VARCHAR(128)  DEFAULT NULL             COMMENT '微信 OpenID（买家授权后绑定）',
  `wx_union_id`   VARCHAR(128)  DEFAULT NULL             COMMENT '微信 UnionID（预留跨平台）',
  `nick_name`     VARCHAR(50)   DEFAULT NULL             COMMENT '昵称',
  `avatar_url`    VARCHAR(500)  DEFAULT NULL             COMMENT '头像 URL',
  `last_login_at` DATETIME      DEFAULT NULL             COMMENT '最后登录时间',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除（0=正常，1=已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone`         (`phone`),
  UNIQUE KEY `uk_wx_openid`     (`wx_openid`),
  KEY `idx_enterprise_id`       (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. sys_enterprise — 企业表
-- 说明：商家创建的企业实体，所有业务数据均挂载在企业下
-- ============================================================
CREATE TABLE `sys_enterprise` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `name`          VARCHAR(100)  NOT NULL                 COMMENT '企业名称',
  `address`       VARCHAR(300)  DEFAULT NULL             COMMENT '企业地址',
  `contact_phone` VARCHAR(20)   DEFAULT NULL             COMMENT '联系电话',
  `contact_name`  VARCHAR(50)   DEFAULT NULL             COMMENT '联系人',
  `owner_id`      BIGINT        NOT NULL                 COMMENT '企业主用户ID',
  `invite_code`   VARCHAR(20)   DEFAULT NULL             COMMENT '团队邀请码',
  `logo_url`      VARCHAR(500)  DEFAULT NULL             COMMENT '企业 Logo URL',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_id`      (`owner_id`),
  UNIQUE KEY `uk_invite_code`   (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业表';

-- 添加外键引用（sys_user.enterprise_id → sys_enterprise.id）
-- 注：因为相互引用，使用 ALTER TABLE 后置添加
ALTER TABLE `sys_user`
  ADD CONSTRAINT `fk_user_enterprise`
  FOREIGN KEY (`enterprise_id`) REFERENCES `sys_enterprise`(`id`)
  ON DELETE SET NULL;

ALTER TABLE `sys_enterprise`
  ADD CONSTRAINT `fk_enterprise_owner`
  FOREIGN KEY (`owner_id`) REFERENCES `sys_user`(`id`)
  ON DELETE RESTRICT;

-- ============================================================
-- 3. pms_category — 商品分类表
-- 说明：商家自定义的商品分类，按企业隔离
-- ============================================================
CREATE TABLE `pms_category` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `enterprise_id` BIGINT        NOT NULL                 COMMENT '所属企业',
  `name`          VARCHAR(50)   NOT NULL                 COMMENT '分类名称',
  `sort_order`    INT           NOT NULL DEFAULT 0       COMMENT '排序值（升序）',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enterprise_name` (`enterprise_id`, `name`, `is_deleted`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  CONSTRAINT `fk_category_enterprise`
    FOREIGN KEY (`enterprise_id`) REFERENCES `sys_enterprise`(`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ============================================================
-- 4. pms_product — 商品表
-- 说明：商家的商品信息，挂载在分类和企业下
-- ============================================================
CREATE TABLE `pms_product` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `enterprise_id`  BIGINT        NOT NULL                 COMMENT '所属企业',
  `category_id`    BIGINT        NOT NULL                 COMMENT '所属分类',
  `name`           VARCHAR(100)  NOT NULL                 COMMENT '商品名称',
  `spec`           VARCHAR(200)  DEFAULT NULL             COMMENT '规格型号',
  `unit`           VARCHAR(20)   NOT NULL                 COMMENT '计量单位（箱、瓶、kg 等）',
  `price`          DECIMAL(10,2) NOT NULL                 COMMENT '销售单价',
  `cost_price`     DECIMAL(10,2) NOT NULL DEFAULT 0.00    COMMENT '成本价（利润计算）',
  `stock`          INT           NOT NULL DEFAULT 0       COMMENT '当前库存量',
  `stock_warning`  INT           NOT NULL DEFAULT 0       COMMENT '库存预警阈值（0=不预警）',
  `images`         JSON          DEFAULT NULL             COMMENT '商品图片 URL 数组 ["url1","url2"]',
  `status`         TINYINT(1)    NOT NULL DEFAULT 1       COMMENT '状态：1=上架，0=下架',
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id`          (`enterprise_id`),
  KEY `idx_category_id`            (`category_id`),
  KEY `idx_enterprise_category`    (`enterprise_id`, `category_id`),
  KEY `idx_stock_warning`          (`enterprise_id`, `stock`, `stock_warning`),
  CONSTRAINT `fk_product_enterprise`
    FOREIGN KEY (`enterprise_id`) REFERENCES `sys_enterprise`(`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_product_category`
    FOREIGN KEY (`category_id`) REFERENCES `pms_category`(`id`)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ============================================================
-- 5. crm_customer — 客户表
-- 说明：商家的下游客户信息
-- ============================================================
CREATE TABLE `crm_customer` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `enterprise_id` BIGINT        NOT NULL                 COMMENT '所属企业',
  `name`          VARCHAR(100)  NOT NULL                 COMMENT '客户名称',
  `phone`         VARCHAR(20)   DEFAULT NULL             COMMENT '联系电话',
  `address`       VARCHAR(300)  DEFAULT NULL             COMMENT '收货地址',
  `wx_openid`     VARCHAR(128)  DEFAULT NULL             COMMENT '微信 OpenID（线上下单自动关联）',
  `remark`        VARCHAR(500)  DEFAULT NULL             COMMENT '备注',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id`             (`enterprise_id`),
  KEY `idx_enterprise_wx_openid`      (`enterprise_id`, `wx_openid`),
  CONSTRAINT `fk_customer_enterprise`
    FOREIGN KEY (`enterprise_id`) REFERENCES `sys_enterprise`(`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- ============================================================
-- 6. crm_supplier — 供应商表
-- 说明：商家的上游供应商信息
-- ============================================================
CREATE TABLE `crm_supplier` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `enterprise_id`  BIGINT        NOT NULL                 COMMENT '所属企业',
  `name`           VARCHAR(100)  NOT NULL                 COMMENT '供应商名称',
  `phone`          VARCHAR(20)   DEFAULT NULL             COMMENT '联系电话',
  `address`        VARCHAR(300)  DEFAULT NULL             COMMENT '供应商地址',
  `main_category`  VARCHAR(100)  DEFAULT NULL             COMMENT '主营品类（如"饮料、零食"）',
  `remark`         VARCHAR(500)  DEFAULT NULL             COMMENT '备注',
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  CONSTRAINT `fk_supplier_enterprise`
    FOREIGN KEY (`enterprise_id`) REFERENCES `sys_enterprise`(`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- ============================================================
-- 7. oms_sales_order — 销售订单主表
-- 说明：记录商家的销售订单（来自买家线上下单或商家手动开单）
-- 防御设计：customer 删除时不级联删除订单，保留历史记录
-- ============================================================
CREATE TABLE `oms_sales_order` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `order_no`        VARCHAR(30)   NOT NULL                 COMMENT '订单编号（规则：SO+年月日+4位序号）',
  `enterprise_id`   BIGINT        NOT NULL                 COMMENT '所属企业',
  `customer_id`     BIGINT        DEFAULT NULL             COMMENT '客户ID（手动开单可为空）',
  `total_amount`    DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '订单总金额',
  `total_cost`      DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '订单总成本',
  `total_profit`    DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '订单毛利润',
  `status`          VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING/CONFIRMED/SHIPPED/COMPLETED/CANCELLED',
  `payment_status`  VARCHAR(20)   NOT NULL DEFAULT 'UNPAID'  COMMENT '支付状态：UNPAID/PAID',
  `remark`          VARCHAR(500)  DEFAULT NULL             COMMENT '订单备注',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no`            (`order_no`),
  KEY `idx_enterprise_id`             (`enterprise_id`),
  KEY `idx_enterprise_status`         (`enterprise_id`, `status`),
  KEY `idx_customer_id`               (`customer_id`),
  KEY `idx_enterprise_created_at`     (`enterprise_id`, `created_at`),
  CONSTRAINT `fk_sales_order_enterprise`
    FOREIGN KEY (`enterprise_id`) REFERENCES `sys_enterprise`(`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_sales_order_customer`
    FOREIGN KEY (`customer_id`) REFERENCES `crm_customer`(`id`)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单主表';

-- ============================================================
-- 8. oms_sales_order_item — 销售订单明细表
-- 说明：销售订单中的商品明细（快照设计，下单时冻结价格/名称）
-- ============================================================
CREATE TABLE `oms_sales_order_item` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `order_id`     BIGINT        NOT NULL                 COMMENT '所属订单',
  `product_id`   BIGINT        NOT NULL                 COMMENT '关联商品',
  `product_name` VARCHAR(100)  NOT NULL                 COMMENT '商品名称（快照）',
  `spec`         VARCHAR(200)  DEFAULT NULL             COMMENT '规格（快照）',
  `unit`         VARCHAR(20)   NOT NULL                 COMMENT '单位（快照）',
  `quantity`     INT           NOT NULL                 COMMENT '购买数量',
  `price`        DECIMAL(10,2) NOT NULL                 COMMENT '单价（快照）',
  `cost_price`   DECIMAL(10,2) NOT NULL DEFAULT 0.00    COMMENT '成本价（快照）',
  `amount`       DECIMAL(12,2) NOT NULL                 COMMENT '小计金额 = price × quantity',
  `profit`       DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '小计利润 = (price - cost_price) × quantity',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`   TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_id`   (`order_id`),
  KEY `idx_product_id` (`product_id`),
  CONSTRAINT `fk_sales_item_order`
    FOREIGN KEY (`order_id`) REFERENCES `oms_sales_order`(`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_sales_item_product`
    FOREIGN KEY (`product_id`) REFERENCES `pms_product`(`id`)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单明细表';

-- ============================================================
-- 9. oms_purchase_order — 采购订单主表
-- 说明：商家向上游供应商的采购记录
-- ============================================================
CREATE TABLE `oms_purchase_order` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `order_no`       VARCHAR(30)   NOT NULL                 COMMENT '订单编号（规则：PO+年月日+4位序号）',
  `enterprise_id`  BIGINT        NOT NULL                 COMMENT '所属企业',
  `supplier_id`    BIGINT        DEFAULT NULL             COMMENT '供应商',
  `total_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0.00    COMMENT '采购总金额',
  `status`         VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '采购状态：PENDING/PURCHASING/ARRIVED/COMPLETED/CANCELLED',
  `remark`         VARCHAR(500)  DEFAULT NULL             COMMENT '备注',
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`     TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no`           (`order_no`),
  KEY `idx_enterprise_id`            (`enterprise_id`),
  KEY `idx_enterprise_status`        (`enterprise_id`, `status`),
  KEY `idx_supplier_id`              (`supplier_id`),
  CONSTRAINT `fk_purchase_order_enterprise`
    FOREIGN KEY (`enterprise_id`) REFERENCES `sys_enterprise`(`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_purchase_order_supplier`
    FOREIGN KEY (`supplier_id`) REFERENCES `crm_supplier`(`id`)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单主表';

-- ============================================================
-- 10. oms_purchase_order_item — 采购订单明细表
-- 说明：采购订单的商品明细（快照设计）
-- ============================================================
CREATE TABLE `oms_purchase_order_item` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `order_id`     BIGINT        NOT NULL                 COMMENT '所属订单',
  `product_id`   BIGINT        NOT NULL                 COMMENT '关联商品',
  `product_name` VARCHAR(100)  NOT NULL                 COMMENT '商品名称（快照）',
  `spec`         VARCHAR(200)  DEFAULT NULL             COMMENT '规格（快照）',
  `unit`         VARCHAR(20)   NOT NULL                 COMMENT '单位（快照）',
  `quantity`     INT           NOT NULL                 COMMENT '采购数量',
  `price`        DECIMAL(10,2) NOT NULL                 COMMENT '采购单价',
  `amount`       DECIMAL(12,2) NOT NULL                 COMMENT '小计金额 = price × quantity',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`   TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_order_id`   (`order_id`),
  KEY `idx_product_id` (`product_id`),
  CONSTRAINT `fk_purchase_item_order`
    FOREIGN KEY (`order_id`) REFERENCES `oms_purchase_order`(`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_purchase_item_product`
    FOREIGN KEY (`product_id`) REFERENCES `pms_product`(`id`)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单明细表';

-- ============================================================
-- 11. sys_team_member — 团队成员表
-- 说明：企业主邀请的团队成员及权限配置
-- ============================================================
CREATE TABLE `sys_team_member` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `enterprise_id` BIGINT        NOT NULL                 COMMENT '所属企业',
  `user_id`       BIGINT        NOT NULL                 COMMENT '用户ID',
  `role`          VARCHAR(20)   NOT NULL DEFAULT 'MEMBER' COMMENT '成员角色',
  `permissions`   JSON          DEFAULT NULL             COMMENT '权限配置 {"inventory":true,"order":true,"statistics":false}',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enterprise_user` (`enterprise_id`, `user_id`),
  CONSTRAINT `fk_team_enterprise`
    FOREIGN KEY (`enterprise_id`) REFERENCES `sys_enterprise`(`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_team_user`
    FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队成员表';

-- ============================================================
-- 12. sys_backup — 数据备份表
-- 说明：记录企业的数据备份操作及文件存档
-- ============================================================
CREATE TABLE `sys_backup` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `enterprise_id` BIGINT        NOT NULL                 COMMENT '所属企业',
  `file_url`      VARCHAR(500)  NOT NULL                 COMMENT 'COS 备份文件 URL',
  `file_size`     BIGINT        DEFAULT 0                COMMENT '文件大小（字节）',
  `backup_type`   VARCHAR(20)   NOT NULL DEFAULT 'FULL'  COMMENT '备份类型：FULL/PARTIAL',
  `status`        VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED' COMMENT '状态：PROCESSING/COMPLETED/FAILED',
  `remark`        VARCHAR(200)  DEFAULT NULL             COMMENT '备注',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  CONSTRAINT `fk_backup_enterprise`
    FOREIGN KEY (`enterprise_id`) REFERENCES `sys_enterprise`(`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据备份表';

-- ============================================================
-- 13. sys_printer_device — 蓝牙打印机表
-- 说明：用户绑定的蓝牙热敏打印机信息及纸宽偏好
-- ============================================================
CREATE TABLE `sys_printer_device` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `enterprise_id` BIGINT        NOT NULL                 COMMENT '所属企业',
  `user_id`       BIGINT        NOT NULL                 COMMENT '绑定用户',
  `device_id`     VARCHAR(100)  NOT NULL                 COMMENT 'BLE 设备 ID',
  `device_name`   VARCHAR(100)  DEFAULT NULL             COMMENT '设备名称',
  `paper_width`   INT           NOT NULL DEFAULT 80      COMMENT '纸张宽度：58/80（mm）',
  `is_default`    TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '是否默认打印机',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`    TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id`       (`user_id`),
  UNIQUE KEY `uk_user_device` (`user_id`, `device_id`),
  CONSTRAINT `fk_printer_enterprise`
    FOREIGN KEY (`enterprise_id`) REFERENCES `sys_enterprise`(`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_printer_user`
    FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蓝牙打印机表';

-- ============================================================
-- 14. sys_import_log — 批量导入记录表
-- 说明：记录商品批量导入操作的结果明细
-- ============================================================
CREATE TABLE `sys_import_log` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '主键',
  `enterprise_id`      BIGINT        NOT NULL                 COMMENT '所属企业',
  `user_id`            BIGINT        NOT NULL                 COMMENT '操作用户',
  `file_name`          VARCHAR(200)  NOT NULL                 COMMENT '上传的文件名',
  `file_url`           VARCHAR(500)  DEFAULT NULL             COMMENT 'COS 存档 URL',
  `total_count`        INT           NOT NULL DEFAULT 0       COMMENT '总行数',
  `success_count`      INT           NOT NULL DEFAULT 0       COMMENT '成功导入行数',
  `fail_count`         INT           NOT NULL DEFAULT 0       COMMENT '失败行数',
  `new_category_count` INT           NOT NULL DEFAULT 0       COMMENT '新建分类数',
  `status`             VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED' COMMENT 'PROCESSING/COMPLETED/FAILED',
  `error_detail`       JSON          DEFAULT NULL             COMMENT '失败详情 [{"row":3,"reason":"价格格式错误"}]',
  `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
  `updated_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`         TINYINT(1)    NOT NULL DEFAULT 0       COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_enterprise_id` (`enterprise_id`),
  CONSTRAINT `fk_import_enterprise`
    FOREIGN KEY (`enterprise_id`) REFERENCES `sys_enterprise`(`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_import_user`
    FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量导入记录表';
