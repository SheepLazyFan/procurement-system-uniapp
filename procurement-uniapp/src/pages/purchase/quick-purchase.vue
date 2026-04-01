<template>
  <view class="page-quick-purchase">
    <!-- 选择供应商 -->
    <view class="saas-card animate-fade-up" style="animation-delay: 0s;" @tap="openSupplierPicker" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
      <text class="form-label">供应商</text>
      <text class="form-value">{{ selectedSupplier ? selectedSupplier.name : '请选择供应商' }}</text>
    </view>

    <!-- 搜索添加商品 -->
    <view class="animate-fade-up" style="animation-delay: 0.1s;">
      <SearchBar v-model="keyword" placeholder="搜索商品添加" @search="searchProducts" />
    </view>

    <!-- 已选商品列表 -->
    <view class="saas-card animate-fade-up" style="animation-delay: 0.2s;" v-if="selectedItems.length > 0">
      <text class="section-title">已选商品</text>
      <view v-for="(item, idx) in selectedItems" :key="item.productId" class="selected-item">
        <view class="selected-item__info">
          <text class="selected-item__name">{{ item.productName }}</text>
          <text class="selected-item__spec">{{ item.spec }}</text>
        </view>
        <view class="selected-item__right">
          <input
            v-model="item.quantity"
            type="number"
            class="qty-input num-font"
            placeholder="数量"
            placeholder-class="input-placeholder"
          />
          <input
            v-model="item.price"
            type="digit"
            class="price-input num-font"
            placeholder="单价"
            placeholder-class="input-placeholder"
          />
          <text class="remove-btn" @tap="removeItem(idx)">✕</text>
        </view>
      </view>
    </view>

    <!-- 备注 -->
    <view class="saas-card animate-fade-up" style="animation-delay: 0.3s;">
      <text class="form-label">备注</text>
      <textarea v-model="remark" placeholder="可选填写备注" class="form-textarea" placeholder-class="input-placeholder" />
    </view>

    <!-- 防止底部遮挡的安全垫片 -->
    <view class="safe-bottom-space"></view>

    <!-- 悬浮操作舱 -->
    <view class="bottom-action-bar safe-area-bottom animate-fade-up" style="animation-delay: 0.4s;">
      <button class="btn-primary" @tap="handleSubmit" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">生成采购订单</button>
    </view>

    <!-- 供应商选择弹窗 -->
    <view class="popup-mask" v-if="showSupplierPopup" @tap="showSupplierPopup = false">
      <view class="popup-content" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">选择供应商</text>
          <text class="popup-close" @tap="showSupplierPopup = false">✕</text>
        </view>
        <view class="popup-search">
          <input class="popup-search-input" v-model="supplierKeyword" placeholder="搜索供应商" @confirm="doSearchSuppliers" />
          <text class="popup-search-btn" @tap="doSearchSuppliers">搜索</text>
        </view>
        <scroll-view scroll-y class="popup-list">
          <view v-if="supplierLoading" class="popup-loading"><text>加载中...</text></view>
          <view v-else-if="supplierList.length === 0" class="popup-empty">
            <text>{{ supplierKeyword ? '未找到供应商' : '暂无供应商' }}</text>
          </view>
          <view v-for="(s, index) in supplierList" :key="s.id" class="popup-item animate-fade-up" :style="{ 'animation-delay': (index % 10) * 0.05 + 's' }" @tap="selectSupplier(s)">
            <view class="popup-item__info">
              <text class="popup-item__name">{{ s.name }}</text>
              <text class="popup-item__spec" v-if="s.contactPerson">联系人: {{ s.contactPerson }}</text>
            </view>
            <text v-if="selectedSupplier && selectedSupplier.id === s.id" class="popup-item__check">✓</text>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- 商品搜索弹窗 -->
    <view class="popup-mask" v-if="showProductPopup" @tap="showProductPopup = false">
      <view class="popup-content popup-content--product" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">选择商品</text>
          <text class="popup-close" @tap="showProductPopup = false">✕</text>
        </view>
        <view class="popup-search">
          <input class="popup-search-input" v-model="productKeyword" placeholder="输入商品名称搜索" @confirm="doSearchProducts" />
          <text class="popup-search-btn" @tap="doSearchProducts">搜索</text>
        </view>
        <!-- 供应商商品 / 全部商品 切换 -->
        <view class="source-tabs" v-if="selectedSupplier">
          <view class="source-tab" :class="{ 'source-tab--active': productSourceTab === 'supplier' }" @tap="switchSourceTab('supplier')">
            {{ selectedSupplier.name }}的商品
          </view>
          <view class="source-tab" :class="{ 'source-tab--active': productSourceTab === 'all' }" @tap="switchSourceTab('all')">
            全部商品
          </view>
        </view>
        <!-- 分类横向滚动 -->
        <scroll-view scroll-x class="category-scroll">
          <view class="category-tabs">
            <view
              class="category-tab"
              :class="{ 'category-tab--active': selectedCategoryId === null }"
              @tap="selectCategory(null)"
            >全部</view>
            <view
              v-for="cat in categoryList"
              :key="cat.id"
              class="category-tab"
              :class="{ 'category-tab--active': selectedCategoryId === cat.id }"
              @tap="selectCategory(cat.id)"
            >{{ cat.name }}</view>
          </view>
        </scroll-view>
        <scroll-view scroll-y class="popup-list popup-list--with-category">
          <view v-if="productLoading" class="popup-loading"><text>加载中...</text></view>
          <view v-else-if="productList.length === 0" class="popup-empty">
            <text>{{ productKeyword ? '未找到商品' : '暂无商品' }}</text>
          </view>
          <view v-for="(p, index) in productList" :key="p.id" class="popup-item animate-fade-up" :style="{ 'animation-delay': (index % 10) * 0.05 + 's' }" @tap="addProduct(p)">
            <view class="popup-item__info">
              <text class="popup-item__name">{{ p.name }}</text>
              <text class="popup-item__spec" v-if="p.spec">{{ p.spec }}</text>
            </view>
            <view class="popup-item__right">
              <text class="popup-item__price price-text">¥{{ p.costPrice || p.price || '-' }}</text>
              <text class="popup-item__stock">库存: {{ p.stock || 0 }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { createPurchaseOrder } from '@/api/purchaseOrder'
import { getProductList } from '@/api/product'
import { getCategoryList } from '@/api/category'
import { getSupplierList } from '@/api/supplier'
import SearchBar from '@/components/common/SearchBar.vue'

export default {
  components: { SearchBar },
  data() {
    return {
      keyword: '',
      selectedSupplier: null,
      selectedItems: [],
      remark: '',
      // 供应商弹窗
      showSupplierPopup: false,
      supplierKeyword: '',
      supplierList: [],
      supplierLoading: false,
      // 商品弹窗
      showProductPopup: false,
      productKeyword: '',
      productList: [],
      productLoading: false,
      // 分类
      categoryList: [],
      selectedCategoryId: null,
      // 商品来源切换
      productSourceTab: 'supplier'
    }
  },
  methods: {
    // ====== 供应商选择 ======
    openSupplierPicker() {
      this.showSupplierPopup = true
      if (this.supplierList.length === 0) {
        this.doSearchSuppliers()
      }
    },
    async doSearchSuppliers() {
      this.supplierLoading = true
      try {
        const res = await getSupplierList({ keyword: this.supplierKeyword.trim(), pageNum: 1, pageSize: 50 })
        this.supplierList = res?.records || res || []
      } catch (e) {
        uni.showToast({ title: '加载供应商失败', icon: 'none' })
      } finally {
        this.supplierLoading = false
      }
    },
    selectSupplier(supplier) {
      this.selectedSupplier = supplier
      this.showSupplierPopup = false
      this.productSourceTab = 'supplier'
    },
    // ====== 商品搜索 ======
    searchProducts() {
      this.productKeyword = this.keyword
      this.showProductPopup = true
      this.loadCategories()
      this.doSearchProducts()
    },
    async loadCategories() {
      if (this.categoryList.length > 0) return
      try {
        const res = await getCategoryList()
        this.categoryList = res || []
      } catch (e) {
        console.warn('[quick-purchase] 分类加载失败（非关键）:', e)
      }
    },
    selectCategory(categoryId) {
      this.selectedCategoryId = categoryId
      this.doSearchProducts()
    },
    switchSourceTab(tab) {
      if (this.productSourceTab === tab) return
      this.productSourceTab = tab
      this.doSearchProducts()
    },
    async doSearchProducts() {
      this.productLoading = true
      try {
        const params = { pageNum: 1, pageSize: 50 }
        if (this.productKeyword.trim()) {
          params.keyword = this.productKeyword.trim()
        }
        if (this.selectedCategoryId) {
          params.categoryId = this.selectedCategoryId
        }
        if (this.productSourceTab === 'supplier' && this.selectedSupplier) {
          params.supplierId = this.selectedSupplier.id
        }
        const res = await getProductList(params)
        this.productList = res?.records || res || []
      } catch (e) {
        uni.showToast({ title: '搜索失败', icon: 'none' })
      } finally {
        this.productLoading = false
      }
    },
    addProduct(product) {
      const existing = this.selectedItems.find(i => i.productId === product.id)
      if (existing) {
        existing.quantity = Number(existing.quantity) + 1
        uni.showToast({ title: '数量+1', icon: 'none' })
      } else {
        // 价格预填优先级：供货价 > 成本价 > 售价
        // supplyPrice 由后端在 supplierId 过滤时填充（pms_product_supplier.supply_price）
        const prefillPrice = product.supplyPrice ?? product.costPrice ?? product.price ?? ''
        this.selectedItems.push({
          productId: product.id,
          productName: product.name,
          spec: product.spec || '',
          quantity: 1,
          price: prefillPrice
        })
        uni.showToast({ title: '已添加', icon: 'none' })
      }
    },
    removeItem(idx) {
      this.selectedItems.splice(idx, 1)
    },
    async handleSubmit() {
      if (!this.selectedSupplier) {
        return uni.showToast({ title: '请选择供应商', icon: 'none' })
      }
      if (this.selectedItems.length === 0) {
        return uni.showToast({ title: '请添加商品', icon: 'none' })
      }
      try {
        await createPurchaseOrder({
          supplierId: this.selectedSupplier.id,
          items: this.selectedItems.map(i => ({
            productId: i.productId,
            quantity: Number(i.quantity),
            price: Number(i.price)
          })),
          remark: this.remark
        })
        uni.showToast({ title: '采购订单已创建', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 1000)
      } catch (e) {
        uni.showToast({ title: '创建失败', icon: 'none' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page-quick-purchase {
  padding: 24rpx;
}

.safe-bottom-space {
  height: calc(180rpx + env(safe-area-inset-bottom));
  width: 100%;
}

.section-title { font-size: 32rpx; font-weight: 600; color: var(--text-primary); margin-bottom: 24rpx; display: block; }

.form-label {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12rpx;
  display: block;
}

.form-value {
  font-size: 28rpx;
  color: var(--text-secondary);
  display: block;
  padding: 16rpx 24rpx;
  background: var(--bg-page);
  border-radius: var(--radius-md);
  margin-top: 8rpx;
}

.selected-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid var(--border-light);

  &__name {
    font-size: 28rpx;
    font-weight: 600;
    color: var(--text-primary);
  }

  &__spec {
    font-size: 24rpx;
    color: var(--text-tertiary);
    margin-top: 8rpx;
    display: block;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }
}

.qty-input, .price-input {
  width: 130rpx;
  height: 64rpx;
  border: none;
  background: var(--bg-page);
  border-radius: var(--radius-sm);
  text-align: center;
  font-size: 26rpx;
  color: var(--text-primary);
  box-shadow: inset 0 2rpx 4rpx rgba(0,0,0,0.02);
}

.remove-btn {
  color: var(--text-tertiary);
  font-size: 32rpx;
  padding: 8rpx;
  transition: all 0.2s;
  &:active {
    color: var(--color-danger);
    transform: scale(0.9);
  }
}

.form-textarea {
  width: 100%;
  height: 180rpx;
  background: var(--bg-page);
  border-radius: var(--radius-md);
  padding: 24rpx;
  font-size: 28rpx;
  color: var(--text-primary);
  box-sizing: border-box;
  box-shadow: inset 0 2rpx 4rpx rgba(0,0,0,0.02);
}

.btn-submit { display: none; } /* safely hide any straggler */

/* 悬浮操作舱 */
.bottom-action-bar {
  position: fixed; bottom: 0; left: 0; right: 0;
  padding: 24rpx 32rpx; background: rgba(255, 255, 255, 0.9); backdrop-filter: blur(20px);
  box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.04); z-index: 100; box-sizing: border-box;
}
.bottom-action-bar button {
  width: 100%; margin: 0; padding: 0; height: 88rpx; line-height: 88rpx;
  background: var(--brand-primary); color: #fff; border-radius: 44rpx;
  font-size: 32rpx; font-weight: 600; text-align: center;
  box-shadow: 0 8rpx 24rpx rgba(41, 121, 255, 0.25); border: none;
  &::after { border: none; }
}

/* 弹层矩阵 */
.popup-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
  animation: fadeIn 0.3s ease-out both;
}
.popup-content {
  width: 100%;
  height: 75vh;
  background: var(--bg-card);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  display: flex;
  flex-direction: column;
  animation: slideUpSpring 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.2) both;
}
.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 32rpx 20rpx;
  border-bottom: 1rpx solid var(--border-light);
}
.popup-title { font-size: 34rpx; font-weight: 700; color: var(--text-primary); }
.popup-close { font-size: 36rpx; color: var(--text-tertiary); padding: 8rpx; }
.popup-search {
  display: flex;
  align-items: center;
  padding: 16rpx 32rpx;
  gap: 16rpx;
}
.popup-search-input {
  flex: 1;
  height: 72rpx;
  background: var(--bg-page);
  border-radius: var(--radius-full);
  padding: 0 32rpx;
  font-size: 28rpx;
  color: var(--text-primary);
}
.popup-search-btn {
  color: var(--brand-primary);
  font-size: 28rpx;
  font-weight: 600;
  padding: 8rpx 16rpx;
}
.popup-list {
  flex: 1;
  overflow-y: auto;
}
.popup-list--with-category {
  flex: 1;
}

/* 供应商来源/全部来源切换 */
.source-tabs {
  display: flex;
  padding: 0 32rpx;
  border-bottom: 1rpx solid var(--border-light);
}
.source-tab {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  color: var(--text-secondary);
  position: relative;
  transition: all 0.2s;

  &--active {
    color: var(--brand-primary);
    font-weight: 600;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 60rpx;
      height: 4rpx;
      background: var(--brand-primary);
      border-radius: 2rpx;
    }
  }
}
.category-scroll {
  white-space: nowrap;
  padding: 0 16rpx;
  border-bottom: 1rpx solid var(--border-light);
}
.category-tabs {
  display: inline-flex;
}
.category-tab {
  display: inline-block;
  padding: 20rpx 32rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
  position: relative;
  flex-shrink: 0;
  transition: all 0.2s;

  &--active {
    color: var(--brand-primary);
    font-weight: 600;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 40rpx;
      height: 4rpx;
      background: var(--brand-primary);
      border-radius: 2rpx;
    }
  }
}
.popup-loading, .popup-empty {
  padding: 80rpx 0;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 28rpx;
}
.popup-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 32rpx;
  border-bottom: 1rpx solid var(--border-light);
  width: 100%;
  box-sizing: border-box;
  transition: all 0.2s;
  
  &:active { background: var(--bg-page); }

  &__info { flex: 1; min-width: 0; overflow: hidden; }
  &__name { font-size: 28rpx; font-weight: 600; color: var(--text-primary); display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  &__spec { font-size: 24rpx; color: var(--text-tertiary); margin-top: 8rpx; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  &__price { display: block; font-size: 28rpx; font-weight: 700; color: var(--brand-primary); white-space: nowrap; }
  &__stock { display: block; font-size: 22rpx; color: var(--text-secondary); white-space: nowrap; margin-top: 4rpx; }
  &__right { flex: 0 0 auto; display: flex; flex-direction: column; align-items: flex-end; margin-left: 16rpx; }
  &__check { color: var(--brand-primary); font-size: 36rpx; font-weight: 700; flex-shrink: 0; margin-left: 16rpx; }
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
