import { test, expect } from '@playwright/test'

/**
 * 授权申请历史查询管理:2 个 authMode Tab,布局对齐确权申请查询 + 我的申请。
 * 第一性优化:一行 = 一个申请(批量→清单/batchListId、一事一议→formNo),非单库表项;展开看多系统多库表明细;计数=申请数。
 * 覆盖:①两 Tab 带计数 + 清单级列(清单编号/系统数/库表数/年度)②切一事一议→申请单号列 ③展开行=多库表明细 ④进度详情抽屉。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]
function collect(page) {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('response', r => { if (r.url().includes('/api/') && r.status() >= 400) errs.push(`api ${r.status()}: ${r.request().method()} ${r.url()}`) })
  return errs
}

test.describe('授权申请历史 · 申请级(清单/申请单)聚合', () => {

  test('A 批量 Tab:清单级列(清单编号/年度/系统数/库表数)+ 统计条 + 工具,无错误', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/history', { waitUntil: 'networkidle' })
    await expect(page.getByRole('tab', { name: /批量授权 \(\d+\)/ })).toBeVisible({ timeout: 15000 })
    await expect(page.getByRole('tab', { name: /一事一议授权 \(\d+\)/ })).toBeVisible()
    // 申请级列头:清单编号 / 年度 / 系统数 / 库表数(证明一行=一个清单申请,而非单库表)
    await expect(page.getByRole('columnheader', { name: '清单编号' })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: '年度' })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: '系统数' })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: '库表数' })).toBeVisible()
    // 统计条 + 筛选 + 工具
    await expect(page.getByText('批量授权总数', { exact: false }).first()).toBeVisible()
    await expect(page.getByText('被授权方', { exact: false }).first()).toBeVisible()
    await expect(page.getByRole('button', { name: '导出' })).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('B 切一事一议 Tab → 列头变「申请单号」+ 系统数/库表数保留', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/history', { waitUntil: 'networkidle' })
    await page.getByRole('tab', { name: /一事一议授权 \(\d+\)/ }).click()
    await page.waitForTimeout(400)
    await expect(page.getByRole('columnheader', { name: '申请单号' })).toBeVisible({ timeout: 8000 })
    await expect(page.getByRole('columnheader', { name: '系统数' })).toBeVisible()
    await expect(page.getByText('一事一议授权总数', { exact: false }).first()).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('C 展开申请行 → 明细为多系统/多库表逐项(表6/表5)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/history', { waitUntil: 'networkidle' })
    const firstRow = page.locator('.prm-table-card > .el-table .el-table__row').first()
    if (!(await firstRow.isVisible().catch(() => false))) { test.skip(true, '当前无批量清单数据'); return }
    // 点展开图标
    await firstRow.locator('.el-table__expand-icon').first().click()
    await expect(page.locator('.ah-expand')).toBeVisible({ timeout: 8000 })
    // 展开内含明细子表(数据表列)+ 覆盖系统/库表说明
    await expect(page.locator('.ah-expand').getByRole('columnheader', { name: '数据表' }).first()).toBeVisible()
    await expect(page.locator('.ah-expand').getByText(/覆盖 \d+ 个系统|个系统/).first()).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('D 统计条状态卡下钻 + 进度详情抽屉', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/history', { waitUntil: 'networkidle' })
    const doneCard = page.locator('.stat-card').filter({ hasText: '已生效' }).first()
    if (await doneCard.isVisible().catch(() => false)) await doneCard.click()
    await page.waitForTimeout(300)
    const firstRow = page.locator('.prm-table-card > .el-table .el-table__row').first()
    if (await firstRow.isVisible().catch(() => false)) {
      await firstRow.getByRole('button', { name: '进度详情' }).click()
      await expect(page.locator('.el-drawer')).toBeVisible({ timeout: 8000 })
      await expect(page.getByText('进度跟踪', { exact: false })).toBeVisible()
    }
    expect(errs, errs.join('\n')).toEqual([])
  })
})
