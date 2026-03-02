# 采购系统微信小程序 — 原子化开发任务清单 (TASKS)

> **文档版本**：v1.0  
> **创建日期**：2026-03-01  
> **关联文档**：[PRD v1.4](./PRD.md), [ARCHITECTURE v1.0](./ARCHITECTURE.md)  
> **文档状态**：Phase One 第三部分

---

## 1. 项目初始化 (Project Setup)
### 1.1 后端初始化 (Spring Boot)
- [ ] 1.1.1 使用 Spring Initializr 创建 `procurement-server`，引入 Spring Web, Spring Security, MyBatis-Plus, MySQL Driver, Redis 依赖。
- [ ] 1.1.2 配置 `application-dev.yml`，设置 MySQL 和 Redis 连接信息及服务端口 (8080)。
- [ ] 1.1.3 配置 `MyBatisPlusConfig.java`，添加分页插件 (`PaginationInnerInterceptor`) 和自动填充处理器 (处理 `created_at` / `updated_at`)。
- [ ] 1.1.4 配置 `CorsConfig.java`，实现全局跨域资源共享配置。
- [ ] 1.1.5 配置 `Knife4jConfig.java`，集成 Swagger 在线接口文档。
- [ ] 1.1.6 编写基础响应包装类 `R.java`、状态码枚举 `ResultCode.java` 及全局异常处理 `GlobalExceptionHandler.java`。

### 1.2 前端初始化 (uni-app)
- [ ] 1.2.1 使用 Vite 模板创建 `procurement-uniapp` 项目 (Vue3 + TS/JS 语法)。
- [ ] 1.2.2 安装并配置 `pinia` 用于状态管理，在 `main.js` 中全局注册。
- [ ] 1.2.3 引入 `uni-ui` 组件库并配置 `easycom` 自动导入。
- [ ] 1.2.4 在 `src/api/request.js` 中封装 `uni.request`，实现统一域名、Token 自动注入请求头及全局异常拦截。
- [ ] 1.2.5 编写全局 SCSS 样式变量 `uni.scss` 适配主题色（如采购主题的蓝色/绿色）。
- [ ] 1.2.6 根据 ARCHITECTURE.md 配置 `pages.json`，包括所有页面路由注册、NavigationBar 颜色及 TabBar 结构 (库存、采购订单、销售订单、统计、我的)。

---

## 2. 数据库建模与基础 (Database Schema)
- [ ] 2.1 编写 `init.sql`，定义通用实体字段，创建 `sys_user` 用户表。
- [ ] 2.2 在 `init.sql` 中创建 `sys_enterprise` 企业表及关联约束。
- [ ] 2.3 在 `init.sql` 中创建商品模块表：`pms_category` (分类) 和 `pms_product` (商品)。
- [ ] 2.4 在 `init.sql` 中创建订单模块主子表：`oms_sales_order`、`oms_sales_order_item`、`oms_purchase_order`、`oms_purchase_order_item`。
- [ ] 2.5 在 `init.sql` 中创建 CRM 模块表：`crm_customer` (客户) 和 `crm_supplier` (供应商)。
- [ ] 2.6 在 `init.sql` 中创建扩展功能表：`sys_team_member`、`sys_backup`、`sys_printer_device`、`sys_import_log`。
- [ ] 2.7 运行脚本初始化本地开发数据库 `procurement_db`。
- [ ] 2.8 使用 MyBatis-Plus Generator (或手动) 针对上述表生成基础的 Entity 类。

---

## 3. 认证与账户模块 (Auth Module)
### 3.1 后端认证开发
- [ ] 3.1.1 编写 `UserMapper` 及 `UserService` 基础 CRUD，测试数据库连接。
- [ ] 3.1.2 编写 `JwtTokenProvider.java`，实现 JWT 的生成(Sign)、解析与有效性验证。
- [ ] 3.1.3 编写 `JwtAuthFilter.java`，拦截请求从 Header 提取 Bearer Token 并注入 Spring Security。
- [ ] 3.1.4 编写 `SecurityConfig.java`，配置路由白名单（放行 `/api/auth/**` 和静态资源），禁用 Session。
- [ ] 3.1.5 实现 `AuthController.sendSms()`，生成 6 位验证码存入 Redis 设置 5 分钟过期时间并打印至控制台(后续接入腾讯云)。
- [ ] 3.1.6 实现 `AuthController.login()`，验证手机号及 Redis 验证码；若新用户自动注册并关联企业状态，签发 JWT。
- [ ] 3.1.7 实现 `AuthController.wxLogin()`，调用微信 API `code2Session` 获取 OpenID 并完成买家免密登录机制。

