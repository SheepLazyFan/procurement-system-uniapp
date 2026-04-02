/**
 * 应用全局常量配置
 * - 微信订阅消息模板 ID 等运营配置统一在此维护
 * - 修改模板 ID 时只需改此文件，无需搜索各页面
 */

// ===== 微信订阅消息模板 ID =====
// 库存预警通知（模板编号 26427：商品调仓通知）
// 字段：thing1（商品名）/ number2（当前库存）/ number3（预警阈值）/ time4（提醒时间）
export const WX_STOCK_WARNING_TEMPLATE_ID = 'your_template_id'
