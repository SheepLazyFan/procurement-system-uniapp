<template>
  <view class="page-sales">
    <!-- 无权限提示 -->
    <view v-if="noAccess" class="no-access-container">
      <text class="no-access-icon">🔒</text>
      <text class="no-access-text">{{ noAccessTip }}</text>
    </view>

    <template v-else>
    <view class="hero-header">
      <!-- 汇总数据 -->
      <view class="hero-header__stat">
        <view class="stat-content">
          <text class="stat-label">今日待处理回款 (元)</text>
          <text class="stat-value num-font">--</text>
        </view>
      </view>
      
      <!-- 顶部搜索栏 -->
      <view class="search-bar">
        <view class="search-bar__input-wrap">
          <view class="search-bar__icon"></view>
          <input
            class="search-bar__input"
            v-model="keyword"
            placeholder="搜索订单号 / 客户"
            placeholder-class="search-placeholder-white"
            confirm-type="search"
            @confirm="doSearch"
          />
          <view v-if="keyword" class="search-bar__clear" @tap="clearKeyword">
            <view class="svg-icon-close"></view>
          </view>
        </view>
        <view class="search-bar__filter" :class="{ 'search-bar__filter--active': hasActiveFilter }" @tap="showFilterPopup = true" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
          <view class="filter-icon-svg"></view>
          <view v-if="hasActiveFilter" class="search-bar__filter-badge"></view>
        </view>
      </view>

      <!-- 状态筛选 -->
      <scroll-view scroll-x class="status-tabs">
        <view
          v-for="tab in statusTabs"
          :key="tab.value"
          class="status-tab"
          :class="{ 'status-tab--active': selectedStatus === tab.value }"
          @tap="onStatusTap(tab.value)"
          hover-class="saas-card-push"
          :hover-start-time="0"
          :hover-stay-time="100"
        >
          <text class="status-tab__text">{{ tab.label }}</text>
          <text v-if="statusCounts[tab.countKey] > 0" class="status-tab__badge">{{ statusCounts[tab.countKey] > 99 ? '99+' : statusCounts[tab.countKey] }}</text>
        </view>
      </scroll-view>
    </view>

    <!-- 便当盒操作区 (Bento Box) -->
    <view class="bento-actions container">
      <view class="bento-card bento-supplier" @tap="goCustomerList" hover-class="saas-card-push">
        <view class="bento-icon-users"></view>
        <text class="bento-text">客户大厅</text>
      </view>
      <view class="bento-card bento-primary" @tap="goCreateOrder" hover-class="saas-card-push">
        <view class="bento-icon-plus"></view>
        <text class="bento-text">新建销售单</text>
      </view>
    </view>

    <!-- 悬浮订单孤岛 (Floating Islands) -->
    <view class="order-list">
      <view
        v-for="(order, index) in orderList"
        :key="order.id"
        class="order-island animate-fade-up"
        hover-class="saas-card-push"
        :hover-start-time="0"
        :hover-stay-time="100"
        :style="{ 'animation-delay': (index % 10) * 0.05 + 's' }"
        @tap="goDetail(order.id)"
      >
        <view class="order-card__header flex-between">
          <text class="order-card__no num-font">{{ order.orderNo }}</text>
          <view class="status-pill">
            <view class="status-dot" :class="'status-dot--' + getStatusType(order.status)"></view>
            <text class="status-text">{{ getStatusText(order.status, order.cancelBy) }}</text>
          </view>
        </view>
        <view class="order-card__customer">
          <text class="order-card__label">客户：</text>
          <text>{{ (order.customer && order.customer.name) || '-' }}</text>
        </view>
        <view class="order-card__footer flex-between">
          <text class="order-card__amount price-text num-font">¥{{ order.totalAmount }}</text>
          <text class="order-card__date num-font">{{ formatShortDate(order.createdAt) }}</text>
        </view>
      </view>

      <EmptyState v-if="orderList.length === 0 && !loading" text="空空如也" buttonText="起草首单" @action="goCreateOrder" />
      <LoadMore v-if="orderList.length > 0" :status="loadMoreStatus" @load="loadMore" />
    </view>

    <!-- 筛选弹窗 -->
    <view v-if="showFilterPopup" class="popup-mask" @tap="showFilterPopup = false">
      <view class="filter-sheet" @tap.stop>
        <view class="filter-sheet__header">
          <text class="filter-sheet__title">高级筛选</text>
          <text class="filter-sheet__close" @tap="showFilterPopup = false">✕</text>
        </view>

        <scroll-view scroll-y class="filter-sheet__body">
          <!-- 时间范围 -->
          <view class="filter-section">
            <text class="filter-section__label">时间范围</text>
            <view class="filter-chips">
              <text v-for="opt in timeOptions" :key="opt.value"
                class="filter-chip" :class="{ 'filter-chip--active': filterForm.timeRange === opt.value }"
                @tap="onTimeOptionTap(opt.value)">{{ opt.label }}</text>
            </view>
            <view v-if="filterForm.timeRange === 'custom'" class="filter-date-range">
              <picker mode="date" :value="filterForm.startDate" @change="e => filterForm.startDate = e.detail.value">
                <view class="filter-date-input">{{ filterForm.startDate || '开始日期' }}</view>
              </picker>
              <text class="filter-range-sep">至</text>
              <picker mode="date" :value="filterForm.endDate" @change="e => filterForm.endDate = e.detail.value">
                <view class="filter-date-input">{{ filterForm.endDate || '结束日期' }}</view>
              </picker>
            </view>
          </view>

          <!-- 金额范围 -->
          <view class="filter-section">
            <text class="filter-section__label">金额范围</text>
            <view class="filter-chips">
              <text v-for="opt in amountOptions" :key="opt.label"
                class="filter-chip" :class="{ 'filter-chip--active': filterForm.amountRange === opt.value }"
                @tap="onAmountOptionTap(opt.value)">{{ opt.label }}</text>
            </view>
            <view v-if="filterForm.amountRange === 'custom'" class="filter-custom-range">
              <input class="filter-range-input" type="digit" v-model="filterForm.customMinAmount" placeholder="最低金额" />
              <text class="filter-range-sep">—</text>
              <input class="filter-range-input" type="digit" v-model="filterForm.customMaxAmount" placeholder="最高金额" />
            </view>
          </view>

          <!-- 客户 -->
          <view class="filter-section">
            <text class="filter-section__label">客户</text>
            <view class="filter-customer-pick" @tap="openCustomerPicker">
              <text :class="filterForm.customerId ? 'filter-customer-name' : 'filter-customer-placeholder'">
                {{ filterForm.customerName || '全部客户' }}
              </text>
              <text v-if="filterForm.customerId" class="filter-customer-clear" @tap.stop="clearCustomer">✕</text>
            </view>
          </view>

          <!-- 支付状态 -->
          <view class="filter-section">
            <text class="filter-section__label">支付状态</text>
            <view class="filter-chips">
              <text v-for="opt in paymentOptions" :key="opt.value"
                class="filter-chip" :class="{ 'filter-chip--active': filterForm.paymentStatus === opt.value }"
                @tap="filterForm.paymentStatus = opt.value">{{ opt.label }}</text>
            </view>
          </view>

          <!-- 排序方式 -->
          <view class="filter-section">
            <text class="filter-section__label">排序方式</text>
            <view class="filter-chips">
              <text v-for="opt in sortOptions" :key="opt.value"
                class="filter-chip" :class="{ 'filter-chip--active': filterForm.sortBy === opt.value }"
                @tap="filterForm.sortBy = opt.value">{{ opt.label }}</text>
            </view>
          </view>
        </scroll-view>

        <view class="filter-sheet__footer">
          <text class="filter-sheet__btn filter-sheet__btn--reset" @tap="resetFilter">重置</text>
          <text class="filter-sheet__btn filter-sheet__btn--confirm" @tap="applyFilter">确定</text>
        </view>
      </view>
    </view>

    <!-- 客户选择弹窗 -->
    <view v-if="showCustomerPicker" class="popup-mask" @tap="showCustomerPicker = false">
      <view class="filter-sheet" @tap.stop>
        <view class="filter-sheet__header">
          <text class="filter-sheet__title">选择客户</text>
          <text class="filter-sheet__close" @tap="showCustomerPicker = false">✕</text>
        </view>
        <view class="customer-search">
          <input class="customer-search__input" v-model="customerKeyword" placeholder="搜索客户" @confirm="loadCustomers" />
        </view>
        <scroll-view scroll-y class="customer-list">
          <view v-for="c in customerList" :key="c.id" class="customer-item" @tap="pickCustomer(c)">
            <text class="customer-item__name">{{ c.name }}</text>
            <text v-if="filterForm.customerId === c.id" class="customer-item__check">✓</text>
          </view>
          <view v-if="customerList.length === 0" class="customer-empty">
            <text class="customer-empty__text">暂无客户</text>
          </view>
        </scroll-view>
      </view>
    </view>
    </template>
  </view>
