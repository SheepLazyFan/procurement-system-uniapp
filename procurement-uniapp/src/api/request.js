/**
 * uni.request 统一封装
 * - JWT 自动注入
 * - 统一错误处理
 * - 请求/响应拦截
 */

// 后端 API 基础地址（开发环境）
const BASE_URL = 'http://localhost:8080/api'

// 无需 token 的白名单路径
const WHITE_LIST = [
  '/auth/sms/send',
  '/auth/login',
  '/auth/wx-login',
  '/buyer/store',
  '/buyer/product'
]

/**
 * 判断路径是否在白名单中
 */
function isWhiteListed(url) {
  return WHITE_LIST.some(path => url.includes(path))
}

/**
 * 核心请求函数
 * @param {Object} options - 请求配置
 * @param {string} options.url - 请求路径（不含 BASE_URL）
 * @param {string} options.method - 请求方法
 * @param {Object} options.data - 请求体 / Query 参数
 * @param {Object} options.header - 额外请求头
 * @param {boolean} options.showLoading - 是否显示 loading（默认 false）
 * @param {boolean} options.showError - 是否自动弹出错误提示（默认 true）
 * @returns {Promise}
 */
function request(options = {}) {
  const {
    url,
    method = 'GET',
    data = {},
    header = {},
    showLoading = false,
    showError = true
  } = options

  return new Promise((resolve, reject) => {
    // 显示 loading
    if (showLoading) {
      uni.showLoading({ title: '加载中...', mask: true })
    }

    // 构造请求头
    const headers = {
      'Content-Type': 'application/json',
      ...header
    }

    // 自动注入 JWT token
    if (!isWhiteListed(url)) {
      const token = uni.getStorageSync('token')
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }
    }

    uni.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: headers,
      timeout: 15000,
      success: (res) => {
        if (showLoading) uni.hideLoading()

        const { statusCode } = res
        const body = res.data

        // HTTP 状态码判断
        if (statusCode === 200) {
          // 业务状态码判断
          if (body.code === 200) {
            resolve(body.data)
          } else {
            // 业务错误
            handleBusinessError(body, showError)
            reject(body)
          }
        } else if (statusCode === 401) {
          // token 过期或无效
          handleUnauthorized()
          reject(body)
        } else {
          // 其他 HTTP 错误
          if (showError) {
            uni.showToast({
              title: body?.message || '请求失败',
              icon: 'none',
              duration: 2000
            })
          }
          reject(body)
        }
      },
      fail: (err) => {
        if (showLoading) uni.hideLoading()
        if (showError) {
          uni.showToast({
            title: '网络异常，请稍后重试',
            icon: 'none',
            duration: 2000
          })
        }
        reject(err)
      }
    })
  })
}

/**
 * 处理业务错误
 */
function handleBusinessError(body, showError) {
  if (showError) {
    uni.showToast({
      title: body.message || '操作失败',
      icon: 'none',
      duration: 2000
    })
  }
}

/**
 * 处理 401 未授权
 */
function handleUnauthorized() {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.showToast({
    title: '登录已过期，请重新登录',
    icon: 'none',
    duration: 2000
  })
  setTimeout(() => {
    uni.reLaunch({ url: '/pages/auth/login' })
  }, 1500)
}

/**
 * 文件上传封装
 * @param {string} filePath - 本地文件路径
 * @param {string} url - 上传接口路径
 * @param {Object} formData - 附加表单数据
 * @returns {Promise}
 */
export function uploadFile(filePath, url = '/files/upload', formData = {}) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.uploadFile({
      url: `${BASE_URL}${url}`,
      filePath,
      name: 'file',
      formData,
      header: {
        Authorization: token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          const body = JSON.parse(res.data)
          if (body.code === 200) {
            resolve(body.data)
          } else {
            uni.showToast({ title: body.message || '上传失败', icon: 'none' })
            reject(body)
          }
        } else {
          uni.showToast({ title: '上传失败', icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '上传失败，请检查网络', icon: 'none' })
        reject(err)
      }
    })
  })
}

// 便捷方法
export const get = (url, data, options = {}) =>
  request({ url, method: 'GET', data, ...options })

export const post = (url, data, options = {}) =>
  request({ url, method: 'POST', data, ...options })

export const put = (url, data, options = {}) =>
  request({ url, method: 'PUT', data, ...options })

export const del = (url, data, options = {}) =>
  request({ url, method: 'DELETE', data, ...options })

export default request
