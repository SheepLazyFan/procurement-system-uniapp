<template>
  <view class="page-pay-success">
    <!-- 成功动画区 -->
    <view class="success-hero">
      <view class="success-circle">
        <view class="svg-icon-check"></view>
      </view>
      <text class="success-title">订单已提交</text>
      <text class="success-amount" v-if="amount">¥{{ amount }}</text>
    </view>

    <!-- 付款引导 -->
    <view class="guide-card">
      <view class="guide-card__icon">
        <view class="svg-icon-clock"></view>
      </view>
      <view class="guide-card__content">
        <text class="guide-card__title">请尽快完成付款</text>
        <text class="guide-card__desc">在订单页查看收款码，完成线下扫码付款后提交付款声明</text>
        <view class="guide-card__countdown" v-if="countdown > 0">
          <text class="countdown-label">建议在</text>
          <text class="countdown-time">{{ countdownText }}</text>
          <text class="countdown-label">内完成付款</text>
        </view>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="action-group">
      <button class="btn-go-pay" hover-class="btn-go-pay--hover" @tap="goOrders">查看订单并付款</button>
      <button class="btn-continue" hover-class="btn-continue--hover" @tap="goStore">继续购物</button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      orderId: '',
      amount: '',
      enterpriseId: '',
      countdown: 30 * 60, // 30 分钟倒计时（秒）
      timer: null
    }
  },
  computed: {
    countdownText() {
      const m = Math.floor(this.countdown / 60)
      const s = this.countdown % 60
      return `${m}分${s < 10 ? '0' : ''}${s}秒`
    }
  },
  onLoad(query) {
    this.orderId = query.orderId || ''
    this.amount = query.amount || ''
    this.enterpriseId = query.enterpriseId || ''
    this.startCountdown()
  },
  onUnload() {
    if (this.timer) clearInterval(this.timer)
  },
  methods: {
    startCountdown() {
      this.timer = setInterval(() => {
        if (this.countdown <= 0) {
          clearInterval(this.timer)
          return
        }
        this.countdown--
      }, 1000)
    },
    goOrders() {
      if (this.orderId) {
        uni.redirectTo({ url: `/pages/buyer/order-detail?id=${this.orderId}` })
      } else {
        uni.redirectTo({ url: '/pages/buyer/orders' })
      }
    },
    goStore() {
      if (this.enterpriseId) {
        uni.navigateTo({ url: `/pages/buyer/store?enterpriseId=${this.enterpriseId}` })
      } else {
        uni.navigateBack({ delta: 10 })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page-pay-success {
  min-height: 100vh;
  background: #f7f8fa;
  padding: 0 32rpx;
}

/* SVG 图标 */
.svg-icon-check {
  width: 64rpx; height: 64rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23fff' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='20 6 9 17 4 12'%3E%3C/polyline%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}
.svg-icon-clock {
  width: 36rpx; height: 36rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ff9900' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='12' cy='12' r='10'%3E%3C/circle%3E%3Cpolyline points='12 6 12 12 16 14'%3E%3C/polyline%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

/* 成功动画区 */
.success-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0 48rpx;
}
.success-circle {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  background: #52c41a;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(82, 196, 26, 0.3);
  animation: scaleIn 0.4s ease both;
}
@keyframes scaleIn {
  0% { transform: scale(0.5); opacity: 0; }
  60% { transform: scale(1.1); }
  100% { transform: scale(1); opacity: 1; }
}
.success-title {
  font-size: 36rpx;
  color: #1a1a1a;
  font-weight: 700;
  margin-top: 28rpx;
}
.success-amount {
  font-size: 52rpx;
  color: #ff4d4f;
  font-weight: 700;
  margin-top: 12rpx;
  letter-spacing: -2rpx;
}

/* 付款引导卡片 */
.guide-card {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  margin-top: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.guide-card__icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 14rpx;
  background: #fff8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 20rpx;
}
.guide-card__content {
  flex: 1;
  min-width: 0;
}
.guide-card__title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 6rpx;
}
.guide-card__desc {
  display: block;
  font-size: 24rpx;
  color: #999;
  line-height: 1.6;
}
.guide-card__countdown {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  padding: 12rpx 16rpx;
  background: #fffbe6;
  border-radius: 10rpx;
}
.countdown-label {
  font-size: 22rpx;
  color: #999;
}
.countdown-time {
  font-size: 26rpx;
  color: #ff9900;
  font-weight: 700;
  margin: 0 6rpx;
}

/* 操作按钮 */
.action-group {
  margin-top: 48rpx;
  padding: 0 16rpx;
}
.btn-go-pay {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #2979ff;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 44rpx;
  text-align: center;
  border: none;
  letter-spacing: 2rpx;
}
.btn-go-pay--hover { opacity: 0.85; }
.btn-continue {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #fff;
  color: #666;
  font-size: 28rpx;
  border-radius: 44rpx;
  text-align: center;
  border: 1rpx solid #e8e8e8;
  margin-top: 20rpx;
}
.btn-continue--hover { background: #f9f9f9; }
</style>
