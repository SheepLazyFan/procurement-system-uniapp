/**
 * 供应商管理 API
 */
import { get, post, put, del } from './request'

/** 供应商列表（分页） */
export const getSupplierList = (params) =>
  get('/suppliers', params)

/** 供应商详情 */
export const getSupplierDetail = (id) =>
  get(`/suppliers/${id}`)

/** 添加供应商 */
export const createSupplier = (data) =>
  post('/suppliers', data)

/** 更新供应商 */
export const updateSupplier = (id, data) =>
  put(`/suppliers/${id}`, data)

/** 删除供应商 */
export const deleteSupplier = (id) =>
  del(`/suppliers/${id}`)
