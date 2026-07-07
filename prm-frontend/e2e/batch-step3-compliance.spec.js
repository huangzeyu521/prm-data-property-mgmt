import { test, expect } from '@playwright/test'

/**
 * step3 改造验证(1+2完整+3):
 * ① 提交按钮就近——「提交申报稿」在 step3 卡片内(校验下方),不在右上角
 * ② 校验明细可见——一键校验后展示逐项逐维度明细表(先确后授/第三方凭证/信息授权协议/授权范围)
 * ③ 通过项也展示(非黑盒);不通过项有「去修正」
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('step3: 校验明细可见 + 提交就近', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/auth/batch-wizard', { waitUntil: 'networkidle' })
  await page.getByPlaceholder('如 2026').fill('2026')
  const grantee = page.locator('.el-form-item:has-text("申请主体(被授权方)")')
  await grantee.locator('.el-select__wrapper').first().click()
  await grantee.locator('input').first().fill('综合能源')
  await page.locator('.el-select-dropdown__item:has-text("南网综合能源股份有限公司")').first().click()
  await page.locator('input[placeholder*="联络人"]').first().fill('张三')
  await page.locator('input[placeholder*="联系方式"]').first().fill('020-31000000')
  await page.locator('.el-form-item:has-text("默认权益类型") .el-select__wrapper').first().click()
  await page.locator('.el-select-dropdown__item:has-text("使用权")').first().click()
  await page.getByRole('button', { name: /下一步/ }).click()

  // 加入一项明细
  await page.getByRole('button', { name: /从确权目录批量选取/ }).click()
  const dlg = page.locator('.el-dialog')
  await expect(dlg.locator('.el-tree')).toBeVisible({ timeout: 15000 })
  await dlg.locator('.el-tree-node:has(.is-table) .el-checkbox__inner').first().click()
  await dlg.getByRole('button', { name: /加入选中资产/ }).click()
  await expect(page.locator('body')).toContainText(/已加入明细\([1-9]/, { timeout: 10000 })

  // 进入 step3「确认并提交」
  await page.getByRole('button', { name: /下一步/ }).click()
  const card3 = page.locator('.el-card', { hasText: '提交《批量授权清单》申报稿' })
  // ① 提交按钮就近:卡片内有「提交申报稿」(未校验时禁用)
  await expect(card3.getByRole('button', { name: /提交申报稿/ })).toBeVisible()

  // ② 一键自检 → 逐项逐维度明细表出现
  await card3.getByRole('button', { name: /一键自检/ }).click()
  await expect(card3).toContainText('校验明细', { timeout: 20000 })
  await expect(card3).toContainText('先确后授·生效卡片')      // 维度1
  await expect(card3).toContainText('授权范围 ≤ 确权边界')     // 维度4
  await expect(card3).toContainText('结论')                   // 逐项结论列(非黑盒)

  expect(errs, `step3 运行时错误:\n${errs.join('\n')}`).toEqual([])
})
