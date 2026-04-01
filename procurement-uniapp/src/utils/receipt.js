/**
 * 小票模板 — 将订单数据格式化为 ESC/POS 打印指令
 */
import { EscPosBuilder } from './escpos'
import { formatDateTime } from './format'

/**
 * 生成销售订单小票
 * @param {Object} order - 销售订单详情（来自 API）
 * @param {string} enterpriseName - 企业名称（小票抬头）
 * @returns {ArrayBuffer}
 */
export function buildSalesReceipt(order, enterpriseName) {
  const builder = new EscPosBuilder()

  builder.init()

  // ===== 标题 =====
  builder.alignCenter()
    .sizeDouble()
    .bold(true)
    .textLine(enterpriseName || '采购系统')
    .sizeNormal()
    .bold(false)
    .textLine('销售订单小票')
    .alignLeft()

  builder.doubleSeparator()

  // ===== 订单信息 =====
  builder.textLine('订单号: ' + (order.orderNo || ''))

  if (order.customer && order.customer.name) {
    builder.textLine('客  户: ' + order.customer.name)
  }

  if (order.deliveryAddress) {
    builder.textLine('地  址: ' + order.deliveryAddress)
  }

  builder.textLine('日  期: ' + formatDateTime(order.createdAt))

  if (order.paymentStatus) {
    builder.textLine('支  付: ' + (order.paymentStatus === 'PAID' ? '已支付' : '未支付'))
  }

  builder.separator()

  // ===== 商品明细 =====
  builder.bold(true)
    .threeColumns('商品', '数量', '金额')
    .bold(false)
  builder.separator()

  if (order.items && order.items.length) {
    for (const item of order.items) {
      const name = item.productName || ''
      const spec = item.spec ? `(${item.spec})` : ''
      const fullName = name + spec
      const qty = 'x' + (item.quantity || 0)
      const amt = '¥' + formatAmount(item.amount)
      builder.threeColumns(fullName, qty, amt)
    }
  }

  builder.separator()

  // ===== 合计（不打印利润） =====
  builder.bold(true)
    .leftRight('合计:', '¥' + formatAmount(order.totalAmount))
    .bold(false)

  builder.doubleSeparator()

  // ===== 备注 =====
  if (order.remark) {
    builder.textLine('备注: ' + order.remark)
    builder.separator()
  }

  // ===== 尾部 =====
  builder.alignCenter()
    .textLine('谢谢惠顾！')
    .alignLeft()
    .feed(4)

  return builder.build()
}

/**
 * 生成采购订单小票
 * @param {Object} order - 采购订单详情
 * @param {string} enterpriseName
 * @returns {ArrayBuffer}
 */
export function buildPurchaseReceipt(order, enterpriseName) {
  const builder = new EscPosBuilder()

  builder.init()

  // ===== 标题 =====
  builder.alignCenter()
    .sizeDouble()
    .bold(true)
    .textLine(enterpriseName || '采购系统')
    .sizeNormal()
    .bold(false)
    .textLine('采购订单小票')
    .alignLeft()

  builder.doubleSeparator()

  // ===== 订单信息 =====
  builder.textLine('订单号: ' + (order.orderNo || ''))

  if (order.supplier && order.supplier.name) {
    builder.textLine('供应商: ' + order.supplier.name)
  }

  builder.textLine('日  期: ' + formatDateTime(order.createdAt))

  builder.separator()

  // ===== 商品明细 =====
  builder.bold(true)
    .threeColumns('商品', '数量', '金额')
    .bold(false)
  builder.separator()

  if (order.items && order.items.length) {
    for (const item of order.items) {
      const name = item.productName || ''
      const spec = item.spec ? `(${item.spec})` : ''
      const fullName = name + spec
      const qty = 'x' + (item.quantity || 0)
      const amt = '¥' + formatAmount(item.amount)
      builder.threeColumns(fullName, qty, amt)
    }
  }

  builder.separator()

  // ===== 合计 =====
  builder.bold(true)
    .leftRight('合计:', '¥' + formatAmount(order.totalAmount))
    .bold(false)

  builder.doubleSeparator()

  // ===== 备注 =====
  if (order.remark) {
    builder.textLine('备注: ' + order.remark)
    builder.separator()
  }

  // ===== 尾部 =====
  builder.alignCenter()
    .textLine('采购凭证')
    .alignLeft()
    .feed(4)

  return builder.build()
}

/**
 * 生成测试打印页
 * @returns {ArrayBuffer}
 */
export function buildTestReceipt() {
  const builder = new EscPosBuilder()

  builder.init()
    .alignCenter()
    .sizeDouble()
    .bold(true)
    .textLine('打印测试')
    .sizeNormal()
    .bold(false)
    .doubleSeparator()
    .alignLeft()
    .textLine('中文测试: 采购系统蓝牙打印')
    .textLine('English: Bluetooth Printer OK')
    .textLine('数字: 0123456789')
    .textLine('符号: ¥ @ # % & * ( ) - +')
    .separator()
    .threeColumns('商品名称', '数量', '金额')
    .separator()
    .threeColumns('矿泉水500ml', 'x10', '¥20.00')
    .threeColumns('可口可乐330ml', 'x5', '¥15.00')
    .separator()
    .bold(true)
    .leftRight('合计:', '¥35.00')
    .bold(false)
    .doubleSeparator()
    .alignCenter()
    .textLine('打印机工作正常 √')
    .textLine(formatDateTime(new Date()))
    .alignLeft()
    .feed(4)

  return builder.build()
}

/**
 * 格式化金额（保留两位小数）
 */
function formatAmount(val) {
  if (val == null || isNaN(val)) return '0.00'
  return Number(val).toFixed(2)
}
