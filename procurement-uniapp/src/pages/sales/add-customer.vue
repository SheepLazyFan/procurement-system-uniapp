<template>
  <view class="page-add-customer container">
    <view class="card">
      <view class="form-group">
        <text class="form-label">客户名称 *</text>
        <input v-model="form.name" placeholder="请输入客户名称" class="form-input" />
      </view>
      <view class="form-group">
        <text class="form-label">联系电话</text>
        <input v-model="form.phone" type="number" maxlength="11" placeholder="请输入联系电话" class="form-input" />
      </view>
      <view class="form-group">
        <text class="form-label">地址</text>
        <input v-model="form.address" placeholder="请输入地址" class="form-input" />
      </view>
      <view class="form-group">
        <text class="form-label">备注</text>
        <textarea v-model="form.remark" placeholder="可选填写备注" class="form-textarea" />
      </view>
    </view>

    <button class="btn-primary btn-save" @tap="handleSave">保存</button>
  </view>
</template>

<script>
import { createCustomer } from '@/api/customer'

export default {
  data() {
    return {
      form: {
        name: '',
        phone: '',
        address: '',
        remark: ''
      }
    }
  },
  methods: {
    async handleSave() {
      if (!this.form.name.trim()) {
        return uni.showToast({ title: '请输入客户名称', icon: 'none' })
      }
      try {
        await createCustomer(this.form)
        uni.showToast({ title: '添加成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 1000)
      } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
.form-group { margin-bottom: 28rpx; }
.form-label { display: block; font-size: 26rpx; color: #666; margin-bottom: 12rpx; }
.form-input {
  height: 80rpx; background: #f5f6fa; border-radius: 12rpx;
  padding: 0 20rpx; font-size: 28rpx;
}
.form-textarea {
  width: 100%; height: 160rpx; background: #f5f6fa; border-radius: 12rpx;
  padding: 16rpx; font-size: 28rpx; box-sizing: border-box;
}
.btn-save { margin-top: 32rpx; }
</style>
