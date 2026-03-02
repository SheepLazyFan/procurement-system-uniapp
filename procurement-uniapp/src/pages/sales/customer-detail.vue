<template>
  <view class="page-customer-detail container">
    <view class="card">
      <text class="customer-name">{{ customer.name }}</text>
      <view class="info-row">
        <text class="info-label">电话</text>
        <text class="info-value">{{ customer.phone || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">地址</text>
        <text class="info-value">{{ customer.address || '-' }}</text>
      </view>
      <view class="info-row" v-if="customer.remark">
        <text class="info-label">备注</text>
        <text class="info-value">{{ customer.remark }}</text>
      </view>
    </view>

    <view class="card">
      <view class="stats-row">
        <view class="stats-item">
          <text class="stats-value">{{ customer.orderCount || 0 }}</text>
          <text class="stats-label">订单数</text>
        </view>
        <view class="stats-item">
          <text class="stats-value price-text">¥{{ customer.totalAmount || 0 }}</text>
          <text class="stats-label">累计金额</text>
        </view>
      </view>
    </view>

    <view class="card">
      <text class="section-title">最近订单</text>
      <view v-for="order in customer.recentOrders" :key="order.id" class="recent-order" @tap="goOrderDetail(order.id)">
        <text class="recent-order__no">{{ order.orderNo }}</text>
        <text class="recent-order__amount">¥{{ order.totalAmount }}</text>
      </view>
      <EmptyState v-if="!customer.recentOrders || customer.recentOrders.length === 0" text="暂无订单" />
    </view>
  </view>
</template>

<script>
import { getCustomerDetail } from '@/api/customer'
import EmptyState from '@/components/common/EmptyState.vue'

export default {
  components: { EmptyState },
  data() {
    return {
      customerId: null,
      customer: {}
    }
  },
  onLoad(query) {
    this.customerId = Number(query.id)
    this.loadDetail()
  },
  methods: {
    async loadDetail() {
      try {
        this.customer = await getCustomerDetail(this.customerId)
      } catch (e) {}
    },
    goOrderDetail(id) {
      uni.navigateTo({ url: `/pages/sales/detail?id=${id}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.customer-name { font-size: 36rpx; font-weight: 700; color: #333; margin-bottom: 20rpx; }
.section-title { font-size: 30rpx; font-weight: 600; margin-bottom: 16rpx; }
.info-row { display: flex; justify-content: space-between; padding: 10rpx 0; }
.info-label { font-size: 26rpx; color: #999; }
.info-value { font-size: 26rpx; color: #333; }
.stats-row { display: flex; justify-content: space-around; }
.stats-item { text-align: center; }
.stats-value { display: block; font-size: 40rpx; font-weight: 700; color: #333; }
.stats-label { font-size: 24rpx; color: #999; }
.recent-order {
  display: flex; justify-content: space-between;
  padding: 12rpx 0; border-bottom: 1rpx solid #f5f5f5;
  &__no { font-size: 26rpx; color: #333; }
  &__amount { font-size: 26rpx; color: #ff4d4f; }
}
</style>
