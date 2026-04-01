/**
 * 商品管理 API
 */
import { get, post, put, del } from './request'

/** 商品列表（分页） */
export const getProductList = (params) =>
  get('/products', params)

/** 商品详情 */
export const getProductDetail = (id) =>
  get(`/products/${id}`)

/** 创建商品 */
export const createProduct = (data) =>
  post('/products', data)

/** 更新商品 */
export const updateProduct = (id, data) =>
  put(`/products/${id}`, data)

/** 删除商品 */
export const deleteProduct = (id) =>
  del(`/products/${id}`)

/** 调整库存（showError:false 避免 request.js 与调用方 catch 双重弹窗） */
export const adjustStock = (id, data) =>
  put(`/products/${id}/stock`, data, { showError: false })

/** 库存预警列表 */
export const getStockWarnings = (params) =>
  get('/products/stock-warnings', params)

/** 批量导入商品 — 由 batch-import.vue 直接使用 wx.uploadFile，此函数保留备用 */
export const batchImportProducts = (data) =>
  post('/products/batch-import', data, { showLoading: true })

/** 下载导入模板 — 返回 URL 路径 */
export const getImportTemplateUrl = () =>
  '/products/import-template'
