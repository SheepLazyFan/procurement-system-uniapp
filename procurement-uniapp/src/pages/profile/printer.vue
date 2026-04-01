<template>
  <view class="page-printer">
    <NavBar title="打印机管理" />

    <!-- 硬件状态主屏 -->
    <view class="hardware-hero animate-fade-up">
      <view class="device-showcase">
        <view class="soundwaves" v-if="isConnected">
          <view class="wave wave-1"></view>
          <view class="wave wave-2"></view>
          <view class="wave wave-3"></view>
        </view>
        <view class="printer-icon-glow" :class="{ 'is-online': isConnected }"></view>
        <view class="svg-icon-printer" :class="{ 'is-online': isConnected }"></view>
      </view>
      
      <view class="device-status">
        <text class="status-name">{{ connectedDevice ? connectedDevice.name : '尚未连接打印机' }}</text>
        <view class="status-badge" :class="isConnected ? 'bg-green' : 'bg-gray'">
          <text class="status-dot" :class="{ 'pulse': isConnected }"></text>
          <text class="status-text">{{ connectedDevice ? (isConnected ? '已连接' : '已断开连接') : '无设备记录' }}</text>
        </view>
      </view>
      
      <text class="device-id-text num-font" v-if="connectedDevice">{{ connectedDevice.deviceId }}</text>

      <!-- 设备核心操作区 -->
      <view class="hero-actions" v-if="connectedDevice">
        <button class="hero-btn btn-secondary" @tap="handleDisconnect" hover-class="btn-hover" v-if="isConnected">断开连接</button>
        <button class="hero-btn btn-primary" @tap="handleReconnect" hover-class="btn-hover" v-else>重新连接</button>
        <button class="hero-btn btn-test" @tap="handleTestPrint" hover-class="btn-hover" :class="{ 'disabled': !isConnected }">测试打印</button>
      </view>
    </view>

    <!-- 硬件建议区 -->
    <view class="saas-card config-card animate-fade-up" style="animation-delay: 0.1s;">
      <view class="card-header">
        <text class="card-title">设备说明</text>
      </view>
      <view class="config-alert">
        <view class="alert-icon-box">
          <view class="svg-icon-alert"></view>
        </view>
        <view class="alert-content">
          <text class="alert-title">规格建议</text>
          <text class="alert-text">本系统排版专精适配 80mm 标准热敏机。请在下方执行雷达扫描进行配对连接。</text>
        </view>
      </view>
    </view>

    <!-- 蓝牙扫描大厅 -->
    <view class="saas-card scan-card animate-fade-up" style="animation-delay: 0.2s;">
      <view class="scan-header">
        <view class="scan-title-row">
          <text class="card-title">附近设备</text>
          <view class="svg-icon-radar" :class="{ 'is-scanning': searching }"></view>
        </view>
        <button class="scan-btn" @tap="handleSearch" hover-class="scan-btn-hover" :disabled="searching">
          {{ searching ? '正在雷达扫描...' : '重新扫描' }}
        </button>
      </view>

      <view class="device-list" v-if="discoveredDevices.length">
        <view class="device-list-item" v-for="device in discoveredDevices" :key="device.deviceId" 
              hover-class="item-hover" :hover-start-time="0" :hover-stay-time="100" @tap="handleConnect(device)">
          <view class="item-left">
            <view class="item-icon bg-blue">
              <view class="svg-icon-bluetooth"></view>
            </view>
            <view class="item-info">
              <text class="item-name">{{ device.name || '未知设备' }}</text>
              <text class="item-mac num-font">{{ device.deviceId }}</text>
            </view>
          </view>
          <view class="item-right">
            <text class="connect-label">连接</text>
            <text class="connect-arrow">›</text>
          </view>
        </view>
      </view>

      <view class="empty-scan" v-else-if="!searching">
        <view class="svg-icon-search"></view>
        <text class="empty-text">附近暂无可连接打印机，请确保设备已开机</text>
      </view>
    </view>

    <view class="safe-bottom-space"></view>
  </view>
</template>

<script>
import NavBar from '@/components/common/NavBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { usePrinterStore } from '@/store/printer'
import { buildTestReceipt } from '@/utils/receipt'

