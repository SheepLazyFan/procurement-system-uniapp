<template>
  <view class="page-product-detail">
    <!-- 加载中 -->
    <view v-if="loading" class="loading-wrap">
      <text class="loading-text">加载中...</text>
    </view>

    <!-- 加载失败 -->
    <view v-else-if="loadError" class="error-wrap">
      <text class="error-emoji">⚠️</text>
      <text class="error-text">加载失败，请返回重试</text>
    </view>

    <!-- 正常内容 -->
    <block v-else>
    <!-- 商品图片 -->
    <view class="product-image-wrap">
      <image v-if="product.mainImage" :src="$fileUrl(product.mainImage)" class="product-image" mode="aspectFill" />
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
      <text v-if="product.stockStatus === 'OUT_OF_STOCK'" class="stock-info stock-out">缺货</text>
      <text v-else-if="product.stockStatus === 'LOW_STOCK'" class="stock-info stock-low">仅剩 {{ product.stock }} 件</text>
      <text v-else class="stock-info stock-in">有货</text>
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

    <!-- 二维码图片（扫码看演示视频等） — TODO: 后续部署迁移 COS 后 URL 会变化 -->
    <view v-if="product.qrcodeImage" class="qrcode-section container card">
      <text class="section-label">扫码查看演示视频</text>
      <view class="qrcode-wrap">
        <image
          :src="$fileUrl(product.qrcodeImage)"
          class="qrcode-img"
          mode="aspectFit"
          show-menu-by-longpress
          @tap="previewQrcode"
        />
        <text class="qrcode-hint">点击放大 · 长按识别二维码</text>
        <view class="qrcode-save-btn" hover-class="qrcode-save-btn--hover" @tap="saveQrcodeToAlbum">
          <text class="qrcode-save-text">保存图片 · 用微信扫一扫</text>
        </view>
      </view>
    </view>

    <!-- 商品描述 -->
    <view v-if="product.description" class="desc-section container card">
      <text class="section-label">商品介绍</text>
      <text class="desc-content">{{ product.description }}</text>
    </view>
    </block>

    <!-- 底部操作栏（始终显示，让用户能返回） -->
    <view class="bottom-bar safe-area-bottom">
      <view class="btn-cart-nav" @tap="goCart">
        <view class="cart-nav-wrap">
          <text>🛒</text>
          <text v-if="cartInStore > 0" class="cart-nav-badge">{{ cartInStore > 99 ? '99+' : cartInStore }}</text>
        </view>
      </view>
      <button class="btn-cart" :disabled="!product.id || product.stockStatus === 'OUT_OF_STOCK'" @tap="handleAddCart">{{ product.stockStatus === 'OUT_OF_STOCK' ? '已售罄' : '加入购物车' }}</button>
      <button class="btn-buy" :disabled="!product.id || product.stockStatus === 'OUT_OF_STOCK'" @tap="handleBuyNow">{{ product.stockStatus === 'OUT_OF_STOCK' ? '缺货中' : '立即购买' }}</button>
    </view>
  </view>
</template>

<script>
import { getProductDetail } from '@/api/buyer'
import { useCartStore } from '@/store/cart'

