<template>
  <view class="page-login">
    <view class="login-header">
      <text class="login-title">采购系统</text>
      <text class="login-subtitle">B2B 进销存管理平台</text>
    </view>

    <view class="login-card">
      <text class="login-tip">微信授权登录中...</text>
      <text class="login-desc">系统将自动通过微信授权完成登录</text>
      <button class="btn-login" @tap="handleWxLogin">重新登录</button>
    </view>
  </view>
</template>

<script>
import { useUserStore } from '@/store/user'

export default {
  onShow() {
    // 如果已登录，直接跳转首页
    const userStore = useUserStore()
    if (userStore.isLoggedIn) {
      uni.switchTab({ url: '/pages/inventory/index' })
    }
  },
  methods: {
    async handleWxLogin() {
      try {
        const userStore = useUserStore()
        await userStore.silentWxLogin()
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/inventory/index' })
        }, 1000)
      } catch (e) {
        uni.showToast({ title: '登录失败，请重试', icon: 'none' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page-login {
  min-height: 100vh;
  background: linear-gradient(180deg, #2979ff 0%, #e8f0fe 100%);
  padding: 0 48rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-header {
  text-align: center;
  margin-bottom: 80rpx;
}

.login-title {
  display: block;
  font-size: 56rpx;
  font-weight: 700;
  color: #fff;
  margin-bottom: 16rpx;
}

.login-subtitle {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
}

.login-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 60rpx 36rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.1);
  text-align: center;
}

.login-tip {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 16rpx;
}

.login-desc {
  display: block;
  font-size: 26rpx;
  color: #999;
  margin-bottom: 48rpx;
}

.btn-login {
  width: 100%;
  height: 88rpx;
  background: #2979ff;
  color: #fff;
  border: none;
  border-radius: 12rpx;
  font-size: 32rpx;
}
</style>
