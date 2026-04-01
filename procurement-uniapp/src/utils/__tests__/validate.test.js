import { describe, it, expect, test, vi } from 'vitest'
import { isValidPhone, isValidPrice, isValidStock, isNotEmpty, validate } from '../validate.js'

/**
 * validate.js 单元测试
 * 测试范围：手机号校验、价格校验、库存校验、非空校验、多规则校验器。
 * 注意：validateWithToast 调用 uni.showToast（UniApp 全局 API），本文件不测试该函数。
 * 遵循全局规范：AAA 模式 + Should...when... 命名。
 */
describe('isValidPhone — 手机号校验', () => {
  test.each([
    // [input, expected, desc]
    ['13800138000', true,  '合法 138 开头'],
    ['17612345678', true,  '合法 176 开头'],
    ['19999999999', true,  '合法 199 开头'],
    ['12345678901', false, '不合法：1 开头但第二位为 2'],
    ['138001380',   false, '位数不足 11 位'],
    ['1380013800011', false, '位数超过 11 位'],
    ['',            false, '空字符串'],
    ['abcdefghijk', false, '纯字母'],
    ['1381234567a', false, '含字母'],
  ])('should return %s when phone is "%s"', (phone, expected) => {
    expect(isValidPhone(phone)).toBe(expected)
  })
})

describe('isValidPrice — 价格校验', () => {
  test.each([
    [1,       true,  '正整数'],
    [9.99,    true,  '小数 9.99'],
    ['29.9',  true,  '字符串小数'],
    [0,       false, '0 不合法（价格必须 > 0）'],
    [-1,      false, '负数不合法'],
    [0.001,   false, '小数超过 2 位'],
    ['abc',   false, '非数字字符串'],
    [null,    false, 'null'],
    ['',      false, '空字符串'],
    [undefined, false, 'undefined'],
  ])('should return %s when price is %s', (price, expected) => {
    expect(isValidPrice(price)).toBe(expected)
  })
})

describe('isValidStock — 库存校验', () => {
  test.each([
    [0,    true,  '0 合法（可以为 0）'],
    [1,    true,  '正整数'],
    [999,  true,  '大正整数'],
    [-1,   false, '负数不合法'],
    [3.5,  false, '小数不合法'],
    [null, false, 'null'],
    ['',   false, '空字符串'],
    ['abc', false, '字母字符串'],
  ])('should return %s when stock is %s', (stock, expected) => {
    expect(isValidStock(stock)).toBe(expected)
  })
})

describe('isNotEmpty — 非空校验', () => {
  test.each([
    ['hello',  true,  '普通字符串'],
    ['  a  ',  true,  '含空格但有实际内容'],
    ['',       false, '空字符串'],
    ['   ',    false, '纯空格'],
    [null,     false, 'null'],
    [undefined, false, 'undefined'],
    [0,        true,  '数字 0 转字符串为 "0"，非空'],
  ])('should return %s when value is "%s"', (value, expected) => {
    expect(isNotEmpty(value)).toBe(expected)
  })
})

describe('validate — 多规则表单校验', () => {
  it('should return valid=true when all rules pass', () => {
    // Arrange
    const rules = [
      { value: '商品名称', message: '商品名称不能为空' },
      { value: '10', message: '数量不能为空' },
    ]
    // Act
    const result = validate(rules)
    // Assert
    expect(result.valid).toBe(true)
    expect(result.message).toBe('')
  })

  it('should return valid=false with first failing message when a required field is empty', () => {
    // Arrange
    const rules = [
      { value: '商品名称', message: '商品名称不能为空' },
      { value: '',         message: '数量不能为空' },       // 这条失败
      { value: '10.00',    message: '价格不能为空' },
    ]
    // Act
    const result = validate(rules)
    // Assert
    expect(result.valid).toBe(false)
    expect(result.message).toBe('数量不能为空')
  })

  it('should use custom validator when provided', () => {
    // Arrange
    const rules = [
      {
        value: '-5',
        message: '价格必须大于 0',
        validator: (v) => isValidPrice(v),
      },
    ]
    // Act
    const result = validate(rules)
    // Assert
    expect(result.valid).toBe(false)
    expect(result.message).toBe('价格必须大于 0')
  })

  it('should pass when custom validator returns true', () => {
    // Arrange
    const rules = [
      {
        value: '13800138000',
        message: '手机号格式不正确',
        validator: (v) => isValidPhone(v),
      },
    ]
    // Act
    const result = validate(rules)
    // Assert
    expect(result.valid).toBe(true)
  })

  it('should return valid=true when rules array is empty', () => {
    // Arrange & Act
    const result = validate([])
    // Assert
    expect(result.valid).toBe(true)
  })
})
