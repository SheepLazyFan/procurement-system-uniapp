<template>
  <view class="page-supplier-detail container">
    <!-- 基本信息卡片 -->
    <view class="saas-card">
      <text class="supplier-name">{{ supplier.name }}</text>
      <view class="info-row">
        <text class="info-label">电话</text>
        <text class="info-value">{{ supplier.phone || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">地址</text>
        <text class="info-value">{{ supplier.address || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">主营品类</text>
        <text class="info-value">{{ supplier.mainCategory || '-' }}</text>
      </view>
      <view class="info-row" v-if="supplier.remark">
        <text class="info-label">备注</text>
        <text class="info-value">{{ supplier.remark }}</text>
      </view>
    </view>

    <!-- 统计卡片 -->
    <view class="saas-card">
      <view class="stats-row">
        <view class="stats-item">
          <text class="stats-value">{{ supplier.purchaseCount || 0 }}</text>
          <text class="stats-label">采购次数</text>
        </view>
        <view class="stats-item">
          <text class="stats-value price-text">¥{{ supplier.totalAmount || 0 }}</text>
          <text class="stats-label">累计金额</text>
        </view>
      </view>
    </view>

    <!-- ===== 订单与商品 统一超级容器 ===== -->
    <view class="saas-card unified-tabs-card">
      <!-- Tab 栏 -->
      <view class="tab-bar">
        <view class="tab-item" :class="{ 'tab-item--active': activeTab === 'orders' }" @tap="switchTab('orders')">
          采购记录
        </view>
        <view class="tab-item" :class="{ 'tab-item--active': activeTab === 'products' }" @tap="switchTab('products')">
          关联商品
          <text class="tab-badge" v-if="productPage.total > 0">{{ productPage.total }}</text>
        </view>
      </view>

      <!-- ===== 采购记录 Tab ===== -->
      <view v-if="activeTab === 'orders'" class="tab-content">
        <view v-for="order in supplier.recentOrders" :key="order.id" class="order-card"
          :class="{ 'order-card--expanded': expandedOrders[order.id] }">
          <!-- 收起行 -->
          <view class="order-card__header" @tap="toggleOrder(order.id)">
            <text class="order-card__time">{{ formatTime(order.createdAt) }}</text>
            <text class="order-card__status" :class="'status--' + order.status">{{ statusLabel(order.status) }}</text>
            <text class="order-card__amount num-font">¥{{ order.totalAmount }}</text>
            <text class="order-card__arrow">{{ expandedOrders[order.id] ? '∧' : '∨' }}</text>
          </view>
          <!-- 展开区 -->
          <view v-if="expandedOrders[order.id]" class="order-card__detail">
            <view class="detail-row" @tap.stop="goOrderDetail(order.id)">
              <text class="detail-label">订单号</text>
              <text class="detail-value detail-value--link">{{ order.orderNo }} ›</text>
            </view>
            <view class="detail-row">
              <text class="detail-label">商品摘要</text>
              <text class="detail-value">{{ order.itemSummary || '-' }}</text>
            </view>
          </view>
        </view>
        <EmptyState v-if="!supplier.recentOrders || supplier.recentOrders.length === 0" text="暂无采购记录" />
      </view>

      <!-- ===== 关联商品 Tab ===== -->
      <view v-if="activeTab === 'products'">
        <!-- 搜索 + 每页条数 + 添加按钮 -->
        <view class="product-filter-bar">
          <view class="filter-search">
            <text style="margin-right: 12rpx; color: var(--text-tertiary);">🔍</text>
            <input
              class="filter-search-input"
              v-model="pKeyword"
              placeholder="搜索商品名"
              @confirm="onProductSearch"
            />
          </view>
          <view class="filter-pagesize" @tap="showPageSizeSheet">
            <text class="filter-pagesize-text">{{ pPageSize }}条/页 ▾</text>
          </view>
          <view class="btn-add-product" @tap="openAddProductPopup">+添加</view>
        </view>

        <!-- 分类横向筛选 -->
        <scroll-view scroll-x class="category-scroll">
          <view class="category-tabs">
            <view class="category-tab" :class="{ 'category-tab--active': pCategoryId === null }"
              @tap="onCategoryFilter(null)">全部</view>
            <view v-for="cat in categoryList" :key="cat.id"
              class="category-tab" :class="{ 'category-tab--active': pCategoryId === cat.id }"
              @tap="onCategoryFilter(cat.id)">{{ (cat.name || '').replace(/\s+/g, '') }}</view>
          </view>
        </scroll-view>

        <!-- 商品列表 -->
        <view class="linked-product-list">
          <view v-if="pLoading" class="loading-tip"><text>加载中...</text></view>
          <template v-else>
            <view v-for="item in linkedProducts" :key="item.productId" class="linked-product-row">
              <view class="linked-product-row__main">
                <text class="linked-product-row__name">{{ item.productName }}</text>
                <text class="linked-product-row__sub" v-if="item.spec">{{ item.spec }}</text>
                <view class="linked-product-row__tags">
                  <text v-if="item.categoryName" class="saas-tag saas-tag-info" style="margin-right: 8rpx">{{ (item.categoryName || '').replace(/\s+/g, '') }}</text>
                  <text v-if="item.unit" class="saas-tag saas-tag-info">{{ item.unit }}</text>
                </view>
              </view>
              <view class="linked-product-row__right">
                <view class="linked-product-row__prices">
                  <text class="linked-product-row__supply-price num-font">供货价 ¥{{ item.supplyPrice }}</text>
                  <text class="linked-product-row__stock">库存 {{ item.stock }}</text>
                </view>
                <view class="linked-product-row__actions">
                  <text class="action-btn action-btn--edit" @tap="openEditPricePopup(item)">编辑</text>
                  <text class="action-btn action-btn--del" @tap="handleUnbind(item)">解绑</text>
                </view>
              </view>
            </view>
            <EmptyState v-if="linkedProducts.length === 0" text="暂无关联商品，点击「+添加」绑定" />
          </template>
        </view>

        <!-- 分页控件 -->
        <view class="pagination" v-if="productPage.total > pPageSize">
          <text class="page-btn" :class="{ 'page-btn--disabled': productPage.pageNum <= 1 }"
            @tap="onPageChange(-1)">‹ 上一页</text>
          <text class="page-info">{{ productPage.pageNum }} / {{ totalPages }}</text>
          <text class="page-btn" :class="{ 'page-btn--disabled': productPage.pageNum >= totalPages }"
            @tap="onPageChange(1)">下一页 ›</text>
        </view>
      </view>
    </view>

    <!-- 底部悬浮操作舱 -->
    <view class="bottom-action-bar">
      <view class="bottom-btn bottom-btn--danger" @tap="handleDelete">删除供应商</view>
      <view class="bottom-btn bottom-btn--primary" @tap="showEditForm">编辑信息</view>
    </view>

    <!-- ===== 编辑供应商弹窗（含 remark） ===== -->
    <view class="popup-mask" v-if="showEditPopup" @tap="showEditPopup = false">
      <view class="popup-content" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">编辑供应商</text>
          <text class="popup-close" @tap="showEditPopup = false">✕</text>
        </view>
        <scroll-view scroll-y class="popup-form">
          <view class="form-group">
            <text class="form-label">名称 *</text>
            <input v-model="editForm.name" placeholder="供应商名称" class="form-input" />
          </view>
          <view class="form-group">
            <text class="form-label">电话</text>
            <input v-model="editForm.phone" placeholder="联系电话" class="form-input" />
          </view>
          <view class="form-group">
            <text class="form-label">地址</text>
            <input v-model="editForm.address" placeholder="地址" class="form-input" />
          </view>
          <view class="form-group">
            <text class="form-label">主营品类</text>
            <input v-model="editForm.mainCategory" placeholder="主营品类" class="form-input" />
          </view>
          <view class="form-group">
            <text class="form-label">备注</text>
            <textarea v-model="editForm.remark" placeholder="可选填写备注" class="form-textarea" />
          </view>
          <button class="btn-primary" @tap="handleEdit">保存修改</button>
        </scroll-view>
      </view>
    </view>

    <!-- ===== 添加关联商品弹窗 ===== -->
    <view class="popup-mask" v-if="showAddProductPopup" @tap="closeAddProductPopup">
      <view class="popup-content popup-content--tall" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">添加关联商品</text>
          <text class="popup-close" @tap="closeAddProductPopup">✕</text>
        </view>
        <view class="popup-search">
          <input class="popup-search-input" v-model="addProductKeyword"
            placeholder="输入商品名称搜索" @confirm="loadAddProductList" />
          <text class="popup-search-btn" @tap="loadAddProductList">搜索</text>
        </view>
        <scroll-view scroll-x class="category-scroll" style="margin: 0; padding: 0 32rpx;">
          <view class="category-tabs">
            <view class="category-tab" :class="{ 'category-tab--active': addCategoryId === null }"
              @tap="onAddCategoryFilter(null)">全部</view>
            <view v-for="cat in categoryList" :key="cat.id"
              class="category-tab" :class="{ 'category-tab--active': addCategoryId === cat.id }"
              @tap="onAddCategoryFilter(cat.id)">{{ (cat.name || '').replace(/\s+/g, '') }}</view>
          </view>
        </scroll-view>
        <scroll-view scroll-y class="popup-list">
          <view v-if="addProductLoading" class="popup-loading"><text>加载中...</text></view>
          <view v-else-if="addProductList.length === 0" class="popup-empty">
            <text>{{ addProductKeyword ? '未找到商品' : '暂无商品' }}</text>
          </view>
          <view v-else v-for="p in addProductList" :key="p.id"
            class="add-product-row"
            :class="{
              'add-product-row--selected': selectedAddMap[p.id] !== undefined,
              'add-product-row--bound': alreadyBoundIds.has(p.id)
            }"
            @tap="toggleAddSelection(p)">
            <view class="add-product-row__check">
              <text v-if="alreadyBoundIds.has(p.id)" class="check-bound">已绑</text>
              <text v-else-if="selectedAddMap[p.id] !== undefined" class="check-selected">✓</text>
              <text v-else class="check-empty">○</text>
            </view>
            <view class="add-product-row__info">
              <text class="add-product-row__name">{{ p.name }}</text>
              <text class="add-product-row__sub" v-if="p.spec || p.categoryName">
                {{ [(p.categoryName || '').replace(/\s+/g, ''), p.spec].filter(Boolean).join(' · ') }}
              </text>
              <text class="add-product-row__stock">库存: {{ p.stock || 0 }}</text>
            </view>
            <!-- 选中时：显示供货价输入框（每选一个填一次） -->
            <view class="add-product-row__price" v-if="selectedAddMap[p.id] !== undefined" @tap.stop>
              <text class="add-price-label">供货价 *</text>
              <input
                class="add-price-input"
                type="digit"
                placeholder="必填"
                :value="selectedAddMap[p.id]"
                @input="onAddPriceInput(p.id, $event)"
              />
            </view>
          </view>
        </scroll-view>
        <view class="popup-footer">
          <text class="popup-footer-count">已选 {{ selectedAddCount }} 件</text>
          <button class="btn-primary btn-confirm-add" @tap="confirmBindProducts"
            :disabled="selectedAddCount === 0">确认添加</button>
        </view>
      </view>
    </view>

    <!-- ===== 编辑供货价弹窗 ===== -->
    <view class="popup-mask" v-if="showEditPricePopup" @tap="showEditPricePopup = false">
      <view class="popup-content popup-content--small" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">编辑供货价</text>
          <text class="popup-close" @tap="showEditPricePopup = false">✕</text>
        </view>
        <view class="popup-form">
          <view class="form-group">
            <text class="form-label">商品</text>
            <text class="form-value-text">{{ editPriceForm.productName }}</text>
          </view>
          <view class="form-group">
            <text class="form-label">新供货价 *</text>
            <input
              v-model="editPriceForm.supplyPrice"
              type="digit"
              placeholder="请输入供货价"
              class="form-input"
            />
          </view>
          <view class="popup-btn-row">
            <button class="btn-cancel" @tap="showEditPricePopup = false">取消</button>
            <button class="btn-primary btn-confirm" @tap="confirmEditPrice">保存</button>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import {
  getSupplierDetail, updateSupplier, deleteSupplier,
  getSupplierProducts, bindSupplierProducts, unbindSupplierProduct, updateSupplierProductPrice
} from '@/api/supplier'
import { getProductList } from '@/api/product'
import { getCategoryList } from '@/api/category'
import EmptyState from '@/components/common/EmptyState.vue'

export default {
  components: { EmptyState },
  data() {
    return {
      supplierId: null,
      supplier: {},
      // 采购记录 tab
      expandedOrders: {},
      // Tab 状态
      activeTab: 'orders',
      // 关联商品 tab
      linkedProducts: [],
      productPage: { pageNum: 1, total: 0 },
      pPageSize: 10,
      pKeyword: '',
      pCategoryId: null,
      pLoading: false,
      categoryList: [],
      // 编辑供应商弹窗
      showEditPopup: false,
      editForm: { name: '', phone: '', address: '', mainCategory: '', remark: '' },
      // 添加商品弹窗
      showAddProductPopup: false,
      addProductList: [],
      addProductLoading: false,
      addProductKeyword: '',
      addCategoryId: null,
      selectedAddMap: {},      // { productId: supplyPriceStr }
      alreadyBoundIds: new Set(),
      // 编辑供货价弹窗
      showEditPricePopup: false,
      editPriceForm: { productId: null, productName: '', supplyPrice: '' }
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.productPage.total / this.pPageSize))
    },
    selectedAddCount() {
      return Object.keys(this.selectedAddMap).length
    }
  },
  onLoad(query) {
    this.supplierId = Number(query.id)
    this.loadDetail()
    this.loadCategories()
  },
  methods: {
    // ===== 基础数据 =====
    async loadDetail() {
      try {
        this.supplier = await getSupplierDetail(this.supplierId)
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    async loadCategories() {
      if (this.categoryList.length > 0) return
      try {
        const res = await getCategoryList()
        this.categoryList = res || []
      } catch (e) {
        console.warn('[supplier-detail] 分类加载失败', e)
      }
    },

    // ===== Tab 切换 =====
    switchTab(tab) {
      if (this.activeTab === tab) return
      this.activeTab = tab
      if (tab === 'products' && this.linkedProducts.length === 0) {
        this.loadLinkedProducts()
      }
    },

    // ===== 关联商品 Tab =====
    async loadLinkedProducts() {
      this.pLoading = true
      try {
        const res = await getSupplierProducts(this.supplierId, {
          pageNum: this.productPage.pageNum,
          pageSize: this.pPageSize,
          keyword: this.pKeyword.trim() || undefined,
          categoryId: this.pCategoryId || undefined
        })
        this.linkedProducts = res?.records || []
        this.productPage.total = res?.total || 0
      } catch (e) {
        uni.showToast({ title: '加载关联商品失败', icon: 'none' })
      } finally {
        this.pLoading = false
      }
    },
    onProductSearch() {
      this.productPage.pageNum = 1
      this.loadLinkedProducts()
    },
    onCategoryFilter(categoryId) {
      this.pCategoryId = categoryId
      this.productPage.pageNum = 1
      this.loadLinkedProducts()
    },
    onPageChange(direction) {
      const next = this.productPage.pageNum + direction
      if (next < 1 || next > this.totalPages) return
      this.productPage.pageNum = next
      this.loadLinkedProducts()
    },
    showPageSizeSheet() {
      uni.showActionSheet({
        itemList: ['10 条/页', '15 条/页', '20 条/页', '40 条/页'],
        success: (res) => {
          this.pPageSize = [10, 15, 20, 40][res.tapIndex]
          this.productPage.pageNum = 1
          this.loadLinkedProducts()
        }
      })
    },

    // ===== 添加商品弹窗 =====
    async openAddProductPopup() {
      this.showAddProductPopup = true
      this.selectedAddMap = {}
      this.addProductKeyword = ''
      this.addCategoryId = null
      // 先获取已绑定的商品 ID 集合，用于标记"已绑"状态
      try {
        const res = await getSupplierProducts(this.supplierId, { pageNum: 1, pageSize: 1000 })
        this.alreadyBoundIds = new Set((res?.records || []).map(r => r.productId))
      } catch (e) {
        this.alreadyBoundIds = new Set()
      }
      this.loadAddProductList()
    },
    closeAddProductPopup() {
      this.showAddProductPopup = false
    },
    async loadAddProductList() {
      this.addProductLoading = true
      try {
        const params = { pageNum: 1, pageSize: 50 }
        if (this.addProductKeyword.trim()) params.keyword = this.addProductKeyword.trim()
        if (this.addCategoryId) params.categoryId = this.addCategoryId
        const res = await getProductList(params)
        this.addProductList = res?.records || res || []
      } catch (e) {
        uni.showToast({ title: '加载商品失败', icon: 'none' })
      } finally {
        this.addProductLoading = false
      }
    },
    onAddCategoryFilter(categoryId) {
      this.addCategoryId = categoryId
      this.loadAddProductList()
    },
    toggleAddSelection(product) {
      // 已绑定商品不可选择
      if (this.alreadyBoundIds.has(product.id)) return
      const map = { ...this.selectedAddMap }
      if (map[product.id] !== undefined) {
        delete map[product.id]
      } else {
        // 选中时初始化价格为空字符串，必须用户填写
        map[product.id] = ''
      }
      this.selectedAddMap = map
    },
    onAddPriceInput(productId, event) {
      this.selectedAddMap = { ...this.selectedAddMap, [productId]: event.detail.value }
    },
    async confirmBindProducts() {
      const entries = Object.entries(this.selectedAddMap)
      if (entries.length === 0) return

      // 校验所有选中项都填了供货价
      const hasMissing = entries.some(([, price]) => price === '' || price === null || price === undefined)
      if (hasMissing) {
        return uni.showToast({ title: '请填写所有已选商品的供货价', icon: 'none' })
      }

      // 解析并校验格式
      const items = entries.map(([id, price]) => ({
        productId: Number(id),
        supplyPrice: parseFloat(price)
      }))
      if (items.some(i => isNaN(i.supplyPrice) || i.supplyPrice < 0)) {
        return uni.showToast({ title: '供货价格式不正确', icon: 'none' })
      }

      try {
        await bindSupplierProducts(this.supplierId, { items })
        uni.showToast({ title: `已绑定 ${items.length} 件商品`, icon: 'success' })
        this.showAddProductPopup = false
        // 刷新关联商品列表（回到第一页确保看到新增的）
        this.productPage.pageNum = 1
        this.loadLinkedProducts()
      } catch (e) {
        const msg = e?.data?.message || e?.message || '绑定失败'
        uni.showToast({ title: msg, icon: 'none' })
      }
    },

    // ===== 编辑供货价 =====
    openEditPricePopup(item) {
      this.editPriceForm = {
        productId: item.productId,
        productName: item.productName,
        supplyPrice: String(item.supplyPrice ?? '')
      }
      this.showEditPricePopup = true
    },
    async confirmEditPrice() {
      const price = parseFloat(this.editPriceForm.supplyPrice)
      if (isNaN(price) || price < 0) {
        return uni.showToast({ title: '请输入正确的供货价', icon: 'none' })
      }
      try {
        await updateSupplierProductPrice(
          this.supplierId,
          this.editPriceForm.productId,
          { supplyPrice: price }
        )
        uni.showToast({ title: '供货价已更新', icon: 'success' })
        this.showEditPricePopup = false
        // 本地刷新，避免重新请求
        const item = this.linkedProducts.find(p => p.productId === this.editPriceForm.productId)
        if (item) item.supplyPrice = price
      } catch (e) {
        uni.showToast({ title: '更新失败', icon: 'none' })
      }
    },

    // ===== 解绑 =====
    handleUnbind(item) {
      const count = item.supplierCount || 1
      const contentMsg = count <= 1
        ? `「${item.productName}」目前只绑定该供应商，解绑后将无供应商记录，确认解绑？`
        : `「${item.productName}」共关联 ${count} 个供应商，确认从当前供应商解绑？`
      uni.showModal({
        title: '确认解绑',
        content: contentMsg,
        confirmColor: '#e43d33',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await unbindSupplierProduct(this.supplierId, item.productId)
            uni.showToast({ title: '已解绑', icon: 'success' })
            this.productPage.total = Math.max(0, this.productPage.total - 1)
            this.linkedProducts = this.linkedProducts.filter(p => p.productId !== item.productId)
            // 若当前页变空且不是第一页，跳到上一页
            if (this.linkedProducts.length === 0 && this.productPage.pageNum > 1) {
              this.productPage.pageNum -= 1
              this.loadLinkedProducts()
            }
          } catch (e) {
            uni.showToast({ title: '解绑失败', icon: 'none' })
          }
        }
      })
    },

    // ===== 编辑供应商 =====
    showEditForm() {
      this.editForm = {
        name: this.supplier.name || '',
        phone: this.supplier.phone || '',
        address: this.supplier.address || '',
        mainCategory: this.supplier.mainCategory || '',
        remark: this.supplier.remark || ''
      }
      this.showEditPopup = true
    },
    async handleEdit() {
      if (!this.editForm.name.trim()) {
        return uni.showToast({ title: '请输入供应商名称', icon: 'none' })
      }
      try {
        await updateSupplier(this.supplierId, this.editForm)
        uni.showToast({ title: '修改成功', icon: 'success' })
        this.showEditPopup = false
        this.loadDetail()
      } catch (e) {
        uni.showToast({ title: '修改失败', icon: 'none' })
      }
    },
    handleDelete() {
      uni.showModal({
        title: '确认删除',
        content: `确定删除供应商「${this.supplier.name}」吗？删除后不可恢复。`,
        confirmColor: '#e43d33',
        success: async (res) => {
          if (res.confirm) {
            try {
              await deleteSupplier(this.supplierId)
              uni.showToast({ title: '删除成功', icon: 'success' })
              setTimeout(() => uni.navigateBack(), 1000)
            } catch (e) {
              uni.showToast({ title: '删除失败', icon: 'none' })
            }
          }
        }
      })
    },

    // ===== 采购记录 =====
    goOrderDetail(id) {
      uni.navigateTo({ url: `/pages/purchase/detail?id=${id}` })
    },
    toggleOrder(orderId) {
      this.expandedOrders = { ...this.expandedOrders, [orderId]: !this.expandedOrders[orderId] }
    },
    formatTime(dateStr) {
      if (!dateStr) return ''
      const d = new Date(dateStr.replace(' ', 'T'))
      if (isNaN(d.getTime())) return dateStr
      const M = String(d.getMonth() + 1).padStart(2, '0')
      const D = String(d.getDate()).padStart(2, '0')
      const h = String(d.getHours()).padStart(2, '0')
      const m = String(d.getMinutes()).padStart(2, '0')
      return `${M}-${D} ${h}:${m}`
    },
    statusLabel(status) {
      const map = {
        PENDING: '待处理', PURCHASING: '采购中',
        ARRIVED: '已到货', COMPLETED: '已完成', CANCELLED: '已取消'
      }
      return map[status] || status || '-'
    }
  }
}
</script>

<style lang="scss" scoped>
.page-supplier-detail {
  padding-bottom: 180rpx;
}

.supplier-name {
  font-size: 38rpx;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 24rpx;
  display: block;
}
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
  border-bottom: 1rpx solid var(--border-light);
  &:last-child { border-bottom: none; }
}
.info-label { font-size: 26rpx; color: var(--text-tertiary); flex-shrink: 0; }
.info-value { font-size: 28rpx; color: var(--text-primary); font-weight: 500; text-align: right; flex: 1; margin-left: 20rpx; }

