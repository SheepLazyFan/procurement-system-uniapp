<template>
  <view class="page-statistics">
    <!-- 无权限提示 -->
    <view v-if="noAccess" class="no-access-container">
      <text class="no-access-icon">🔒</text>
      <text class="no-access-text">{{ noAccessTip }}</text>
    </view>

    <template v-else>
      <!-- 1. 顶部数据驾驶舱 (Hero Header) -->
      <view class="hero-card animate-fade-up" style="animation-delay: 0s;">
        <text class="hero-title">今日销售 (元)</text>
        <text class="hero-main-value num-font">¥{{ overview.todaySales || '0.00' }}</text>
        
        <view class="hero-sub-metrics">
          <view class="hero-sub-item">
            <text class="hero-sub-label">今日利润</text>
            <text class="hero-sub-value num-font">+{{ overview.todayProfit || '0.00' }}</text>
          </view>
          <view class="hero-divider"></view>
          <view class="hero-sub-item">
            <text class="hero-sub-label">本月销售</text>
            <text class="hero-sub-value num-font">{{ overview.monthSales || '0.00' }}</text>
          </view>
          <view class="hero-divider"></view>
          <view class="hero-sub-item">
            <text class="hero-sub-label">本月利润</text>
            <text class="hero-sub-value num-font">+{{ overview.monthProfit || '0.00' }}</text>
          </view>
        </view>
      </view>

      <!-- 2. 模块化网格 (Bento Box) -->
      <view class="bento-grid animate-fade-up" style="animation-delay: 0.1s;">
        <view class="bento-item saas-card" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
          <view class="bento-icon-wrapper bg-blue-light">
            <view class="svg-icon-box"></view>
          </view>
          <text class="bento-value num-font">¥{{ overview.inventoryValue || '0.00' }}</text>
          <text class="bento-label">库存总值</text>
        </view>
        
        <view class="bento-item saas-card" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
          <view class="bento-icon-wrapper bg-purple-light">
            <view class="svg-icon-chart"></view>
          </view>
          <text class="bento-value num-font">{{ overview.inventoryCount || 0 }}</text>
          <text class="bento-label">库存商品数</text>
        </view>

        <view class="bento-item saas-card" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
          <view class="bento-icon-wrapper bg-orange-light">
            <view class="svg-icon-timer"></view>
          </view>
          <text class="bento-value num-font">{{ overview.pendingOrderCount || 0 }}</text>
          <text class="bento-label">待处理订单</text>
        </view>

        <view class="bento-item saas-card" @tap="goStockWarning" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
          <view class="bento-icon-wrapper bg-red-light">
            <view class="svg-icon-alert"></view>
          </view>
          <view class="bento-value-row">
            <text class="bento-value num-font text-danger">{{ overview.stockWarningCount || 0 }}</text>
            <text class="bento-arrow">›</text>
          </view>
          <text class="bento-label">库存预警</text>
        </view>
      </view>

      <!-- 3. 销售趋势 -->
      <view class="saas-card chart-card animate-fade-up" style="animation-delay: 0.2s;">
        <view class="chart-header">
          <text class="section-title" style="margin-bottom:0;">销售趋势</text>
          <!-- 跑道型玻璃分段器 -->
          <view class="segmented-control">
            <view class="segment-slider" :class="'slider-' + period"></view>
            <text
              v-for="p in periodOptions"
              :key="p.value"
              class="segment-tab"
              :class="{ 'segment-tab--active': period === p.value }"
              @tap="switchPeriod(p.value)"
            >{{ p.label }}</text>
          </view>
        </view>

        <!-- 图表区域 -->
        <view v-if="trendData.length" class="chart-area">
          <view class="bar-chart">
            <view class="y-col">
              <view class="y-labels">
                <text class="y-label" style="top:0;">{{ formatCompact(maxAmount) }}</text>
                <text class="y-label" style="top:50%;">{{ formatCompact(Math.round(maxAmount / 2)) }}</text>
                <text class="y-label" style="top:100%;">0</text>
              </view>
              <view class="y-spacer"></view>
            </view>
            
            <scroll-view scroll-x class="chart-scroll">
              <view class="chart-body" :style="{ width: chartWidth }">
                <view class="plot-region">
                  <view class="grid-base"></view> 
                  <view v-for="(item, idx) in trendData" :key="idx"
                    class="plot-col"
                    :class="{ 'plot-col--active': selectedBarIdx === idx }"
                    @tap="selectBar(idx)">
                    <view class="bar bar--sales" :style="{ height: barHeight(item.amount) + 'rpx' }"></view>
                    <view class="bar bar--profit" :style="{ height: barHeight(item.profit) + 'rpx' }"></view>
                  </view>
                </view>
                <view class="x-row">
                  <view v-for="(item, idx) in trendData" :key="'x'+idx" class="x-cell">
                    <text class="x-text">{{ showLabel(idx) ? formatDateLabel(item.date) : '' }}</text>
                  </view>
                </view>
              </view>
            </scroll-view>
          </view>

          <!-- 悬浮数据小窗 -->
          <view v-if="selectedBarIdx >= 0 && trendData[selectedBarIdx]" class="selected-detail animate-fade-up">
            <view class="selected-detail__header">
              <text class="selected-detail__date">{{ trendData[selectedBarIdx].date }}</text>
              <text class="selected-detail__close" @tap="selectBar(-1)">✕</text>
            </view>
            <view class="selected-detail__body">
              <view class="selected-detail__item">
                <view class="selected-detail__dot bg-brand"></view>
                <text class="selected-detail__label">销售</text>
                <text class="selected-detail__value text-brand num-font">¥{{ trendData[selectedBarIdx].amount || 0 }}</text>
              </view>
              <view class="selected-detail__item">
                <view class="selected-detail__dot bg-success"></view>
                <text class="selected-detail__label">利润</text>
                <text class="selected-detail__value text-success num-font">¥{{ trendData[selectedBarIdx].profit || 0 }}</text>
              </view>
            </view>
          </view>

          <!-- 全景胶囊 (Pill Metrics) -->
          <view class="trend-capsules">
            <view class="capsule capsule--blue">
              <text class="capsule-label">累计销售</text>
              <text class="capsule-value num-font">¥{{ trendTotalSales }}</text>
            </view>
            <view class="capsule capsule--green">
              <text class="capsule-label">累计利润</text>
              <text class="capsule-value num-font">¥{{ trendTotalProfit }}</text>
            </view>
            <view class="capsule capsule--purple">
              <text class="capsule-label">总订单数</text>
              <text class="capsule-value num-font">{{ trendTotalOrders }}</text>
            </view>
          </view>
        </view>
        <view v-else class="chart-empty">
          <text class="chart-empty__text">暂无趋势数据</text>
        </view>
      </view>

      <!-- 4. 商品排行 -->
      <view class="saas-card animate-fade-up" style="animation-delay: 0.3s;">
        <text class="section-title">商品销售排行</text>
        <view class="ranking-list">
          <view v-for="(item, idx) in ranking" :key="idx" class="ranking-item">
            <view class="ranking-item__left">
              <view class="rank-badge" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</view>
              <text class="ranking-item__name">{{ item.productName }}</text>
            </view>
            <text class="ranking-item__amount num-font text-brand">¥{{ item.totalAmount }}</text>
          </view>
        </view>
        <view v-if="loading" class="loading-tip">加载中...</view>
        <EmptyState v-else-if="ranking.length === 0" text="暂无数据" />
      </view>

      <view class="safe-bottom-space"></view>
    </template>
  </view>
