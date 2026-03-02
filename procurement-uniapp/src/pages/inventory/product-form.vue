<template>
  <view class="page-product-form container">
    <view class="card">
      <view class="form-group">
        <text class="form-label">分类</text>
        <picker :range="categoryNames" @change="onCategoryChange">
          <view class="form-picker">
            <text>{{ form.categoryId ? selectedCategoryName : '请选择分类' }}</text>
          </view>
        </picker>
      </view>

      <view class="form-group">
        <text class="form-label">商品名称 *</text>
        <input v-model="form.name" placeholder="请输入商品名称" class="form-input" />
      </view>

      <view class="form-group">
        <text class="form-label">规格</text>
        <input v-model="form.spec" placeholder="如：330ml、500g" class="form-input" />
      </view>

      <view class="form-group">
        <text class="form-label">单位</text>
        <input v-model="form.unit" placeholder="如：箱、瓶、包" class="form-input" />
      </view>

      <view class="form-row">
        <view class="form-group form-group--half">
          <text class="form-label">售价 *</text>
          <input v-model="form.price" type="digit" placeholder="0.00" class="form-input" />
        </view>
        <view class="form-group form-group--half">
          <text class="form-label">成本价</text>
          <input v-model="form.costPrice" type="digit" placeholder="0.00" class="form-input" />
        </view>
      </view>

      <view class="form-row">
        <view class="form-group form-group--half">
          <text class="form-label">库存 *</text>
          <input v-model="form.stock" type="number" placeholder="0" class="form-input" />
        </view>
        <view class="form-group form-group--half">
          <text class="form-label">预警值</text>
          <input v-model="form.stockWarning" type="number" placeholder="10" class="form-input" />
        </view>
      </view>

      <!-- 图片上传区域（Phase 3 实现） -->
      <view class="form-group">
        <text class="form-label">商品图片</text>
        <view class="image-upload-placeholder">
          <text class="image-upload-text">+ 添加图片</text>
        </view>
      </view>
    </view>

    <button class="btn-save btn-primary" @tap="handleSave">
      {{ isEdit ? '保存修改' : '添加商品' }}
    </button>
  </view>
</template>

<script>
import { getProductDetail, createProduct, updateProduct } from '@/api/product'
import { getCategoryList } from '@/api/category'

export default {
  data() {
    return {
      isEdit: false,
      productId: null,
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
        stockWarning: '10',
        images: []
      }
    }
  },
  computed: {
    selectedCategoryName() {
      const cat = this.categoryList.find(c => c.id === this.form.categoryId)
      return cat ? cat.name : ''
    }
  },
  onLoad(query) {
    if (query.id) {
      this.isEdit = true
      this.productId = Number(query.id)
      uni.setNavigationBarTitle({ title: '编辑商品' })
    }
    this.loadCategories()
  },
  methods: {
    async loadCategories() {
      try {
        this.categoryList = await getCategoryList()
        this.categoryNames = this.categoryList.map(c => c.name)
        if (this.isEdit) this.loadProduct()
      } catch (e) {}
    },
    async loadProduct() {
      try {
        const data = await getProductDetail(this.productId)
        this.form = {
          categoryId: data.categoryId,
          name: data.name,
          spec: data.spec || '',
          unit: data.unit || '',
          price: String(data.price),
          costPrice: data.costPrice ? String(data.costPrice) : '',
          stock: String(data.stock),
          stockWarning: String(data.stockWarning || 10),
          images: data.images || []
        }
        this.selectedCategoryIndex = this.categoryList.findIndex(c => c.id === data.categoryId)
      } catch (e) {}
    },
    onCategoryChange(e) {
      this.selectedCategoryIndex = e.detail.value
      this.form.categoryId = this.categoryList[this.selectedCategoryIndex].id
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
        stockWarning: Number(this.form.stockWarning) || 10
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
      } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
.form-group {
  margin-bottom: 28rpx;

  &--half {
    flex: 1;
  }
}

.form-label {
  display: block;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
}

.form-input {
  height: 80rpx;
  background: #f5f6fa;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
}

.form-picker {
  height: 80rpx;
  background: #f5f6fa;
  border-radius: 12rpx;
  padding: 0 20rpx;
  display: flex;
  align-items: center;
  font-size: 28rpx;
  color: #333;
}

.form-row {
  display: flex;
  gap: 24rpx;
}

.image-upload-placeholder {
  width: 160rpx;
  height: 160rpx;
  background: #f5f6fa;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx dashed #ccc;
}

.image-upload-text {
  font-size: 26rpx;
  color: #999;
}

.btn-save {
  margin-top: 32rpx;
}
</style>
