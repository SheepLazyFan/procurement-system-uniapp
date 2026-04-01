<template>
  <view class="page-buyer-orders container">
    <NavBar title="我的订单" />

    <!-- 状态筛选 -->
    <scroll-view scroll-x class="status-tabs">
      <view v-for="tab in tabs" :key="tab.value" class="tab-item" :class="{ active: currentTab === tab.value }" @tap="switchTab(tab.value)">
        <text>{{ tab.label }}</text>
      </view>
    </scroll-view>

    <!-- 订单列表 -->
    <view v-for="order in orderList" :key="order.id" class="card order-card" @tap="goOrderDetail(order.id)">
      <view class="order-header">
        <text class="order-no">{{ order.orderNo }}</text>
        <text class="order-status" :class="'status-' + order.status">{{ statusText(order.status, order.cancelBy) }}</text>
      </view>
      <view v-for="item in (order.items || [])" :key="item.id" class="order-item">
        <text class="item-name">{{ item.productName }}</text>
        <text class="item-qty">×{{ item.quantity }}</text>
        <text class="item-amount">￥{{ item.amount }}</text>
      </view>
      <view class="order-footer">
        <text class="order-total">合计：<text class="total-price">￥{{ order.totalAmount }}</text></text>
        <view class="order-actions">
          <text v-if="order.status !== 'CANCELLED'" class="order-pay-status" :class="'pay-' + order.paymentStatus">{{ paymentStatusText(order.paymentStatus) }}</text>
          <text class="order-time">{{ formatDate(order.createdAt) }}</text>
          <button v-if="order.status === 'PENDING'" class="btn-cancel" @tap.stop="handleCancel(order.id)">取消</button>
          <button v-if="order.paymentStatus === 'UNPAID' && order.status !== 'CANCELLED'" class="btn-pay" @tap.stop="handlePay(order)">去支付</button>
        </view>
      </view>
    </view>

    <EmptyState v-if="!orderList.length && !loading" text="暂无订单" icon="📋" />
    <LoadMore v-if="orderList.length" :status="loadStatus" @loadMore="loadMore" />

    <!-- 付款弹窗 -->
    <view v-if="showPayPopup" class="pay-popup-mask" @tap.self="showPayPopup = false">
      <view class="pay-popup">
        <text class="pay-popup-title">扫码完成付款</text>
        <text class="pay-popup-amount">应付：¥{{ payingOrder && payingOrder.totalAmount }}</text>
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
import NavBar from '@/components/common/NavBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'
import { getBuyerOrders, claimBuyerOrderPaid, cancelBuyerOrder, getStoreInfo } from '@/api/buyer'
import { formatDate } from '@/utils/format'

