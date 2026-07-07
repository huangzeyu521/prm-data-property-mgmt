import { test, expect } from '@playwright/test'

/**
 * 初始确权·确权范围树「整系统勾选」联动验证(修复:懒加载下勾收起的系统父节点右侧无反应)。
 *  复现:展开业务域 → 勾「系统」节点复选框(保持收起,子库表未加载)→ 右侧应带出该系统全部未确权库表。
 *  断言:① 出现「已选 N 张库表」(N≥1) ② 不再显示「尚无库表」空态 ③ 库表清单行数 === N。
 *  数据漂移友好:不写死「客户服务系统/4 张」,取首个有未确权库表的系统。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('初始确权:勾收起的系统节点 → 右侧带出整系统库表', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })

  // 树根:业务域(el-tree 直接子节点=第1层)渲染。
  // 注:Element Plus 2.7.6 的 .el-tree-node 只设 role="treeitem",不设 aria-level,故按 DOM 层级结构定位。
  const domains = page.locator('.cat-tree .el-tree > .el-tree-node')
  await expect(domains.first()).toBeVisible()

  // 展开第一个业务域(点展开箭头,非复选框)→ 其下「系统」节点(第2层 = 该域 __children 的直接子)加载
  await domains.first().locator('.el-tree-node__expand-icon').first().click()
  const systems = page.locator('.cat-tree .el-tree-node__children > .el-tree-node')
  await expect(systems.first()).toBeVisible()

  // 初始右侧为空态
  await expect(page.getByText('尚无库表', { exact: false })).toBeVisible()

  // 关键复现:勾「系统」节点复选框,保持收起(不点展开)——子库表此时尚未加载
  await systems.first().locator('.el-checkbox').first().click()

  // 右侧应联动:出现「已选 N 张库表」且 N≥1(成功带出),空态消失
  const selTag = page.getByText(/已选 \d+ 张库表/)
  await expect(selTag).toBeVisible({ timeout: 10000 })
  await expect(page.getByText('尚无库表', { exact: false })).toBeHidden()

  const n = parseInt((await selTag.first().innerText()).match(/已选 (\d+) 张库表/)[1], 10)
  expect(n, '整系统应带出 ≥1 张未确权库表').toBeGreaterThan(0)

  // 库表清单(逐表确权)同步出现「共 N 张库表」,与「已选 N」一致 —— 右侧表格已真正填充,非仅标签
  await expect(page.getByText(`共 ${n} 张库表`, { exact: false })).toBeVisible({ timeout: 8000 })

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
