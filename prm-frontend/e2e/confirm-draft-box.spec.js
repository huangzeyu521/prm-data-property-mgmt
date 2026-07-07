import { test, expect } from '@playwright/test'

/**
 * 申请草稿箱 + 自动保存 + 找回提示(确权域)。
 * 覆盖:①草稿箱可达且无错误 ②预置本地未同步草稿 → 草稿箱呈现「本地未同步」行 ③进向导 → 找回提示 → 恢复回填。
 * 自动保存的服务端同步依赖左树选系统(易漂移),作为 best-effort(D)可跳过。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]
function collect(page) {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('response', r => { if (r.url().includes('/api/') && r.status() >= 400) errs.push(`api ${r.status()}: ${r.request().method()} ${r.url()}`) })
  return errs
}

// 读当前登录用户 id → 构造本地草稿键(与 useDraftAutosave.localDraftKey 一致)
async function seedLocalDraft(page, { scope = 'confirm-initial', systemName = '营销管理系统', title, ageDays = 0 } = {}) {
  const uid = await page.evaluate(() => { try { return JSON.parse(localStorage.getItem('prm-user') || 'null')?.userId || '' } catch { return '' } })
  await page.evaluate(({ uid, scope, systemName, title, ageDays }) => {
    const key = `prm-draft:${scope}:${uid || 'anon'}:new`
    localStorage.setItem(key, JSON.stringify({
      form: { systemName, rightHolder: '测试权属主体', subjectLevel: '分省公司', systemOwner: '', contactInfo: '' },
      tableItems: [{ tableName: '示例库表A' }],
      changeTriggers: [], registerType: scope === 'confirm-change' ? '确权变更' : '初始确权',
      title: title || `${systemName} · 1 表`, __ts: Date.now() - (ageDays * 86400000)
    }))
  }, { uid, scope, systemName, title, ageDays })
  return uid
}
async function clearLocalDrafts(page) {
  await page.evaluate(() => {
    const rm = []
    for (let i = 0; i < localStorage.length; i++) { const k = localStorage.key(i); if (k && k.startsWith('prm-draft:')) rm.push(k) }
    rm.forEach(k => localStorage.removeItem(k))
  })
}

test.describe('申请草稿箱 · 自动保存 · 找回', () => {

  test('A 草稿箱页可达且渲染,无运行时/接口错误', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/draft-box', { waitUntil: 'networkidle' })
    await expect(page.getByRole('heading', { name: '申请草稿箱' })).toBeVisible({ timeout: 15000 })
    // 自动保存说明可见(私有 + 不进审核)
    await expect(page.getByText('草稿仅本人可见', { exact: false })).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('B 预置本地未同步草稿 → 草稿箱呈现「本地未同步」行 + 继续填写/删除', async ({ page }) => {
    const errs = collect(page)
    // 先到任一同源页拿到 localStorage 上下文,再预置
    await page.goto('/dpr/confirm/draft-box', { waitUntil: 'networkidle' })
    await clearLocalDrafts(page)
    await seedLocalDraft(page, { systemName: '营销管理系统' })
    await page.goto('/dpr/confirm/draft-box', { waitUntil: 'networkidle' })

    await expect(page.getByText('本地未同步').first()).toBeVisible({ timeout: 10000 })
    await expect(page.getByRole('cell', { name: /营销管理系统/ }).first()).toBeVisible()
    await expect(page.getByText('本地待续填', { exact: false }).first()).toBeVisible()
    await expect(page.getByRole('button', { name: '继续填写' }).first()).toBeVisible()
    await clearLocalDrafts(page)
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('C 全新进入向导 → 找回提示 → 恢复回填', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })
    await clearLocalDrafts(page)
    await seedLocalDraft(page, { systemName: '营销管理系统' })
    await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })

    // 找回提示弹窗
    await expect(page.getByText('恢复未完成的草稿')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('是否恢复继续填写', { exact: false })).toBeVisible()
    await page.getByRole('button', { name: '恢复' }).click()
    await expect(page.getByText('已恢复本地草稿', { exact: false })).toBeVisible({ timeout: 8000 })
    await clearLocalDrafts(page)
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('D 全新进入向导 → 丢弃草稿 → 本地键被清除', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })
    await clearLocalDrafts(page)
    const uid = await seedLocalDraft(page, { systemName: '客户服务系统' })
    await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })

    await expect(page.getByText('恢复未完成的草稿')).toBeVisible({ timeout: 10000 })
    await page.getByRole('button', { name: '丢弃' }).click()
    // 丢弃后本地 'new' 键应被移除
    const stillThere = await page.evaluate((uid) => !!localStorage.getItem(`prm-draft:confirm-initial:${uid || 'anon'}:new`), uid)
    expect(stillThere, '丢弃后本地草稿键应被清除').toBe(false)
    expect(errs, errs.join('\n')).toEqual([])
  })

  // ===== P1:菜单草稿数徽标 / 完成度材料缺口列 / 我的申请深链(须 PW_USER=apply,徽标与深链按 canJumpTo 显隐)=====
  test('E 菜单「申请草稿箱」显示草稿数徽标', async ({ page }) => {
    await page.goto('/dpr/confirm/draft-box', { waitUntil: 'networkidle' })
    await clearLocalDrafts(page)
    await seedLocalDraft(page, { systemName: '营销管理系统' })
    // 路由切换(仍在数据确权管理组内,子菜单保持展开;非向导→无找回弹窗)触发徽标刷新
    await page.goto('/dpr/confirm/card', { waitUntil: 'networkidle' })
    const item = page.locator('.el-menu-item').filter({ hasText: '申请草稿箱' }).first()
    await expect(item.locator('.menu-count'), '有草稿时应显示计数徽标').toBeVisible({ timeout: 8000 })
    await clearLocalDrafts(page)
  })

  test('F 草稿箱完成度列含「材料缺口」', async ({ page }) => {
    await page.goto('/dpr/confirm/draft-box', { waitUntil: 'networkidle' })
    await expect(page.getByRole('columnheader', { name: /完成度 · 材料缺口/ })).toBeVisible({ timeout: 10000 })
  })

  test('G 我的申请深链 → 跳转确权草稿箱', async ({ page }) => {
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    const link = page.getByRole('button', { name: /确权草稿箱/ })
    await expect(link, '我的申请应有确权草稿箱深链').toBeVisible({ timeout: 10000 })
    await link.click()
    await expect(page).toHaveURL(/\/dpr\/confirm\/draft-box/)
    await expect(page.getByRole('heading', { name: '申请草稿箱' })).toBeVisible()
  })

  // ===== P2:陈旧草稿策略 =====
  test('I 陈旧草稿:>90天本地草稿标「陈旧」+ 一键清理', async ({ page }) => {
    await page.goto('/dpr/confirm/draft-box', { waitUntil: 'networkidle' })
    await clearLocalDrafts(page)
    await seedLocalDraft(page, { systemName: '客户服务系统', ageDays: 100 }) // 100 天前 → 陈旧
    await page.goto('/dpr/confirm/draft-box', { waitUntil: 'networkidle' })
    // 陈旧标签 + 清理按钮
    await expect(page.getByText('陈旧', { exact: true }).first()).toBeVisible({ timeout: 10000 })
    const cleanupBtn = page.getByRole('button', { name: /清理陈旧草稿/ })
    await expect(cleanupBtn).toBeVisible()
    await cleanupBtn.click()
    const dlg = page.locator('.el-message-box')
    await expect(dlg).toBeVisible()
    await dlg.getByRole('button', { name: /确定|确认/ }).click()
    await expect(page.getByText(/已清理 \d+ 份陈旧草稿/)).toBeVisible({ timeout: 10000 })
    await clearLocalDrafts(page)
  })

  // ===== 云端草稿回归:修复前 /apply/page 携带 status 入参致 400,云端草稿从不显示 =====
  test('J 云端确权草稿(本人)在草稿箱可见,且 /page 端点不再 400', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/draft-box', { waitUntil: 'networkidle' })
    await clearLocalDrafts(page)
    await page.locator('.prm-toolbar').getByRole('button', { name: '刷新' }).click()
    await page.waitForTimeout(1500)
    expect(errs.filter((e) => e.includes('/confirm/apply/page') && e.includes('400')), errs.join('\n')).toEqual([])
    const cloud = page.locator('.el-table__row').filter({ hasNotText: '本地未同步' })
    expect(await cloud.count(), '修复后应能加载本人云端确权草稿(demo 库存 9 份)').toBeGreaterThan(0)
  })
})
