import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 开发代理:按业务域分流到各微服务(本地直连,无需网关);
// 生产统一走网关(9000)。ledger 服务同时承载产权台账(/ledger)与权益动态监测(/monitor)。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api/auth': { target: 'http://localhost:9102', changeOrigin: true },
      '/api/dpr/ledger': { target: 'http://localhost:9101', changeOrigin: true },
      '/api/dpr/monitor': { target: 'http://localhost:9101', changeOrigin: true },
      '/api/dpr/confirm': { target: 'http://localhost:9102', changeOrigin: true },
      '/api/dpr/system': { target: 'http://localhost:9102', changeOrigin: true },
      '/api/dpr/auth': { target: 'http://localhost:9103', changeOrigin: true }
    }
  }
})
