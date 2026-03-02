/**
 * 用户状态管理
 */
import { defineStore } from 'pinia'
import { loginByPhone, wxLogin, getProfile, logout as apiLogout } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: {
      id: null,
      phone: '',
      role: '',       // SELLER / MEMBER / BUYER
      nickName: '',
      avatarUrl: '',
      enterpriseId: null,
      wxOpenid: ''
    },
    isLoggedIn: false
  }),

  getters: {
    /** 是否为卖家（店主） */
    isSeller: (state) => state.userInfo.role === 'SELLER',
    /** 是否为团队成员 */
    isMember: (state) => state.userInfo.role === 'MEMBER',
    /** 是否为买家 */
    isBuyer: (state) => state.userInfo.role === 'BUYER',
    /** 是否已创建/加入企业 */
    hasEnterprise: (state) => !!state.userInfo.enterpriseId
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
     * 手机号 + 验证码登录
     */
    async loginByPhone(phone, code) {
      const res = await loginByPhone(phone, code)
      this._setLoginState(res.token, res.user)
      return res
    },

    /**
     * 微信授权登录（买家）
     */
    async wxLogin(data) {
      const res = await wxLogin(data)
      this._setLoginState(res.token, res.user)
      return res
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
      uni.reLaunch({ url: '/pages/auth/login' })
    },

    /**
     * 更新企业 ID（创建/加入企业后）
     */
    setEnterpriseId(enterpriseId) {
      this.userInfo.enterpriseId = enterpriseId
      uni.setStorageSync('userInfo', JSON.stringify(this.userInfo))
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
        id: null, phone: '', role: '', nickName: '',
        avatarUrl: '', enterpriseId: null, wxOpenid: ''
      }
      this.isLoggedIn = false
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
    }
  }
})
