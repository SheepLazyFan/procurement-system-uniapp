<template>
  <!-- 付款弹窗 — 底部弹出 Action Sheet -->
  <view v-if="show" class="pay-popup-mask" @tap.self="$emit('close')">
    <view class="pay-popup">
      <!-- 标题 + 金额 -->
      <text class="pay-popup__title">扫码完成付款</text>
      <text class="pay-popup__amount">应付：¥{{ amount }}</text>

      <!-- QR 码区域 -->
      <view class="pay-popup__qr-section">
        <!-- 骨架屏：加载中 -->
        <view v-if="loading" class="qr-skeleton">
          <view class="qr-skeleton__img"></view>
          <view class="qr-skeleton__line"></view>
        </view>
        <!-- 加载成功 -->
        <template v-else-if="qrUrl">
          <view class="qr-frame">
            <image
              :src="$fileUrl(qrUrl)"
              class="qr-image"
              mode="aspectFit"
              show-menu-by-longpress
              @error="onQrLoadError"
            />
          </view>
          <!-- 辅助操作：保存 + 长按提示 -->
          <view class="qr-actions">
            <text class="qr-save-link" @tap="saveToAlbum">📥 保存到相册</text>
            <text class="qr-longpress-tip">长按图片可直接识别付款</text>
          </view>
        </template>
        <!-- 加载失败 / 未设置 -->
        <view v-else class="qr-empty">
          <text class="qr-empty__icon">🖼️</text>
          <text class="qr-empty__text">{{ errorMsg || '商家暂未设置收款码' }}</text>
          <text v-if="errorMsg" class="qr-empty__retry" @tap="loadQr">点击重试</text>
          <text v-else class="qr-empty__hint">请联系商家获取收款方式</text>
        </view>
      </view>

      <!-- 底部提示 -->
      <text class="pay-popup__tip">请用微信/支付宝扫描商家收款码，付款后点击"我已付款"</text>

      <!-- 按钮组 -->
      <button class="pay-popup__btn-primary" hover-class="pay-popup__btn-primary--hover" @tap="handleClaimPaid">我已付款</button>
      <button class="pay-popup__btn-secondary" hover-class="pay-popup__btn-secondary--hover" @tap="$emit('close')">稍后再付</button>
    </view>
  </view>
</template>

<script>
import { getStoreInfo, claimBuyerOrderPaid } from '@/api/buyer'