.stats-row { display: flex; justify-content: space-around; padding: 12rpx 0;}
.stats-item { text-align: center; }
.stats-value { display: block; font-size: 44rpx; font-weight: 700; color: var(--text-primary); font-family: 'SF Pro Display', sans-serif;}
.stats-label { font-size: 24rpx; color: var(--text-secondary); margin-top: 8rpx; display: block; }
.price-text { color: var(--brand-primary); }

/* 核心融合容器 */
.unified-tabs-card {
  padding: 0;
  padding-bottom: 40rpx;
  overflow: hidden;
}

/* Tab 栏 */
.tab-bar {
  display: flex;
  border-bottom: 1rpx solid var(--border-light);
  margin-bottom: 24rpx;
}
.tab-item {
  flex: 1;
  text-align: center;
  padding: 26rpx 0;
  font-size: 30rpx;
  color: var(--text-secondary);
  position: relative;
  transition: all 0.2s;
  &--active {
    color: var(--brand-primary);
    font-weight: 600;
    &::after {
      content: '';
      position: absolute;
      bottom: 0; left: 30%; right: 30%;
      height: 6rpx;
      background: var(--brand-primary);
      border-radius: 6rpx 6rpx 0 0;
    }
  }
}
.tab-badge {
  display: inline-block;
  background: var(--color-danger);
  color: #fff;
  font-size: 20rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  border-radius: 16rpx;
  text-align: center;
  padding: 0 8rpx;
  margin-left: 8rpx;
  vertical-align: middle;
}

