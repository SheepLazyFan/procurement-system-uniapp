<template>
  <view class="search-bar">
    <view class="search-bar__input-wrap">
      <view class="search-bar__icon"></view>
      <input
        class="search-bar__input"
        type="text"
        :placeholder="placeholder"
        :value="modelValue"
        confirm-type="search"
        @input="onInput"
        @confirm="onSearch"
      />
      <text
        v-if="modelValue"
        class="search-bar__clear"
        @tap="onClear"
      >✕</text>
    </view>
    <view v-if="showAction" class="search-bar__action" @tap="onSearch">
      <text class="search-bar__action-text">搜索</text>
    </view>
  </view>
</template>

<script>
export default {
  name: 'SearchBar',
  props: {
    modelValue: { type: String, default: '' },
    placeholder: { type: String, default: '搜索' },
    showAction: { type: Boolean, default: false }
  },
  emits: ['update:modelValue', 'search', 'clear'],
  methods: {
    onInput(e) {
      this.$emit('update:modelValue', e.detail.value)
    },
    onSearch() {
      this.$emit('search', this.modelValue)
    },
    onClear() {
      this.$emit('update:modelValue', '')
      this.$emit('clear')
    }
  }
}
</script>

<style lang="scss" scoped>
.search-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background-color: transparent;

  &__input-wrap {
    flex: 1;
    display: flex;
    align-items: center;
    background-color: #fff;
    border-radius: 36rpx;
    padding: 0 24rpx;
    height: 72rpx;
    box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
    border: 2rpx solid transparent;
    transition: all 0.2s;
    
    &:focus-within {
      border-color: rgba(41, 121, 255, 0.3);
      box-shadow: 0 6rpx 20rpx rgba(41, 121, 255, 0.08);
    }
  }

  &__icon {
    width: 32rpx;
    height: 32rpx;
    margin-right: 12rpx;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23A0AEC0' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='11' cy='11' r='8'%3E%3C/circle%3E%3Cline x1='21' y1='21' x2='16.65' y2='16.65'%3E%3C/line%3E%3C/svg%3E");
    background-size: cover;
  }

  &__input {
    flex: 1;
    font-size: 28rpx;
    color: var(--text-primary);
  }

  &__clear {
    font-size: 28rpx;
    color: var(--text-tertiary);
    padding: 8rpx;
  }

  &__action {
    margin-left: 16rpx;
  }

  &__action-text {
    font-size: 28rpx;
    font-weight: 600;
    color: var(--brand-primary);
  }
}
</style>
