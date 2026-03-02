<template>
  <view class="page-printer container">
    <NavBar title="打印机管理" />

    <!-- 已连接设备 -->
    <view class="card" v-if="connectedDevice">
      <view class="section-title">已连接设备</view>
      <view class="device-item connected">
        <view class="device-info">
          <text class="device-name">{{ connectedDevice.name || '未知设备' }}</text>
          <text class="device-id">{{ connectedDevice.deviceId }}</text>
        </view>
        <view class="device-actions">
          <text class="action-btn" @tap="handleTestPrint">测试打印</text>
          <text class="action-btn danger" @tap="handleDisconnect">断开</text>
        </view>
      </view>
    </view>

    <!-- 打印设置 -->
    <view class="card">
      <view class="section-title">打印设置</view>
      <view class="form-group">
        <text class="form-label">纸张宽度</text>
        <view class="radio-group">
          <view class="radio-item" :class="{ active: paperWidth === 58 }" @tap="setPaperWidth(58)">
            <text>58mm</text>
          </view>
          <view class="radio-item" :class="{ active: paperWidth === 80 }" @tap="setPaperWidth(80)">
            <text>80mm</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 搜索设备 -->
    <view class="card">
      <view class="section-title">搜索蓝牙设备</view>
      <button class="btn-search" @tap="handleSearch" :disabled="searching">
        {{ searching ? '搜索中...' : '开始搜索' }}
      </button>

      <view v-for="device in discoveredDevices" :key="device.deviceId" class="device-item" @tap="handleConnect(device)">
        <view class="device-info">
          <text class="device-name">{{ device.name || '未知设备' }}</text>
          <text class="device-id">{{ device.deviceId }}</text>
        </view>
        <text class="connect-btn">连接</text>
      </view>
      <EmptyState v-if="!searching && !discoveredDevices.length" text="未发现蓝牙设备" icon="📡" />
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { usePrinterStore } from '@/store/printer'

export default {
  components: { NavBar, EmptyState },
  computed: {
    connectedDevice() { return usePrinterStore().connectedDevice },
    searching() { return usePrinterStore().searching },
    discoveredDevices() { return usePrinterStore().discoveredDevices },
    paperWidth() { return usePrinterStore().paperWidth }
  },
  onShow() {
    usePrinterStore().restoreDevice()
  },
  methods: {
    setPaperWidth(w) {
      usePrinterStore().setPaperWidth(w)
    },
    handleSearch() {
      // Phase 3: 蓝牙设备搜索逻辑
      uni.showToast({ title: 'Phase 3 实现蓝牙搜索', icon: 'none' })
    },
    handleConnect(device) {
      // Phase 3: 蓝牙连接逻辑
      uni.showToast({ title: 'Phase 3 实现蓝牙连接', icon: 'none' })
    },
    handleDisconnect() {
      usePrinterStore().disconnect()
      uni.showToast({ title: '已断开' })
    },
    handleTestPrint() {
      // Phase 3: 测试打印逻辑
      uni.showToast({ title: 'Phase 3 实现测试打印', icon: 'none' })
    }
  }
}
</script>

<style lang="scss" scoped>
.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
}
.device-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  &:last-child { border-bottom: none; }
  &.connected {
    background: #f0f7ff;
    border-radius: 12rpx;
    padding: 20rpx;
    margin: -4rpx -20rpx;
  }
}
.device-info { flex: 1; }
.device-name {
  display: block;
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}
.device-id {
  display: block;
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}
.device-actions {
  display: flex;
  gap: 12rpx;
}
.action-btn {
  font-size: 24rpx;
  color: #2979ff;
  padding: 6rpx 16rpx;
  border: 1rpx solid #2979ff;
  border-radius: 8rpx;
  &.danger {
    color: #e43d33;
    border-color: #e43d33;
  }
}
.connect-btn {
  font-size: 24rpx;
  color: #2979ff;
  padding: 6rpx 20rpx;
  border: 1rpx solid #2979ff;
  border-radius: 8rpx;
}
.form-group { margin-bottom: 20rpx; }
.form-label {
  display: block;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
}
.radio-group {
  display: flex;
  gap: 20rpx;
}
.radio-item {
  padding: 12rpx 32rpx;
  border: 1rpx solid #ddd;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #666;
  &.active {
    border-color: #2979ff;
    color: #2979ff;
    background: #e8f0fe;
  }
}
.btn-search {
  width: 100%;
  height: 76rpx;
  line-height: 76rpx;
  background: #2979ff;
  color: #fff;
  font-size: 28rpx;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
}
</style>
