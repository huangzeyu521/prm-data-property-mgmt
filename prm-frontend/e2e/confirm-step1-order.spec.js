import { test, expect } from '@playwright/test'

/**
 * 批3 验证:第一步「填写申请」
 *  ① 因果顺序:库表清单(逐表表2) 在 经营权归集判定 之上(y 更小)。
 *  ② P3 集中校验:清空必填「申报权属主体」→ 点下一步 → 出现「还差 N 项」汇总提示。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('批3:step1 因果顺序 + 集中校验提示', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })

  // 选一个系统(整系统),让右侧库表清单 + 并集 + 经营权判定全部渲染
  const domains = page.locator('.cat-tree .el-tree > .el-tree-node')
  await expect(domains.first()).toBeVisible()
  await domains.first().locator('.el-tree-node__expand-icon').first().click()
  const systems = page.locator('.cat-tree .el-tree-node__children > .el-tree-node')
  await expect(systems.first()).toBeVisible()
  await systems.first().locator('.el-checkbox').first().click()
  await expect(page.getByText(/已选 \d+ 张库表/)).toBeVisible({ timeout: 10000 })

  // ① 因果顺序:库表清单 divider 在 经营权归集判定 divider 之上
  const libBox = await page.getByText('库表清单 · 逐表确权', { exact: false }).first().boundingBox()
  const consolBox = await page.getByText('经营权归集判定', { exact: false }).first().boundingBox()
  expect(libBox, '库表清单 divider 应可见').not.toBeNull()
  expect(consolBox, '经营权归集判定 divider 应可见').not.toBeNull()
  expect(libBox.y, '库表清单应在经营权归集判定之上(因→果)').toBeLessThan(consolBox.y)

  // ② P3 集中校验:清空「申报权属主体」→ 下一步 → 「还差 N 项」
  const rh = page.locator('.el-form-item', { hasText: '申报权属主体' }).first().locator('input')
  await rh.fill('')
  await page.getByRole('button', { name: '下一步' }).click()
  await expect(page.locator('.el-message--warning').filter({ hasText: '还差' })).toBeVisible({ timeout: 8000 })

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
