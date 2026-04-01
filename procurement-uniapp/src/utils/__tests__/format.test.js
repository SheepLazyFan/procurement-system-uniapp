import { describe, it, expect, test } from 'vitest'
import {
  formatPrice, formatDate, formatDateTime, formatQuantity, formatFileSize,
  getSalesStatusText, getPurchaseStatusText, getPaymentStatusText
} from '../format.js'

/**
 * format.js 单元测试
 * 测试范围：金额格式化、日期格式化、数量格式化、文件大小格式化、状态文字映射。
 * 遵循全局规范：AAA 模式 + Should...when... 命名。
 */
describe('formatPrice — 金额格式化', () => {
  // 表格驱动测试（Table-Driven）
  test.each([
    [null,      '¥0.00',       'null 应返回 ¥0.00'],
    [undefined, '¥0.00',       'undefined 应返回 ¥0.00'],
    [NaN,       '¥0.00',       'NaN 应返回 ¥0.00'],
    [0,         '¥0.00',       '0 应返回 ¥0.00'],
    [1234.5,    '¥1,234.50',   '1234.5 应格式化为千分位带两位小数'],
    [0.01,      '¥0.01',       '最小精度 0.01 应正确展示'],
    [999999.99, '¥999,999.99', '大数字应带千分位'],
    [10,        '¥10.00',      '整数应补两位小数'],
  ])('should return %s when input is %s', (input, expected) => {
    // Arrange — 输入已由矩阵提供
    // Act
    const result = formatPrice(input)
    // Assert
    expect(result).toBe(expected)
  })

  it('should use custom prefix when prefix is provided', () => {
    // Arrange
    const amount = 100
    const prefix = '$'
    // Act
    const result = formatPrice(amount, prefix)
    // Assert
    expect(result).toBe('$100.00')
  })

  it('should return ¥0.00 when amount is empty string', () => {
    // Arrange & Act
    const result = formatPrice('')
    // Assert
    expect(result).toBe('¥0.00')
  })
})

describe('formatDate — 日期格式化', () => {
  it('should return empty string when date is null', () => {
    // Arrange & Act
    const result = formatDate(null)
    // Assert
    expect(result).toBe('')
  })

  it('should return empty string when date is undefined', () => {
    expect(formatDate(undefined)).toBe('')
  })

  it('should format date string in YYYY-MM-DD by default', () => {
    // Arrange
    const dateStr = '2026-03-15T10:30:00'
    // Act
    const result = formatDate(dateStr)
    // Assert
    expect(result).toBe('2026-03-15')
  })

  it('should format date with custom format', () => {
    // Arrange
    const dateStr = '2026-03-15T10:30:00'
    // Act
    const result = formatDate(dateStr, 'MM/DD/YYYY')
    // Assert
    expect(result).toBe('03/15/2026')
  })
})

describe('formatDateTime — 日期时间格式化', () => {
  it('should return empty string when date is null', () => {
    expect(formatDateTime(null)).toBe('')
  })

  it('should format to YYYY-MM-DD HH:mm:ss', () => {
    // Arrange
    const dateStr = '2026-03-15T10:30:45'
    // Act
    const result = formatDateTime(dateStr)
    // Assert
    expect(result).toBe('2026-03-15 10:30:45')
  })
})

describe('formatQuantity — 数量格式化', () => {
  test.each([
    [null,    '0',   'null 应返回 0'],
    [0,       '0',   '0 应返回 0'],
    [5,       '5',   '整数 5 应返回 5'],
    [3.7,     '3',   '小数 3.7 应取整为 3'],
    [9.99,    '9',   '小数 9.99 应取整为 9'],
    ['10',    '10',  '字符串数字应正确处理'],
  ])('should return "%s" when input is %s', (input, expected) => {
    expect(formatQuantity(input)).toBe(expected)
  })
})

describe('formatFileSize — 文件大小格式化', () => {
  test.each([
    [0,           '0 B',      '0 字节'],
    [null,        '0 B',      'null'],
    [1023,        '1023 B',   '< 1KB'],
    [1024,        '1.00 KB',  '恰好 1KB'],
    [1048576,     '1.00 MB',  '恰好 1MB'],
    [1073741824,  '1.00 GB',  '恰好 1GB'],
    [1536,        '1.50 KB',  '1.5KB'],
  ])('should return "%s" when bytes is %s', (input, expected) => {
    expect(formatFileSize(input)).toBe(expected)
  })
})

// ===========================================================
// 新增：状态文字映射测试（含 BUG-3 验证）
// ===========================================================

describe('getSalesStatusText — 销售订单状态文字', () => {
  test.each([
    ['PENDING',   undefined,  '待确认'],
    ['CONFIRMED', undefined,  '已确认'],
    ['SHIPPED',   undefined,  '已发货'],
    ['COMPLETED', undefined,  '已完成'],
    ['CANCELLED', undefined,  '已取消'],
    ['CANCELLED', 'BUYER',    '买家已取消'],
    ['CANCELLED', 'SALES',    '销售员已取消'],
    ['CANCELLED', 'SYSTEM',   '已超时'],
  ])('should return "%s" for status=%s cancelBy=%s', (status, cancelBy, expected) => {
    expect(getSalesStatusText(status, cancelBy)).toBe(expected)
  })

  it('should return "已取消" when cancelBy is MERCHANT (default label)', () => {
    // MERCHANT 没有特殊标签，走默认 SALES_ORDER_STATUS
    expect(getSalesStatusText('CANCELLED', 'MERCHANT')).toBe('已取消')
  })
})

describe('getPurchaseStatusText — 采购订单状态文字', () => {
  test.each([
    ['DRAFT',      '草稿'],
    ['PURCHASING', '采购中'],
    ['ARRIVED',    '已到货'],
    ['COMPLETED',  '已完成'],
    ['CANCELLED',  '已取消'],
    ['UNKNOWN',    'UNKNOWN'],
  ])('should return "%s" for status=%s', (status, expected) => {
    expect(getPurchaseStatusText(status)).toBe(expected)
  })
})

describe('getPaymentStatusText — 支付状态文字', () => {
  test.each([
    ['UNPAID', '未支付'],
    ['PAID',   '已支付'],
    ['OTHER',  'OTHER'],
  ])('should return "%s" for status=%s', (status, expected) => {
    expect(getPaymentStatusText(status)).toBe(expected)
  })
})
