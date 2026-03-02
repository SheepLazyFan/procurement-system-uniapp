/**
 * 采购订单 API
 */
import { get, post, put } from './request'

/** 订单列表（分页） */
export const getPurchaseOrderList = (params) =>
  get('/purchase-orders', params)

/** 订单详情 */
export const getPurchaseOrderDetail = (id) =>
  get(`/purchase-orders/${id}`)

/** 创建采购订单（快速采购） */
export const createPurchaseOrder = (data) =>
  post('/purchase-orders', data, { showLoading: true })

/** 标记采购中 */
export const markPurchasing = (id) =>
  put(`/purchase-orders/${id}/purchasing`)

/** 标记到货（自动增加库存） */
export const markArrived = (id) =>
  put(`/purchase-orders/${id}/arrive`)

/** 完成采购 */
export const completePurchaseOrder = (id) =>
  put(`/purchase-orders/${id}/complete`)

/** 取消采购 */
export const cancelPurchaseOrder = (id) =>
  put(`/purchase-orders/${id}/cancel`)
