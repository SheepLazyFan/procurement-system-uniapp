/**
 * GBK 编码器 — 将 JavaScript 字符串转为 GBK 字节数组
 * 用于蓝牙热敏打印机（CX-801 编码为 GBK）
 */
import GBK_TABLE from './gbk-table'

/**
 * Unicode 字符替换表
 * 将 GB2312 不支持但常用的 Unicode 字符映射为 GB2312 中等价的字符
 * 在编码前统一替换，从根本上避免打印出 '?' 乱码
 */
const CHAR_FALLBACK = {
  0x00A5: 0xFFE5,  // ¥ (半角) → ￥ (全角人民币符号)
  0x2713: 0x221A,  // ✓ (勾号) → √ (方根号)
  0x2714: 0x221A,  // ✔ (粗勾) → √
  0x2715: 0x00D7,  // ✕ (乘号) → × (但×本身是ASCII扩展，下面再映射)
  0x2716: 0x00D7,  // ✖ (粗叉) → ×
  0x00D7: 0xD7,    // × (Latin乘号) 保持ASCII范围内不需映射
  0x2018: 0x2018,  // ' 左单引号 → GB2312 有
  0x2019: 0x2019,  // ' 右单引号 → GB2312 有
  0x201C: 0x201C,  // " 左双引号 → GB2312 有
  0x201D: 0x201D,  // " 右双引号 → GB2312 有
  0x2014: 0x2014,  // — 破折号 → GB2312 有
  0x2026: 0x2026,  // … 省略号 → GB2312 有
}

/**
 * 将字符串编码为 GBK 字节 Uint8Array
 * ASCII 字符 1 字节，中文/符号 2 字节
 * 未映射字符先尝试 fallback 替换，仍无法映射则用 '?' (0x3F) 替代
 */
export function encodeGBK(str) {
  const bytes = []
  for (let i = 0; i < str.length; i++) {
    let code = str.charCodeAt(i)

    // 先做 fallback 替换
    if (CHAR_FALLBACK[code] !== undefined) {
      code = CHAR_FALLBACK[code]
    }

    if (code < 0x80) {
      // ASCII 单字节
      bytes.push(code)
    } else {
      const gbk = GBK_TABLE.get(code)
      if (gbk) {
        bytes.push((gbk >> 8) & 0xFF) // 高字节
        bytes.push(gbk & 0xFF)        // 低字节
      } else {
        bytes.push(0x3F) // '?' — 最终兜底
      }
    }
  }
  return new Uint8Array(bytes)
}

/**
 * 计算字符串的 GBK 字节宽度
 * ASCII = 1, 中文 = 2
 */
export function gbkByteLength(str) {
  let len = 0
  for (let i = 0; i < str.length; i++) {
    len += str.charCodeAt(i) < 0x80 ? 1 : 2
  }
  return len
}

/**
 * 按 GBK 字节宽度截断字符串
 */
export function gbkSubstr(str, maxBytes) {
  let len = 0
  for (let i = 0; i < str.length; i++) {
    const w = str.charCodeAt(i) < 0x80 ? 1 : 2
    if (len + w > maxBytes) return str.substring(0, i)
    len += w
  }
  return str
}
