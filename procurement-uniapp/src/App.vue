<script>
import { useUserStore, waitForLoginReady } from '@/store/user'
import { useCartStore } from '@/store/cart'

export default {
  onLaunch() {
    console.log('App Launch')
    // 延迟确保 Pinia 已被Vue app.use() 注册完毕
    setTimeout(() => {
      try {
        const userStore = useUserStore()
        // 1. 先恢复本地缓存的登录态（用于页面立即显示）
        userStore.checkLoginStatus()
        // 2. 静默登录刷新 token（完成后 loginReadyPromise resolve）
        userStore.silentWxLogin()
        // 3. 恢复购物车本地缓存
        useCartStore().restoreFromStorage()
      } catch (e) {
        console.warn('onLaunch store init failed', e)
      }
    }, 0)
  },
  async onShow() {
    console.log('App Show')
    // App 从后台切回前台时刷新用户信息
    // 覆盖「员工正在使用 app、店主此时更改其角色」的场景
    try {
      await waitForLoginReady()
      const userStore = useUserStore()
      if (userStore.isLoggedIn) {
        userStore.fetchProfile().catch((e) => {
          console.warn('[app] profile refresh failed', e && e.message || e)
        })
      }
    } catch (e) {
      console.warn('[app] onShow profile sync failed', e && e.message || e)
    }
  },
  onHide() {
    console.log('App Hide')
  }
}
</script>

<style lang="scss">
/* 全局公共样式 */
page {
  /* ========== Design Tokens (SaaS 现代风格) ========== */
  /* Colors */
  --bg-page: #F7F9FA;
  --bg-card: #FFFFFF;
  --text-primary: #111827;
  --text-secondary: #6B7280;
  --text-tertiary: #9CA3AF;
  --brand-primary: #3B82F6;
  --brand-primary-light: rgba(59, 130, 246, 0.1);
  --color-danger: #EF4444;
  --color-danger-light: rgba(239, 68, 68, 0.1);
  --color-success: #10B981;
  --color-success-light: rgba(16, 185, 129, 0.1);
  --color-warning: #F59E0B;
  --color-warning-light: rgba(245, 158, 11, 0.1);
  --border-color: #E5E7EB;
  --border-light: #F3F4F6;

  /* Spacing */
  --space-xs: 8rpx;
  --space-sm: 16rpx;
  --space-md: 24rpx;
  --space-lg: 32rpx;
  --space-xl: 48rpx;

  /* Radius */
  --radius-sm: 8rpx;
  --radius-md: 12rpx;
  --radius-lg: 24rpx; /* SaaS 大圆角趋势 */
  --radius-full: 9999rpx;

  /* Shadows */
  --shadow-sm: 0 2rpx 4rpx rgba(0, 0, 0, 0.02);
  --shadow-card: 0 8rpx 32rpx -4rpx rgba(16, 24, 40, 0.04), 0 4rpx 12rpx -4rpx rgba(16, 24, 40, 0.02);
  --shadow-floating: 0 16rpx 48rpx -12rpx rgba(16, 24, 40, 0.08);

  background-color: var(--bg-page);
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'PingFang SC', 'HarmonyOS Sans SC', 'Microsoft YaHei', sans-serif;
  font-size: 28rpx;
  color: var(--text-primary);
  box-sizing: border-box;
  letter-spacing: 0.5rpx;
}

/* 专属数字字体 (等宽、更粗、更圆润) */
.num-font {
  font-family: system-ui, -apple-system, 'SF Pro Display', 'PingFang SC', sans-serif;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1rpx;
}

/* ========== 现代核心组件库 ========== */

/* 现代微胶囊标签 (Tint Tag) */
.saas-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6rpx 20rpx;
  font-size: 24rpx;
  border-radius: var(--radius-sm);
  font-weight: 600;
  border: none;
}
.saas-tag-primary { background: var(--brand-primary-light); color: var(--brand-primary); }
.saas-tag-success { background: var(--color-success-light); color: var(--color-success); }
.saas-tag-danger { background: var(--color-danger-light); color: var(--color-danger); }
.saas-tag-warning { background: var(--color-warning-light); color: var(--color-warning); }
.saas-tag-info { background: var(--border-light); color: var(--text-secondary); }

/* 现代 SaaS 卡片 */
.saas-card {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  margin-bottom: var(--space-md);
  box-shadow: var(--shadow-card);
  box-sizing: border-box;
  border: none;
  /* 松手弹起时：长阻尼、高弹性回位 */
  transition: all 0.4s cubic-bezier(0.25, 1.25, 0.2, 1);
}

/* 真正原生的触碰反馈类 (配合 hover-class 使用，避免 :active 延迟) */
.saas-card-push {
  transform: scale(0.93) !important;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04) !important;
  opacity: 0.8;
  /* 按下瞬间：极速闪塌，去除阻尼 */
  transition: all 0.05s ease !important;
}

/* 安全区适配 */
.safe-area-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

/* 通用间距 */
.container {
  padding: var(--space-md);
}

/* 老本系统通用卡片样式 (兼容旧代码，平滑演进) */
.card {
  background-color: var(--bg-card);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  margin-bottom: 20rpx;
  box-shadow: var(--shadow-sm);
}

/* 通用 flex 布局 */
.flex-row {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.flex-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.flex-center {
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 文字省略 */
.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 主色按钮 (向 SaaS 靠拢) */
.btn-primary {
  background-color: var(--brand-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 28rpx;
  font-weight: 500;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  box-shadow: 0 4rpx 12rpx var(--brand-primary-light);
  transition: all 0.2s ease;
}

.btn-primary:active {
  transform: scale(0.97);
  box-shadow: none;
}

/* 价格文字颜色 */
.price-text {
  color: var(--color-danger);
  font-weight: 600;
}

/* 分割线 */
.divider {
  height: 1rpx;
  background-color: #eee;
  margin: 20rpx 0;
}

/* 加载提示 */
.loading-tip {
  text-align: center;
  padding: 40rpx 0;
  font-size: 26rpx;
  color: #999;
}

/* 全局动画 (瀑布流卡片入场等) */
.animate-fade-up {
  animation: fadeInUp 0.4s cubic-bezier(0.25, 1, 0.5, 1) both;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30rpx); }
  to { opacity: 1; transform: translateY(0); }
}

</style>
