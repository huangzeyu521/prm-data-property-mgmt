import { test, expect } from '@playwright/test'

/**
 * 授权(一事一议 + 批量)一站式「保存草稿」全生命周期(PDD 8.1) —— 运行须 PW_USER=apply。
 * 覆盖:① 两向导均有独立「保存草稿」键;② 填一半即可主动存草稿(不校验完成度、不前进);
 *      ③「我的申请」授权草稿→编辑(就地续填)/删除;④ 在审→撤回。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i, /sourcemap/i]

function collect(page) {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('response', r => { if (r.url().includes('/api/') && r.status() >= 400) errs.push(`api ${r.status()}: ${r.request().method()} ${r.url()}`) })
  return errs
}
function clearListeners(page) { page.removeAllListeners('console'); page.removeAllListeners('pageerror'); page.removeAllListeners('response') }

/** 一事一议:填最简单头 + 从资源池加一张表(满足存草稿底线=≥1数据表)。 */
async function buildSpecialDraft(page) {
  await page.goto('/dpr/auth/wizard', { waitUntil: 'networkidle' })
  // 单头:权益类型(radio)+ 被授权方 + 场景 —— 用一键示例最稳(建单+加示例表)
  const demo = page.getByRole('button', { name: /一键示例/ })
  await expect(demo).toBeVisible({ timeout: 10000 })
  await demo.click()
  // 示例会建 formNo + 加入示例数据表 → step 应到明细,items≥1
  await expect(page.getByText(/已.*加入|已加入数据表|示例/).first()).toBeVisible({ timeout: 15000 })
}

