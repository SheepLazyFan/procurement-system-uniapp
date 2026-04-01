<template>
  <view class="page-checkout container">
    <NavBar title="确认订单" />

    <!-- 收货信息 -->
    <view class="card">
      <view class="section-title">收货信息</view>
      <view class="form-group">
        <text class="form-label">联系人 *</text>
        <input class="form-input" v-model="form.contactName" placeholder="请输入联系人姓名" />
      </view>
      <view class="form-group">
        <text class="form-label">联系电话 *</text>
        <input class="form-input" v-model="form.contactPhone" placeholder="请输入联系电话" type="number" />
      </view>
      <view class="form-group">
        <text class="form-label">收货地址</text>
        <input class="form-input" v-model="form.address" placeholder="请输入收货地址" />
      </view>
      <view class="form-group">
        <text class="form-label">备注</text>
        <textarea class="form-textarea" v-model="form.remark" placeholder="留言备注（选填）" />
      </view>
    </view>

    <!-- 订单商品 -->
    <view class="card">
      <view class="section-title">订单商品</view>
      <view v-for="item in cartItems" :key="item.productId" class="order-item">
        <text class="item-name">{{ item.name }}</text>
        <text class="item-qty">×{{ item.quantity }}</text>
        <text class="item-amount">¥{{ (item.price * item.quantity).toFixed(2) }}</text>
      </view>
      <view class="divider"></view>
      <view class="total-row">
        <text class="total-label">合计</text>
        <text class="total-price">¥{{ totalAmount.toFixed(2) }}</text>
      </view>
    </view>

    <!-- 提交按钮 -->
    <button class="btn-primary" @tap="handleSubmit" :loading="submitting">
      {{ submitting ? '支付中...' : '确认支付' }}
    </button>
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
      /** 非 null 时为立即购买模式，直接用此商品结算，不读取/清空购物车 */
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

    // 立即购买模式：读取暂存商品，读后立即删除，避免二次误用
    if (query.buyNow === '1') {
      const item = uni.getStorageSync('buyNowItem')
      if (item && item.productId) {
        this.buyNowItem = item
      }
      uni.removeStorageSync('buyNowItem')
    }

    // 登录检查
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      uni.showModal({
        title: '请先登录',
        content: '下单需要登录账号',
        showCancel: false,
        success: () => {
          uni.navigateTo({ url: '/pages/auth/login' })
        }
      })
      return
    }

    // 还原上次填写的收货地址
    const savedAddr = uni.getStorageSync('buyerAddress')
    if (savedAddr) {
      this.form.contactName = savedAddr.contactName || ''
      this.form.contactPhone = savedAddr.contactPhone || ''
      this.form.address = savedAddr.address || ''
    } else {
      // 首次使用：预填用户信息
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

        // 保存本次收货地址，下次自动填充
        uni.setStorageSync('buyerAddress', {
          contactName: this.form.contactName,
          contactPhone: this.form.contactPhone,
          address: this.form.address
        })

        // 先记录金额，再清购物车（清空后 totalAmount 会变 0）
        const finalAmount = this.totalAmount
        // 立即购买模式不清空购物车，购物车模式才清
        if (!this.buyNowItem) {
          useCartStore().clearCart()
        }
        // 跳转支付成功页
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
.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
}
.form-group { margin-bottom: 20rpx; }
.form-label {
  display: block;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 8rpx;
}
.form-input {
  width: 100%;
  height: 76rpx;
  border: 1rpx solid #e5e5e5;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}
.form-textarea {
  width: 100%;
  height: 160rpx;
  border: 1rpx solid #e5e5e5;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}
.order-item {
  display: flex;
  align-items: center;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  &:last-child { border-bottom: none; }
}
.item-name {
  flex: 1;
  font-size: 26rpx;
  color: #333;
}
.item-qty {
  font-size: 26rpx;
  color: #999;
  margin-right: 24rpx;
}
.item-amount {
  font-size: 26rpx;
  color: #333;
  font-weight: 500;
}
.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16rpx;
}
.total-label {
  font-size: 28rpx;
  color: #333;
}
.total-price {
  font-size: 36rpx;
  color: #ff4d4f;
  font-weight: 700;
}
</style>
