<template>
  <view class="page-batch-import container">
    <!-- 模式切换 -->
    <view class="saas-card">
      <text class="section-title">批量导入商品</text>
      <text class="section-desc">支持 .xlsx / .xls 格式，单次最多 500 条</text>

      <view class="adjust-segment-group">
        <view class="adjust-segment-btn" :class="{ 'is-active': importMode === 'TEMPLATE' }" @tap="switchMode('TEMPLATE')">
          <text>标准模板</text>
        </view>
        <view class="adjust-segment-btn" :class="{ 'is-active': importMode === 'SUPPLIER' }" @tap="switchMode('SUPPLIER')">
          <text>供应商价格表</text>
        </view>
      </view>

      <view class="upload-area" @tap="chooseFile">
        <view class="svg-icon-upload"></view>
        <text class="upload-text">{{ fileName || '点击选择 Excel 文件' }}</text>
      </view>

      <view v-if="importMode === 'TEMPLATE'" class="template-link" @tap="downloadTemplate">
        <view class="svg-icon-download"></view>
        <text class="template-text">下载导入模板</text>
      </view>
    </view>

    <!-- 格式说明 -->
    <view class="saas-card">
      <text class="section-title">{{ importMode === 'TEMPLATE' ? 'Excel 格式说明' : '供应商价格表说明' }}</text>

      <!-- 标准模板 -->
      <view v-if="importMode === 'TEMPLATE'" class="format-table">
        <view class="format-row format-header">
          <text class="format-col col-name">列名</text>
          <text class="format-col col-required">必填</text>
          <text class="format-col col-desc">说明</text>
        </view>
        <view class="format-row" v-for="col in templateColumns" :key="col.name">
          <text class="format-col col-name">{{ col.name }}</text>
          <view class="format-col col-required">
            <view v-if="col.required" class="required-badge">
              <view class="svg-icon-check"></view>
              <text class="text-required">是</text>
            </view>
            <text v-else class="text-optional">否</text>
          </view>
          <text class="format-col col-desc">{{ col.desc }}</text>
        </view>
      </view>

      <!-- 供应商模式 -->
      <view v-else class="supplier-desc">
        <view class="desc-item">
          <text class="desc-label">自动读取列：</text>
          <text class="desc-text">B列(类别) → E列(名称) → H列(含量) → I列(单位) → J列(进价) → K列(售价)</text>
        </view>
        <view class="desc-item">
          <text class="desc-label">自动跳过：</text>
          <text class="desc-text">A列(序号)、C列(图片)、D列(二维码)、F列(单价)、G列(销售单位)</text>
        </view>
        <view class="desc-item">
          <text class="desc-label">库存默认：</text>
          <text class="desc-text">0（导入后可在库存页调整）</text>
        </view>
        <view class="desc-item">
          <text class="desc-label">图片/二维码：</text>
          <text class="desc-text">导入后在商品详情页手动上传</text>
        </view>
      </view>
    </view>

    <!-- 重复策略 -->
    <view v-if="filePath" class="saas-card">
      <text class="section-title">重复商品处理</text>
      <text class="section-desc">当 Excel 中的商品名称与已有商品相同时</text>
      <view class="adjust-segment-group" style="margin-top: 16rpx;">
        <view class="adjust-segment-btn" :class="{ 'is-active': strategy === 'SKIP' }" @tap="strategy = 'SKIP'">
          <text>跳过不导入</text>
        </view>
        <view class="adjust-segment-btn" :class="{ 'is-active': strategy === 'OVERWRITE' }" @tap="strategy = 'OVERWRITE'">
          <text>覆盖更新</text>
        </view>
      </view>
    </view>

    <button
      v-if="filePath"
      class="btn-submit"
      :class="{ 'btn-submit--uploading': uploading }"
      :disabled="uploading"
      @tap="handleImport"
    >
      {{ uploading ? '导入中...' : '确认导入' }}
    </button>
  </view>
</template>

<script>
const BASE_URL = import.meta.env.VITE_API_BASE || 'http://your-server-ip:8080/api'

