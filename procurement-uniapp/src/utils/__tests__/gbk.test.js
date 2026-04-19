import { describe, it, expect, test } from 'vitest'
import { gbkByteLength, gbkSubstr } from '../gbk.js'

/**
 * gbk.js 单元测试
 * 测试范围：gbkByteLength（字节宽度计算）、gbkSubstr（字节截断）。
 * 注意：encodeGBK 依赖 gbk-table（73KB Map），测试 byteLength 和 substr 即可覆盖核心逻辑。
 * 遵循全局规范：AAA 模式 + Should...when... 命名。
 */
describe('gbkByteLength — GBK 字节宽度计算', () => {
  test.each([
    ['',       0,   '空字符串'],
    ['abc',    3,   '纯 ASCII = 3 字节'],
    ['你好',    4,   '纯中文 = 2×2 = 4 字节'],
    ['Hi你好',  6,   '混合 = 2(ASCII) + 4(中文) = 6'],
    ['¥100',   5,   '¥ 是 non-ASCII (> 0x80) = 2 + 3(ASCII) = 5'],
    ['A',      1,   '单个 ASCII'],
    ['中',      2,   '单个中文'],
    ['12345',  5,   '纯数字 = 5 字节'],
    ['a b c',  5,   '含空格 = 5 字节（空格是 ASCII）'],
    ['！',      2,   '全角感叹号 = 2 字节'],
  ])('should return %s when str is "%s"', (str, expected) => {
    expect(gbkByteLength(str)).toBe(expected)
  })
})

describe('gbkSubstr — 按 GBK 字节宽度截断', () => {
  it('should return full string when maxBytes >= actual length', () => {
    // Arrange
    const str = 'Hello'
    // Act
    const result = gbkSubstr(str, 10)
    // Assert
    expect(result).toBe('Hello')
  })

  it('should truncate ASCII string at correct position', () => {
    // Arrange
    const str = 'ABCDEF'
    // Act — 只保留前 3 字节
    const result = gbkSubstr(str, 3)
    // Assert
    expect(result).toBe('ABC')
  })

  it('should truncate Chinese string at character boundary (not mid-char)', () => {
    // Arrange — 3 个中文字 = 6 字节
    const str = '你好吗'
    // Act — 最多 4 字节 → 只能取 2 个中文（4 字节），第 3 个中文需要 6 字节 > 4
    const result = gbkSubstr(str, 4)
    // Assert
    expect(result).toBe('你好')
  })

  it('should handle mixed ASCII and Chinese with correct byte counting', () => {
    // Arrange — 'A你B好' = 1 + 2 + 1 + 2 = 6 字节
    const str = 'A你B好'
    // Act — 最多 4 字节 → 'A'(1) + '你'(2) + 'B'(1) = 4，正好
    const result = gbkSubstr(str, 4)
    // Assert
    expect(result).toBe('A你B')
  })

  it('should return empty string when maxBytes is 0', () => {
    expect(gbkSubstr('任意内容', 0)).toBe('')
  })

  it('should not split a Chinese character when remaining bytes is 1', () => {
    // Arrange — '你好' = 4 字节
    const str = '你好'
    // Act — 最多 3 字节 → 只能取 '你'(2 字节)，'好' 需要 4 > 3
    const result = gbkSubstr(str, 3)
    // Assert
    expect(result).toBe('你')
  })

  it('should handle ¥ symbol as 2-byte character', () => {
    // Arrange — '¥100' = ¥(2) + 1(1) + 0(1) + 0(1) = 5 字节
    const str = '¥100'
    // Act — 最多 3 字节 → ¥(2) + 1(1) = 3
    const result = gbkSubstr(str, 3)
    // Assert
    expect(result).toBe('¥1')
  })
})
