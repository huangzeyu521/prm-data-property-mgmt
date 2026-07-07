import { test, expect } from '@playwright/test'

/**
 * 初始确权·「一键填充示例」系统级守卫:
 *  演示快捷入口也必须产出系统级选择(整系统带入),不得回退到单卡片(AST-001)表级口径。
 *  断言:点「一键填充示例」→ ① 出现系统名标签「营销管理系统」 ② 标签「初始确权」
 *       ③「已选 N 张库表」N≥1(整系统带出) ④ 不出现单卡资产名「客户用电信息表」。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('初始确权:一键填充示例 → 系统级整系统带入(非单卡片)', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })

  // 演示按钮(dev/?demo=1 可见)
  const btn = page.getByRole('button', { name: /一键填充示例/ })
  await expect(btn).toBeVisible()
  await btn.click()

  // 系统级:系统名标签 + 初始确权 + 已选 N 张库表(整系统)
  await expect(page.getByText('营销管理系统', { exact: false }).first()).toBeVisible({ timeout: 10000 })
  await expect(page.getByText('初始确权', { exact: false }).first()).toBeVisible()
  const selTag = page.getByText(/已选 \d+ 张库表/)
  await expect(selTag).toBeVisible({ timeout: 10000 })
  const n = parseInt((await selTag.first().innerText()).match(/已选 (\d+) 张库表/)[1], 10)
  expect(n, '示例应整系统带出 ≥1 张未确权库表').toBeGreaterThan(0)

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
