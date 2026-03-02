# TODO — 采购系统待办清单

> 本文档整合前后端所有待修复 BUG、已知风险、待实现功能与需人工干预事项。  
> 生成时间：2025-01-XX（Phase 3 编码完成后）  
> 状态标记：🔴 BUG | 🟡 RISK | 🔵 TODO | 🟢 IMPROVEMENT | ⚪ DECISION（需人工决策）

---

## 一、需要与客户/人工确认的事项（⚪ DECISION / EXTERNAL）

### 1.1 Excel 批量导入格式（⚪ 待客户确认）
- **位置**：后端 `ProductServiceImpl.batchImport()` + 前端 `batch-import.vue`
- **现状**：后端返回空结果并打印"暂未实现"；前端 `handleImport()` 仅做 Toast 提示
- **所需信息**：客户确认 Excel 列头格式、必填字段、数据校验规则
- **待完成**：后端解析逻辑、前端预览/校验 UI、模板下载文件

### 1.2 权限设置功能（⚪ 待产品决策）
- **位置**：前端 `team.vue` — `setPermission()` 为 Toast 占位
- **所需信息**：团队成员权限粒度（哪些功能需要细分权限？只读/读写？模块级别还是操作级别？）
- **待完成**：后端 `TeamController` 增加权限管理接口、前端权限设置 UI

### 1.3 蓝牙打印机硬件适配（⚪ 需实际设备）
- **位置**：前端 `printer.vue` — `scanDevices()` / `connectDevice()` / `testPrint()` 均为 Toast 占位
- **所需信息**：目标打印机型号、SDK、ESC/POS 指令集
- **待完成**：uni-app 蓝牙 API 对接、打印模板设计、设备配对流程

### 1.4 数据备份与恢复方案（⚪ 待架构决策）
- **位置**：后端 `BackupServiceImpl.create()` / `restore()` — 均为 TODO 桩代码
- **所需信息**：备份格式（SQL dump / JSON / Excel）、存储位置（本地 / COS）、恢复策略（全量 / 增量）
- **待完成**：实际数据导出逻辑、文件上传至 COS、恢复事务处理

### 1.5 库存预警阈值规则（⚪ 待产品决策）
- **位置**：前端 `inventory/index.vue` 无预警列表页面；后端无预警推送逻辑
- **所需信息**：预警阈值是全局还是按商品设置？通知方式（小程序消息 / 短信）？
- **待完成**：后端预警检查定时任务、前端预警列表页

---

## 二、外部服务对接（🔵 TODO — 需配置凭证）

| # | 服务 | 现状 | 所需配置 |
|---|------|------|----------|
| 2.1 | **微信小程序登录** | `AuthServiceImpl.wxLogin()` 中 `wx.login` 调用已实现，但需真实 AppID/Secret 才能换取 openid | `application.yml` → `wx.appid`, `wx.secret` |
| 2.2 | **腾讯云短信** | `SmsServiceImpl` 仅控制台打印验证码 | 恢复 `pom.xml` SMS SDK 依赖 + 配置 `SecretId/SecretKey/SmsSdkAppId/SignName/TemplateId` |
| 2.3 | **腾讯云 COS** | `FileServiceImpl.upload()` 代码已完成 | `application.yml` → `cos.secretId`, `cos.secretKey`, `cos.region`, `cos.bucketName` |
| 2.4 | **微信订阅消息** | 前端 `requestSubscribeMessage` 未实现 | 微信公众平台申请模板 ID + 后端推送接口 |

---

## 三、后端 BUG（🔴 需立即修复）

### 3.1 销售订单取消 — 库存双倍恢复
- **位置**：`SalesOrderServiceImpl.cancel()`
- **问题**：取消操作中库存恢复在状态检查之前执行。若对已取消订单并发调用 `cancel()`，库存会被恢复两次
- **修复**：将 `status == CANCELLED` 检查移至方法最前，在任何库存操作之前

### 3.2 采购订单取消 — 同样的双倍恢复问题
- **位置**：`PurchaseOrderServiceImpl.cancel()`
- **问题**：与 3.1 完全相同的 BUG — 状态检查在库存操作之后
- **修复**：将状态检查移至方法最前

