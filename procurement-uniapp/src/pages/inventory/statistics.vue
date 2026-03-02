<template>
  <view class="page-inv-stats container">
    <view class="card">
      <text class="section-title">库存概览</text>
      <view class="stats-grid">
        <view class="stats-item">
          <text class="stats-value">{{ stats.totalProducts || 0 }}</text>
          <text class="stats-label">商品总数</text>
        </view>
        <view class="stats-item">
          <text class="stats-value">{{ stats.totalStock || 0 }}</text>
          <text class="stats-label">总库存量</text>
        </view>
        <view class="stats-item">
          <text class="stats-value stats-value--warning">{{ stats.warningCount || 0 }}</text>
          <text class="stats-label">预警商品</text>
        </view>
      </view>
    </view>

    <!-- 分类库存分布 -->
    <view class="card">
      <text class="section-title">分类库存分布</text>
      <view
        v-for="item in stats.categoryStats"
        :key="item.categoryName"
        class="category-stat-row"
      >
        <text class="category-stat-name">{{ item.categoryName }}</text>
        <text class="category-stat-count">{{ item.productCount }} 件 / {{ item.stockCount }} 库存</text>
      </view>
      <EmptyState v-if="!stats.categoryStats || stats.categoryStats.length === 0" text="暂无数据" />
    </view>
  </view>
</template>

<script>
import { getInventoryStats } from '@/api/statistics'
import EmptyState from '@/components/common/EmptyState.vue'

export default {
  components: { EmptyState },
  data() {
    return {
      stats: {}
    }
  },
  onShow() {
    this.loadStats()
  },
  methods: {
    async loadStats() {
      try {
        this.stats = await getInventoryStats()
      } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 24rpx;
}

.stats-grid {
  display: flex;
  justify-content: space-around;
}

.stats-item {
  text-align: center;
}

.stats-value {
  display: block;
  font-size: 44rpx;
  font-weight: 700;
  color: #333;

  &--warning {
    color: #f3a73f;
  }
}

.stats-label {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.category-stat-row {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.category-stat-name {
  font-size: 28rpx;
  color: #333;
}

.category-stat-count {
  font-size: 26rpx;
  color: #999;
}
</style>
