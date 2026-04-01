<template>
  <view class="page-product-form container">
    <view class="saas-card form-section">
      <view class="section-title">基本信息</view>
      <view class="form-group">
        <text class="form-label">分类</text>
        <picker :range="categoryNames" :value="selectedCategoryIndex" :disabled="readonly" @change="onCategoryChange">
          <view class="form-picker">
            <text class="form-picker__text">{{ form.categoryId ? selectedCategoryName : (readonly ? '未选择分类' : '请选择分类') }}</text>
            <text v-if="!readonly" class="form-picker__arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-group">
        <text class="form-label">商品名称 *</text>
        <input v-model="form.name" placeholder="请输入商品名称" class="form-input" :disabled="readonly" />
      </view>

      <view class="form-group">
        <text class="form-label">规格</text>
        <input v-model="form.spec" placeholder="如：330ml、500g" class="form-input" :disabled="readonly" />
      </view>

      <view class="form-group mb-0">
        <text class="form-label">单位</text>
        <input v-model="form.unit" placeholder="如：箱、瓶、包" class="form-input" :disabled="readonly" />
      </view>
    </view>

    <view class="saas-card form-section">
      <view class="section-title">售价与库存</view>
      <view class="form-row">
        <view class="form-group form-group--half">
          <text class="form-label">售价 *</text>
          <input v-model="form.price" type="digit" placeholder="0.00" class="form-input" :disabled="readonly" />
        </view>
        <view v-if="!readonly" class="form-group form-group--half">
          <text class="form-label">成本价</text>
          <input v-model="form.costPrice" type="digit" placeholder="0.00" class="form-input" />
        </view>
      </view>

      <view class="form-row mb-0">
        <view class="form-group form-group--half">
          <text class="form-label">库存 *</text>
          <input v-model="form.stock" type="number" placeholder="0" class="form-input" :disabled="readonly" />
        </view>
        <view class="form-group form-group--half">
          <text class="form-label">预警值</text>
          <input v-model="form.stockWarning" type="number" placeholder="10" class="form-input" :disabled="readonly" />
        </view>
      </view>
    </view>

    <view class="saas-card form-section">
      <view class="section-title">图文与详情</view>
      <!-- 图片上传区域 -->
      <view class="form-group">
        <text class="form-label">商品图片</text>
        <view class="image-upload-area">
          <view v-for="(img, idx) in form.images" :key="idx" class="image-preview">
            <image :src="$fileUrl(img)" class="image-preview__img" mode="aspectFill" />
            <text v-if="!readonly" class="image-preview__delete" @tap="removeImage(idx)">✕</text>
          </view>
          <view v-if="form.images.length < 5 && !readonly" class="image-upload-btn" @tap="uploadImage">
            <text class="image-upload-text">+ 选图</text>
          </view>
        </view>
      </view>
      <!-- 商品描述 -->
      <view class="form-group">
        <text class="form-label">商品描述</text>
        <textarea v-model="form.description" class="form-textarea" placeholder="请输入详细描述（选填）" maxlength="2000" auto-height :disabled="readonly" />
      </view>
      <!-- 二维码 -->
      <view class="form-group mb-0">
        <text class="form-label">演示视频二维码 <text class="form-hint">(选填)</text></text>
        <view class="image-upload-area">
          <view v-if="form.qrcodeImage" class="image-preview">
            <image :src="$fileUrl(form.qrcodeImage)" class="image-preview__img" mode="aspectFill" />
            <text v-if="!readonly" class="image-preview__delete" @tap="removeQrcode">✕</text>
          </view>
          <view v-else-if="!readonly" class="image-upload-btn" @tap="uploadQrcode">
            <text class="image-upload-text">+ 选图</text>
          </view>
        </view>
      </view>
    </view>

    <button v-if="!readonly" class="btn-submit" @tap="handleSave">
      {{ isEdit ? '保存修改' : '添加商品' }}
    </button>
  </view>
</template>

<script>
import { getProductDetail, createProduct, updateProduct } from '@/api/product'
import { getCategoryList } from '@/api/category'
import { chooseAndUploadImages } from '@/api/upload'
import { useUserStore } from '@/store/user'

