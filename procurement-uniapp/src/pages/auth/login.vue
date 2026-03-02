<template>
  <view class="page-login">
    <view class="login-header">
      <text class="login-title">采购系统</text>
      <text class="login-subtitle">B2B 进销存管理平台</text>
    </view>

    <view class="login-form">
      <view class="form-item">
        <input
          v-model="phone"
          type="number"
          maxlength="11"
          placeholder="请输入手机号"
          class="form-input"
        />
      </view>
      <view class="form-item form-item--code">
        <input
          v-model="code"
          type="number"
          maxlength="6"
          placeholder="请输入验证码"
          class="form-input"
        />
        <view
          class="code-btn"
          :class="{ 'code-btn--disabled': countdown > 0 }"
          @tap="handleSendCode"
        >
          <text class="code-btn-text">
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </text>
        </view>
      </view>
      <button class="btn-login" @tap="handleLogin">登录</button>
    </view>
  </view>
</template>

<script>
import { sendSmsCode } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { isValidPhone, isValidSmsCode } from '@/utils/validate'

export default {
  data() {
    return {
      phone: '',
      code: '',
      countdown: 0,
      timer: null
    }
  },
  onUnload() {
    if (this.timer) clearInterval(this.timer)
  },
  methods: {
    async handleSendCode() {
      if (this.countdown > 0) return
      if (!isValidPhone(this.phone)) {
        return uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
      }
      try {
        await sendSmsCode(this.phone)
        uni.showToast({ title: '验证码已发送', icon: 'success' })
        this.countdown = 60
        this.timer = setInterval(() => {
          this.countdown--
          if (this.countdown <= 0) clearInterval(this.timer)
        }, 1000)
      } catch (e) {
        // request.js 已处理错误提示
      }
    },
    async handleLogin() {
      if (!isValidPhone(this.phone)) {
        return uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
      }
      if (!isValidSmsCode(this.code)) {
        return uni.showToast({ title: '请输入6位验证码', icon: 'none' })
      }
      try {
        const userStore = useUserStore()
        await userStore.loginByPhone(this.phone, this.code)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/inventory/index' })
        }, 1000)
      } catch (e) {
        // request.js 已处理错误提示
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

.login-form {
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx 36rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.1);
}

.form-item {
  margin-bottom: 32rpx;

  &--code {
    display: flex;
    align-items: center;
  }
}

.form-input {
  flex: 1;
  height: 88rpx;
  background: #f5f6fa;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
}

.code-btn {
  margin-left: 16rpx;
  background: #2979ff;
  border-radius: 12rpx;
  padding: 0 24rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  white-space: nowrap;

  &--disabled {
    background: #ccc;
  }
}

.code-btn-text {
  color: #fff;
  font-size: 26rpx;
}

.btn-login {
  width: 100%;
  height: 88rpx;
  background: #2979ff;
  color: #fff;
  border: none;
  border-radius: 12rpx;
  font-size: 32rpx;
  margin-top: 16rpx;
}
</style>
