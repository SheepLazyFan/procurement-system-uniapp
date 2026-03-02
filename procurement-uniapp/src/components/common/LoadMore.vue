<template>
  <view class="load-more">
    <view v-if="status === 'loading'" class="load-more__loading">
      <view class="load-more__spinner" />
      <text class="load-more__text">加载中...</text>
    </view>
    <view v-else-if="status === 'noMore'" class="load-more__no-more">
      <text class="load-more__text">— 没有更多了 —</text>
    </view>
    <view v-else class="load-more__tip" @tap="$emit('load')">
      <text class="load-more__text">点击加载更多</text>
    </view>
  </view>
</template>

<script>
export default {
  name: 'LoadMore',
  props: {
    /** loading | noMore | more */
    status: { type: String, default: 'more' }
  },
  emits: ['load']
}
</script>

<style lang="scss" scoped>
.load-more {
  display: flex;
  justify-content: center;
  padding: 24rpx 0;

  &__loading,
  &__no-more,
  &__tip {
    display: flex;
    align-items: center;
  }

  &__spinner {
    width: 32rpx;
    height: 32rpx;
    border: 3rpx solid #ddd;
    border-top-color: #2979ff;
    border-radius: 50%;
    margin-right: 12rpx;
    animation: spin 0.6s linear infinite;
  }

  &__text {
    font-size: 24rpx;
    color: #999;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
