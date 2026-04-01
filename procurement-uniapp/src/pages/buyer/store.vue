<template>
  <view class="page-store container">
    <!-- 店铺信息 -->
    <view class="store-header card" v-if="storeInfo.enterpriseName">
      <view class="store-header__top">
        <view class="store-header__info">
          <text class="store-name">{{ storeInfo.enterpriseName }}</text>
          <text class="store-desc">{{ storeInfo.address || '' }}</text>
        </view>
        <view class="store-header__cart" @tap="goCart">
          <view class="cart-icon-wrap">
            <text class="cart-icon">🛒</text>
            <text v-if="cartCount > 0" class="cart-badge">{{ cartCount > 99 ? '99+' : cartCount }}</text>
          </view>
          <text class="orders-text">购物车</text>
        </view>
        <view class="store-header__orders" @tap="goOrders">
          <text class="orders-icon">📝</text>
          <text class="orders-text">我的订单</text>
        </view>
      </view>
    </view>

    <!-- 搜索 + 筛选按钮 -->
    <view class="search-row">
      <view class="search-row__bar">
        <SearchBar v-model="keyword" placeholder="搜索商品" @search="handleSearch" @clear="handleClear" />
      </view>
      <view class="filter-btn" :class="{ 'filter-btn--active': filterActive }" @tap="openFilter">
        <text class="filter-btn__text">筛选{{ filterActive ? ' ✦' : '' }}</text>
      </view>
    </view>

    <!-- 分类筛选 -->
    <scroll-view scroll-x class="category-scroll">
      <view class="category-list">
        <view
          v-for="cat in enrichedCategoryList"
          :key="String(cat.id)"
          class="category-item"
          :class="{ 'category-item--active': selectedCategoryId === cat.id }"
          @tap="onCategoryChange(cat.id)"
        >
          <text class="category-item__text">{{ (cat.name || '').replace(/\n/g, ' ') }}</text>
          <text v-if="cat.productCount > 0" class="category-item__count">{{ cat.productCount }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 商品列表 -->
    <view class="product-grid">
      <view v-for="item in productList" :key="item.id" class="product-card" @tap="goDetail(item.id)">
        <image v-if="item.mainImage" :src="$fileUrl(item.mainImage)" class="product-img" mode="aspectFill" />
        <view v-else class="product-img-placeholder">
          <text>📦</text>
        </view>
        <view class="product-info">
          <text class="product-name">{{ item.name }}</text>
          <text class="product-spec" v-if="item.spec">{{ item.spec }}</text>
          <view class="product-bottom">
            <text class="product-price">¥{{ item.price }}</text>
            <text class="product-unit">/{{ item.unit || '件' }}</text>
            <text v-if="item.stockStatus === 'OUT_OF_STOCK'" class="stock-tag stock-tag--out">缺货</text>
            <text v-else-if="item.stockStatus === 'LOW_STOCK'" class="stock-tag stock-tag--low">仅剩 {{ item.stock }} 件</text>
            <text v-else class="stock-tag stock-tag--in">有货</text>
          </view>
        </view>
      </view>
    </view>

    <EmptyState v-if="!productList.length && !loading" text="暂无商品" icon="🛒" />
    <LoadMore v-if="productList.length" :status="loadStatus" @loadMore="loadMore" />

    <!-- 筛选面板 + 遮罩 -->
    <view v-if="filterVisible" class="filter-overlay">
      <view class="filter-mask" @tap="closeFilter" />
      <view class="filter-panel">
        <!-- 库存 -->
        <view class="filter-section">
          <text class="filter-section__title">库存状态</text>
          <view class="filter-chips">
            <view class="filter-chip" :class="{ 'filter-chip--active': draftStock === null }" @tap="draftStock = null">全部</view>
            <view class="filter-chip" :class="{ 'filter-chip--active': draftStock === 'IN_STOCK' }" @tap="draftStock = 'IN_STOCK'">有货</view>
            <view class="filter-chip" :class="{ 'filter-chip--active': draftStock === 'OUT_OF_STOCK' }" @tap="draftStock = 'OUT_OF_STOCK'">缺货</view>
          </view>
        </view>
        <!-- 价格排序 -->
        <view class="filter-section">
          <text class="filter-section__title">价格排序</text>
          <view class="filter-chips">
            <view class="filter-chip" :class="{ 'filter-chip--active': draftSort === null }" @tap="draftSort = null">默认</view>
            <view class="filter-chip" :class="{ 'filter-chip--active': draftSort === 'price_asc' }" @tap="draftSort = 'price_asc'">低 → 高</view>
            <view class="filter-chip" :class="{ 'filter-chip--active': draftSort === 'price_desc' }" @tap="draftSort = 'price_desc'">高 → 低</view>
          </view>
        </view>
        <!-- 价格区间 -->
        <view class="filter-section">
          <text class="filter-section__title">价格区间</text>
          <view class="filter-price-row">
            <input
              class="filter-price-input"
              type="digit"
              :value="draftPriceMin"
              @input="draftPriceMin = $event.detail.value"
              placeholder="最低价 ¥"
              placeholder-class="filter-price-ph"
            />
            <text class="filter-price-sep">～</text>
            <input
              class="filter-price-input"
              type="digit"
              :value="draftPriceMax"
              @input="draftPriceMax = $event.detail.value"
              placeholder="最高价 ¥"
              placeholder-class="filter-price-ph"
            />
          </view>
        </view>
        <!-- 底部操作 -->
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
    // 任意筛选条件生效时为 true，筛选按钮高亮
    filterActive() {
      return this.filterStock !== null ||
        this.filterSort !== null ||
        this.filterPriceMin !== '' ||
        this.filterPriceMax !== ''
    },
    // 分类列表＋商品数量
    // “全部” chip ：筛选生效时显示筛选后的总数（categoryFilteredTotal），否则显示全店总数
    // 其余分类的数量来自后端已计算的 productCount 字段
    enrichedCategoryList() {
      return this.categoryList.map(cat => {
        if (cat.id === null) {
          const count = this.filterActive
            ? this.categoryFilteredTotal
            : (this.storeInfo.productCount || 0)
          return { ...cat, productCount: count }
        }
        return cat  // productCount 已由后端内嵌（含筛选条件）
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
      productList: [],
      page: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      loadStatus: 'more',
      // 筛选状态下 "全部" 分类匹配到的总商品数（初始化时与 storeInfo.productCount 同步）
      categoryFilteredTotal: 0,
      // 筛选面板开关
      filterVisible: false,
      // 草稿（面板内临时值，确定前不影响列表）
      draftStock: null,
      draftSort: null,
      draftPriceMin: '',
      draftPriceMax: '',
      // 已生效的筛选值
      filterStock: null,
      filterSort: null,
      filterPriceMin: '',
      filterPriceMax: ''
    }
  },
  onShow() {
    // 买家页面不应允许通过 home 按钮跳回商家/员工界面
    // hideHomeButton 仅在微信小程序环境中存在，做运行时判断以兼容 H5/其他平台
    if (typeof wx !== 'undefined' && typeof wx.hideHomeButton === 'function') {
      wx.hideHomeButton()
    }
  },
  onLoad(query) {
    this.enterpriseId = query.enterpriseId || ''
    if (this.enterpriseId) {
      this.loadStoreInfo()
      this.loadCategories()
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
    async loadProducts() {
      this.loading = true
      try {
        const params = {
          keyword: this.keyword,
          pageNum: this.page,
          pageSize: this.pageSize
        }
        if (this.selectedCategoryId) {
          params.categoryId = this.selectedCategoryId
        }
        if (this.filterStock) params.stockStatus = this.filterStock
        if (this.filterSort) params.sortBy = this.filterSort
        if (this.filterPriceMin !== '') params.priceMin = this.filterPriceMin
        if (this.filterPriceMax !== '') params.priceMax = this.filterPriceMax
        const res = await getStoreProducts(this.enterpriseId, params)
        const list = res?.records || res || []
        const total = res?.total ?? list.length
        this.total = total
        // 当无分类过滤时，第 1 页返回的 total 就是“全部”分类匹配当前筛选的总数
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
    // ---- 筛选面板 ----
    openFilter() {
      // 打开时把已生效值复制到草稿，保持上次选择的回显
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
      // 价格区间合法性校验
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
      // 分类数量与商品列表同步刷新
      this.loadCategories(this.filterStock, this.filterPriceMin, this.filterPriceMax)
      this.loadProducts()
    }
  }
}
</script>

<style lang="scss" scoped>
.stock-tag {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 4rpx;
  margin-left: 12rpx;
}
.stock-tag--in {
  color: #18bc37;
  background: #e8f8eb;
}
.stock-tag--out {
  color: #e43d33;
  background: #fde8e7;
}
.stock-tag--low {
  color: #e67e22;
  background: #fef5e7;
}

.store-header {
  padding: 24rpx;
}
.store-header__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.store-header__info {
  flex: 1;
}
.store-header__cart,
.store-header__orders {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8rpx 16rpx;
}
.cart-icon-wrap {
  position: relative;
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cart-icon {
  font-size: 40rpx;
}
.cart-badge {
  position: absolute;
  top: -10rpx;
  right: -18rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  padding: 0 8rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 18rpx;
  border-radius: 16rpx;
  text-align: center;
}
.orders-icon {
  font-size: 40rpx;
}
.orders-text {
  font-size: 20rpx;
  color: #666;
  margin-top: 4rpx;
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
/* 分类筛选 */
.category-scroll {
  background: #fff;
  margin-bottom: 8rpx;
  width: 100%;
}
.category-list {
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
  padding: 16rpx 24rpx;
  box-sizing: border-box;
}
.category-item {
  flex-shrink: 0;
  padding: 10rpx 28rpx;
  margin-right: 16rpx;
  border-radius: 32rpx;
  background: #f5f6fa;

  &--active {
    background: #2979ff;
    .category-item__text { color: #fff; }
    .category-item__count { color: rgba(255,255,255,0.85); background: rgba(255,255,255,0.25); }
  }

  &__text {
    font-size: 26rpx;
    color: #666;
    white-space: nowrap;
  }
  &__count {
    font-size: 20rpx;
    color: #999;
    margin-left: 6rpx;
    background: rgba(0,0,0,0.06);
    border-radius: 20rpx;
    padding: 0 10rpx;
    line-height: 34rpx;
    white-space: nowrap;
  }
}
.product-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}
.product-card {
  width: calc(50% - 8rpx);
  box-sizing: border-box;
  margin-bottom: 16rpx;
  padding: 0;
  overflow: hidden;
  border-radius: 16rpx;
  background-color: #fff;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
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

/* 搜索 + 筛选行 */
.search-row {
  display: flex;
  align-items: center;
  background: #fff;
  padding-right: 24rpx;
}
.search-row__bar {
  flex: 1;
  min-width: 0;
}
.filter-btn {
  flex-shrink: 0;
  padding: 10rpx 28rpx;
  margin-left: 8rpx;
  border-radius: 32rpx;
  background: #f5f6fa;
  &--active {
    background: #2979ff;
    .filter-btn__text { color: #fff; }
  }
}
.filter-btn__text {
  font-size: 26rpx;
  color: #666;
  white-space: nowrap;
}

/* 筛选遮罩 + 面板 */
.filter-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 99;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.filter-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
}
.filter-panel {
  position: relative;
  z-index: 1;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
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
  color: #333;
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
  height: 72rpx;
  background: #f5f6fa;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 26rpx;
  color: #333;
}
.filter-price-ph {
  color: #bbb;
  font-size: 24rpx;
}
.filter-price-sep {
  font-size: 28rpx;
  color: #bbb;
  flex-shrink: 0;
}
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
  border: 2rpx solid #ddd;
  font-size: 28rpx;
  color: #666;
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
