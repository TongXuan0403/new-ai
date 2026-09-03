import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// 后端地址：默认指向原服务器，可用环境变量 VITE_API_TARGET 覆盖
const apiTarget = process.env.VITE_API_TARGET || 'http://localhost:1236'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        // 第三方大库分包，优化首屏加载与缓存
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'element-vendor': ['element-plus', '@element-plus/icons-vue'],
          'echarts-vendor': ['echarts'],
          'editor-vendor': ['@wangeditor/editor', '@wangeditor/editor-for-vue']
        }
      }
    }
  }
})
