/**
 * 认证相关工具函数
 */

/** 获取本地存储的 token */
export function getToken() {
  return uni.getStorageSync('token') || ''
}

/** 设置 token */
export function setToken(token) {
  uni.setStorageSync('token', token)
}

/** 移除 token */
export function removeToken() {
  uni.removeStorageSync('token')
}

/** 是否已登录 */
export function isLoggedIn() {
  return !!getToken()
}

/**
 * 登录拦截 — 未登录则跳转登录页
 * @param {string} redirectUrl - 登录后跳回的页面路径
 * @returns {boolean} 是否已登录
 */
export function requireLogin(redirectUrl) {
  if (!isLoggedIn()) {
    uni.navigateTo({
      url: `/pages/auth/login?redirect=${encodeURIComponent(redirectUrl || '')}`
    })
    return false
  }
  return true
}
