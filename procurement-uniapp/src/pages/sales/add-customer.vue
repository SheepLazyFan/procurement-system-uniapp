<template>
  <view class="page-add-customer">
    <view class="saas-card animate-fade-up" style="animation-delay: 0s;">
      <view class="form-group">
        <text class="form-label">客户名称<text class="required">*</text></text>
        <input v-model="form.name" placeholder="请输入客户名称" class="form-input" placeholder-class="input-placeholder" />
      </view>
      <view class="form-group">
        <text class="form-label">联系电话</text>
        <input v-model="form.phone" type="number" maxlength="11" placeholder="请输入联系电话" class="form-input num-font" placeholder-class="input-placeholder" />
      </view>
      <view class="form-group">
        <text class="form-label">地址</text>
        <input v-model="form.address" placeholder="请输入地址" class="form-input" placeholder-class="input-placeholder" />
      </view>
      <view class="form-group">
        <text class="form-label">备注</text>
        <textarea v-model="form.remark" placeholder="可选填写备注" class="form-textarea" placeholder-class="input-placeholder" />
      </view>
    </view>

    <!-- 悬浮操作舱 -->
    <view class="bottom-action-bar safe-area-bottom animate-fade-up" style="animation-delay: 0.1s;">
      <button class="btn-primary" @tap="handleSave" hover-class="saas-card-push" :hover-start-time="0" :hover-stay-time="100">保存客户</button>
    </view>
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
      } catch (e) {
        uni.showToast({ title: '添加失败', icon: 'none' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page-add-customer {
  padding: 24rpx;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
}

.form-group { margin-bottom: 32rpx; }
.form-label { 
  display: block; font-size: 28rpx; font-weight: 500; color: var(--text-primary); margin-bottom: 16rpx; 
}
.required { color: var(--color-danger); margin-left: 8rpx; }

.form-input {
  height: 88rpx; background: var(--bg-page); border-radius: 16rpx;
  padding: 0 24rpx; font-size: 28rpx; color: var(--text-primary);
  border: 2rpx solid transparent; transition: all 0.2s;
  
  &:focus { border-color: var(--brand-primary-light); background: #fff; box-shadow: 0 4rpx 16rpx rgba(41,121,255,0.08); }
}

.form-textarea {
  width: 100%; height: 200rpx; background: var(--bg-page); border-radius: 16rpx;
  padding: 24rpx; font-size: 28rpx; color: var(--text-primary); box-sizing: border-box;
  border: 2rpx solid transparent; transition: all 0.2s;
  
  &:focus { border-color: var(--brand-primary-light); background: #fff; box-shadow: 0 4rpx 16rpx rgba(41,121,255,0.08); }
}

::v-deep .input-placeholder {
  color: var(--text-tertiary);
}

/* 悬浮操作舱 */
.bottom-action-bar {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  padding: 24rpx 32rpx;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.04);
  z-index: 100;
  box-sizing: border-box;
}

.bottom-action-bar button {
  width: 100%; margin: 0; padding: 0;
  height: 88rpx; line-height: 88rpx;
  background: var(--brand-primary); color: #fff;
  border-radius: 44rpx; font-size: 32rpx; font-weight: 600; text-align: center;
  box-shadow: 0 4rpx 12rpx rgba(41, 121, 255, 0.2);
  border: none;
  &::after { border: none; }
}
</style>
