/**
 * 统计报表 API
 */
import { get } from './request'

/** 经营数据概览 */
export const getOverview = () =>
  get('/statistics/overview')

/** 销售趋势 */
export const getSalesTrend = (params) =>
  get('/statistics/sales-trend', params)

/** 利润趋势 */
export const getProfitTrend = (params) =>
  get('/statistics/profit-trend', params)

/** 库存统计 */
export const getInventoryStats = () =>
  get('/statistics/inventory')

/** 商品销售排行 */
export const getSalesRanking = (params) =>
  get('/statistics/sales-ranking/products', params)

/** 客户销售排行 */
export const getCustomerRanking = (params) =>
  get('/statistics/sales-ranking/customers', params)