export default {
  data() {
    return {
      importMode: 'SUPPLIER',
      fileName: '',
      filePath: '',
      strategy: 'SKIP',
      uploading: false,
      templateColumns: [
        { name: '商品分类(必填)', required: true, desc: '分类名称，不存在则自动新建' },
        { name: '商品名称(必填)', required: true, desc: '不超过 100 字，用于去重判断' },
        { name: '规格型号', required: false, desc: '如 500ml/瓶、75g/袋' },
        { name: '计量单位(必填)', required: true, desc: '如 箱、瓶、袋、kg' },
        { name: '销售单价(必填)', required: true, desc: '数值，需大于 0' },
        { name: '成本价', required: false, desc: '默认 0.00' },
        { name: '初始库存', required: false, desc: '整数，默认 0' },
        { name: '库存预警阈值', required: false, desc: '整数，0 = 不预警' },
        { name: '二维码图片URL', required: false, desc: '选填，可先留空后续在App上传' }
      ]
    }
  },
  methods: {
    switchMode(mode) {
      this.importMode = mode
      this.fileName = ''
      this.filePath = ''
    },

    chooseFile() {
      // #ifdef MP-WEIXIN
      wx.chooseMessageFile({
        count: 1,
        type: 'file',
        extension: ['xlsx', 'xls'],
        success: (res) => {
          const file = res.tempFiles[0]
          this.fileName = file.name
          this.filePath = file.path
          if (file.size > 10 * 1024 * 1024) {
            uni.showModal({
              title: '文件较大（' + (file.size / 1024 / 1024).toFixed(1) + 'MB）',
              content: 'Excel 内嵌图片会导致文件很大。建议：复制纯数据到新 Excel 再导入，上传更快。\n\n继续上传当前文件？',
              confirmText: '继续上传',
              cancelText: '我去处理',
              success: (r) => { if (!r.confirm) { this.fileName = ''; this.filePath = '' } }
            })
          }
        },
        fail: () => {
          uni.showToast({ title: '取消选择', icon: 'none' })
        }
      })
      // #endif

      // #ifdef H5
      // H5 环境使用 input[type=file]
      const input = document.createElement('input')
      input.type = 'file'
      input.accept = '.xlsx,.xls'
      input.onchange = (e) => {
        const file = e.target.files[0]
        if (file) {
          this.fileName = file.name
          this.filePath = file  // H5 直接存 File 对象
        }
      }
      input.click()
      // #endif
    },

    downloadTemplate() {
      const token = uni.getStorageSync('token')
      // #ifdef MP-WEIXIN
      uni.showLoading({ title: '下载中...', mask: true })
      wx.downloadFile({
        url: `${BASE_URL}/products/import-template`,
        header: { 'Authorization': `Bearer ${token}` },
        success: (res) => {
          uni.hideLoading()
          if (res.statusCode === 200) {
            wx.openDocument({
              filePath: res.tempFilePath,
              fileType: 'xlsx',
              showMenu: true,  // 允许用户转发、保存
              success: () => {},
              fail: () => {
                uni.showToast({ title: '打开文件失败', icon: 'none' })
              }
            })
          } else {
            uni.showToast({ title: '下载失败', icon: 'none' })
          }
        },
        fail: () => {
          uni.hideLoading()
          uni.showToast({ title: '下载失败，请检查网络', icon: 'none' })
        }
      })
      // #endif

      // #ifdef H5
      // H5 环境直接打开下载链接
      window.open(`${BASE_URL}/products/import-template?token=${token}`)
      // #endif
    },

    handleImport() {
      if (!this.filePath) return
      const token = uni.getStorageSync('token')
      this.uploading = true

      // #ifdef MP-WEIXIN
      wx.uploadFile({
        url: `${BASE_URL}/products/batch-import`,
        filePath: this.filePath,
        name: 'file',
        header: { 'Authorization': `Bearer ${token}` },
        formData: { duplicateStrategy: this.strategy, importMode: this.importMode },
        success: (res) => {
          this.uploading = false
          try {
            const body = JSON.parse(res.data)
            if (body.code === 200) {
              const d = body.data
              uni.showModal({
                title: '导入完成',
                content: `总计 ${d.totalCount} 条\n成功 ${d.successCount} 条\n失败 ${d.failCount} 条\n新建分类 ${d.newCategoryCount} 个` +
                  (d.errors && d.errors.length > 0
                    ? '\n\n失败明细：\n' + d.errors.slice(0, 5).map(e => `第${e.row}行 ${e.name}：${e.reason}`).join('\n')
                    : ''),
                showCancel: false,
                success: () => {
                  if (d.successCount > 0) {
                    uni.navigateBack()
                  }
                }
              })
            } else {
              uni.showToast({ title: body.message || '导入失败', icon: 'none' })
            }
          } catch (e) {
            uni.showToast({ title: '响应解析失败', icon: 'none' })
          }
        },
        fail: () => {
          this.uploading = false
          uni.showToast({ title: '上传失败，请检查网络', icon: 'none' })
        }
      })
      // #endif

      // #ifdef H5
      const formData = new FormData()
      formData.append('file', this.filePath)
      formData.append('duplicateStrategy', this.strategy)
      formData.append('importMode', this.importMode)
      fetch(`${BASE_URL}/products/batch-import`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData
      })
        .then(res => res.json())
        .then(body => {
          this.uploading = false
          if (body.code === 200) {
            const d = body.data
            uni.showModal({
              title: '导入完成',
              content: `成功 ${d.successCount} 条，失败 ${d.failCount} 条，新建分类 ${d.newCategoryCount} 个`,
              showCancel: false,
              success: () => { if (d.successCount > 0) uni.navigateBack() }
            })
          } else {
            uni.showToast({ title: body.message || '导入失败', icon: 'none' })
          }
        })
        .catch(() => {
          this.uploading = false
          uni.showToast({ title: '上传失败', icon: 'none' })
        })
      // #endif
    }
  }
}
</script>

