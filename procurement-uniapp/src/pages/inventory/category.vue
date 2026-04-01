<template>
  <view class="page-category container">
    <!-- 未加入企业提示 -->
    <view v-if="noEnterprise" class="enterprise-tip" @tap="goCreateEnterprise">
      <text class="enterprise-tip__icon">⚠</text>
      <text class="enterprise-tip__text">尚未创建或加入企业，点击前往设置</text>
      <text class="enterprise-tip__arrow">›</text>
    </view>

    <!-- 添加分类 -->
    <view v-else class="add-bar saas-card">
      <view class="add-input-wrap">
        <input
          v-model="newName"
          placeholder="输入新分类名称"
          class="add-input"
        />
      </view>
      <view class="add-btn" @tap="handleAdd">
        <text class="add-btn__text">添加</text>
      </view>
    </view>

    <!-- 分类列表 -->
    <view class="category-list">
      <view
        v-for="(item, index) in categoryList"
        :key="item.id"
        class="category-row saas-card animate-fade-up"
        :style="{ animationDelay: (index % 15) * 0.05 + 's' }"
      >
        <view class="category-row__info">
          <text class="category-row__name">{{ item.name }}</text>
          <text class="category-row__count num-font">{{ item.productCount || 0 }} 款</text>
        </view>
        <view class="category-row__actions">
          <text class="saas-tag saas-tag-primary action-tag" @tap="handleEdit(item)">编辑</text>
          <text class="saas-tag saas-tag-danger action-tag" @tap="handleDelete(item)">删除</text>
        </view>
      </view>
    </view>

    <view v-if="loading" class="loading-tip card">加载中...</view>
    <EmptyState v-else-if="categoryList.length === 0" text="暂无分类" />
    <ConfirmDialog
      v-model:visible="showConfirm"
      content="确认删除该分类？"
      @confirm="doDelete"
    />
  </view>
</template>

<script>
import { getCategoryList, createCategory, updateCategory, deleteCategory } from '@/api/category'
import { useUserStore } from '@/store/user'
import EmptyState from '@/components/common/EmptyState.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

export default {
  components: { EmptyState, ConfirmDialog },
  data() {
    return {
      categoryList: [],
      newName: '',
      showConfirm: false,
      deleteId: null,
      loading: false,
      noEnterprise: false
    }
  },
  onShow() {
    const userStore = useUserStore()
    if (!userStore.hasEnterprise) {
      this.noEnterprise = true
      return
    }
    this.noEnterprise = false
    this.loadList()
  },
  methods: {
    goCreateEnterprise() {
      uni.navigateTo({ url: '/pages/profile/create-enterprise' })
    },
    async loadList() {
      this.loading = true
      try {
        this.categoryList = await getCategoryList()
      } catch (e) {
        if (e && (e.code === 40100 || e.code === 40402 || e.silent)) {
          this.noEnterprise = true
          return
        }
        uni.showToast({ title: '加载分类失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    async handleAdd() {
      if (this.noEnterprise) {
        return uni.showToast({ title: '请先创建或加入企业', icon: 'none' })
      }
      if (!this.newName.trim()) {
        return uni.showToast({ title: '请输入分类名称', icon: 'none' })
      }
      try {
        await createCategory({ name: this.newName.trim() })
        this.newName = ''
        uni.showToast({ title: '添加成功', icon: 'success' })
        this.loadList()
      } catch (e) {
        if (e && (e.code === 40100 || e.code === 40402 || e.silent)) return
        uni.showToast({ title: '添加失败', icon: 'none' })
      }
    },
    handleEdit(item) {
      uni.showModal({
        title: '编辑分类',
        editable: true,
        placeholderText: '分类名称',
        content: item.name,
        success: async (res) => {
          if (res.confirm && res.content?.trim()) {
            try {
              await updateCategory(item.id, { name: res.content.trim() })
              uni.showToast({ title: '修改成功', icon: 'success' })
              this.loadList()
            } catch (e) {
              console.error(e)
              uni.showToast({ title: '修改失败', icon: 'none' })
            }
          }
        }
      })
    },
    handleDelete(item) {
      this.deleteId = item.id
      this.showConfirm = true
    },
    async doDelete() {
      try {
        await deleteCategory(this.deleteId)
        uni.showToast({ title: '删除成功', icon: 'success' })
        this.loadList()
      } catch (e) {
        // 拦截器已自动弹出后端错误提示（如"该分类下存在商品，无法删除"）
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page-category {
  padding: 24rpx;
}

.add-bar {
  display: flex;
  align-items: center;
  padding: 24rpx;
  margin-bottom: 32rpx;
}

.add-input-wrap {
  flex: 1;
  background: var(--bg-page);
  border-radius: var(--radius-full);
  height: 80rpx;
  display: flex;
  align-items: center;
  padding: 0 32rpx;
}

.add-input {
  flex: 1;
  font-size: 28rpx;
  color: var(--text-primary);
}

.add-btn {
  background: var(--brand-primary);
  border-radius: var(--radius-full);
  padding: 0 48rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 20rpx;
  transition: all 0.2s;

  &:active {
    transform: scale(0.94);
  }

  &__text {
    color: #fff;
    font-size: 28rpx;
    font-weight: 600;
  }
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.category-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 28rpx;

  &__info {
    display: flex;
    align-items: baseline;
    gap: 16rpx;
  }

  &__name {
    font-size: 32rpx;
    color: var(--text-primary);
    font-weight: 600;
    letter-spacing: 0.5rpx;
  }

  &__count {
    font-size: 24rpx;
    color: var(--text-tertiary);
  }

  &__actions {
    display: flex;
    gap: 16rpx;
  }
}

.action-tag {
  transition: all 0.2s;
  font-size: 26rpx;
  padding: 8rpx 28rpx;
  border-radius: var(--radius-full);
  &:active {
    transform: scale(0.92);
  }
}

.animate-fade-up {
  animation: fadeInUp 0.3s cubic-bezier(0.25, 1, 0.5, 1) both;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20rpx); }
  to { opacity: 1; transform: translateY(0); }
}

.enterprise-tip {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
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
</style>
