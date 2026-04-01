# 🛒 采购管理系统 (Procurement System)

> 面向小微商户的一站式采购管理微信小程序 — 从进货、销售到库存预警，全流程数字化。

---

## 项目简介

采购管理系统是一个基于 **微信小程序 + Spring Boot** 的 SaaS 化采购管理平台，帮助小微商户解决：

- 📦 **商品 & 库存管理** — 多分类商品维护、库存预警（微信订阅消息通知）
- 🛍️ **采购订单** — 供应商管理、采购开单、到货入库
- 💰 **销售订单** — 商家开单 + 买家小程序下单双入口、利润自动核算
- 👥 **团队协作** — SELLER / ADMIN / SALES / WAREHOUSE 四级 RBAC 权限
- 🛒 **买家端** — 独立二维码入口，商品浏览、下单、订单追踪

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
| **构建** | Maven 3.9 + JDK 21 (后端) · Vite + HBuilderX (前端) |

## 快速开始

### 后端

```bash
cd procurement-server
# 1. 初始化数据库
mysql -u root -p < sql/schema.sql
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
│   ├── src/test/             # JUnit 测试 (77 cases)
│   ├── sql/                  # 数据库脚本
│   └── scripts/              # 部署/备份脚本
├── procurement-uniapp/       # UniApp 微信小程序前端
│   ├── src/pages/            # 页面组件
│   ├── src/api/              # API 请求封装
│   ├── src/store/            # Pinia 状态管理
│   └── src/components/       # 公共组件
├── dev-rules/                # 开发规范文档
├── dev-logs/                 # 研发日志
└── prompt/                   # 需求/测试/TODO 文档
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
# 运行全部 77 个 JUnit 测试
./mvnw test
# 运行指定测试
./mvnw test -Dtest=BuyerServiceImplTest
```

| 测试类型 | 用例数 | 状态 |
|---------|--------|------|
| JUnit 后端 | 77 | ✅ |
| Vitest 前端 | 39 | ✅ |
| API 接口 | 96 | ✅ |

## 部署

当前部署在腾讯云轻量服务器，详见 [procurement-server/README.md](procurement-server/README.md) 中的"生产部署"章节。

## License

Private — 未经授权不得使用、复制或分发。
