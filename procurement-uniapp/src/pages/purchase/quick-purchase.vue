<template>
  <view class="page-quick-purchase container">
    <!-- 选择供应商 -->
    <view class="card" @tap="openSupplierPicker">
      <text class="form-label">供应商</text>
      <text class="form-value">{{ selectedSupplier ? selectedSupplier.name : '请选择供应商' }}</text>
    </view>

    <!-- 搜索添加商品 -->
    <SearchBar v-model="keyword" placeholder="搜索商品添加" @search="searchProducts" />

    <!-- 已选商品列表 -->
    <view class="card" v-if="selectedItems.length > 0">
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
            class="qty-input"
            placeholder="数量"
          />
          <input
            v-model="item.price"
            type="digit"
            class="price-input"
            placeholder="单价"
          />
          <text class="remove-btn" @tap="removeItem(idx)">✕</text>
        </view>
      </view>
    </view>

    <!-- 备注 -->
    <view class="card">
      <text class="form-label">备注</text>
      <textarea v-model="remark" placeholder="可选填写备注" class="form-textarea" />
    </view>

    <!-- 提交按钮 -->
    <button class="btn-primary btn-submit" @tap="handleSubmit">生成采购订单</button>

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
          <view v-for="s in supplierList" :key="s.id" class="popup-item" @tap="selectSupplier(s)">
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
      <view class="popup-content" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">选择商品</text>
          <text class="popup-close" @tap="showProductPopup = false">✕</text>
        </view>
        <view class="popup-search">
          <input class="popup-search-input" v-model="productKeyword" placeholder="输入商品名称搜索" @confirm="doSearchProducts" />
          <text class="popup-search-btn" @tap="doSearchProducts">搜索</text>
        </view>
        <scroll-view scroll-y class="popup-list">
          <view v-if="productLoading" class="popup-loading"><text>加载中...</text></view>
          <view v-else-if="productList.length === 0" class="popup-empty">
            <text>{{ productKeyword ? '未找到商品' : '请输入关键词搜索' }}</text>
          </view>
          <view v-for="p in productList" :key="p.id" class="popup-item" @tap="addProduct(p)">
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
      productLoading: false
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
        const res = await getSupplierList({ keyword: this.supplierKeyword.trim(), page: 1, size: 50 })
        this.supplierList = res.data?.records || res.data?.list || res.data || []
      } catch (e) {
        uni.showToast({ title: '加载供应商失败', icon: 'none' })
      } finally {
        this.supplierLoading = false
      }
    },
    selectSupplier(supplier) {
      this.selectedSupplier = supplier
      this.showSupplierPopup = false
    },
    // ====== 商品搜索 ======
    searchProducts() {
      this.productKeyword = this.keyword
      this.showProductPopup = true
      if (this.keyword) {
        this.doSearchProducts()
      }
    },
    async doSearchProducts() {
      if (!this.productKeyword.trim()) {
        this.productList = []
        return
      }
      this.productLoading = true
      try {
        const res = await getProductList({ keyword: this.productKeyword.trim(), page: 1, size: 50 })
        this.productList = res.data?.records || res.data?.list || res.data || []
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
        this.selectedItems.push({
          productId: product.id,
          productName: product.name,
          spec: product.spec || '',
          quantity: 1,
          price: product.costPrice || product.price || ''
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
.section-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}

.form-label {
  font-size: 26rpx;
  color: #999;
  margin-bottom: 8rpx;
}

.form-value {
  font-size: 28rpx;
  color: #333;
}

.selected-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &__name {
    font-size: 28rpx;
    color: #333;
  }

  &__spec {
    font-size: 24rpx;
    color: #999;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 12rpx;
  }
}

.qty-input, .price-input {
  width: 120rpx;
  height: 60rpx;
  border: 1rpx solid #eee;
  border-radius: 8rpx;
  text-align: center;
  font-size: 26rpx;
}

.remove-btn {
  color: #e43d33;
  font-size: 28rpx;
  padding: 8rpx;
}

.form-textarea {
  width: 100%;
  height: 160rpx;
  background: #f5f6fa;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.btn-submit {
  margin-top: 32rpx;
}

/* 弹窗 */
.popup-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}
.popup-content {
  width: 100%;
  height: 70vh;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  display: flex;
  flex-direction: column;
}
.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.popup-title { font-size: 32rpx; font-weight: 600; }
.popup-close { font-size: 36rpx; color: #999; padding: 8rpx; }
.popup-search {
  display: flex;
  align-items: center;
  padding: 16rpx 32rpx;
  gap: 16rpx;
}
.popup-search-input {
  flex: 1;
  height: 64rpx;
  background: #f5f6fa;
  border-radius: 32rpx;
  padding: 0 24rpx;
  font-size: 26rpx;
}
.popup-search-btn {
  color: #2979ff;
  font-size: 28rpx;
  padding: 8rpx 16rpx;
}
.popup-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 32rpx;
}
.popup-loading, .popup-empty {
  padding: 60rpx 0;
  text-align: center;
  color: #999;
  font-size: 26rpx;
}
.popup-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &__name { font-size: 28rpx; color: #333; }
  &__spec { font-size: 24rpx; color: #999; margin-top: 4rpx; }
  &__price { font-size: 28rpx; }
  &__stock { font-size: 22rpx; color: #999; }
  &__right { text-align: right; }
  &__check { color: #2979ff; font-size: 36rpx; font-weight: 700; }
}
</style>
