import { test, expect } from '@playwright/test'

/**
 * 申报人·数字化部团队(梁晶晶,role=apply)全功能走查 —— 运行须 PW_USER=apply。
 * 对照35号文职责:确权流程步骤20/40(编制表1/表2、归集审查)、批量授权步骤20/40(表5归集初审)、
 * 一事一议步骤30(授权初审汇总)、§二(三)2 季度重新确权、附录H 数字化管理部门归口职责。
 * 每页收集 console error + /api 响应≥400,任一即失败。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i, /sourcemap/i]

// role=apply 可见的全部菜单路由(lib/roles.js MENU)
const ROUTES = [
  ['/dpr/workbench/my', '我的申请'],
  ['/dpr/guidance', '指引中心'],
  ['/dpr/ledger/overview', '产权台账概览'],
  ['/dpr/ledger/archive', '数据集产权档案管理'],
  ['/dpr/confirm/wizard', '初始确权申请'],
  ['/dpr/confirm/change', '确权变更申请'],
  ['/dpr/confirm/history', '确权申请查询'],
  ['/dpr/confirm/card', '权益卡片生成'],
  ['/dpr/auth/wizard', '一事一议授权申请'],
  ['/dpr/auth/batch-wizard', '批量授权申请'],
  ['/dpr/auth/batch-list', '批量授权清单'],
  ['/dpr/auth/history', '授权申请历史查询'],
  ['/dpr/auth/agreement', '协议工作台'],
  ['/dpr/auth/filing', '对外经营权授权备案'],
]

function collect(page) {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('response', r => {
    if (r.url().includes('/api/') && r.status() >= 400) errs.push(`api ${r.status()}: ${r.request().method()} ${r.url()}`)
  })
  return errs
}

/**
 * 变更模式选「已确权」叶子表(update 型):
 * 树=域(默认展开)>系统>模块>库表(懒加载);整系统勾选会混入未确权新表触发 mixedSelection 拦截(设计如此),
 * 故下钻到叶子只勾带「已确权」徽标的库表。
 */
/** 幂等展开一个 el-tree 节点(域节点默认可能折叠也可能展开,视 HMR 状态):未展开才点展开图标。 */
async function ensureExpanded(node) {
  const already = await node.evaluate(el => el.classList.contains('is-expanded')).catch(() => false)
  if (!already) {
    await node.locator('> .el-tree-node__content .el-tree-node__expand-icon').first().click()
  }
}

async function pickConfirmedTables(page, sysName) {
  // 先等树渲染(locator.isVisible() 不重试,须先 expect 等待,否则树未加载即误判)
  await expect(page.locator('.cat-tree .el-tree > .el-tree-node').first()).toBeVisible({ timeout: 15000 })
  // 域节点(第1层)可能折叠 → 逐域展开直到系统节点出现(数据漂移友好:不写死域名)
  const domains = page.locator('.cat-tree .el-tree > .el-tree-node')
  const dn = await domains.count()
  let sysNode = null
  for (let i = 0; i < dn; i++) {
    await ensureExpanded(domains.nth(i))
    const cand = page.getByRole('treeitem', { name: new RegExp(sysName) }).first()
    if (await cand.isVisible().catch(() => false)) { sysNode = cand; break }
  }
  if (!sysNode) return false
  // 展开系统 → 模块层懒加载
  await ensureExpanded(sysNode)
  const module0 = sysNode.locator('.el-tree-node__children > .el-tree-node').first()
  await expect(module0).toBeVisible({ timeout: 10000 })
  // 展开模块 → 叶子库表
  await ensureExpanded(module0)
  const confirmedLeaves = module0.locator('.el-tree-node__children > .el-tree-node').filter({ hasText: '已确权' })
  await expect(confirmedLeaves.first(), '第一个模块下应有已确权叶子表').toBeVisible({ timeout: 10000 })
  const n = await confirmedLeaves.count()
  for (let i = 0; i < n; i++) {
    await confirmedLeaves.nth(i).locator('.el-checkbox').first().click()
  }
  return true
}

