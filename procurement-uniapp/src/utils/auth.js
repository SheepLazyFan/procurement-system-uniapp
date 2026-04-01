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
 * 登录拦截 — 未登录则提示并跳转库存首页
 * 注：微信登录在 App.vue onLaunch 中自动完成，此处仅作安全兆底
 * @returns {boolean} 是否已登录
 */
export function requireLogin() {
  if (!isLoggedIn()) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    uni.reLaunch({ url: '/pages/inventory/index' })
    return false
  }
  return true
}
