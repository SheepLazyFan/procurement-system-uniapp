<template>
  <view class="page-store container">
    <!-- 店铺信息 -->
    <view class="store-header card" v-if="storeInfo.name">
      <text class="store-name">{{ storeInfo.name }}</text>
      <text class="store-desc">{{ storeInfo.address || '' }}</text>
    </view>

    <!-- 搜索 -->
    <SearchBar v-model="keyword" placeholder="搜索商品" @search="handleSearch" @clear="handleClear" />

    <!-- 商品列表 -->
    <view class="product-grid">
      <view v-for="item in productList" :key="item.id" class="product-card card" @tap="goDetail(item.id)">
        <image v-if="item.mainImage" :src="item.mainImage" class="product-img" mode="aspectFill" />
        <view v-else class="product-img-placeholder">
          <text>📦</text>
        </view>
        <view class="product-info">
          <text class="product-name">{{ item.name }}</text>
          <text class="product-spec" v-if="item.spec">{{ item.spec }}</text>
          <view class="product-bottom">
            <text class="product-price">¥{{ item.price }}</text>
            <text class="product-unit">/{{ item.unit || '件' }}</text>
          </view>
        </view>
      </view>
    </view>

    <EmptyState v-if="!productList.length && !loading" text="暂无商品" icon="🛒" />
    <LoadMore v-if="productList.length" :status="loadStatus" @loadMore="loadMore" />
  </view>
</template>

<script>
import SearchBar from '@/components/common/SearchBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'
import { getStoreInfo, getStoreProducts } from '@/api/buyer'

export default {
  components: { SearchBar, EmptyState, LoadMore },
  data() {
    return {
      enterpriseId: '',
      storeInfo: {},
      keyword: '',
      productList: [],
      page: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      loadStatus: 'more'
    }
  },
  onLoad(query) {
    this.enterpriseId = query.enterpriseId || ''
    if (this.enterpriseId) {
      this.loadStoreInfo()
      this.loadProducts()
    }
  },
  onPullDownRefresh() {
    this.page = 1
    this.loadProducts().finally(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    if (this.loadStatus === 'more') this.loadMore()
  },
  methods: {
    async loadStoreInfo() {
      try {
        const res = await getStoreInfo(this.enterpriseId)
        this.storeInfo = res.data || {}
      } catch (e) {
        console.error('加载店铺信息失败', e)
      }
    },
    async loadProducts() {
      this.loading = true
      try {
        const res = await getStoreProducts(this.enterpriseId, {
          keyword: this.keyword,
          page: this.page,
          pageSize: this.pageSize
        })
        const list = res.data?.records || res.data || []
        this.total = res.data?.total || list.length
        if (this.page === 1) {
          this.productList = list
        } else {
          this.productList = [...this.productList, ...list]
        }
        this.loadStatus = this.productList.length >= this.total ? 'noMore' : 'more'
      } catch (e) {
        console.error('加载商品失败', e)
      } finally {
        this.loading = false
      }
    },
    loadMore() {
      this.page++
      this.loadProducts()
    },
    handleSearch() {
      this.page = 1
      this.loadProducts()
    },
    handleClear() {
      this.keyword = ''
      this.page = 1
      this.loadProducts()
    },
    goDetail(id) {
      uni.navigateTo({ url: `/pages/buyer/product-detail?enterpriseId=${this.enterpriseId}&productId=${id}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.store-header {
  text-align: center;
}
.store-name {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #333;
  margin-bottom: 8rpx;
}
.store-desc {
  font-size: 24rpx;
  color: #999;
}
.product-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.product-card {
  width: calc(50% - 8rpx);
  padding: 0;
  overflow: hidden;
  border-radius: 16rpx;
}
.product-img {
  width: 100%;
  height: 300rpx;
}
.product-img-placeholder {
  width: 100%;
  height: 300rpx;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
}
.product-info {
  padding: 16rpx;
}
.product-name {
  display: block;
  font-size: 26rpx;
  color: #333;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-spec {
  display: block;
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}
.product-bottom {
  display: flex;
  align-items: baseline;
  margin-top: 8rpx;
}
.product-price {
  font-size: 32rpx;
  color: #ff4d4f;
  font-weight: 700;
}
.product-unit {
  font-size: 22rpx;
  color: #999;
}
</style>
