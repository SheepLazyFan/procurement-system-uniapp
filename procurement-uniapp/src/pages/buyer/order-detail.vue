<template>
  <view class="page-buyer-order-detail container">
    <view class="card">
      <view class="flex-between">
        <text class="order-no">{{ order.orderNo }}</text>
        <StatusTag :text="getStatusText(order.status)" :type="getStatusType(order.status)" />
      </view>
      <view class="divider" />
      <view class="info-row" v-if="order.status !== 'CANCELLED'">
        <text class="info-label">支付状态</text>
        <text class="info-value" :class="'pay-' + order.paymentStatus">{{ paymentStatusText(order.paymentStatus) }}</text>
      </view>
      <view class="info-row" v-if="order.deliveryAddress">
        <text class="info-label">收货地址</text>
        <text class="info-value">{{ order.deliveryAddress }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">下单时间</text>
        <text class="info-value">{{ formatDateTime(order.createdAt) }}</text>
      </view>
      <view class="info-row" v-if="order.remark">
        <text class="info-label">备注</text>
        <text class="info-value">{{ order.remark }}</text>
      </view>
    </view>

    <!-- 商品明细 -->
    <view class="card">
      <text class="section-title">商品明细</text>
      <view v-for="item in order.items" :key="item.productId" class="item-row">
        <view class="item-info">
          <text class="item-name">{{ item.productName }}</text>
          <text class="item-spec">{{ item.spec }} / {{ item.unit }}</text>
        </view>
        <view class="item-right">
          <text class="item-qty">x{{ item.quantity }}</text>
          <text class="item-amount">¥{{ item.amount }}</text>
        </view>
      </view>
      <view class="divider" />
      <view class="total-row flex-between">
        <text class="total-label">合计</text>
        <text class="total-amount price-text">¥{{ order.totalAmount }}</text>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="action-buttons" v-if="order.status">
      <button v-if="order.paymentStatus === 'UNPAID' && order.status !== 'CANCELLED'" class="btn-primary" @tap="handlePay">去支付</button>
      <button v-if="order.status === 'PENDING'" class="btn-cancel" @tap="handleCancel">取消订单</button>
    </view>

    <!-- 付款弹窗 -->
    <view v-if="showPayPopup" class="pay-popup-mask" @tap.self="showPayPopup = false">
      <view class="pay-popup">
        <text class="pay-popup-title">扫码完成付款</text>
        <text class="pay-popup-amount">应付：¥{{ order.totalAmount }}</text>
        <view class="pay-qr-wrap">
          <image v-if="storeQrUrl" :src="$fileUrl(storeQrUrl)" class="pay-qr-img" mode="aspectFit" show-menu-by-longpress />
          <text v-else class="pay-qr-tip">商家暂未设置收款码，请联系商家</text>
          <view v-if="storeQrUrl" class="qrcode-save-btn" hover-class="qrcode-save-btn--hover" @tap="savePayQrToAlbum">
            <text class="qrcode-save-text">保存图片 · 用微信扫一扫</text>
          </view>
        </view>
        <text class="pay-tip">请用微信/支付宝扫描商家收款码，付款后点击"我已付款"</text>
        <button class="btn-claimed" @tap="handleClaimPaid">我已付款</button>
        <button class="btn-close-popup" @tap="showPayPopup = false">稍后再付</button>
      </view>
    </view>
  </view>
</template>

<script>
import { getBuyerOrderDetail, claimBuyerOrderPaid, cancelBuyerOrder, getStoreInfo } from '@/api/buyer'
import { getSalesStatusText, formatDateTime } from '@/utils/format'
import StatusTag from '@/components/common/StatusTag.vue'

export default {
  components: { StatusTag },
  data() {
    return {
      orderId: null,
      order: { items: [] },
      showPayPopup: false,
      storeQrUrl: ''
    }
  },
  onLoad(query) {
    this.orderId = Number(query.id)
    this.loadDetail()
  },
  methods: {
    async loadDetail() {
      try {
        this.order = await getBuyerOrderDetail(this.orderId)
      } catch (e) {
        uni.showToast({ title: '加载订单失败', icon: 'none' })
      }
    },
    formatDateTime,
    paymentStatusText(status) {
      const map = { UNPAID: '待付款', CLAIMED: '待商家确认', PAID: '已付款' }
      return map[status] || status
    },
    getStatusText(status) {
      if (status === 'CANCELLED' && this.order.cancelBy === 'MERCHANT') return '商家已取消'
      return getSalesStatusText(status)
    },
    getStatusType(status) {
      const map = { PENDING: 'warning', CONFIRMED: 'primary', SHIPPED: 'purple', COMPLETED: 'success', CANCELLED: 'danger' }
      return map[status] || 'info'
    },
    async handlePay() {
      this.storeQrUrl = ''
      this.showPayPopup = true
      if (this.order.enterpriseId) {
        try {
          const info = await getStoreInfo(this.order.enterpriseId)
          this.storeQrUrl = info?.paymentQrUrl || ''
        } catch (e) {
          console.error('加载收款码失败', e)
        }
      }
    },
    async handleClaimPaid() {
      try {
        await claimBuyerOrderPaid(this.orderId)
        this.showPayPopup = false
        uni.showToast({ title: '已提交，等待商家确认', icon: 'success' })
        this.loadDetail()
      } catch (e) {
        uni.showToast({ title: e.message || '提交失败', icon: 'none' })
      }
    },
    savePayQrToAlbum() {
      if (!this.storeQrUrl) return
      const url = this.$fileUrl(this.storeQrUrl)
      uni.showLoading({ title: '保存中...' })
      uni.downloadFile({
        url,
        success: (res) => {
          if (res.statusCode === 200) {
            uni.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success: () => {
                uni.hideLoading()
                uni.showModal({ title: '已保存到相册', content: '请打开微信「扫一扫」→ 右上角「相册」选择刚保存的收款码', showCancel: false, confirmText: '知道了' })
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
    handleCancel() {
      uni.showModal({
        title: '取消订单',
        content: '确定取消该订单吗？取消后库存将恢复。',
        success: async (res) => {
          if (res.confirm) {
            try {
              await cancelBuyerOrder(this.orderId)
              uni.showToast({ title: '已取消', icon: 'success' })
              this.loadDetail()
            } catch (e) {
              uni.showToast({ title: '取消失败', icon: 'none' })
            }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.order-no { font-size: 30rpx; font-weight: 600; color: #333; }
.section-title { font-size: 30rpx; font-weight: 600; margin-bottom: 20rpx; }
.info-row { display: flex; justify-content: space-between; padding: 10rpx 0; }
.info-label { font-size: 26rpx; color: #999; }
.info-value {
  font-size: 26rpx; color: #333;
  &.pay-UNPAID { color: #fa8c16; }
  &.pay-CLAIMED { color: #2979ff; }
  &.pay-PAID { color: #52c41a; }
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}
.item-name { font-size: 28rpx; color: #333; }
.item-spec { font-size: 24rpx; color: #999; }
.item-right { text-align: right; }
.item-qty { font-size: 26rpx; color: #666; display: block; }
.item-amount { font-size: 26rpx; color: #333; font-weight: 500; }

.total-row { padding: 8rpx 0; }
.total-label { font-size: 28rpx; color: #333; font-weight: 600; }
.total-amount { font-size: 36rpx; }

.action-buttons { padding: 32rpx 0; }
.btn-cancel {
  margin-top: 16rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  background: #f5f6fa;
  border-radius: 12rpx;
  color: #e43d33;
  font-size: 28rpx;
  border: none;
}

/* 付款弹窗 */
.pay-popup-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}
.pay-popup {
  width: 100%;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 40rpx 32rpx 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
}
.pay-popup-title { font-size: 34rpx; font-weight: 600; color: #333; }
.pay-popup-amount { font-size: 40rpx; font-weight: 700; color: #ff4d4f; }
.pay-qr-wrap {
  width: 320rpx;
  height: 320rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid #eee;
  border-radius: 12rpx;
}
.pay-qr-img { width: 300rpx; height: 300rpx; }
.pay-qr-tip { font-size: 24rpx; color: #999; text-align: center; padding: 20rpx; }
.pay-tip { font-size: 24rpx; color: #999; text-align: center; line-height: 1.6; }
.btn-claimed {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #2979ff;
  color: #fff;
  font-size: 32rpx;
  border-radius: 44rpx;
  text-align: center;
  border: none;
}
.btn-close-popup {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background: #f5f5f5;
  color: #666;
  font-size: 28rpx;
  border-radius: 40rpx;
  text-align: center;
  border: none;
}
.qrcode-save-btn {
  margin-top: 20rpx;
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
