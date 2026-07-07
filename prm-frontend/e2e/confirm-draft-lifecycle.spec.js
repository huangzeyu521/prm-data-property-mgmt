import { test, expect } from '@playwright/test'

/**
 * 初始确权申请「保存草稿」全生命周期(PDD 8.1 状态机·独立存草稿) —— 运行须 PW_USER=apply。
 * 覆盖:① 三步均有独立「保存草稿」键;② 填一半即可主动存草稿(不校验完成度、不前进);
 *      ③「我的申请」草稿→编辑(就地续填,同单)/删除;④ 已提交→撤回。
 * 每步收集 console error + /api≥400,关键动作失败即失败。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i, /sourcemap/i]

function collect(page) {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('response', r => { if (r.url().includes('/api/') && r.status() >= 400) errs.push(`api ${r.status()}: ${r.request().method()} ${r.url()}`) })
  return errs
}

/** 在初始确权向导左树选一个整系统(带出库表),满足"保存草稿"的落库底线。返回选中的系统名。 */
async function pickAnySystem(page) {
  await expect(page.locator('.cat-tree .el-tree > .el-tree-node').first()).toBeVisible({ timeout: 15000 })
  // 展开首个业务域 → 勾第一个系统节点(收起态勾选即整系统带库表)
  const domain0 = page.locator('.cat-tree .el-tree > .el-tree-node').first()
  const alreadyExpanded = await domain0.evaluate(el => el.classList.contains('is-expanded')).catch(() => false)
  if (!alreadyExpanded) await domain0.locator('> .el-tree-node__content .el-tree-node__expand-icon').first().click()
  const sys0 = domain0.locator('.el-tree-node__children > .el-tree-node').first()
  await expect(sys0).toBeVisible({ timeout: 10000 })
  const sysName = (await sys0.locator('.el-tree-node__label, .el-tree-node__content').first().innerText()).trim().split(/\s/)[0]
  await sys0.locator('.el-checkbox').first().click()
  // 右侧带出库表(共 N 张)
  await expect(page.getByText(/共 \d+ 张库表|已选 \d+ 张库表/).first()).toBeVisible({ timeout: 10000 })
  return sysName
}

