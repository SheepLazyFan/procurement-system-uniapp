/**
 * 客户管理 API
 */
import { get, post, put, del } from './request'

/** 客户列表（分页） */
export const getCustomerList = (params) =>
  get('/customers', params)

/** 客户详情 */
export const getCustomerDetail = (id) =>
  get(`/customers/${id}`)

/** 添加客户 */
export const createCustomer = (data) =>
  post('/customers', data)

/** 更新客户 */
export const updateCustomer = (id, data) =>
  put(`/customers/${id}`, data)

/** 删除客户 */
export const deleteCustomer = (id) =>
  del(`/customers/${id}`)
