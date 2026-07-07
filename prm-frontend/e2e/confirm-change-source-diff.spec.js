import { test, expect } from '@playwright/test'

/**
 * 确权变更·来源逐表改动 → 系统级不做并集对照(修 A→B 误显示为「A、B」)。
 * 场景:客户服务系统(目录合成底版,无上一版真实确权)选 客服工单表,触发数据来源变更,
 *   在「编辑表2」把来源类型改为另一值 → 顶部变更对照不得出现 union「X、Y」/系统级「来源识别(A–F)」diff 行,
 *   改动仍由逐表留痕计入(summary 含「库表」)。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

async function ensureExpanded(node) {
  const already = await node.evaluate(el => el.classList.contains('is-expanded')).catch(() => false)
  if (!already) await node.locator('> .el-tree-node__content .el-tree-node__expand-icon').first().click()
}
async function pickConfirmedTables(page, sysName) {
  await expect(page.locator('.cat-tree .el-tree > .el-tree-node').first()).toBeVisible({ timeout: 15000 })
  const domains = page.locator('.cat-tree .el-tree > .el-tree-node')
  const dn = await domains.count()
  let sysNode = null
  for (let i = 0; i < dn; i++) {
    await ensureExpanded(domains.nth(i))
    const cand = page.getByRole('treeitem', { name: new RegExp(sysName) }).first()
    if (await cand.isVisible().catch(() => false)) { sysNode = cand; break }
  }
  if (!sysNode) return false
  await ensureExpanded(sysNode)
  const module0 = sysNode.locator('.el-tree-node__children > .el-tree-node').first()
  await expect(module0).toBeVisible({ timeout: 10000 })
  await ensureExpanded(module0)
  const confirmed = module0.locator('.el-tree-node__children > .el-tree-node').filter({ hasText: '已确权' })
  await expect(confirmed.first(), '应有已确权叶子表').toBeVisible({ timeout: 10000 })
  const n = await confirmed.count()
  for (let i = 0; i < n; i++) await confirmed.nth(i).locator('.el-checkbox').first().click()
  return true
}

test('确权变更:来源逐表 A→B 不再被系统级并集显示为「A、B」', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/confirm/change', { waitUntil: 'networkidle' })

  const ok = await pickConfirmedTables(page, '客户服务系统')
  if (!ok) { test.skip(true, '演示数据无「客户服务系统」已确权表;跳过'); return }

  // 触发:数据来源变更
  const triggerGroup = page.locator('.el-radio-group').filter({ hasText: '数据来源变更' })
  await expect(triggerGroup).toBeVisible({ timeout: 15000 })
  await triggerGroup.getByText('数据来源变更', { exact: false }).click()

  // 库表清单出现该表 → 打开「编辑表2」抽屉
  await expect(page.getByText(/共 \d+ 张库表/)).toBeVisible({ timeout: 10000 })
  await page.getByRole('button', { name: '编辑表2' }).first().click()
  const drawer = page.locator('.el-drawer')
  await expect(drawer).toBeVisible()

  // 来源类型:改为与当前不同的值(A→B,若本就 B 则→C)
  const srcItem = drawer.locator('.el-form-item').filter({ hasText: '来源类型' }).first()
  const srcSelect = srcItem.locator('.el-select').first()
  const cur = (await srcSelect.innerText()).trim()
  const target = cur.startsWith('B') ? 'C 公共数据授权' : 'B 公开采集'
  await srcSelect.click()
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: target }).first().click()
  // 抽屉内确认改动已登记(平台原值留痕)
  await expect(srcItem.getByText('已修改', { exact: false })).toBeVisible({ timeout: 5000 })
  // 关闭抽屉
  await drawer.getByRole('button', { name: '完成' }).click()
  await expect(drawer).toBeHidden()

  // 顶部变更对照:不得出现 union「、」到「来源识别(A–F)」,不得出现系统级来源识别 diff 行
  const changeAlert = page.locator('.el-alert').filter({ hasText: '本次确权变更' }).first()
  await expect(changeAlert).toBeVisible({ timeout: 8000 })
  await expect(changeAlert, '来源逐表改动不应被并集显示为 X、Y').not.toContainText('A、B')
  await expect(changeAlert, '不应把系统级「来源识别(A–F)」当变更维度对照').not.toContainText('来源识别(A–F)')
  // 改动仍被逐表机制计入(不误报"无差异")
  await expect(changeAlert).toContainText('库表')
  // 变更对照表(若渲染)不得含「来源识别(A–F)」维度行
  await expect(page.getByRole('cell', { name: '来源识别(A–F)' })).toHaveCount(0)

  // 逐表改动明细表:「变更项」列(明细表独有)+「数据表名称」列 + 该表 + 来源类型改动可见
  await expect(page.getByRole('columnheader', { name: '变更项' })).toBeVisible()
  await expect(page.getByRole('columnheader', { name: '数据表名称' }).first()).toBeVisible()
  await expect(page.getByRole('cell', { name: '来源类型', exact: true }).first(), '明细应含来源类型变更项').toBeVisible()
  await expect(page.getByRole('cell', { name: '客服工单表', exact: true }).first(), '明细应标明是哪张数据表').toBeVisible()

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
