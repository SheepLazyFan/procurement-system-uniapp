<template>
  <view class="page-inventory">
    <!-- 搜索栏 -->
    <SearchBar v-model="keyword" placeholder="搜索商品" @search="onSearch" @clear="onSearch" />

    <!-- 分类筛选横向滚动 -->
    <scroll-view scroll-x class="category-scroll">
      <view
        v-for="cat in categoryList"
        :key="cat.id"
        class="category-item"
        :class="{ 'category-item--active': selectedCategoryId === cat.id }"
        @tap="onCategoryTap(cat.id)"
      >
        <text class="category-item__text">{{ cat.name }}</text>
      </view>
    </scroll-view>

    <!-- 操作按钮组 -->
    <view class="action-bar">
      <view class="action-btn" @tap="goCategory">
        <text class="action-btn__text">分类管理</text>
      </view>
      <view class="action-btn" @tap="goBatchImport">
        <text class="action-btn__text">批量导入</text>
      </view>
      <view class="action-btn action-btn--primary" @tap="goAddProduct">
        <text class="action-btn__text action-btn__text--white">+ 添加商品</text>
      </view>
    </view>

    <!-- 商品列表 -->
    <view class="product-list">
      <view
        v-for="item in productList"
        :key="item.id"
        class="product-card card"
        @tap="goEditProduct(item.id)"
      >
        <view class="product-card__info">
          <text class="product-card__name">{{ item.name }}</text>
          <text class="product-card__spec">{{ item.spec }} / {{ item.unit }}</text>
          <view class="product-card__row">
            <text class="product-card__price price-text">¥{{ item.price }}</text>
            <text
              class="product-card__stock"
              :class="{ 'stock-warning': item.stock <= item.stockWarning }"
            >
              库存: {{ item.stock }}
            </text>
          </view>
        </view>
      </view>
      <EmptyState v-if="productList.length === 0 && !loading" text="暂无商品" buttonText="添加商品" @action="goAddProduct" />
      <LoadMore v-if="productList.length > 0" :status="loadMoreStatus" @load="loadMore" />
    </view>
  </view>
</template>

<script>
import { getProductList } from '@/api/product'
import { getCategoryList } from '@/api/category'
import SearchBar from '@/components/common/SearchBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

export default {
  components: { SearchBar, EmptyState, LoadMore },
  data() {
    return {
      keyword: '',
      categoryList: [{ id: null, name: '全部' }],
      selectedCategoryId: null,
      productList: [],
      pageNum: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      loadMoreStatus: 'more'
    }
  },
  onShow() {
    this.loadCategories()
    this.refresh()
  },
  onPullDownRefresh() {
    this.refresh().finally(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    this.loadMore()
  },
  methods: {
    async loadCategories() {
      try {
        const list = await getCategoryList()
        this.categoryList = [{ id: null, name: '全部' }, ...list]
      } catch (e) {}
    },
    async refresh() {
      this.pageNum = 1
      this.productList = []
      await this.loadProducts()
    },
    async loadProducts() {
      if (this.loading) return
      this.loading = true
      this.loadMoreStatus = 'loading'
      try {
        const res = await getProductList({
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          categoryId: this.selectedCategoryId,
          keyword: this.keyword || undefined
        })
        this.productList = this.pageNum === 1 ? res.records : [...this.productList, ...res.records]
        this.total = res.total
        this.loadMoreStatus = this.productList.length >= this.total ? 'noMore' : 'more'
      } catch (e) {
        this.loadMoreStatus = 'more'
      } finally {
        this.loading = false
      }
    },
    loadMore() {
      if (this.productList.length < this.total) {
        this.pageNum++
        this.loadProducts()
      }
    },
    onSearch() {
      this.refresh()
    },
    onCategoryTap(id) {
      this.selectedCategoryId = id
      this.refresh()
    },
    goCategory() {
      uni.navigateTo({ url: '/pages/inventory/category' })
    },
    goBatchImport() {
      uni.navigateTo({ url: '/pages/inventory/batch-import' })
    },
    goAddProduct() {
      uni.navigateTo({ url: '/pages/inventory/product-form' })
    },
    goEditProduct(id) {
      uni.navigateTo({ url: `/pages/inventory/product-form?id=${id}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.category-scroll {
  white-space: nowrap;
  padding: 16rpx 24rpx;
  background: #fff;
}

.category-item {
  display: inline-block;
  padding: 12rpx 28rpx;
  margin-right: 16rpx;
  border-radius: 32rpx;
  background: #f5f6fa;

  &--active {
    background: #2979ff;
    .category-item__text { color: #fff; }
  }

  &__text {
    font-size: 26rpx;
    color: #666;
  }
}

.action-bar {
  display: flex;
  padding: 16rpx 24rpx;
  gap: 16rpx;
}

.action-btn {
  flex: 1;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  background: #f5f6fa;

  &--primary {
    background: #2979ff;
  }

  &__text {
    font-size: 26rpx;
    color: #666;

    &--white {
      color: #fff;
    }
  }
}

.product-list {
  padding: 0 24rpx;
}

.product-card {
  &__info {
    flex: 1;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 600;
    color: #333;
    margin-bottom: 8rpx;
  }

  &__spec {
    font-size: 24rpx;
    color: #999;
    margin-bottom: 12rpx;
  }

  &__row {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__price {
    font-size: 32rpx;
  }

  &__stock {
    font-size: 24rpx;
    color: #999;
  }
}

.stock-warning {
  color: #f3a73f;
  font-weight: 600;
}
</style>
