<template>
  <view class="empty-state">
    <!-- URL/图片路径 → <image>；emoji / 文字 → <text> 避免渲染层500错误 -->
    <image
      v-if="iconIsUrl"
      class="empty-state__icon"
      :src="icon"
      mode="aspectFit"
    />
    <view v-else class="empty-state__default-icon">
      <text class="empty-state__emoji">{{ icon || '📭' }}</text>
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
    /** 支持 emoji（直接渲染为文字）或图片路径（/ 或 http 开头才渲染为 image） */
    icon: { type: String, default: '' },
    buttonText: { type: String, default: '' }
  },
  computed: {
    iconIsUrl() {
      return this.icon &&
        (this.icon.startsWith('/') || this.icon.startsWith('http') || this.icon.startsWith('static/'))
    }
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