export default {
  components: { NavBar, EmptyState, LoadMore },
  data() {
    return {
      tabs: [
        { label: '全部', value: '' },
        { label: '待确认', value: 'PENDING' },
        { label: '已确认', value: 'CONFIRMED' },
        { label: '已发货', value: 'SHIPPED' },
        { label: '已完成', value: 'COMPLETED' }
      ],
      currentTab: '',
      orderList: [],
      page: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      loadStatus: 'more',
      // 付款弹窗
      showPayPopup: false,
      payingOrder: null,
      storeQrUrl: ''
    }
  },
  onShow() {
    this.page = 1
    this.loadOrders()
  },
  onPullDownRefresh() {
    this.page = 1
    this.loadOrders().finally(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    if (this.loadStatus === 'more') this.loadMore()
  },
  methods: {
    formatDate,
    statusText(status, cancelBy) {
      if (status === 'CANCELLED') return cancelBy === 'MERCHANT' ? '商家已取消' : '已取消'
      const map = { PENDING: '待确认', CONFIRMED: '已确认', SHIPPED: '已发货', COMPLETED: '已完成' }
      return map[status] || status
    },
    paymentStatusText(status) {
      const map = { UNPAID: '待付款', CLAIMED: '待商家确认', PAID: '已付款' }
      return map[status] || ''
    },
    switchTab(val) {
      this.currentTab = val
      this.page = 1
      this.loadOrders()
    },
    async loadOrders() {
      this.loading = true
      try {
        const params = { pageNum: this.page, pageSize: this.pageSize }
        if (this.currentTab) params.status = this.currentTab
        const res = await getBuyerOrders(params)
        const list = res?.records || res || []
        this.total = res?.total || list.length
        if (this.page === 1) {
          this.orderList = list
        } else {
          this.orderList = [...this.orderList, ...list]
        }
        this.loadStatus = this.orderList.length >= this.total ? 'noMore' : 'more'
      } catch (e) {
        console.error('加载订单失败', e)
      } finally {
        this.loading = false
      }
    },
    loadMore() {
      this.page++
      this.loadOrders()
    },
    goOrderDetail(id) {
      uni.navigateTo({ url: `/pages/buyer/order-detail?id=${id}` })
    },
    async handlePay(order) {
      this.payingOrder = order
      this.storeQrUrl = ''
      this.showPayPopup = true
      // 异步加载收款码，不阻塞弹窗显示
      if (order.enterpriseId) {
        try {
          const info = await getStoreInfo(order.enterpriseId)
          this.storeQrUrl = info?.paymentQrUrl || ''
        } catch (e) {
          console.error('加载收款码失败', e)
        }
      }
    },
    async handleClaimPaid() {
      if (!this.payingOrder) return
      try {
        await claimBuyerOrderPaid(this.payingOrder.id)
        this.showPayPopup = false
        uni.showToast({ title: '已提交，等待商家确认', icon: 'success' })
        this.page = 1
        this.loadOrders()
      } catch (e) {
        uni.showToast({ title: e.message || '提交失败', icon: 'none' })
      }
    },
    handleCancel(orderId) {
      uni.showModal({
        title: '取消订单',
        content: '确定取消该订单吗？取消后库存将恢复。',
        success: async (res) => {
          if (res.confirm) {
            try {
              await cancelBuyerOrder(orderId)
              uni.showToast({ title: '已取消', icon: 'success' })
              this.page = 1
              this.loadOrders()
            } catch (e) {
              uni.showToast({ title: '取消失败', icon: 'none' })
            }
          }
        }
      })
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
    }
  }
}
</script>

<style lang="scss" scoped>
.status-tabs {
  white-space: nowrap;
  padding: 16rpx 0;
}
.tab-item {
  display: inline-block;
  padding: 12rpx 28rpx;
  font-size: 26rpx;
  color: #666;
  border-radius: 32rpx;
  margin-right: 12rpx;
  background: #f5f5f5;
  &.active {
    background: #2979ff;
    color: #fff;
  }
}
.order-card {
  margin-bottom: 16rpx;
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.order-no {
  font-size: 24rpx;
  color: #999;
}
.order-status {
  font-size: 24rpx;
  font-weight: 500;
  &.status-PENDING { color: #ff9900; }
  &.status-CONFIRMED { color: #2979ff; }
  &.status-SHIPPED { color: #18bc37; }
  &.status-COMPLETED { color: #999; }
  &.status-CANCELLED { color: #ccc; }
}
.order-item {
  display: flex;
  align-items: center;
  padding: 8rpx 0;
}
.item-name {
  flex: 1;
  font-size: 26rpx;
  color: #333;
}
.item-qty {
  font-size: 24rpx;
  color: #999;
  margin-right: 20rpx;
}
.item-amount {
  font-size: 26rpx;
  color: #333;
}
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f5f5f5;
}
.order-total {
  font-size: 26rpx;
  color: #666;
}
.total-price {
  color: #ff4d4f;
  font-weight: 600;
}
.order-actions {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.order-pay-status {
  font-size: 22rpx;
  &.pay-UNPAID { color: #fa8c16; }
  &.pay-CLAIMED { color: #2979ff; }
  &.pay-PAID { color: #52c41a; }
}
.order-time {
  font-size: 22rpx;
  color: #ccc;
}
.btn-pay {
  display: inline-block;
  padding: 8rpx 24rpx;
  font-size: 24rpx;
  color: #fff;
  background: #ff4d4f;
  border-radius: 32rpx;
  line-height: 1.5;
}
.btn-cancel {
  display: inline-block;
  padding: 8rpx 24rpx;
  font-size: 24rpx;
  color: #e43d33;
  background: #f5f6fa;
  border-radius: 32rpx;
  line-height: 1.5;
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
.pay-popup-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #333;
}
.pay-popup-amount {
  font-size: 40rpx;
  font-weight: 700;
  color: #ff4d4f;
}
.pay-qr-wrap {
  width: 320rpx;
  height: 320rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid #eee;
  border-radius: 12rpx;
}
.pay-qr-img {
  width: 300rpx;
  height: 300rpx;
}
.pay-qr-tip {
  font-size: 24rpx;
  color: #999;
  text-align: center;
  padding: 20rpx;
}
.pay-tip {
  font-size: 24rpx;
  color: #999;
  text-align: center;
  line-height: 1.6;
}
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
