/**
 * 订阅消息 API
 */
import { get, post } from './request'

/** 手动触发库存预警推送（测试用） */
export const triggerStockWarning = () =>
  post('/subscribe/stock-warning/trigger')

/** 查询当前用户库存预警通知开关状态 */
export const getNotifyStatus = () =>
  get('/subscribe/notify/status')

/** 开启库存预警通知（WeChat 授权后调用） */
export const enableNotify = () =>
  post('/subscribe/notify/enable')

/** 关闭库存预警通知 */
export const disableNotify = () =>
  post('/subscribe/notify/disable')
