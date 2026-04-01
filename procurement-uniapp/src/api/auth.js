/**
 * 认证模块 API
 */
import { get, post, put, uploadFile } from './request'

/** 微信授权登录 */
export const wxLogin = (data) =>
  post('/auth/wx-login', data)

/** 获取当前用户信息 */
export const getProfile = () =>
  get('/auth/profile')

/** 上传并更新用户头像 */
export const uploadAvatar = (filePath) =>
  uploadFile(filePath, '/auth/avatar', { type: 'avatar' })

/** 修改用户昵称 */
export const updateNickName = (nickName) =>
  put('/auth/nickname', { nickName })

/** 退出登录 */
export const logout = () =>
  post('/auth/logout')
