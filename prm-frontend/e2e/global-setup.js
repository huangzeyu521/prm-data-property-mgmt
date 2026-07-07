import { chromium } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

/**
 * 全局前置:用演示账号 admin/Prm@1234 登录一次,把会话(localStorage token)存为 storageState,
 * 供各 spec 复用(避免每个用例重复登录)。需要后端 confirm-service(9102,内建登录)在跑。
 */
/**
 * 后端三服务体检:gate 经 vite 代理直连活容器(9101/9102/9103)。任一不可达时,
 * 路由用例会成片爆 api≥400,看起来像「代码回归」,实为「服务未起」。此处先探活,
 * 让基础设施问题在 setup 阶段就以清晰信息失败,而非淹没在 19 条 500 里。
 */
async function preflightBackends() {
  // 用 127.0.0.1(非 localhost):node fetch 会先解析 localhost→IPv6 ::1,而 Docker 端口绑 IPv4 → 误判不可达。
  // 探活 URL 取确定返回 200 的端点(服务"起着但全 404"也应能区分)。
  // 端口可被环境变量覆盖(默认 9101/9102/9103),与 vite.config.js 一致——本机端口被系统保留时改起其它端口。
  const pLedger = process.env.PRM_LEDGER_PORT || '9101'
  const pConfirm = process.env.PRM_CONFIRM_PORT || '9102'
  const pAuth = process.env.PRM_AUTH_PORT || '9103'
  const probes = [
    { name: `dpr-ledger-service(${pLedger})`, url: `http://127.0.0.1:${pLedger}/api/dpr/ledger/statistics` },
    { name: `dpr-confirm-service(${pConfirm})`, url: `http://127.0.0.1:${pConfirm}/api/dpr/confirm/apply/page` },
    { name: `dpr-authorize-service(${pAuth})`, url: `http://127.0.0.1:${pAuth}/api/dpr/auth/scenario/list` },
  ]
  const down = []
  for (const p of probes) {
    try {
      const res = await fetch(p.url, { method: 'GET' })
      if (res.status >= 500 || res.status === 0) down.push(`${p.name} -> HTTP ${res.status}`)
    } catch (e) {
      down.push(`${p.name} -> 不可达(${e.cause?.code || e.message})`)
    }
  }
  if (down.length) {
    throw new Error(
      '后端服务未就绪(基础设施问题,非代码 bug):\n  ' + down.join('\n  ') +
      '\n修复:docker start prm-ledger prm-confirm prm-authorize —— 待其返回 200 后重跑 E2E。' +
      '\n注意:勿与重型 Gradle 构建并发跑 E2E,Docker Desktop 内存吃紧会 SIGTERM(143)掉容器。'
    )
  }
}

export default async function globalSetup() {
  const base = process.env.PW_BASE || 'http://localhost:5173'
  await preflightBackends()
  const browser = await chromium.launch({ headless: true, executablePath: process.env.PW_CHROME || undefined })
  const ctx = await browser.newContext()
  const page = await ctx.newPage()
  await page.goto(base + '/login', { waitUntil: 'domcontentloaded' })
  await page.getByPlaceholder('用户名').fill(process.env.PW_USER || 'admin')
  await page.getByPlaceholder('密码').fill(process.env.PW_PASS || 'Prm@1234')
  await page.getByRole('button', { name: /登\s*录/ }).click()
  // 登录成功后硬跳转离开 /login;等待 token 落地
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), { timeout: 15000 })
    .catch(() => { throw new Error('登录失败:仍停留在 /login —— 确认 confirm-service(9102)在跑且 admin/Prm@1234 有效') })
  const dir = path.resolve('e2e/.auth')
  fs.mkdirSync(dir, { recursive: true })
  await ctx.storageState({ path: path.join(dir, 'state.json') })
  await browser.close()
}
