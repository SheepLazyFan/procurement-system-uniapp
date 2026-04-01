/**
 * 买家端 API
 */
import { get, post, put } from './request'

/** 获取商家门店信息 */
export const getStoreInfo = (enterpriseId) =>
  get(`/buyer/store/${enterpriseId}`)

/** 商家分类列表（可传 stockStatus / priceMin / priceMax 使分类数量与筛选同步） */
export const getStoreCategories = (enterpriseId, params) =>
  get(`/buyer/store/${enterpriseId}/categories`, params)

/** 商家商品列表 */
export const getStoreProducts = (enterpriseId, params) =>
  get(`/buyer/store/${enterpriseId}/products`, params)

/** 商品详情 */
export const getProductDetail = (id) =>
  get(`/buyer/product/${id}`)

/** 提交采购订单 */
export const createBuyerOrder = (data) =>
  post('/buyer/orders', data, { showLoading: true })

/** 伪支付 */
export const payBuyerOrder = (id) =>
  put(`/buyer/orders/${id}/pay`)

/** 买家声明线下已付款（UNPAID → CLAIMED） */
export const claimBuyerOrderPaid = (id) =>
  put(`/buyer/orders/${id}/claim-paid`)

/** 我的订单列表 */
export const getBuyerOrders = (params) =>
  get('/buyer/orders', params)

/** 订单详情 */
export const getBuyerOrderDetail = (id) =>
  get(`/buyer/orders/${id}`)

/** 买家取消订单 */
export const cancelBuyerOrder = (id) =>
  put(`/buyer/orders/${id}/cancel`)
