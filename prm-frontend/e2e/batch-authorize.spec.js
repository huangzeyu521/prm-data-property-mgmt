import { test, expect } from '@playwright/test'

/**
 * 批量授权一站式·关键流程回归:建清单头(含表5/表6 联系人/联系方式必填)→ 选权益类型 →
 * 从确权目录资源池(先确后授+权属可授+经营权对外开放)勾选 → 加入明细 → 校验明细非空 + 跨系统域自动判定。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('batch authorize: 建清单 → 资源池选取 → 加入明细', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/auth/batch-wizard', { waitUntil: 'networkidle' })

  // 步骤1:清单头(年度/被授权方/联系人/联系方式 必填,权益类型驱动资源池过滤)
  await page.getByPlaceholder('如 2026').fill('2026')
  // 被授权方 = 南网组织选择器(listOrg);键入即从已确权种子组织里命中(验证 21 组织已加载),点选「南网综合能源股份有限公司」
  const grantee = page.locator('.el-form-item:has-text("申请主体(被授权方)")')
  await grantee.locator('.el-select__wrapper').first().click()
  await grantee.locator('input').first().fill('综合能源')
  await page.locator('.el-select-dropdown__item:has-text("南网综合能源股份有限公司")').first().click()
  await page.locator('input[placeholder*="联络人"]').first().fill('张三')
  await page.locator('input[placeholder*="联系方式"]').first().fill('020-31000000')
  await page.locator('.el-form-item:has-text("默认权益类型") .el-select__wrapper').first().click()
  await page.locator('.el-select-dropdown__item:has-text("数据加工使用权")').first().click()
  await page.getByRole('button', { name: /下一步/ }).click()

  // 步骤2:打开资源池 picker
  const pickerBtn = page.getByRole('button', { name: /从确权目录批量选取/ })
  await expect(pickerBtn).toBeVisible()
  await pickerBtn.click()

  // 资源池树渲染(系统→模块→库表),勾选一个含库表叶的节点
  const dlg = page.locator('.el-dialog')
  await expect(dlg.locator('.el-tree')).toBeVisible({ timeout: 15000 })
  await dlg.locator('.el-tree-node:has(.is-table) .el-checkbox__inner').first().click()
  await dlg.getByRole('button', { name: /加入选中资产/ }).click()

  // 校验:已加入明细 ≥ 1 项 + 跨系统域横幅出现
  await expect(page.locator('body')).toContainText(/已加入明细\([1-9]\d* 项\)/, { timeout: 10000 })
  await expect(page.locator('body')).toContainText('是否跨系统域')
  expect(errs, `批量授权流程运行时错误:\n${errs.join('\n')}`).toEqual([])
})
