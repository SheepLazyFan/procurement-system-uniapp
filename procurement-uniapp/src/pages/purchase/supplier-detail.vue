<template>
  <view class="page-supplier-detail container">
    <view class="card">
      <text class="supplier-name">{{ supplier.name }}</text>
      <view class="info-row">
        <text class="info-label">电话</text>
        <text class="info-value">{{ supplier.phone || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">地址</text>
        <text class="info-value">{{ supplier.address || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">主营品类</text>
        <text class="info-value">{{ supplier.mainCategory || '-' }}</text>
      </view>
    </view>

    <view class="card">
      <view class="stats-row">
        <view class="stats-item">
          <text class="stats-value">{{ supplier.purchaseCount || 0 }}</text>
          <text class="stats-label">采购次数</text>
        </view>
        <view class="stats-item">
          <text class="stats-value price-text">¥{{ supplier.totalAmount || 0 }}</text>
          <text class="stats-label">累计金额</text>
        </view>
      </view>
    </view>

    <!-- 最近采购记录 -->
    <view class="card">
      <text class="section-title">最近采购</text>
      <view v-for="order in supplier.recentOrders" :key="order.id" class="recent-order">
        <text class="recent-order__no">{{ order.orderNo }}</text>
        <text class="recent-order__amount">¥{{ order.totalAmount }}</text>
      </view>
      <EmptyState v-if="!supplier.recentOrders || supplier.recentOrders.length === 0" text="暂无采购记录" />
    </view>
  </view>
</template>

<script>
import { getSupplierDetail } from '@/api/supplier'
import EmptyState from '@/components/common/EmptyState.vue'

export default {
  components: { EmptyState },
  data() {
    return {
      supplierId: null,
      supplier: {}
    }
  },
  onLoad(query) {
    this.supplierId = Number(query.id)
    this.loadDetail()
  },
  methods: {
    async loadDetail() {
      try {
        this.supplier = await getSupplierDetail(this.supplierId)
      } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
.supplier-name {
  font-size: 36rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
}

.info-label { font-size: 26rpx; color: #999; }
.info-value { font-size: 26rpx; color: #333; }

.stats-row {
  display: flex;
  justify-content: space-around;
}

.stats-item { text-align: center; }
.stats-value { display: block; font-size: 40rpx; font-weight: 700; color: #333; }
.stats-label { font-size: 24rpx; color: #999; }

.recent-order {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &__no { font-size: 26rpx; color: #333; }
  &__amount { font-size: 26rpx; color: #ff4d4f; }
}
</style>
