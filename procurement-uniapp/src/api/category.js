/**
 * 商品分类 API
 */
import { get, post, put, del } from './request'

/** 获取分类列表 */
export const getCategoryList = async () => {
  const res = await get('/categories')
  if (Array.isArray(res)) {
    return res.map(c => ({
      ...c,
      name: c.name ? String(c.name).replace(/[\r\n\t]+/g, ' ').trim() : ''
    }))
  }
  return res || []
}

/** 创建分类 */
export const createCategory = (data) =>
  post('/categories', data)

/** 更新分类 */
export const updateCategory = (id, data) =>
  put(`/categories/${id}`, data)

/** 删除分类 */
export const deleteCategory = (id) =>
  del(`/categories/${id}`)

/** 批量更新排序 */
export const sortCategories = (data) =>
  put('/categories/sort', data)

/** 按当前筛选条件统计各分类商品种数 */
export const getCategoryStats = async (params) => {
  const res = await get('/categories/stats', params)
  if (Array.isArray(res)) {
    return res.map(c => ({
      ...c,
      name: c.name ? String(c.name).replace(/[\r\n\t]+/g, ' ').trim() : ''
    }))
  }
  return res || []
}