### 3.3 买家下单 — 库存检查非原子性
- **位置**：`BuyerServiceImpl.createOrder()`
- **问题**：创建订单时检查库存但不扣减（订单为 PENDING 状态时库存未锁定），两个买家可同时通过库存检查，最终只有一个能完成
- **修复建议**：要么在创建时预扣库存（乐观锁），要么移除创建时的库存检查仅在完成时检查，并明确告知用户

### 3.4 团队加入 — 覆盖已有企业
- **位置**：`TeamServiceImpl.joinByInviteCode()`
- **问题**：用户已属于企业 A，使用邀请码加入企业 B 时，直接覆盖 `enterpriseId`。无已有企业归属检查
- **修复**：加入前检查用户是否已有企业，若有则拒绝或要求先退出

---

## 四、后端风险项（🟡 RISK — 建议尽快处理）

### 4.1 订单号生成 — 并发冲突（高优先级）
- **位置**：`SalesOrderServiceImpl.generateOrderNo()` / `PurchaseOrderServiceImpl.generateOrderNo()` / `BuyerServiceImpl.createOrder()`
- **问题**：使用 `selectCount` + 格式化生成订单号，并发请求可能生成重复订单号
- **修复**：改用 Redis 原子自增 `INCR order:no:20250101` 或数据库序列

### 4.2 登录注册 — 并发重复用户（高优先级）
- **位置**：`AuthServiceImpl.login()` / `wxLogin()`
- **问题**：两个并发请求使用同一手机号/openid 登录时，自动注册逻辑可创建两条用户记录
- **修复**：在 `sys_user` 表添加 `UNIQUE` 索引 (`phone`, `wx_openid`) + 捕获 `DuplicateKeyException`

### 4.3 短信接口无频率限制
- **位置**：`AuthServiceImpl.sendSmsCode()`
- **问题**：无每分钟/每小时发送限制，攻击者可耗尽短信额度
- **修复**：Redis `SETNX sms:limit:{phone}` 设置 60s 过期时间

### 4.4 无角色权限控制（高优先级）
- **位置**：`SecurityConfig`
- **问题**：所有认证用户（SELLER / BUYER / MEMBER）可访问所有接口。BUYER 可调用卖家管理 API
- **修复**：添加 `.requestMatchers("/buyer/**").hasRole("BUYER")` + `.requestMatchers("/sales-orders/**", ...).hasAnyRole("SELLER", "MEMBER")` 等

### 4.5 邀请码碰撞
- **位置**：`EnterpriseServiceImpl.generateInviteCode()`
- **问题**：6 位 hex ≈ 1677 万种组合，无唯一性校验
- **修复**：添加 `UNIQUE` 索引 + 碰撞重试循环

### 4.6 企业创建 — 并发重复
- **位置**：`EnterpriseServiceImpl.create()`
- **问题**：并发请求可为同一用户创建多个企业
- **修复**：`enterprise` 表 `owner_id` 添加 `UNIQUE` 索引

### 4.7 采购到货 — 库存调整未校验
- **位置**：`PurchaseOrderServiceImpl.arrive()`
- **问题**：`adjustStock()` 返回值未检查，商品被删除时库存增加静默失败
- **修复**：检查 `productMapper.adjustStock()` 返回值

### 4.8 库存统计 — NullPointerException
- **位置**：`StatisticsServiceImpl.getInventoryStats()`
- **问题**：`getCategoryId()` 为 null 时 `Collectors.groupingBy` 抛 NPE
- **修复**：分组前过滤 null 或使用 `Optional` 默认值

### 4.9 文件上传 — 无类型/大小校验
- **位置**：`FileServiceImpl.upload()`
- **问题**：任意文件类型均可上传（.exe .jsp 等），仅依赖 Spring 配置限制大小
- **修复**：白名单校验扩展名（jpg/png/xlsx 等） + 服务层大小检查

### 4.10 团队成员移除 — 可移除企业所有者
- **位置**：`TeamServiceImpl.removeMember()`
- **问题**：无检查是否为企业所有者，若移除所有者将导致企业成为孤儿
- **修复**：添加 `member.userId != enterprise.ownerId` 检查

