<template>
  <view class="page-buyer-orders">
    <NavBar title="我的订单" />

    <view class="container">
      <!-- 状态筛选 -->
      <scroll-view scroll-x class="tab-bar" :show-scrollbar="false">
        <view class="tab-bar__inner">
          <view v-for="tab in tabs" :key="tab.value" class="tab-item" :class="{ 'tab-item--active': currentTab === tab.value }" @tap="switchTab(tab.value)">
            <text>{{ tab.label }}</text>
          </view>
        </view>
      </scroll-view>

      <!-- 订单列表 -->
      <view v-for="order in orderList" :key="order.id" class="order-card" hover-class="order-card--hover" @tap="goOrderDetail(order.id)">
        <view class="order-card__header">
          <text class="order-card__no">{{ order.orderNo }}</text>
          <text class="order-card__status" :class="'status--' + order.status">{{ statusText(order.status, order.cancelBy) }}</text>
        </view>
        <view v-for="item in (order.items || [])" :key="item.id" class="order-card__item">
          <text class="order-card__item-name">{{ item.productName }}</text>
          <text class="order-card__item-qty">×{{ item.quantity }}</text>
          <text class="order-card__item-amount">￥{{ item.amount }}</text>
        </view>
        <view class="order-card__footer">
          <view class="order-card__summary">
            <text class="order-card__total">合计：<text class="order-card__total-price">￥{{ order.totalAmount }}</text></text>
            <view class="order-card__meta">
              <text v-if="order.status !== 'CANCELLED'" class="order-card__pay-status" :class="'pay--' + order.paymentStatus">{{ paymentStatusText(order.paymentStatus) }}</text>
              <text class="order-card__time">{{ formatDate(order.createdAt) }}</text>
            </view>
          </view>
          <view class="order-card__btns">
            <button v-if="order.status === 'PENDING' && order.paymentStatus === 'UNPAID'" class="btn-outline-red" @tap.stop="handleCancel(order.id)">取消</button>
            <button v-if="order.paymentStatus === 'UNPAID' && order.status !== 'CANCELLED' && order.status !== 'COMPLETED'" class="btn-solid-red" @tap.stop="handlePay(order)">去支付</button>
          </view>
        </view>
      </view>

      <EmptyState v-if="!orderList.length && !loading" text="暂无订单" icon="📋" />
      <LoadMore v-if="orderList.length" :status="loadStatus" @loadMore="loadMore" />
    </view>

    <!-- 付款弹窗 -->
    <PayQrPopup
      :show="showPayPopup"
      :amount="payingOrder && payingOrder.totalAmount"
      :enterpriseId="payingOrder && payingOrder.enterpriseId"
      :orderId="payingOrder && payingOrder.id"
      @close="showPayPopup = false"
      @paid="onPaySuccess"
    />
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'
import PayQrPopup from '@/components/buyer/PayQrPopup.vue'
import { getBuyerOrders, cancelBuyerOrder } from '@/api/buyer'
import { formatDate } from '@/utils/format'

export default {
  components: { NavBar, EmptyState, LoadMore, PayQrPopup },
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
      showPayPopup: false,
      payingOrder: null
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
      if (status === 'CANCELLED') {
        if (cancelBy === 'MERCHANT' || cancelBy === 'SALES') return '商家已取消'
        if (cancelBy === 'SYSTEM') return '已超时'
        if (cancelBy === 'BUYER') return '已取消'
      }
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
    handlePay(order) {
      this.payingOrder = order
      this.showPayPopup = true
    },
    onPaySuccess() {
      this.showPayPopup = false
      this.page = 1
      this.loadOrders()
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
    }
  }
}
</script>

<style lang="scss" scoped>
.page-buyer-orders {
  background: #f7f8fa;
  min-height: 100vh;
}
.tab-bar {
  white-space: nowrap;
  margin-bottom: 12rpx;
}
.tab-bar__inner {
  display: inline-flex;
  padding: 4rpx 0;
}
.tab-item {
  display: inline-block;
  padding: 12rpx 28rpx;
  font-size: 26rpx;
  color: #888;
  border-radius: 32rpx;
  margin-right: 12rpx;
  background: #fff;
  font-weight: 500;
  &--active {
    background: #2979ff;
    color: #fff;
    font-weight: 600;
  }
}
.order-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}
.order-card--hover { background: #fafafa; }
.order-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.order-card__no { font-size: 22rpx; color: #bbb; }
.order-card__status {
  font-size: 24rpx;
  font-weight: 600;
  &.status--PENDING { color: #ff9900; }
  &.status--CONFIRMED { color: #2979ff; }
  &.status--SHIPPED { color: #18bc37; }
  &.status--COMPLETED { color: #999; }
  &.status--CANCELLED { color: #ccc; }
}
.order-card__item {
  display: flex;
  align-items: center;
  padding: 6rpx 0;
}
.order-card__item-name { flex: 1; font-size: 26rpx; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.order-card__item-qty { font-size: 24rpx; color: #aaa; margin: 0 16rpx; }
.order-card__item-amount { font-size: 26rpx; color: #333; font-weight: 500; }
.order-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f5f5f5;
}
.order-card__summary { flex: 1; }
.order-card__total { font-size: 24rpx; color: #666; }
.order-card__total-price { color: #ff4d4f; font-weight: 700; font-size: 28rpx; }
.order-card__meta {
  display: flex;
  align-items: center;
  margin-top: 8rpx;
}
.order-card__pay-status {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
  margin-right: 12rpx;
  &.pay--UNPAID { color: #fa8c16; background: #fff8f0; }
  &.pay--CLAIMED { color: #2979ff; background: #eef4ff; }
  &.pay--PAID { color: #52c41a; background: #f0faf2; }
}
.order-card__time { font-size: 20rpx; color: #ccc; }
.order-card__btns {
  display: flex;
  gap: 12rpx;
  flex-shrink: 0;
}
.btn-outline-red {
  padding: 0 24rpx;
  height: 56rpx;
  line-height: 56rpx;
  font-size: 24rpx;
  color: #e43d33;
  background: #fff;
  border: 1rpx solid #fde2e0;
  border-radius: 28rpx;
}
.btn-solid-red {
  padding: 0 28rpx;
  height: 56rpx;
  line-height: 56rpx;
  font-size: 24rpx;
  color: #fff;
  background: #ff4d4f;
  border-radius: 28rpx;
  border: none;
  font-weight: 600;
}
</style>
