# 🛒 采购管理系统 (Procurement System)

---

## 项目简介

采购管理系统是一个基于 **微信小程序 + Spring Boot** 的采购管理工具，当前更适合用于小微商户数字化改造、试点商用和定制交付，而不是直接作为通用 SaaS 平台大规模售卖，但是后期一定会发展成Saas平台。

- 📦 **商品 & 库存管理** — 多分类商品维护、库存预警（微信订阅消息通知）
- 🛍️ **采购订单** — 供应商管理、采购开单、到货入库
- 💰 **销售订单** — 商家开单 + 买家小程序下单双入口、利润自动核算
- 👥 **团队协作** — SELLER / ADMIN / SALES / WAREHOUSE 四级 RBAC 权限
- 🛒 **买家端** — 独立二维码入口，商品浏览、下单、订单追踪

## 商业定位

- **目标客户** — `200-3000 SKU`、`1-10 人团队`、高度依赖微信沟通与线下收款的夫妻店/小批发/社区供货商
- **最强卖点** — 把“微信接单 + Excel 记库存 + 手工开单”收拢到一个小程序中
- **差异能力** — 买家端扫码下单、团队权限、库存预警、蓝牙打印、数据备份
- **推荐路径** — 先做 `定制交付 / 试点商用`，验证真实客户是否持续使用，再决定是否继续标准化 SaaS

## 商业化文档

- [商业定位与交付策略](prompt/markdown/business-positioning.md)
- [试点验证计划](prompt/markdown/pilot-validation-plan.md)
- [初始化数据迁移清单](prompt/markdown/data-migration-checklist.md)
- [商户操作手册](prompt/markdown/merchant-operator-manual.md)
- [异常恢复预案](prompt/markdown/incident-recovery-runbook.md)
- [标准导入模板 CSV](deploy/templates/product-import-template.csv)

## 架构概览

```
┌─────────────────────────────────────────┐
│         微信小程序 (UniApp/Vue 3)        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│  │  商家端   │ │  买家端   │ │  库存端   │ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ │
└───────┼────────────┼────────────┼───────┘
        │            │            │
        ▼            ▼            ▼
┌─────────────────────────────────────────┐
│     Spring Boot 3.2.3 REST API          │
│  ┌──────┐ ┌──────┐ ┌───────┐ ┌───────┐ │
│  │ Auth │ │Order │ │Product│ │ Team  │ │
│  │(JWT) │ │(Sale │ │(Stock)│ │(RBAC) │ │
│  │      │ │ /Buy)│ │       │ │       │ │
│  └──┬───┘ └──┬───┘ └───┬───┘ └───┬───┘ │
└─────┼────────┼─────────┼────────┼──────┘
      │        │         │        │
      ▼        ▼         ▼        ▼
┌──────────┐  ┌──────────┐  ┌──────────┐
│ MySQL 8  │  │  Redis   │  │ 微信 API │
│ (14表)   │  │ (缓存/ID)│  │(登录/通知)│
└──────────┘  └──────────┘  └──────────┘
```

## 技术栈

| 层级 | 技术 |
|------|------|
| **前端** | UniApp (Vue 3 + Pinia) → 微信小程序 |
| **后端** | Spring Boot 3.2.3 + Spring Security + MyBatis-Plus 3.5.5 |
| **数据库** | MySQL 8.0 (14 表) + Redis 6.x |
| **认证** | 微信授权登录 → JWT (JJWT) |
| **部署** | 腾讯云轻量 Ubuntu 22.04 + systemd |
| **构建** | Maven 3.9 + JDK 21 (后端) · UniApp CLI + Vite + 微信开发者工具 (前端) |

## 快速开始

### 后端

```bash
cd procurement-server
# 1. 初始化数据库
mysql -u root -p < sql/init.sql
# 2. 启动（dev profile）
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# API: http://localhost:8080/api
# Swagger: http://localhost:8080/api/swagger-ui.html
```

详见 [procurement-server/README.md](procurement-server/README.md)

### 前端

```bash
cd procurement-uniapp
npm install
npm run dev:mp-weixin
# 用微信开发者工具打开 dist/dev/mp-weixin
```

详见 [procurement-uniapp/README.md](procurement-uniapp/README.md)

## 项目结构

