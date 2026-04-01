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

// ===== 供应商-商品关联 API =====

/** 查询供应商关联商品列表（分页） */
export const getSupplierProducts = (supplierId, params) =>
  get(`/suppliers/${supplierId}/products`, params)

/** 批量绑定商品到供应商（items: [{productId, supplyPrice}]） */
export const bindSupplierProducts = (supplierId, data) =>
  post(`/suppliers/${supplierId}/products`, data)

/** 解绑供应商与商品 */
export const unbindSupplierProduct = (supplierId, productId) =>
  del(`/suppliers/${supplierId}/products/${productId}`)

/** 更新供货价 */
export const updateSupplierProductPrice = (supplierId, productId, data) =>
  put(`/suppliers/${supplierId}/products/${productId}/price`, data)
