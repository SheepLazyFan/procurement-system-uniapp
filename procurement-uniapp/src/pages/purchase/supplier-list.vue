<template>
  <view class="page-supplier-list">
    <SearchBar v-model="keyword" placeholder="搜索供应商" @search="onSearch" @clear="onSearch" />

    <view class="supplier-list container">
      <view
        v-for="item in supplierList"
        :key="item.id"
        class="supplier-card card"
        @tap="goDetail(item.id)"
      >
        <view class="supplier-card__header">
          <text class="supplier-card__name">{{ item.name }}</text>
          <text class="supplier-card__category">{{ item.mainCategory || '' }}</text>
        </view>
        <view class="supplier-card__info">
          <text class="supplier-card__phone">📞 {{ item.phone || '-' }}</text>
        </view>
        <view class="supplier-card__stats">
          <text class="supplier-card__stat">采购 {{ item.purchaseCount || 0 }} 次</text>
          <text class="supplier-card__stat">累计 ¥{{ item.totalAmount || 0 }}</text>
        </view>
      </view>

      <EmptyState v-if="supplierList.length === 0 && !loading" text="暂无供应商" buttonText="添加供应商" @action="goAdd" />
      <LoadMore v-if="supplierList.length > 0" :status="loadMoreStatus" @load="loadMore" />
    </view>

    <!-- 添加按钮 -->
    <view class="fab" @tap="goAdd">
      <text class="fab__text">+</text>
    </view>
  </view>
</template>

<script>
import { getSupplierList } from '@/api/supplier'
import SearchBar from '@/components/common/SearchBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

export default {
  components: { SearchBar, EmptyState, LoadMore },
  data() {
    return {
      keyword: '',
      supplierList: [],
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
  methods: {
    async refresh() {
      this.pageNum = 1
      this.supplierList = []
      await this.loadList()
    },
    async loadList() {
      if (this.loading) return
      this.loading = true
      this.loadMoreStatus = 'loading'
      try {
        const res = await getSupplierList({
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          keyword: this.keyword || undefined
        })
        this.supplierList = this.pageNum === 1 ? res.records : [...this.supplierList, ...res.records]
        this.total = res.total
        this.loadMoreStatus = this.supplierList.length >= this.total ? 'noMore' : 'more'
      } catch (e) {
        this.loadMoreStatus = 'more'
      } finally {
        this.loading = false
      }
    },
    loadMore() {
      if (this.supplierList.length < this.total) {
        this.pageNum++
        this.loadList()
      }
    },
    onSearch() {
      this.refresh()
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/purchase/supplier-detail?id=${id}` })
    },
    goAdd() {
      // 使用 modal 快捷添加，Phase 3 可完善为独立页面
      uni.showToast({ title: '添加供应商功能待完善', icon: 'none' })
    }
  }
}
</script>

<style lang="scss" scoped>
.supplier-card {
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8rpx;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
  }

  &__category {
    font-size: 24rpx;
    color: #2979ff;
    background: #e8f0fe;
    padding: 4rpx 12rpx;
    border-radius: 8rpx;
  }

  &__phone {
    font-size: 26rpx;
    color: #666;
    margin-bottom: 8rpx;
  }

  &__stats {
    display: flex;
    gap: 32rpx;
  }

  &__stat {
    font-size: 24rpx;
    color: #999;
  }
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

  &__text {
    font-size: 48rpx;
    color: #fff;
  }
}
</style>
