/**
 * 企业管理 API
 */
import { get, post, put } from './request'

/** 创建企业 */
export const createEnterprise = (data) =>
  post('/enterprise', data)

/** 获取当前企业信息 */
export const getEnterprise = () =>
  get('/enterprise')

/** 更新企业信息 */
export const updateEnterprise = (data) =>
  put('/enterprise', data)

/** 刷新邀请码 */
export const refreshInviteCode = () =>
  put('/enterprise/invite-code/refresh')

/** 获取团队成员列表（含店主） */
export const getTeamMembers = () =>
  get('/team/members')
