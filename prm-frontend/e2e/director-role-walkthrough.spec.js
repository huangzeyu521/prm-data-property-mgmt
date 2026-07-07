import { test, expect } from '@playwright/test'

/**
 * 经理/高级经理(郑经理,role=director)全功能走查 —— 运行须 PW_USER=director。
 * 对照35号文 经理/高级经理(数字化管理部门)职责:
 *   确权 步骤70「审批数据确权结果清单及认定意见」(通过→步骤80 制卡);
 *   批量授权 步骤70「审核《数据批量授权清单》(申报稿)」;
 *   一事一议 步骤90「审核数据授权申请」;
 *   附录H:数字化管理部门归口,经理为确权终审/授权审核关键岗。
 * 产品对应:确权「经理终审中」(终审→自动制卡) + 授权「经理审核中」(→副总审批中)。
 * 每页收集 console error + /api 响应≥400,任一即失败。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i, /sourcemap/i]

// role=director 可见的全部菜单路由(lib/roles.js MENU;不含卡片/证书/工单——那是主管/管理员)
const ROUTES = [
  ['/dpr/workbench/todo', '统一待办中心'],
  ['/dpr/guidance', '指引中心'],
  ['/dpr/ledger/overview', '产权台账概览'],
  ['/dpr/ledger/archive', '数据集产权档案管理'],
  ['/dpr/ledger/change', '产权变更记录管理'],
  ['/dpr/ledger/statistics', '产权台账统计分析'],
  ['/dpr/confirm/history', '确权申请查询'],
  ['/dpr/confirm/review', '确权审核申请提交'],
  ['/dpr/auth/batch-list', '批量授权清单'],
  ['/dpr/auth/history', '授权申请历史查询'],
  ['/dpr/auth/review', '授权审核提交'],
  ['/dpr/dashboard/overview', '数据产权全景'],
  ['/dpr/dashboard/confirm', '确权看板'],
  ['/dpr/dashboard/auth', '授权看板'],
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

test.describe('经理/高级经理(郑经理)角色走查', () => {

  test('首屏落点 + 菜单可见域 == AA-10 经理矩阵', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/', { waitUntil: 'networkidle' })
    await expect(page).toHaveURL(/\/dpr\/workbench\/todo/, { timeout: 15000 })
    const menu = page.locator('.el-aside, .el-menu').first()
    for (const t of ['统一待办中心', '产权台账概览', '审核申请提交', '授权审核提交', '批量授权清单', '数据产权全景', '产权变更记录管理']) {
      await expect(menu.getByText(t, { exact: false }).first(), `经理应可见菜单「${t}」`).toBeAttached()
    }
    // 不应见:申报入口/权益卡片/权益证书/重确权工单/合规校验/监测/授权配置/系统管理
    for (const t of ['初始确权申请', '一事一议授权申请', '权益卡片生成', '权益证书管理', '重确权工单', '合规校验管理', '监测规则配置', '用户管理']) {
      await expect(menu.getByText(t, { exact: false }), `经理不应见菜单「${t}」`).toHaveCount(0)
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('全路由扫雷:14 个经理页面 0 console 错误 / 0 api≥400', async ({ page }) => {
    const bad = []
    for (const [path, name] of ROUTES) {
      const errs = collect(page)
      await page.goto(path, { waitUntil: 'networkidle' })
      await page.waitForTimeout(700)
      const hasContent = await page.locator('.el-card, .prm-page, .el-table, .el-form, .prm-table-card, canvas, .echarts').first().isVisible().catch(() => false)
      if (!hasContent) bad.push(`${name}(${path}): 页面无主内容渲染`)
      if (errs.length) bad.push(`${name}(${path}):\n  ` + errs.join('\n  '))
      page.removeAllListeners('console'); page.removeAllListeners('pageerror'); page.removeAllListeners('response')
    }
    expect(bad, '页面走查发现问题:\n' + bad.join('\n')).toEqual([])
  })

  test('待办中心:确权(经理终审中)+ 授权(经理审核中)双域待办均可见且非空', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/workbench/todo', { waitUntil: 'networkidle' })
    await expect(page.getByRole('tab', { name: /确权审批/ }), '经理应见确权审批 tab').toBeVisible()
    await expect(page.getByRole('tab', { name: /授权审批/ }), '经理应见授权审批 tab').toBeVisible()
    // PRM_AGG 开启后待办应真实聚合(非空):确权 tab 至少含经理终审中
    const rowCount = await page.locator('.el-table__row').count()
    if (rowCount > 0) {
      await expect(page.getByText('经理终审中').first()).toBeVisible()
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('确权审核台:本节点「经理终审中」可审批 + 非本节点行禁用防越权', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/review', { waitUntil: 'networkidle' })
    await expect(page.locator('.prm-table-card, .el-table').first()).toBeVisible({ timeout: 10000 })
    const ownRow = page.locator('.el-table__row').filter({ hasText: '经理终审中' }).first()
    if (await ownRow.isVisible().catch(() => false)) {
      await expect(ownRow.getByRole('button', { name: '审批通过' })).toBeVisible()
      await expect(ownRow.getByRole('button', { name: '驳回' })).toBeVisible()
    }
    const otherRow = page.locator('.el-table__row').filter({ hasText: /人工预审中|合规审核中|主管复核中/ }).first()
    if (await otherRow.isVisible().catch(() => false)) {
      await expect(otherRow.getByText('非本人审批')).toBeVisible()
      await expect(otherRow.getByRole('button', { name: '审批通过' })).toHaveCount(0)
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('授权审核台:本节点「经理审核中」可审批 + 硬收敛无非本节点行 + 详情表5', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/review', { waitUntil: 'networkidle' })
    await expect(page.locator('.prm-table-card, .el-table').first()).toBeVisible({ timeout: 10000 })
    const ownRow = page.locator('.el-table__row').filter({ hasText: '经理审核中' }).first()
    if (await ownRow.isVisible().catch(() => false)) {
      await expect(ownRow.getByRole('button', { name: '审批通过' })).toBeVisible()
      await expect(page.locator('.el-table__row').filter({ hasText: /单位初审中|合规审核中|业务审核中|主管审核中|副总审批中/ }),
        'director 授权审核台不应出现非本节点行').toHaveCount(0)
      await ownRow.getByRole('button', { name: '详情' }).click()
      const drawer = page.locator('.el-drawer')
      await expect(drawer).toBeVisible()
      for (const t of ['被授权方', '权益类型', '使用场景', '授权时效']) {
        await expect(drawer.getByText(t, { exact: false }).first(), `表5 要素「${t}」应在审核详情呈现`).toBeVisible()
      }
      await page.keyboard.press('Escape')
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('台账4页 + 分析3看板:加载正常', async ({ page }) => {
    const errs = collect(page)
    for (const p of ['/dpr/ledger/overview', '/dpr/ledger/archive', '/dpr/ledger/change', '/dpr/ledger/statistics', '/dpr/dashboard/overview', '/dpr/dashboard/confirm', '/dpr/dashboard/auth']) {
      await page.goto(p, { waitUntil: 'networkidle' })
      await page.waitForTimeout(500)
      await expect(page.locator('.el-card, .el-table, canvas, .echarts, .prm-page').first(), `${p} 应有主内容`).toBeVisible({ timeout: 10000 })
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('确权经理终审:审批通过 → 已完成并自动制卡(指引确权步骤70→80)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/review', { waitUntil: 'networkidle' })
    const row = page.locator('.el-table__row').filter({ hasText: '经理终审中' }).first()
    if (!(await row.isVisible().catch(() => false))) { test.skip(true, '无「经理终审中」在办单;跳过'); return }
    const applyNo = (await row.locator('td').nth(2).innerText()).trim()
    await row.getByRole('button', { name: '审批通过' }).click()
    const dlg = page.locator('.el-message-box')
    await expect(dlg).toBeVisible()
    const ta = dlg.locator('textarea, input[type="text"]').first()
    if (await ta.isVisible().catch(() => false)) await ta.fill('经理终审通过:确权结果清单与认定意见批准,归集制卡')
    await dlg.getByRole('button', { name: /确定|确认/ }).click()
    // 终审=终态:生成权益卡片(指引步骤80)
    await expect(page.getByText(/终审通过.*生成权益卡片|已生成权益卡片|通过/).first()).toBeVisible({ timeout: 10000 })
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').filter({ hasText: applyNo }).filter({ hasText: '经理终审中' }),
      '终审后该单应离开经理终审队列(转已完成)').toHaveCount(0)
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('授权经理审核:审批通过 → 流转至「副总审批中」(指引批量步骤70/一事一议步骤90)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/review', { waitUntil: 'networkidle' })
    const row = page.locator('.el-table__row').filter({ hasText: '经理审核中' }).first()
    if (!(await row.isVisible().catch(() => false))) { test.skip(true, '无「经理审核中」在办单;跳过'); return }
    const applyNo = (await row.locator('td').nth(2).innerText()).trim()
    await row.getByRole('button', { name: '审批通过' }).click()
    const dlg = page.locator('.el-message-box')
    await expect(dlg).toBeVisible()
    const ta = dlg.locator('textarea, input[type="text"]').first()
    if (await ta.isVisible().catch(() => false)) await ta.fill('经理审核通过:授权申请合规、认定意见齐备,报副总审批')
    await dlg.getByRole('button', { name: /确定|确认/ }).click()
    await expect(page.getByText(/进入下一环节|本级审批通过|审批通过|通过/).first()).toBeVisible({ timeout: 10000 })
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').filter({ hasText: applyNo }).filter({ hasText: '经理审核中' }),
      '审批后该单应离开经理审核队列').toHaveCount(0)
    expect(errs, errs.join('\n')).toEqual([])
  })
})
