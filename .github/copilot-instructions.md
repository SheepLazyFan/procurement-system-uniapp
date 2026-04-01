# Testing Guidelines — 全局测试规范

## 技术栈
- **后端**：JUnit 5 + Mockito 5（`spring-boot-starter-test` 内置）
- **前端**：Vitest + @vue/test-utils

## 1. 命名规范
测试描述采用 `"Should [预期结果] when [输入条件]"` 格式。

```java
// 正确
@Test void should_returnMemberRole_when_newUserRegisters() {}
// 正确
@Test void should_throwStockInsufficient_when_stockIsZero() {}
```

```js
// 正确
it('should return ¥0.00 when amount is null')
it('should return false when phone is empty')
```

## 2. 隔离性 — Mock 所有外部依赖
- 后端：每个 Service 单元测试使用 `@ExtendWith(MockitoExtension.class)`，**绝不启动 Spring 上下文**。
- 数据库、Redis、第三方 API（微信）全部 Mock。
- 前端：Mock 所有 `uni.*` API 和 HTTP 请求。

## 3. AAA 模式（必须）
所有测试代码必须分段为三部分：

```java
// Arrange — 准备数据和 Mock
// Act     — 调用被测方法
// Assert  — 验证结果
```

## 4. 表格驱动测试（Table-Driven Tests）
对有多种输入组合的函数，使用参数化测试：

```java
@ParameterizedTest
@MethodSource("provideStockCases")
void should_handleStock_correctly(int input, boolean expected) { ... }
```

```js
test.each([
  [null, '¥0.00'],
  [0, '¥0.00'],
  [1234.5, '¥1,234.50'],
])('formatPrice(%s) should return %s', (input, expected) => { ... })
```

## 5. 红绿循环（TDD 强制）
- 写测试时：只写测试代码，确保测试**先失败**（Red）
- 写实现时：只写让测试通过的最小代码（Green）
- 重构时：代码行为不变，现有测试必须保持通过（Refactor）

## 6. 关键业务规则必须有测试保护
以下规则每次修改后必须验证测试通过：
- 新用户默认角色为 `MEMBER`（非 `SELLER`）
- 库存不足时抛出 `STOCK_INSUFFICIENT`（40902）
- 订单状态非法流转时抛出 `ORDER_STATUS_ERROR`（40907）
- 企业数据隔离（跨 enterpriseId 访问返回 NOT_FOUND）
- 价格快照（订单明细金额 = 下单时商品单价 × 数量）
