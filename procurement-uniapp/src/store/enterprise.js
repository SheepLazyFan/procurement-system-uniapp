/**
 * 企业状态管理
 */
import { defineStore } from 'pinia'
import {
  getEnterprise,
  createEnterprise as apiCreate,
  updateEnterprise as apiUpdate,
  refreshInviteCode as apiRefreshCode
} from '@/api/enterprise'

export const useEnterpriseStore = defineStore('enterprise', {
  state: () => ({
    info: {
      id: null,
      name: '',
      address: '',
      contactPhone: '',
      contactName: '',
      inviteCode: '',
      logoUrl: ''
    },
    loaded: false
  }),

  actions: {
    /** 获取企业信息 */
    async fetchEnterprise() {
      const data = await getEnterprise()
      this.info = data
      this.loaded = true
      return data
    },

    /** 创建企业 */
    async createEnterprise(formData) {
      const data = await apiCreate(formData)
      this.info = data
      this.loaded = true
      return data
    },

    /** 更新企业信息 */
    async updateEnterprise(formData) {
      const data = await apiUpdate(formData)
      this.info = { ...this.info, ...data }
      return data
    },

    /** 刷新邀请码 */
    async refreshInviteCode() {
      const data = await apiRefreshCode()
      this.info.inviteCode = data.inviteCode
      return data.inviteCode
    },

    /** 清除企业信息（退出登录时） */
    clearEnterprise() {
      this.info = {
        id: null, name: '', address: '', contactPhone: '',
        contactName: '', inviteCode: '', logoUrl: ''
      }
      this.loaded = false
    }
  }
})
