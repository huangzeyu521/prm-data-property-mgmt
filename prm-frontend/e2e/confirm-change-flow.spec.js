import { test, expect } from '@playwright/test'

/**
 * 确权变更·全触发流程综合回归(资深 debug:从头到尾覆盖 step1 各分支 + 推进闸门)。
 * 覆盖:权益到期(validity 维)/ 数据来源变更(逐表 A→C + next0 推进)/ 无变更(维持原值)。
 * 数据漂移友好:取首个有已确权表的系统,不写死表名。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]
function collect(page) {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('response', r => { if (r.url().includes('/api/') && r.status() >= 400) errs.push(`api ${r.status()}: ${r.request().method()} ${r.url()}`) })
  return errs
}
async function ensureExpanded(node) {
  const already = await node.evaluate(el => el.classList.contains('is-expanded')).catch(() => false)
  if (already) return
  // 仅当存在「非叶子」展开图标时才点(叶子 is-leaf 无展开动作,:not(.is-leaf) 匹配 0 → 跳过,避免超时)
  const icon = node.locator('> .el-tree-node__content .el-tree-node__expand-icon:not(.is-leaf)')
  if (await icon.count() > 0) await icon.first().click().catch(() => {})
}
async function findSystem(page, sysName) {
  await expect(page.locator('.cat-tree .el-tree > .el-tree-node').first()).toBeVisible({ timeout: 15000 })
  const domains = page.locator('.cat-tree .el-tree > .el-tree-node')
  const dn = await domains.count()
  for (let i = 0; i < dn; i++) {
    await ensureExpanded(domains.nth(i))
    const cand = page.getByRole('treeitem', { name: new RegExp(sysName) }).first()
    if (await cand.isVisible().catch(() => false)) { await ensureExpanded(cand); return cand }
  }
  return null
}
async function pickConfirmedTables(page, sysName) {
  const sysNode = await findSystem(page, sysName)
  if (!sysNode) return false
  const module0 = sysNode.locator('.el-tree-node__children > .el-tree-node').first()
  await expect(module0).toBeVisible({ timeout: 10000 })
  await ensureExpanded(module0)
  const confirmed = module0.locator('.el-tree-node__children > .el-tree-node').filter({ hasText: '已确权' })
  await expect(confirmed.first(), '应有已确权叶子表').toBeVisible({ timeout: 10000 })
  const n = await confirmed.count()
  for (let i = 0; i < n; i++) await confirmed.nth(i).locator('.el-checkbox').first().click()
  return true
}
// 跨模块找 status(已确权/未确权)叶子表并勾选 count 张;返回实际勾选数
async function pickLeafByStatus(page, sysName, status, count = 1) {
  const sysNode = await findSystem(page, sysName)
  if (!sysNode) return 0
  const modules = sysNode.locator('.el-tree-node__children > .el-tree-node')
  await expect(modules.first()).toBeVisible({ timeout: 10000 })
  const mc = await modules.count()
  for (let i = 0; i < mc; i++) await ensureExpanded(modules.nth(i)) // 先把所有模块展开(懒加载叶子)
  await page.waitForTimeout(500)
  // 叶子节点 = 带 is-leaf 展开图标者;按其自身文本含 status 过滤(排除已授权)
  const leaves = sysNode.locator('.el-tree-node:has(> .el-tree-node__content .el-tree-node__expand-icon.is-leaf)')
    .filter({ hasText: status }).filter({ hasNotText: '已授权' })
  const lc = await leaves.count()
  let picked = 0
  for (let j = 0; j < lc && picked < count; j++) {
    const cb = leaves.nth(j).locator('> .el-tree-node__content .el-checkbox')
    if (await cb.count() > 0) { await cb.first().click(); picked++ }
  }
  return picked
}

test.describe('确权变更·全流程', () => {

  test('权益到期:填新有效期 → 变更对照出现「权益期限」维', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/change', { waitUntil: 'networkidle' })
    if (!await pickConfirmedTables(page, '营销管理系统')) { test.skip(true, '无营销管理系统已确权表'); return }
    const grp = page.locator('.el-radio-group').filter({ hasText: '数据来源变更' })
    await expect(grp).toBeVisible({ timeout: 15000 })
    await grp.getByText('权益到期', { exact: false }).click()
    await expect(page.getByText('申报权益有效期', { exact: false }).first()).toBeVisible()
    const dateInput = page.locator('.el-date-editor input').first()
    await dateInput.click(); await dateInput.fill('2029-07-18'); await dateInput.press('Enter')
    await expect(page.getByRole('cell', { name: '权益期限', exact: true }).first(), '应出现权益期限维').toBeVisible({ timeout: 8000 })
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('无变更:选表未改 → 暂未检测到差异 + 逐表维持原值', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/change', { waitUntil: 'networkidle' })
    if (!await pickConfirmedTables(page, '客户服务系统')) { test.skip(true, '无客户服务系统已确权表'); return }
    const grp = page.locator('.el-radio-group').filter({ hasText: '数据来源变更' })
    await expect(grp).toBeVisible({ timeout: 15000 })
    await grp.getByText('数据来源变更', { exact: false }).click()
    await expect(page.getByText(/共 \d+ 张库表/)).toBeVisible({ timeout: 10000 })
    // 未改:变更摘要=暂未检测到差异;逐表明细=维持原值
    await expect(page.getByText('暂未检测到与原确权结论的差异', { exact: false })).toBeVisible()
    await expect(page.getByRole('cell', { name: '维持原值', exact: true }).first()).toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('数据来源变更:编辑表2 A→C → next0 通过校验推进到「上传材料」', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/change', { waitUntil: 'networkidle' })
    if (!await pickConfirmedTables(page, '客户服务系统')) { test.skip(true, '无客户服务系统已确权表'); return }
    const grp = page.locator('.el-radio-group').filter({ hasText: '数据来源变更' })
    await expect(grp).toBeVisible({ timeout: 15000 })
    await grp.getByText('数据来源变更', { exact: false }).click()
    await expect(page.getByText(/共 \d+ 张库表/)).toBeVisible({ timeout: 10000 })

    // 编辑表2:来源类型改 C 公共数据授权 + 来源主体名称必填补齐
    await page.getByRole('button', { name: '编辑表2' }).first().click()
    const drawer = page.locator('.el-drawer')
    await expect(drawer).toBeVisible()
    const srcSel = drawer.locator('.el-form-item').filter({ hasText: '来源类型' }).first().locator('.el-select').first()
    await srcSel.click()
    await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: 'C 公共数据授权' }).first().click()
    const subj = drawer.locator('.el-form-item').filter({ hasText: '来源主体名称' }).first().locator('input, textarea').first()
    await subj.fill('公共数据授权来源方(变更测试)')
    await drawer.getByRole('button', { name: '完成' }).click()
    await expect(drawer).toBeHidden()

    // 逐表明细应标已修订
    await expect(page.getByRole('cell', { name: '已修订', exact: true }).first()).toBeVisible()

    // 下一步:通过 step1 校验(必填/表2完整/授权影响)→ 进入「上传材料」步(出现「上一步」)
    await page.getByRole('button', { name: '下一步' }).click()
    await expect(page.getByRole('button', { name: '上一步' }), 'next0 应通过校验推进到 step1').toBeVisible({ timeout: 12000 })
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('数据新增:仅选未确权表 → 锁定「数据新增」+ 集合级前后对照', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/change', { waitUntil: 'networkidle' })
    const n = await pickLeafByStatus(page, '营销管理系统', '未确权', 1)
    if (!n) { test.skip(true, '无未确权表可选;跳过'); return }
    await expect(page.getByText(/共 \d+ 张库表/)).toBeVisible({ timeout: 10000 })
    // isDataAdd 分支:自动锁定"数据新增",不出现来源/管理/到期三选一;alert 标题含"新表首次确权登记"
    await expect(page.getByText('数据新增', { exact: false }).first()).toBeVisible({ timeout: 8000 })
    await expect(page.getByText('新表', { exact: false }).first(), 'data-add 应提示新表首次确权登记').toBeVisible()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('混选守卫:未确权 + 已确权同单 → 提示编辑模式冲突', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/change', { waitUntil: 'networkidle' })
    const u = await pickLeafByStatus(page, '营销管理系统', '未确权', 1)
    const c = await pickLeafByStatus(page, '营销管理系统', '已确权', 1)
    if (!c || !u) { test.skip(true, '需同系统同时有已确权与未确权表;跳过'); return }
    // 混选 → 冲突提示(内联 alert 或点下一步拦截)
    const mixedTip = page.getByText('混含', { exact: false }).first()
    if (await mixedTip.isVisible().catch(() => false)) {
      await expect(mixedTip).toBeVisible()
    } else {
      await page.getByRole('button', { name: '下一步' }).click()
      await expect(page.getByText(/混含|编辑模式冲突|分两单/).first()).toBeVisible({ timeout: 8000 })
    }
    expect(errs, errs.join('\n')).toEqual([])
  })
})