export default {
  components: { NavBar, EmptyState },
  computed: {
    connectedDevice() { return usePrinterStore().connectedDevice },
    isConnected() { return usePrinterStore().isConnected },
    searching() { return usePrinterStore().searching },
    discoveredDevices() { return usePrinterStore().discoveredDevices }
  },
  onShow() {
    usePrinterStore().restoreDevice()
  },
  methods: {
    async handleSearch() {
      try {
        await usePrinterStore().search()
      } catch (e) {
        uni.showToast({ title: e.message || '搜索失败', icon: 'none' })
      }
    },
    async handleConnect(device) {
      uni.showLoading({ title: '连接中...', mask: true })
      try {
        await usePrinterStore().connect(device)
        uni.hideLoading()
        uni.showToast({ title: '连接成功', icon: 'success' })
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '连接失败', icon: 'none' })
      }
    },
    async handleReconnect() {
      uni.showLoading({ title: '重连中...', mask: true })
      try {
        await usePrinterStore().reconnect()
        uni.hideLoading()
        if (usePrinterStore().isConnected) {
          uni.showToast({ title: '重连成功', icon: 'success' })
        } else {
          uni.showToast({ title: '重连失败，请重新搜索连接', icon: 'none' })
        }
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '重连失败', icon: 'none' })
      }
    },
    async handleDisconnect() {
      await usePrinterStore().disconnect()
      uni.showToast({ title: '已断开' })
    },
    async handleTestPrint() {
      if (!usePrinterStore().isConnected) {
        uni.showToast({ title: '请先连接打印机', icon: 'none' })
        return
      }
      uni.showLoading({ title: '打印中...', mask: true })
      try {
        const buffer = buildTestReceipt()
        await usePrinterStore().print(buffer)
        uni.hideLoading()
        uni.showToast({ title: '打印成功', icon: 'success' })
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: e.message || '打印失败', icon: 'none' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page-printer {
  min-height: 100vh; background: var(--bg-page); padding: 24rpx; padding-bottom: 80rpx;
}

/* =======================================
   1. 硬件状态主屏 (Hero Dashboard)
   ======================================= */
.hardware-hero {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 60rpx 40rpx; background: #ffffff; border-radius: var(--radius-xl);
  margin-bottom: 32rpx; box-shadow: 0 16rpx 40rpx rgba(0,0,0,0.02);
}
.device-showcase {
  position: relative; width: 240rpx; height: 240rpx; margin-bottom: 32rpx;
  display: flex; align-items: center; justify-content: center;
}
.svg-icon-printer {
  width: 140rpx; height: 140rpx; position: relative; z-index: 2;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%234b5563' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 6 2 18 2 18 9'%3E%3C/polyline%3E%3Cpath d='M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2'%3E%3C/path%3E%3Crect x='6' y='14' width='12' height='8'%3E%3C/rect%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
  transition: all 0.4s;
}
.svg-icon-printer.is-online {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%232979ff' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 6 2 18 2 18 9'%3E%3C/polyline%3E%3Cpath d='M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2'%3E%3C/path%3E%3Crect x='6' y='14' width='12' height='8'%3E%3C/rect%3E%3C/svg%3E");
}
.printer-icon-glow {
  position: absolute; left: 0; top: 0; width: 100%; height: 100%;
  border-radius: 50%; opacity: 0; background: radial-gradient(circle, rgba(24, 188, 55, 0.2) 0%, rgba(24, 188, 55, 0) 70%);
  transition: opacity 0.4s; z-index: 1;
}
.printer-icon-glow.is-online { opacity: 1; animation: breathe 3s infinite; }
@keyframes breathe { 0% { transform: scale(0.9); opacity: 0.6; } 50% { transform: scale(1.1); opacity: 1; } 100% { transform: scale(0.9); opacity: 0.6; } }

.soundwaves { position: absolute; left: 0; top: 0; width: 100%; height: 100%; z-index: 0; }
.wave {
  position: absolute; left: 0; top: 0; width: 100%; height: 100%;
  border-radius: 50%; border: 4rpx solid var(--color-success);
  opacity: 0; animation: soundwave 3s linear infinite;
}
.wave-1 { animation-delay: 0s; }
.wave-2 { animation-delay: 1s; }
.wave-3 { animation-delay: 2s; }
@keyframes soundwave { 0% { transform: scale(0.6); opacity: 0.8; border-width: 6rpx; } 100% { transform: scale(2); opacity: 0; border-width: 1rpx; } }

.device-status { display: flex; flex-direction: column; align-items: center; gap: 16rpx; margin-bottom: 12rpx; }
.status-name { font-size: 36rpx; font-weight: 800; color: var(--text-primary); }
.status-badge { display: flex; align-items: center; gap: 8rpx; padding: 6rpx 20rpx; border-radius: 30rpx; }
.bg-green { background: rgba(24, 188, 55, 0.1); color: var(--color-success); }
.bg-gray { background: #f0f2f5; color: var(--text-tertiary); }
.status-dot { width: 12rpx; height: 12rpx; border-radius: 50%; background: currentColor; }
.status-dot.pulse { animation: pulse 2s infinite; }
@keyframes pulse { 0% { box-shadow: 0 0 0 0 rgba(24, 188, 55, 0.4); } 70% { box-shadow: 0 0 0 12rpx rgba(24, 188, 55, 0); } 100% { box-shadow: 0 0 0 0 rgba(24, 188, 55, 0); } }
.status-text { font-size: 24rpx; font-weight: 600; }

.device-id-text { font-size: 24rpx; color: var(--text-tertiary); letter-spacing: 2rpx; margin-bottom: 40rpx; }

.hero-actions { display: flex; gap: 24rpx; width: 100%; }
.hero-btn {
  flex: 1; height: 80rpx; line-height: 80rpx; border-radius: 40rpx;
  font-size: 28rpx; font-weight: 600; text-align: center; margin: 0;
  &::after { border: none; }
}
.btn-primary { background: linear-gradient(135deg, #1c5ff8 0%, #2979ff 100%); color: #fff; box-shadow: 0 8rpx 24rpx rgba(41, 121, 255, 0.3); }
.btn-secondary { background: #f0f2f5; color: var(--text-primary); }
.btn-test { background: rgba(24, 188, 55, 0.1); color: var(--color-success); }
.btn-test.disabled { opacity: 0.5; background: #f0f2f5; color: var(--text-tertiary); }
.btn-hover { transform: scale(0.96); opacity: 0.9; }

/* =======================================
   2. 硬件建议说明栏
   ======================================= */
.card-header { margin-bottom: 24rpx; }
.card-title { font-size: 32rpx; font-weight: 700; color: var(--text-primary); }

.config-alert {
  display: flex; align-items: flex-start; gap: 20rpx;
  background: rgba(41, 121, 255, 0.05); border-radius: 24rpx;
  padding: 32rpx; border: 2rpx dashed rgba(41, 121, 255, 0.2);
}
.alert-icon-box {
  width: 60rpx; height: 60rpx; border-radius: 50%; background: #ffffff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(41, 121, 255, 0.1); flex-shrink: 0;
}
.svg-icon-alert {
  width: 36rpx; height: 36rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%232979ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='12' cy='12' r='10'%3E%3C/circle%3E%3Cline x1='12' y1='16' x2='12' y2='12'%3E%3C/line%3E%3Cline x1='12' y1='8' x2='12.01' y2='8'%3E%3C/line%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}
.alert-content { display: flex; flex-direction: column; gap: 8rpx; flex: 1; }
.alert-title { font-size: 28rpx; font-weight: 700; color: var(--brand-primary); }
.alert-text { font-size: 24rpx; line-height: 1.6; color: var(--text-secondary); }

/* =======================================
   3. 蓝牙扫描大厅
   ======================================= */
.scan-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx; }
.scan-title-row { display: flex; align-items: center; gap: 12rpx; }
.svg-icon-radar {
  width: 36rpx; height: 36rpx; opacity: 0.4;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%234b5563' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M4.9 19.1C1 15.2 1 8.8 4.9 4.9'%3E%3C/path%3E%3Cpath d='M7.8 16.2c-2.3-2.3-2.3-6.1 0-8.5'%3E%3C/path%3E%3Ccircle cx='12' cy='12' r='2'%3E%3C/circle%3E%3Cpath d='M16.2 7.8c2.3 2.3 2.3 6.1 0 8.5'%3E%3C/path%3E%3Cpath d='M19.1 4.9C23 8.8 23 15.2 19.1 19.1'%3E%3C/path%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}
.svg-icon-radar.is-scanning { opacity: 1; animation: radar-scan 2s linear infinite; }
@keyframes radar-scan { 0% { transform: scale(0.9); opacity: 0.5; } 50% { transform: scale(1.1); opacity: 1; } 100% { transform: scale(0.9); opacity: 0.5; } }

.scan-btn {
  margin: 0; padding: 0 32rpx; height: 60rpx; line-height: 60rpx; border-radius: 30rpx;
  background: var(--brand-primary); color: #fff; font-size: 24rpx; font-weight: 600;
  &::after { border: none; }
  &[disabled] { background: #ccc; color: #fff; }
}
.scan-btn-hover { transform: scale(0.95); opacity: 0.9; }

.device-list { display: flex; flex-direction: column; gap: 16rpx; }
.device-list-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx; border-radius: var(--radius-md); background: #f8f9fc;
  border: 1rpx solid transparent; transition: all 0.2s;
}
.item-hover { border-color: rgba(41, 121, 255, 0.3); background: #f0f5ff; transform: translateX(8rpx); }

.item-left { display: flex; align-items: center; gap: 24rpx; }
.item-icon { width: 72rpx; height: 72rpx; border-radius: 16rpx; display: flex; align-items: center; justify-content: center; }
.svg-icon-bluetooth {
  width: 40rpx; height: 40rpx;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%232979ff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6.5 6.5 11 11L12 23V1l5.5 5.5-11 11'%3E%3C/path%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}
.item-info { display: flex; flex-direction: column; gap: 6rpx; }
.item-name { font-size: 28rpx; font-weight: 600; color: var(--text-primary); }
.item-mac { font-size: 22rpx; color: var(--text-tertiary); letter-spacing: 1rpx; }

.item-right { display: flex; align-items: center; gap: 8rpx; }
.connect-label { font-size: 24rpx; font-weight: 600; color: var(--brand-primary); }
.connect-arrow { font-size: 36rpx; color: var(--brand-primary); margin-top: -4rpx; }

.empty-scan { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80rpx 0; gap: 24rpx; }
.svg-icon-search {
  width: 64rpx; height: 64rpx; opacity: 0.6;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239ca3af' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='11' cy='11' r='8'%3E%3C/circle%3E%3Cline x1='21' y1='21' x2='16.65' y2='16.65'%3E%3C/line%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; background-position: center;
}
.empty-text { font-size: 26rpx; color: var(--text-secondary); }

.safe-bottom-space { display: block; width: 100%; height: 60rpx; }
</style>
