/**
 * uni.request 统一封装
 * - JWT 自动注入
 * - 统一错误处理
 * - 请求/响应拦截
 */

// 后端 API 基础地址（编译时由 vite.config.js define 注入，区分 dev/prod 环境）
// dev:  .env.development → http://127.0.0.1:8080/api
// prod: .env.production  → http://your-server-ip:8080/api（上线后换域名/HTTPS）
const BASE_URL = typeof __API_BASE__ !== 'undefined' ? __API_BASE__ : 'http://127.0.0.1:8080/api'

// 无需 token 的白名单路径
const WHITE_LIST = [
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

    // 自动注入 JWT token；非白名单接口无 token 时直接静默跳过请求
    if (!isWhiteListed(url)) {
      const token = uni.getStorageSync('token')
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      } else {
        // 未登录状态下请求需鉴权的接口，直接静默拒绝，不发请求
        if (showLoading) uni.hideLoading()
        return reject({ code: 40100, message: '未登录', silent: true })
      }
    }

    // GET 请求时过滤掉 null / undefined / '' 参数，防止后端解析错误
    let requestData = data
    if (method === 'GET' && data && typeof data === 'object') {
      requestData = {}
      Object.keys(data).forEach(k => {
        if (data[k] !== null && data[k] !== undefined && data[k] !== '') {
          requestData[k] = data[k]
        }
      })
    }

    uni.request({
      url: `${BASE_URL}${url}`,
      method,
      data: requestData,
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
        const fullUrl = `${BASE_URL}${url}`
        const isTimeout = !!(err && typeof err.errMsg === 'string' && err.errMsg.toLowerCase().includes('timeout'))
        console.warn(`[request] ${method} ${fullUrl} failed`, err)
        if (showError) {
          uni.showToast({
            title: '网络异常，请稍后重试',
            icon: 'none',
            duration: 2000
          })
        }
        reject({
          ...err,
          code: isTimeout ? 'REQUEST_TIMEOUT' : err?.code,
          message: isTimeout ? '请求超时' : (err?.message || err?.errMsg || '网络异常'),
          url: fullUrl,
          method
        })
      }
    })
  })
}

/**
 * 处理业务错误
 */
let isRefreshingToken = false
function handleBusinessError(body, showError) {
  const token = uni.getStorageSync('token')
  // 未登录状态下的权限错误静默处理
  if (!token && (body.code === 40100 || body.code === 40300)) {
    return
  }
  // Token 过期：自动尝试静默重新登录（防止并发多次刷新）
  if (token && body.code === 40100) {
    // 正在刷新中的并发请求，静默忽略
    if (isRefreshingToken) return
    isRefreshingToken = true
    silentRefreshLogin().then(() => {
      isRefreshingToken = false
      // 刷新当前页面以重新发起请求
      const pages = getCurrentPages()
      if (pages.length > 0) {
        const current = pages[pages.length - 1]
        const route = '/' + current.route
        uni.reLaunch({ url: route })
      }
    }).catch(() => {
      isRefreshingToken = false
      forceRelogin('登录已过期，请重新登录')
    })
    return
  }
  // 企业恢复、权限重置等场景下的强制重新登录
  if (token && body.code === 40103) {
    forceRelogin(body.message || '登录态已失效，请重新登录')
    return
  }
  // 未创建/加入企业错误静默处理（由页面自行展示提示）
  if (body.code === 40402) {
    return
  }
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
  const token = uni.getStorageSync('token')
  // 如果本来就没有 token（未登录状态），静默处理，不弹窗不跳转
  if (!token) return
  forceRelogin('登录已过期，请重新登录')
}

function silentRefreshLogin() {
  return new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: (loginRes) => {
        if (!loginRes || !loginRes.code) {
          reject(new Error('wx.login 未返回有效 code'))
          return
        }
        uni.request({
          url: `${BASE_URL}/auth/wx-login`,
          method: 'POST',
          data: { code: loginRes.code },
          header: {
            'Content-Type': 'application/json'
          },
          timeout: 15000,
          success: (res) => {
            const body = res.data
            if (res.statusCode === 200 && body?.code === 200 && body?.data?.token) {
              uni.setStorageSync('token', body.data.token)
              uni.setStorageSync('userInfo', JSON.stringify(body.data.user || {}))
              resolve(body.data)
              return
            }
            reject(body || new Error('静默登录失败'))
          },
          fail: reject
        })
      },
      fail: reject
    })
  })
}

function forceRelogin(message) {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.showToast({
    title: message,
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
          let body
          try {
            body = JSON.parse(res.data)
          } catch (parseErr) {
            uni.showToast({ title: '响应解析失败', icon: 'none' })
            reject({ code: -1, message: '响应解析失败' })
            return
          }
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

/**
 * 文件下载封装 — 适用于需要鉴权的文件下载
 * @param {string} url - 下载接口路径（不含 BASE_URL）
 * @returns {Promise<string>} 临时文件路径
 */
export function downloadFile(url) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.downloadFile({
      url: `${BASE_URL}${url}`,
      header: { Authorization: token ? `Bearer ${token}` : '' },
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.tempFilePath)
        } else {
          reject(new Error('下载失败'))
        }
      },
      fail: reject
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

/**
 * 将后端返回的文件相对路径转为完整可访问 URL
 * 兼容：已有完整 URL（http 开头）直接返回；相对路径自动拼接 BASE_URL
 */
export function fileUrl(path) {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  return BASE_URL + path
}

export default request
