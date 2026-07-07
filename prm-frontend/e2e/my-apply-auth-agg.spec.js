import { test, expect } from '@playwright/test'

/**
 * 我的申请·授权两 Tab 申请级聚合(与授权申请历史对齐):一行 = 一个申请(批量→清单、一事一议→申请单),非单库表项。
 * 覆盖:①批量 Tab 有系统数/库表数列 + 年度 ②确权 Tab 不受影响(仍系统名称/权属类型)③展开授权行 = 多库表明细。须 PW_USER=apply。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]
function collect(page) {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('response', r => { if (r.url().includes('/api/') && r.status() >= 400) errs.push(`api ${r.status()}: ${r.request().method()} ${r.url()}`) })
  return errs
}

test.describe('我的申请 · 授权申请级聚合', () => {

  test('A 批量授权 Tab:申请级列(年度/系统数/库表数),无错误', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    await page.getByRole('tab', { name: /批量授权/ }).click()
    await page.waitForTimeout(400)
    await expect(page.getByRole('columnheader', { name: '系统数' })).toBeVisible({ timeout: 8000 })
    await expect(page.getByRole('columnheader', { name: '库表数' })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: '年度' })).toBeVisible()
    // 不应再有单库表的「数据表」主列(已收进展开明细)
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('B 一事一议授权 Tab:申请单级(系统数/库表数保留,无年度列)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    await page.getByRole('tab', { name: /一事一议授权/ }).click()
    await page.waitForTimeout(400)
    await expect(page.getByRole('columnheader', { name: '系统数' })).toBeVisible({ timeout: 8000 })
    await expect(page.getByRole('columnheader', { name: '库表数' })).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('C 确权 Tab 不受影响(仍系统名称/权属类型列)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    await page.getByRole('tab', { name: /初始确权/ }).click()
    await page.waitForTimeout(400)
    await expect(page.getByRole('columnheader', { name: '系统名称' })).toBeVisible({ timeout: 8000 })
    await expect(page.getByRole('columnheader', { name: /权属类型/ })).toBeVisible()
    // 确权 Tab 无「系统数」聚合列
    await expect(page.getByRole('columnheader', { name: '系统数' })).toHaveCount(0)
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('D 展开授权申请行 → 多系统/多库表明细(数据表列)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    await page.getByRole('tab', { name: /批量授权/ }).click()
    await page.waitForTimeout(400)
    const firstRow = page.locator('.prm-table-card > .el-table .el-table__row').first()
    if (!(await firstRow.isVisible().catch(() => false))) { test.skip(true, '当前无本人批量清单'); return }
    await firstRow.locator('.el-table__expand-icon').first().click()
    await expect(page.locator('.ma-expand')).toBeVisible({ timeout: 8000 })
    await expect(page.locator('.ma-expand').getByRole('columnheader', { name: '数据表' }).first()).toBeVisible()
    await expect(page.locator('.ma-expand').getByText(/个系统/).first()).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })
})