.tab-content { padding: 0 32rpx; }

/* 关联商品筛选栏 */
.product-filter-bar {
  display: flex;
  align-items: center;
  padding: 0 32rpx 20rpx;
  gap: 16rpx;
}
.filter-search {
  flex: 1;
  display: flex;
  align-items: center;
  background: var(--bg-page);
  border-radius: var(--radius-full);
  padding: 0 24rpx;
  height: 72rpx;
}
.filter-search-input { flex: 1; height: 72rpx; font-size: 28rpx; background: transparent; color: var(--text-primary); }
.filter-pagesize { flex-shrink: 0; }
.filter-pagesize-text {
  font-size: 26rpx;
  color: var(--text-secondary);
  padding: 12rpx 20rpx;
  background: var(--bg-page);
  border-radius: var(--radius-sm);
}
.btn-add-product {
  flex-shrink: 0;
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 32rpx;
  background: var(--brand-primary-light);
  color: var(--brand-primary);
  border-radius: var(--radius-full);
  font-size: 28rpx;
  font-weight: 600;
  transition: all 0.2s;
  &:active { transform: scale(0.92); opacity: 0.8; }
}

/* 分类横向筛选 */
.category-scroll {
  white-space: nowrap;
  padding: 0 32rpx;
  margin-bottom: 20rpx;
}
.category-tabs {
  display: inline-flex;
  gap: 16rpx;
}
.category-tab {
  display: inline-flex;
  align-items: center;
  padding: 12rpx 32rpx;
  background: var(--bg-page);
  border-radius: var(--radius-full);
  font-size: 26rpx;
  color: var(--text-secondary);
  flex-shrink: 0;
  transition: all 0.2s;
  &--active { background: var(--brand-primary-light); color: var(--brand-primary); font-weight: 600; }
}