### 4.11 无 CORS 配置
- **位置**：`SecurityConfig`
- **问题**：未配置跨域。小程序不需要 CORS，但 H5 端或调试时会被浏览器拦截
- **修复**：添加 `CorsConfigurationSource` Bean

---

## 五、前端 BUG（🔴 需立即修复）

### 5.1 创建销售订单 — 商品选择不可用
- **位置**：`create-order.vue` — `searchProducts()` 仅做 Toast 提示
- **影响**：整个销售订单创建流程不可用
- **修复**：实现商品搜索弹窗，调用 `productApi.getProducts()` 获取商品列表，支持选择商品并填入订单项

### 5.2 快速采购 — 商品搜索和供应商选择不可用
- **位置**：`quick-purchase.vue` — `searchProducts()` 为 Toast 占位；供应商选择器 `<uni-data-select>` 已注释
- **影响**：快速采购核心流程不可用
- **修复**：实现商品搜索 + 取消注释供应商选择器并绑定数据

### 5.3 买家商品详情 — API 参数错误
- **位置**：`buyer/product-detail.vue` — `buyerApi.getProductDetail(enterpriseId, productId)` 传入 2 个参数，但 `getProductDetail()` 函数只接受 1 个参数
- **影响**：商品详情页 API 调用失败
- **修复**：确认后端接口签名，修正前端调用参数

### 5.4 买家订单详情 — 无导航入口
- **位置**：`buyer/orders.vue` — 列表项无点击事件跳转到订单详情
- **影响**：买家无法查看订单详细信息
- **修复**：为订单项添加 `@click` 事件，跳转到 `/pages/buyer/order-detail`

---

## 六、前端功能缺失（🔵 TODO — 需编码实现）

### 6.1 商品图片上传
- **位置**：`product-form.vue` — `uploadImage()` 为 Toast 占位
- **影响**：无法为商品添加图片
- **修复**：调用 `uni.chooseImage()` + `fileApi.upload()` + 将返回 URL 赋值给表单

### 6.2 客户编辑/删除
- **位置**：`customer-detail.vue` — 无编辑/删除按钮
- **影响**：客户信息创建后无法维护
- **修复**：添加编辑表单 + 删除确认，调用 `customerApi.updateCustomer()` / `deleteCustomer()`

### 6.3 供应商添加
- **位置**：`supplier-list.vue` — `addSupplier()` 为 Toast 占位
- **影响**：无法添加新供应商
- **修复**：创建供应商表单页面或弹窗，调用 `supplierApi.createSupplier()`

### 6.4 供应商编辑/删除
- **位置**：`supplier-detail.vue` — 无编辑/删除按钮
- **影响**：供应商信息创建后无法维护
- **修复**：添加编辑表单 + 删除确认，调用 `supplierApi.updateSupplier()` / `deleteSupplier()`

### 6.5 买家订单支付
- **位置**：`buyer/orders.vue` — `payBuyerOrder()` API 函数从未被调用
- **影响**：买家无法标记订单为已支付
- **修复**：在订单详情页添加支付按钮，调用 `buyerApi.payBuyerOrder()`

### 6.6 统计图表
- **位置**：`statistics/index.vue` — 图表区域使用文字占位 "图表区域"
- **影响**：无法可视化展示销售趋势
- **修复**：集成 `ucharts` 或 `echarts` 图表库，渲染折线图/柱状图

### 6.7 买家店铺分类筛选
- **位置**：`buyer/store.vue` — 分类选项卡已渲染但无过滤逻辑
- **影响**：切换分类无效果
- **修复**：在 `onCategoryChange()` 中传递 `categoryId` 参数给商品列表查询

### 6.8 库存调整 UI
- **位置**：`inventory/index.vue` — 无库存调整入口（手动调库存）
- **影响**：库存只能通过订单流转改变
- **修复**：添加库存调整弹窗或页面，调用 `productApi.adjustStock()`

### 6.9 商品表单 — 已有图片展示和删除
- **位置**：`product-form.vue` — 编辑模式下不展示已有商品图片，无删除图片按钮
- **修复**：回显已有图片列表 + 添加删除按钮

---

## 七、后端性能优化（🟢 IMPROVEMENT — 建议优化）

