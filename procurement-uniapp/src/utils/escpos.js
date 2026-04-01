/**
 * ESC/POS 指令构建器
 * 适配鹭岛宸芯 CX-801 热敏打印机（80mm, 203dpi, GBK 编码）
 */
import { encodeGBK, gbkByteLength, gbkSubstr } from './gbk'

// 每行最大字节数（80mm 纸，正常字体 48 英文字符 / 24 中文字符）
const LINE_BYTES_NORMAL = 48
const LINE_BYTES_DOUBLE = 24

export class EscPosBuilder {
  constructor() {
    this.chunks = []
  }

  /**
   * 追加原始字节
   */
  raw(bytes) {
    this.chunks.push(new Uint8Array(bytes))
    return this
  }

  /**
   * 打印机初始化
   */
  init() {
    return this.raw([0x1B, 0x40]) // ESC @
  }

  /**
   * 设置对齐方式 0=左 1=居中 2=右
   */
  align(n) {
    return this.raw([0x1B, 0x61, n])
  }

  alignLeft() { return this.align(0) }
  alignCenter() { return this.align(1) }
  alignRight() { return this.align(2) }

  /**
   * 设置字体大小
   * @param {number} w - 宽倍数 1-8
   * @param {number} h - 高倍数 1-8
   */
  fontSize(w = 1, h = 1) {
    const n = ((w - 1) << 4) | (h - 1)
    return this.raw([0x1D, 0x21, n]) // GS !
  }

  sizeNormal() { return this.fontSize(1, 1) }
  sizeDouble() { return this.fontSize(2, 2) }

  /**
   * 加粗开关
   */
  bold(on = true) {
    return this.raw([0x1B, 0x45, on ? 1 : 0])
  }

  /**
   * 打印 GBK 编码文本
   */
  text(str) {
    this.chunks.push(encodeGBK(str))
    return this
  }

  /**
   * 打印文本 + 换行
   */
  textLine(str) {
    return this.text(str + '\n')
  }

  /**
   * 换行
   */
  newline(n = 1) {
    for (let i = 0; i < n; i++) {
      this.raw([0x0A])
    }
    return this
  }

  /**
   * 打印虚线分隔符
   */
  separator(char = '-') {
    return this.textLine(char.repeat(LINE_BYTES_NORMAL))
  }

  /**
   * 打印双线分隔符
   */
  doubleSeparator() {
    return this.textLine('='.repeat(LINE_BYTES_NORMAL))
  }

  /**
   * 左右对齐的一行（用于 label: value 格式）
   * @param {string} left - 左侧文本
   * @param {string} right - 右侧文本
   */
  leftRight(left, right) {
    const leftBytes = gbkByteLength(left)
    const rightBytes = gbkByteLength(right)
    const spaceCount = LINE_BYTES_NORMAL - leftBytes - rightBytes
    if (spaceCount > 0) {
      return this.textLine(left + ' '.repeat(spaceCount) + right)
    }
    // 超长则截断左侧
    const maxLeft = LINE_BYTES_NORMAL - rightBytes - 1
    return this.textLine(gbkSubstr(left, maxLeft) + ' ' + right)
  }

  /**
   * 三列对齐（用于商品明细：名称 / 数量 / 金额）
   * 比例大致 26:8:14 字节
   */
  threeColumns(col1, col2, col3) {
    const w1 = 26, w3 = 14, w2 = LINE_BYTES_NORMAL - w1 - w3
    const c1 = gbkSubstr(col1, w1)
    const c2 = col2
    const c3 = col3

    const pad1 = w1 - gbkByteLength(c1)
    const pad2 = w2 - gbkByteLength(c2)

    const line = c1 + ' '.repeat(Math.max(pad1, 0))
      + c2 + ' '.repeat(Math.max(pad2, 0))
      + c3
    return this.textLine(line)
  }

  /**
   * 走纸（末尾留白便于撕纸）
   */
  feed(lines = 4) {
    return this.raw([0x1B, 0x64, lines]) // ESC d n
  }

  /**
   * 合并所有 chunks 为一个 ArrayBuffer
   */
  build() {
    let totalLen = 0
    for (const chunk of this.chunks) {
      totalLen += chunk.byteLength
    }
    const result = new Uint8Array(totalLen)
    let offset = 0
    for (const chunk of this.chunks) {
      result.set(chunk, offset)
      offset += chunk.byteLength
    }
    return result.buffer
  }
}