export default {
  computed: {
    cartInStore() {
      const cart = useCartStore()
      return cart.enterpriseId === this.enterpriseId ? cart.totalCount : 0
    }
  },
  data() {
    return {
      enterpriseId: '',
      productId: '',
      product: {},
      quantity: 1,
      loading: false,
      loadError: false
    }
  },
  onLoad(query) {
    this.enterpriseId = query.enterpriseId || ''
    this.productId = query.productId || ''
    // 恢复购物车缓存，确保嵌章数显示正确
    useCartStore().restoreFromStorage()
    if (!this.productId) {
      uni.showToast({ title: '参数错误', icon: 'none' })
      return
    }
    this.loadDetail()
  },
  onShareAppMessage() {
    return {
      title: this.product.name || '商品详情',
      path: `/pages/buyer/product-detail?enterpriseId=${this.enterpriseId}&productId=${this.productId}`,
      imageUrl: this.product.mainImage || ''
    }
  },
  onShareTimeline() {
    return {
      title: this.product.name || '商品详情',
      query: `enterpriseId=${this.enterpriseId}&productId=${this.productId}`
    }
  },
  methods: {
    async loadDetail() {
      this.loading = true
      this.loadError = false
      try {
        const res = await getProductDetail(this.productId)
        if (res && res.id) {
          this.product = res
        } else {
          this.loadError = true
          uni.showToast({ title: '商品不存在', icon: 'none' })
        }
      } catch (e) {
        this.loadError = true
        uni.showToast({ title: '加载失败，请检查网络', icon: 'none', duration: 3000 })
        console.error('[product-detail] loadDetail失败', e)
      } finally {
        this.loading = false
      }
    },
    changeQty(delta) {
      const n = Number(this.quantity) + delta
      if (n < 1) return
      this.quantity = n
    },
    checkQty() {
      let n = parseInt(this.quantity)
      if (isNaN(n) || n < 1) n = 1
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
        image: this.product.mainImage
      })
      uni.showToast({ title: '已加入购物车' })
    },
    handleBuyNow() {
      const qty = Number(this.quantity)
      if (isNaN(qty) || qty < 1) {
        uni.showToast({ title: '请输入有效数量', icon: 'none' })
        return
      }
      // 立即购买：暂存当前商品，不写入购物车，不影响已有购物车内容
      uni.setStorageSync('buyNowItem', {
        productId: this.product.id,
        name: this.product.name,
        spec: this.product.spec || '',
        unit: this.product.unit || '',
        price: this.product.price,
        quantity: qty,
        image: this.product.mainImage || ''
      })
      uni.navigateTo({ url: `/pages/buyer/checkout?enterpriseId=${this.enterpriseId}&buyNow=1` })
    },
    previewQrcode() {
      if (this.product.qrcodeImage) {
        const url = this.$fileUrl(this.product.qrcodeImage)
        uni.previewImage({
          urls: [url],
          current: url
        })
      }
    },
    saveQrcodeToAlbum() {
      const url = this.$fileUrl(this.product.qrcodeImage)
      uni.showLoading({ title: '保存中...' })
      // 先下载到本地临时文件
      uni.downloadFile({
        url,
        success: (res) => {
          if (res.statusCode === 200) {
            uni.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success: () => {
                uni.hideLoading()
                uni.showModal({
                  title: '已保存到相册',
                  content: '请打开微信「扫一扫」→ 右上角「相册」选择刚保存的二维码图片',
                  showCancel: false,
                  confirmText: '知道了'
                })
              },
              fail: (err) => {
                uni.hideLoading()
                if (err.errMsg && err.errMsg.includes('auth deny')) {
                  uni.showModal({
                    title: '需要相册权限',
                    content: '请在设置中允许保存图片到相册',
                    confirmText: '去设置',
                    success: (res) => {
                      if (res.confirm) uni.openSetting()
                    }
                  })
                } else {
                  uni.showToast({ title: '保存失败', icon: 'none' })
                }
              }
            })
          } else {
            uni.hideLoading()
            uni.showToast({ title: '下载图片失败', icon: 'none' })
          }
        },
        fail: () => {
          uni.hideLoading()
          uni.showToast({ title: '网络异常', icon: 'none' })
        }
      })
    },
    goCart() {
      uni.navigateTo({ url: `/pages/buyer/cart?enterpriseId=${this.enterpriseId}` })
    }
  }
}
</script>

<style lang="scss" scoped>
/* 加载/错误状态 */
.loading-wrap, .error-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 48rpx;
}
.loading-text {
  font-size: 28rpx;
  color: #999;
}
.error-emoji {
  font-size: 100rpx;
  margin-bottom: 24rpx;
}
.error-text {
  font-size: 28rpx;
  color: #999;
}

.stock-in {
  color: #18bc37;
  font-size: 26rpx;
}
.stock-out {
  color: #e43d33;
  font-size: 26rpx;
}

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
  align-items: center;
}
.btn-cart-nav {
  position: relative;
  width: 84rpx;
  height: 84rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6fa;
  border-radius: 42rpx;
  flex-shrink: 0;
  font-size: 40rpx;
}
.cart-nav-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}
.cart-nav-badge {
  position: absolute;
  top: 0rpx;
  right: 0rpx;
  min-width: 28rpx;
  height: 28rpx;
  line-height: 28rpx;
  padding: 0 6rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 18rpx;
  border-radius: 14rpx;
  text-align: center;
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

/* 二维码展示区 — TODO: 后续部署迁移 COS */
.qrcode-section {
  margin-top: 20rpx;
}
.qrcode-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 16rpx;
}
.qrcode-img {
  width: 320rpx;
  height: 320rpx;
  border-radius: 12rpx;
  border: 1rpx solid #eee;
}
.qrcode-hint {
  font-size: 22rpx;
  color: #999;
  margin-top: 12rpx;
}
.stock-low {
  color: #e67e22;
}

/* 商品描述区 */
.desc-section {
  margin-top: 20rpx;
  margin-bottom: 180rpx;
}
.desc-content {
  display: block;
  font-size: 26rpx;
  color: #555;
  line-height: 1.8;
  margin-top: 16rpx;
  white-space: pre-wrap;
}

/* 二维码保存按钮 — 简约线条 + 圆润饱满 */
.qrcode-save-btn {
  margin-top: 24rpx;
  padding: 18rpx 40rpx;
  border: 1rpx solid #ddd;
  border-radius: 40rpx;
  text-align: center;
  background: #fafafa;
}
.qrcode-save-btn--hover {
  background: #f0f0f0;
  border-color: #ccc;
}
.qrcode-save-text {
  font-size: 24rpx;
  color: #666;
  letter-spacing: 1rpx;
}
</style>
