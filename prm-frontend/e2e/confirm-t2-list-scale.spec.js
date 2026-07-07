import { test, expect } from '@playwright/test'

/**
 * 库表清单·逐表确权 规模化(几百/几千张表)三需求回归:
 *  ① 分页(10/页)控件存在;② 列名对齐指引表2(紧凑核心列 + 「表2完整视图」严格12列);
 *  ③ 筛选生效(来源类型/关键字…):用无匹配关键字确定性验证 命中数→0→重置复原。
 * 数据漂移友好:取首个有未确权库表的系统,不写死表数/表名。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('初始确权·库表清单:列名对齐表2 + 分页 + 筛选生效', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })

  // 选一个系统 → 右侧库表清单填充(复用 treepick 流程)
  const domains = page.locator('.cat-tree .el-tree > .el-tree-node')
  await expect(domains.first()).toBeVisible()
  await domains.first().locator('.el-tree-node__expand-icon').first().click()
  const systems = page.locator('.cat-tree .el-tree-node__children > .el-tree-node')
  await expect(systems.first()).toBeVisible()
  await systems.first().locator('.el-checkbox').first().click()
  await expect(page.getByText(/共 \d+ 张库表/)).toBeVisible({ timeout: 10000 })

  // ② 紧凑视图:官方表2列名可见(非旧的「库表(schema.表代码)/来源主体/敏感标记 G–J」)
  await expect(page.getByRole('columnheader', { name: '数据表名称' })).toBeVisible()
  await expect(page.getByRole('columnheader', { name: '来源主体名称' })).toBeVisible()
  await expect(page.getByRole('columnheader', { name: '来源权益限制摘要' })).toBeVisible()

  // ① 分页控件存在(10/页)
  await expect(page.locator('.el-pagination')).toBeVisible()

  // ③ 筛选生效:命中数初值 = 总数;无匹配关键字 → 命中 0;重置 → 复原
  const hit = page.locator('.t2-hit b').first()
  const total0 = (await hit.innerText()).trim()
  expect(parseInt(total0), '命中初值应≥1').toBeGreaterThan(0)
  await page.getByPlaceholder(/搜 schema/).fill('zzz_nomatch_xyz_9999')
  await expect(hit).toHaveText('0')
  await page.getByRole('button', { name: '重置' }).click()
  await expect(hit).toHaveText(total0)

  // ② 「表2完整视图」严格12列:切换后出现指引原列名(紧凑视图没有的列)
  await page.locator('.t2-filter').getByText('表2 完整视图').click()
  await expect(page.getByRole('columnheader', { name: '来源凭证附件或说明' })).toBeVisible()
  await expect(page.getByRole('columnheader', { name: '信息识别关联主体说明' })).toBeVisible()
  await expect(page.getByRole('columnheader', { name: '信息识别关联资料附件' })).toBeVisible()

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
