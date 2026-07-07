import { test, expect } from '@playwright/test'

/**
 * P2 验证:step2 逐表凭证材料「上传原件/替换」→ 写回 ConfirmTableItem(名+materialId),UI 反映。
 *  客户服务系统逐表区(平台已预填附件)→ 对某行「替换」上传 e2e-upload.pdf →
 *  该行附件名变为 e2e-upload.pdf + 出现「预览」(matId 已回填) + 成功提示,零控制台错误。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('P2:step2 逐表凭证上传→写回表2', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })
  const domains = page.locator('.cat-tree .el-tree > .el-tree-node')
  await expect(domains.first()).toBeVisible()
  await domains.first().locator('.el-tree-node__expand-icon').first().click()
  const sysNode = page.locator('.cat-tree .el-tree-node__children > .el-tree-node').filter({ hasText: '客户服务系统' })
  await expect(sysNode).toBeVisible()
  await sysNode.locator('.el-checkbox').first().click()
  await expect(page.getByText(/已选 \d+ 张库表/)).toBeVisible({ timeout: 10000 })
  await page.getByRole('button', { name: '下一步' }).click()
  await expect(page.getByText('逐表凭证材料', { exact: false })).toBeVisible({ timeout: 12000 })

  const perTable = page.locator('.el-card').filter({ hasText: '逐表凭证材料' }).locator('.el-table').filter({ hasText: '凭证槽位' })  /* 限定 step2 卡:step3 逐表校验表同有该表头 */
  const firstRow = perTable.locator('.el-table__row').first()
  await expect(firstRow).toBeVisible()

  // 「替换」上传 e2e-upload.pdf(el-upload 隐藏 input)
  await firstRow.locator('input[type=file]').setInputFiles({
    name: 'e2e-upload.pdf', mimeType: 'application/pdf', buffer: Buffer.from('%PDF-1.4\n e2e test\n')
  })

  // 成功提示 + 该行附件名更新为上传件 + 预览按钮出现(matId 回填)
  await expect(page.locator('.el-message--success').filter({ hasText: '写回表2' })).toBeVisible({ timeout: 10000 })
  await expect(firstRow.getByText('e2e-upload.pdf', { exact: false })).toBeVisible()
  await expect(firstRow.getByRole('button', { name: '预览' })).toBeVisible()

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
