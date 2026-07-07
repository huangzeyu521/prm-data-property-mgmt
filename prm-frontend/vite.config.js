import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 开发代理:按业务域分流到各微服务(本地直连,无需网关);
// 生产统一走网关(9000)。ledger 服务同时承载产权台账(/ledger)与权益动态监测(/monitor)。
// 端口可被环境变量覆盖(默认 9101/9102/9103),用于本机端口被占用/被系统保留时改起到其它端口:
//   PRM_BACKEND_HOST(默认 localhost)、PRM_LEDGER_PORT、PRM_CONFIRM_PORT、PRM_AUTH_PORT。
const H = process.env.PRM_BACKEND_HOST || 'localhost'
const P_LEDGER = process.env.PRM_LEDGER_PORT || '9101'
const P_CONFIRM = process.env.PRM_CONFIRM_PORT || '9102'
const P_AUTH = process.env.PRM_AUTH_PORT || '9103'
const to = (port) => ({ target: `http://${H}:${port}`, changeOrigin: true })

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
      '/api/auth': to(P_CONFIRM),
      '/api/dpr/ledger': to(P_LEDGER),
      '/api/dpr/monitor': to(P_LEDGER),
      '/api/dpr/confirm': to(P_CONFIRM),
      '/api/dpr/system': to(P_CONFIRM),
      '/api/dpr/org': to(P_CONFIRM),
      '/api/dpr/auth': to(P_AUTH)
    }
  }
})
