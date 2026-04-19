<template>
  <view class="page-checkout">
    <NavBar title="确认订单" />

    <view class="container">
      <!-- 收货信息 -->
      <view class="form-card">
        <text class="form-card__title">收货信息</text>
        <view class="form-row">
          <text class="form-row__label">联系人 <text class="required">*</text></text>
          <input class="form-row__input" v-model="form.contactName" placeholder="请输入联系人姓名" />
        </view>
        <view class="form-row">
          <text class="form-row__label">联系电话 <text class="required">*</text></text>
          <input class="form-row__input" v-model="form.contactPhone" placeholder="请输入联系电话" type="number" />
        </view>
        <view class="form-row">
          <text class="form-row__label">收货地址</text>
          <input class="form-row__input" v-model="form.address" placeholder="请输入收货地址" />
        </view>
        <view class="form-row form-row--textarea">
          <text class="form-row__label">备注</text>
          <textarea class="form-row__textarea" v-model="form.remark" placeholder="留言备注（选填）" />
        </view>
      </view>

      <!-- 订单商品 -->
      <view class="form-card">
        <text class="form-card__title">订单商品</text>
        <view v-for="item in cartItems" :key="item.productId" class="order-line">
          <text class="order-line__name">{{ item.name }}</text>
          <text class="order-line__qty">×{{ item.quantity }}</text>
          <text class="order-line__amount">¥{{ (item.price * item.quantity).toFixed(2) }}</text>
        </view>
        <view class="order-total">
          <text class="order-total__label">合计</text>
          <text class="order-total__price">¥{{ totalAmount.toFixed(2) }}</text>
        </view>
      </view>

      <!-- 提交按钮 -->
      <button class="btn-submit" hover-class="btn-submit--hover" @tap="handleSubmit" :loading="submitting">
        {{ submitting ? '提交中...' : '提交订单' }}
      </button>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import { useCartStore } from '@/store/cart'
import { useUserStore } from '@/store/user'
import { createBuyerOrder } from '@/api/buyer'

export default {
  components: { NavBar },
  data() {
    return {
      enterpriseId: '',
      buyNowItem: null,
      form: { contactName: '', contactPhone: '', address: '', remark: '' },
      submitting: false
    }
  },
  computed: {
    cartItems() {
      return this.buyNowItem ? [this.buyNowItem] : useCartStore().items
    },
    totalAmount() {
      return this.buyNowItem
        ? Number(this.buyNowItem.price || 0) * this.buyNowItem.quantity
        : useCartStore().totalAmount
    }
  },
  onLoad(query) {
    this.enterpriseId = query.enterpriseId || ''
    if (query.buyNow === '1') {
      const item = uni.getStorageSync('buyNowItem')
      if (item && item.productId) this.buyNowItem = item
      uni.removeStorageSync('buyNowItem')
    }
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      uni.showModal({
        title: '请先登录',
        content: '下单需要登录账号',
        showCancel: false,
        success: () => { uni.navigateTo({ url: '/pages/auth/login' }) }
      })
      return
    }
    const savedAddr = uni.getStorageSync('buyerAddress')
    if (savedAddr) {
      this.form.contactName = savedAddr.contactName || ''
      this.form.contactPhone = savedAddr.contactPhone || ''
      this.form.address = savedAddr.address || ''
    } else {
      const user = userStore.userInfo
      if (user.phone) this.form.contactPhone = user.phone
      if (user.nickName) this.form.contactName = user.nickName
    }
  },
  methods: {
    async handleSubmit() {
      if (!this.form.contactName) return uni.showToast({ title: '请输入联系人', icon: 'none' })
      if (!this.form.contactPhone) return uni.showToast({ title: '请输入联系电话', icon: 'none' })
      if (!this.cartItems.length) return uni.showToast({ title: '购物车为空', icon: 'none' })
      this.submitting = true
      try {
        const orderData = {
          enterpriseId: this.enterpriseId,
          contactName: this.form.contactName,
          contactPhone: this.form.contactPhone,
          address: this.form.address,
          remark: this.form.remark,
          items: this.cartItems.map(item => ({
            productId: item.productId,
            quantity: item.quantity,
            price: item.price
          }))
        }
        const res = await createBuyerOrder(orderData)
        uni.setStorageSync('buyerAddress', {
          contactName: this.form.contactName,
          contactPhone: this.form.contactPhone,
          address: this.form.address
        })
        const finalAmount = this.totalAmount
        if (!this.buyNowItem) useCartStore().clearCart()
        uni.redirectTo({
          url: `/pages/buyer/pay-success?orderId=${res?.id || ''}&amount=${finalAmount.toFixed(2)}&enterpriseId=${this.enterpriseId}`
        })
      } catch (e) {
        uni.showToast({ title: e.message || '提交失败', icon: 'none' })
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page-checkout {
  background: #f7f8fa;
  min-height: 100vh;
}
.form-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}
.form-card__title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 24rpx;
}
.form-row {
  margin-bottom: 20rpx;
}
.form-row--textarea { margin-bottom: 0; }
.form-row__label {
  display: block;
  font-size: 24rpx;
  color: #888;
  margin-bottom: 8rpx;
  font-weight: 500;
}
.required { color: #ff4d4f; }
.form-row__input {
  width: 100%;
  height: 76rpx;
  background: #f7f8fa;
  border: 1rpx solid transparent;
  border-radius: 14rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #1a1a1a;
  box-sizing: border-box;
  transition: border-color 0.2s;
  &:focus { border-color: #2979ff; background: #fff; }
}
.form-row__textarea {
  width: 100%;
  height: 160rpx;
  background: #f7f8fa;
  border: 1rpx solid transparent;
  border-radius: 14rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
  color: #1a1a1a;
  box-sizing: border-box;
  &:focus { border-color: #2979ff; background: #fff; }
}
.order-line {
  display: flex;
  align-items: center;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  &:last-child { border-bottom: none; }
}
.order-line__name {
  flex: 1;
  font-size: 26rpx;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.order-line__qty {
  font-size: 24rpx;
  color: #aaa;
  margin: 0 24rpx 0 12rpx;
}
.order-line__amount {
  font-size: 26rpx;
  color: #1a1a1a;
  font-weight: 600;
}
.order-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20rpx;
  margin-top: 8rpx;
  border-top: 1rpx solid #f0f0f0;
}
.order-total__label {
  font-size: 28rpx;
  color: #1a1a1a;
  font-weight: 600;
}
.order-total__price {
  font-size: 36rpx;
  color: #ff4d4f;
  font-weight: 700;
}
.btn-submit {
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
  margin-top: 24rpx;
  letter-spacing: 2rpx;
}
.btn-submit--hover { opacity: 0.85; }
</style>