export default {
  name: 'PayQrPopup',
  props: {
    /** 是否显示弹窗 */
    show: { type: Boolean, default: false },
    /** 应付金额 */
    amount: { type: [Number, String], default: 0 },
    /** 企业 ID（用于加载收款码） */
    enterpriseId: { type: [Number, String], default: null },
    /** 订单 ID（用于标记已付款） */
    orderId: { type: [Number, String], default: null }
  },
  emits: ['close', 'paid'],
  data() {
    return {
      qrUrl: '',
      loading: false,
      errorMsg: ''
    }
  },
  watch: {
    show(val) {
      if (val && this.enterpriseId) {
        this.loadQr()
      }
      if (!val) {
        // 弹窗关闭时重置状态
        this.qrUrl = ''
        this.errorMsg = ''
      }
    }
  },
  methods: {
    async loadQr() {
      if (!this.enterpriseId) return
      this.loading = true
      this.errorMsg = ''
      try {
        const info = await getStoreInfo(this.enterpriseId)
        this.qrUrl = info?.paymentQrUrl || ''
        if (!this.qrUrl) {
          this.errorMsg = ''  // 商家未设置，走 qr-empty 默认提示
        }
      } catch (e) {
        console.error('[PayQrPopup] 加载收款码失败', e)
        this.errorMsg = '加载收款码失败'
      } finally {
        this.loading = false
      }
    },

    onQrLoadError() {
      this.qrUrl = ''
      this.errorMsg = '收款码图片加载失败'
    },

    async handleClaimPaid() {
      if (!this.orderId) return
      try {
        await claimBuyerOrderPaid(this.orderId)
        this.$emit('paid')
        uni.showToast({ title: '已提交，等待商家确认', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e.message || '提交失败', icon: 'none' })
      }
    },

    saveToAlbum() {
      if (!this.qrUrl) return
      const url = this.$fileUrl(this.qrUrl)
      uni.showLoading({ title: '保存中...' })
      uni.downloadFile({
        url,
        timeout: 10000,
        success: (res) => {
          if (res.statusCode === 200) {
            uni.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success: () => {
                uni.hideLoading()
                uni.showModal({
                  title: '已保存到相册',
                  content: '请打开微信「扫一扫」→ 右上角「相册」选择刚保存的收款码',
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
                    success: (r) => { if (r.confirm) uni.openSetting() }
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
          uni.showToast({ title: '网络异常，请重试', icon: 'none' })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
/* ==========================================
   付款弹窗 — 底部 Action Sheet 风格
   ========================================== */
.pay-popup-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}

.pay-popup {
  width: 100%;
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  padding: 48rpx 40rpx calc(env(safe-area-inset-bottom, 0px) + 40rpx);
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 标题 + 金额 */
.pay-popup__title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 2rpx;
}
.pay-popup__amount {
  margin-top: 12rpx;
  font-size: 44rpx;
  font-weight: 800;
  color: #ff4d4f;
  letter-spacing: 1rpx;
}

/* QR 码区域 */
.pay-popup__qr-section {
  margin-top: 36rpx;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qr-frame {
  width: 480rpx;
  height: 480rpx;
  border: 2rpx solid #f0f0f0;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  overflow: hidden;
}

.qr-image {
  width: 460rpx;
  height: 460rpx;
}

/* 辅助操作 */
.qr-actions {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.qr-save-link {
  font-size: 26rpx;
  color: #2979ff;
  font-weight: 500;
}

.qr-longpress-tip {
  font-size: 22rpx;
  color: #c0c0c0;
}

/* 骨架屏 */
.qr-skeleton {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
}
.qr-skeleton__img {
  width: 480rpx;
  height: 480rpx;
  border-radius: 20rpx;
  background: linear-gradient(90deg, #f2f2f2 25%, #e8e8e8 50%, #f2f2f2 75%);
  background-size: 200% 100%;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}
.qr-skeleton__line {
  width: 200rpx;
  height: 28rpx;
  border-radius: 14rpx;
  background: linear-gradient(90deg, #f2f2f2 25%, #e8e8e8 50%, #f2f2f2 75%);
  background-size: 200% 100%;
  animation: skeleton-pulse 1.5s ease-in-out 0.2s infinite;
}
@keyframes skeleton-pulse {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 空态 / 失败 */
.qr-empty {
  width: 480rpx;
  height: 480rpx;
  border: 2rpx dashed #e0e0e0;
  border-radius: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  background: #fafafa;
}
.qr-empty__icon { font-size: 64rpx; opacity: 0.5; }
.qr-empty__text { font-size: 26rpx; color: #999; text-align: center; padding: 0 32rpx; }
.qr-empty__retry { font-size: 26rpx; color: #2979ff; font-weight: 500; }
.qr-empty__hint { font-size: 22rpx; color: #c0c0c0; }

/* 底部提示 */
.pay-popup__tip {
  margin-top: 28rpx;
  font-size: 24rpx;
  color: #b0b0b0;
  text-align: center;
  line-height: 1.6;
}

/* 按钮 */
.pay-popup__btn-primary {
  margin-top: 32rpx;
  width: 100%;
  height: 92rpx;
  line-height: 92rpx;
  background: linear-gradient(135deg, #2979ff, #448aff);
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 46rpx;
  text-align: center;
  border: none;
  box-shadow: 0 8rpx 24rpx rgba(41, 121, 255, 0.25);
  letter-spacing: 2rpx;
}
.pay-popup__btn-primary--hover {
  opacity: 0.85;
  transform: scale(0.98);
}

.pay-popup__btn-secondary {
  margin-top: 16rpx;
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: transparent;
  color: #999;
  font-size: 28rpx;
  border-radius: 40rpx;
  text-align: center;
  border: none;
}
.pay-popup__btn-secondary--hover {
  background: #f5f5f5;
}
</style>
