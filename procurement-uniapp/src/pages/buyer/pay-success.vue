<template>
  <view class="page-pay-success">
    <view class="success-icon">✅</view>
    <text class="success-title">支付成功</text>
    <text class="success-amount" v-if="amount">¥{{ amount }}</text>
    <text class="success-tip">商家将尽快处理您的订单</text>

    <view class="action-buttons">
      <button class="btn-secondary" @tap="goOrders">查看订单</button>
      <button class="btn-primary" @tap="goStore">继续购物</button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      orderId: '',
      amount: '',
      enterpriseId: ''
    }
  },
  onLoad(query) {
    this.orderId = query.orderId || ''
    this.amount = query.amount || ''
    this.enterpriseId = query.enterpriseId || ''
  },
  methods: {
    goOrders() {
      uni.redirectTo({ url: '/pages/buyer/orders' })
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
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  padding: 40rpx;
}
.success-icon {
  font-size: 120rpx;
  margin-bottom: 24rpx;
}
.success-title {
  font-size: 36rpx;
  color: #333;
  font-weight: 700;
  margin-bottom: 16rpx;
}
.success-amount {
  font-size: 48rpx;
  color: #ff4d4f;
  font-weight: 700;
  margin-bottom: 12rpx;
}
.success-tip {
  font-size: 26rpx;
  color: #999;
  margin-bottom: 60rpx;
}
.action-buttons {
  display: flex;
  gap: 24rpx;
  width: 100%;
  padding: 0 40rpx;
}
.btn-secondary {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  border: 1rpx solid #2979ff;
  color: #2979ff;
  background: #fff;
  border-radius: 42rpx;
  font-size: 28rpx;
}
.btn-primary {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  background: #2979ff;
  color: #fff;
  border-radius: 42rpx;
  font-size: 28rpx;
}
</style>
