/**
 * 格式化工具函数
 */
import dayjs from 'dayjs'

/**
 * 格式化金额（保留 2 位小数，千分位逗号）
 * @param {number} amount
 * @param {string} prefix - 前缀，默认 '¥'
 * @returns {string}
 */
export function formatPrice(amount, prefix = '¥') {
  if (amount == null || isNaN(amount)) return `${prefix}0.00`
  const num = Number(amount)
  return `${prefix}${num.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')}`
}

/**
 * 格式化日期时间
 * @param {string|Date} date
 * @param {string} format
 * @returns {string}
 */
export function formatDate(date, format = 'YYYY-MM-DD') {
  if (!date) return ''
  return dayjs(date).format(format)
}

/**
 * 格式化日期时间（含时分秒）
 */
export function formatDateTime(date) {
  return formatDate(date, 'YYYY-MM-DD HH:mm:ss')
}

/**
 * 格式化日期（相对时间）
 */
export function formatRelativeTime(date) {
  if (!date) return ''
  const now = dayjs()
  const target = dayjs(date)
  const diffMinutes = now.diff(target, 'minute')
  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟前`
  const diffHours = now.diff(target, 'hour')
  if (diffHours < 24) return `${diffHours}小时前`
  const diffDays = now.diff(target, 'day')
  if (diffDays < 30) return `${diffDays}天前`
  return formatDate(date)
}

/**
 * 格式化数量（整数）
 */
export function formatQuantity(num) {
  if (num == null) return '0'
  return String(Math.floor(num))
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(i > 0 ? 2 : 0)} ${units[i]}`
}

/**
 * 销售订单状态中文映射
 */
export const SALES_ORDER_STATUS = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

/**
 * 采购订单状态中文映射
 */
export const PURCHASE_ORDER_STATUS = {
  DRAFT: '草稿',
  PENDING: '待处理',
  PURCHASING: '采购中',
  ARRIVED: '已到货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

/**
 * 支付状态中文映射
 */
export const PAYMENT_STATUS = {
  UNPAID: '未支付',
  CLAIMED: '待确认收款',
  PAID: '已支付'
}

/**
 * 获取销售订单状态文字
 * @param {string} status - 订单状态
 * @param {string} [cancelBy] - 取消操作方（仅 SELLER/ADMIN 调用时传入）
 */
export function getSalesStatusText(status, cancelBy) {
  if (status === 'CANCELLED' && cancelBy) {
    if (cancelBy === 'SYSTEM') return '已超时'
    if (cancelBy === 'SALES') return '销售员已取消'
    if (cancelBy === 'BUYER') return '买家已取消'
  }
  return SALES_ORDER_STATUS[status] || status
}

export function getPurchaseStatusText(status) {
  return PURCHASE_ORDER_STATUS[status] || status
}

export function getPaymentStatusText(status) {
  return PAYMENT_STATUS[status] || status
}
