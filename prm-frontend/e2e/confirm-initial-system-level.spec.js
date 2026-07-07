import { test, expect } from '@playwright/test'

/**
 * 初始确权·系统级粒度守卫(35号文 附录C 表1《数据确权信息清单(系统级)》一系统一行):
 *  初始确权的确权单元=信息系统,申报人只能选「一个系统」,整系统纳入,
 *  不得下钻到库表级去单挑子集。
 *  断言:① 业务域可展开 ② 系统节点为叶子(expand-icon.is-leaf)→ 无法下钻到模块/库表
 *       ③ 勾系统 → 整系统带出(已选 N 张库表, N≥1),证明"整系统"而非"逐表"。
 *  对照:确权变更模式(/dpr/confirm/change-*)仍保留逐表/逐模块,故此守卫只针对初始确权向导。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('初始确权:系统级粒度 — 系统为叶子不可下钻到库表,勾系统整体带出', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })

  // 标题反映系统级口径
  await expect(page.getByText('系统级整体确权', { exact: false }).first()).toBeVisible()

  // 业务域(第1层)可展开
  const domains = page.locator('.cat-tree .el-tree > .el-tree-node')
  await expect(domains.first()).toBeVisible()
  await domains.first().locator('.el-tree-node__expand-icon').first().click()

  // 系统节点(第2层)出现
  const systems = page.locator('.cat-tree .el-tree-node__children > .el-tree-node')
  await expect(systems.first()).toBeVisible({ timeout: 10000 })

  // 关键守卫:系统节点是叶子(expand-icon 带 is-leaf)→ 无展开箭头 → 无法下钻到模块/库表
  await expect(systems.first().locator('.el-tree-node__expand-icon').first()).toHaveClass(/is-leaf/)

  // 勾系统 → 整系统带出(证明确权对象是"整系统"而非"逐表挑选")
  await systems.first().locator('.el-checkbox').first().click()
  const selTag = page.getByText(/已选 \d+ 张库表/)
  await expect(selTag).toBeVisible({ timeout: 10000 })
  const n = parseInt((await selTag.first().innerText()).match(/已选 (\d+) 张库表/)[1], 10)
  expect(n, '整系统应带出 ≥1 张未确权库表').toBeGreaterThan(0)

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