```
procurement-system/
├── procurement-server/       # Spring Boot 后端
│   ├── src/main/java/        # Java 源码
│   ├── src/main/resources/   # 配置文件 (application*.yml)
│   ├── src/test/             # JUnit 测试 (98 cases)
│   ├── sql/                  # 数据库脚本
│   └── scripts/              # 部署/备份脚本
├── procurement-uniapp/       # UniApp 微信小程序前端
│   ├── src/pages/            # 页面组件
│   ├── src/api/              # API 请求封装
│   ├── src/store/            # Pinia 状态管理
│   └── src/components/       # 公共组件
├── deploy/templates/         # 客户交付模板（导入模板、初始化资料）
├── dev-rules/                # 开发规范文档
├── dev-logs/                 # 研发日志
└── prompt/                   # 需求/测试/商业化文档
```

## API 示例

### 微信登录

```
POST /api/auth/wx-login
Content-Type: application/json

{ "code": "0b3k..." }
```

**成功响应** (200)：
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOi...",
    "userInfo": {
      "id": 1,
      "role": "SELLER",
      "enterpriseId": 1,
      "enterpriseName": "XX 商贸"
    }
  }
}
```

### 获取商品列表（商家端）

```
GET /api/products?pageNum=1&pageSize=20
Authorization: Bearer <token>
```

### 买家端浏览商品

```
GET /api/buyer/store/{enterpriseId}/products
```

> 完整 API 文档：开发环境启动后访问 `http://localhost:8080/api/swagger-ui.html`

## 测试

```bash
cd procurement-server
# 运行全部 98 个 JUnit 测试
./mvnw test
# 运行指定测试
./mvnw test -Dtest=BuyerServiceImplTest
```

| 测试类型 | 用例数 | 状态 |
|---------|--------|------|
| JUnit 后端 | 98 | ✅ |
| Vitest 前端 | 87 | ✅ |
| API 接口 | 96 | ✅ |

## 部署

当前部署在腾讯云轻量服务器，详见 [procurement-server/README.md](procurement-server/README.md) 中的"生产部署"章节。

当前 HTTP 开发阶段额外建议先遵守这两条：

- 上线前先核对 [生产部署与 SQL 升级检查清单](/e:/WorkSpace/DevSpace/procurement system uniapp---600/prompt/sessions/temp/20260407/production_deployment_sql_upgrade_checklist.md)
- 后端启动现在会主动检查关键增量字段，发现数据库没跑 SQL 会直接拒绝启动，而不是等到运行期再出现登录异常或页面假空态

## 项目移植配置更改表

> **场景**：将项目部署到新的服务器 / 新的企业微信小程序账号时，必须逐项检查并更改以下配置。

### 一、后端环境变量（`/opt/procurement/.env`）

| # | 变量名 | 配置文件 | 必填 | 当前值来源 | 迁移操作 |
|---|--------|---------|------|-----------|---------|
| 1 | `JWT_SECRET` | `application-prod.yml` | ✅ | 服务器 `.env` | 新服务器执行 `openssl rand -hex 32` 重新生成 |
| 2 | `WX_APP_ID` | `application.yml:63` | ✅ | 微信公众平台 → 开发管理 → AppID | 替换为新小程序的 AppID |
| 3 | `WX_APP_SECRET` | `application.yml:64` | ✅ | 微信公众平台 → 开发管理 → AppSecret | 替换为新小程序的 AppSecret |
| 4 | `WX_STOCK_WARNING_TEMPLATE_ID` | `application.yml:67` | ⚠️ | 微信公众平台 → 订阅消息 → 模板ID | 新小程序需重新申请「商品调仓通知」模板 |
| 5 | `WX_BACKUP_ALERT_TEMPLATE_ID` | `application.yml:69` | 否 | 同上（可选） | 如需备份告警则申请新模板 |
| 6 | `DB_HOST` | `application-prod.yml:6` | 否 | 默认 `localhost` | 如 MySQL 非本机则填写 |
| 7 | `DB_PORT` | `application-prod.yml:6` | 否 | 默认 `3306` | 非标准端口时填写 |
| 8 | `DB_USERNAME` | `application-prod.yml:7` | 否 | 默认 `root` | 建议新建专用用户 |
| 9 | `DB_PASSWORD` | `application-prod.yml:8` | ✅ | 无默认值（Fail-Fast） | 填写新数据库密码 |
| 10 | `REDIS_HOST` | `application-prod.yml:18` | 否 | 默认 `localhost` | 非本机则填写 |
| 11 | `REDIS_PORT` | `application-prod.yml:19` | 否 | 默认 `6379` | 非标准端口时填写 |
| 12 | `REDIS_PASSWORD` | `application-prod.yml:20` | 否 | 默认空 | Redis 有密码时填写 |
| 13 | `COS_SECRET_ID` | `application.yml:90` | ⚠️ | 腾讯云控制台 → API密钥管理 | 替换为客户的腾讯云密钥 |
| 14 | `COS_SECRET_KEY` | `application.yml:91` | ⚠️ | 同上 | 替换为客户的腾讯云密钥 |
| 15 | `COS_BUCKET` | `application.yml:93` | ⚠️ | 腾讯云 COS 控制台 | 替换为客户的 Bucket 名称 |
| 16 | `CORS_ALLOWED_ORIGINS` | `application-prod.yml:56` | 否 | 默认 `https://servicewechat.com` | 通常无需改 |

