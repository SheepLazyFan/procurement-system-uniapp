/**
 * 销售订单 API
 */
import { get, post, put } from './request'

/** 订单列表（分页） */
export const getSalesOrderList = (params) =>
  get('/sales-orders', params)

/** 各状态订单数量 */
export const getSalesOrderCounts = (params) =>
  get('/sales-orders/count-by-status', params)

/** 订单详情 */
export const getSalesOrderDetail = (id) =>
  get(`/sales-orders/${id}`)

/** 商家开单 */
export const createSalesOrder = (data) =>
  post('/sales-orders', data, { showLoading: true })

/** 确认订单 */
export const confirmSalesOrder = (id) =>
  put(`/sales-orders/${id}/confirm`)

/** 标记发货 */
export const shipSalesOrder = (id) =>
  put(`/sales-orders/${id}/ship`)

/** 完成订单 */
export const completeSalesOrder = (id) =>
  put(`/sales-orders/${id}/complete`)

/** 取消订单 */
export const cancelSalesOrder = (id) =>
  put(`/sales-orders/${id}/cancel`)

/** 确认收款（线下收款） */
export const paySalesOrder = (id) =>
  put(`/sales-orders/${id}/pay`)
