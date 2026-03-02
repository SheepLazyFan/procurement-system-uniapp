<template>
  <view class="search-bar">
    <view class="search-bar__input-wrap">
      <text class="search-bar__icon">🔍</text>
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
  background-color: #fff;

  &__input-wrap {
    flex: 1;
    display: flex;
    align-items: center;
    background-color: #f5f6fa;
    border-radius: 32rpx;
    padding: 12rpx 24rpx;
    height: 64rpx;
  }

  &__icon {
    font-size: 28rpx;
    margin-right: 12rpx;
  }

  &__input {
    flex: 1;
    font-size: 28rpx;
    color: #333;
  }

  &__clear {
    font-size: 28rpx;
    color: #ccc;
    padding: 8rpx;
  }

  &__action {
    margin-left: 16rpx;
  }

  &__action-text {
    font-size: 28rpx;
    color: #2979ff;
  }
}
</style>