### 7.1 N+1 查询问题
以下 Service 的 `toResponse()` 方法在列表查询时对每条记录单独查关联数据：

| 位置 | 额外查询 | 修复建议 |
|------|----------|----------|
| `SalesOrderServiceImpl.toResponse()` | customer + items × N | JOIN 查询或批量加载 |
| `PurchaseOrderServiceImpl.toResponse()` | supplier + items × N | 同上 |
| `ProductServiceImpl.toResponse()` | category × N | 同上 |
| `CustomerServiceImpl.toResponse()` | 全量 orders × N | SQL COUNT/SUM 聚合 |
| `SupplierServiceImpl.toResponse()` | 全量 purchase orders × N | SQL COUNT/SUM 聚合 |
| `StatisticsServiceImpl.getCustomerRanking()` | customer × N | JOIN 查询 |

### 7.2 统计全量内存聚合
- **位置**：`StatisticsServiceImpl` — `getOverview()` / `getSalesTrend()` / `getProductRanking()` / `getCustomerRanking()`
- **问题**：加载全部订单到 Java 内存做聚合，数据量大时 OOM
- **修复**：改用 SQL `GROUP BY` / `SUM` / `COUNT` 聚合查询

### 7.3 分类排序批量更新
- **位置**：`CategoryServiceImpl.batchSort()`
- **问题**：逐条 `selectById` + `updateById`，N 个分类产生 2N 次数据库往返
- **修复**：使用 MyBatis-Plus `updateBatchById` 或自定义批量 SQL

---

## 八、前端代码质量（🟢 IMPROVEMENT）

### 8.1 异常处理缺失
- 多处页面 `catch(e) {}` 静默吞掉错误，无用户提示
- **涉及页面**：`create-order.vue`, `quick-purchase.vue`, `product-form.vue`, `batch-import.vue`, `customer-detail.vue`, `supplier-detail.vue`, `buyer/store.vue`, `buyer/product-detail.vue` 等
- **修复**：`catch(e) { uni.showToast({ title: '操作失败', icon: 'none' }) }`

### 8.2 未使用的 API 函数清理
以下 API 函数已定义但前端页面中从未调用：

| API 模块 | 函数 | 说明 |
|----------|------|------|
| `auth.js` | `refreshToken` | 无自动刷新机制 |
| `enterprise.js` | `updateEnterprise`, `getEnterpriseStatistics` | 无编辑企业 UI |
| `product.js` | `getProductDetail`, `adjustStock`, `importTemplate` | 调用缺失 |
| `salesOrder.js` | `updateSalesOrder`, `getSalesOrderDetail` | 编辑/详情未对接 |
| `purchaseOrder.js` | `updatePurchaseOrder`, `getPurchaseOrderDetail` | 同上 |
| `customer.js` | `updateCustomer`, `deleteCustomer`, `getCustomerDetail` | 同上 |
| `supplier.js` | `updateSupplier`, `deleteSupplier` | 同上 |
| `team.js` | `updateMemberRole`, `getTeamStatistics` | 权限管理未实现 |
| `buyer.js` | `getStoreCategories`, `payBuyerOrder` | 分类筛选/支付未对接 |
| `statistics.js` | `getCustomerRanking` | 客户排名未渲染 |
| `backup.js` | `restoreBackup`, `deleteBackup`, `downloadBackup` | 备份功能桩代码 |

### 8.3 加载状态与空状态
- 多数列表页缺少 loading 骨架屏 和 空数据提示组件
- **修复**：uni-ui `<uni-load-more>` + 自定义空状态组件

### 8.4 占位字符串替换
- 部分页面仍有开发阶段的硬编码文本（如 `'邀请码已复制'` 但实际未对接剪贴板 API）
- **修复**：逐一检查并替换为真实功能

---

## 九、数据库约束补充（🟡 建议添加）

以下 UNIQUE 索引建议在 DDL 中补充以防止并发数据异常：

