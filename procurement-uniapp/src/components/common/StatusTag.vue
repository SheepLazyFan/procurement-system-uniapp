<template>
  <view class="status-tag" :class="'status-tag--' + type">
    <text class="status-tag__text">{{ text }}</text>
  </view>
</template>

<script>
export default {
  name: 'StatusTag',
  props: {
    /** 状态文字 */
    text: { type: String, default: '' },
    /**
     * 主题类型：
     * primary (蓝)、success (绿)、warning (橙)、
     * danger (红)、info (灰)、purple (紫)
     */
    type: { type: String, default: 'info' }
  }
}
</script>

<style lang="scss" scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;

  &--primary {
    background-color: #e8f0fe;
    .status-tag__text { color: #2979ff; }
  }

  &--success {
    background-color: #e8f8eb;
    .status-tag__text { color: #18bc37; }
  }

  &--warning {
    background-color: #fff7e6;
    .status-tag__text { color: #f3a73f; }
  }

  &--danger {
    background-color: #fef0ef;
    .status-tag__text { color: #e43d33; }
  }

  &--info {
    background-color: #f5f5f5;
    .status-tag__text { color: #999; }
  }

  &--purple {
    background-color: #f3edff;
    .status-tag__text { color: #7c3aed; }
  }
}
</style>