</template>

<script>
import { getOverview, getSalesTrend, getSalesRanking } from '@/api/statistics'
import { guardTabPage, getNoAccessTip } from '@/utils/permission'
import EmptyState from '@/components/common/EmptyState.vue'

export default {
  components: { EmptyState },
  data() {
    return {
      noAccess: false,
      noAccessTip: '',
      overview: {},
      ranking: [],
      trendData: [],
      selectedBarIdx: -1,
      loading: false,
      period: 'week',
      periodOptions: [
        { label: '近7天', value: 'week' },
        { label: '近30天', value: 'month' }
      ]
    }
  },
  computed: {
    chartWidth() {
      const count = this.trendData.length
      if (this.period === 'week') return '100%'
      // 30天模式: 每列46rpx + 左右padding各12rpx
      const w = count * 46 + 24
      return w + 'rpx'
    },
    maxAmount() {
      if (!this.trendData.length) return 0
      const max = Math.max(...this.trendData.map(d => Number(d.amount) || 0))
      return max || 1
    },
    trendTotalSales() {
      return this.trendData.reduce((s, d) => s + (Number(d.amount) || 0), 0).toFixed(2)
    },
    trendTotalProfit() {
      return this.trendData.reduce((s, d) => s + (Number(d.profit) || 0), 0).toFixed(2)
    },
    trendTotalOrders() {
      return this.trendData.reduce((s, d) => s + (Number(d.orderCount) || 0), 0)
    }
  },
  onShow() {
    const canAccess = guardTabPage('statistics')
    this.noAccess = !canAccess
    if (!canAccess) {
      this.noAccessTip = getNoAccessTip('statistics')
      return
    }
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true

      // 每个 API 独立调用、独立容错，一个失败不影响其他
      // 1) 概览数据
      try {
        const overviewData = await getOverview()
        this.overview = overviewData || {}
        // 设置库存 tabBar 角标
        const warnCount = this.overview.stockWarningCount || 0
        if (warnCount > 0) {
          uni.setTabBarBadge({ index: 0, text: String(warnCount) })
        } else {
          uni.removeTabBarBadge({ index: 0 })
        }
      } catch (e) {
        console.warn('[statistics] 概览数据加载失败:', e && e.message || e)
        this.overview = {}
      }

      // 2) 商品排行
      try {
        const rankingData = await getSalesRanking({ period: 'month', limit: 10 })
        this.ranking = rankingData || []
      } catch (e) {
        console.warn('[statistics] 排行数据加载失败:', e && e.message || e)
        this.ranking = []
      }

      this.loading = false

      // 3) 趋势图表（独立加载）
      this.loadTrend()
    },
    async loadTrend() {
      this.selectedBarIdx = -1
      try {
        const days = this.period === 'week' ? 7 : 30
        const end = this.formatDate(new Date())
        const startD = new Date()
        startD.setDate(startD.getDate() - days + 1)
        const start = this.formatDate(startD)
        const res = await getSalesTrend({ period: 'day', startDate: start, endDate: end })
        this.trendData = res || []
      } catch (e) {
        console.warn('[statistics] 趋势数据加载失败:', e && e.message || e)
        this.trendData = []
      }
    },
    switchPeriod(p) {
      this.period = p
      this.loadTrend()
    },
    selectBar(idx) {
      this.selectedBarIdx = this.selectedBarIdx === idx ? -1 : idx
    },
    showLabel(idx) {
      if (this.period === 'week') return true
      return idx % 5 === 0 || idx === this.trendData.length - 1
    },
    barHeight(val) {
      const PLOT_H = 260
      const v = Number(val) || 0
      const max = this.maxAmount
      return Math.max(Math.round((v / max) * PLOT_H), 3)
    },
    formatCompact(num) {
      if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
      if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
      return num
    },
    formatDateLabel(dateStr) {
      if (!dateStr) return ''
      const parts = dateStr.split('-')
      return `${parts[1]}/${parts[2]}`
    },
    formatDate(d) {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    },
    goStockWarning() {
      uni.switchTab({ url: '/pages/inventory/index' })
      // switchTab 不支持 query 参数，通过全局事件通知库存页
      uni.$emit('showStockWarning')
    }
  }
}
</script>

