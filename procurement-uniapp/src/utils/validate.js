/**
 * 表单验证工具函数
 */

/** 手机号正则 */
const PHONE_REG = /^1[3-9]\d{9}$/

// SMS 验证码功能已废弃 — 全部使用微信授权登录

/**
 * 验证手机号
 * @param {string} phone
 * @returns {boolean}
 */
export function isValidPhone(phone) {
  return PHONE_REG.test(phone)
}

// isValidSmsCode() 已移除 — 不再使用短信验证码

/**
 * 验证价格（正数，最多两位小数）
 * @param {number|string} price
 * @returns {boolean}
 */
export function isValidPrice(price) {
  if (price == null || price === '') return false
  const num = Number(price)
  return !isNaN(num) && num > 0 && /^\d+(\.\d{1,2})?$/.test(String(price))
}

/**
 * 验证库存数量（非负整数）
 * @param {number|string} stock
 * @returns {boolean}
 */
export function isValidStock(stock) {
  if (stock == null || stock === '') return false
  const num = Number(stock)
  return Number.isInteger(num) && num >= 0
}

/**
 * 通用非空验证
 * @param {string} value
 * @returns {boolean}
 */
export function isNotEmpty(value) {
  return value != null && String(value).trim().length > 0
}

/**
 * 表单验证器
 * @param {Array} rules - [{ value, message, validator? }]
 * @returns {{ valid: boolean, message: string }}
 */
export function validate(rules) {
  for (const rule of rules) {
    if (rule.validator) {
      if (!rule.validator(rule.value)) {
        return { valid: false, message: rule.message }
      }
    } else if (!isNotEmpty(rule.value)) {
      return { valid: false, message: rule.message }
    }
  }
  return { valid: true, message: '' }
}

/**
 * 表单验证失败时弹出提示
 * @param {Array} rules
 * @returns {boolean} 是否通过验证
 */
export function validateWithToast(rules) {
  const result = validate(rules)
  if (!result.valid) {
    uni.showToast({ title: result.message, icon: 'none' })
  }
  return result.valid
}
