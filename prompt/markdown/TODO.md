# TODO — 采购系统待办清单

> 初始生成：2026-03-02（Phase 3 编码完成后）  
> 最后更新：2026-04-01（Phase 4 测试补全完成 + T-12 Token 修复 + Phase 5-B 文档启动）  
> 状态标记：🔴 必须 | 🔵 TODO | 🟢 优化 | ⚪ 待决策 | ✅ 已完成

> **当前部署状态**：个人腾讯云测试环境（IP `106.52.136.176`）| 待迁移至客户腾讯云 | 未提交微信小程序平台审核 | 未上架

---

## 项目总览

| 阶段 | 状态 | 备注 |
|------|------|------|
| Phase 1（规划与需求分析） | ✅ 完成 | PRD v1.4、ARCHITECTURE v1.0、TASKS v1.0 |
| Phase 2（系统架构与数据建模） | ✅ 完成 | 14 张表 DDL、71 Java 文件、64 Vue 组件 |
| Phase 3（编码与实现） | ✅ 完成 | 31 项任务 + 43 项累计修复 |
| Phase 4（测试与验证） | ✅ 完成 | API 96/96 · Vitest 39/39 · JUnit 77/77 ✅ · BUG 3/3 修复 · P1 3/3 · P2 4/4 · T-01~T-13 补充测试 |
| Phase 5（部署与运维） | ⚠️ 进行中 | 体验版 v0.9 已上传 · 文档生成中 · 域名注册中 · HTTPS 待备案后配置 |

**服务器**：腾讯云轻量（2 核 2G 3Mbps）| IP `106.52.136.176` | Ubuntu 22.04  
**技术栈**：Spring Boot 3.2.3 + MyBatis-Plus 3.5.5 + JDK 21 | UniApp (Vue 3) → 微信小程序  
**部署**：`/opt/procurement/` | systemd 自启 | MySQL 8 + Redis

---

## 一、待完成事项

### 🔴 迁移到客户腾讯云前

| # | 事项 | 状态 | 说明 |
|---|------|------|------|
| 1.7 | MySQL 定时自动备份 | ⚠️ | 脚本已有 `scripts/mysql-auto-backup.sh`，仅差 crontab 注册 |
| 1.8 | 腾讯云 COS 真实凭证 | ⚠️ | 替换占位符，迁移时配置客户 COS bucket |
| 1.11 | README.md | ⚠️ 进行中 | Phase 5-B：根目录 + 后端 + 前端 3 份 README |
| 1.21 | 客户服务器环境初始化 | ⬜ | JDK 21、MySQL 8（含 `pms_product_supplier` 表）、Redis、systemd、防火墙 |
| 1.22 | 测试数据清理 | ⬜ | 迁移前清空测试数据，或以全新数据库交付 |

### 🔴 提交微信审核前

| # | 事项 | 状态 | 说明 |
|---|------|------|------|
| 1.3 | HTTPS / SSL 证书 | 🔴 | 域名备案通过后：腾讯云免费 SSL + Nginx 反代 |
| 1.4 | 域名注册 + ICP 备案 | ⚠️ 进行中 | 域名实名模板审核中 → 注册域名 → ICP 备案（7-14 天）|
| 1.23 | `miniprogram_state` 改 `"formal"` | 🔴 | 当前 `"developer"`，正式发布前必改 |
| 1.24 | 微信后台服务器域名白名单 | 🔴 | 开发管理 → 服务器域名（备案通过后配置）|
| 1.5 | 微信小程序代码审核 | 🔴 | 体验版 v0.9 已上传 → 备案后提交审核 |

### 🔵 上线后尽快完成

| # | 事项 | 状态 | 说明 |
|---|------|------|------|
| 1.10 | CI/CD Pipeline | 🔵 | GitHub Actions → SCP jar → `systemctl restart` |
| 1.25 | Phase 4 测试补全 | ✅ 完成 | JUnit 77/77 全绿（T-01~T-13 含 RBAC + 低库存阈值）|
| 1.26 | 备份告警通知 | 🔵 | cron 失败时推微信通知 |

### 🟢 后续优化

| # | 事项 | 说明 |
|---|------|------|
| 1.16 | 结构化日志 | JSON 格式，便于腾讯云日志服务接入 |
| 1.17 | E2E 测试 | 买家完整下单流程 / 多角色权限回归 |
| 1.18 | 运维手册 | 客户交付后的 DevOps 操作指南 |
| 1.19 | 多身份切换 | 同一微信号在多企业间切换（远期 B2B） |
| 1.20 | 企业小程序码 | `getWXACodeUnlimit` API，正式发布后开发 |
| 1.28 | Pinterest 级深度重构 | Phase 20-22：云顶天穹层 + 悬空孤岛布局 |

---

## 二、部署配置清单

