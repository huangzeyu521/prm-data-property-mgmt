import { test, expect } from '@playwright/test'

/**
 * 一事一议「单场景·多表」一站式验证(对齐 35号文 表5 多行申请单):
 *  ① 一键示例:填单头 + createForm(formNo) → 落 step1(明细)
 *  ② 从确权目录多选加入数据表(树 multiple) → 明细可累加
 *  ③《表5》系统按数据表多行生成下载(非上传)
 *  ④ step2 提交前自检:逐表 dims 门禁,全过点亮提交
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('一事一议:单场景多表 一站式(建单→树多选→表5多行→逐表自检)', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/auth/wizard', { waitUntil: 'networkidle' })

  // ① 一键示例:填单头(权益=使用权/被授权方/场景/主管/联系)+ createForm → 落 step1
  await page.getByRole('button', { name: /一键示例/ }).click()
  const addBtn = page.getByRole('button', { name: /从确权目录选取数据资产/ })
  await expect(addBtn).toBeVisible({ timeout: 15000 })
  const detailTable = page.locator('.el-table').filter({ hasText: '生效卡片' }).first()

  // ② 从确权目录多选加入(树 multiple;使用权池已知有数据)
  await addBtn.click()
  const dlg = page.locator('.el-dialog', { hasText: '从确权目录选取数据资产(可多张)' })
  await expect(dlg).toBeVisible()
  await page.waitForTimeout(1500)
  const leaves = dlg.locator('.is-table')
  const lc = await leaves.count()
  expect(lc, '使用权资源池应有可授数据表(种子)').toBeGreaterThan(0)
  await leaves.nth(0).locator('xpath=ancestor::*[contains(@class,"el-tree-node__content")][1]').locator('.el-checkbox').click()
  if (lc > 1) {
    await leaves.nth(1).locator('xpath=ancestor::*[contains(@class,"el-tree-node__content")][1]').locator('.el-checkbox').click()
  }
  await dlg.getByRole('button', { name: /加入选中资产/ }).click()
  await expect(dlg).toBeHidden()
  await expect(detailTable.locator('.el-table__row')).not.toHaveCount(0)

  // ③《表5》系统生成行(非上传):有"系统生成" + "生成《表5》并下载"触发下载
  const t5row = page.locator('.el-table__row', { hasText: '表5' }).first()
  await expect(t5row).toContainText('系统生成')
  const [dl] = await Promise.all([
    page.waitForEvent('download'),
    t5row.getByRole('button', { name: /生成《表5》并下载/ }).click()
  ])
  expect(dl.suggestedFilename()).toContain('表5')

  // ④ 下一步 → step2 提交前自检:逐表校验明细 + 全过点亮提交
  await page.getByRole('button', { name: '下一步' }).click()
  await page.getByRole('button', { name: /一键自检/ }).click()
  await expect(page.getByText(/校验明细/)).toBeVisible({ timeout: 20000 })
  await expect(page.getByText('全部合规,可提交')).toBeVisible()
  await expect(page.getByRole('button', { name: /提交申请单/ })).toBeEnabled()

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
