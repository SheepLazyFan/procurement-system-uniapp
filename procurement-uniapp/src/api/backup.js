/**
 * 数据备份 API
 */
import { get, post } from './request'

/** 创建备份 */
export const createBackup = (backupType = 'FULL') =>
  post('/backup', { backupType }, { showLoading: true })

/** 备份记录列表 */
export const getBackupList = (params) =>
  get('/backup/list', params)

/** 从备份恢复 */
export const restoreBackup = (id) =>
  post(`/backup/${id}/restore`, {}, { showLoading: true })
