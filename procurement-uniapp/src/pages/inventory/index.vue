<template>
  <view class="page-inventory">
    <view class="hero-header">
      <!-- 汇总数据 -->
      <view class="hero-header__stat">
        <view class="stat-content">
          <text class="stat-label">库藏全局总项 (SKU)</text>
          <text class="stat-value num-font">{{ total }}</text>
        </view>
      </view>
      
      <!-- 顶部搜索栏 -->
      <view class="search-bar">
        <view class="search-bar__input-wrap">
          <view class="search-bar__icon"></view>
          <input
            class="search-bar__input"
            v-model="keyword"
            placeholder="搜索商品名称/分类"
            placeholder-class="search-placeholder-white"
            confirm-type="search"
            @confirm="onSearch"
          />
          <view v-if="keyword" class="search-bar__clear" @tap="[keyword='', onSearch()]">
            <view class="svg-icon-close"></view>
          </view>
        </view>
      </view>

      <template v-if="!noEnterprise">
        <!-- 预警筛选 + 通知订阅 -->
        <view class="toolbar-row">
          <view
            class="filter-tag"
            :class="{ 'filter-tag--active': onlyWarning }"
            @tap="toggleWarning"
            hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100"
          >
            <view class="svg-icon-tool svg-icon-zap"></view>
            <text class="filter-tag__text">预警商品</text>
            <text v-if="warningCount > 0" class="filter-tag__badge">{{ warningCount }}</text>
          </view>
          <view
            class="filter-tag"
            :class="notifyEnabled ? 'filter-tag--notify-on' : 'filter-tag--notify'"
            @tap="toggleNotify"
            hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100"
          >
            <view class="svg-icon-tool" :class="notifyEnabled ? 'svg-icon-bell-off' : 'svg-icon-bell'"></view>
            <text class="filter-tag__text">{{ notifyEnabled ? '关闭通知' : '预警通知' }}</text>
          </view>
          <view class="filter-tag filter-tag--filter" :class="{ 'filter-tag--active': hasActiveFilter }" @tap="showFilterPopup = true" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
            <view class="svg-icon-tool svg-icon-filter"></view>
            <text class="filter-tag__text">高级筛选</text>
            <text v-if="activeFilterCount > 0" class="filter-tag__badge">{{ activeFilterCount }}</text>
          </view>
        </view>
      </template>
    </view>

    <!-- 未加入企业提示-->
    <view v-if="noEnterprise" class="enterprise-tip container" @tap="goCreateEnterprise">
      <text class="enterprise-tip__icon">⚠</text>
      <text class="enterprise-tip__text">请先创建或加入企业，点击前往设置</text>
      <text class="enterprise-tip__arrow">›</text>
    </view>

    <template v-else>
      <view class="bento-category-container">
        <scroll-view scroll-x class="category-scroll" :show-scrollbar="false">
          <view class="category-list">
            <view
              v-for="cat in categoryList"
              :key="cat.id === null ? 'all' : cat.id"
              class="category-pill"
              :class="{ 'category-pill--active': selectedCategoryId === cat.id }"
              @tap="onCategoryTap(cat.id)"
            >
              <view class="category-pill__name">{{ (cat.name || '未命名').replace(/\s+/g, '') }}</view>
              <view v-if="cat.id !== null && categoryStats[cat.id] != null" class="category-pill__count">{{ categoryStats[cat.id] }}</view>
            </view>
          </view>
        </scroll-view>
        <view v-if="categoryList.length > 5" class="category-expand-btn" @tap="showCategoryPopup = true">
          <view class="svg-icon-action svg-icon-chevron-down" style="background-color: var(--text-tertiary); width: 40rpx; height: 40rpx;"></view>
        </view>
      </view>

      <!-- 操作按钮区 -> Bento Actions -->
      <view class="bento-actions container">
        <button class="bento-card bento-success" open-type="share" plain hover-class="saas-card-push">
          <view class="bento-icon-share"></view>
          <text class="bento-text">分享橱窗</text>
        </button>
        <template v-if="hasFullAccess">
          <view class="bento-card" @tap="goCategory" hover-class="saas-card-push">
            <view class="bento-icon-folder"></view>
            <text class="bento-text">分类管理</text>
          </view>
          <view class="bento-card" @tap="goBatchImport" hover-class="saas-card-push">
            <view class="bento-icon-download"></view>
            <text class="bento-text">批量导入</text>
          </view>
          <view class="bento-card bento-primary" @tap="goAddProduct" hover-class="saas-card-push">
            <view class="bento-icon-plus"></view>
            <text class="bento-text">新建商品</text>
          </view>
        </template>
      </view>
    </template>

    <!-- 商品列表 -->
    <view class="product-list">
      <!-- 骨架屏加载 -->
      <view v-if="loading && productList.length === 0" class="skeleton-list">
        <view v-for="i in 6" :key="i" class="product-card saas-card">
          <view class="skeleton-img skeleton-shimmer"></view>
          <view class="product-card__info" style="gap: 16rpx; display: flex; flex-direction: column;">
            <view class="skeleton-text skeleton-shimmer" style="width: 70%; height: 32rpx; border-radius: 8rpx;"></view>
            <view class="skeleton-text skeleton-shimmer" style="width: 40%; height: 24rpx; border-radius: 8rpx;"></view>
            <view class="product-card__row" style="margin-top: 8rpx;">
              <view class="skeleton-text skeleton-shimmer" style="width: 30%; height: 40rpx; border-radius: 8rpx;"></view>
              <view class="skeleton-btn skeleton-shimmer" style="width: 120rpx; height: 50rpx; border-radius: 25rpx;"></view>
            </view>
          </view>
        </view>
      </view>

      <view
        v-for="(item, index) in productList"
        :key="item.id"
        class="product-card saas-card animate-fade-up"
        hover-class="saas-card-push"
        :hover-start-time="0"
        :hover-stay-time="100"
        :style="{ animationDelay: (index % 10) * 0.05 + 's' }"
        @tap="goEditProduct(item.id)"
      >
        <view class="product-card__img-wrap">
          <image
            v-if="item.images && item.images.length > 0"
            class="product-card__img"
            :src="$fileUrl(item.images[0])"
            mode="aspectFill"
          />
          <view v-else class="product-card__img-placeholder" :style="{ background: getCategoryStyle(item.categoryId).bg }">
            <view class="svg-cat-icon" :style="{ backgroundImage: getCategoryStyle(item.categoryId).icon }"></view>
          </view>
        </view>
        <view class="product-card__info">
          <text class="product-card__name">{{ item.name }}</text>
          <text class="product-card__spec">{{ item.spec }} / {{ item.unit }}</text>
          <view class="product-card__row">
            <text class="product-card__price price-text num-font">¥{{ item.price }}</text>
            <view class="stock-status">
              <view class="stock-status__dot" :class="item.stockWarning > 0 && item.stock <= item.stockWarning ? 'bg-danger' : 'bg-success'"></view>
              <text class="stock-status__text num-font">{{ item.stock }} 件</text>
            </view>
            <view v-if="!isSales" class="product-card__right">
              <view class="btn-adjust" hover-class="btn-adjust-hover" hover-stop-propagation="true" @tap.stop="openAdjust(item)">调整</view>
            </view>
          </view>
        </view>
      </view>
      <EmptyState v-if="productList.length === 0 && !loading" text="暂无商品" buttonText="添加商品" @action="goAddProduct" />
      <LoadMore v-if="productList.length > 0" :status="loadMoreStatus" @load="loadMore" />
    </view>

    <!-- 库存调整弹窗 -->
    <view v-if="showAdjustPopup" class="popup-mask" @tap="showAdjustPopup = false">
      <view class="popup-content" @tap.stop>
        <text class="popup-title">库存调整  {{ adjustProduct.name }}</text>
        <text class="popup-stock-info">当前库存: {{ adjustProduct.stock }}</text>
        
        <view class="popup-field">
          <text class="popup-label">调整类型</text>
          <view class="adjust-segment-group">
            <view
              class="adjust-segment-btn"
              :class="{ 'is-active is-in': adjustForm.type === 'IN' }"
              @tap="adjustForm.type = 'IN'"
            ><text>入库</text></view>
            <view
              class="adjust-segment-btn"
              :class="{ 'is-active is-out': adjustForm.type === 'OUT' }"
              @tap="adjustForm.type = 'OUT'"
            ><text>出库</text></view>
          </view>
        </view>
        
        <view class="popup-field">
          <text class="popup-label">调整数量</text>
          <view class="stepper-group">
            <view class="stepper-btn" hover-class="stepper-btn--hover" @tap="adjustForm.quantity = Math.max(1, (parseInt(adjustForm.quantity) || 0) - 1)">
              <view class="stepper-icon-minus"></view>
            </view>
            <input class="stepper-input num-font" type="number" v-model="adjustForm.quantity" placeholder="0" />
            <view class="stepper-btn" hover-class="stepper-btn--hover" @tap="adjustForm.quantity = (parseInt(adjustForm.quantity) || 0) + 1">
              <view class="stepper-icon-plus"></view>
            </view>
          </view>
        </view>
        
        <view class="popup-actions-asymmetric">
          <text class="action-cancel-text" @tap="showAdjustPopup = false">取消</text>
          <view
            class="action-confirm-capsule"
            hover-class="action-confirm-capsule--hover"
            :style="{ opacity: adjustSubmitting ? 0.6 : 1 }"
            @tap="handleAdjust"
          >
            <text class="confirm-text">{{ adjustSubmitting ? '处理中...' : '确认调整' }}</text>
          </view>
        </view>
      </view>
    </view>
    <!-- 筛选底部弹窗-->
    <view v-if="showFilterPopup" class="popup-mask" @tap="showFilterPopup = false">
      <view class="filter-sheet" @tap.stop>
        <view class="filter-sheet__header">
          <text class="filter-sheet__title">商品筛选</text>
          <text class="filter-sheet__close" @tap="showFilterPopup = false">✕</text>
        </view>

        <scroll-view scroll-y class="filter-sheet__body">
          <!-- 价格区间 -->
          <view class="filter-section">
            <text class="filter-section__label">价格区间</text>
            <view class="filter-chips">
              <text v-for="opt in priceOptions" :key="opt.label"
                class="filter-chip" :class="{ 'filter-chip--active': filterForm.priceRange === opt.value }"
                @tap="filterForm.priceRange = opt.value">{{ opt.label }}</text>
            </view>
            <view v-if="filterForm.priceRange === 'custom'" class="filter-custom-range">
              <input class="filter-range-input" type="digit" v-model="filterForm.customMinPrice" placeholder="最低价" />
              <text class="filter-range-sep"></text>
              <input class="filter-range-input" type="digit" v-model="filterForm.customMaxPrice" placeholder="最高价" />
            </view>
          </view>

          <!-- 库存区间 -->
          <view class="filter-section">
            <text class="filter-section__label">库存区间</text>
            <view class="filter-chips">
              <text v-for="opt in stockOptions" :key="opt.label"
                class="filter-chip" :class="{ 'filter-chip--active': filterForm.stockRange === opt.value }"
                @tap="filterForm.stockRange = opt.value">{{ opt.label }}</text>
            </view>
          </view>

          <!-- 商品状态 -->
          <view class="filter-section">
            <text class="filter-section__label">商品状态</text>
            <view class="filter-chips">
              <text class="filter-chip" :class="{ 'filter-chip--active': filterForm.status === null }"
                @tap="filterForm.status = null">全部</text>
              <text class="filter-chip" :class="{ 'filter-chip--active': filterForm.status === 1 }"
                @tap="filterForm.status = 1">上架中</text>
              <text class="filter-chip" :class="{ 'filter-chip--active': filterForm.status === 0 }"
                @tap="filterForm.status = 0">已下架</text>
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

    <!-- 分类全览弹窗 -->
    <view v-if="showCategoryPopup" class="category-popup-mask" @tap="showCategoryPopup = false">
      <view class="category-popup" @tap.stop>
        <view class="category-popup__header">
          <text class="category-popup__title">所有分类</text>
          <text class="category-popup__close" @tap="showCategoryPopup = false">✕</text>
        </view>
        <scroll-view scroll-y class="category-popup__body">
          <view class="category-grid">
            <view
              v-for="cat in categoryList"
              :key="cat.id === null ? 'all' : cat.id"
              class="category-grid-item"
              :class="{ 'category-grid-item--active': selectedCategoryId === cat.id }"
              @tap="onCategoryTap(cat.id)"
            >
              <view class="category-grid-item__name">{{ (cat.name || '未命名').replace(/\s+/g, '') }}</view>
              <view v-if="cat.id !== null && categoryStats[cat.id] != null" class="category-grid-item__count">· {{ categoryStats[cat.id] }}</view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { getProductList, adjustStock, getStockWarnings } from '@/api/product'