<style lang="scss" scoped>
.page-statistics {
  padding: 24rpx;
  background: var(--bg-page);
  min-height: 100vh;
  box-sizing: border-box;
}
.saas-card { border-radius: 32rpx !important; }

.no-access-container {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 200rpx 60rpx;
}
.no-access-icon { font-size: 120rpx; margin-bottom: 32rpx; }
.no-access-text { font-size: 28rpx; color: var(--text-tertiary); text-align: center; }

/* =======================================
   1. Hero Header (深空渐变数据驾驶舱)
   ======================================= */
.hero-card {
  position: relative;
  background: #ffffff;
  border-radius: 40rpx;
  padding: 48rpx 40rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(0,0,0,0.04);
  overflow: hidden;
}
.hero-title {
  position: relative; z-index: 10;
  display: block; font-size: 26rpx; color: var(--text-secondary); margin-bottom: 12rpx; font-weight: 600;
}
.hero-main-value {
  position: relative; z-index: 10; display: block;
  font-size: 72rpx; font-weight: 800; color: var(--text-primary); letter-spacing: 2rpx;
  margin-bottom: 40rpx;
}
.hero-sub-metrics {
  position: relative; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  background: var(--bg-page);
  padding: 24rpx;
  border-radius: var(--radius-lg);
}
.hero-sub-item { display: flex; flex-direction: column; gap: 8rpx; }
.hero-sub-label { font-size: 22rpx; color: var(--text-tertiary); font-weight: 600; }
.hero-sub-value { font-size: 30rpx; font-weight: 800; color: var(--text-primary); }
.hero-divider { width: 2rpx; height: 32rpx; background: rgba(0,0,0,0.06); }

