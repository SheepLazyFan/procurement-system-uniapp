# 📱 采购系统小程序前端 (procurement-uniapp)

基于 UniApp (Vue 3) 开发的微信小程序前端，包含商家端和买家端。

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| UniApp | 3.0.0-408 | 跨端框架 |
| Vue 3 | 3.4.21 | 响应式 UI |
| Pinia | 2.1.7 | 状态管理 |
| uni-ui | 1.5.6 | UI 组件库 |
| Sass | 1.71.1 | CSS 预处理 |
| Vite | 5.2.8 | 构建工具 |
| Vitest | 4.1.0 | 单元测试（39 用例）|
| dayjs | 1.11.10 | 日期处理 |

---

## 开发环境搭建

### 前提条件

- Node.js ≥ 18
- [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)

### 安装与运行

```bash
# 安装依赖
npm install

# 开发模式（微信小程序）
npm run dev:mp-weixin

# 开发模式（H5 浏览器调试）
npm run dev:h5
```

编译完成后，用微信开发者工具打开:
```
dist/dev/mp-weixin
```

---

## 环境变量

通过 `.env.*` 文件配置，Vite 编译时注入：

| 文件 | 用途 | 内容 |
|------|------|------|
| `.env.development` | 开发/调试 | `VITE_API_BASE=http://your-server-ip:8080/api` |
| `.env.production` | 生产构建 | `VITE_API_BASE=https://your-domain.com/api` |

> ⚠️ **上线前**必须将 `.env.production` 中的 `VITE_API_BASE` 改为 HTTPS 域名。

### 环境变量使用方式

```
.env.* → VITE_API_BASE
  ↓ vite.config.js → define: { __API_BASE__: ... }
    ↓ src/api/request.js → const BASE_URL = __API_BASE__
```

---

## 项目结构

```
procurement-uniapp/
├── src/
│   ├── api/                # API 请求封装
│   │   └── request.js      # 统一请求拦截器（JWT 注入、401 处理、静默重登）
│   ├── components/         # 公共组件
│   ├── pages/
│   │   ├── auth/           # 登录页
│   │   ├── inventory/      # 📦 库存管理（首页）
│   │   ├── purchase/       # 🛍️ 采购订单
│   │   ├── sales/          # 💰 销售订单
│   │   ├── buyer/          # 🛒 买家端（商品浏览/下单）
│   │   ├── profile/        # 👤 个人中心
│   │   └── statistics/     # 📊 数据统计
│   ├── store/
│   │   └── user.js         # Pinia 用户状态（登录、silentWxLogin）
│   ├── static/             # 静态资源（图标等）
│   ├── App.vue             # 应用入口
│   ├── main.js             # 初始化
│   └── pages.json          # 路由配置
├── .env.development        # 开发环境变量
├── .env.production         # 生产环境变量
├── vite.config.js          # Vite 构建配置
├── vitest.config.js        # 测试配置
└── package.json
```

---

## 构建发布

### 微信小程序

```bash
# 1. 生产构建
npm run build:mp-weixin

# 2. 用微信开发者工具打开
#    路径: dist/build/mp-weixin

# 3. 在微信开发者工具中点击"上传"
#    填写版本号和备注
```

### H5 (可选)

```bash
npm run build:h5
# 输出目录: dist/build/h5
```

---

## 测试

```bash
# 运行全部测试（39 用例）
npm test

# 监听模式
npm run test:watch

# 覆盖率报告
npm run test:coverage
```

---

## 页面模块说明

| 模块 | 路径 | 说明 |
|------|------|------|
| 登录 | `pages/auth/` | 微信授权一键登录 |
| 库存 | `pages/inventory/` | 商品 CRUD、分类管理、库存调整、低库存预警 |
| 采购 | `pages/purchase/` | 采购订单管理、供应商管理 |
| 销售 | `pages/sales/` | 销售开单、订单状态流转 |
| 买家 | `pages/buyer/` | 扫码进入、商品浏览、下单、订单追踪 |
| 统计 | `pages/statistics/` | 销售/采购/利润统计 |
| 个人 | `pages/profile/` | 企业信息、团队管理、数据备份 |

---

## 多角色测试

小程序支持 4 种角色。开发调试时通过切换数据库 `wx_openid` 实现身份切换：

| 角色 | 权限 |
|------|------|
| SELLER | 全部功能（企业主） |
| ADMIN | 商品/订单/库存管理 |
| SALES | 仅销售订单 |
| WAREHOUSE | 仅库存管理 |

```sql
-- 切换当前微信号为 ADMIN 身份
UPDATE sys_user SET wx_openid = 'backup_seller' WHERE id = 1;
UPDATE sys_user SET wx_openid = '<你的openid>' WHERE id = 2;
```
