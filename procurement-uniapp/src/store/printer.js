/**
 * 蓝牙打印机状态管理
 * 集成 BLE 搜索/连接/写入/断连操作
 */
import { defineStore } from 'pinia'
import {
  openBluetooth,
  closeBluetooth,
  startSearch,
  stopSearch,
  connectDevice,
  disconnectDevice,
  writeData,
  onConnectionChange
} from '@/utils/ble'

export const usePrinterStore = defineStore('printer', {
  state: () => ({
    /** 已连接设备 { deviceId, name, serviceId, characteristicId, paperWidth } */
    connectedDevice: null,
    /** 蓝牙适配器是否可用 */
    bluetoothAvailable: false,
    /** 是否正在搜索 */
    searching: false,
    /** 发现的设备列表 [{ deviceId, name, RSSI }] */
    discoveredDevices: [],
    /** 是否已连接 */
    isConnected: false,
    /** 是否正在打印 */
    printing: false
  }),

  actions: {
    /**
     * 从本地缓存恢复上次连接的设备信息（仅恢复元数据，不自动重连）
     */
    restoreDevice() {
      try {
        const data = uni.getStorageSync('printerDevice')
        if (data) {
          const device = JSON.parse(data)
          this.connectedDevice = device
          // 注意：恢复缓存不代表 BLE 已连接，isConnected 保持 false
          // 用户需点击重连或在 printer.vue 自动尝试重连
        }
      } catch (e) {
        // ignore
      }
    },

    /**
     * 初始化蓝牙并监听连接断开
     */
    async initBluetooth() {
      try {
        await openBluetooth()
        this.bluetoothAvailable = true

        // 监听连接状态变化（设备主动断开时自动更新状态）
        onConnectionChange((deviceId, connected) => {
          if (!connected && this.connectedDevice && this.connectedDevice.deviceId === deviceId) {
            this.isConnected = false
          }
        })
      } catch (e) {
        this.bluetoothAvailable = false
        throw e
      }
    },

    /**
     * 搜索蓝牙设备
     * 安卓 6-11 的 BLE 扫描依赖定位权限，搜索前检查小程序级授权状态：
     *  - 已明确拒绝 (false)：引导去设置，中止搜索
     *  - 从未询问 (undefined) / 已授权 (true)：直接搜索
     */
    async search() {
      if (this.searching) return

      // 仅当用户在小程序内已明确拒绝定位权限时才拦截引导
      const denied = await new Promise(resolve => {
        uni.getSetting({
          success: (res) => resolve(res.authSetting['scope.userLocation'] === false),
          fail: () => resolve(false)
        })
      })

      if (denied) {
        const confirmed = await new Promise(resolve => {
          uni.showModal({
            title: '需要定位权限',
            content: '安卓设备蓝牙搜索需要定位权限，请在小程序设置中开启后重试',
            confirmText: '去设置',
            cancelText: '取消',
            success: (res) => resolve(res.confirm)
          })
        })
        if (confirmed) {
          uni.openSetting({ withSubscriptions: false })
        }
        return  // 权限未开启，中止搜索
      }

      this.searching = true
      this.discoveredDevices = []
      const deviceMap = new Map()

      try {
        await this.initBluetooth()
        await startSearch((devices) => {
          for (const d of devices) {
            deviceMap.set(d.deviceId, d)
          }
          this.discoveredDevices = Array.from(deviceMap.values())
        })
      } finally {
        this.searching = false
      }
    },

    /**
     * 连接指定设备
     */
    async connect(device) {
      try {
        await this.initBluetooth()
        const { serviceId, characteristicId } = await connectDevice(device.deviceId)

        const fullDevice = {
          deviceId: device.deviceId,
          name: device.name,
          serviceId,
          characteristicId,
          paperWidth: 80
        }

        this.connectedDevice = fullDevice
        this.isConnected = true
        uni.setStorageSync('printerDevice', JSON.stringify(fullDevice))

        // 停止搜索
        await stopSearch()
        this.searching = false
      } catch (e) {
        this.isConnected = false
        throw e
      }
    },

    /**
     * 断开当前连接
     */
    async disconnect() {
      if (this.connectedDevice) {
        await disconnectDevice(this.connectedDevice.deviceId)
      }
      this.connectedDevice = null
      this.isConnected = false
      uni.removeStorageSync('printerDevice')
    },

    /**
     * 尝试重连上次保存的设备
     */
    async reconnect() {
      if (!this.connectedDevice || this.isConnected) return
      try {
        await this.connect({
          deviceId: this.connectedDevice.deviceId,
          name: this.connectedDevice.name
        })
      } catch (e) {
        // 重连失败不抛异常，用户可手动重连
        this.isConnected = false
      }
    },

    /**
     * 发送打印数据
     * @param {ArrayBuffer} buffer - ESC/POS 指令数据
     */
    async print(buffer) {
      if (!this.connectedDevice || !this.isConnected) {
        throw new Error('打印机未连接')
      }
      if (this.printing) {
        throw new Error('正在打印中，请稍候')
      }

      this.printing = true
      try {
        const { deviceId, serviceId, characteristicId } = this.connectedDevice
        await writeData(deviceId, serviceId, characteristicId, buffer)
      } finally {
        this.printing = false
      }
    },

  }
})