export default {
  data() {
    return {
      isEdit: false,
      productId: null,
      readonly: false,
      categoryList: [],
      categoryNames: [],
      selectedCategoryIndex: -1,
      form: {
        categoryId: null,
        name: '',
        spec: '',
        unit: '',
        price: '',
        costPrice: '',
        stock: '',
        stockWarning: '0',
        images: [],
        qrcodeImage: '',   // 二维码图片 URL — TODO: 后续部署迁移至腾训云 COS
        description: ''
      }
    }
  },
  computed: {
    hasFullAccess() { return useUserStore().hasFullAccess },
    selectedCategoryName() {
      // 健壮性检查：确保categoryList存在且有效
      if (!this.categoryList || this.categoryList.length === 0) {
        return ''
      }
      const cat = this.categoryList.find(c => c && c.id === this.form.categoryId)
      return (cat && cat.name) ? String(cat.name).trim() : ''
    }
  },
  onLoad(query) {
    if (query.id) {
      this.isEdit = true
      this.productId = Number(query.id)
    }
    if (query.readonly === 'true') {
      this.readonly = true
      uni.setNavigationBarTitle({ title: '商品详情' })
    } else if (query.id) {
      uni.setNavigationBarTitle({ title: '编辑商品' })
    }
    this.loadCategories()
  },
  methods: {
    async loadCategories() {
      try {
        const data = await getCategoryList()
        // 健壮性检查：确保分类数据有效
        this.categoryList = (Array.isArray(data) && data.length > 0) 
          ? data.filter(c => c && c.id && c.name)
          : []
        this.categoryNames = this.categoryList.map(c => c.name || '未命名')
        if (this.isEdit) this.loadProduct()
      } catch (e) {
        this.categoryList = []
        this.categoryNames = []
        uni.showToast({ title: '加载分类失败', icon: 'none' })
      }
    },
    async loadProduct() {
      try {
        const data = await getProductDetail(this.productId)
        // 健壮性检查：验证分类ID有效性
        const validCategoryId = data.categoryId && this.categoryList.some(c => c.id === data.categoryId)
          ? data.categoryId
          : null
        
        this.form = {
          categoryId: validCategoryId,
          name: data.name || '',
          spec: data.spec || '',
          unit: data.unit || '',
          price: String(data.price || 0),
          costPrice: data.costPrice ? String(data.costPrice) : '',
          stock: String(data.stock || 0),
          stockWarning: String(data.stockWarning != null ? data.stockWarning : 0),
          images: Array.isArray(data.images) ? data.images : [],
          qrcodeImage: data.qrcodeImage || '',
          description: data.description || ''
        }
        
        // 更新选中索引：确保有效性
        if (validCategoryId) {
          this.selectedCategoryIndex = this.categoryList.findIndex(c => c && c.id === validCategoryId)
        } else {
          this.selectedCategoryIndex = -1
        }
      } catch (e) {
        uni.showToast({ title: '加载商品失败', icon: 'none' })
      }
    },
    onCategoryChange(e) {
      const index = e.detail.value
      // 健壮性检查：防止数组越界
      if (index >= 0 && index < this.categoryList.length) {
        this.selectedCategoryIndex = index
        const selectedCat = this.categoryList[index]
        this.form.categoryId = (selectedCat && selectedCat.id) ? selectedCat.id : null
      }
    },
    async uploadImage() {
      try {
        const urls = await chooseAndUploadImages(5 - this.form.images.length, 'product')
        this.form.images = [...this.form.images, ...urls]
      } catch (e) {
        if (e?.errMsg && !e.errMsg.includes('cancel')) {
          uni.showToast({ title: '上传失败', icon: 'none' })
        }
      }
    },
    removeImage(idx) {
      this.form.images.splice(idx, 1)
    },
    async uploadQrcode() {
      try {
        const urls = await chooseAndUploadImages(1, 'qrcode')
        if (urls.length > 0) {
          this.form.qrcodeImage = urls[0]
        }
      } catch (e) {
        if (e?.errMsg && !e.errMsg.includes('cancel')) {
          uni.showToast({ title: '上传失败', icon: 'none' })
        }
      }
    },
    removeQrcode() {
      this.form.qrcodeImage = ''
    },
    async handleSave() {
      // 基础验证
      if (!this.form.name.trim()) {
        return uni.showToast({ title: '请输入商品名称', icon: 'none' })
      }
      if (!this.form.price || Number(this.form.price) <= 0) {
        return uni.showToast({ title: '请输入正确的售价', icon: 'none' })
      }

      const payload = {
        ...this.form,
        price: Number(this.form.price),
        costPrice: this.form.costPrice ? Number(this.form.costPrice) : null,
        stock: Number(this.form.stock) || 0,
        stockWarning: this.form.stockWarning !== '' ? Number(this.form.stockWarning) : 0,
        qrcodeImage: this.form.qrcodeImage || null  // 二维码图片 — TODO: COS 迁移
      }

      try {
        if (this.isEdit) {
          await updateProduct(this.productId, payload)
          uni.showToast({ title: '修改成功', icon: 'success' })
        } else {
          await createProduct(payload)
          uni.showToast({ title: '添加成功', icon: 'success' })
        }
        setTimeout(() => uni.navigateBack(), 1000)
      } catch (e) {
        uni.showToast({ title: '保存失败', icon: 'none' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page-product-form {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.form-section {
  padding: 32rpx 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 32rpx;
  padding-left: 12rpx;
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 6rpx;
    height: 28rpx;
    background: var(--brand-primary);
    border-radius: var(--radius-full);
  }
}

.form-group {
  margin-bottom: 32rpx;

  &--half {
    flex: 1;
  }
}

.mb-0 {
  margin-bottom: 0 !important;
}

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
  /* subtle inner boundary */
  box-shadow: inset 0 2rpx 4rpx rgba(0,0,0,0.02);
}

.form-textarea {
  width: 100%;
  min-height: 200rpx;
  background: var(--bg-page);
  border-radius: var(--radius-md);
  padding: 24rpx;
  font-size: 28rpx;
  color: var(--text-primary);
  box-sizing: border-box;
  line-height: 1.6;
  box-shadow: inset 0 2rpx 4rpx rgba(0,0,0,0.02);
}

.form-picker {
  height: 88rpx;
  background: var(--bg-page);
  border-radius: var(--radius-md);
  padding: 0 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 28rpx;
  color: var(--text-primary);
  box-sizing: border-box;
  box-shadow: inset 0 2rpx 4rpx rgba(0,0,0,0.02);

  &__text {
    flex: 1;
    min-width: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    padding-right: 12rpx;
    display: block;
  }

  &__arrow {
    flex-shrink: 0;
    color: var(--text-tertiary);
    font-size: 32rpx;
    line-height: 1;
  }
}

.form-row {
  display: flex;
  gap: 24rpx;
  margin-bottom: 32rpx;
  
  .form-group {
    margin-bottom: 0;
  }
}

.image-upload-area {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.image-preview {
  position: relative;
  width: 160rpx;
  height: 160rpx;
  box-shadow: var(--shadow-sm);
  border-radius: var(--radius-md);

  &__img {
    width: 100%;
    height: 100%;
    border-radius: var(--radius-md);
  }

  &__delete {
    position: absolute;
    top: -12rpx;
    right: -12rpx;
    width: 40rpx;
    height: 40rpx;
    line-height: 38rpx;
    text-align: center;
    background: var(--color-danger);
    color: #fff;
    border-radius: 50%;
    font-size: 24rpx;
    border: 2rpx solid #fff;
    box-shadow: var(--shadow-sm);
  }
}

.image-upload-btn {
  width: 160rpx;
  height: 160rpx;
  background: var(--bg-page);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3rpx dashed #d1d5db;
  transition: all 0.2s;
  
  &:active {
    background: var(--border-light);
    transform: scale(0.96);
  }
}

.image-upload-text {
  font-size: 26rpx;
  color: var(--text-tertiary);
  font-weight: 500;
}

.form-hint {
  font-size: 24rpx;
  color: var(--text-tertiary);
  font-weight: normal;
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
  margin-top: 16rpx;
  margin-bottom: 40rpx;
  box-shadow: 0 8rpx 24rpx rgba(41, 121, 255, 0.25);
  transition: all 0.2s;
  
  &:active {
    transform: scale(0.95);
  }
}
</style>
