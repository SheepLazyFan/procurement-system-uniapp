<script>
import { useUserStore } from '@/store/user'

export default {
  onLaunch() {
    console.log('App Launch')
    // 检查本地 token，恢复登录态
    const userStore = useUserStore()
    userStore.checkLoginStatus()
  },
  onShow() {
    console.log('App Show')
  },
  onHide() {
    console.log('App Hide')
  }
}
</script>

<style lang="scss">
/* 全局公共样式 */
page {
  background-color: #f5f6fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
}

/* 安全区适配 */
.safe-area-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

/* 通用间距 */
.container {
  padding: 24rpx;
}

/* 通用卡片样式 */
.card {
  background-color: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
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

/* 主色按钮 */
.btn-primary {
  background-color: #2979ff;
  color: #fff;
  border: none;
  border-radius: 12rpx;
  font-size: 28rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
}

.btn-primary:active {
  opacity: 0.85;
}

/* 价格文字颜色 */
.price-text {
  color: #ff4d4f;
  font-weight: 600;
}

/* 分割线 */
.divider {
  height: 1rpx;
  background-color: #eee;
  margin: 20rpx 0;
}
</style>
