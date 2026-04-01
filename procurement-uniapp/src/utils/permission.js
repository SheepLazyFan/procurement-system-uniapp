/**
 * 角色权限工具
 *
 * 权限矩阵：
 *   页面      | SELLER | ADMIN | SALES | WAREHOUSE
 *   库存      |   ✓    |   ✓   |   ✓   |     ✓
 *   采购/供应商|   ✓    |   ✓   |   ✗   |     ✓
 *   销售/客户  |   ✓    |   ✓   |   ✓   |     ✗
 *   统计      |   ✓    |   ✓   |   ✗   |     ✗
 *   团队管理   |   ✓    |   ✓   |   ✗   |     ✗
 *   数据备份   |   ✓    |   ✗   |   ✗   |     ✗
 */
import { useUserStore } from '@/store/user'

/**
 * 获取当前用户有效角色
 * @returns {'SELLER'|'ADMIN'|'SALES'|'WAREHOUSE'|''}
 */
export function getEffectiveRole() {
  const store = useUserStore()
  if (store.isSeller) return 'SELLER'
  if (store.isMember) return store.userInfo.memberRole || ''
  return ''
}

/**
 * 检查当前用户是否有权访问指定模块
 * @param {'purchase'|'sales'|'statistics'|'team'|'backup'} module
 * @returns {boolean}
 */
export function hasAccess(module) {
  const role = getEffectiveRole()
  // 数据备份仅店主可操作
  if (module === 'backup') return role === 'SELLER'
  // 店主 & 管理员拥有其余全部权限
  if (role === 'SELLER' || role === 'ADMIN') return true

  switch (module) {
    case 'purchase':   return role === 'WAREHOUSE'
    case 'sales':      return role === 'SALES'
    case 'statistics': return false
    case 'team':       return false
    case 'backup':     return false
    default:           return true
  }
}

/**
 * Tab 页面权限守卫 — 在 onShow() 首行调用
 * 若无权限返回 false，由页面自行展示无权限 UI
 * @param {'purchase'|'sales'|'statistics'} module
 * @returns {boolean} true=有权限，false=无权限
 */
export function guardTabPage(module) {
  return hasAccess(module)
}

/**
 * 获取无权限提示文案
 * @param {'purchase'|'sales'|'statistics'} module
 * @returns {string}
 */
export function getNoAccessTip(module) {
  const role = getEffectiveRole()
  const moduleNames = { purchase: '采购', sales: '销售', statistics: '统计' }
  const moduleName = moduleNames[module] || module
  if (role === 'SALES') return `销售员暂无「${moduleName}」模块的访问权限\n请联系总管理员（商家）开通`
  if (role === 'WAREHOUSE') return `仓管员暂无「${moduleName}」模块的访问权限\n请联系总管理员（商家）开通`
  return `暂无「${moduleName}」模块的访问权限\n只有总管理员（商家）可查看`
}
