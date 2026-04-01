import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    // 纯 Node 环境即可，不需要 DOM（测试工具函数）
    environment: 'node',
    include: ['src/**/__tests__/**/*.test.{js,ts}', 'src/**/*.test.{js,ts}'],
    coverage: {
      provider: 'v8',
      include: ['src/utils/**'],
    },
  },
})
