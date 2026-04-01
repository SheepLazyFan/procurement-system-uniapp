<template>
  <view class="page-create-order">
    <!-- 选择客户 -->
    <view class="saas-card animate-fade-up" style="animation-delay: 0s;" @tap="goSelectCustomer" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">
      <text class="form-label">客户</text>
      <text class="form-value">{{ selectedCustomer ? selectedCustomer.name : '请选择客户（可选）' }}</text>
    </view>

    <!-- 添加商品 -->
    <view class="animate-fade-up" style="animation-delay: 0.1s;">
      <SearchBar v-model="keyword" placeholder="搜索商品添加" @search="searchProducts" />
    </view>

    <!-- 已选商品列表 -->
    <view class="saas-card animate-fade-up" style="animation-delay: 0.2s;" v-if="selectedItems.length > 0">
      <text class="section-title">已选商品</text>
      <view v-for="(item, idx) in selectedItems" :key="item.productId" class="selected-item">
        <view class="selected-item__info">
          <text class="selected-item__name">{{ item.productName }}</text>
          <text class="selected-item__price price-text num-font">¥{{ item.price }}</text>
        </view>
        <view class="selected-item__right">
          <view class="qty-control">
            <text class="qty-btn" @tap="changeQty(idx, -1)" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">-</text>
            <text class="qty-num num-font">{{ item.quantity }}</text>
            <text class="qty-btn" @tap="changeQty(idx, 1)" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">+</text>
          </view>
          <text class="remove-btn" @tap="removeItem(idx)">✕</text>
        </view>
      </view>
      <view class="divider" />
      <view class="total-row flex-between">
        <text class="total-label">合计</text>
        <text class="total-amount price-text num-font">¥{{ totalAmount.toFixed(2) }}</text>
      </view>
    </view>

    <!-- 备注 -->
    <view class="saas-card animate-fade-up" style="animation-delay: 0.3s;">
      <text class="form-label">收货地址</text>
      <input v-model="deliveryAddress" placeholder="可选填写收货地址" class="form-input" placeholder-class="input-placeholder" />
      <text class="form-label" style="margin-top: 24rpx;">备注</text>
      <textarea v-model="remark" placeholder="可选填写备注" class="form-textarea" placeholder-class="input-placeholder" />
    </view>

    <!-- 防止底部遮挡的安全垫片 -->
    <view class="safe-bottom-space"></view>

    <!-- 悬浮操作舱 -->
    <view class="bottom-action-bar safe-area-bottom animate-fade-up" style="animation-delay: 0.4s;">
      <button class="btn-primary" @tap="handleSubmit" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">确认开单</button>
    </view>

    <!-- 商品搜索弹窗 -->
    <view class="popup-mask" v-if="showProductPopup" @tap="showProductPopup = false">
      <view class="popup-content" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">选择商品</text>
          <text class="popup-close" @tap="showProductPopup = false">✕</text>
        </view>
        <view class="popup-search">
          <input class="popup-search-input" v-model="searchKeyword" placeholder="输入商品名称搜索" @confirm="doSearchProducts" />
          <text class="popup-search-btn" @tap="doSearchProducts">搜索</text>
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
        <scroll-view scroll-y class="popup-list">
          <view v-if="productLoading" class="popup-loading">
            <text>加载中...</text>
          </view>
          <view v-else-if="productList.length === 0" class="popup-empty">
            <text>{{ searchKeyword ? '未找到商品' : '暂无商品' }}</text>
          </view>
          <view v-for="p in productList" :key="p.id" class="popup-item" @tap="addProduct(p)">
            <view class="popup-item__info">
              <text class="popup-item__name">{{ p.name }}</text>
              <text class="popup-item__spec" v-if="p.spec">{{ p.spec }}</text>
            </view>
            <view class="popup-item__right">
              <text class="popup-item__price price-text">¥{{ p.price }}</text>
              <text class="popup-item__stock">库存: {{ p.stock || 0 }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { createSalesOrder } from '@/api/salesOrder'
import { getProductList } from '@/api/product'
import { getCategoryList } from '@/api/category'
import { useUserStore } from '@/store/user'
import { WX_STOCK_WARNING_TEMPLATE_ID } from '@/config/index'
import SearchBar from '@/components/common/SearchBar.vue'

export default {
  components: { SearchBar },
  data() {
    return {
      keyword: '',
      selectedCustomer: null,
      selectedItems: [],
      deliveryAddress: '',
      remark: '',
      showProductPopup: false,
      searchKeyword: '',
      productList: [],
      productLoading: false,
      categoryList: [],
      selectedCategoryId: null
    }
  },
  computed: {
    totalAmount() {
      return this.selectedItems.reduce((sum, i) => sum + i.price * i.quantity, 0)
    }
  },
  methods: {
    goSelectCustomer() {
      uni.navigateTo({ url: '/pages/sales/customer-list?select=1' })
    },
    searchProducts() {
      this.searchKeyword = this.keyword
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
        console.warn('[create-order] 分类加载失败（非关键）:', e)
      }
    },
    selectCategory(categoryId) {
      this.selectedCategoryId = categoryId
      this.doSearchProducts()
    },
    async doSearchProducts() {
      this.productLoading = true
      try {
        const params = { pageNum: 1, pageSize: 50 }
        if (this.searchKeyword.trim()) {
          params.keyword = this.searchKeyword.trim()
        }
        if (this.selectedCategoryId) {
          params.categoryId = this.selectedCategoryId
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
        existing.quantity += 1
        uni.showToast({ title: '数量+1', icon: 'none' })
      } else {
        this.selectedItems.push({
          productId: product.id,
          productName: product.name,
          price: Number(product.price) || 0,
          quantity: 1
        })
        uni.showToast({ title: '已添加', icon: 'none' })
      }
    },
    changeQty(idx, delta) {
      const item = this.selectedItems[idx]
      const newQty = item.quantity + delta
      if (newQty <= 0) {
        this.removeItem(idx)
      } else {
        item.quantity = newQty
      }
    },
    removeItem(idx) {
      this.selectedItems.splice(idx, 1)
    },
    async handleSubmit() {
      if (this.selectedItems.length === 0) {
        return uni.showToast({ title: '请添加商品', icon: 'none' })
      }
      // 销售开单会扣减库存，提前申请订阅配额确保库存预警能推送到位
      // #ifdef MP-WEIXIN
      if (useUserStore().notifyEnabled) {
        await new Promise(resolve => {
          wx.requestSubscribeMessage({
            tmplIds: [WX_STOCK_WARNING_TEMPLATE_ID],
            complete: () => resolve() // 无论用户选择允许/拒绝/关闭，都继续开单
          })
        })
      }
      // #endif
      try {
        await createSalesOrder({
          customerId: this.selectedCustomer?.id || null,
          items: this.selectedItems.map(i => ({
            productId: i.productId,
            quantity: i.quantity
          })),
          deliveryAddress: this.deliveryAddress || null,
          remark: this.remark
        })
        uni.showToast({ title: '开单成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 1000)
      } catch (e) {
        uni.showToast({ title: '开单失败', icon: 'none' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page-create-order {
  padding: 24rpx;
}

.safe-bottom-space {
  height: calc(180rpx + env(safe-area-inset-bottom));
  width: 100%;
}

.section-title { font-size: 32rpx; font-weight: 600; color: var(--text-primary); margin-bottom: 24rpx; display: block; }
.form-label { font-size: 28rpx; font-weight: 500; color: var(--text-primary); margin-bottom: 12rpx; display: block; }
.form-value { font-size: 28rpx; color: var(--text-secondary); display: block; }

.selected-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx 0; border-bottom: 1rpx solid var(--border-light);
  &:last-of-type { border-bottom: none; }
  
  &__name { font-size: 28rpx; color: var(--text-primary); font-weight: 500; margin-bottom: 8rpx; display: block; }
  &__price { font-size: 30rpx; color: var(--color-danger); font-weight: 600; }
  
  &__right { display: flex; align-items: center; gap: 24rpx; }
}

.qty-control {
  display: flex; align-items: center; gap: 16rpx;
  background: var(--bg-page); border-radius: 40rpx; padding: 4rpx 8rpx;
}
.qty-btn {
  width: 56rpx; height: 56rpx; line-height: 52rpx; text-align: center;
  background: #fff; border-radius: 28rpx; font-size: 36rpx; font-weight: 300; color: var(--text-primary);
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}
.qty-num { font-size: 30rpx; min-width: 48rpx; text-align: center; font-weight: 600; color: var(--text-primary); }

.remove-btn { color: var(--color-danger); font-size: 32rpx; padding: 12rpx; font-weight: 300; }

.total-row { padding-top: 24rpx; }
.total-label { font-size: 30rpx; color: var(--text-primary); font-weight: 600; }
.total-amount { font-size: 44rpx; color: var(--color-danger); font-weight: 700; }

.form-input {
  width: 100%; height: 88rpx; background: var(--bg-page); border-radius: 16rpx;
  padding: 0 24rpx; font-size: 28rpx; color: var(--text-primary); box-sizing: border-box;
  margin-bottom: 8rpx; border: 2rpx solid transparent; transition: all 0.2s;
  &:focus { border-color: var(--brand-primary-light); background: #fff; box-shadow: 0 4rpx 16rpx rgba(41,121,255,0.08); }
}

.form-textarea {
  width: 100%; height: 200rpx; background: var(--bg-page); border-radius: 16rpx;
  padding: 24rpx; font-size: 28rpx; color: var(--text-primary); box-sizing: border-box;
  border: 2rpx solid transparent; transition: all 0.2s;
  &:focus { border-color: var(--brand-primary-light); background: #fff; box-shadow: 0 4rpx 16rpx rgba(41,121,255,0.08); }
}

::v-deep .input-placeholder { color: var(--text-tertiary); }

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
  box-shadow: 0 4rpx 12rpx rgba(41, 121, 255, 0.2); border: none;
  &::after { border: none; }
}

/* 商品搜索弹窗 */
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

/* 分类横向滚动 */
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
  &__price { display: block; font-size: 28rpx; font-weight: 700; color: var(--color-danger); white-space: nowrap; }
  &__stock { display: block; font-size: 22rpx; color: var(--text-secondary); white-space: nowrap; margin-top: 4rpx; }
  &__right { flex: 0 0 auto; display: flex; flex-direction: column; align-items: flex-end; margin-left: 16rpx; }
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
