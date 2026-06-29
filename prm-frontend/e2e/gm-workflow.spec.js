import { test, expect } from '@playwright/test'
import { visibleMenu } from '../src/lib/roles.js'

/**
 * 副总经理/总经理(gm)角色端到端回归:以 gm 身份登录,逐「gm 可见路由」真浏览器加载,
 * 断言 0 console error / 0 pageerror / 0 API≥400;并校验 gm 工作流收敛
 * (待办中心不含确权 tab、授权审核台只见「副总审批中」)。
 *
 * 不复用全局 admin 会话:本文件自登录为 gm(覆盖 storageState)。
 */
test.use({ storageState: { cookies: [], origins: [] } })

const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

// gm 可见路由(由 roles.js 派生,菜单变更自动跟随)
function gmRoutes() {
  const out = new Set()
  for (const node of visibleMenu('gm')) {
    if (node.path) out.add(node.path)
    for (const it of node.items || []) out.add(it.path)
  }
  return [...out]
}

async function loginAsGm(page) {
  await page.goto('/login', { waitUntil: 'domcontentloaded' })
  await page.getByPlaceholder('用户名').fill('gm')
  await page.getByPlaceholder('密码').fill('Prm@1234')
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), { timeout: 15000 })
  await expect.poll(() => page.evaluate(() => localStorage.getItem('prm-role'))).toBe('gm')
}

test.beforeEach(async ({ page }) => { await loginAsGm(page) })

test('gm 所有可见模块:逐路由零运行时错误', async ({ page, baseURL }) => {
  const failures = []
  for (const route of gmRoutes()) {
    const errs = []
    const onConsole = m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) }
    const onPageErr = e => errs.push('pageerror: ' + e.message)
    const onResp = r => { if (r.url().includes('/api/') && r.status() >= 400) errs.push('api' + r.status() + ': ' + r.url().replace(baseURL, '')) }
    page.on('console', onConsole); page.on('pageerror', onPageErr); page.on('response', onResp)
    await page.goto(route, { waitUntil: 'networkidle' })
    await page.waitForTimeout(700)
    page.off('console', onConsole); page.off('pageerror', onPageErr); page.off('response', onResp)
    if (errs.length) failures.push(`@ ${route}:\n  ${errs.join('\n  ')}`)
  }
  expect(failures, `gm 路由运行时错误:\n${failures.join('\n')}`).toEqual([])
})

test('gm 工作流收敛:待办中心无确权 tab,授权审核台仅「副总审批中」', async ({ page }) => {
  // 统一待办中心:gm 不涉确权 → 不应出现「确权审批」tab;授权 tab 应在
  await page.goto('/dpr/workbench/todo', { waitUntil: 'networkidle' })
  await expect(page.locator('.el-tabs__item')).toContainText([/授权审批/])
  await expect(page.locator('.el-tabs__item', { hasText: '确权审批' })).toHaveCount(0)

  // 授权审核台:任何展示的待办行,其「当前环节」只能是副总审批中(gm 本节点)
  await page.goto('/dpr/auth/review', { waitUntil: 'networkidle' })
  await page.waitForTimeout(600)
  const statusTags = await page.locator('table tbody tr td:nth-last-child(2) .el-tag').allInnerTexts().catch(() => [])
  for (const s of statusTags) {
    expect(['副总审批中'], `审核台出现非 gm 节点行:${s}`).toContain(s.trim())
  }
})
