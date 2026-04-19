import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // 加载对应 mode 的 .env 文件
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [uni()],
    css: {
      preprocessorOptions: {
        scss: {
          api: 'modern-compiler',
          silenceDeprecations: ['legacy-js-api']
        },
        sass: {
          api: 'modern-compiler',
          silenceDeprecations: ['legacy-js-api']
        }
      }
    },
    define: {
      // 注入 API 基础地址，编译时替换为字符串字面量（小程序环境不支持 import.meta.env）
      __API_BASE__: JSON.stringify(env.VITE_API_BASE || 'http://127.0.0.1:8080/api')
    }
  }
})
