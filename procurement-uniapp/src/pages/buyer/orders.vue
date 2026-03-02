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
    <view v-for="order in orderList" :key="order.id" class="card order-card">
      <view class="order-header">
        <text class="order-no">{{ order.orderNo }}</text>
        <text class="order-status" :class="'status-' + order.status">{{ statusText(order.status) }}</text>
      </view>
      <view v-for="item in (order.items || [])" :key="item.id" class="order-item">
        <text class="item-name">{{ item.productName }}</text>
        <text class="item-qty">×{{ item.quantity }}</text>
        <text class="item-amount">¥{{ item.amount }}</text>
      </view>
      <view class="order-footer">
        <text class="order-total">合计：<text class="total-price">¥{{ order.totalAmount }}</text></text>
        <text class="order-time">{{ formatDate(order.createdAt) }}</text>
      </view>
    </view>

    <EmptyState v-if="!orderList.length && !loading" text="暂无订单" icon="📋" />
    <LoadMore v-if="orderList.length" :status="loadStatus" @loadMore="loadMore" />
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'
import { getBuyerOrders } from '@/api/buyer'
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
      loadStatus: 'more'
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
    statusText(status) {
      const map = { PENDING: '待确认', CONFIRMED: '已确认', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
      return map[status] || status
    },
    switchTab(val) {
      this.currentTab = val
      this.page = 1
      this.loadOrders()
    },
    async loadOrders() {
      this.loading = true
      try {
        const params = { page: this.page, pageSize: this.pageSize }
        if (this.currentTab) params.status = this.currentTab
        const res = await getBuyerOrders(params)
        const list = res.data?.records || res.data || []
        this.total = res.data?.total || list.length
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
.order-time {
  font-size: 22rpx;
  color: #ccc;
}
</style>