<style lang="scss" scoped>
.page-batch-import {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

/* 极简分段器 Segmented Control (复用统一架构) */
.adjust-segment-group {
  display: flex; background: var(--bg-page); border-radius: 20rpx; padding: 6rpx; gap: 6rpx;
  margin-top: 32rpx; margin-bottom: 32rpx;
}
.adjust-segment-btn {
  flex: 1; text-align: center; padding: 20rpx 0; border-radius: 16rpx;
  font-size: 28rpx; font-weight: 600; color: var(--text-tertiary); transition: all 0.3s;
}
.adjust-segment-btn.is-active { background: #fff; color: var(--brand-primary); box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.06); }

/* 供应商模式说明 */
.supplier-desc {
  padding: 8rpx 0;
}

.desc-item {
  display: flex;
  padding: 12rpx 0;
  font-size: 26rpx;
  line-height: 1.6;
}

.desc-label {
  color: var(--text-primary);
  font-weight: 600;
  width: 180rpx;
  flex-shrink: 0;
}

.desc-text {
  color: var(--text-secondary);
  flex: 1;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12rpx;
  letter-spacing: 0.5rpx;
}

.section-desc {
  display: block;
  font-size: 26rpx;
  color: var(--text-tertiary);
  margin-bottom: 24rpx;
}

.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 280rpx;
  background: var(--bg-page);
  border: 3rpx dashed #d1d5db;
  border-radius: 32rpx;
  margin-bottom: 24rpx;
  transition: all 0.2s;
  
  &:active {
    background: var(--border-light);
    transform: scale(0.98);
  }
}

.svg-icon-upload {
  width: 72rpx; height: 72rpx;
  margin-bottom: 24rpx; opacity: 0.8;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%232979ff' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'/%3E%3Cpolyline points='17 8 12 3 7 8'/%3E%3Cline x1='12' y1='3' x2='12' y2='15'/%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}

.upload-text {
  font-size: 28rpx;
  color: var(--text-secondary);
  font-weight: 600;
  letter-spacing: 1rpx;
}

.template-link {
  display: flex; justify-content: center; align-items: center; gap: 8rpx;
  padding: 24rpx 0; transition: opacity 0.2s;
}
.template-link:active { opacity: 0.5; }

.svg-icon-download {
  width: 32rpx; height: 32rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%232979ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'/%3E%3Cpolyline points='7 10 12 15 17 10'/%3E%3Cline x1='12' y1='15' x2='12' y2='3'/%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}

.template-text {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--brand-primary);
}

/* 格式说明表格 */
.format-table {
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 2rpx solid var(--border-light);
  margin-top: 16rpx;
}

.format-row {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid var(--border-light);

  &:last-child {
    border-bottom: none;
  }
}

.format-header {
  background: var(--bg-page);
  font-weight: 600;
  color: var(--text-primary);
}

.format-col {
  font-size: 26rpx;
  line-height: 1.4;
}

.col-name {
  width: 220rpx;
  flex-shrink: 0;
  color: var(--text-primary);
  font-weight: 500;
}

.col-required {
  width: 120rpx;
  flex-shrink: 0;
  display: flex;
}

.required-badge {
  display: inline-flex; align-items: center; gap: 4rpx;
  background: rgba(76, 175, 80, 0.1); padding: 4rpx 12rpx; border-radius: 8rpx;
}

.svg-icon-check {
  width: 20rpx; height: 20rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%234caf50' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='20 6 9 17 4 12'/%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}

.col-desc {
  flex: 1;
  color: var(--text-secondary);
}

.text-required {
  color: var(--color-success);
  font-weight: 700; font-size: 24rpx;
}
.text-optional {
  color: var(--text-tertiary); font-size: 24rpx; padding-left: 12rpx;
}

.btn-submit {
  background: var(--brand-primary);
  color: #fff;
  border-radius: 40rpx;
  font-size: 32rpx;
  font-weight: 700;
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 40rpx;
  box-shadow: 0 8rpx 24rpx rgba(41, 121, 255, 0.25);
  transition: all 0.2s;
  
  &:active {
    transform: scale(0.95);
  }
  
  &--uploading {
    opacity: 0.7;
  }
}
</style>
