<template>
  <view class="page-product-detail">
    <!-- 商品图片 -->
    <view class="product-image-wrap">
      <image v-if="product.mainImage" :src="product.mainImage" class="product-image" mode="aspectFill" />
      <view v-else class="product-image placeholder">
        <text style="font-size:80rpx">📦</text>
      </view>
    </view>

    <!-- 基本信息 -->
    <view class="info-section container">
      <view class="price-row">
        <text class="price">¥{{ product.price || '0.00' }}</text>
        <text class="unit">/{{ product.unit || '件' }}</text>
      </view>
      <text class="product-name">{{ product.name }}</text>
      <text class="product-spec" v-if="product.spec">规格：{{ product.spec }}</text>
      <text class="stock-info" v-if="product.stock != null">库存：{{ product.stock }}{{ product.unit || '件' }}</text>
    </view>

    <!-- 数量选择 -->
    <view class="quantity-section container card">
      <text class="section-label">购买数量</text>
      <view class="quantity-control">
        <text class="qty-btn" @tap="changeQty(-1)">－</text>
        <input class="qty-input" type="number" v-model="quantity" @blur="checkQty" />
        <text class="qty-btn" @tap="changeQty(1)">＋</text>
      </view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar safe-area-bottom">
      <button class="btn-cart" @tap="handleAddCart">加入购物车</button>
      <button class="btn-buy" @tap="handleBuyNow">立即购买</button>
    </view>
  </view>
</template>

<script>
import { getProductDetail } from '@/api/buyer'
import { useCartStore } from '@/store/cart'

export default {
  data() {
    return {
      enterpriseId: '',
      productId: '',
      product: {},
      quantity: 1
    }
  },
  onLoad(query) {
    this.enterpriseId = query.enterpriseId || ''
    this.productId = query.productId || ''
    this.loadDetail()
  },
  methods: {
    async loadDetail() {
      try {
        const res = await getProductDetail(this.productId)
        this.product = res.data || {}
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    changeQty(delta) {
      const n = Number(this.quantity) + delta
      if (n < 1) return
      if (this.product.stock && n > this.product.stock) {
        return uni.showToast({ title: '超出库存', icon: 'none' })
      }
      this.quantity = n
    },
    checkQty() {
      let n = parseInt(this.quantity)
      if (isNaN(n) || n < 1) n = 1
      if (this.product.stock && n > this.product.stock) n = this.product.stock
      this.quantity = n
    },
    handleAddCart() {
      const cart = useCartStore()
      cart.setEnterprise(this.enterpriseId)
      cart.addItem({
        productId: this.product.id,
        name: this.product.name,
        spec: this.product.spec,
        unit: this.product.unit,
        price: this.product.price,
        quantity: Number(this.quantity),
        stock: this.product.stock,
        image: this.product.mainImage
      })
      uni.showToast({ title: '已加入购物车' })
    },
    handleBuyNow() {
      this.handleAddCart()
      uni.navigateTo({ url: `/pages/buyer/cart?enterpriseId=${this.enterpriseId}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.product-image-wrap {
  width: 100%;
  height: 600rpx;
}
.product-image {
  width: 100%;
  height: 100%;
  &.placeholder {
    background: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
.info-section {
  padding-top: 24rpx;
}
.price-row {
  display: flex;
  align-items: baseline;
  margin-bottom: 12rpx;
}
.price {
  font-size: 48rpx;
  color: #ff4d4f;
  font-weight: 700;
}
.unit {
  font-size: 24rpx;
  color: #999;
  margin-left: 4rpx;
}
.product-name {
  display: block;
  font-size: 32rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 8rpx;
}
.product-spec, .stock-info {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-bottom: 4rpx;
}
.quantity-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20rpx;
}
.section-label {
  font-size: 28rpx;
  color: #333;
}
.quantity-control {
  display: flex;
  align-items: center;
  gap: 0;
}
.qty-btn {
  width: 64rpx;
  height: 64rpx;
  line-height: 64rpx;
  text-align: center;
  background: #f5f5f5;
  font-size: 32rpx;
  color: #333;
  border-radius: 8rpx;
}
.qty-input {
  width: 100rpx;
  height: 64rpx;
  text-align: center;
  font-size: 28rpx;
  border: 1rpx solid #eee;
  margin: 0 4rpx;
}
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.06);
}
.btn-cart, .btn-buy {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  text-align: center;
  border-radius: 42rpx;
  font-size: 28rpx;
  font-weight: 600;
}
.btn-cart {
  background: #ff9900;
  color: #fff;
}
.btn-buy {
  background: #ff4d4f;
  color: #fff;
}
</style>
