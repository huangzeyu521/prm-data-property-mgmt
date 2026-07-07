import { test, expect } from '@playwright/test'

/**
 * 管制属性单选·后果提示(对齐 35 号文§授权原则)+ 布局解耦(判定卡不再挤单选)。
 *  ① 默认非管制 → 出现"本单位经营权可确权认定"提示;
 *  ② 选管制业务 → 提示切换为"仅经公司授权取得";
 *  ③ 0 console error / pageerror。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('初始确权·管制属性:随选后果提示 + 无运行时错误', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })

  // 选一个系统,让「经营权归集判定」区块(含管制属性)渲染
  const domains = page.locator('.cat-tree .el-tree > .el-tree-node')
  await expect(domains.first()).toBeVisible()
  await domains.first().locator('.el-tree-node__expand-icon').first().click()
  const systems = page.locator('.cat-tree .el-tree-node__children > .el-tree-node')
  await expect(systems.first()).toBeVisible()
  await systems.first().locator('.el-checkbox').first().click()
  await expect(page.getByText(/共 \d+ 张库表/)).toBeVisible({ timeout: 10000 })

  // 管制属性单选可见
  await expect(page.getByText('管制属性', { exact: false }).first()).toBeVisible()

  // ① 默认非管制 → 后果提示
  const hint = page.locator('.reg-hint')
  await expect(hint).toContainText('本单位经营权可确权认定')

  // ② 选「管制业务」→ 提示切换
  await page.getByText('管制业务', { exact: true }).click()
  await expect(hint).toContainText('仅经公司授权取得')

  // ③ 切回非管制复原
  await page.getByText('非管制', { exact: true }).click()
  await expect(hint).toContainText('本单位经营权可确权认定')

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