```sql
-- 防止重复用户
ALTER TABLE sys_user ADD UNIQUE INDEX uk_phone (phone);
ALTER TABLE sys_user ADD UNIQUE INDEX uk_wx_openid (wx_openid);

-- 防止重复企业
ALTER TABLE enterprise ADD UNIQUE INDEX uk_owner_id (owner_id);

-- 防止邀请码碰撞
ALTER TABLE enterprise ADD UNIQUE INDEX uk_invite_code (invite_code);

-- 防止订单号重复
ALTER TABLE sales_order ADD UNIQUE INDEX uk_order_no (order_no);
ALTER TABLE purchase_order ADD UNIQUE INDEX uk_order_no (order_no);
```

---

## 十、部署前必要配置（🔵 部署阶段）

| # | 配置项 | 说明 |
|---|--------|------|
| 10.1 | MySQL 生产环境 | 连接地址、用户名、密码、连接池参数 |
| 10.2 | Redis 生产环境 | 连接地址、密码 |
| 10.3 | HTTPS / SSL 证书 | 微信小程序要求后端必须使用 HTTPS |
| 10.4 | 域名备案 | 小程序后端域名需 ICP 备案 |
| 10.5 | JWT Secret | 生产环境更换 `jwt.secret`，不得使用默认值 |
| 10.6 | Spring Profile | `application-prod.yml` 关闭 debug 日志、SQL 打印 |
| 10.7 | 微信小程序审核 | 提交代码审核、配置服务器域名白名单 |

---

## 修复优先级建议

### P0 — 阻塞核心流程（立即修复）✅ 已全部修复
1. ~~[3.1] 销售订单取消双倍库存恢复~~ ✅ 状态检查已移至库存操作之前
2. ~~[3.2] 采购订单取消双倍库存恢复~~ ✅ 验证：原代码已正确（状态检查在前）
3. ~~[4.4] 无角色权限控制（BUYER 可调用卖家 API）~~ ✅ SecurityConfig 已添加 hasAnyRole 规则
4. ~~[5.1] 创建销售订单 — 商品选择不可用~~ ✅ 已实现商品搜索弹窗 + 添加逻辑
5. ~~[5.2] 快速采购 — 核心流程不可用~~ ✅ 已实现商品搜索弹窗 + 供应商选择弹窗
6. ~~[5.3] 买家商品详情 API 参数错误~~ ✅ 已修正为 getProductDetail(productId)

### P1 — 数据安全（尽快修复）✅ 已全部修复
7. ~~[4.1] 订单号并发冲突~~ ✅ 改用 Redis 原子自增（SalesOrder/PurchaseOrder/Buyer 三处）
8. ~~[4.2] 登录并发重复用户~~ ✅ 捕获 DuplicateKeyException（login + wxLogin）
9. ~~[4.3] 短信无频率限制~~ ✅ Redis SETNX 60s 限流 + SecureRandom
10. ~~[3.4] 团队加入覆盖已有企业~~ ✅ 加入前检查 user.enterpriseId
11. ~~[4.9] 文件上传无类型校验~~ ✅ 扩展名白名单 + 10MB 文件大小限制
12. ~~[4.10] 可移除企业所有者~~ ✅ removeMember 增加 ownerId 检查
13. ~~[九] 数据库唯一索引~~ ✅ init.sql 已包含 + 代码层 DuplicateKeyException 兜底 + 邀请码碰撞重试

### P2 — 功能完善（正常迭代）
14. [6.1] 商品图片上传
15. [6.2] 客户编辑/删除
16. [6.3] 供应商添加
17. [6.4] 供应商编辑/删除
18. [6.5] 买家订单支付
19. [6.6] 统计图表
20. [6.7] 分类筛选
21. [6.8] 库存调整 UI
22. [5.4] 买家订单详情导航

### P3 — 性能与质量（优化迭代）
23. [7.1] N+1 查询
24. [7.2] 统计全量内存聚合
25. [8.1] 异常处理
26. [8.2] 清理未使用 API
27. [8.3] 加载与空状态

### P4 — 待决策/外部依赖
28. [1.1] Excel 批量导入（待客户确认）
29. [1.2] 权限设置（待产品决策）
30. [1.3] 蓝牙打印机（需硬件）
31. [1.4] 数据备份方案（待架构决策）
32. [1.5] 库存预警规则（待产品决策）
33. [二] 外部服务凭证配置
