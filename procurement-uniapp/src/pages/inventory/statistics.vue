<template>
  <view class="page-inv-stats container">
    <view class="card saas-card">
      <view class="section-header">
        <text class="section-title">📦 库存概览</text>
      </view>
      <view class="stats-grid">
        <view class="stats-item">
          <text class="stats-value">{{ stats.totalProducts || 0 }}</text>
          <text class="stats-label">商品种数</text>
        </view>
        <view class="stats-item">
          <text class="stats-value">{{ stats.totalStock || 0 }}</text>
          <text class="stats-label">总库存件数</text>
        </view>
        <view class="stats-item stats-item--clickable" @tap="goWarning">
          <text class="stats-value stats-value--warning">{{ stats.warningCount || 0 }}</text>
          <text class="stats-label">预警商品 <text class="arrow">›</text></text>
        </view>
      </view>
    </view>

    <!-- 分类库存分布 -->
    <view class="card saas-card">
      <view class="section-header">
        <text class="section-title">📊 分类库存分布</text>
      </view>
      <view
        v-for="item in stats.categoryStats"
        :key="item.categoryName"
        class="category-stat-row"
      >
        <text class="category-stat-name">{{ item.categoryName }}</text>
        <text class="category-stat-count">{{ item.productCount }} 件 / {{ item.stockCount }} 库存</text>
      </view>
      <view v-if="loading" class="loading-tip">加载中...</view>
      <EmptyState v-else-if="!stats.categoryStats || stats.categoryStats.length === 0" text="暂无数据" />
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
      stats: {},
      loading: false
    }
  },
  onShow() {
    this.loadStats()
  },
  methods: {
    async loadStats() {
      this.loading = true
      try {
        this.stats = await getInventoryStats()
      } catch (e) {
        uni.showToast({ title: '加载统计失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    goWarning() {
      if (!this.stats.warningCount) return
      uni.$emit('showStockWarning')
      uni.navigateBack()
    }
  }
}
</script>

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 24rpx -2rpx rgba(16, 24, 40, 0.04);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #111827;
}

.stats-grid {
  display: flex;
  justify-content: space-around;
  padding: 16rpx 0;
}

.stats-item {
  text-align: center;
  flex: 1;
  
  &--clickable {
    background: rgba(243, 167, 63, 0.05);
    border-radius: 12rpx;
    padding: 16rpx 0;
    margin-top: -16rpx;
  }
}

.stats-value {
  display: block;
  font-size: 48rpx;
  font-weight: 700;
  color: #111827;
  font-family: inherit, DIN, Roboto, -apple-system;

  &--warning {
    color: #EF4444;
  }
}

.stats-label {
  display: block;
  font-size: 26rpx;
  color: #6B7280;
  margin-top: 12rpx;
  
  .arrow {
    display: inline-block;
    margin-left: 4rpx;
    color: #EF4444;
  }
}

.category-stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #E5E7EB;
  
  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.category-stat-name {
  font-size: 28rpx;
  color: #374151;
  font-weight: 500;
}

.category-stat-count {
  font-size: 28rpx;
  color: #6B7280;
  background: #F3F4F6;
  padding: 4rpx 16rpx;
  border-radius: 30rpx;
}
</style>
