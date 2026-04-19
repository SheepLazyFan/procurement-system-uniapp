<template>
  <view class="page-product-detail">
    <!-- 加载中 -->
    <view v-if="loading" class="state-wrap">
      <view class="state-spinner"></view>
      <text class="state-text">加载中...</text>
    </view>

    <!-- 加载失败 -->
    <view v-else-if="loadError" class="state-wrap">
      <view class="svg-icon-warn"></view>
      <text class="state-text">加载失败，请返回重试</text>
    </view>

    <!-- 正常内容 -->
    <block v-else>
    <!-- 商品图片 -->
    <view class="product-hero">
      <image v-if="product.mainImage" :src="$fileUrl(product.mainImage)" class="product-hero__img" mode="aspectFill" />
      <view v-else class="product-hero__placeholder">
        <view class="svg-icon-package-lg"></view>
      </view>
    </view>

    <!-- 价格 + 基本信息 -->
    <view class="product-info">
      <view class="product-info__price-bar">
        <text class="price-symbol">¥</text>
        <text class="price-integer">{{ priceInteger }}</text>
        <text class="price-decimal" v-if="priceDecimal">.{{ priceDecimal }}</text>
        <text class="price-unit">/{{ product.unit || '件' }}</text>
        <view v-if="product.stockStatus === 'OUT_OF_STOCK'" class="stock-tag stock-tag--out">缺货</view>
        <view v-else-if="product.stockStatus === 'LOW_STOCK'" class="stock-tag stock-tag--low">仅剩{{ product.stock }}件</view>
        <view v-else class="stock-tag stock-tag--in">有货</view>
      </view>
      <text class="product-info__name">{{ product.name }}</text>
      <text class="product-info__spec" v-if="product.spec">规格：{{ product.spec }}</text>
    </view>

    <!-- 数量选择 -->
    <view class="section-card">
      <text class="section-card__label">购买数量</text>
      <view class="qty-stepper">
        <text class="qty-stepper__btn" :class="{ 'qty-stepper__btn--disabled': quantity <= 1 }" @tap="changeQty(-1)">－</text>
        <input class="qty-stepper__input" type="number" v-model="quantity" @blur="checkQty" />
        <text class="qty-stepper__btn" @tap="changeQty(1)">＋</text>
      </view>
    </view>

    <view v-if="product.qrcodeImage" class="section-card section-card--last">
      <text class="section-card__label">扫码查看演示视频</text>
      <view class="qrcode-area">
        <image :src="$fileUrl(product.qrcodeImage)" class="qrcode-area__img" mode="aspectFit" show-menu-by-longpress @tap="previewQrcode" />
        <text class="qrcode-area__hint">点击放大 · 长按识别二维码</text>
        <view class="qrcode-area__save" @tap="saveQrcodeToAlbum">
          <view class="svg-icon-download"></view>
          <text class="qrcode-area__save-text">保存到相册</text>
        </view>
      </view>
    </view>

    <!-- 商品描述 -->
    <view v-if="product.description" class="section-card section-card--last">
      <text class="section-card__label">商品介绍</text>
      <text class="desc-body">{{ product.description }}</text>
    </view>
    </block>

    <!-- 底部操作栏 -->
    <view class="bottom-bar safe-area-bottom">
      <view class="bottom-bar__cart-icon" hover-class="bottom-bar__cart-icon--hover" @tap="goCart">
        <view class="svg-icon-cart-bottom"></view>
        <text v-if="cartInStore > 0" class="cart-icon-badge">{{ cartInStore > 99 ? '99+' : cartInStore }}</text>
      </view>
      <button class="bottom-bar__btn-cart" :disabled="!product.id || product.stockStatus === 'OUT_OF_STOCK'" hover-class="bottom-bar__btn--hover" @tap="handleAddCart">
        {{ product.stockStatus === 'OUT_OF_STOCK' ? '已售罄' : '加入购物车' }}
      </button>
      <button class="bottom-bar__btn-buy" :disabled="!product.id || product.stockStatus === 'OUT_OF_STOCK'" hover-class="bottom-bar__btn--hover" @tap="handleBuyNow">
        {{ product.stockStatus === 'OUT_OF_STOCK' ? '缺货中' : '立即购买' }}
      </button>
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
    },
    priceInteger() {
      const p = String(this.product.price || '0')
      return p.split('.')[0]
    },
    priceDecimal() {
      const p = String(this.product.price || '0')
      const parts = p.split('.')
      return parts.length > 1 ? parts[1] : ''
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
        uni.previewImage({ urls: [url], current: url })
      }
    },
    saveQrcodeToAlbum() {
      const url = this.$fileUrl(this.product.qrcodeImage)
      uni.showLoading({ title: '保存中...' })
      uni.downloadFile({
        url,
        success: (res) => {
          if (res.statusCode === 200) {
            uni.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success: () => {
                uni.hideLoading()
                uni.showModal({ title: '已保存到相册', content: '请打开微信「扫一扫」→ 右上角「相册」选择刚保存的二维码图片', showCancel: false, confirmText: '知道了' })
              },
              fail: (err) => {
                uni.hideLoading()
                if (err.errMsg && err.errMsg.includes('auth deny')) {
                  uni.showModal({ title: '需要相册权限', content: '请在设置中允许保存图片到相册', confirmText: '去设置', success: (r) => { if (r.confirm) uni.openSetting() } })
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
.page-product-detail {
  background: #f7f8fa;
  min-height: 100vh;
  padding-bottom: 140rpx;
}

/* 状态页 */
.state-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 240rpx 48rpx;
}
.state-spinner {
  width: 48rpx; height: 48rpx;
  border: 4rpx solid #e8e8e8;
  border-top-color: #2979ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.state-text { font-size: 28rpx; color: #999; margin-top: 20rpx; }

/* SVG 图标 */
.svg-icon-warn {
  width: 80rpx; height: 80rpx; margin-bottom: 16rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23e8b339' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='12' cy='12' r='10'%3E%3C/circle%3E%3Cline x1='12' y1='8' x2='12' y2='12'%3E%3C/line%3E%3Cline x1='12' y1='16' x2='12.01' y2='16'%3E%3C/line%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}
.svg-icon-package-lg {
  width: 80rpx; height: 80rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ccc' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cline x1='16.5' y1='9.4' x2='7.5' y2='4.21'%3E%3C/line%3E%3Cpath d='M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z'%3E%3C/path%3E%3Cpolyline points='3.27 6.96 12 12.01 20.73 6.96'%3E%3C/polyline%3E%3Cline x1='12' y1='22.08' x2='12' y2='12'%3E%3C/line%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}
.svg-icon-cart-bottom {
  width: 40rpx; height: 40rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23555' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='9' cy='21' r='1'%3E%3C/circle%3E%3Ccircle cx='20' cy='21' r='1'%3E%3C/circle%3E%3Cpath d='M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6'%3E%3C/path%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}

/* 商品大图 */
.product-hero {
  width: 100%;
  height: 600rpx;
}
.product-hero__img {
  width: 100%;
  height: 100%;
}
.product-hero__placeholder {
  width: 100%;
  height: 100%;
  background: #f0f1f3;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 价格 + 信息 */
.product-info {
  background: #fff;
  padding: 28rpx 28rpx 24rpx;
  margin-bottom: 12rpx;
}
.product-info__price-bar {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
}
.price-symbol {
  font-size: 28rpx;
  color: #ff4d4f;
  font-weight: 700;
}
.price-integer {
  font-size: 48rpx;
  color: #ff4d4f;
  font-weight: 700;
  letter-spacing: -2rpx;
  line-height: 1;
}
.price-decimal {
  font-size: 28rpx;
  color: #ff4d4f;
  font-weight: 700;
}
.price-unit {
  font-size: 24rpx;
  color: #aaa;
  margin-left: 4rpx;
}
.stock-tag {
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;
  margin-left: auto;
  font-weight: 500;
}
.stock-tag--in { color: #52c41a; background: #f0faf2; }
.stock-tag--out { color: #ff4d4f; background: #fff1f0; }
.stock-tag--low { color: #e67e22; background: #fff8f0; }
.product-info__name {
  display: block;
  font-size: 32rpx;
  color: #1a1a1a;
  font-weight: 600;
  margin-top: 16rpx;
  line-height: 1.5;
}
.product-info__spec {
  display: block;
  font-size: 24rpx;
  color: #aaa;
  margin-top: 8rpx;
}

/* 通用 section 卡片 */
.section-card {
  background: #fff;
  padding: 28rpx;
  margin-bottom: 12rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.section-card--last {
  flex-direction: column;
  align-items: stretch;
  margin-bottom: 0;
}
.section-card__label {
  font-size: 28rpx;
  color: #1a1a1a;
  font-weight: 500;
}

/* 数量步进器 */
.qty-stepper {
  display: flex;
  align-items: center;
  background: #f5f6fa;
  border-radius: 12rpx;
  overflow: hidden;
}
.qty-stepper__btn {
  width: 64rpx;
  height: 56rpx;
  line-height: 56rpx;
  text-align: center;
  font-size: 30rpx;
  color: #333;
  &--disabled { color: #ccc; }
}
.qty-stepper__input {
  width: 80rpx;
  height: 56rpx;
  text-align: center;
  font-size: 28rpx;
  color: #1a1a1a;
  font-weight: 600;
  background: #fff;
  border: none;
}

/* 二维码区域 */
.qrcode-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 16rpx;
  width: 100%;
}
.qrcode-area__img {
  width: 320rpx;
  height: 320rpx;
  border-radius: 16rpx;
  border: 1rpx solid #f0f0f0;
}
.qrcode-area__hint {
  font-size: 22rpx;
  color: #bbb;
  margin-top: 12rpx;
}
.qrcode-area__save {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 16rpx;
}
.svg-icon-download {
  width: 28rpx; height: 28rpx;
  margin-right: 6rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%232979ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'%3E%3C/path%3E%3Cpolyline points='7 10 12 15 17 10'%3E%3C/polyline%3E%3Cline x1='12' y1='15' x2='12' y2='3'%3E%3C/line%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}
.qrcode-area__save-text {
  font-size: 24rpx;
  color: #2979ff;
}

/* 商品描述 */
.desc-body {
  display: block;
  font-size: 26rpx;
  color: #555;
  line-height: 1.8;
  margin-top: 16rpx;
  white-space: pre-wrap;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 12rpx 20rpx;
  background: #fff;
  border-top: 1rpx solid #f0f0f0;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.04);
}
.bottom-bar__cart-icon {
  position: relative;
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6fa;
  border-radius: 40rpx;
  flex-shrink: 0;
  margin-right: 16rpx;
}
.bottom-bar__cart-icon--hover { background: #ebedf0; }
.cart-icon-badge {
  position: absolute;
  top: 4rpx;
  right: 2rpx;
  min-width: 28rpx;
  height: 28rpx;
  line-height: 28rpx;
  padding: 0 6rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 18rpx;
  border-radius: 14rpx;
  text-align: center;
  font-weight: 600;
}
.bottom-bar__btn-cart,
.bottom-bar__btn-buy {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 40rpx;
  font-size: 28rpx;
  font-weight: 600;
  border: none;
  margin-right: 12rpx;
}
.bottom-bar__btn-buy { margin-right: 0; }
.bottom-bar__btn-cart {
  background: #ff9900;
  color: #fff;
}
.bottom-bar__btn-buy {
  background: #ff4d4f;
  color: #fff;
}
.bottom-bar__btn--hover { opacity: 0.85; }
.bottom-bar__btn-cart[disabled],
.bottom-bar__btn-buy[disabled] {
  background: #e8e8e8;
  color: #bbb;
}
</style>
