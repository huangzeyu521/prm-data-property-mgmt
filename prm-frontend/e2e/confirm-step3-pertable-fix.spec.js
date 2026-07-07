import { test, expect } from '@playwright/test'

/**
 * A′+B′+C′ 验证:step3 逐表凭证同步。
 *  制造真实缺口:step2 在「编辑表2」抽屉勾 I 商密(平台无 I 附件)→ 新增一个无附件槽位。
 *  断言:① step3 A′ chip「逐表凭证 4/5 · 缺 1」 ② 需处理清单出现「逐表」缺失行(未跑校验也显示,本地真源)
 *       ③ 就地「仅登记」修复 → chip 变 5/5 · 齐、清单消失(B′ 写回 ConfirmTableItem,死循环根除)。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('step3:逐表缺口显示 + 就地修复闭环', async ({ page }) => {
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
  await page.getByRole('button', { name: '下一步' }).click()           // → step2
  await expect(page.getByText('逐表凭证材料', { exact: false })).toBeVisible({ timeout: 12000 })

  // 制造缺口:step2 逐表区首行「编辑表2」→ 勾 I 商密(平台无 I 附件 → 新槽位缺附件)
  const perTable = page.locator('.el-card').filter({ hasText: '逐表凭证材料' }).locator('.el-table').filter({ hasText: '凭证槽位' })  /* 限定 step2 卡:step3 逐表校验表同有该表头 */
  await perTable.locator('.el-table__row').first().getByRole('button', { name: '编辑表2' }).click()
  const drawer = page.locator('.el-drawer').filter({ hasText: '表2 第三方权益' })
  await expect(drawer).toBeVisible()
  await drawer.getByText('I 第三方商业机密').click()                    // 勾选 I
  await drawer.getByRole('button', { name: '完成' }).click()
  await expect(drawer).toBeHidden()

  // → step3:A′ chip 显 4/5 · 缺 1;需处理清单显「逐表」缺失行(未跑校验,本地真源即知)
  await page.getByRole('button', { name: '下一步' }).click()
  await expect(page.getByText('校验状态', { exact: false })).toBeVisible({ timeout: 12000 })
  await expect(page.getByText(/逐表凭证 4\/5 · 缺 1/)).toBeVisible()
  const pendCard = page.locator('.el-card .el-card').filter({ hasText: '需处理以下' })  // 内层待处理卡(外层 step3 卡也含该文案)
  await expect(pendCard).toBeVisible()
  const missRow = pendCard.locator('.el-table__row').filter({ hasText: '商业机密' })
  await expect(missRow).toBeVisible()
  await expect(missRow.getByText('逐表', { exact: true })).toBeVisible()  // 来源 tag(建议文案含"逐表凭证…",须 exact)

  // B′ 就地修复:仅登记 → 写回 ConfirmTableItem → chip 变齐、清单消失
  await missRow.getByRole('button', { name: '仅登记' }).click()
  await expect(page.locator('.el-message--success').filter({ hasText: '已登记' })).toBeVisible({ timeout: 8000 })
  await expect(page.getByText(/逐表凭证 5\/5 · 齐/)).toBeVisible()
  await expect(pendCard).toBeHidden()

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