| 配置项 | 个人测试云 | 客户云（目标） |
|--------|-----------|---------------|
| MySQL 8 | ✅ | ⬜ 待初始化 |
| Redis | ✅ | ⬜ 待初始化 |
| Spring Boot prod profile | ✅ | ⬜ 待配置 |
| systemd + 开机自启 | ✅ | ⬜ 待配置 |
| 防火墙 | ✅ 22/80/8080 | ⬜ 需加 443 |
| HTTPS / Nginx | ❌ | 🔴 审核前必须 |
| 域名 + ICP 备案 | ❌ | 🔴 审核前必须 |
| JWT Secret | ✅ `.env` | ⬜ 新服务器重新生成 |
| 微信凭据 | ✅ | ⬜ 可复用 |
| `miniprogram_state` | ⚠️ `"developer"` | 🔴 改 `"formal"` |
| COS 凭证 | ⚠️ 占位符 | 🔵 配置客户 bucket |
| 前端 API 地址 | ✅ 当前 IP | ⬜ 改客户域名 |
| 服务器域名白名单 | ❌ | 🔴 审核前必须 |
| MySQL cron 备份 | ❌ | 🔵 脚本已有 |

---

## 三、Phase 4 测试进度（最终：2026-04-01）

| 测试类型 | 项数 | 状态 |
|---------|------|------|
| 前端 Vitest | 39/39 | ✅ 全部通过 |
| API 接口 + RBAC | 96/96 | ✅ 全部通过 |
| JUnit 后端单元测试 | 77/77 | ✅ 全部通过（含 T-01~T-13 补充测试）|
| BUG 修复验证 | 22/22 | ✅ 全部通过 |
| BUG 修复 | 3/3 | ✅ BUG-1(库存)/BUG-2(采购取消)/BUG-3(超时标记) |
| 前端手动测试 | 48/55 | ✅ 87.3% 通过（5 失败已修复、1 跳过、1 未测） |
| P1 问题修复 | 3/3 | ✅ F-003(描述保存)/F-005(状态显示)/D-006(RBAC) |
| P2 短期优化 | 4/4 | ✅ F-001(下拉刷新)/F-004(二维码)/D-008(分类换行)/QR保存 |
| 讨论项实施 | 4/4 | ✅ D-004(库存阈值)/D-005(调整权限)/D-011(取消方)/D-012(付款QR) |
| T-12 Token 过期修复 | ✅ | silentWxLogin 自动刷新 + 失败兜底跳登录页 |

> 详见 `dev-logs/20260327.md`、`dev-logs/20260329.md`

---

## 四、历史追溯

> 从 Phase 1 (2026-03-01) 至今的完整研发日志保存在 `dev-logs/*.md`

| 日期 | 里程碑 |
|------|--------|
| 03-11 | 安全体系、CORS 防护、CI/CD 服务自启 |
| 03-13 | 全域库存警告闭环 + 微信通知触达 |
| 03-15 | 基础单元测试 116 条全部通过 |
| 03-16 | SELLER/SALES/WAREHOUSE 三权分立拦截器 |
| 03-26 | 前端 SaaS 化改造 + Pinterest 级 UI 重构启航 |
| 03-27 | Phase 4 测试验证（135 项通过）、3 BUG 修复、自动化测试补充 |
| 03-29 | P1 修复 + P2 优化 + 讨论项实施（D-004/005/011/012） |
| 03-31 | T-01~T-07 低库存阈值 JUnit 参数化测试 |
| 04-01 | T-08~T-11 RBAC 权限测试、T-12 Token 修复、77 测试全绿、Phase 5-B 启动 |

---

## 附录 D：数据库迁移说明

### D.1 全新环境
执行 `procurement-server/sql/init.sql`（含全量建表 + 迁移补丁），无需额外操作。

### D.2 已部署环境增量升级
```sql
-- 补丁 1：库存预警通知开关
ALTER TABLE `sys_user`
  ADD COLUMN IF NOT EXISTS `notify_stock_warning` TINYINT(1) NOT NULL DEFAULT 0
  COMMENT '库存预警通知开关' AFTER `last_login_at`;

-- 补丁 2：供应商-商品关联表（2026-03-15）
CREATE TABLE IF NOT EXISTS `pms_product_supplier` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT,
  `enterprise_id` BIGINT NOT NULL,
  `product_id`    BIGINT NOT NULL,
  `supplier_id`   BIGINT NOT NULL,
  `supply_price`  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `is_default`    TINYINT(1) NOT NULL DEFAULT 0,
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`    TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_supplier` (`product_id`, `supplier_id`),
  KEY `idx_enterprise_supplier` (`enterprise_id`, `supplier_id`),
  KEY `idx_enterprise_product`  (`enterprise_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 附录 B：多身份测试方案

**买家**：微信开发者工具 → 编译模式 → 启动页 `pages/buyer/store` → 参数 `enterpriseId=1`  
**员工**：交换 `wx_openid` 切换身份（user_id 2/3/4 → ADMIN/SALES/WAREHOUSE）

```sql
UPDATE sys_user SET wx_openid = 'backup_seller' WHERE id = 1;
UPDATE sys_user SET wx_openid = 'or1dT17iWQkPNXbGIh1zQEZCHX6A' WHERE id = 2;  -- 切到ADMIN
```

---

## 附录 C：条件触发的优化项

| 内容 | 触发条件 |
|------|----------|
| `pms_product.stock` INT→DECIMAL | 需按重量计量时 |
| 权限模型细粒度扩展 | 需操作级权限控制时 |
| `sys_audit_log` 审计日志表 | 审计合规需求时 |
| `pms_category` 多级分类 | 品类复杂度增长时 |
| WebSocket 实时通知 | 需即时推送时 |
| 统计图表增强 | 需更丰富可视化时 |
