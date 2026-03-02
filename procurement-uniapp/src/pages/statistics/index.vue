<template>
  <view class="page-statistics container">
    <!-- 经营概览 -->
    <view class="card">
      <text class="section-title">经营概览</text>
      <view class="overview-grid">
        <view class="overview-item">
          <text class="overview-value price-text">¥{{ overview.todaySales || 0 }}</text>
          <text class="overview-label">今日销售</text>
        </view>
        <view class="overview-item">
          <text class="overview-value price-text">¥{{ overview.monthSales || 0 }}</text>
          <text class="overview-label">本月销售</text>
        </view>
        <view class="overview-item">
          <text class="overview-value" style="color: #18bc37;">¥{{ overview.todayProfit || 0 }}</text>
          <text class="overview-label">今日利润</text>
        </view>
        <view class="overview-item">
          <text class="overview-value" style="color: #18bc37;">¥{{ overview.monthProfit || 0 }}</text>
          <text class="overview-label">本月利润</text>
        </view>
      </view>
    </view>

    <!-- 其他指标 -->
    <view class="card">
      <view class="metric-row">
        <text class="metric-label">库存总值</text>
        <text class="metric-value">¥{{ overview.inventoryValue || 0 }}</text>
      </view>
      <view class="metric-row">
        <text class="metric-label">库存商品数</text>
        <text class="metric-value">{{ overview.inventoryCount || 0 }}</text>
      </view>
      <view class="metric-row">
        <text class="metric-label">待处理订单</text>
        <text class="metric-value" style="color: #2979ff;">{{ overview.pendingOrderCount || 0 }}</text>
      </view>
      <view class="metric-row">
        <text class="metric-label">库存预警</text>
        <text class="metric-value" style="color: #f3a73f;">{{ overview.stockWarningCount || 0 }}</text>
      </view>
    </view>

    <!-- 销售趋势（图表占位，Phase 3 完善） -->
    <view class="card">
      <text class="section-title">销售趋势</text>
      <view class="chart-placeholder">
        <text class="chart-placeholder__text">📊 图表将在 Phase 3 实现</text>
      </view>
    </view>

    <!-- 商品排行 -->
    <view class="card">
      <text class="section-title">商品销售排行</text>
      <view v-for="(item, idx) in ranking" :key="idx" class="ranking-item">
        <view class="ranking-item__left">
          <text class="ranking-item__rank">{{ idx + 1 }}</text>
          <text class="ranking-item__name">{{ item.productName }}</text>
        </view>
        <text class="ranking-item__amount price-text">¥{{ item.totalAmount }}</text>
      </view>
      <EmptyState v-if="ranking.length === 0" text="暂无数据" />
    </view>
  </view>
</template>

<script>
import { getOverview, getSalesRanking } from '@/api/statistics'
import EmptyState from '@/components/common/EmptyState.vue'

export default {
  components: { EmptyState },
  data() {
    return {
      overview: {},
      ranking: []
    }
  },
  onShow() {
    this.loadData()
  },
  methods: {
    async loadData() {
      try {
        const [overviewData, rankingData] = await Promise.all([
          getOverview(),
          getSalesRanking({ period: 'month', limit: 10 })
        ])
        this.overview = overviewData || {}
        this.ranking = rankingData || []
      } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
.section-title { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 24rpx; }

.overview-grid {
  display: flex;
  flex-wrap: wrap;
}

.overview-item {
  width: 50%;
  text-align: center;
  padding: 12rpx 0;
}

.overview-value { display: block; font-size: 36rpx; font-weight: 700; }
.overview-label { font-size: 24rpx; color: #999; margin-top: 4rpx; }

.metric-row {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.metric-label { font-size: 28rpx; color: #666; }
.metric-value { font-size: 28rpx; font-weight: 600; color: #333; }

.chart-placeholder {
  height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6fa;
  border-radius: 12rpx;

  &__text { font-size: 28rpx; color: #999; }
}

.ranking-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &__left { display: flex; align-items: center; gap: 16rpx; }
  &__rank { font-size: 28rpx; color: #2979ff; font-weight: 600; min-width: 40rpx; }
  &__name { font-size: 28rpx; color: #333; }
  &__amount { font-size: 28rpx; }
}
</style>
