import { test, expect } from '@playwright/test'

/**
 * 初始确权·确权范围树「单系统硬约束(事前)」验证:
 *  ① 业务域=纯导航 → 无勾选框(隐藏)
 *  ② 勾一个系统 → 出现「已锁定系统:X」提示
 *  ③ 其它系统(不同系统)即时置灰禁用(is-disabled),无法再选 → 一份申请限一个系统
 *  ④ 「清空换系统」→ 解锁,其它系统恢复可选
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('初始确权:单系统锁定 — 选一个系统后其它系统置灰,清空可换', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })

  const domains = page.locator('.cat-tree .el-tree > .el-tree-node')
  await expect(domains.first()).toBeVisible()

  // ① 业务域=纯导航:其本行的勾选框隐藏
  await expect(domains.first().locator('.el-tree-node__content').first().locator('.el-checkbox')).toBeHidden()

  // 展开前两个业务域,确保拿到 ≥2 个(不同)系统
  await domains.nth(0).locator('.el-tree-node__expand-icon').first().click()
  await domains.nth(1).locator('.el-tree-node__expand-icon').first().click()
  const systems = page.locator('.cat-tree .el-tree-node__children > .el-tree-node')
  await expect(systems.nth(1)).toBeVisible({ timeout: 10000 })

  // ② 勾第一个系统 → 锁定提示出现
  await systems.nth(0).locator('.el-checkbox').first().click()
  await expect(page.getByText(/已锁定系统/)).toBeVisible({ timeout: 10000 })

  // ③ 第二个(不同)系统即时置灰禁用
  await expect(systems.nth(1).locator('.el-checkbox').first()).toHaveClass(/is-disabled/)

  // ④ 清空换系统 → 解锁,第二系统恢复可选
  await page.getByRole('button', { name: /清空换系统/ }).click()
  await expect(page.getByText(/已锁定系统/)).toBeHidden()
  await expect(systems.nth(1).locator('.el-checkbox').first()).not.toHaveClass(/is-disabled/)

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
