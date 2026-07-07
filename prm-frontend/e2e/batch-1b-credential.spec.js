import { test, expect } from '@playwright/test'

/**
 * 1-B 实证:涉第三方明细行的「第三方凭证」列应显示"确权带出"(确权侧 thirdPartyInfo 带出,免重传)。
 * 选营销管理系统「市场交易结算表」(确权种子:涉第三方来源E/商业秘密 + thirdPartyInfo 有内容)。
 */
test('1-B: 涉三方行第三方凭证=确权带出', async ({ page }) => {
  const errs = []
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

  await page.getByRole('button', { name: /从确权目录批量选取/ }).click()
  const dlg = page.locator('.el-dialog')
  await expect(dlg.locator('.el-tree')).toBeVisible({ timeout: 15000 })
  // 勾选"市场交易结算表"叶子(涉第三方)
  const leaf = dlg.locator('.el-tree-node:has-text("市场交易结算表")').last()
  await leaf.locator('.el-checkbox__inner').first().click()
  await dlg.getByRole('button', { name: /加入选中资产/ }).click()
  await expect(page.locator('body')).toContainText(/已加入明细\([1-9]/, { timeout: 10000 })

  // 明细表:第三方凭证列存在;市场交易结算表行 涉三方=涉、凭证=确权带出
  const itemsTable = page.locator('.el-table').last()
  await expect(itemsTable).toContainText('第三方凭证')
  await expect(itemsTable).toContainText('信息授权协议') // 隐私对称列存在
  const row = itemsTable.locator('tbody tr', { hasText: '市场交易结算表' }).first()
  await expect(row).toContainText('涉')
  // 第三方凭证 + 信息授权协议(涉商业秘密)均应确权带出(确权侧已留存,免重传)
  await expect(row.getByText('确权带出')).toHaveCount(2)

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