</template>

<script>
import { getSalesOrderList, getSalesOrderCounts } from '@/api/salesOrder'
import { getCustomerList } from '@/api/customer'
import { getSalesStatusText } from '@/utils/format'
import { guardTabPage, getNoAccessTip } from '@/utils/permission'
import { useUserStore } from '@/store/user'
import dayjs from 'dayjs'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

export default {
  components: { EmptyState, LoadMore },
  data() {
    return {
      statusTabs: [
        { label: '全部', value: '', countKey: 'ALL' },
        { label: '待确认', value: 'PENDING', countKey: 'PENDING' },
        { label: '已确认', value: 'CONFIRMED', countKey: 'CONFIRMED' },
        { label: '已发货', value: 'SHIPPED', countKey: 'SHIPPED' },
        { label: '已完成', value: 'COMPLETED', countKey: 'COMPLETED' }
      ],
      noAccess: false,
      noAccessTip: '',
      selectedStatus: '',
      keyword: '',
      orderList: [],
      pageNum: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      loadMoreStatus: 'more',
      statusCounts: {},
      // 筛选相关
      showFilterPopup: false,
      showCustomerPicker: false,
      customerKeyword: '',
      customerList: [],
      // 已应用的筛选参数
      appliedFilter: {},
      // 筛选表单（弹窗内编辑用）
      filterForm: {
        timeRange: '',
        startDate: '',
        endDate: '',
        amountRange: '',
        customMinAmount: '',
        customMaxAmount: '',
        customerId: null,
        customerName: '',
        paymentStatus: '',
        sortBy: ''
      },
      timeOptions: [
        { label: '不限', value: '' },
        { label: '今天', value: 'today' },
        { label: '近7天', value: '7d' },
        { label: '近30天', value: '30d' },
        { label: '自定义', value: 'custom' }
      ],
      amountOptions: [
        { label: '不限', value: '' },
        { label: '0-500', value: '0-500' },
        { label: '500-2000', value: '500-2000' },
        { label: '2000-10000', value: '2000-10000' },
        { label: '10000以上', value: '10000+' },
        { label: '自定义', value: 'custom' }
      ],
      paymentOptions: [
        { label: '不限', value: '' },
        { label: '未支付', value: 'UNPAID' },
        { label: '已支付', value: 'PAID' }
      ],
      sortOptions: [
        { label: '默认', value: '' },
        { label: '金额升序', value: 'amount_asc' },
        { label: '金额降序', value: 'amount_desc' },
        { label: '时间升序', value: 'time_asc' }
      ]
    }
  },
  computed: {
    hasFullAccess() { return useUserStore().hasFullAccess },
    hasActiveFilter() {
      const f = this.appliedFilter
      return !!(f.startDate || f.endDate || f.minAmount != null || f.maxAmount != null || f.customerId || f.paymentStatus || f.sortBy)
    }
  },
  onShow() {
    const canAccess = guardTabPage('sales')
    this.noAccess = !canAccess
    if (!canAccess) {
      this.noAccessTip = getNoAccessTip('sales')
      return
    }
    this.refresh()
  },
  onPullDownRefresh() {
    this.refresh().finally(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    this.loadMore()
  },
  methods: {
    async refresh() {
      this.pageNum = 1
      this.orderList = []
      await Promise.all([this.loadOrders(), this.loadCounts()])
    },
    async loadOrders() {
      if (this.loading) return
      this.loading = true
      this.loadMoreStatus = 'loading'
      try {
        const params = {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          status: this.selectedStatus || undefined,
          keyword: this.keyword || undefined,
          ...this.appliedFilter
        }
        Object.keys(params).forEach(k => params[k] === undefined && delete params[k])
        const res = await getSalesOrderList(params)
        this.orderList = this.pageNum === 1 ? res.records : [...this.orderList, ...res.records]
        this.total = res.total
        this.loadMoreStatus = this.orderList.length >= this.total ? 'noMore' : 'more'
      } catch (e) {
        this.loadMoreStatus = 'more'
      } finally {
        this.loading = false
      }
    },
    async loadCounts() {
      try {
        const { sortBy, ...filterParams } = this.appliedFilter
        if (this.keyword) filterParams.keyword = this.keyword
        this.statusCounts = await getSalesOrderCounts(filterParams)
      } catch (e) {
        console.warn('[sales/index] 状态统计加载失败（非关键）:', e)
      }
    },
    loadMore() {
      if (this.orderList.length < this.total) {
        this.pageNum++
        this.loadOrders()
      }
    },
    onStatusTap(status) {
      this.selectedStatus = status
      this.pageNum = 1
      this.orderList = []
      this.loadOrders()
    },
    // 搜索
    doSearch() {
      this.pageNum = 1
      this.orderList = []
      this.loadOrders()
      this.loadCounts()
    },
    clearKeyword() {
      this.keyword = ''
      this.doSearch()
    },
    // 日期格式化
    formatShortDate(dateStr) {
      if (!dateStr) return ''
      return dayjs(dateStr).format('MM-DD HH:mm')
    },
    // 时间快捷选项
    onTimeOptionTap(val) {
      this.filterForm.timeRange = val
      if (val !== 'custom') {
        this.filterForm.startDate = ''
        this.filterForm.endDate = ''
      }
    },
    // 金额快捷选项
    onAmountOptionTap(val) {
      this.filterForm.amountRange = val
      if (val !== 'custom') {
        this.filterForm.customMinAmount = ''
        this.filterForm.customMaxAmount = ''
      }
    },
    // 客户选择
    async openCustomerPicker() {
      this.showCustomerPicker = true
      await this.loadCustomers()
    },
    async loadCustomers() {
      try {
        const res = await getCustomerList({ pageNum: 1, pageSize: 100, keyword: this.customerKeyword || undefined })
        this.customerList = res.records || []
      } catch (e) {
        this.customerList = []
      }
    },
    pickCustomer(c) {
      this.filterForm.customerId = c.id
      this.filterForm.customerName = c.name
      this.showCustomerPicker = false
    },
    clearCustomer() {
      this.filterForm.customerId = null
      this.filterForm.customerName = ''
    },
    // 筛选操作
    resetFilter() {
      this.filterForm = {
        timeRange: '',
        startDate: '',
        endDate: '',
        amountRange: '',
        customMinAmount: '',
        customMaxAmount: '',
        customerId: null,
        customerName: '',
        paymentStatus: '',
        sortBy: ''
      }
    },
    applyFilter() {
      const f = this.filterForm
      const params = {}

      // 时间
      if (f.timeRange === 'today') {
        params.startDate = dayjs().format('YYYY-MM-DD')
        params.endDate = dayjs().format('YYYY-MM-DD')
      } else if (f.timeRange === '7d') {
        params.startDate = dayjs().subtract(6, 'day').format('YYYY-MM-DD')
        params.endDate = dayjs().format('YYYY-MM-DD')
      } else if (f.timeRange === '30d') {
        params.startDate = dayjs().subtract(29, 'day').format('YYYY-MM-DD')
        params.endDate = dayjs().format('YYYY-MM-DD')
      } else if (f.timeRange === 'custom') {
        if (f.startDate) params.startDate = f.startDate
        if (f.endDate) params.endDate = f.endDate
      }

      // 金额
      if (f.amountRange === '0-500') {
        params.minAmount = 0; params.maxAmount = 500
      } else if (f.amountRange === '500-2000') {
        params.minAmount = 500; params.maxAmount = 2000
      } else if (f.amountRange === '2000-10000') {
        params.minAmount = 2000; params.maxAmount = 10000
      } else if (f.amountRange === '10000+') {
        params.minAmount = 10000
      } else if (f.amountRange === 'custom') {
        if (f.customMinAmount) params.minAmount = Number(f.customMinAmount)
        if (f.customMaxAmount) params.maxAmount = Number(f.customMaxAmount)
      }

      // 客户
      if (f.customerId) params.customerId = f.customerId

      // 支付状态
      if (f.paymentStatus) params.paymentStatus = f.paymentStatus

      // 排序
      if (f.sortBy) params.sortBy = f.sortBy

      this.appliedFilter = params
      this.showFilterPopup = false
      this.pageNum = 1
      this.orderList = []
      this.loadOrders()
      this.loadCounts()
    },
    getStatusText(status, cancelBy) {
      return getSalesStatusText(status, cancelBy)
    },
    getStatusType(status) {
      const map = { PENDING: 'warning', CONFIRMED: 'primary', SHIPPED: 'purple', COMPLETED: 'success', CANCELLED: 'danger' }
      return map[status] || 'info'
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/sales/detail?id=${id}` })
    },
    goCreateOrder() {
      uni.navigateTo({ url: '/pages/sales/create-order' })
    },
    goCustomerList() {
      uni.navigateTo({ url: '/pages/sales/customer-list' })
    }
  }
}
</script>

<style lang="scss" scoped>
.no-access-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 60rpx;
}
.no-access-icon {
  font-size: 120rpx;
  margin-bottom: 32rpx;
}
.no-access-text {
  font-size: 28rpx;
  color: #999;
  text-align: center;
  line-height: 1.8;
  white-space: pre-line;
}

/* 天穹悬浮头 (Immersive Hero Header) */
.hero-header {
  background: #ffffff;
  padding: 24rpx 24rpx 32rpx;
  position: relative;
  z-index: 10;
}

.hero-header__stat {
  margin-bottom: 24rpx;
  padding: 0 8rpx;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 26rpx;
  color: var(--text-secondary);
  margin-bottom: 8rpx;
  font-weight: 500;
}

.stat-value {
  font-size: 56rpx;
  color: var(--text-primary);
  font-weight: 800;
}

/* 搜索栏重修 */
.search-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.search-bar__input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 0 32rpx;
  height: 72rpx;
  background: var(--bg-page);
  border-radius: var(--radius-full);
  transition: all 0.3s;
}

.search-bar__icon {
  width: 32rpx; height: 32rpx; margin-right: 16rpx; opacity: 0.8;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239ca3af' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='11' cy='11' r='8'%3E%3C/circle%3E%3Cline x1='21' y1='21' x2='16.65' y2='16.65'%3E%3C/line%3E%3C/svg%3E");
  background-size: cover;
}

.search-bar__input {
  flex: 1;
  font-size: 26rpx;
  height: 72rpx;
  color: var(--text-primary);
}

.search-placeholder-white {
  color: var(--text-tertiary);
}

.svg-icon-close {
  width: 32rpx; height: 32rpx; opacity: 0.7;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239ca3af' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cline x1='18' y1='6' x2='6' y2='18'/%3E%3Cline x1='6' y1='6' x2='18' y2='18'/%3E%3C/svg%3E");
  background-size: cover;
}

.search-bar__filter {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72rpx;
  height: 72rpx;
  position: relative;
  background: var(--bg-page);
  border-radius: var(--radius-full);
  transition: all 0.2s;
}

.search-bar__filter:active {
  transform: scale(0.92);
}

.search-bar__filter--active {
  background: var(--brand-primary-light);
  border: 2rpx solid var(--brand-primary);
}

.filter-icon-svg {
  width: 36rpx; height: 36rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%234b5563' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolygon points='22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3'/%3E%3C/svg%3E");
  background-size: cover;
}

.search-bar__filter-badge {
  position: absolute;
  top: 14rpx;
  right: 14rpx;
  width: 14rpx;
  height: 14rpx;
  background: var(--color-danger);
  border-radius: 50%;
  border: 4rpx solid var(--bg-page);
}

/* 状态筛选 */
.status-tabs {
  white-space: nowrap;
}

.status-tab {
  display: inline-flex;
  align-items: center;
  padding: 12rpx 36rpx;
  margin-right: 20rpx;
  border-radius: var(--radius-full);
  background: var(--bg-page);
  transition: all 0.3s;
}

.status-tab--active {
  background: var(--brand-primary-light);
  .status-tab__text { color: var(--brand-primary); font-weight: 700; }
  .status-tab__badge { background: var(--brand-primary); color: #fff; }
}

.status-tab__text {
  font-size: 26rpx;
  color: var(--text-secondary);
  font-weight: 500;
}

.status-tab__badge {
  display: inline-block;
  min-width: 32rpx; height: 32rpx; line-height: 32rpx; text-align: center;
  font-size: 20rpx; font-weight: 700; border-radius: var(--radius-full);
  padding: 0 8rpx; margin-left: 10rpx;
  background: rgba(0,0,0,0.06); color: var(--text-secondary);
}

/* 便当盒操作区 (Bento Box) */
.bento-actions {
  display: flex; gap: 24rpx;
  padding: 12rpx 24rpx 32rpx;
  background: #ffffff;
  border-radius: 0 0 40rpx 40rpx;
  box-shadow: 0 12rpx 32rpx rgba(0,0,0,0.02);
  margin-bottom: 24rpx;
}

.bento-card {
  flex: 1; height: 140rpx; border-radius: 32rpx; background: var(--bg-page);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  transition: transform 0.2s;
}

.bento-card:active {
  transform: scale(0.95);
}

.bento-primary {
  background: var(--brand-primary-light);
  color: var(--brand-primary);
}

.bento-icon-users {
  width: 50rpx; height: 50rpx; margin-bottom: 12rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%234b5563' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2'/%3E%3Ccircle cx='9' cy='7' r='4'/%3E%3Cpath d='M23 21v-2a4 4 0 0 0-3-3.87'/%3E%3Cpath d='M16 3.13a4 4 0 0 1 0 7.75'/%3E%3C/svg%3E");
  background-size: cover;
}

.bento-icon-plus {
  width: 50rpx; height: 50rpx; margin-bottom: 12rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%232979ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cline x1='12' y1='5' x2='12' y2='19'/%3E%3Cline x1='5' y1='12' x2='19' y2='12'/%3E%3C/svg%3E");
  background-size: cover;
}

.bento-text {
  font-size: 26rpx; font-weight: 600;
  color: inherit;
}
.bento-supplier { color: var(--text-primary); }

/* 悬浮孤岛订单列 (Floating Islands) */
.order-list {
  padding: 0 24rpx 80rpx; display: flex; flex-direction: column; gap: 24rpx;
}

.order-island {
  background: var(--bg-card);
  border-radius: 40rpx;
  padding: 32rpx;
  box-shadow: 0 16rpx 48rpx rgba(0,0,0,0.03);
  transition: transform 0.2s;
}

.status-pill {
  display: flex; align-items: center; gap: 12rpx;
}

.status-dot {
  width: 16rpx; height: 16rpx; border-radius: 50%; box-shadow: 0 0 12rpx currentColor;
}
.status-dot--info { color: var(--text-tertiary); background: currentColor; }
.status-dot--primary { color: var(--brand-primary); background: currentColor; }
.status-dot--warning { color: var(--color-warning); background: currentColor; }
.status-dot--success { color: var(--color-success); background: currentColor; }
.status-dot--danger { color: var(--color-danger); background: currentColor; }
.status-dot--purple { color: #9c27b0; background: currentColor; }

.status-text {
  font-size: 26rpx; color: var(--text-secondary); font-weight: 600;
}

.order-card__header { margin-bottom: 24rpx; }
.order-card__no { font-size: 32rpx; font-weight: 800; color: var(--text-primary); letter-spacing: 0.5rpx; }
.order-card__customer { font-size: 28rpx; color: var(--text-primary); margin-bottom: 32rpx; font-weight: 500;}
.order-card__label { color: var(--text-tertiary); margin-right: 8rpx;}
.order-card__amount { font-size: 44rpx; font-weight: 800; color: #7c3aed; letter-spacing: -0.5rpx;}
.order-card__date { font-size: 24rpx; color: var(--text-disabled); font-weight: 500;}

/* 弹窗遮罩 */
.popup-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 999;
}

/* 筛选底部弹窗 */
.filter-sheet {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  max-height: 75vh;
  display: flex;
  flex-direction: column;
  z-index: 1000;
}
.filter-sheet__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 32rpx 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.filter-sheet__title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}
.filter-sheet__close {
  font-size: 36rpx;
  color: #999;
  padding: 8rpx;
}
.filter-sheet__body {
  flex: 1;
  padding: 16rpx 32rpx;
  overflow-y: auto;
}
.filter-section {
  margin-bottom: 28rpx;
}
.filter-section__label {
  display: block;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 16rpx;
  font-weight: 500;
}
.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.filter-chip {
  padding: 14rpx 28rpx;
  border-radius: var(--radius-full);
  font-size: 24rpx;
  color: var(--text-secondary);
  background: var(--bg-page);
}
.filter-chip--active {
  background: var(--brand-primary-light);
  color: var(--brand-primary);
  font-weight: 600;
}
.filter-date-range {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 16rpx;
}
.filter-date-input {
  flex: 1;
  height: 64rpx;
  line-height: 64rpx;
  border: 1rpx solid #e0e0e0;
  border-radius: 12rpx;
  padding: 0 16rpx;
  font-size: 26rpx;
  text-align: center;
  color: #333;
  background: #fff;
}
.filter-custom-range {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 16rpx;
}
.filter-range-input {
  flex: 1;
  height: 64rpx;
  border: 1rpx solid #e0e0e0;
  border-radius: 12rpx;
  padding: 0 16rpx;
  font-size: 26rpx;
  text-align: center;
  box-sizing: border-box;
}
.filter-range-sep {
  font-size: 26rpx;
  color: #999;
}
.filter-customer-pick {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72rpx;
  border: 1rpx solid #e0e0e0;
  border-radius: 12rpx;
  padding: 0 24rpx;
}
.filter-customer-name {
  font-size: 26rpx;
  color: #333;
}
.filter-customer-placeholder {
  font-size: 26rpx;
  color: #999;
}
.filter-customer-clear {
  font-size: 28rpx;
  color: #999;
  padding: 8rpx;
}
.filter-sheet__footer {
  display: flex;
  gap: 24rpx;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #f0f0f0;
}
.filter-sheet__btn {
  flex: 1;
  text-align: center;
  padding: 22rpx 0;
  border-radius: 12rpx;
  font-size: 28rpx;
}
.filter-sheet__btn--reset {
  background: #f5f6fa;
  color: #666;
}
.filter-sheet__btn--confirm {
  background: #2979ff;
  color: #fff;
}

/* 客户选择 */
.customer-search {
  padding: 16rpx 32rpx;
}
.customer-search__input {
  width: 100%;
  height: 68rpx;
  background: #f5f6fa;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 26rpx;
  box-sizing: border-box;
}
.customer-list {
  max-height: 50vh;
  padding: 0 32rpx;
}
.customer-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}
.customer-item__name {
  font-size: 28rpx;
  color: #333;
}
.customer-item__check {
  font-size: 32rpx;
  color: #2979ff;
  font-weight: 600;
}
.customer-empty {
  padding: 60rpx 0;
  text-align: center;
}
.customer-empty__text {
  font-size: 26rpx;
  color: #999;
}
</style>