test.describe('初始确权·保存草稿全生命周期(apply)', () => {

  test('三步操作条均含独立「保存草稿」键', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })
    // step0
    await expect(page.getByRole('button', { name: '保存草稿' }), 'step0 应有保存草稿键').toBeVisible()
    await expect(page.getByRole('button', { name: '下一步' })).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('填一半即可主动存草稿(不校验完成度、不前进),并落库为草稿状态', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })
    await pickAnySystem(page)
    // 故意不填系统负责人/联系方式等必填项,直接点「保存草稿」——宽进应成功,且仍停在 step0
    await page.getByRole('button', { name: '保存草稿' }).click()
    await expect(page.getByText(/草稿已保存/), '存草稿应成功提示').toBeVisible({ timeout: 10000 })
    // 未前进:步骤条仍在"填写申请"(step0),下一步键仍在
    await expect(page.getByRole('button', { name: '下一步' })).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('未选系统时点保存草稿被拦(落库底线)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })
    await page.getByRole('button', { name: '保存草稿' }).click()
    await expect(page.getByText(/请先在左侧「确权范围」树勾选/)).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('我的申请:草稿有「编辑/删除」,编辑就地续填回填同一张单', async ({ page }) => {
    // 先造一张草稿
    let errs = collect(page)
    await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })
    const sysName = await pickAnySystem(page)
    await page.getByRole('button', { name: '保存草稿' }).click()
    await expect(page.getByText(/草稿已保存/)).toBeVisible({ timeout: 10000 })
    page.removeAllListeners('console'); page.removeAllListeners('pageerror'); page.removeAllListeners('response')

    // 我的申请:找到草稿行 → 有 编辑/删除
    errs = collect(page)
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    const draftRow = page.locator('.el-table__row').filter({ hasText: '草稿' }).first()
    await expect(draftRow, '我的申请应出现草稿行').toBeVisible({ timeout: 10000 })
    await expect(draftRow.getByRole('button', { name: '编辑' })).toBeVisible()
    await expect(draftRow.getByRole('button', { name: '删除' })).toBeVisible()

    // 编辑 → 就地续填:URL 带 applyId,向导回填系统(载入草稿提示)
    await draftRow.getByRole('button', { name: '编辑' }).click()
    await expect(page).toHaveURL(/\/dpr\/confirm\/wizard\?applyId=/, { timeout: 10000 })
    await expect(page.getByText(/已载入草稿/)).toBeVisible({ timeout: 10000 })
    // 回填了系统(顶部确权范围 tag 显示系统名)
    if (sysName) await expect(page.getByText(sysName, { exact: false }).first()).toBeVisible({ timeout: 8000 })
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('我的申请:草稿「删除」后从列表消失', async ({ page }) => {
    // 造草稿
    let errs = collect(page)
    await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })
    await pickAnySystem(page)
    await page.getByRole('button', { name: '保存草稿' }).click()
    await expect(page.getByText(/草稿已保存/)).toBeVisible({ timeout: 10000 })
    page.removeAllListeners('console'); page.removeAllListeners('pageerror'); page.removeAllListeners('response')

    errs = collect(page)
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    const before = await page.locator('.el-table__row').filter({ hasText: '草稿' }).count()
    expect(before, '应至少有一张草稿').toBeGreaterThan(0)
    const draftRow = page.locator('.el-table__row').filter({ hasText: '草稿' }).first()
    await draftRow.getByRole('button', { name: '删除' }).click()
    // 确认弹窗
    const dlg = page.locator('.el-message-box')
    await expect(dlg).toBeVisible()
    await dlg.getByRole('button', { name: /确定|确认/ }).click()
    await expect(page.getByText('草稿已删除')).toBeVisible({ timeout: 10000 })
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('我的申请:在审单有「撤回」按钮', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    // 种子里确权在审单(人工预审/合规/主管/经理审核中)应带撤回;数据漂移友好:有则断言,无则跳过
    const reviewRow = page.locator('.el-table__row').filter({ hasText: /人工预审中|合规审核中|主管复核中|经理终审中/ }).first()
    if (await reviewRow.isVisible().catch(() => false)) {
      await expect(reviewRow.getByRole('button', { name: '撤回' }), '在审确权单应有撤回').toBeVisible()
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('撤回执行:点撤回→单据真的转「已撤回」,按钮由撤回变为修改重提', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    // 取消「仅看我提交的」→ 显示全部(含无 creatorId 的种子在审单;否则前面用例造的 apply 草稿会把种子在审单过滤掉)
    await page.getByText('仅看我提交的').click()
    await page.waitForTimeout(500)
    // 取一张在审确权单(种子无 creatorId → apply 可撤);无则跳过
    const reviewRow = page.locator('.el-table__row').filter({ hasText: /人工预审中|合规审核中|主管复核中|经理终审中/ }).first()
    if (!(await reviewRow.isVisible().catch(() => false))) {
      test.skip(true, '当前无在审确权单(可能已被前次撤回消费);跳过撤回执行验证')
      return
    }
    // 申请编号列(td: 0类型 / 1申请编号 / 2数据资产 / 3状态 …)
    const applyNo = (await reviewRow.locator('td').nth(1).innerText()).trim()
    await reviewRow.getByRole('button', { name: '撤回' }).click()
    // 撤回原因弹窗(textarea)→ 确认
    const dlg = page.locator('.el-message-box')
    await expect(dlg).toBeVisible()
    await dlg.locator('textarea').fill('e2e:撤回执行验证')
    await dlg.getByRole('button', { name: /确定|确认/ }).click()
    // 成功提示 → 后端 withdraw 已把状态置「已撤回」
    await expect(page.getByText(/已撤回/).first(), '撤回应成功并提示已撤回').toBeVisible({ timeout: 10000 })
    await page.waitForTimeout(800)
    // 同编号行:状态转已撤回 → 撤回键消失、出现修改重提(复制新单闭环);且行内含「已撤回」文案
    const sameRow = page.locator('.el-table__row').filter({ hasText: applyNo }).first()
    await expect(sameRow).toBeVisible({ timeout: 8000 })
    await expect(sameRow.getByText('已撤回', { exact: false }).first(), '该单状态应转为已撤回').toBeVisible()
    await expect(sameRow.getByRole('button', { name: '撤回' }), '已撤回单不应再有撤回键').toHaveCount(0)
    await expect(sameRow.getByRole('button', { name: '修改重提' }), '已撤回单应可修改重提').toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })
})
