<template>
  <view class="page-batch-import container">
    <!-- 第一步：上传文件 -->
    <view class="card">
      <text class="section-title">批量导入商品</text>
      <text class="section-desc">支持 .xlsx / .xls 格式，单次最多 500 条</text>

      <view class="upload-area" @tap="chooseFile">
        <text class="upload-icon">📄</text>
        <text class="upload-text">{{ fileName || '点击选择 Excel 文件' }}</text>
      </view>

      <view class="template-link" @tap="downloadTemplate">
        <text class="template-text">📥 下载导入模板</text>
      </view>
    </view>

    <!-- 第二步：预览（Phase 3 完善） -->
    <view v-if="previewData.length > 0" class="card">
      <text class="section-title">数据预览</text>
      <text class="section-desc">共 {{ previewData.length }} 条记录</text>
      <!-- 预览表格将在 Phase 3 实现 -->
    </view>

    <!-- 重复策略 -->
    <view v-if="previewData.length > 0" class="card">
      <text class="section-title">重复商品处理</text>
      <view class="strategy-options">
        <view
          class="strategy-option"
          :class="{ 'strategy-option--active': strategy === 'SKIP' }"
          @tap="strategy = 'SKIP'"
        >
          <text>跳过</text>
        </view>
        <view
          class="strategy-option"
          :class="{ 'strategy-option--active': strategy === 'OVERWRITE' }"
          @tap="strategy = 'OVERWRITE'"
        >
          <text>覆盖更新</text>
        </view>
      </view>
    </view>

    <button
      v-if="previewData.length > 0"
      class="btn-primary btn-import"
      @tap="handleImport"
    >
      确认导入
    </button>
  </view>
</template>

<script>
import { batchImportProducts } from '@/api/product'

export default {
  data() {
    return {
      fileName: '',
      previewData: [],
      strategy: 'SKIP'
    }
  },
  methods: {
    chooseFile() {
      // 微信小程序选择文件 API
      // #ifdef MP-WEIXIN
      wx.chooseMessageFile({
        count: 1,
        type: 'file',
        extension: ['xlsx', 'xls'],
        success: (res) => {
          const file = res.tempFiles[0]
          this.fileName = file.name
          // Excel 解析将在 Phase 3 通过 SheetJS 实现
          uni.showToast({ title: '文件已选择，解析功能待实现', icon: 'none' })
        }
      })
      // #endif
    },
    downloadTemplate() {
      uni.showToast({ title: '模板下载功能待实现', icon: 'none' })
    },
    async handleImport() {
      if (this.previewData.length === 0) return
      try {
        const res = await batchImportProducts({
          items: this.previewData,
          duplicateStrategy: this.strategy
        })
        uni.showModal({
          title: '导入完成',
          content: `成功 ${res.successCount} 条，失败 ${res.failCount} 条，新建分类 ${res.newCategoryCount} 个`,
          showCancel: false,
          success: () => uni.navigateBack()
        })
      } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 8rpx;
}

.section-desc {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-bottom: 24rpx;
}

.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200rpx;
  background: #f5f6fa;
  border: 2rpx dashed #ccc;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
}

.upload-icon {
  font-size: 64rpx;
  margin-bottom: 12rpx;
}

.upload-text {
  font-size: 26rpx;
  color: #666;
}

.template-link {
  text-align: center;
}

.template-text {
  font-size: 26rpx;
  color: #2979ff;
}

.strategy-options {
  display: flex;
  gap: 24rpx;
}

.strategy-option {
  flex: 1;
  text-align: center;
  padding: 20rpx;
  border: 2rpx solid #eee;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #666;

  &--active {
    border-color: #2979ff;
    color: #2979ff;
    background: #e8f0fe;
  }
}

.btn-import {
  margin-top: 32rpx;
}
</style>
