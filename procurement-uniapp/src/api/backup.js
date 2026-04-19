/**
 * 数据备份 API
 */
import { get, post, del, downloadFile } from './request'

/** 创建备份 */
export const createBackup = (backupType = 'FULL') =>
  post('/backup', { backupType }, { showLoading: true })

/** 备份记录列表 */
export const getBackupList = () =>
  get('/backup/list')

/** 恢复预检信息 */
export const getRestorePreview = (id) =>
  get(`/backup/${id}/restore-preview`)

/** 从备份恢复 */
export const restoreBackup = (id) =>
  post(`/backup/${id}/restore`, {}, { showLoading: true })

/** 删除备份 */
export const deleteBackup = (id) =>
  del(`/backup/${id}`)

/** 下载备份文件（返回临时文件路径） */
export const downloadBackup = (id) =>
  downloadFile(`/backup/${id}/download`)
