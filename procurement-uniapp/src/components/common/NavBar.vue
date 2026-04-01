<template>
  <view class="navbar" :style="{ paddingTop: statusBarHeight + 'px' }">
    <view class="navbar__content" :style="{ height: navBarHeight + 'px' }">
      <view class="navbar__left" v-if="showBack" @tap="handleBack">
        <view class="navbar__back-arrow" />
      </view>
      <view class="navbar__title">
        <text class="navbar__title-text">{{ title }}</text>
      </view>
      <view class="navbar__right">
        <slot name="right" />
      </view>
    </view>
  </view>
  <!-- 占位高度 -->
  <view :style="{ height: (statusBarHeight + navBarHeight) + 'px' }" />
</template>

<script>
export default {
  name: 'NavBar',
  props: {
    title: { type: String, default: '' },
    showBack: { type: Boolean, default: true }
  },
  data() {
    return {
      statusBarHeight: 0,
      navBarHeight: 44
    }
  },
  created() {
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 20
  },
  methods: {
    handleBack() {
      const pages = getCurrentPages()
      if (pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.switchTab({ url: '/pages/inventory/index' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background-color: #fff;

  &__content {
    display: flex;
    align-items: center;
    padding: 0 24rpx;
    position: relative;
  }

  &__left {
    width: 80rpx;
    display: flex;
    align-items: center;
  }

  &__back-arrow {
    width: 18rpx;
    height: 18rpx;
    border-left: 4rpx solid #333;
    border-bottom: 4rpx solid #333;
    transform: rotate(45deg);
  }

  &__title {
    flex: 1;
    text-align: center;
  }

  &__title-text {
    font-size: 34rpx;
    font-weight: 600;
    color: #333;
  }

  &__right {
    width: 80rpx;
    display: flex;
    justify-content: flex-end;
    align-items: center;
  }
}
</style>