/* 关联商品列表行 */
.linked-product-list {
  padding: 0 32rpx;
}
.linked-product-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid var(--border-light);
  &:last-child { border-bottom: none; }
  &__main { flex: 1; min-width: 0; overflow: hidden; }
  &__name {
    font-size: 30rpx; color: var(--text-primary); font-weight: 600;
    display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  }
  &__sub { font-size: 24rpx; color: var(--text-tertiary); display: block; margin-top: 8rpx; }
  &__tags { display: flex; gap: 12rpx; margin-top: 12rpx; flex-wrap: wrap; }
  &__right {
    flex: 0 0 auto;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 16rpx;
    margin-left: 20rpx;
  }
  &__prices { text-align: right; }
  &__supply-price { display: block; font-size: 30rpx; font-weight: 700; color: var(--color-danger); white-space: nowrap; }
  &__stock { display: block; font-size: 22rpx; color: var(--text-tertiary); white-space: nowrap; margin-top: 4rpx; }
  &__actions { display: flex; gap: 16rpx; }
}

.action-btn {
  font-size: 24rpx; padding: 8rpx 20rpx; border-radius: var(--radius-full); font-weight: 500;
  transition: all 0.2s;
  &:active { transform: scale(0.9); }
  &--edit { color: var(--brand-primary); background: var(--bg-page); }
  &--del  { color: var(--color-danger); background: var(--bg-page); }
}