### 3.2 前端认证开发
- [ ] 3.2.1 开发 `src/pages/auth/login.vue`，实现手机号输入校验、获取验证码倒计时(60s)。
- [ ] 3.2.2 在 `src/api/auth.js` 编写短信和登录的网络请求。
- [ ] 3.2.3 在 `src/store/user.js` 中实现 action，处理登录后将 Token 和 userInfo 存入 `uni.setStorageSync`。
- [ ] 3.2.4 在登录页完成 API 对接，登录成功后跳转到 TabBar 首页。

---

## 4. 企业及我的页面 (Profile & Enterprise)
### 4.1 后端企业管理开发
- [ ] 4.1.1 编写 `EnterpriseMapper` 及 `EnterpriseService` 基础实现。
- [ ] 4.1.2 实现 `EnterpriseController.createEnterprise()`，新建记录，并更新当前登录用户的 `enterprise_id`。
- [ ] 4.1.3 实现获取及更新当前企业详情接口 `/api/enterprise`。

### 4.2 前端"我的"页面开发
- [ ] 4.2.1 开发 `src/pages/profile/index.vue`，订阅 userStore 判断企业状态，分别渲染"未建企业"提示面板或"已建企业"功能列表。
- [ ] 4.2.2 开发 `src/pages/profile/create-enterprise.vue`，实现填写企业名称、联系人等表单及提交逻辑。
- [ ] 4.2.3 开发 `src/pages/profile/enterprise.vue` 编辑页，加载详情数据并支持修改更新。

---

## 5. 商品与库存模块 (Inventory & Product)
### 5.1 后端商品分类开发
- [ ] 5.1.1 实现 `CategoryController` 的 CRUD 接口，在查询和操作时均强制附加当前用户的 `enterprise_id`。
- [ ] 5.1.2 编写 `CategoryService.deleteCategory()`，加入业务校验：检查该分类下是否还有关联的 `pms_product`，有则拒绝删除。

### 5.2 后端商品管理开发
- [ ] 5.2.1 实现 `ProductController` 分页接口 `getProducts()`，结合 MyBatis-Plus 提供按名字模糊搜索、按分类筛选功能。
- [ ] 5.2.2 实现添加和更新商品接口 `addProduct()` / `updateProduct()`，进行 JSR-303 参数格式校验。
- [ ] 5.2.3 实现原子化库存调整接口 `adjustStock()`，使用原生 SQL 或 Wrapper 增加/扣减库存。
- [ ] 5.2.4 集成腾讯云 COS，实现 `FileController.upload()` 统一文件上传接口，用于处理商品图片。
- [ ] 5.2.5 基于 EasyExcel 引入 Excel 读取能力，实现 `ProductController.batchImport()`，自动排查或新建分类并插入商品。

### 5.3 前端商品库存开发
- [ ] 5.3.1 开发 `src/pages/inventory/category.vue`，实现可编辑列表组件，进行分类的新增、改名、拖拽/按钮排序。
- [ ] 5.3.2 开发可复用组件 `src/components/product/ProductCard.vue` 渲染单条商品展示。
- [ ] 5.3.3 开发 `src/pages/inventory/index.vue` 主页，集成顶部搜索栏、分类 Tab 滚动切换以及加载商品列表功能。
- [ ] 5.3.4 开发 `src/pages/inventory/product-form.vue` 发布页，实现带有多图上传功能的表单及分类下拉选取 (`CategorySelector.vue`)。
- [ ] 5.3.5 开发 `src/pages/inventory/batch-import.vue`，实现文件选择、前端预览解析表并高亮错误行，调用后端提交导入。

---

## 6. CRM：客户与供应商模块
### 6.1 后后端 CRM 开发
- [ ] 6.1.1 编写 `CustomerController` 实现 `crm_customer` 表的增删改查。
- [ ] 6.1.2 编写 `SupplierController` 实现 `crm_supplier` 表的增删改查。

### 6.2 前端 CRM 开发
- [ ] 6.2.1 开发 `src/pages/sales/customer-list.vue` 及对应项的搜索查询。
- [ ] 6.2.2 开发 `src/pages/sales/add-customer.vue`，提供新建客户视图。
- [ ] 6.2.3 开发 `src/pages/purchase/supplier-list.vue` 及 `supplier-detail.vue`。

---

## 7. 采购订单模块 (Purchase Order)
### 7.1 后端采购逻辑
- [ ] 7.1.1 实现 `PurchaseOrderService.createOrder()`，组装订单主表 (生成规则编号 PO+日期序列) 及持久化商品快照到 `oms_purchase_order_item`。
- [ ] 7.1.2 实现订单到货流转逻辑 `PurchaseOrderService.arriveOrder()`，状态转为 ARRIVED 时，遍历明细自动给相应商品增加库存。
- [ ] 7.1.3 编写联合查询 Mapper (支持连表查出 Item 列表) 以实现采购订单详情及列表 API。

