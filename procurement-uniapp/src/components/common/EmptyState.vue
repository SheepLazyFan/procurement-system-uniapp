<template>
  <view class="empty-state">
    <image
      v-if="icon"
      class="empty-state__icon"
      :src="icon"
      mode="aspectFit"
    />
    <view v-else class="empty-state__default-icon">
      <text class="empty-state__emoji">📭</text>
    </view>
    <text class="empty-state__text">{{ text }}</text>
    <view v-if="buttonText" class="empty-state__btn" @tap="$emit('action')">
      <text class="empty-state__btn-text">{{ buttonText }}</text>
    </view>
  </view>
</template>

<script>
export default {
  name: 'EmptyState',
  props: {
    text: { type: String, default: '暂无数据' },
    icon: { type: String, default: '' },
    buttonText: { type: String, default: '' }
  },
  emits: ['action']
}
</script>

<style lang="scss" scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 48rpx;

  &__icon {
    width: 240rpx;
    height: 240rpx;
    margin-bottom: 32rpx;
  }

  &__default-icon {
    margin-bottom: 32rpx;
  }

  &__emoji {
    font-size: 120rpx;
  }

  &__text {
    font-size: 28rpx;
    color: #999;
    margin-bottom: 32rpx;
  }

  &__btn {
    background-color: #2979ff;
    border-radius: 40rpx;
    padding: 16rpx 48rpx;
  }

  &__btn-text {
    color: #fff;
    font-size: 28rpx;
  }
}
</style>
