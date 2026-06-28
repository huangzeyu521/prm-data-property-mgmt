import { defineConfig } from '@playwright/test'

/**
 * PRM 前端 E2E 回归门(真浏览器跑活后端)。
 * 前置:三服务容器在跑(9101/9102/9103);vite dev 由 webServer 自动起(占用则复用)。
 * 运行:`npm run e2e`(本机复用已装 chromium:先 `export PW_CHROME=<chrome.exe>`,见 e2e/README.md);
 *       CI:先 `npx playwright install chromium` 再 `npm run e2e`。
 */
const BASE = process.env.PW_BASE || 'http://localhost:5173'

export default defineConfig({
  testDir: './e2e',
  timeout: 45000,
  expect: { timeout: 10000 },
  fullyParallel: false,
  workers: 1, // 串行:活后端共享 dev H2,避免相互污染
  retries: process.env.CI ? 1 : 0,
  reporter: [['list']],
  globalSetup: './e2e/global-setup.js',
  use: {
    baseURL: BASE,
    storageState: 'e2e/.auth/state.json', // 由 global-setup 登录后写入
    headless: true,
    actionTimeout: 12000,
    navigationTimeout: 25000,
    // 本机无 1228 headless-shell 时,用 PW_CHROME 指向已装 chromium(executablePath 覆盖捆绑浏览器)
    launchOptions: { executablePath: process.env.PW_CHROME || undefined }
  },
  projects: [
    { name: 'chromium', use: { browserName: 'chromium', viewport: { width: 1680, height: 1000 } } }
  ],
  webServer: {
    command: 'npm run dev',
    url: BASE,
    reuseExistingServer: true,
    timeout: 60000
  }
})
