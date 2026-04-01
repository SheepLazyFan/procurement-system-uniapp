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
  border-radius: var(--radius-sm);
  font-size: 24rpx;
  font-weight: 500;

  &--primary {
    background-color: var(--brand-primary-light);
    .status-tag__text { color: var(--brand-primary); }
  }

  &--success {
    background-color: var(--color-success-light);
    .status-tag__text { color: var(--color-success); }
  }

  &--warning {
    background-color: var(--color-warning-light);
    .status-tag__text { color: var(--color-warning); }
  }

  &--danger {
    background-color: var(--color-danger-light);
    .status-tag__text { color: var(--color-danger); }
  }

  &--info {
    background-color: var(--border-light);
    .status-tag__text { color: var(--text-secondary); }
  }

  &--purple {
    background-color: rgba(139, 92, 246, 0.1);
    .status-tag__text { color: #8B5CF6; }
  }
}
</style>
