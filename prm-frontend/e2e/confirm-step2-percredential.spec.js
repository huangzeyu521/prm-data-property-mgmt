import { test, expect } from '@playwright/test'

/**
 * P1 验证:step2「逐表凭证材料」区(投影自表2 tableItems·与表2 同源)。
 *  客户服务系统 4 表(CS_COMPLAINT A+H / CS_KB_FAQ B / CS_KB_POLICY C / CS_KB_EXT F):
 *   逐表区应出 4 行 = 3 来源凭证(B/C/F)+ 1 关联资料(H),且各表附件为其专属文件(承接 MEDIUM② 修复)。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('P1:step2 逐表凭证材料区 · 投影自表2同源', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })

  // 展开首个业务域 → 勾「客户服务系统」系统节点(整系统纳入)
  const domains = page.locator('.cat-tree .el-tree > .el-tree-node')
  await expect(domains.first()).toBeVisible()
  await domains.first().locator('.el-tree-node__expand-icon').first().click()
  const sysNode = page.locator('.cat-tree .el-tree-node__children > .el-tree-node').filter({ hasText: '客户服务系统' })
  await expect(sysNode).toBeVisible()
  await sysNode.locator('.el-checkbox').first().click()
  await expect(page.getByText(/已选 \d+ 张库表/)).toBeVisible({ timeout: 10000 })

  // 下一步 → 进入 step2(上传材料)
  await page.getByRole('button', { name: '下一步' }).click()
  await expect(page.getByText('逐表凭证材料', { exact: false })).toBeVisible({ timeout: 12000 })

  // 定位逐表表格(唯一含「凭证槽位」列头),断言在其内
  const perTable = page.locator('.el-card').filter({ hasText: '逐表凭证材料' }).locator('.el-table').filter({ hasText: '凭证槽位' })  /* 限定 step2 卡:step3 逐表校验表同有该表头 */
  await expect(perTable).toBeVisible()
  await expect(perTable.locator('.el-table__row')).toHaveCount(4)

  // 槽位:来源凭证 ×3(B/C/F)+ 关联资料 ×1(H)
  await expect(perTable.getByText('来源凭证', { exact: false })).toHaveCount(3)
  await expect(perTable.getByText('关联资料', { exact: false })).toHaveCount(1)

  // 各表专属附件(MEDIUM② 修复的逐字母文件,经平台预填进 tableItems)
  await expect(perTable.getByText('公共采集情况说明.pdf', { exact: false })).toBeVisible()
  await expect(perTable.getByText('公共数据授权说明.pdf', { exact: false })).toBeVisible()
  await expect(perTable.getByText('其他来源情况说明.pdf', { exact: false })).toBeVisible()
  await expect(perTable.getByText('用户入网协议(个人信息授权).pdf', { exact: false })).toBeVisible()

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
