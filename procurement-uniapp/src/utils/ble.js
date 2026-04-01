/**
 * BLE 蓝牙打印机服务
 * 封装微信小程序 BLE API，提供搜索/连接/写入/断连等功能
 * 适配鹭岛宸芯 CX-801（BT Name: MPT-III）
 */

/** 分包写入每包字节数 */
const WRITE_CHUNK_SIZE = 20
/** 分包写入间隔 ms */
const WRITE_INTERVAL = 20
/** 搜索超时 ms */
const SEARCH_TIMEOUT = 15000

/**
 * 初始化蓝牙适配器
 * @returns {Promise<void>}
 */
export function openBluetooth() {
  return new Promise((resolve, reject) => {
    uni.openBluetoothAdapter({
      success: () => resolve(),
      fail: (err) => {
        if (err.errCode === 10001) {
          reject(new Error('请打开手机蓝牙'))
        } else {
          reject(new Error('蓝牙初始化失败: ' + (err.errMsg || err.errCode)))
        }
      }
    })
  })
}

/**
 * 关闭蓝牙适配器
 */
export function closeBluetooth() {
  return new Promise((resolve) => {
    uni.closeBluetoothAdapter({ complete: () => resolve() })
  })
}

/**
 * 搜索 BLE 设备
 * @param {Function} onDeviceFound - 每次发现设备的回调 (devices: Array)
 * @param {number} timeout - 搜索超时时间 ms
 * @returns {Promise<void>} 搜索结束（超时）后 resolve
 */
export function startSearch(onDeviceFound, timeout = SEARCH_TIMEOUT) {
  return new Promise((resolve, reject) => {
    // 监听设备发现
    uni.onBluetoothDeviceFound((res) => {
      if (res.devices && res.devices.length) {
        // 过滤掉无名设备
        const valid = res.devices.filter(d => d.name || d.localName)
        if (valid.length) {
          onDeviceFound(valid.map(d => ({
            deviceId: d.deviceId,
            name: d.name || d.localName,
            RSSI: d.RSSI
          })))
        }
      }
    })

    uni.startBluetoothDevicesDiscovery({
      allowDuplicatesKey: false,
      success: () => {
        // 超时后自动停止搜索
        setTimeout(() => {
          stopSearch().then(resolve)
        }, timeout)
      },
      fail: (err) => {
        const hint = '（安卓请确认微信已获得定位权限）'
        reject(new Error('搜索启动失败' + hint + ': ' + (err.errMsg || '')))
      }
    })
  })
}

/**
 * 停止搜索
 */
export function stopSearch() {
  return new Promise((resolve) => {
    uni.stopBluetoothDevicesDiscovery({ complete: () => resolve() })
  })
}

/**
 * 连接 BLE 设备并自动发现可写特征值
 * @param {string} deviceId
 * @returns {Promise<{serviceId: string, characteristicId: string}>}
 */
export function connectDevice(deviceId) {
  return new Promise((resolve, reject) => {
    uni.createBLEConnection({
      deviceId,
      timeout: 10000,
      success: () => {
        // 连接成功后，延迟 500ms 再获取服务（部分设备需要）
        setTimeout(() => {
          discoverWriteCharacteristic(deviceId).then(resolve).catch(reject)
        }, 500)
      },
      fail: (err) => {
        reject(new Error('连接失败: ' + (err.errMsg || '')))
      }
    })
  })
}

/**
 * 动态扫描 services -> characteristics，找到可写特征值
 */
function discoverWriteCharacteristic(deviceId) {
  return new Promise((resolve, reject) => {
    uni.getBLEDeviceServices({
      deviceId,
      success: (svcRes) => {
        const services = svcRes.services || []
        if (!services.length) {
          return reject(new Error('未发现蓝牙服务'))
        }
        // 遍历所有 service 寻找可写 characteristic
        findWritableCharacteristic(deviceId, services, 0, resolve, reject)
      },
      fail: (err) => reject(new Error('获取服务失败: ' + (err.errMsg || '')))
    })
  })
}

/**
 * 递归遍历 services 查找可写特征值
 */
function findWritableCharacteristic(deviceId, services, index, resolve, reject) {
  if (index >= services.length) {
    return reject(new Error('未找到可写特征值，请确认打印机型号'))
  }
  const service = services[index]
  uni.getBLEDeviceCharacteristics({
    deviceId,
    serviceId: service.uuid,
    success: (charRes) => {
      const chars = charRes.characteristics || []
      // 优先找 writeNoResponse（更快），其次 write
      const writable = chars.find(c =>
        c.properties && (c.properties.writeNoResponse || c.properties.write)
      )
      if (writable) {
        resolve({
          serviceId: service.uuid,
          characteristicId: writable.uuid
        })
      } else {
        findWritableCharacteristic(deviceId, services, index + 1, resolve, reject)
      }
    },
    fail: () => {
      findWritableCharacteristic(deviceId, services, index + 1, resolve, reject)
    }
  })
}

/**
 * 断开 BLE 连接
 */
export function disconnectDevice(deviceId) {
  return new Promise((resolve) => {
    if (!deviceId) return resolve()
    uni.closeBLEConnection({
      deviceId,
      complete: () => resolve()
    })
  })
}

/**
 * 分包写入数据到 BLE 设备
 * @param {string} deviceId
 * @param {string} serviceId
 * @param {string} characteristicId
 * @param {ArrayBuffer} buffer - 完整的 ESC/POS 指令数据
 * @returns {Promise<void>}
 */
export function writeData(deviceId, serviceId, characteristicId, buffer) {
  const data = new Uint8Array(buffer)
  const total = data.byteLength
  let offset = 0

  return new Promise((resolve, reject) => {
    function writeNext() {
      if (offset >= total) return resolve()

      const end = Math.min(offset + WRITE_CHUNK_SIZE, total)
      const chunk = data.slice(offset, end).buffer

      uni.writeBLECharacteristicValue({
        deviceId,
        serviceId,
        characteristicId,
        value: chunk,
        success: () => {
          offset = end
          if (offset >= total) {
            resolve()
          } else {
            setTimeout(writeNext, WRITE_INTERVAL)
          }
        },
        fail: (err) => {
          reject(new Error('数据写入失败: ' + (err.errMsg || '')))
        }
      })
    }

    writeNext()
  })
}

/**
 * 监听 BLE 连接状态变化
 * @param {Function} callback - (deviceId, connected) => void
 */
export function onConnectionChange(callback) {
  uni.onBLEConnectionStateChange((res) => {
    callback(res.deviceId, res.connected)
  })
}
