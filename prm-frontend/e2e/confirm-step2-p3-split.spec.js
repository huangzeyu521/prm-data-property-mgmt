import { test, expect } from '@playwright/test'

/**
 * P3 验证:step2 系统级清单去 B–J(只留 表1/表2/权属凭证 + A);B–J 凭证仅在逐表区。
 *  客户服务系统(A/B/C/F 源 + H 关联):系统级清单应为 4 行(表1/权属/表2/A),
 *  不含 B/C/F/H 的应交材料;逐表凭证区仍 4 行(3 来源 B/C/F + 1 关联 H)。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('P3:step2 系统级去B–J + 逐表承载', async ({ page }) => {
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

  // 系统级清单(含「应交材料」列)= 4 行:表1/权属凭证/表2/A;不含 B/C/F/H 应交材料
  const systemTable = page.locator('.el-table').filter({ hasText: '应交材料' })
  await expect(systemTable.locator('.el-table__row')).toHaveCount(4)
  await expect(systemTable.getByText('公共采集情况说明', { exact: false })).toHaveCount(0)
  await expect(systemTable.getByText('公共数据授权说明', { exact: false })).toHaveCount(0)
  await expect(systemTable.getByText('个人/家庭隐私授权说明', { exact: false })).toHaveCount(0)

  // 逐表凭证区仍承载 B/C/F/H = 4 行
  const perTable = page.locator('.el-card').filter({ hasText: '逐表凭证材料' }).locator('.el-table').filter({ hasText: '凭证槽位' })  /* 限定 step2 卡:step3 逐表校验表同有该表头 */
  await expect(perTable.locator('.el-table__row')).toHaveCount(4)

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
