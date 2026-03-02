<template>
  <view class="page-customer-list">
    <SearchBar v-model="keyword" placeholder="搜索客户" @search="onSearch" @clear="onSearch" />

    <view class="customer-list container">
      <view
        v-for="item in customerList"
        :key="item.id"
        class="customer-card card"
        @tap="handleTap(item)"
      >
        <view class="customer-card__header">
          <text class="customer-card__name">{{ item.name }}</text>
        </view>
        <view class="customer-card__info">
          <text class="customer-card__phone">📞 {{ item.phone || '-' }}</text>
        </view>
        <view class="customer-card__stats">
          <text class="customer-card__stat">下单 {{ item.orderCount || 0 }} 次</text>
          <text class="customer-card__stat">累计 ¥{{ item.totalAmount || 0 }}</text>
        </view>
      </view>

      <EmptyState v-if="customerList.length === 0 && !loading" text="暂无客户" buttonText="添加客户" @action="goAdd" />
      <LoadMore v-if="customerList.length > 0" :status="loadMoreStatus" @load="loadMore" />
    </view>

    <view class="fab" @tap="goAdd">
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
.customer-card {
  &__header { margin-bottom: 8rpx; }
  &__name { font-size: 30rpx; font-weight: 600; color: #333; }
  &__phone { font-size: 26rpx; color: #666; margin-bottom: 8rpx; }
  &__stats { display: flex; gap: 32rpx; }
  &__stat { font-size: 24rpx; color: #999; }
}

.fab {
  position: fixed;
  right: 32rpx;
  bottom: 160rpx;
  width: 96rpx;
  height: 96rpx;
  background: #2979ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(41, 121, 255, 0.4);

  &__text { font-size: 48rpx; color: #fff; }
}
</style>
