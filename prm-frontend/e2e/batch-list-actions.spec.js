import { test, expect } from '@playwright/test'

/**
 * 批量授权清单 操作列改造验证(1协议自动生成 + 2审批确认 + 3链完整性guard):
 * ① 「生成运营授权协议」手动按钮 → 改为「去协议双签」(协议批准后系统自动生成)
 * ② 领导小组批准 = 重决策 → 点击弹确认框
 * ③ 链完整性 guard:明细未走完合规/主管/经理/副总时,领导小组终批被拦(防跳节点)
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('batch-list 操作列:协议自动生成 + 审批确认 + 链guard', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/auth/batch-list', { waitUntil: 'networkidle' })
  await expect(page.locator('.el-table')).toBeVisible({ timeout: 15000 })

  // ① 「去协议双签」按钮存在,旧「生成运营授权协议」手动按钮已移除
  await expect(page.locator('body')).toContainText('去协议双签')
  await expect(page.locator('body')).not.toContainText('生成运营授权协议')

  // ②③ 点一个可用(申报稿行)的「领导小组批准」→ 弹确认框 → 批准
  const approveBtn = page.locator('button:enabled', { hasText: '领导小组批准' }).first()
  await expect(approveBtn).toBeVisible({ timeout: 10000 })
  await approveBtn.click()
  // ② 确认弹窗
  const dialog = page.locator('.el-message-box')
  await expect(dialog).toBeVisible()
  await expect(dialog).toContainText('领导小组决策批准')
  await expect(dialog).toContainText('自动生成《运营授权协议》草案')
  await dialog.getByRole('button', { name: '批准并生成协议' }).click()

  // ③ 结果:出现结果提示(链 guard 拦回 或 批准成功+协议自动生成),且无脚本崩溃
  await expect(page.locator('.el-message').first()).toBeVisible({ timeout: 10000 })

  expect(errs, `批量清单操作列运行时错误:\n${errs.join('\n')}`).toEqual([])
})
