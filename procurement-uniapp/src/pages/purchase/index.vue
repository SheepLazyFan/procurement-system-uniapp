<template>
  <view class="page-purchase">
    <!-- 状态筛选 -->
    <scroll-view scroll-x class="status-tabs">
      <view
        v-for="tab in statusTabs"
        :key="tab.value"
        class="status-tab"
        :class="{ 'status-tab--active': selectedStatus === tab.value }"
        @tap="onStatusTap(tab.value)"
      >
        <text class="status-tab__text">{{ tab.label }}</text>
      </view>
    </scroll-view>

    <!-- 订单列表 -->
    <view class="order-list container">
      <view
        v-for="order in orderList"
        :key="order.id"
        class="order-card card"
        @tap="goDetail(order.id)"
      >
        <view class="order-card__header flex-between">
          <text class="order-card__no">{{ order.orderNo }}</text>
          <StatusTag :text="getStatusText(order.status)" :type="getStatusType(order.status)" />
        </view>
        <view class="order-card__supplier">
          <text class="order-card__label">供应商：</text>
          <text>{{ order.supplierName || '-' }}</text>
        </view>
        <view class="order-card__footer flex-between">
          <text class="order-card__amount price-text">¥{{ order.totalAmount }}</text>
          <text class="order-card__date">{{ order.createdAt }}</text>
        </view>
      </view>

      <EmptyState v-if="orderList.length === 0 && !loading" text="暂无采购订单" buttonText="快速采购" @action="goQuickPurchase" />
      <LoadMore v-if="orderList.length > 0" :status="loadMoreStatus" @load="loadMore" />
    </view>

    <!-- 底部操作 -->
    <view class="bottom-bar safe-area-bottom">
      <view class="bottom-btn" @tap="goSupplierList">
        <text class="bottom-btn__text">供应商</text>
      </view>
      <view class="bottom-btn bottom-btn--primary" @tap="goQuickPurchase">
        <text class="bottom-btn__text bottom-btn__text--white">快速采购</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getPurchaseOrderList } from '@/api/purchaseOrder'
import { getPurchaseStatusText } from '@/utils/format'
import StatusTag from '@/components/common/StatusTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

export default {
  components: { StatusTag, EmptyState, LoadMore },
  data() {
    return {
      statusTabs: [
        { label: '全部', value: '' },
        { label: '草稿', value: 'DRAFT' },
        { label: '采购中', value: 'PURCHASING' },
        { label: '已到货', value: 'ARRIVED' },
        { label: '已完成', value: 'COMPLETED' }
      ],
      selectedStatus: '',
      orderList: [],
      pageNum: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      loadMoreStatus: 'more'
    }
  },
  onShow() {
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
      await this.loadOrders()
    },
    async loadOrders() {
      if (this.loading) return
      this.loading = true
      this.loadMoreStatus = 'loading'
      try {
        const res = await getPurchaseOrderList({
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          status: this.selectedStatus || undefined
        })
        this.orderList = this.pageNum === 1 ? res.records : [...this.orderList, ...res.records]
        this.total = res.total
        this.loadMoreStatus = this.orderList.length >= this.total ? 'noMore' : 'more'
      } catch (e) {
        this.loadMoreStatus = 'more'
      } finally {
        this.loading = false
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
      this.refresh()
    },
    getStatusText(status) {
      return getPurchaseStatusText(status)
    },
    getStatusType(status) {
      const map = {
        DRAFT: 'info',
        PURCHASING: 'primary',
        ARRIVED: 'success',
        COMPLETED: 'info',
        CANCELLED: 'danger'
      }
      return map[status] || 'info'
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/purchase/detail?id=${id}` })
    },
    goQuickPurchase() {
      uni.navigateTo({ url: '/pages/purchase/quick-purchase' })
    },
    goSupplierList() {
      uni.navigateTo({ url: '/pages/purchase/supplier-list' })
    }
  }
}
</script>

<style lang="scss" scoped>
.status-tabs {
  white-space: nowrap;
  padding: 16rpx 24rpx;
  background: #fff;
}

.status-tab {
  display: inline-block;
  padding: 12rpx 28rpx;
  margin-right: 16rpx;
  border-radius: 32rpx;
  background: #f5f6fa;

  &--active {
    background: #2979ff;
    .status-tab__text { color: #fff; }
  }

  &__text {
    font-size: 26rpx;
    color: #666;
  }
}

.order-card {
  &__header {
    margin-bottom: 12rpx;
  }

  &__no {
    font-size: 28rpx;
    font-weight: 600;
    color: #333;
  }

  &__supplier {
    font-size: 26rpx;
    color: #666;
    margin-bottom: 12rpx;
  }

  &__label {
    color: #999;
  }

  &__amount {
    font-size: 32rpx;
  }

  &__date {
    font-size: 24rpx;
    color: #999;
  }
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  padding: 16rpx 24rpx;
  background: #fff;
  gap: 24rpx;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.bottom-btn {
  flex: 1;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  background: #f5f6fa;

  &--primary {
    background: #2979ff;
  }

  &__text {
    font-size: 28rpx;
    color: #666;

    &--white {
      color: #fff;
    }
  }
}
</style>
