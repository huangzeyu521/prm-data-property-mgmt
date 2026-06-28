import { chromium } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

/**
 * 全局前置:用演示账号 admin/Prm@1234 登录一次,把会话(localStorage token)存为 storageState,
 * 供各 spec 复用(避免每个用例重复登录)。需要后端 confirm-service(9102,内建登录)在跑。
 */
export default async function globalSetup() {
  const base = process.env.PW_BASE || 'http://localhost:5173'
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
