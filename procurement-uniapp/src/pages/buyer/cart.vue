<template>
  <view class="page-cart container">
    <NavBar title="购物车" />

    <view v-if="!isEmpty">
      <!-- 商品列表 -->
      <view class="card" v-for="item in cartItems" :key="item.productId">
        <view class="cart-item">
          <image v-if="item.image" :src="$fileUrl(item.image)" class="item-img" mode="aspectFill" />
          <view v-else class="item-img-placeholder">📦</view>
          <view class="item-info">
            <text class="item-name">{{ item.name }}</text>
            <text class="item-spec" v-if="item.spec">{{ item.spec }}</text>
            <text class="item-price">¥{{ item.price }}</text>
          </view>
          <view class="item-right">
            <view class="quantity-control">
              <text class="qty-btn" @tap="changeQty(item, -1)">－</text>
              <text class="qty-text">{{ item.quantity }}</text>
              <text class="qty-btn" @tap="changeQty(item, 1)">＋</text>
            </view>
            <text class="item-remove" @tap="handleRemove(item)">删除</text>
          </view>
        </view>
      </view>

      <!-- 底部结算栏 -->
      <view class="bottom-bar safe-area-bottom">
        <view class="total-section">
          <text class="total-label">合计：</text>
          <text class="total-price">¥{{ totalAmount.toFixed(2) }}</text>
        </view>
        <button class="btn-checkout" @tap="goCheckout">
          去结算({{ totalCount }})
        </button>
      </view>
    </view>

    <EmptyState v-else text="购物车是空的" icon="🛒" buttonText="去逛逛" @action="goBack" />
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useCartStore } from '@/store/cart'

export default {
  components: { NavBar, EmptyState },
  data() {
    return {
      enterpriseId: ''
    }
  },
  computed: {
    cartItems() { return useCartStore().items },
    totalCount() { return useCartStore().totalCount },
    totalAmount() { return useCartStore().totalAmount },
    isEmpty() { return useCartStore().isEmpty }
  },
  onLoad(query) {
    this.enterpriseId = query.enterpriseId || ''
    useCartStore().restoreFromStorage()
  },
  onShow() {
    // 从结算页/其他页面返回时，微信小程序不保证后台页面响应式更新
    // 主动从 storage 同步最新状态（clearCart 已写入 storage）
    useCartStore().restoreFromStorage()
  },
  methods: {
    changeQty(item, delta) {
      const newQty = item.quantity + delta
      if (newQty < 1) return
      useCartStore().updateQuantity(item.productId, newQty)
    },
    handleRemove(item) {
      uni.showModal({
        title: '提示',
        content: `删除 ${item.name}？`,
        success: (res) => {
          if (res.confirm) useCartStore().removeItem(item.productId)
        }
      })
    },
    goCheckout() {
      uni.navigateTo({ url: `/pages/buyer/checkout?enterpriseId=${this.enterpriseId}` })
    },
    goBack() {
      const eid = useCartStore().enterpriseId || this.enterpriseId
      if (eid) {
        uni.navigateTo({ url: `/pages/buyer/store?enterpriseId=${eid}` })
      } else {
        uni.navigateBack({ delta: 1 })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.cart-item {
  display: flex;
  gap: 16rpx;
}
.item-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}
.item-img-placeholder {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  flex-shrink: 0;
}
.item-info {
  flex: 1;
  min-width: 0;
}
.item-name {
  display: block;
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-spec {
  display: block;
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}
.item-price {
  display: block;
  font-size: 28rpx;
  color: #ff4d4f;
  font-weight: 600;
  margin-top: 12rpx;
}
.item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
}
.quantity-control {
  display: flex;
  align-items: center;
}
.qty-btn {
  width: 52rpx;
  height: 52rpx;
  line-height: 52rpx;
  text-align: center;
  background: #f5f5f5;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: #333;
}
.qty-text {
  width: 64rpx;
  text-align: center;
  font-size: 28rpx;
  color: #333;
}
.item-remove {
  font-size: 22rpx;
  color: #e43d33;
}
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx;
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.06);
}
.total-section {
  display: flex;
  align-items: baseline;
}
.total-label {
  font-size: 26rpx;
  color: #666;
}
.total-price {
  font-size: 36rpx;
  color: #ff4d4f;
  font-weight: 700;
}
.btn-checkout {
  min-width: 240rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 40rpx;
  text-align: center;
}
</style>
