import { test, expect } from '@playwright/test'
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]
test('P3 收尾:step1→step2→step3(材料校验)渲染无崩', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })
  const domains = page.locator('.cat-tree .el-tree > .el-tree-node')
  await expect(domains.first()).toBeVisible()
  await domains.first().locator('.el-tree-node__expand-icon').first().click()
  const sysNode = page.locator('.cat-tree .el-tree-node__children > .el-tree-node').filter({ hasText: '客户服务系统' })
  await sysNode.locator('.el-checkbox').first().click()
  await expect(page.getByText(/已选 \d+ 张库表/)).toBeVisible({ timeout: 10000 })
  await page.getByRole('button', { name: '下一步' }).click()           // next0 → 上传材料
  await expect(page.getByText('逐表凭证材料', { exact: false })).toBeVisible({ timeout: 12000 })
  await page.getByRole('button', { name: '下一步' }).click()           // next1 → 材料校验
  await expect(page.getByText('校验状态', { exact: false })).toBeVisible({ timeout: 12000 })
  // 一键校验(规则+AI)按钮存在(不点,避免慢 AI);页面渲染完好
  await expect(page.getByRole('button', { name: /一键校验/ })).toBeVisible()

  // 两层材料呈现(与 step2 对称,消除"8 vs 4 丢材料"误解):
  // ① 系统级材料标注 divider + 4 行
  await expect(page.getByText('系统级材料(表1 / 表2 / 权属凭证 / A 建设投入说明)')).toBeVisible()
  const sysTable = page.locator('.el-table').filter({ hasText: '材料名称' })
  await expect(sysTable.locator('.el-table__row')).toHaveCount(4)
  // ② 逐表凭证校验表:4 行、全过绿"通过"、附件与 step2 同源(各表专属文件)
  await expect(page.getByText(/逐表凭证校验\(4\)/)).toBeVisible()
  const vt = page.locator('.el-card').filter({ hasText: '逐表凭证校验' }).locator('.el-table').filter({ hasText: '凭证槽位' }).last()
  await expect(vt.locator('.el-table__row')).toHaveCount(4)
  await expect(vt.locator('.el-table__row').getByText('通过', { exact: true })).toHaveCount(4)  // UI评审:el-tag→纯色文字
  await expect(vt.getByText('公共采集情况说明.pdf', { exact: false })).toBeVisible()
  await expect(vt.getByText('用户入网协议(个人信息授权).pdf', { exact: false })).toBeVisible()
  // ③ 汇总 chip 与明细一致
  await expect(page.getByText(/逐表凭证 4\/4 · 齐/)).toBeVisible()

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
