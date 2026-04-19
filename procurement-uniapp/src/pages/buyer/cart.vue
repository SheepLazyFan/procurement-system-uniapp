<template>
  <view class="page-cart">
    <NavBar title="购物车" />

    <view class="container" v-if="!isEmpty">
      <!-- 商品列表 -->
      <view class="cart-card" v-for="item in cartItems" :key="item.productId">
        <image v-if="item.image" :src="$fileUrl(item.image)" class="cart-card__img" mode="aspectFill" />
        <view v-else class="cart-card__img-placeholder">
          <view class="svg-icon-pkg"></view>
        </view>
        <view class="cart-card__body">
          <text class="cart-card__name">{{ item.name }}</text>
          <text class="cart-card__spec" v-if="item.spec">{{ item.spec }}</text>
          <view class="cart-card__bottom">
            <text class="cart-card__price">¥{{ item.price }}</text>
            <view class="cart-card__actions">
              <view class="qty-mini">
                <text class="qty-mini__btn" @tap="changeQty(item, -1)">－</text>
                <text class="qty-mini__val">{{ item.quantity }}</text>
                <text class="qty-mini__btn" @tap="changeQty(item, 1)">＋</text>
              </view>
              <text class="cart-card__remove" @tap="handleRemove(item)">删除</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <EmptyState v-else text="购物车是空的" icon="🛒" buttonText="去逛逛" @action="goBack" />

    <!-- 底部结算栏 -->
    <view v-if="!isEmpty" class="settle-bar safe-area-bottom">
      <view class="settle-bar__info">
        <text class="settle-bar__label">合计：</text>
        <text class="settle-bar__price">¥{{ totalAmount.toFixed(2) }}</text>
      </view>
      <button class="settle-bar__btn" hover-class="settle-bar__btn--hover" @tap="goCheckout">
        去结算({{ totalCount }})
      </button>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useCartStore } from '@/store/cart'

export default {
  components: { NavBar, EmptyState },
  data() {
    return { enterpriseId: '' }
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
.page-cart {
  background: #f7f8fa;
  min-height: 100vh;
  padding-bottom: 130rpx;
}
.svg-icon-pkg {
  width: 48rpx; height: 48rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ccc' stroke-width='1.5'%3E%3Cpath d='M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z'/%3E%3Cpolyline points='3.27 6.96 12 12.01 20.73 6.96'/%3E%3Cline x1='12' y1='22.08' x2='12' y2='12'/%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}
.cart-card {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}
.cart-card__img {
  width: 160rpx; height: 160rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}
.cart-card__img-placeholder {
  width: 160rpx; height: 160rpx;
  border-radius: 12rpx;
  background: #f5f6fa;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.cart-card__body {
  flex: 1;
  min-width: 0;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.cart-card__name {
  font-size: 28rpx;
  color: #1a1a1a;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cart-card__spec {
  font-size: 22rpx;
  color: #aaa;
  margin-top: 4rpx;
}
.cart-card__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
}
.cart-card__price {
  font-size: 30rpx;
  color: #ff4d4f;
  font-weight: 700;
}
.cart-card__actions {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.qty-mini {
  display: flex;
  align-items: center;
  background: #f5f6fa;
  border-radius: 10rpx;
  overflow: hidden;
}
.qty-mini__btn {
  width: 48rpx; height: 44rpx;
  line-height: 44rpx;
  text-align: center;
  font-size: 26rpx;
  color: #555;
}
.qty-mini__val {
  width: 52rpx;
  text-align: center;
  font-size: 26rpx;
  color: #1a1a1a;
  font-weight: 600;
  background: #fff;
}
.cart-card__remove {
  font-size: 22rpx;
  color: #e43d33;
}
/* 底部结算栏 */
.settle-bar {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid #f0f0f0;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.04);
}
.settle-bar__info {
  display: flex;
  align-items: baseline;
}
.settle-bar__label {
  font-size: 26rpx;
  color: #666;
}
.settle-bar__price {
  font-size: 36rpx;
  color: #ff4d4f;
  font-weight: 700;
}
.settle-bar__btn {
  min-width: 220rpx;
  height: 76rpx;
  line-height: 76rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 38rpx;
  text-align: center;
  border: none;
}
.settle-bar__btn--hover { opacity: 0.85; }
</style>
