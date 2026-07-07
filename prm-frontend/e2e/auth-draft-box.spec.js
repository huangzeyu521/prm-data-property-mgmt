import { test, expect } from '@playwright/test'

/**
 * 授权域·申请草稿箱 + 自动保存 + 找回(一事一议 + 批量)。须 PW_USER=apply(徽标/深链按 canJumpTo 显隐)。
 * 覆盖:①草稿箱可达无错 ②本地未同步一事一议行 ③本地未同步批量行 ④一事一议向导找回 ⑤批量向导找回
 *      ⑥陈旧清理 ⑦我的申请授权草稿箱深链。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]
function collect(page) {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('response', r => { if (r.url().includes('/api/') && r.status() >= 400) errs.push(`api ${r.status()}: ${r.request().method()} ${r.url()}`) })
  return errs
}
async function uidOf(page) {
  return page.evaluate(() => { try { return JSON.parse(localStorage.getItem('prm-user') || 'null')?.userId || '' } catch { return '' } })
}
async function clearLocalDrafts(page) {
  await page.evaluate(() => {
    const rm = []
    for (let i = 0; i < localStorage.length; i++) { const k = localStorage.key(i); if (k && k.startsWith('prm-draft:')) rm.push(k) }
    rm.forEach(k => localStorage.removeItem(k))
  })
}
async function seedSpecial(page, ageDays = 0) {
  const uid = await uidOf(page)
  await page.evaluate(({ uid, ageDays }) => {
    localStorage.setItem(`prm-draft:auth-special:${uid || 'anon'}:new`, JSON.stringify({
      listForm: { granteeOrg: '测试被授权方', scenario: '测试使用场景', rightType: '使用权' },
      pendingAsset: null, items: [{ assetName: '示例数据表A' }],
      title: '测试被授权方 · 测试使用场景', __ts: Date.now() - ageDays * 86400000
    }))
  }, { uid, ageDays })
  return uid
}
async function seedBatch(page, ageDays = 0) {
  const uid = await uidOf(page)
  await page.evaluate(({ uid, ageDays }) => {
    localStorage.setItem(`prm-draft:auth-batch:${uid || 'anon'}:new`, JSON.stringify({
      listForm: { listYear: '2026', granteeOrg: '测试被授权方' }, items: [{ assetName: '示例数据表A' }],
      externalGrantee: false, title: '测试被授权方 · 2026 年度', __ts: Date.now() - ageDays * 86400000
    }))
  }, { uid, ageDays })
  return uid
}

test.describe('授权·申请草稿箱 · 自动保存 · 找回', () => {

  test('A 草稿箱页可达且渲染,无运行时/接口错误', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/draft-box', { waitUntil: 'networkidle' })
    await expect(page.getByRole('heading', { name: '申请草稿箱' })).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('草稿仅本人可见', { exact: false })).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('B 本地未同步·一事一议草稿行', async ({ page }) => {
    await page.goto('/dpr/auth/draft-box', { waitUntil: 'networkidle' })
    await clearLocalDrafts(page); await seedSpecial(page)
    await page.goto('/dpr/auth/draft-box', { waitUntil: 'networkidle' })
    const row = page.locator('.el-table__row').filter({ hasText: '本地未同步' }).first()
    await expect(row).toBeVisible({ timeout: 10000 })
    await expect(row.getByText('一事一议', { exact: true }).first()).toBeVisible()
    await expect(row.getByText('本地待续填', { exact: false }).first()).toBeVisible()
    await clearLocalDrafts(page)
  })

  test('C 本地未同步·批量草稿行', async ({ page }) => {
    await page.goto('/dpr/auth/draft-box', { waitUntil: 'networkidle' })
    await clearLocalDrafts(page); await seedBatch(page)
    await page.goto('/dpr/auth/draft-box', { waitUntil: 'networkidle' })
    const row = page.locator('.el-table__row').filter({ hasText: '本地未同步' }).first()
    await expect(row).toBeVisible({ timeout: 10000 })
    await expect(row.getByText('批量', { exact: true }).first()).toBeVisible()
    await clearLocalDrafts(page)
  })

  test('D 一事一议向导 → 找回提示 → 恢复', async ({ page }) => {
    await page.goto('/dpr/auth/draft-box', { waitUntil: 'networkidle' })
    await clearLocalDrafts(page); await seedSpecial(page)
    await page.goto('/dpr/auth/wizard', { waitUntil: 'networkidle' })
    await expect(page.getByText('恢复未完成的草稿')).toBeVisible({ timeout: 12000 })
    await expect(page.getByText('一事一议授权草稿', { exact: false })).toBeVisible()
    await page.getByRole('button', { name: '恢复' }).click()
    await expect(page.getByText('已恢复本地草稿', { exact: false })).toBeVisible({ timeout: 8000 })
    await clearLocalDrafts(page)
  })

  test('E 批量向导 → 找回提示 → 恢复', async ({ page }) => {
    await page.goto('/dpr/auth/draft-box', { waitUntil: 'networkidle' })
    await clearLocalDrafts(page); await seedBatch(page)
    await page.goto('/dpr/auth/batch-wizard', { waitUntil: 'networkidle' })
    await expect(page.getByText('恢复未完成的草稿')).toBeVisible({ timeout: 12000 })
    await expect(page.getByText('批量授权草稿', { exact: false })).toBeVisible()
    await page.getByRole('button', { name: '恢复' }).click()
    await expect(page.getByText('已恢复本地草稿', { exact: false })).toBeVisible({ timeout: 8000 })
    await clearLocalDrafts(page)
  })

  test('F 陈旧草稿一键清理', async ({ page }) => {
    await page.goto('/dpr/auth/draft-box', { waitUntil: 'networkidle' })
    await clearLocalDrafts(page); await seedSpecial(page, 100) // 100 天前 → 陈旧
    await page.goto('/dpr/auth/draft-box', { waitUntil: 'networkidle' })
    await expect(page.getByText('陈旧', { exact: true }).first()).toBeVisible()
    const cleanupBtn = page.getByRole('button', { name: /清理陈旧草稿/ })
    await expect(cleanupBtn).toBeVisible()
    await cleanupBtn.click()
    const dlg = page.locator('.el-message-box')
    await expect(dlg).toBeVisible()
    await dlg.getByRole('button', { name: /确定|确认/ }).click()
    await expect(page.getByText(/已清理 \d+ 份陈旧草稿/)).toBeVisible({ timeout: 10000 })
    await clearLocalDrafts(page)
  })

  test('G 我的申请授权草稿箱深链 → 跳转', async ({ page }) => {
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    const link = page.getByRole('button', { name: /授权草稿箱/ })
    await expect(link, '我的申请应有授权草稿箱深链').toBeVisible({ timeout: 10000 })
    await link.click()
    await expect(page).toHaveURL(/\/dpr\/auth\/draft-box/)
    await expect(page.getByRole('heading', { name: '申请草稿箱' })).toBeVisible()
  })

  // ===== 云端草稿回归:修复前 pageAuthApply(status) 400、pageBatchList(status) 漏筛(申报稿/批准泄露) =====
  test('H 云端批量草案在草稿箱可见,且 /page 端点不再 400', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/draft-box', { waitUntil: 'networkidle' })
    await clearLocalDrafts(page)
    await page.locator('.prm-toolbar').getByRole('button', { name: '刷新' }).click()
    await page.waitForTimeout(1500)
    expect(errs.filter((e) => e.includes('/page') && e.includes('400')), errs.join('\n')).toEqual([])
    const batchCloud = page.locator('.el-table__row').filter({ hasText: '批量' }).filter({ hasNotText: '本地未同步' })
    expect(await batchCloud.count(), '修复后应能加载本人云端批量草案清单').toBeGreaterThan(0)
  })
})