import { getCategoryList, getCategoryStats } from '@/api/category'
import { getNotifyStatus, enableNotify, disableNotify } from '@/api/subscribe'
import { useUserStore, waitForLoginReady } from '@/store/user'
import { WX_STOCK_WARNING_TEMPLATE_ID } from '@/config/index'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

export default {
  components: { EmptyState, LoadMore },
  data() {
    return {
      keyword: '',
      noEnterprise: false,
      categoryList: [{ id: null, name: '全部' }],
      categoryStats: {},
      showCategoryPopup: false,
      notifyEnabled: false,
      selectedCategoryId: null,
      _statsVersion: 0,  // 防竞态版本号
      productList: [],
      pageNum: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      loadMoreStatus: 'more',
      showAdjustPopup: false,
      adjustProduct: {},
      adjustForm: { type: 'IN', quantity: '' },
      adjustSubmitting: false,
      onlyWarning: false,
      warningCount: 0,
      showFilterPopup: false,
      filterForm: {
        priceRange: 'all',
        customMinPrice: '',
        customMaxPrice: '',
        stockRange: 'all',
        status: null,
        sortBy: 'default'
      },
      appliedFilter: {},
      priceOptions: [
        { label: '不限', value: 'all' },
        { label: '0~50', value: '0-50' },
        { label: '50~200', value: '50-200' },
        { label: '200~500', value: '200-500' },
        { label: '500+', value: '500-' },
        { label: '自定义', value: 'custom' }
      ],
      stockOptions: [
        { label: '不限', value: 'all' },
        { label: '0~50', value: '0-50' },
        { label: '50~200', value: '50-200' },
        { label: '200+', value: '200-' }
      ],
      sortOptions: [
        { label: '默认', value: 'default' },
        { label: '价格升序', value: 'priceAsc' },
        { label: '价格降序', value: 'priceDesc' },
        { label: '库存升序', value: 'stockAsc' },
        { label: '库存降序', value: 'stockDesc' },
        { label: '最新', value: 'newest' }
      ]
    }
  },
  computed: {
    hasActiveFilter() {
      const f = this.appliedFilter
      return f.priceRange && f.priceRange !== 'all'
        || f.stockRange && f.stockRange !== 'all'
        || f.status != null
        || f.sortBy && f.sortBy !== 'default'
    },
    activeFilterCount() {
      let count = 0
      const f = this.appliedFilter
      if (f.priceRange && f.priceRange !== 'all') count++
      if (f.stockRange && f.stockRange !== 'all') count++
      if (f.status != null) count++
      if (f.sortBy && f.sortBy !== 'default') count++
      return count
    },
    showCategoryToggle() {
      // 超过5个分类时才显示折叠按钮（少量分类通常一行内就能放下）
      return this.categoryList.length > 5
    },
    hasFullAccess() { return useUserStore().hasFullAccess },
    isSales()       { return useUserStore().isSales },
    isWarehouse()   { return useUserStore().isWarehouse }
  },
  onLoad(query) {
    if (query && query.stockWarning === 'true') {
      this.onlyWarning = true
    }
    // 监听统计页跳转过来的预警筛选事件
    uni.$on('showStockWarning', () => {
      this.onlyWarning = true
      this.loadCategoryStats()
      this.refresh()
    })
  },
  onUnload() {
    uni.$off('showStockWarning')
  },
  async onShow() {
    await waitForLoginReady()
    const userStore = useUserStore()
    if (userStore.isLoggedIn && !userStore.hasEnterprise) {
      try {
        await userStore.fetchProfile()
      } catch (e) {
        console.warn('[inventory] profile sync before enterprise check failed', e && e.message || e)
      }
    }
    if (!userStore.hasEnterprise) {
      this.noEnterprise = true
      return
    }
    this.noEnterprise = false
    this.loadCategories()
    this.loadWarningCount()
    await this.loadCategoryStats()
    this.loadNotifyStatus()
    this.refresh()
  },
  /**
   * 微信小程序分享回调，分享店铺链接给买家
   */
  onShareAppMessage() {
    const userStore = useUserStore()
    const enterpriseId = userStore.userInfo.enterpriseId
    const storeName = userStore.userInfo.nickName || '店铺'
    return {
      title: `${storeName}的商品库存`,
      path: `/pages/buyer/store?enterpriseId=${enterpriseId || ''}`,
      imageUrl: ''
    }
  },
  onShareTimeline() {
    const userStore = useUserStore()
    const enterpriseId = userStore.userInfo.enterpriseId
    const storeName = userStore.userInfo.nickName || '店铺'
    return {
      title: `${storeName}的商品库存`,
      query: `enterpriseId=${enterpriseId || ''}`
    }
  },
  onPullDownRefresh() {
    this.refresh().finally(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    this.loadMore()
  },
  methods: {
    goCreateEnterprise() {
      uni.navigateTo({ url: '/pages/profile/create-enterprise' })
    },
    async loadCategories() {
      try {
        const list = await getCategoryList()
        this.categoryList = [{ id: null, name: '全部' }, ...list]
      } catch (e) {
        if (e && (e.code === 40100 || e.code === 40402 || e.silent)) {
          this.noEnterprise = true
          return
        }
        uni.showToast({ title: '加载分类失败', icon: 'none' })
      }
    },
    async loadWarningCount() {
      try {
        const res = await getProductList({ pageNum: 1, pageSize: 1, stockWarning: true })
        this.warningCount = res.total || 0
        if (this.warningCount > 0) {
          uni.setTabBarBadge({ index: 0, text: String(this.warningCount) })
        } else {
          uni.removeTabBarBadge({ index: 0 })
        }
      } catch (e) {
        if (e && (e.code === 40100 || e.code === 40402 || e.silent)) {
          this.noEnterprise = true
          return
        }
      }
    },
    toggleWarning() {
      this.onlyWarning = !this.onlyWarning
      this.loadCategoryStats()
      this.refresh()
    },
    parseRange(rangeStr) {
      // '0-50'  [0, 50], '500-'  [500, null], 'all'  [null, null]
      if (!rangeStr || rangeStr === 'all') return [null, null]
      const parts = rangeStr.split('-')
      const min = parts[0] !== '' ? Number(parts[0]) : null
      const max = parts[1] !== '' && parts[1] !== undefined ? Number(parts[1]) : null
      return [min, max]
    },
    resetFilter() {
      this.filterForm = {
        priceRange: 'all',
        customMinPrice: '',
        customMaxPrice: '',
        stockRange: 'all',
        status: null,
        sortBy: 'default'
      }
    },
    applyFilter() {
      this.appliedFilter = { ...this.filterForm }
      this.showFilterPopup = false
      this.loadCategoryStats()
      this.refresh()
    },
    async loadNotifyStatus() {
      try {
        const enabled = await getNotifyStatus()
        const val = !!enabled
        this.notifyEnabled = val
        useUserStore().setNotifyEnabled(val)
      } catch (e) {
        // 静默失败，默认显示"开启"
      }
    },
    toggleNotify() {
      if (this.notifyEnabled) {
        // 已开启  确认关闭
        uni.showModal({
          title: '关闭预警通知',
          content: '关闭后将不再收到库存预警推送。如需在微信通知栏彻底取消，请前往「微信→服务通知」中管理。',
          confirmText: '确认关闭',
          cancelText: '取消',
          success: async ({ confirm }) => {
            if (!confirm) return
            try {
              await disableNotify()
              this.notifyEnabled = false
              useUserStore().setNotifyEnabled(false)
              uni.showToast({ title: '通知已关闭', icon: 'none' })
            } catch (e) {
              uni.showToast({ title: '操作失败，请重试', icon: 'none' })
            }
          }
        })
      } else {
        // 未开启  弹微信订阅授权
        // #ifdef MP-WEIXIN
        const tmplId = WX_STOCK_WARNING_TEMPLATE_ID
        wx.requestSubscribeMessage({
          tmplIds: [tmplId],
          success: async (res) => {
            if (res[tmplId] === 'accept') {
              try {
                await enableNotify()
                this.notifyEnabled = true
                useUserStore().setNotifyEnabled(true)
                uni.showToast({ title: '已开启预警通知', icon: 'success' })
              } catch (e) {
                uni.showToast({ title: '开启失败，请重试', icon: 'none' })
              }
            } else if (res[tmplId] === 'reject') {
              uni.showToast({ title: '您已拒绝通知授权', icon: 'none' })
            }
          },
          fail: () => {
            uni.showToast({ title: '授权失败，请重试', icon: 'none' })
          }
        })
        // #endif
        // #ifndef MP-WEIXIN
        uni.showToast({ title: '仅微信小程序支持通知', icon: 'none' })
        // #endif
      }
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
        const params = {
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }
        if (this.selectedCategoryId != null) {
          params.categoryId = this.selectedCategoryId
        }
        if (this.keyword) {
          params.keyword = this.keyword
        }
        if (this.onlyWarning) {
          params.stockWarning = true
        }
        // 筛选参数
        const f = this.appliedFilter
        if (f.priceRange && f.priceRange !== 'all') {
          if (f.priceRange === 'custom') {
            if (f.customMinPrice) params.minPrice = f.customMinPrice
            if (f.customMaxPrice) params.maxPrice = f.customMaxPrice
          } else {
            const [min, max] = this.parseRange(f.priceRange)
            if (min != null) params.minPrice = min
            if (max != null) params.maxPrice = max
          }
        }
        if (f.stockRange && f.stockRange !== 'all') {
          const [min, max] = this.parseRange(f.stockRange)
          if (min != null) params.minStock = min
          if (max != null) params.maxStock = max
        }
        if (f.status != null) {
          params.status = f.status
        }
        if (f.sortBy && f.sortBy !== 'default') {
          params.sortBy = f.sortBy
        }
        const res = await getProductList(params)
        this.productList = this.pageNum === 1 ? res.records : [...this.productList, ...res.records]
        this.total = res.total
        this.loadMoreStatus = this.productList.length >= this.total ? 'noMore' : 'more'
      } catch (e) {
        if (e && (e.code === 40100 || e.code === 40402 || e.silent)) {
          this.noEnterprise = true
          return
        }
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
    /**
     * 构造分类统计接口的筛选参数（不含 categoryId）
     */
    buildFilterParams() {
      const params = {}
      if (this.keyword) params.keyword = this.keyword
      if (this.onlyWarning) params.stockWarning = true
      const f = this.appliedFilter
      if (f.priceRange && f.priceRange !== 'all') {
        if (f.priceRange === 'custom') {
          if (f.customMinPrice) params.minPrice = f.customMinPrice
          if (f.customMaxPrice) params.maxPrice = f.customMaxPrice
        } else {
          const [min, max] = this.parseRange(f.priceRange)
          if (min != null) params.minPrice = min
          if (max != null) params.maxPrice = max
        }
      }
      if (f.stockRange && f.stockRange !== 'all') {
        const [min, max] = this.parseRange(f.stockRange)
        if (min != null) params.minStock = min
        if (max != null) params.maxStock = max
      }
      if (f.status != null) params.status = f.status
      return params
    },
    /**
     * 加载分类统计：当前筛选条件下各分类的商品种数
     * 不含选中分类维度，工筛选和搜索变化时触发
     */
    async loadCategoryStats() {
      const version = ++this._statsVersion
      try {
        const params = this.buildFilterParams()
        const res = await getCategoryStats(params)
        if (version !== this._statsVersion) return  // 丢弃过期响应
        this.categoryStats = res || {}
      } catch (e) {
        // 分类计数非核心功能，静默失败
      }
    },
    onSearch() {
      this.loadCategoryStats()
      this.refresh()
    },
    onCategoryTap(id) {
      this.selectedCategoryId = id
      this.showCategoryPopup = false // 点击分类后自动收起全览菜单
      this.refresh()  // 仅刷新商品列表，不重载分类统计
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
      const readonly = !this.hasFullAccess
      uni.navigateTo({ url: `/pages/inventory/product-form?id=${id}${readonly ? '&readonly=true' : ''}` })
    },

    openAdjust(item) {
      this.adjustProduct = { ...item }
      // 默认类型：SALES 只能出库，WAREHOUSE 只能入库，其余默认入库
      const defaultType = this.isSales ? 'OUT' : 'IN'
      this.adjustForm = { type: defaultType, quantity: '' }
      this.showAdjustPopup = true
    },
    getCategoryStyle(categoryId) {
      const iconFood = `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23FF9800' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 13.87A4 4 0 0 1 7.41 6a5.11 5.11 0 0 1 1.05-1.54 5 5 0 0 1 7.08 0A5.11 5.11 0 0 1 16.59 6 4 4 0 0 1 18 13.87V21H6Z'/%3E%3Cline x1='6' y1='17' x2='18' y2='17'/%3E%3C/svg%3E")`
      const iconLiquid = `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%232196F3' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M4 22h16c0-4.5-3-5.5-6-10 1.5 0 1.5-2 1.5-4C15.5 3 13 2 12 2s-3.5 1-3.5 6c0 2 0 4 1.5 4-3 4.5-6 5.5-6 10Z'/%3E%3Cpath d='M12 11v11'/%3E%3C/svg%3E")`
      const iconToy = `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23E91E63' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='12' cy='12' r='10'/%3E%3Ccircle cx='12' cy='12' r='4'/%3E%3Cline x1='21.17' y1='8' x2='12' y2='8'/%3E%3Cline x1='3.95' y1='6.06' x2='8.54' y2='14'/%3E%3Cline x1='10.88' y1='21.94' x2='15.46' y2='14'/%3E%3C/svg%3E")`
      const iconStationery = `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%234CAF50' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M12 20h9'/%3E%3Cpath d='M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z'/%3E%3C/svg%3E")`
      const iconTech = `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239C27B0' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Crect x='2' y='3' width='20' height='14' rx='2' ry='2'/%3E%3Cline x1='8' y1='21' x2='16' y2='21'/%3E%3Cline x1='12' y1='17' x2='12' y2='21'/%3E%3C/svg%3E")`
      const iconClothes = `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23FFC107' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M20.38 3.46L16 2a4 4 0 0 1-8 0L3.62 3.46a2 2 0 0 0-1.34 2.23l.58 3.47a1 1 0 0 0 .99.84H6v10c0 1.1.9 2 2 2h8a2 2 0 0 0 2-2V10h2.15a1 1 0 0 0 .99-.84l.58-3.47a2 2 0 0 0-1.34-2.23z'/%3E%3C/svg%3E")`
      const iconDefault = `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239E9E9E' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z'/%3E%3Cpolyline points='3.27 6.96 12 12.01 20.73 6.96'/%3E%3Cline x1='12' y1='22.08' x2='12' y2='12'/%3E%3C/svg%3E")`

      const map = {
        1: { icon: iconFood, bg: '#FFF3E0' },
        2: { icon: iconLiquid, bg: '#E3F2FD' },
        3: { icon: iconToy, bg: '#FCE4EC' },
        4: { icon: iconStationery, bg: '#E8F5E9' },
        5: { icon: iconTech, bg: '#EDE7F6' },
        6: { icon: iconClothes, bg: '#FFF8E1' }
      }
      return map[categoryId] || { icon: iconDefault, bg: '#F5F5F5' }
    },
    async handleAdjust() {
      if (this.adjustSubmitting) return  // 防止订阅弹窗期间重复点击
      const qty = parseInt(this.adjustForm.quantity)
      if (!qty || qty < 1) {
        return uni.showToast({ title: '请输入有效数量', icon: 'none' })
      }
      if (this.adjustForm.type === 'OUT' && qty > this.adjustProduct.stock) {
        return uni.showToast({ title: '出库数量不能超过当前库存', icon: 'none' })
      }
      this.adjustSubmitting = true  // 加锁：在弹窗出现前即锁住，防止并发
      try {
        // 出库时静默申请一次订阅配额，补充微信推送权限（一次性订阅机制）
        // #ifdef MP-WEIXIN
        if (this.adjustForm.type === 'OUT' && this.notifyEnabled) {
          await new Promise((resolve) => {
            wx.requestSubscribeMessage({
              tmplIds: [WX_STOCK_WARNING_TEMPLATE_ID],
              complete: () => resolve() // 无论用户点允许/拒绝/关闭，都继续执行出库
            })
          })
        }
        // #endif
        await adjustStock(this.adjustProduct.id, {
          productId: this.adjustProduct.id,
          quantity: qty,
          type: this.adjustForm.type
        })
        uni.showToast({ title: this.adjustForm.type === 'IN' ? '入库成功' : '出库成功', icon: 'success' })
        this.showAdjustPopup = false
        this.loadWarningCount()
        this.refresh()
      } catch (e) {
        console.error(e)
        // 区分业务错误（库存不足40902）和网络/服务器错误，给出精准提示
        if (e && e.code === 40902) {
          uni.showToast({ title: '库存不足，无法出库', icon: 'none' })
        } else {
          uni.showToast({ title: '库存调整失败，请重试', icon: 'none' })
        }
      } finally {
        this.adjustSubmitting = false  // 无论成功/失败，解锁
      }
    }
  }
}
</script>

<style lang="scss" scoped>
/* 天穹悬浮头 (Immersive Hero Header) */
.hero-header {
  background: #ffffff;
  padding: 24rpx 24rpx 40rpx;
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

/* 搜索栏 */
.search-bar {
  display: flex;
  align-items: center;
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

/* 工具栏 */
.toolbar-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.filter-tag {
  display: flex; align-items: center; gap: 6rpx; padding: 12rpx 24rpx; border-radius: var(--radius-full);
  background: var(--bg-page); transition: all 0.3s; color: var(--text-secondary); border: none;
}
.filter-tag:active { transform: scale(0.95); }
.filter-tag--active { background: var(--brand-primary-light); color: var(--brand-primary); font-weight: 600; }
.filter-tag__text { font-size: 24rpx; white-space: nowrap; }
.filter-tag__badge {
  font-size: 20rpx; color: #fff; background: var(--color-warning); border-radius: var(--radius-full);
  padding: 2rpx 10rpx; min-width: 28rpx; text-align: center;
}
.filter-tag--notify-on { background: #ecfdf5; color: #10b981; font-weight: 600; }
.filter-tag--filter {
  background: var(--brand-primary-light);
  color: var(--brand-primary);
}

/* 孤岛折叠分类列 */
.bento-category-container {
  position: relative;
  display: flex;
  align-items: center;
  height: 100rpx;
  margin: 0 24rpx 24rpx;
  background: var(--bg-card);
  border-radius: 40rpx;
  box-shadow: 0 16rpx 48rpx rgba(0,0,0,0.03);
  z-index: 20;
}
.category-scroll { flex: 1; height: 100%; white-space: nowrap; }
::-webkit-scrollbar { display: none; width: 0; height: 0; color: transparent; }
.category-list {
  display: inline-flex; align-items: center; height: 100%; padding: 0 100rpx 0 24rpx; gap: 16rpx;
}
.category-pill {
  display: inline-flex; align-items: center; height: 60rpx; padding: 0 24rpx; border-radius: var(--radius-full);
  background: var(--bg-page); transition: all 0.3s; flex-shrink: 0;
}
.category-pill--active {
  background: var(--brand-primary-light);
  .category-pill__name, .category-pill__count { color: var(--brand-primary); font-weight: 600; }
}
.category-pill__name { font-size: 28rpx; color: var(--text-primary); font-weight: 500; font-family: system-ui, -apple-system; }
.category-pill__count { font-size: 24rpx; color: var(--text-tertiary); margin-left: 6rpx; background: rgba(0,0,0,0.04); padding: 4rpx 10rpx; border-radius: 20rpx; font-weight: 600;}

.category-expand-btn {
  position: absolute; right: 0; top: 0; width: 100rpx; height: 100rpx;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(90deg, rgba(255,255,255,0) 0%, rgba(255,255,255,1) 30%, #fff 100%); border-radius: 0 40rpx 40rpx 0;
}

/* ================== 分类全览弹窗 ================== */
.category-popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 999;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.category-popup {
  background: var(--bg-card);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  max-height: 75vh;
  display: flex;
  flex-direction: column;
}

.category-popup__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 32rpx 20rpx;
  
  .category-popup__title {
    font-size: 34rpx;
    font-weight: 700;
    color: var(--text-primary);
    letter-spacing: 1rpx;
  }
  
  .category-popup__close {
    font-size: 36rpx;
    color: var(--text-tertiary);
    padding: 10rpx;
  }
}

.category-popup__body {
  flex: 1;
  width: 100%;
  box-sizing: border-box; /* 必须加，否则 padding 会撑破 100% 宽度导致右侧内容跑到屏幕外 */
  padding: 0 32rpx 60rpx;
}

.category-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  padding-top: 10rpx;
}

.category-grid-item {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
  height: 64rpx;
  padding: 0 32rpx;
  border-radius: var(--radius-full);
  background: var(--border-light);
  border: 1.5rpx solid transparent;
  max-width: 100%;
  box-sizing: border-box;
  transition: all 0.2s;
  
  &--active {
    background: var(--brand-primary-light);
    border-color: var(--brand-primary);
    transform: scale(1.02);
    .category-grid-item__name, .category-grid-item__count { color: var(--brand-primary); /* handled below */ }
  }

  &__name {
    font-size: 28rpx;
    font-weight: 600;
    color: var(--text-primary);
    white-space: nowrap;
    flex-shrink: 0;
  }

  &__count {
    font-size: 24rpx;
    font-weight: 500;
    color: var(--text-secondary);
    margin-left: 6rpx;
    white-space: nowrap;
    flex-shrink: 0;
  }
}

.enterprise-tip {
  display: flex;
  align-items: center;
  margin: 16rpx 24rpx;
  padding: 20rpx 24rpx;
  background: #fff7e6;
  border: 1rpx solid #f3a73f;
  border-radius: 12rpx;
  gap: 12rpx;

  &__icon {
    font-size: 32rpx;
    flex-shrink: 0;
  }

  &__text {
    flex: 1;
    font-size: 26rpx;
    color: #d48806;
  }

  &__arrow {
    font-size: 32rpx;
    color: #d48806;
    flex-shrink: 0;
  }
}

/* 便当操作卡矩阵 (Bento Action) */
.bento-actions {
  display: flex; gap: 16rpx; margin-bottom: 24rpx; padding: 0 24rpx;
}
.bento-card {
  flex: 1; height: 140rpx; border-radius: 32rpx; background: var(--bg-page); border: none; letter-spacing: 1rpx;
  display: flex; flex-direction: column; align-items: center; justify-content: center; line-height: normal;
  transition: transform 0.2s; box-sizing: border-box;
}
button.bento-card::after { border: none; }
.bento-card:active { transform: scale(0.95); }

.bento-success { background: #ecfdf5; color: #059669; box-shadow: none; }
.bento-primary { background: var(--brand-primary-light); color: var(--brand-primary); box-shadow: none; }

.bento-text { font-size: 26rpx; font-weight: 600; color: inherit; }

.bento-icon-share { width: 44rpx; height: 44rpx; margin-bottom: 8rpx; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23059669' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8'/%3E%3Cpolyline points='16 6 12 2 8 6'/%3E%3Cline x1='12' y1='2' x2='12' y2='15'/%3E%3C/svg%3E"); background-size: cover; }
.bento-icon-folder { width: 44rpx; height: 44rpx; margin-bottom: 8rpx; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%234b5563' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z'/%3E%3C/svg%3E"); background-size: cover; }
.bento-icon-download { width: 44rpx; height: 44rpx; margin-bottom: 8rpx; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%234b5563' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'/%3E%3Cpolyline points='7 10 12 15 17 10'/%3E%3Cline x1='12' y1='15' x2='12' y2='3'/%3E%3C/svg%3E"); background-size: cover; }
.bento-icon-plus { width: 44rpx; height: 44rpx; margin-bottom: 8rpx; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%232979ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cline x1='12' y1='5' x2='12' y2='19'/%3E%3Cline x1='5' y1='12' x2='19' y2='12'/%3E%3C/svg%3E"); background-size: cover; }

/* 悬浮孤岛商品列 (Floating Product Islands) */
.product-list {
  padding: 0 24rpx 120rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.product-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  border-radius: 40rpx !important;
  background: var(--bg-card);
  box-shadow: 0 16rpx 48rpx rgba(0,0,0,0.03);
  transition: transform 0.2s;

  &__img-wrap {
    flex-shrink: 0;
    width: 160rpx;
    height: 160rpx;
    border-radius: 20rpx;
    overflow: hidden;
    background: var(--border-light);
  }

  &__img {
    width: 160rpx;
    height: 160rpx;
  }

  &__img-placeholder {
    width: 160rpx;
    height: 160rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 20rpx;
  }

  &__img-emoji {
    font-size: 48rpx;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    display: block;
    font-size: 32rpx;
    font-family: system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
    font-weight: 700;
    letter-spacing: 1rpx;
    color: var(--text-primary);
    margin-bottom: 12rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__spec {
    display: block;
    font-size: 26rpx;
    font-weight: 500;
    color: var(--text-tertiary);
    margin-bottom: 24rpx;
  }

  &__row {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__price {
    font-size: 34rpx;
    color: var(--color-danger);
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }

  &__stock {
    /* Styles handled by .saas-tag and .num-font */
  }
}

.btn-adjust {
  font-size: 28rpx;
  color: var(--brand-primary);
  background: rgba(41, 121, 255, 0.08); /* 极度通透 */
  padding: 14rpx 40rpx;
  border: none;
  font-weight: 700;
  border-radius: 40rpx;
  transition: all 0.2s;
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.btn-adjust-hover {
  transform: scale(0.92);
  background: rgba(41, 121, 255, 0.15);
}

.stock-status {
  display: flex; align-items: center; gap: 12rpx;
}
.stock-status__dot { width: 12rpx; height: 12rpx; border-radius: 50%; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.1); }
.stock-status__text { font-size: 26rpx; font-weight: 700; color: var(--text-secondary); }
.bg-danger { background: var(--color-danger); }
.bg-success { background: var(--color-success); }

/* =============== 弹窗全新重载 (Modal Refactor) =============== */
.popup-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 999;
}
.popup-content {
  width: 600rpx; background: var(--bg-card); border-radius: 48rpx;
  padding: 48rpx; box-shadow: 0 32rpx 64rpx rgba(0,0,0,0.15); box-sizing: border-box;
}
.popup-title { display: block; font-size: 36rpx; font-weight: 800; color: var(--text-primary); margin-bottom: 8rpx; }
.popup-stock-info { display: block; font-size: 24rpx; color: var(--text-tertiary); margin-bottom: 40rpx; letter-spacing: 1rpx; }

.popup-field { margin-bottom: 32rpx; }
.popup-label { display: block; font-size: 24rpx; font-weight: 600; color: var(--text-secondary); margin-bottom: 16rpx; }

/* 分段器 Segmented Control */
.adjust-segment-group {
  display: flex; background: var(--bg-page); border-radius: 20rpx; padding: 6rpx; gap: 6rpx;
}
.adjust-segment-btn {
  flex: 1; text-align: center; padding: 18rpx 0; border-radius: 16rpx;
  font-size: 28rpx; font-weight: 600; color: var(--text-tertiary); transition: all 0.3s;
}
.adjust-segment-btn.is-active { background: #fff; color: var(--text-primary); box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.06); }
.adjust-segment-btn.is-active.is-in { color: var(--brand-primary); }
.adjust-segment-btn.is-active.is-out { color: var(--color-danger); }

/* 加减步进器 Stepper */
.stepper-group {
  display: flex; align-items: center; justify-content: space-between;
  width: 100%; height: 110rpx; background: var(--bg-page); border-radius: 32rpx; padding: 0 16rpx;
  box-sizing: border-box;
}
.stepper-btn {
  width: 80rpx; height: 80rpx; border-radius: 24rpx; background: #fff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04); transition: transform 0.2s;
}
.stepper-btn--hover { transform: scale(0.9); box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.02); }
.stepper-icon-minus {
  width: 28rpx; height: 28rpx; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23333333' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cline x1='5' y1='12' x2='19' y2='12'%3E%3C/line%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center; opacity: 0.6;
}
.stepper-icon-plus {
  width: 28rpx; height: 28rpx; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23333333' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cline x1='12' y1='5' x2='12' y2='19'%3E%3C/line%3E%3Cline x1='5' y1='12' x2='19' y2='12'%3E%3C/line%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center; opacity: 0.8;
}
.stepper-input {
  flex: 1; text-align: center; height: 100%; border: none; background: transparent;
  font-size: 44rpx; font-weight: 800; color: var(--text-primary);
}

/* 极限不对称操作流 Action Bar */
.popup-actions-asymmetric {
  display: flex; align-items: center; justify-content: space-between; margin-top: 48rpx; padding-left: 12rpx;
}
.action-cancel-text { font-size: 30rpx; font-weight: 600; color: var(--text-tertiary); padding: 20rpx; transition: all 0.2s;}
.action-cancel-text:active { opacity: 0.5; }
.action-confirm-capsule {
  background: var(--brand-primary); border-radius: 40rpx; padding: 24rpx 64rpx;
  box-shadow: 0 12rpx 32rpx rgba(41, 121, 255, 0.3); transition: all 0.2s;
}
.action-confirm-capsule--hover { transform: scale(0.96); box-shadow: 0 6rpx 16rpx rgba(41, 121, 255, 0.2); }
.confirm-text { font-size: 30rpx; font-weight: 800; color: #fff; letter-spacing: 2rpx; }

/* SVG 图标补充 */
.svg-cat-icon { width: 64rpx; height: 64rpx; background-size: contain; background-repeat: no-repeat; background-position: center; opacity: 0.8; }

/* Header Tools Icons (Mask technique for perfectly inheriting active text color via currentColor or explicit bg-color) */
.svg-icon-tool {
  width: 28rpx; height: 28rpx; margin-right: 4rpx; display: inline-block;
  background-color: currentColor; 
  -webkit-mask-size: contain; -webkit-mask-position: center; -webkit-mask-repeat: no-repeat;
  mask-size: contain; mask-position: center; mask-repeat: no-repeat;
}

.svg-icon-action {
  width: 32rpx; height: 32rpx; margin-right: 2rpx; display: inline-block;
  -webkit-mask-size: contain; -webkit-mask-position: center; -webkit-mask-repeat: no-repeat;
  mask-size: contain; mask-position: center; mask-repeat: no-repeat;
}

.svg-icon-chevron-down { -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E"); }

/* Lucide SVGs */
.svg-icon-zap { -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolygon points='13 2 3 14 12 14 11 22 21 10 12 10 13 2'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolygon points='13 2 3 14 12 14 11 22 21 10 12 10 13 2'/%3E%3C/svg%3E"); }
.svg-icon-bell { -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9'/%3E%3Cpath d='M13.73 21a2 2 0 0 1-3.46 0'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9'/%3E%3Cpath d='M13.73 21a2 2 0 0 1-3.46 0'/%3E%3C/svg%3E"); }
.svg-icon-bell-off { -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M8.7 3A6 6 0 0 1 18 8a21.3 21.3 0 0 0 .6 5'/%3E%3Cpath d='M17 17H3s3-2 3-9a4.67 4.67 0 0 1 .3-1.7'/%3E%3Cpath d='M10.3 21a1.94 1.94 0 0 0 3.4 0'/%3E%3Cline x1='2' y1='2' x2='22' y2='22'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M8.7 3A6 6 0 0 1 18 8a21.3 21.3 0 0 0 .6 5'/%3E%3Cpath d='M17 17H3s3-2 3-9a4.67 4.67 0 0 1 .3-1.7'/%3E%3Cpath d='M10.3 21a1.94 1.94 0 0 0 3.4 0'/%3E%3Cline x1='2' y1='2' x2='22' y2='22'/%3E%3C/svg%3E"); }
.svg-icon-filter { -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolygon points='22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolygon points='22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3'/%3E%3C/svg%3E"); }
.svg-icon-share { -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8'/%3E%3Cpolyline points='16 6 12 2 8 6'/%3E%3Cline x1='12' y1='2' x2='12' y2='15'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8'/%3E%3Cpolyline points='16 6 12 2 8 6'/%3E%3Cline x1='12' y1='2' x2='12' y2='15'/%3E%3C/svg%3E"); }
.svg-icon-folder { -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolygon points='12 2 2 7 12 12 22 7 12 2'/%3E%3Cpolyline points='2 17 12 22 22 17'/%3E%3Cpolyline points='2 12 12 17 22 12'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolygon points='12 2 2 7 12 12 22 7 12 2'/%3E%3Cpolyline points='2 17 12 22 22 17'/%3E%3Cpolyline points='2 12 12 17 22 12'/%3E%3C/svg%3E"); }
.svg-icon-download { -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'/%3E%3Cpolyline points='7 10 12 15 17 10'/%3E%3Cline x1='12' y1='15' x2='12' y2='3'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'/%3E%3Cpolyline points='7 10 12 15 17 10'/%3E%3Cline x1='12' y1='15' x2='12' y2='3'/%3E%3C/svg%3E"); }
.svg-icon-plus { -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cline x1='12' y1='5' x2='12' y2='19'/%3E%3Cline x1='5' y1='12' x2='19' y2='12'/%3E%3C/svg%3E"); mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cline x1='12' y1='5' x2='12' y2='19'/%3E%3Cline x1='5' y1='12' x2='19' y2='12'/%3E%3C/svg%3E"); }

/* 筛选底部弹窗 */
.filter-sheet {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--bg-card);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  z-index: 1000;
}
.filter-sheet__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 32rpx 20rpx;
}
.filter-sheet__title {
  font-size: 34rpx;
  font-weight: 700;
  color: var(--text-primary);
}
.filter-sheet__close {
  font-size: 36rpx;
  color: var(--text-tertiary);
  padding: 8rpx;
}
.filter-sheet__body {
  flex: 1;
  padding: 16rpx 32rpx;
  overflow-y: auto;
}
.filter-section {
  margin-bottom: 32rpx;
}
.filter-section__label {
  display: block;
  font-size: 26rpx;
  color: var(--text-secondary);
  margin-bottom: 20rpx;
  font-weight: 600;
}
.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.filter-chip {
  padding: 16rpx 32rpx;
  border-radius: var(--radius-full);
  font-size: 26rpx;
  color: var(--text-secondary);
  background: var(--bg-page);
  border: 2rpx solid transparent;
  transition: all 0.2s;
  
  &:active {
    transform: scale(0.92);
  }
}
.filter-chip--active {
  background: var(--brand-primary-light);
  color: var(--brand-primary);
  border-color: var(--brand-primary);
  font-weight: 600;
}
.filter-custom-range {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 20rpx;
}
.filter-range-input {
  flex: 1;
  height: 72rpx;
  border: none;
  background: var(--bg-page);
  color: var(--text-primary);
  border-radius: var(--radius-full);
  padding: 0 24rpx;
  font-size: 28rpx;
  text-align: center;
  box-sizing: border-box;
}
.filter-range-sep {
  font-size: 26rpx;
  color: var(--text-tertiary);
}
.filter-sheet__footer {
  display: flex;
  gap: 24rpx;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  border-top: none;
  background: var(--bg-card);
  box-shadow: 0 -8rpx 24rpx rgba(0,0,0,0.04);
}
.filter-sheet__btn {
  text-align: center;
  padding: 24rpx 0;
  border-radius: var(--radius-full);
  font-size: 30rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  
  &:active {
    transform: scale(0.96);
  }
}
.filter-sheet__btn--reset {
  flex: 1;
  background: var(--bg-page);
  color: var(--text-secondary);
}
.filter-sheet__btn--confirm {
  flex: 2;
  background: var(--brand-primary);
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(41, 121, 255, 0.25);
}

.filter-tag--filter {
  background: var(--brand-primary-light);
  color: var(--brand-primary);
}

/* ================== 动效与骨架屏 (Animations & Skeleton) ================== */
.skeleton-img { width: 120rpx; height: 120rpx; border-radius: 12rpx; flex-shrink: 0; }
.skeleton-shimmer {
  background: linear-gradient(90deg, #f3f4f6 25%, #e5e7eb 50%, #f3f4f6 75%);
  background-size: 400% 100%;
  animation: shimmer 1.5s infinite linear;
}
@keyframes shimmer { 
  0% { background-position: 100% 0; } 
  100% { background-position: -100% 0; } 
}



.popup-mask, .category-popup-mask {
  animation: fadeIn 0.3s ease-out both;
}
.popup-content, .filter-sheet, .category-popup {
  animation: slideUpSpring 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.2) both;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes slideUpSpring {
  from { transform: translateY(100%); opacity: 0.5; }
  to { transform: translateY(0); opacity: 1; }
}

</style>
