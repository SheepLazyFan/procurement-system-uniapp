<template>
  <view class="page-category container">
    <!-- 添加分类 -->
    <view class="add-bar card">
      <input
        v-model="newName"
        placeholder="输入分类名称"
        class="add-input"
      />
      <view class="add-btn" @tap="handleAdd">
        <text class="add-btn__text">添加</text>
      </view>
    </view>

    <!-- 分类列表 -->
    <view
      v-for="item in categoryList"
      :key="item.id"
      class="category-row card"
    >
      <view class="category-row__info">
        <text class="category-row__name">{{ item.name }}</text>
        <text class="category-row__count">{{ item.productCount || 0 }} 件商品</text>
      </view>
      <view class="category-row__actions">
        <text class="action-text" @tap="handleEdit(item)">编辑</text>
        <text class="action-text action-text--danger" @tap="handleDelete(item)">删除</text>
      </view>
    </view>

    <EmptyState v-if="categoryList.length === 0" text="暂无分类" />
    <ConfirmDialog
      v-model:visible="showConfirm"
      content="确认删除该分类？"
      @confirm="doDelete"
    />
  </view>
</template>

<script>
import { getCategoryList, createCategory, updateCategory, deleteCategory } from '@/api/category'
import EmptyState from '@/components/common/EmptyState.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

export default {
  components: { EmptyState, ConfirmDialog },
  data() {
    return {
      categoryList: [],
      newName: '',
      showConfirm: false,
      deleteId: null
    }
  },
  onShow() {
    this.loadList()
  },
  methods: {
    async loadList() {
      try {
        this.categoryList = await getCategoryList()
      } catch (e) {}
    },
    async handleAdd() {
      if (!this.newName.trim()) {
        return uni.showToast({ title: '请输入分类名称', icon: 'none' })
      }
      try {
        await createCategory({ name: this.newName.trim() })
        this.newName = ''
        uni.showToast({ title: '添加成功', icon: 'success' })
        this.loadList()
      } catch (e) {}
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
            } catch (e) {}
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
      } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
.add-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.add-input {
  flex: 1;
  height: 72rpx;
  background: #f5f6fa;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
}

.add-btn {
  background: #2979ff;
  border-radius: 12rpx;
  padding: 0 32rpx;
  height: 72rpx;
  display: flex;
  align-items: center;

  &__text {
    color: #fff;
    font-size: 28rpx;
  }
}

.category-row {
  display: flex;
  justify-content: space-between;
  align-items: center;

  &__name {
    font-size: 30rpx;
    color: #333;
    font-weight: 500;
  }

  &__count {
    font-size: 24rpx;
    color: #999;
    margin-top: 4rpx;
  }

  &__actions {
    display: flex;
    gap: 24rpx;
  }
}

.action-text {
  font-size: 26rpx;
  color: #2979ff;

  &--danger {
    color: #e43d33;
  }
}
</style>
