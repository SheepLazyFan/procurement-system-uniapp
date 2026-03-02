/**
 * 认证模块 API
 */
import { get, post } from './request'

/** 发送短信验证码 */
export const sendSmsCode = (phone) =>
  post('/auth/sms/send', { phone })

/** 手机号 + 验证码登录 */
export const loginByPhone = (phone, code) =>
  post('/auth/login', { phone, code })

/** 买家微信授权登录 */
export const wxLogin = (data) =>
  post('/auth/wx-login', data)

/** 获取当前用户信息 */
export const getProfile = () =>
  get('/auth/profile')

/** 退出登录 */
export const logout = () =>
  post('/auth/logout')