/* 分页控件 */
.pagination {
  display: flex; justify-content: center; align-items: center;
  gap: 32rpx; padding: 24rpx 0;
}
.page-btn { font-size: 26rpx; color: var(--brand-primary); padding: 8rpx 16rpx; font-weight: 500; &--disabled { color: var(--text-tertiary); } }
.page-info { font-size: 24rpx; color: var(--text-tertiary); }

/* 底部悬浮舱 */
.bottom-action-bar {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  display: flex;
  padding: 24rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
  background: var(--bg-card);
  gap: 24rpx;
  box-shadow: 0 -8rpx 24rpx rgba(0, 0, 0, 0.04);
  z-index: 100;
}
.bottom-btn {
  flex: 1;
  height: 88rpx; display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-full);
  font-size: 30rpx; font-weight: 600;
  transition: all 0.2s;
  
  &:active { transform: scale(0.96); }
  
  &--danger { background: var(--bg-page); color: var(--color-danger); }
  &--primary { flex: 2; background: var(--brand-primary); color: #fff; box-shadow: 0 8rpx 24rpx rgba(41, 121, 255, 0.25); }
}

/* 弹窗基础样式 */
.popup-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); z-index: 999;
  display: flex; align-items: flex-end;
}
.popup-content {
  width: 100%; max-height: 75vh;
  background: #fff; border-radius: 24rpx 24rpx 0 0;
  display: flex; flex-direction: column;
  padding-bottom: env(safe-area-inset-bottom);
  &--tall { max-height: 85vh; }
  &--small { max-height: 60vh; }
}
.popup-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx 32rpx; border-bottom: 1rpx solid #f0f0f0; flex-shrink: 0;
}
.popup-title { font-size: 32rpx; font-weight: 600; }
.popup-close { font-size: 36rpx; color: #999; padding: 8rpx; }
.popup-search {
  display: flex; align-items: center;
  padding: 16rpx 32rpx; gap: 16rpx; flex-shrink: 0;
}
.popup-search-input {
  flex: 1; height: 64rpx; background: #f5f6fa;
  border-radius: 32rpx; padding: 0 24rpx; font-size: 26rpx;
}
.popup-search-btn { color: #2979ff; font-size: 28rpx; padding: 8rpx 16rpx; }
.popup-list { flex: 1; overflow-y: auto; padding: 0 32rpx; }
.popup-loading, .popup-empty {
  text-align: center; padding: 40rpx 0; color: #999; font-size: 26rpx;
}

/* 添加商品列表行 */
.add-product-row {
  display: flex; align-items: flex-start;
  padding: 20rpx 0; border-bottom: 1rpx solid #f5f5f5; gap: 12rpx;
  &--selected { background: #f0f7ff; border-radius: 8rpx; padding: 20rpx 12rpx; margin: 4rpx 0; }
  &--bound { opacity: 0.5; }
  &__check { width: 44rpx; flex: 0 0 44rpx; text-align: center; padding-top: 4rpx; }
  &__info { flex: 1; min-width: 0; overflow: hidden; }
  &__name {
    font-size: 28rpx; color: #333; font-weight: 500;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block;
  }
  &__sub, &__stock { font-size: 22rpx; color: #999; display: block; margin-top: 4rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  &__price { flex: 0 0 auto; display: flex; flex-direction: column; align-items: flex-end; gap: 4rpx; }
}
.check-selected { color: #2979ff; font-size: 30rpx; font-weight: 700; }
.check-empty { color: #ccc; font-size: 30rpx; }
.check-bound {
  font-size: 20rpx; color: #999; background: #f0f0f0;
  padding: 2rpx 8rpx; border-radius: 8rpx;
}
.add-price-label { font-size: 22rpx; color: #666; }
.add-price-input {
  width: 160rpx; height: 56rpx; background: #fff;
  border: 1rpx solid #2979ff; border-radius: 8rpx;
  text-align: center; font-size: 26rpx; padding: 0 8rpx;
}
.popup-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 32rpx; border-top: 1rpx solid #f0f0f0; flex-shrink: 0;
}
.popup-footer-count { font-size: 26rpx; color: #666; }
.btn-confirm-add { height: 72rpx; line-height: 72rpx; padding: 0 40rpx; font-size: 28rpx; }

/* 编辑弹窗表单 */
.popup-form { padding: 24rpx 32rpx; flex: 1; overflow-y: auto; }
.form-group { margin-bottom: 24rpx; }
.form-label { display: block; font-size: 26rpx; color: #666; margin-bottom: 12rpx; }
.form-value-text { font-size: 28rpx; color: #333; font-weight: 500; }
.form-input {
  width: 100%; height: 80rpx; background: #f5f6fa;
  border-radius: 12rpx; padding: 0 20rpx; font-size: 28rpx; box-sizing: border-box;
}
.form-textarea {
  width: 100%; height: 120rpx; background: #f5f6fa;
  border-radius: 12rpx; padding: 16rpx 20rpx; font-size: 28rpx; box-sizing: border-box;
}
.popup-btn-row { display: flex; gap: 24rpx; margin-top: 8rpx; }
.btn-cancel {
  flex: 1; height: 80rpx; line-height: 80rpx;
  background: #f5f5f5; color: #666; border-radius: 12rpx; font-size: 28rpx; text-align: center;
}
.btn-confirm { flex: 1; height: 80rpx; line-height: 80rpx; font-size: 28rpx; }

/* 采购记录折叠卡片 */
.order-card {
  background: #fafbfc; border-radius: 12rpx; margin-bottom: 12rpx; overflow: hidden;
  &--expanded { background: #f0f5ff; }
  &__header { display: flex; align-items: center; padding: 16rpx 20rpx; gap: 12rpx; }
  &__time { font-size: 24rpx; color: #666; flex-shrink: 0; }
  &__status { font-size: 22rpx; padding: 2rpx 12rpx; border-radius: 8rpx; flex-shrink: 0; }
  &__amount { font-size: 26rpx; font-weight: 600; color: #ff4d4f; margin-left: auto; }
  &__arrow { font-size: 22rpx; color: #999; flex-shrink: 0; width: 28rpx; text-align: center; }
  &__detail { padding: 0 20rpx 16rpx; border-top: 1rpx solid #e8e8e8; }
}
.status--PENDING    { background: #fff7e6; color: #fa8c16; }
.status--PURCHASING { background: #e6f7ff; color: #1890ff; }
.status--ARRIVED    { background: #e6fffb; color: #13c2c2; }
.status--COMPLETED  { background: #f6ffed; color: #52c41a; }
.status--CANCELLED  { background: #fff1f0; color: #ff4d4f; }
.detail-row { display: flex; justify-content: space-between; padding: 10rpx 0; }
.detail-label { font-size: 24rpx; color: #999; }
.detail-value { font-size: 24rpx; color: #333; max-width: 70%; text-align: right; }
.detail-value--link { color: #2979ff; }
.loading-tip { text-align: center; padding: 40rpx; color: #999; font-size: 26rpx; }
</style>
