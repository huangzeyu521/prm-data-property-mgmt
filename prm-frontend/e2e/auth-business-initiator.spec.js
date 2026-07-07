import { test, expect } from '@playwright/test'

/**
 * 角色评估 MEDIUM#1:35号文 一事一议流程步骤10 发起人=分子公司「业务管理部门」团队。
 *  以 business/Prm@1234 独立登录(覆盖默认 admin storageState):
 *  ① 左菜单可见「⭐ 一事一议授权申请」 ② 页面可打开 ③ 批量授权申请仍不可见(批量发起=数字化部,非 business)。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]
test.use({ storageState: { cookies: [], origins: [] } }) // 弃用 admin 会话,走真实 business 登录

test('business:一事一议发起入口可见可用', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/login', { waitUntil: 'domcontentloaded' })
  await page.getByPlaceholder('用户名').fill('business')
  await page.getByPlaceholder('密码').fill('Prm@1234')
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), { timeout: 15000 })

  // ① 展开「数据授权管理」分组 → 一事一议发起入口可见
  await page.getByText('数据授权管理', { exact: false }).first().click()
  await expect(page.getByText('一事一议授权申请', { exact: false }).first()).toBeVisible({ timeout: 10000 })
  // ③ 批量授权申请菜单项不应对 business 可见(批量发起=总部数字化征集,非业务部门)。
  //    注:只断言 menuitem——顶栏「搜索菜单」下拉含全量路由 option(不按角色过滤,既有设计:菜单控入口、后端控审批)。
  await expect(page.getByRole('menuitem', { name: /批量授权申请/ })).toHaveCount(0)

  // ② 点击进入向导,页面正常渲染
  await page.getByText('一事一议授权申请', { exact: false }).first().click()
  await expect(page).toHaveURL(/\/dpr\/auth\/wizard/)
  await expect(page.getByText('申请主体(被授权方)', { exact: false }).first()).toBeVisible({ timeout: 10000 })

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
