<template>
  <view class="page-customer-list">
    <SearchBar v-model="keyword" placeholder="搜索客户" @search="onSearch" @clear="onSearch" />

    <view class="customer-list container">
      <view
        v-for="(item, index) in customerList"
        :key="item.id"
        class="customer-card saas-card animate-fade-up"
        :style="{ 'animation-delay': (index % 10) * 0.05 + 's' }"
        hover-class="saas-card-push"
        :hover-start-time="0"
        :hover-stay-time="100"
        @tap="handleTap(item)"
      >
        <view class="customer-card__header">
          <text class="customer-card__name">{{ item.name }}</text>
        </view>
        <view class="customer-card__info">
          <view class="customer-card__phone-icon"></view>
          <text class="customer-card__phone">{{ item.phone || '-' }}</text>
        </view>
        <view class="customer-card__stats">
          <text class="customer-card__stat">下单 {{ item.orderCount || 0 }} 次</text>
          <text class="customer-card__stat">累计 <text class="num-font highlight">¥{{ item.totalAmount || 0 }}</text></text>
        </view>
      </view>

      <EmptyState v-if="customerList.length === 0 && !loading" text="暂无客户" buttonText="添加客户" @action="goAdd" />
      <LoadMore v-if="customerList.length > 0" :status="loadMoreStatus" @load="loadMore" />
    </view>

    <view class="fab" @tap="goAdd" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
      <text class="fab__text">+</text>
    </view>
  </view>
</template>

<script>
import { getCustomerList } from '@/api/customer'
import SearchBar from '@/components/common/SearchBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

export default {
  components: { SearchBar, EmptyState, LoadMore },
  data() {
    return {
      keyword: '',
      isSelectMode: false,
      customerList: [],
      pageNum: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      loadMoreStatus: 'more'
    }
  },
  onLoad(query) {
    this.isSelectMode = query.select === '1'
  },
  onShow() {
    this.refresh()
  },
  methods: {
    async refresh() {
      this.pageNum = 1
      this.customerList = []
      await this.loadList()
    },
    async loadList() {
      if (this.loading) return
      this.loading = true
      this.loadMoreStatus = 'loading'
      try {
        const res = await getCustomerList({
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          keyword: this.keyword || undefined
        })
        this.customerList = this.pageNum === 1 ? res.records : [...this.customerList, ...res.records]
        this.total = res.total
        this.loadMoreStatus = this.customerList.length >= this.total ? 'noMore' : 'more'
      } catch (e) {
        this.loadMoreStatus = 'more'
      } finally {
        this.loading = false
      }
    },
    loadMore() {
      if (this.customerList.length < this.total) {
        this.pageNum++
        this.loadList()
      }
    },
    onSearch() {
      this.refresh()
    },
    handleTap(item) {
      if (this.isSelectMode) {
        // 选择客户后返回上一页
        const pages = getCurrentPages()
        const prevPage = pages[pages.length - 2]
        if (prevPage) {
          prevPage.$vm.selectedCustomer = item
        }
        uni.navigateBack()
      } else {
        uni.navigateTo({ url: `/pages/sales/customer-detail?id=${item.id}` })
      }
    },
    goAdd() {
      uni.navigateTo({ url: '/pages/sales/add-customer' })
    }
  }
}
</script>

<style lang="scss" scoped>
.page-customer-list {
  padding-bottom: calc(180rpx + env(safe-area-inset-bottom));
}

.customer-card {
  &__header { margin-bottom: 8rpx; }
  &__name { font-size: 32rpx; font-weight: 600; color: var(--text-primary); }
  &__info { 
    display: flex;
    align-items: center;
    margin-bottom: 12rpx; 
  }
  &__phone-icon {
    width: 26rpx;
    height: 26rpx;
    margin-right: 8rpx;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23C0C4CC' stroke-width='2.2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z'%3E%3C/path%3E%3C/svg%3E");
    background-size: cover;
  }
  &__phone { font-size: 26rpx; color: var(--text-secondary); font-family: var(--font-number); }
  &__stats { display: flex; gap: 32rpx; align-items: baseline; }
  &__stat { font-size: 24rpx; color: var(--text-tertiary); }
  
  .highlight {
    color: var(--color-danger);
    font-size: 30rpx;
    font-weight: 700;
    margin-left: 8rpx;
  }
}

.fab {
  position: fixed;
  right: 32rpx;
  bottom: calc(120rpx + env(safe-area-inset-bottom));
  width: 104rpx;
  height: 104rpx;
  background: var(--brand-primary);
  border-radius: 52rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(41, 121, 255, 0.4);
  z-index: 100;

  &__text { font-size: 56rpx; color: #fff; font-weight: 300; line-height: 1; margin-bottom: 8rpx; }
}
</style>
