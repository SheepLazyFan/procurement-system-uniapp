/**
 * 全局分享 mixin — 为所有页面提供默认的 onShareAppMessage 和 onShareTimeline
 * 各页面可自定义覆盖
 */
export default {
  onShareAppMessage() {
    return {
      title: '采购管理系统',
      path: '/pages/inventory/index'
    }
  },
  onShareTimeline() {
    return {
      title: '采购管理系统'
    }
  }
}
