/**
 * 团队管理 API
 */
import { get, post, put, del } from './request'

/** 团队成员列表 */
export const getTeamMembers = () =>
  get('/team/members')

/** 通过邀请码加入企业 */
export const joinByInviteCode = (inviteCode) =>
  post('/team/join', { inviteCode })

/** 设置成员角色 */
export const setMemberPermissions = (id, data) =>
  put(`/team/members/${id}/permissions`, data)

/** 移除团队成员 */
export const removeMember = (id) =>
  del(`/team/members/${id}`)