### 二、前端配置（需修改文件后重新编译）

| # | 配置项 | 文件路径 | 迁移操作 |
|---|--------|---------|---------|
| 17 | `VITE_API_BASE` | `procurement-uniapp/.env.production` | 改为新服务器域名：`https://新域名.com/api` |
| 18 | `VITE_API_BASE` | `procurement-uniapp/.env.development` | 改为新开发服务器 IP（可选） |
| 19 | `WX_STOCK_WARNING_TEMPLATE_ID` | `procurement-uniapp/src/config/index.js` | 与后端 #4 保持一致，替换为新模板 ID |
| 20 | 微信小程序 AppID | 微信开发者工具 → 项目配置 → AppID | 替换为新小程序的 AppID |

### 三、代码级硬编码值（需改代码后重新编译/打包）

| # | 配置项 | 文件路径 | 当前值 | 迁移操作 |
|---|--------|---------|--------|---------|
| 21 | `miniprogram_state` | `WxSubscribeMessageServiceImpl.java:72,139` | `"developer"` | 正式上线前改为 `"formal"` |
| 22 | COS `region` | `application.yml:92` | `ap-guangzhou` | 若客户 COS 不在广州区则修改 |

### 四、服务器基础设施检查

| # | 检查项 | 操作 |
|---|--------|------|
| 23 | 服务器时区 | `timedatectl set-timezone Asia/Shanghai`（影响 9AM 定时任务） |
| 24 | MySQL 初始化 | 执行 `mysql -u root -p < sql/init.sql` |
| 25 | systemd 服务注册 | 参考 `procurement-server/README.md` 的"生产部署"章节 |
| 26 | 防火墙规则 | 开放 22(SSH) / 80(HTTP) / 443(HTTPS) / 8080(API) |
| 27 | Nginx + HTTPS | 绑定域名 + SSL 证书 + 反向代理（微信审核前必须） |
| 28 | 微信后台域名白名单 | 微信公众平台 → 开发管理 → 服务器域名，添加 `https://新域名.com` |
| 29 | crontab 备份 | `30 3 * * * /opt/procurement/scripts/mysql-auto-backup.sh` |

### 五、迁移执行顺序（推荐）

```
1. 新服务器装机 → JDK 21 / MySQL 8 / Redis / Nginx
2. 执行 sql/init.sql 初始化数据库
3. 创建 /opt/procurement/.env 填写 #1~#16 所有变量
4. 部署 JAR + 注册 systemd 服务
5. 修改前端 .env.production (#17) 和 config/index.js (#19)
6. 微信开发者工具换 AppID (#20) → npm run build:mp-weixin → 上传
7. 配置 Nginx HTTPS (#27) + 微信域名白名单 (#28)
8. 代码中 miniprogram_state 改为 "formal" (#21) → 重新打包部署
9. 提交微信审核 → 发布正式版
```

> 详细部署步骤见 [procurement-server/README.md](procurement-server/README.md) 和 [procurement-uniapp/README.md](procurement-uniapp/README.md)

---

## License

有意者或疑问者，联系作者：2529174839@qq.com