### 7.2 前端采购界面
- [ ] 7.2.1 开发复用组件 `OrderCard.vue` 适配订单布局。
- [ ] 7.2.2 开发 `src/pages/purchase/index.vue`，使用分段器 (Segmented Control) 切换“全部/待采购/已到货”状态，查询并渲染列表。
- [ ] 7.2.3 开发 `src/pages/purchase/quick-purchase.vue` 开单页，结合库存选择弹窗拉取商品，输入进价与数量并提交。
- [ ] 7.2.4 开发 `src/pages/purchase/detail.vue` 详情页，允许点击操作按钮更新订单状态。

---

## 8. 销售订单与买家端 (Sales & Buyer)
### 8.1 后端销售逻辑
- [ ] 8.1.1 实现 `SalesOrderService.createOrder()`，持久化销售记录，计算商品单价和总价。
- [ ] 8.1.2 实现销售订单完成逻辑 `SalesOrderService.completeOrder()`，状态为 COMPLETED 时遍历明细扣减库存，若库存不足返回 409 冲突响应。
- [ ] 8.1.3 实现只读权限的 `BuyerController` 接口：获取门店信息、门店商品及供买家提交订单的公开/鉴权端点。

### 8.2 前端销售界面
- [ ] 8.2.1 开发 `src/pages/sales/index.vue`，展示销售清单并可通过状态进行 Tab 过滤。
- [ ] 8.2.2 开发 `src/pages/sales/create-order.vue` (手工开单)，实现客户选择和商品添加逻辑。
- [ ] 8.2.3 开发 `src/pages/sales/detail.vue`，展示详情并支持改变状态为已发货、完成。

### 8.3 前端买家专属链路 (微信分享入口)
- [ ] 8.3.1 在 `src/pages/inventory/index.vue` 实现微信自带的 `onShareAppMessage` 钩子，将企业 ID 作为分享参数植入路由。
- [ ] 8.3.2 开发 `src/pages/buyer/store.vue` 免登浏览页，渲染分享进入后商家的商品卡片。
- [ ] 8.3.3 开发 `src/pages/buyer/cart.vue` 购物车及 `checkout.vue` 提交订单页面。在此处嵌入微信授权登录按钮 (`getUserProfile`) 进行用户绑定及"伪支付"体验。

---

## 9. 统计与报表模块 (Statistics)
### 9.1 后端统计接口
- [ ] 9.1.1 在 `StatisticsMapper.xml` 编写聚合 SQL，计算当前用户的当日/当月销售额、总利润及当前库存成本汇总。
- [ ] 9.1.2 编写 GROUP BY 查询，计算低于库存预警值的商品数量以及各分类商品库存占比。
- [ ] 9.1.3 编写按日期维度的销量与利润折线图数据接口 (返回结构如 `[ {date: '2026-03-01', amount: 5000} ]`)。

### 9.2 前端统计图表
- [ ] 9.2.1 开发 `src/pages/statistics/index.vue` 数据大屏，利用卡片显示四大核心指标数据。
- [ ] 9.2.2 引入 `ucharts` 组件并在统计页将其渲染为销售/利润趋势折线图。
- [ ] 9.2.3 在 `src/pages/inventory/statistics.vue` 开发库存分析页面，用饼图展示品类分布。

---

## 10. 蓝牙热敏打印机集成 (Bluetooth Print)
- [ ] 10.1 开发 `src/utils/bluetooth.js` 工具：封装 `uni.openBluetoothAdapter`、`uni.startBluetoothDevicesDiscovery`、连接及监听服务特征值功能。
- [ ] 10.2 开发 `src/utils/escpos.js` 工具：编写热敏打印机的 ESC/POS 指令流转换器（包含文字放大、居中、表格左中右排版、切纸等指令转 GBK ArrayBuffer）。
- [ ] 10.3 开发 `src/pages/profile/printer.vue` 打印机中心：搜寻周围 BLE 设备，选择后通过 `sys_printer_device` 存入服务端作默认绑定。
- [ ] 10.4 在订单详情页面 `detail.vue` 增加“打印订单”浮层：获取详情后通过上述工具自动排布商家名、订单明细、总价，循环执行底层特征值 `write` 实现小票出纸。

---

## 11. 团队权限与数据备份 (Team & Backup) (P1/P2)
- [ ] 11.1 实现 `TeamController` 及团队表映射，提供根据邀请码自动分配 Enterprise ID 的能力。
- [ ] 11.2 前端增加 `src/pages/profile/team.vue`，展示员工列表与邀请码展示。
- [ ] 11.3 开发 `BackupController` 触发导出机制（基于 MySQLDump 或业务表数据打包），生成归档文件上传至 COS 后返回链接供商家留存。

---

> **所有原子级任务已列出。可以进入 Phase Two 进行系统架构搭建和代码骨架的实现了。**