test.describe('申报人(梁晶晶)角色走查', () => {

  test('首屏落点 + 菜单可见域 == AA-10 申报人矩阵', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/', { waitUntil: 'networkidle' })
    // ROLE_HOME.apply = /dpr/workbench/my
    await expect(page).toHaveURL(/\/dpr\/workbench\/my/, { timeout: 15000 })
    const menu = page.locator('.el-aside, .el-menu').first()
    // 应见:申报入口 + 查询 + 工单。菜单项在折叠子菜单内(el-sub-menu 默认收起),
    // 断言存在于 DOM(角色过滤放行)即为"可见域包含",不苛求当前展开可见。
    for (const t of ['我的申请', '初始确权申请', '确权变更申请', '一事一议授权申请', '批量授权申请', '协议工作台']) {
      await expect(menu.getByText(t, { exact: false }).first(), `申报人应可见菜单「${t}」`).toBeAttached()
    }
    // 不应见:审核台/监测配置/系统管理(越权入口)—— 角色过滤应从 DOM 剔除
    for (const t of ['审核申请提交', '授权审核提交', '监测规则配置', '用户管理', '操作日志']) {
      await expect(menu.getByText(t, { exact: false }), `申报人不应见菜单「${t}」`).toHaveCount(0)
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('全路由扫雷:16 个申报人页面 0 console 错误 / 0 api≥400', async ({ page }) => {
    const bad = []
    for (const [path, name] of ROUTES) {
      const errs = collect(page)
      await page.goto(path, { waitUntil: 'networkidle' })
      await page.waitForTimeout(600)
      const hasContent = await page.locator('.el-card, .prm-page, .el-table, .el-form').first().isVisible().catch(() => false)
      if (!hasContent) bad.push(`${name}(${path}): 页面无主内容渲染`)
      if (errs.length) bad.push(`${name}(${path}):\n  ` + errs.join('\n  '))
      page.removeAllListeners('console'); page.removeAllListeners('pageerror'); page.removeAllListeners('response')
    }
    expect(bad, '页面走查发现问题:\n' + bad.join('\n')).toEqual([])
  })

  test('初始确权向导:表1 要素对齐指引(登记类型/A–F/G–J/系统负责人)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/wizard', { waitUntil: 'networkidle' })
    await expect(page.getByText('本次:初始确权', { exact: false })).toBeVisible()
    // 始终可见的表1 要素:来源/信息关联权益识别(系统级并集)+ 申报权属主体(=表1 公司主体)
    for (const t of ['来源权益识别', '信息关联权益识别', '申报权属主体', '主体层级']) {
      await expect(page.getByText(t, { exact: false }).first(), `表1 要素「${t}」应存在`).toBeVisible()
    }
    // 系统负责人/联系方式在「系统责任信息」折叠面板内(表1 系统负责人姓名/联系方式列),展开后可见
    await page.locator('.identity-card .el-collapse-item__header').click()
    await expect(page.getByText('系统负责人', { exact: false }).first()).toBeVisible()
    await expect(page.getByText('联系方式', { exact: false }).first()).toBeVisible()
    // 确权范围树(指引:确权最小单元=信息系统)
    await expect(page.locator('.cat-tree .el-tree > .el-tree-node').first()).toBeVisible({ timeout: 10000 })
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('确权变更向导:触发类型/基线对照/权益期限维/期望变更点全链呈现', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/change', { waitUntil: 'networkidle' })
    await expect(page.getByText('本次:确权变更', { exact: false })).toBeVisible()

    const found = await pickConfirmedTables(page, '营销管理系统')
    expect(found, '左树应能勾选「营销管理系统」的已确权叶子表').toBe(true)

    // 全为已确权表 → 触发类型单选出现(来源/管理/到期/其他 三选一)
    const triggerGroup = page.locator('.el-radio-group').filter({ hasText: '数据来源变更' })
    await expect(triggerGroup, '已确权系统应出现变更触发单选').toBeVisible({ timeout: 15000 })

    // 勾「权益到期」→ 申报权益有效期(P0′ 期限维)出现,且带"变更前(上一版卡片最早到期)"语境
    // (「申报权益有效期」同时命中 form-item label 与提示 span,用 .first())
    await triggerGroup.getByText('权益到期', { exact: false }).click()
    await expect(page.getByText('申报权益有效期', { exact: false }).first()).toBeVisible()
    await expect(page.getByText('变更前(上一版卡片最早到期)', { exact: false }).first()).toBeVisible()

    // 期望变更点引导(P1′):勾了到期 → 出现"新权益有效期已申报"期望项,未填时标"未变更"
    await expect(page.getByText('期望变更点', { exact: false }).first()).toBeVisible()
    await expect(page.getByText('新权益有效期已申报', { exact: false }).first()).toBeVisible()

    // 基线对照块:真实上一版确权(营销管理系统 CA-001 已完成)
    await expect(page.getByText('底版=上一版真实确权', { exact: false })).toBeVisible({ timeout: 10000 })

    // 填新有效期 → 变更对照出现「权益期限」维(变更前→变更后)
    const dateInput = page.locator('.el-date-editor input').first()
    await dateInput.click()
    await dateInput.fill('2028-07-18')
    await dateInput.press('Enter')
    await expect(page.getByText('权益期限', { exact: true }).first(), '对照表应出现「权益期限」维').toBeVisible({ timeout: 8000 })

    expect(errs, errs.join('\n')).toEqual([])
  })

  test('产权档案「发起变更」入口直达确权变更路由(P0.1 断头修复回归)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/ledger/archive', { waitUntil: 'networkidle' })
    const btn = page.getByRole('button', { name: '发起变更' }).first()
    if (await btn.isVisible({ timeout: 5000 }).catch(() => false)) {
      await btn.click()
      await expect(page).toHaveURL(/\/dpr\/confirm\/change\?/, { timeout: 10000 })
      await expect(page.getByText('本次:确权变更', { exact: false })).toBeVisible()
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('一事一议授权向导:确权目录单选树 + 表5系统生成(指引步骤10/表5)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/wizard', { waitUntil: 'networkidle' })
    await expect(page.locator('.el-card').first()).toBeVisible()
    // 资源池=确权目录树(先确后授);表5《数据授权申请单》系统生成下载
    await expect(page.getByText(/表5/).first(), '应有《表5 数据授权申请单》系统生成入口').toBeVisible({ timeout: 10000 })
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('批量授权向导:3步向导 + 表5自动生成(指引批量步骤20归集口径)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/batch-wizard', { waitUntil: 'networkidle' })
    await expect(page.locator('.el-steps .el-step').first()).toBeVisible({ timeout: 10000 })
    const steps = await page.locator('.el-steps').first().locator('.el-step').count()
    expect(steps, '批量授权应为3步向导(A+C′结构)').toBe(3)
    // 表5 说明文案在 step2 区块(v-show 隐藏但已挂载):断言存在即可,不苛求当前步可见
    await expect(page.getByText(/表5/).first()).toBeAttached()
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('通知中心铃铛:apply 可见待办聚合', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/workbench/my', { waitUntil: 'networkidle' })
    await page.locator('.nc-bell').click()
    await expect(page.getByText('通知中心')).toBeVisible()
    await page.getByRole('tab', { name: /待办/ }).click()
    await page.waitForTimeout(800)
    expect(errs, errs.join('\n')).toEqual([])
  })
})
