<template>
  <view class="page-supplier-list">
    <view class="page-header animate-slide-down">
      <view class="search-wrapper">
        <SearchBar v-model="keyword" placeholder="搜索供应商" @search="onSearch" @clear="onSearch" />
      </view>
      <view class="header-add-btn" @tap="goAdd">
        <text class="header-add-icon">+</text>
      </view>
    </view>

    <view class="supplier-list container">
      <view
        v-for="(item, index) in supplierList"
        :key="item.id"
        class="supplier-card saas-card animate-fade-up"
        hover-class="saas-card-push"
        :hover-start-time="0"
        :hover-stay-time="100"
        :style="{ 'animation-delay': (index % 10) * 0.05 + 's' }"
        @tap="goDetail(item.id)"
      >
        <view class="supplier-card__header">
          <text class="supplier-card__name">{{ item.name }}</text>
          <text class="supplier-card__category" v-if="item.mainCategory">{{ item.mainCategory || '' }}</text>
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



    <!-- 添加供应商弹窗 -->
    <view class="popup-mask" v-if="showAddPopup" @tap="showAddPopup = false">
      <view class="popup-content" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">添加供应商</text>
          <text class="popup-close" @tap="showAddPopup = false">✕</text>
        </view>
        <view class="popup-form">
          <view class="form-group">
            <text class="form-label">名称 *</text>
            <input v-model="addForm.name" placeholder="请输入供应商名称" class="form-input" />
          </view>
          <view class="form-group">
            <text class="form-label">联系电话</text>
            <input v-model="addForm.phone" placeholder="请输入联系电话" class="form-input" />
          </view>
          <view class="form-group">
            <text class="form-label">地址</text>
            <input v-model="addForm.address" placeholder="请输入地址" class="form-input" />
          </view>
          <view class="form-group mb-0">
            <text class="form-label">主营品类</text>
            <input v-model="addForm.mainCategory" placeholder="如：食品饮料、日用百货" class="form-input" />
          </view>
          <button class="btn-submit" @tap="handleAddSupplier">确认添加</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getSupplierList, createSupplier } from '@/api/supplier'
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
      loadMoreStatus: 'more',
      showAddPopup: false,
      addForm: { name: '', phone: '', address: '', mainCategory: '' }
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
      this.addForm = { name: '', phone: '', address: '', mainCategory: '' }
      this.showAddPopup = true
    },
    async handleAddSupplier() {
      if (!this.addForm.name.trim()) {
        return uni.showToast({ title: '请输入供应商名称', icon: 'none' })
      }
      try {
        await createSupplier(this.addForm)
        uni.showToast({ title: '添加成功', icon: 'success' })
        this.showAddPopup = false
        this.refresh()
      } catch (e) {
        uni.showToast({ title: '添加失败', icon: 'none' })
      }
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
    margin-bottom: 16rpx;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 700;
    color: var(--text-primary);
  }

  &__category {
    font-size: 24rpx;
    color: var(--brand-primary);
    background: var(--brand-primary-light);
    padding: 6rpx 16rpx;
    border-radius: var(--radius-sm);
    font-weight: 600;
  }

  &__phone {
    font-size: 26rpx;
    color: var(--text-secondary);
    margin-bottom: 12rpx;
  }

  &__stats {
    display: flex;
    gap: 32rpx;
    margin-top: 16rpx;
  }

  &__stat {
    font-size: 24rpx;
    color: var(--text-tertiary);
  }
}

.page-header {
  display: flex;
  align-items: center;
  background-color: var(--bg-card);
  padding-right: 32rpx;
}

.search-wrapper {
  flex: 1;
  min-width: 0;
  /* Override inner SearchBar right padding visually */
  margin-right: -8rpx;
}

.header-add-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: var(--radius-full);
  background: var(--brand-primary-light);
  color: var(--brand-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.2);
  flex-shrink: 0;

  &:active {
    transform: scale(0.85);
    background: var(--brand-primary);
    color: #fff;
    box-shadow: 0 4rpx 12rpx rgba(41, 121, 255, 0.3);
  }
}

.header-add-icon {
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1;
  margin-top: -6rpx;
}

@keyframes slideDownHeader {
  from { transform: translateY(-20rpx); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.animate-slide-down {
  animation: slideDownHeader 0.4s cubic-bezier(0.25, 1, 0.5, 1) both;
}

/* 添加供应商弹窗 */
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
  background: var(--bg-card);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  padding-bottom: env(safe-area-inset-bottom);
  animation: slideUpSpring 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.2) both;
}
.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 32rpx 20rpx;
}
.popup-title {
  font-size: 34rpx;
  font-weight: 700;
  color: var(--text-primary);
}
.popup-close {
  font-size: 36rpx;
  color: var(--text-tertiary);
  padding: 8rpx;
}
.popup-form {
  padding: 16rpx 32rpx 40rpx;
}
.form-group { margin-bottom: 32rpx; }
.mb-0 { margin-bottom: 0 !important; }
.form-label {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16rpx;
}
.form-input {
  height: 88rpx;
  background: var(--bg-page);
  border-radius: var(--radius-md);
  padding: 0 24rpx;
  font-size: 28rpx;
  color: var(--text-primary);
  box-shadow: inset 0 2rpx 4rpx rgba(0,0,0,0.02);
}

.btn-submit {
  background: var(--brand-primary);
  color: #fff;
  border-radius: var(--radius-full);
  font-size: 32rpx;
  font-weight: 600;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 48rpx;
  box-shadow: 0 8rpx 24rpx rgba(41, 121, 255, 0.25);
  transition: all 0.2s;
  
  &:active {
    transform: scale(0.95);
  }
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
