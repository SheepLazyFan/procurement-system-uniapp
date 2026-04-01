/**
 * 用户状态管理
 */
import { defineStore } from 'pinia'
import { wxLogin, getProfile, logout as apiLogout } from '@/api/auth'

// 登录就绪 Promise —— 页面可 await 此变量确保 silentWxLogin 已完成
let _loginReadyResolve
const loginReadyPromise = new Promise(resolve => { _loginReadyResolve = resolve })

export function waitForLoginReady() {
  return loginReadyPromise
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    notifyEnabled: false,
    userInfo: {
      id: null,
      phone: '',
      role: '',           // SELLER / MEMBER
      memberRole: '',     // ADMIN / SALES / WAREHOUSE（仅 MEMBER 有值）
      nickName: '',
      avatarUrl: '',
      enterpriseId: null,
      enterpriseName: '',
      wxOpenid: ''
    },
    isLoggedIn: false
  }),

  getters: {
    /** 是否为商家（店主） */
    isSeller: (state) => state.userInfo.role === 'SELLER',
    /** 是否为团队成员 */
    isMember: (state) => state.userInfo.role === 'MEMBER',
    /** 是否已创建/加入企业 */
    hasEnterprise: (state) => !!state.userInfo.enterpriseId,
    /** 团队管理员（成员角色） */
    isAdmin: (state) => state.userInfo.memberRole === 'ADMIN',
    /** 销售员（成员角色） */
    isSales: (state) => state.userInfo.memberRole === 'SALES',
    /** 仓管员（成员角色） */
    isWarehouse: (state) => state.userInfo.memberRole === 'WAREHOUSE',
    /** 是否拥有完整权限（店主 或 管理员） */
    hasFullAccess: (state) => state.userInfo.role === 'SELLER' || state.userInfo.memberRole === 'ADMIN'
  },

  actions: {
    /**
     * 检查本地登录态
     */
    checkLoginStatus() {
      const token = uni.getStorageSync('token')
      const userInfo = uni.getStorageSync('userInfo')
      if (token && userInfo) {
        this.token = token
        this.userInfo = JSON.parse(userInfo)
        this.isLoggedIn = true
      }
    },

    /**
     * 微信授权登录（商家）
     */
    async wxLogin(data) {
      const res = await wxLogin(data)
      this._setLoginState(res.token, res.user)
      return res
    },

    /**
     * 静默微信登录（小程序启动时自动调用）
     * 使用 wx.login 获取 code，发送给后端换取 token
     */
    async silentWxLogin() {
      try {
        const loginRes = await uni.login({ provider: 'weixin' })
        if (!loginRes || !loginRes.code) {
          console.warn('wx.login 失败', loginRes)
          return
        }
        const res = await wxLogin({ code: loginRes.code })
        this._setLoginState(res.token, res.user)
        console.log('静默登录成功')
      } catch (e) {
        console.warn('静默登录失败（后端未配置微信 AppSecret 或网络异常）', e)
      } finally {
        _loginReadyResolve()
      }
    },

    /**
     * 获取最新用户信息
     */
    async fetchProfile() {
      const user = await getProfile()
      this.userInfo = { ...this.userInfo, ...user }
      uni.setStorageSync('userInfo', JSON.stringify(this.userInfo))
      return user
    },

    /**
     * 退出登录
     */
    async logout() {
      try {
        await apiLogout()
      } catch (e) {
        // 即使接口失败也清除本地状态
      }
      this._clearLoginState()
      uni.reLaunch({ url: '/pages/inventory/index' })
    },

    /**
     * 更新企业 ID（创建/加入企业后）
     */
    setEnterpriseId(enterpriseId) {
      this.userInfo.enterpriseId = enterpriseId
      uni.setStorageSync('userInfo', JSON.stringify(this.userInfo))
    },

    /** 同步通知开关到 store 并持久化 */
    setNotifyEnabled(val) {
      this.notifyEnabled = val
      uni.setStorageSync('notifyEnabled', val ? '1' : '0')
    },

    /** 从本地缓存恢复通知开关（页面 onLoad 时调用，避免重复请求后端） */
    loadNotifyStatus() {
      const cached = uni.getStorageSync('notifyEnabled')
      if (cached !== '') this.notifyEnabled = cached === '1'
    },

    /** 内部：设置登录态 */
    _setLoginState(token, user) {
      this.token = token
      this.userInfo = { ...this.userInfo, ...user }
      this.isLoggedIn = true
      uni.setStorageSync('token', token)
      uni.setStorageSync('userInfo', JSON.stringify(this.userInfo))
    },

    /** 内部：清除登录态 */
    _clearLoginState() {
      this.token = ''
      this.userInfo = {
        id: null, phone: '', role: '', memberRole: '', nickName: '',
        avatarUrl: '', enterpriseId: null, enterpriseName: '', wxOpenid: ''
      }
      this.isLoggedIn = false
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
    }
  }
})
