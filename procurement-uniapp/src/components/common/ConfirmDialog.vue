<template>
  <view v-if="visible" class="confirm-dialog" @tap.self="handleCancel">
    <view class="confirm-dialog__card">
      <view class="confirm-dialog__header">
        <text class="confirm-dialog__title">{{ title }}</text>
      </view>
      <view class="confirm-dialog__body">
        <text class="confirm-dialog__content">{{ content }}</text>
      </view>
      <view class="confirm-dialog__footer">
        <view class="confirm-dialog__btn confirm-dialog__btn--cancel" @tap="handleCancel">
          <text class="confirm-dialog__btn-text">{{ cancelText }}</text>
        </view>
        <view class="confirm-dialog__btn confirm-dialog__btn--confirm" @tap="handleConfirm">
          <text class="confirm-dialog__btn-text confirm-dialog__btn-text--confirm">{{ confirmText }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'ConfirmDialog',
  props: {
    visible: { type: Boolean, default: false },
    title: { type: String, default: '提示' },
    content: { type: String, default: '确认执行此操作？' },
    confirmText: { type: String, default: '确定' },
    cancelText: { type: String, default: '取消' }
  },
  emits: ['confirm', 'cancel', 'update:visible'],
  methods: {
    handleConfirm() {
      this.$emit('confirm')
      this.$emit('update:visible', false)
    },
    handleCancel() {
      this.$emit('cancel')
      this.$emit('update:visible', false)
    }
  }
}
</script>

<style lang="scss" scoped>
.confirm-dialog {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;

  &__card {
    width: 560rpx;
    background-color: #fff;
    border-radius: 24rpx;
    overflow: hidden;
  }

  &__header {
    padding: 40rpx 32rpx 16rpx;
    text-align: center;
  }

  &__title {
    font-size: 34rpx;
    font-weight: 600;
    color: #333;
  }

  &__body {
    padding: 16rpx 32rpx 40rpx;
    text-align: center;
  }

  &__content {
    font-size: 28rpx;
    color: #666;
    line-height: 1.6;
  }

  &__footer {
    display: flex;
    border-top: 1rpx solid #eee;
  }

  &__btn {
    flex: 1;
    height: 96rpx;
    display: flex;
    align-items: center;
    justify-content: center;

    &--cancel {
      border-right: 1rpx solid #eee;
    }
  }

  &__btn-text {
    font-size: 32rpx;
    color: #999;

    &--confirm {
      color: #2979ff;
      font-weight: 600;
    }
  }
}
</style>
