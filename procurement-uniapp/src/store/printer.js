/**
 * 蓝牙打印机状态管理
 */
import { defineStore } from 'pinia'

export const usePrinterStore = defineStore('printer', {
  state: () => ({
    /** 已连接设备信息 */
    connectedDevice: null,
    // { deviceId, name, paperWidth: 80 }
    /** 蓝牙适配器是否可用 */
    bluetoothAvailable: false,
    /** 是否正在搜索设备 */
    searching: false,
    /** 发现的设备列表 */
    discoveredDevices: [],
    /** 是否已连接 */
    isConnected: false
  }),

  actions: {
    /**
     * 从本地缓存恢复上次连接的设备
     */
    restoreDevice() {
      try {
        const data = uni.getStorageSync('printerDevice')
        if (data) {
          this.connectedDevice = JSON.parse(data)
        }
      } catch (e) {
        // ignore
      }
    },

    /**
     * 保存已连接设备
     */
    setConnectedDevice(device) {
      this.connectedDevice = device
      this.isConnected = true
      uni.setStorageSync('printerDevice', JSON.stringify(device))
    },

    /**
     * 断开设备
     */
    disconnect() {
      this.connectedDevice = null
      this.isConnected = false
      uni.removeStorageSync('printerDevice')
    },

    /**
     * 设置纸张宽度
     */
    setPaperWidth(width) {
      if (this.connectedDevice) {
        this.connectedDevice.paperWidth = width
        uni.setStorageSync('printerDevice', JSON.stringify(this.connectedDevice))
      }
    },

    /** 设置蓝牙可用状态 */
    setBluetoothAvailable(available) {
      this.bluetoothAvailable = available
    },

    /** 设置搜索状态 */
    setSearching(searching) {
      this.searching = searching
    },

    /** 更新发现的设备列表 */
    setDiscoveredDevices(devices) {
      this.discoveredDevices = devices
    }
  }
})