test.describe('授权·保存草稿全生命周期(apply)', () => {

  test('一事一议向导:操作条含独立「保存草稿」键', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/wizard', { waitUntil: 'networkidle' })
    await expect(page.getByRole('button', { name: '保存草稿' }), '一事一议应有保存草稿键').toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('批量向导:操作条含独立「保存草稿」键', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/batch-wizard', { waitUntil: 'networkidle' })
    await expect(page.getByRole('button', { name: '保存草稿' }), '批量应有保存草稿键').toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('一事一议:未选表点保存被拦(落库底线)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/wizard', { waitUntil: 'networkidle' })
    await page.getByRole('button', { name: '保存草稿' }).click()
    await expect(page.getByText(/请先从确权目录选取至少一张授权数据表/)).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('批量:未填年度点保存被拦(落库底线)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/batch-wizard', { waitUntil: 'networkidle' })
    await page.getByRole('button', { name: '保存草稿' }).click()
    await expect(page.getByText(/请先填写授权年度/)).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('一事一议:一键示例加表后保存草稿成功(不前进)', async ({ page }) => {
    const errs = collect(page)
    await buildSpecialDraft(page)
    await page.getByRole('button', { name: '保存草稿' }).click()
    await expect(page.getByText(/草稿已保存/), '存草稿应成功').toBeVisible({ timeout: 10000 })
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('我的申请:授权草稿有「编辑/删除」,编辑就地续填(带 formNo/batchListId)', async ({ page }) => {
    // 造一张一事一议草稿
    let errs = collect(page)
    await buildSpecialDraft(page)
    await page.getByRole('button', { name: '保存草稿' }).click()
    await expect(page.getByText(/草稿已保存/)).toBeVisible({ timeout: 10000 })
    clearListeners(page)

    errs = collect(page)
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    const draftRow = page.locator('.el-table__row').filter({ hasText: '草稿' }).filter({ hasText: '授权' }).first()
    if (await draftRow.isVisible().catch(() => false)) {
      await expect(draftRow.getByRole('button', { name: '编辑' })).toBeVisible()
      await expect(draftRow.getByRole('button', { name: '删除' })).toBeVisible()
      await draftRow.getByRole('button', { name: '编辑' }).click()
      await expect(page).toHaveURL(/\/dpr\/auth\/(wizard\?formNo=|batch-wizard\?batchListId=)/, { timeout: 10000 })
      await expect(page.getByText(/已载入草稿/)).toBeVisible({ timeout: 10000 })
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('我的申请:授权草稿「删除」后消失', async ({ page }) => {
    let errs = collect(page)
    await buildSpecialDraft(page)
    await page.getByRole('button', { name: '保存草稿' }).click()
    await expect(page.getByText(/草稿已保存/)).toBeVisible({ timeout: 10000 })
    clearListeners(page)

    errs = collect(page)
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    const draftRow = page.locator('.el-table__row').filter({ hasText: '草稿' }).filter({ hasText: '授权' }).first()
    if (await draftRow.isVisible().catch(() => false)) {
      await draftRow.getByRole('button', { name: '删除' }).click()
      const dlg = page.locator('.el-message-box')
      await expect(dlg).toBeVisible()
      await dlg.getByRole('button', { name: /确定|确认/ }).click()
      await expect(page.getByText('草稿已删除')).toBeVisible({ timeout: 10000 })
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('我的申请:一事一议在审单撤回 → 转已撤回,出现修改重提', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    await page.getByText('仅看我提交的').click()
    await page.waitForTimeout(500)
    // 一事一议在审单(排除批量):撤回落「已撤回」(每表独立)
    const reviewRow = page.locator('.el-table__row').filter({ hasText: '授权' }).filter({ hasNotText: '批量' })
      .filter({ hasText: /单位初审中|合规审核中|业务审核中|主管审核中|经理审核中|副总审批中/ }).first()
    if (!(await reviewRow.isVisible().catch(() => false))) {
      test.skip(true, '当前无一事一议在审单;跳过')
      return
    }
    const applyNo = (await reviewRow.locator('td').nth(1).innerText()).trim()
    await reviewRow.getByRole('button', { name: '撤回' }).click()
    const dlg = page.locator('.el-message-box')
    await expect(dlg).toBeVisible()
    await dlg.getByRole('button', { name: /确定|确认/ }).click()
    await expect(page.getByText(/已撤回/).first()).toBeVisible({ timeout: 10000 })
    await page.waitForTimeout(800)
    const sameRow = page.locator('.el-table__row').filter({ hasText: applyNo }).first()
    await expect(sameRow.getByText('已撤回', { exact: false }).first()).toBeVisible()
    await expect(sameRow.getByRole('button', { name: '撤回' })).toHaveCount(0)
    await expect(sameRow.getByRole('button', { name: '修改重提' })).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('我的申请:批量在审单撤回 → 明细退回草稿(清单回草案)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    await page.getByText('仅看我提交的').click()
    await page.waitForTimeout(500)
    // 批量在审明细:撤回整份清单 → 明细退回草稿(不是已撤回)
    const batchRow = page.locator('.el-table__row').filter({ hasText: '批量' })
      .filter({ hasText: /合规审核中|主管审核中|经理审核中|副总审批中|领导小组审批中/ }).first()
    if (!(await batchRow.isVisible().catch(() => false))) {
      test.skip(true, '当前无批量在审明细;跳过')
      return
    }
    const applyNo = (await batchRow.locator('td').nth(1).innerText()).trim()
    await batchRow.getByRole('button', { name: '撤回' }).click()
    const dlg = page.locator('.el-message-box')
    await expect(dlg).toBeVisible()
    await dlg.getByRole('button', { name: /确定|确认/ }).click()
    await expect(page.getByText(/已撤回/).first()).toBeVisible({ timeout: 10000 })
    await page.waitForTimeout(800)
    // 批量撤回后该明细退回草稿:同编号行状态=草稿,出现编辑/删除
    const sameRow = page.locator('.el-table__row').filter({ hasText: applyNo }).first()
    await expect(sameRow.getByText('草稿', { exact: false }).first()).toBeVisible()
    await expect(sameRow.getByRole('button', { name: '编辑' })).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })
})