/* =======================================
   2. Bento Box Grid (微型便当盒)
   ======================================= */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  margin-bottom: 24rpx;
}
.bento-item {
  padding: 32rpx 24rpx;
  display: flex; flex-direction: column; align-items: flex-start;
  margin-bottom: 0;
}
.bento-icon-wrapper {
  width: 72rpx; height: 72rpx;
  border-radius: 20rpx;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 24rpx;
}
.svg-icon-box {
  width: 40rpx; height: 40rpx; opacity: 0.9;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%232979ff' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z'%3E%3C/path%3E%3Cpolyline points='3.27 6.96 12 12.01 20.73 6.96'%3E%3C/polyline%3E%3Cline x1='12' y1='22.08' x2='12' y2='12'%3E%3C/line%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}
.svg-icon-chart {
  width: 40rpx; height: 40rpx; opacity: 0.9;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239c27b0' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Crect x='18' y='3' width='4' height='18' rx='1'%3E%3C/rect%3E%3Crect x='10' y='8' width='4' height='13' rx='1'%3E%3C/rect%3E%3Crect x='2' y='13' width='4' height='8' rx='1'%3E%3C/rect%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}
.svg-icon-timer {
  width: 40rpx; height: 40rpx; opacity: 0.9;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ff9800' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M5 22h14'%3E%3C/path%3E%3Cpath d='M5 2h14'%3E%3C/path%3E%3Cpath d='m17 22-5-8-5 8'%3E%3C/path%3E%3Cpath d='m17 2-5 8-5-8'%3E%3C/path%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}
.svg-icon-alert {
  width: 40rpx; height: 40rpx; opacity: 0.9;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23f44336' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9'%3E%3C/path%3E%3Cpath d='M13.73 21a2 2 0 0 1-3.46 0'%3E%3C/path%3E%3Cpath d='M12 9v4'%3E%3C/path%3E%3Ccircle cx='12' cy='17' r='1'%3E%3C/circle%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}
.bento-value { font-size: 40rpx; font-weight: 700; color: var(--text-primary); margin-bottom: 4rpx; }
.bento-value-row { display: flex; align-items: center; width: 100%; justify-content: space-between; }
.bento-arrow { font-size: 32rpx; color: var(--color-danger); opacity: 0.5; }
.bento-label { font-size: 24rpx; color: var(--text-secondary); }

/* Utility Colors for Bento */
.bg-blue-light { background: rgba(41,121,255,0.1); }
.bg-purple-light { background: rgba(156,39,176,0.1); }
.bg-orange-light { background: rgba(255,152,0,0.1); }
.bg-red-light { background: rgba(244,67,54,0.1); }
.text-brand { color: var(--brand-primary); }
.text-purple { color: #9c27b0; }
.text-orange { color: #ff9800; }
.text-danger { color: var(--color-danger); }
.text-success { color: var(--color-success); }
.bg-brand { background: var(--brand-primary); }
.bg-success { background: var(--color-success); }

/* =======================================
   3. Sales Trend (销售趋势 & 分段器)
   ======================================= */
.section-title { font-size: 32rpx; font-weight: 700; color: var(--text-primary); margin-bottom: 24rpx; display: block; }
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx; }

/* 跑道分段器 */
.segmented-control {
  position: relative;
  display: flex;
  background: var(--bg-page);
  border-radius: 40rpx;
  padding: 6rpx;
  box-shadow: inset 0 2rpx 8rpx rgba(0,0,0,0.02);
}
.segment-tab {
  position: relative; z-index: 2;
  padding: 8rpx 24rpx; font-size: 24rpx; font-weight: 600;
  color: var(--text-secondary); transition: color 0.3s;
}
.segment-tab--active { color: #ffffff; }
.segment-slider {
  position: absolute; top: 6rpx; bottom: 6rpx; width: 50%;
  background: var(--brand-primary); border-radius: 30rpx;
  transition: transform 0.3s cubic-bezier(0.25, 1.25, 0.2, 1);
  box-shadow: 0 4rpx 12rpx rgba(41, 121, 255, 0.4);
}
.slider-week { transform: translateX(0); }
.slider-month { transform: translateX(100%); }

.chart-area { margin-top: 12rpx; }
.bar-chart { display: flex; padding-top: 20rpx; }
.y-col { width: 70rpx; flex-shrink: 0; padding-right: 12rpx; }
.y-labels { height: 260rpx; position: relative; }
.y-label { position: absolute; right: 0; font-size: 20rpx; font-weight: 600; color: var(--text-tertiary); transform: translateY(-50%); }
.y-spacer { height: 28rpx; }

.chart-scroll { flex: 1; min-width: 0; overflow: hidden; }
.chart-body { display: flex; flex-direction: column; padding-bottom: 8rpx; }

.plot-region {
  height: 260rpx; position: relative; display: flex; align-items: stretch;
  padding: 0 16rpx;
}
.grid-base {
  position: absolute; left: 0; right: 0; bottom: 0; height: 2rpx;
  background: var(--border-light); pointer-events: none; z-index: 0;
}
.plot-col {
  flex: 1; height: 100%; display: flex; gap: 6rpx; align-items: flex-end; justify-content: center;
  min-width: 40rpx; padding: 0 2rpx; border-radius: 8rpx; z-index: 1;
  transition: background 0.2s;
}
.plot-col--active { background: rgba(41, 121, 255, 0.05); }
.bar { width: 16rpx; border-radius: 8rpx 8rpx 0 0; transition: height 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.2); }
.bar--sales { background: linear-gradient(180deg, #1c5ff8 0%, #2979ff 100%); }
.bar--profit { background: linear-gradient(180deg, #0fa82e 0%, #18bc37 100%); }

.x-row { display: flex; padding: 0 16rpx; height: 32rpx; margin-top: 8rpx; }
.x-cell { flex: 1; min-width: 40rpx; display: flex; align-items: center; justify-content: center; }
.x-text { font-size: 18rpx; font-weight: 600; color: var(--text-tertiary); white-space: nowrap; }

.selected-detail {
  margin-top: 24rpx; padding: 20rpx 24rpx;
  background: var(--bg-page); border-radius: var(--radius-md);
  border: 1rpx solid rgba(0,0,0,0.02);
}
.selected-detail__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.selected-detail__date { font-size: 26rpx; font-weight: 700; color: var(--text-primary); }
.selected-detail__close { font-size: 32rpx; color: var(--text-tertiary); padding: 4rpx 12rpx; }
.selected-detail__body { display: flex; gap: 32rpx; }
.selected-detail__item { display: flex; align-items: center; gap: 12rpx; }
.selected-detail__dot { width: 14rpx; height: 14rpx; border-radius: 50%; }
.selected-detail__label { font-size: 24rpx; color: var(--text-secondary); }
.selected-detail__value { font-size: 28rpx; font-weight: 700; }

.trend-capsules {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 16rpx;
  margin-top: 32rpx;
}
.capsule {
  padding: 24rpx 16rpx; border-radius: var(--radius-lg);
  display: flex; flex-direction: column; align-items: center; box-sizing: border-box;
}
.capsule--blue { background: rgba(41, 121, 255, 0.08); }
.capsule--green { background: rgba(24, 188, 55, 0.08); }
.capsule--purple { background: rgba(156, 39, 176, 0.08); }

.capsule-label { font-size: 22rpx; font-weight: 600; color: var(--text-secondary); margin-bottom: 8rpx; }
.capsule-value { font-size: 32rpx; font-weight: 800; }
.capsule--blue .capsule-value { color: var(--brand-primary); }
.capsule--green .capsule-value { color: var(--color-success); }
.capsule--purple .capsule-value { color: #9c27b0; }

.ranking-list { display: flex; flex-direction: column; gap: 24rpx; }
.ranking-item { display: flex; justify-content: space-between; align-items: center; }
.ranking-item__left { display: flex; align-items: center; gap: 20rpx; }
.rank-badge {
  width: 48rpx; height: 48rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 26rpx; font-weight: 800; color: var(--text-tertiary);
  background: var(--bg-page);
}
.rank-1 { background: linear-gradient(135deg, #ff4d4f, #ff7875); color: #fff; box-shadow: 0 4rpx 12rpx rgba(255,77,79,0.3); }
.rank-2 { background: linear-gradient(135deg, #ff9c00, #ffb84d); color: #fff; box-shadow: 0 4rpx 12rpx rgba(255,156,0,0.3); }
.rank-3 { background: linear-gradient(135deg, #2979ff, #699fff); color: #fff; box-shadow: 0 4rpx 12rpx rgba(41,121,255,0.3); }
.ranking-item__name { font-size: 28rpx; font-weight: 600; color: var(--text-primary); }
.ranking-item__amount { font-size: 30rpx; font-weight: 800; display: block; }

.safe-bottom-space { content:''; display: block; width: 100%; height: 60rpx; }
</style>
