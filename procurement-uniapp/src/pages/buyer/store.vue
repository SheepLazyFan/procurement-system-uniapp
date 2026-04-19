<template>
  <view class="page-store">
    <!-- ========== 店铺 Header ========== -->
    <view class="store-hero">
      <view class="store-hero__inner" v-if="storeInfo.enterpriseName">
        <!-- 店铺名 + 地址 -->
        <view class="store-identity">
          <view class="store-avatar">
            <view class="svg-icon-store"></view>
          </view>
          <view class="store-text">
            <text class="store-name">{{ storeInfo.enterpriseName }}</text>
            <text class="store-address" v-if="storeInfo.address">{{ storeInfo.address }}</text>
          </view>
        </view>
        <!-- 快捷入口 -->
        <view class="store-shortcuts">
          <view class="shortcut-item" hover-class="shortcut-item--hover" @tap="goCart">
            <view class="shortcut-icon-wrap">
              <view class="svg-icon-cart"></view>
              <text v-if="cartCount > 0" class="shortcut-badge">{{ cartCount > 99 ? '99+' : cartCount }}</text>
            </view>
            <text class="shortcut-label">购物车</text>
          </view>
          <view class="shortcut-item" hover-class="shortcut-item--hover" @tap="goOrders">
            <view class="shortcut-icon-wrap">
              <view class="svg-icon-orders"></view>
            </view>
            <text class="shortcut-label">我的订单</text>
          </view>
        </view>
      </view>

      <!-- 搜索栏 -->
      <view class="store-search">
        <view class="store-search__bar">
          <SearchBar v-model="keyword" placeholder="搜索商品" @search="handleSearch" @clear="handleClear" />
        </view>
        <view class="filter-trigger" :class="{ 'filter-trigger--active': filterActive }" hover-class="filter-trigger--hover" @tap="openFilter">
          <view class="svg-icon-filter-sm"></view>
          <text class="filter-trigger__text">筛选</text>
        </view>
      </view>
    </view>

    <!-- ========== 分类横滚 ========== -->
    <view class="category-bar">
      <scroll-view scroll-x class="category-bar__scroll" :show-scrollbar="false">
        <view class="category-bar__inner">
          <view
            v-for="cat in enrichedCategoryList"
            :key="String(cat.id)"
            class="cat-pill"
            :class="{ 'cat-pill--active': selectedCategoryId === cat.id }"
            @tap="onCategoryChange(cat.id)"
          >
            <text class="cat-pill__name">{{ (cat.name || '').replace(/\s+/g, '') }}</text>
            <text v-if="cat.productCount > 0" class="cat-pill__count">{{ cat.productCount }}</text>
          </view>
        </view>
      </scroll-view>
      <view v-if="categoryList.length > 4" class="category-bar__expand" @tap="showCategoryPopup = true">
        <view class="svg-icon-chevron-down"></view>
      </view>
    </view>

    <!-- 分类全览弹窗 -->
    <view v-if="showCategoryPopup" class="category-popup-mask" @tap="showCategoryPopup = false">
      <view class="category-popup" @tap.stop>
        <view class="category-popup__header">
          <text class="category-popup__title">所有分类</text>
          <text class="category-popup__close" @tap="showCategoryPopup = false">✕</text>
        </view>
        <scroll-view scroll-y class="category-popup__body">
          <view class="category-popup__grid">
            <view
              v-for="cat in enrichedCategoryList"
              :key="'popup-' + String(cat.id)"
              class="category-popup__item"
              :class="{ 'category-popup__item--active': selectedCategoryId === cat.id }"
              @tap="onCategoryTapPopup(cat.id)"
            >
              <text class="category-popup__item-name">{{ (cat.name || '').replace(/\s+/g, '') }}</text>
              <text v-if="cat.productCount > 0" class="category-popup__item-count">{{ cat.productCount }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- ========== 商品网格 ========== -->
    <view class="product-grid container">
      <view v-for="item in productList" :key="item.id" class="product-card" hover-class="product-card--hover" @tap="goDetail(item.id)">
        <view class="product-card__img-wrap">
          <image v-if="item.mainImage" :src="$fileUrl(item.mainImage)" class="product-card__img" mode="aspectFill" />
          <view v-else class="product-card__placeholder">
            <view class="svg-icon-package"></view>
          </view>
          <!-- 库存标签 -->
          <view v-if="item.stockStatus === 'OUT_OF_STOCK'" class="stock-badge stock-badge--out">缺货</view>
          <view v-else-if="item.stockStatus === 'LOW_STOCK'" class="stock-badge stock-badge--low">仅剩{{ item.stock }}件</view>
        </view>
        <view class="product-card__body">
          <text class="product-card__name">{{ item.name }}</text>
          <text class="product-card__spec" v-if="item.spec">{{ item.spec }}</text>
          <view class="product-card__price-row">
            <text class="product-card__price">¥{{ item.price }}</text>
            <text class="product-card__unit">/{{ item.unit || '件' }}</text>
          </view>
        </view>
      </view>
    </view>

    <EmptyState v-if="!productList.length && !loading" text="暂无商品" icon="🛒" />
    <LoadMore v-if="productList.length" :status="loadStatus" @loadMore="loadMore" />

    <!-- ========== 筛选面板 ========== -->
    <view v-if="filterVisible" class="filter-overlay">
      <view class="filter-mask" @tap="closeFilter" />
      <view class="filter-panel">
        <view class="filter-section">
          <text class="filter-section__title">库存状态</text>
          <view class="filter-chips">
            <view class="filter-chip" :class="{ 'filter-chip--active': draftStock === null }" @tap="draftStock = null">全部</view>
            <view class="filter-chip" :class="{ 'filter-chip--active': draftStock === 'IN_STOCK' }" @tap="draftStock = 'IN_STOCK'">有货</view>
            <view class="filter-chip" :class="{ 'filter-chip--active': draftStock === 'OUT_OF_STOCK' }" @tap="draftStock = 'OUT_OF_STOCK'">缺货</view>
          </view>
        </view>
        <view class="filter-section">
          <text class="filter-section__title">价格排序</text>
          <view class="filter-chips">
            <view class="filter-chip" :class="{ 'filter-chip--active': draftSort === null }" @tap="draftSort = null">默认</view>
            <view class="filter-chip" :class="{ 'filter-chip--active': draftSort === 'price_asc' }" @tap="draftSort = 'price_asc'">低 → 高</view>
            <view class="filter-chip" :class="{ 'filter-chip--active': draftSort === 'price_desc' }" @tap="draftSort = 'price_desc'">高 → 低</view>
          </view>
        </view>
        <view class="filter-section">
          <text class="filter-section__title">价格区间</text>
          <view class="filter-price-row">
            <input class="filter-price-input" type="digit" :value="draftPriceMin" @input="draftPriceMin = $event.detail.value" placeholder="最低价 ¥" placeholder-class="filter-price-ph" />
            <text class="filter-price-sep">～</text>
            <input class="filter-price-input" type="digit" :value="draftPriceMax" @input="draftPriceMax = $event.detail.value" placeholder="最高价 ¥" placeholder-class="filter-price-ph" />
          </view>
        </view>
        <view class="filter-footer">
          <view class="filter-reset" @tap="resetDraft">重置</view>
          <view class="filter-confirm" @tap="applyFilter">确定</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import SearchBar from '@/components/common/SearchBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'
import { getStoreInfo, getStoreProducts, getStoreCategories } from '@/api/buyer'
import { useCartStore } from '@/store/cart'

export default {
  components: { SearchBar, EmptyState, LoadMore },
  computed: {
    cartCount() {
      const cart = useCartStore()
      return cart.enterpriseId === this.enterpriseId ? cart.totalCount : 0
    },
    filterActive() {
      return this.filterStock !== null ||
        this.filterSort !== null ||
        this.filterPriceMin !== '' ||
        this.filterPriceMax !== ''
    },
    enrichedCategoryList() {
      return this.categoryList.map(cat => {
        if (cat.id === null) {
          const count = this.filterActive
            ? this.categoryFilteredTotal
            : (this.storeInfo.productCount || 0)
          return { ...cat, productCount: count }
        }
        return cat
      })
    }
  },
  data() {
    return {
      enterpriseId: '',
      storeInfo: {},
      keyword: '',
      categoryList: [{ id: null, name: '全部' }],
      selectedCategoryId: null,
      showCategoryPopup: false,
      productList: [],
      page: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      loadStatus: 'more',
      categoryFilteredTotal: 0,
      filterVisible: false,
      draftStock: null,
      draftSort: null,
      draftPriceMin: '',
      draftPriceMax: '',
      filterStock: 'IN_STOCK',
      filterSort: null,
      filterPriceMin: '',
      filterPriceMax: ''
    }
  },
  onShow() {
    if (typeof wx !== 'undefined' && typeof wx.hideHomeButton === 'function') {
      wx.hideHomeButton()
    }
  },
  onLoad(query) {
    this.enterpriseId = query.enterpriseId || ''
    if (this.enterpriseId) {
      this.loadStoreInfo()
      this.loadCategories(this.filterStock)
      this.loadProducts()
    }
  },
  onShareAppMessage() {
    return {
      title: `${this.storeInfo.enterpriseName || '商家'}的店铺`,
      path: `/pages/buyer/store?enterpriseId=${this.enterpriseId}`
    }
  },
  onShareTimeline() {
    return {
      title: `${this.storeInfo.enterpriseName || '商家'}的店铺`,
      query: `enterpriseId=${this.enterpriseId}`
    }
  },
  onPullDownRefresh() {
    this.page = 1
    this.loadCategories(this.filterStock, this.filterPriceMin, this.filterPriceMax)
    this.loadProducts().finally(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    if (this.loadStatus === 'more') this.loadMore()
  },
  methods: {
    async loadStoreInfo() {
      try {
        const res = await getStoreInfo(this.enterpriseId)
        this.storeInfo = res || {}
      } catch (e) {
        console.error('加载店铺信息失败', e)
      }
    },
    async loadCategories(stock = null, priceMin = '', priceMax = '') {
      try {
        const params = {}
        if (stock) params.stockStatus = stock
        if (priceMin !== '') params.priceMin = priceMin
        if (priceMax !== '') params.priceMax = priceMax
        const list = await getStoreCategories(this.enterpriseId, params)
        this.categoryList = [{ id: null, name: '全部' }, ...(list || [])]
      } catch (e) {
        console.error('加载分类失败', e)
      }
    },
    onCategoryChange(id) {
      this.selectedCategoryId = id
      this.page = 1
      this.productList = []
      this.loadProducts()
    },
    onCategoryTapPopup(id) {
      this.selectedCategoryId = id
      this.showCategoryPopup = false
      this.page = 1
      this.productList = []
      this.loadProducts()
    },
    async loadProducts() {
      this.loading = true
      try {
        const params = {
          keyword: this.keyword,
          pageNum: this.page,
          pageSize: this.pageSize
        }
        if (this.selectedCategoryId) params.categoryId = this.selectedCategoryId
        if (this.filterStock) params.stockStatus = this.filterStock
        if (this.filterSort) params.sortBy = this.filterSort
        if (this.filterPriceMin !== '') params.priceMin = this.filterPriceMin
        if (this.filterPriceMax !== '') params.priceMax = this.filterPriceMax
        const res = await getStoreProducts(this.enterpriseId, params)
        const list = res?.records || res || []
        const total = res?.total ?? list.length
        this.total = total
        if (this.page === 1 && !this.selectedCategoryId) {
          this.categoryFilteredTotal = total
        }
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
    },
    goOrders() {
      uni.navigateTo({ url: '/pages/buyer/orders' })
    },
    goCart() {
      uni.navigateTo({ url: `/pages/buyer/cart?enterpriseId=${this.enterpriseId}` })
    },
    openFilter() {
      this.draftStock = this.filterStock
      this.draftSort = this.filterSort
      this.draftPriceMin = this.filterPriceMin
      this.draftPriceMax = this.filterPriceMax
      this.filterVisible = true
    },
    closeFilter() {
      this.filterVisible = false
    },
    resetDraft() {
      this.draftStock = null
      this.draftSort = null
      this.draftPriceMin = ''
      this.draftPriceMax = ''
    },
    applyFilter() {
      const min = parseFloat(this.draftPriceMin)
      const max = parseFloat(this.draftPriceMax)
      if (this.draftPriceMin !== '' && isNaN(min)) {
        uni.showToast({ title: '最低价格格式不正确', icon: 'none' })
        return
      }
      if (this.draftPriceMax !== '' && isNaN(max)) {
        uni.showToast({ title: '最高价格格式不正确', icon: 'none' })
        return
      }
      if (this.draftPriceMin !== '' && this.draftPriceMax !== '' && min > max) {
        uni.showToast({ title: '最低价不能高于最高价', icon: 'none' })
        return
      }
      this.filterStock = this.draftStock
      this.filterSort = this.draftSort
      this.filterPriceMin = this.draftPriceMin
      this.filterPriceMax = this.draftPriceMax
      this.filterVisible = false
      this.page = 1
      this.productList = []
      this.loadCategories(this.filterStock, this.filterPriceMin, this.filterPriceMax)
      this.loadProducts()
    }
  }
}
</script>

<style lang="scss" scoped>
/* ==================================================
   SVG 图标（CSS 手绘，与商家端统一风格）
   ================================================== */
.svg-icon-store,
.svg-icon-cart,
.svg-icon-orders,
.svg-icon-filter-sm,
.svg-icon-package {
  display: inline-block;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.svg-icon-store {
  width: 40rpx; height: 40rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23fff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z'%3E%3C/path%3E%3Cpolyline points='9 22 9 12 15 12 15 22'%3E%3C/polyline%3E%3C/svg%3E");
}
.svg-icon-cart {
  width: 36rpx; height: 36rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23555' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='9' cy='21' r='1'%3E%3C/circle%3E%3Ccircle cx='20' cy='21' r='1'%3E%3C/circle%3E%3Cpath d='M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6'%3E%3C/path%3E%3C/svg%3E");
}
.svg-icon-orders {
  width: 36rpx; height: 36rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23555' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z'%3E%3C/path%3E%3Cpolyline points='14 2 14 8 20 8'%3E%3C/polyline%3E%3Cline x1='16' y1='13' x2='8' y2='13'%3E%3C/line%3E%3Cline x1='16' y1='17' x2='8' y2='17'%3E%3C/line%3E%3Cpolyline points='10 9 9 9 8 9'%3E%3C/polyline%3E%3C/svg%3E");
}
.svg-icon-filter-sm {
  width: 28rpx; height: 28rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23666' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolygon points='22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3'%3E%3C/polygon%3E%3C/svg%3E");
}
.svg-icon-package {
  width: 64rpx; height: 64rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ccc' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cline x1='16.5' y1='9.4' x2='7.5' y2='4.21'%3E%3C/line%3E%3Cpath d='M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z'%3E%3C/path%3E%3Cpolyline points='3.27 6.96 12 12.01 20.73 6.96'%3E%3C/polyline%3E%3Cline x1='12' y1='22.08' x2='12' y2='12'%3E%3C/line%3E%3C/svg%3E");
}

/* ==================================================
   店铺 Hero Header
   ================================================== */
.store-hero {
  background: #f7f8fa;
  padding: 0 0 20rpx;
}
.store-hero__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 32rpx 24rpx;
}
.store-identity {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}
.store-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #2979ff, #448aff);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4rpx 16rpx rgba(41, 121, 255, 0.25);
}
.store-text {
  margin-left: 20rpx;
  flex: 1;
  min-width: 0;
}
.store-name {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.store-address {
  display: block;
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 快捷入口 */
.store-shortcuts {
  display: flex;
  gap: 24rpx;
  flex-shrink: 0;
}
.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8rpx 12rpx;
  border-radius: 16rpx;
  transition: background 0.15s;
}
.shortcut-item--hover {
  background: rgba(0, 0, 0, 0.04);
}
.shortcut-icon-wrap {
  position: relative;
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.shortcut-badge {
  position: absolute;
  top: -8rpx;
  right: -16rpx;
  min-width: 28rpx;
  height: 28rpx;
  line-height: 28rpx;
  padding: 0 6rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 18rpx;
  border-radius: 14rpx;
  text-align: center;
  font-weight: 600;
}
.shortcut-label {
  font-size: 20rpx;
  color: #888;
  margin-top: 4rpx;
}

/* 搜索栏 */
.store-search {
  display: flex;
  align-items: center;
  padding: 0 24rpx;
}
.store-search__bar {
  flex: 1;
  min-width: 0;
}
.filter-trigger {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 12rpx 24rpx;
  margin-left: 12rpx;
  border-radius: 32rpx;
  background: #fff;
  border: 1rpx solid #e8e8e8;
}
.filter-trigger--active {
  background: #2979ff;
  border-color: #2979ff;
  .filter-trigger__text { color: #fff; }
  .svg-icon-filter-sm {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23fff' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolygon points='22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3'%3E%3C/polygon%3E%3C/svg%3E");
  }
}
.filter-trigger--hover {
  opacity: 0.85;
}
.filter-trigger__text {
  font-size: 24rpx;
  color: #666;
  white-space: nowrap;
}

/* ==================================================
   分类横滚
   ================================================== */
.category-bar {
  position: relative;
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
  height: 80rpx;
}
.category-bar__scroll {
  flex: 1;
  height: 100%;
  white-space: nowrap;
}
.category-bar__inner {
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  display: inline-flex;
  align-items: center;
  height: 100%;
  padding: 0 100rpx 0 24rpx;
}
.cat-pill {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  padding: 10rpx 28rpx;
  margin-right: 14rpx;
  border-radius: 32rpx;
  background: #f5f6fa;
  transition: all 0.2s;

  &--active {
    background: #2979ff;
    .cat-pill__name { color: #fff; }
    .cat-pill__count { color: rgba(255,255,255,0.8); background: rgba(255,255,255,0.2); }
  }
}
.cat-pill__name {
  font-size: 26rpx;
  color: #555;
  white-space: nowrap;
  font-weight: 500;
}
.cat-pill__count {
  font-size: 20rpx;
  color: #999;
  margin-left: 6rpx;
  background: rgba(0,0,0,0.05);
  border-radius: 20rpx;
  padding: 0 10rpx;
  line-height: 32rpx;
  white-space: nowrap;
}

/* 分类展开按钮 */
.category-bar__expand {
  position: absolute;
  right: 0;
  top: 0;
  width: 88rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(90deg, rgba(255,255,255,0) 0%, rgba(255,255,255,1) 35%, #fff 100%);
  z-index: 2;
}
.svg-icon-chevron-down {
  width: 32rpx; height: 32rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23999' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

/* 分类全览弹窗 */
.category-popup-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
}
.category-popup {
  width: 100%;
  max-height: 70vh;
  background: #fff;
  border-radius: 28rpx 28rpx 0 0;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.25s ease both;
}
.category-popup__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 32rpx 20rpx;
}
.category-popup__title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1a1a1a;
}
.category-popup__close {
  font-size: 36rpx;
  color: #ccc;
  padding: 10rpx;
}
.category-popup__body {
  flex: 1;
  width: 100%;
  box-sizing: border-box;
  padding: 0 32rpx 60rpx;
}
.category-popup__grid {
  display: flex;
  flex-wrap: wrap;
  padding-top: 10rpx;
}
.category-popup__item {
  display: inline-flex;
  align-items: center;
  height: 64rpx;
  padding: 0 28rpx;
  border-radius: 32rpx;
  background: #f5f6fa;
  margin-right: 16rpx;
  margin-bottom: 16rpx;
  border: 1.5rpx solid transparent;
  transition: all 0.2s;
  &--active {
    background: #eef4ff;
    border-color: #2979ff;
  }
}
.category-popup__item-name {
  font-size: 26rpx;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
}
.category-popup__item--active .category-popup__item-name { color: #2979ff; }
.category-popup__item-count {
  font-size: 20rpx;
  color: #999;
  margin-left: 6rpx;
}
.category-popup__item--active .category-popup__item-count { color: #2979ff; }

/* ==================================================
   商品网格
   ================================================== */
.product-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  padding-top: 16rpx;
  padding-bottom: 16rpx;
}
.product-card {
  width: calc(50% - 8rpx);
  box-sizing: border-box;
  overflow: hidden;
  border-radius: 20rpx;
  background: #fff;
  box-shadow: 0 2rpx 16rpx rgba(0, 0, 0, 0.05);
  transition: transform 0.15s, box-shadow 0.15s;
  margin-bottom: 16rpx;
}
.product-card--hover {
  transform: scale(0.97);
  box-shadow: 0 1rpx 8rpx rgba(0, 0, 0, 0.08);
}

.product-card__img-wrap {
  position: relative;
  width: 100%;
  height: 300rpx;
  overflow: hidden;
}
.product-card__img {
  width: 100%;
  height: 100%;
}
.product-card__placeholder {
  width: 100%;
  height: 100%;
  background: #f8f9fb;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stock-badge {
  position: absolute;
  top: 12rpx;
  left: 12rpx;
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  font-weight: 500;
  letter-spacing: 1rpx;
}
.stock-badge--out {
  color: #fff;
  background: rgba(228, 61, 51, 0.85);
}
.stock-badge--low {
  color: #fff;
  background: rgba(230, 126, 34, 0.85);
}

.product-card__body {
  padding: 16rpx 20rpx 20rpx;
}
.product-card__name {
  display: block;
  font-size: 26rpx;
  color: #1a1a1a;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}
.product-card__spec {
  display: block;
  font-size: 22rpx;
  color: #aaa;
  margin-top: 4rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-card__price-row {
  display: flex;
  align-items: baseline;
  margin-top: 10rpx;
}
.product-card__price {
  font-size: 32rpx;
  color: #ff4d4f;
  font-weight: 700;
  letter-spacing: -1rpx;
}
.product-card__unit {
  font-size: 22rpx;
  color: #bbb;
  margin-left: 2rpx;
}

/* ==================================================
   筛选遮罩 + 面板（保持既有风格）
   ================================================== */
.filter-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  z-index: 99;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.filter-mask {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.4);
}
.filter-panel {
  position: relative;
  z-index: 1;
  background: #fff;
  border-radius: 28rpx 28rpx 0 0;
  padding: 40rpx 32rpx calc(40rpx + env(safe-area-inset-bottom));
  animation: slideUp 0.25s ease both;
}
@keyframes slideUp {
  from { transform: translateY(100%); }
  to   { transform: translateY(0); }
}
.filter-section {
  margin-bottom: 36rpx;
}
.filter-section__title {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 18rpx;
}
.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.filter-chip {
  padding: 10rpx 32rpx;
  border-radius: 32rpx;
  background: #f5f6fa;
  font-size: 26rpx;
  color: #666;
  font-weight: 500;
  &--active {
    background: #2979ff;
    color: #fff;
  }
}
.filter-price-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.filter-price-input {
  flex: 1;
  height: 76rpx;
  background: #f5f6fa;
  border-radius: 16rpx;
  padding: 0 20rpx;
  font-size: 26rpx;
  color: #333;
}
.filter-price-ph { color: #bbb; font-size: 24rpx; }
.filter-price-sep { font-size: 28rpx; color: #bbb; flex-shrink: 0; }
.filter-footer {
  display: flex;
  gap: 24rpx;
  margin-top: 8rpx;
}
.filter-reset {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 44rpx;
  border: 2rpx solid #e0e0e0;
  font-size: 28rpx;
  color: #666;
  font-weight: 500;
}
.filter-confirm {
  flex: 2;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 44rpx;
  background: #2979ff;
  font-size: 28rpx;
  color: #fff;
  font-weight: 600;
}
</style>
