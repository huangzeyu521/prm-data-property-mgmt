import { test, expect } from '@playwright/test'

/**
 * 数字化部主管(黄主管,role=manager)全功能走查 —— 运行须 PW_USER=manager。
 * 对照35号文 主管(数字化管理部门 主管)职责:
 *   确权 步骤60「审核数据确权结果清单及认定意见」/ 步骤80「制作数据权益卡片,确权记录归集」;
 *   批量授权 步骤60「审核《数据批量授权清单》(申报稿)」/ 步骤120「执行授权」;
 *   一事一议 步骤80「审核数据授权申请,出具数字化部认定意见」/ 步骤130「执行授权」;
 *   附录H:数字化管理部门归口全网数据权益管理,主管为其审核/认定/执行的关键岗。
 * 产品对应:确权「主管复核中」节点 + 授权「主管审核中」节点 + 权益卡片/证书 + 台账/分析只读。
 * 每页收集 console error + /api 响应≥400,任一即失败。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i, /sourcemap/i]

// role=manager 可见的全部菜单路由(lib/roles.js MENU)
const ROUTES = [
  ['/dpr/workbench/todo', '统一待办中心'],
  ['/dpr/guidance', '指引中心'],
  ['/dpr/ledger/overview', '产权台账概览'],
  ['/dpr/ledger/archive', '数据集产权档案管理'],
  ['/dpr/ledger/change', '产权变更记录管理'],
  ['/dpr/ledger/statistics', '产权台账统计分析'],
  ['/dpr/confirm/history', '确权申请查询'],
  ['/dpr/confirm/review', '确权审核申请提交'],
  ['/dpr/confirm/card', '权益卡片生成'],
  ['/dpr/confirm/cert', '权益证书管理'],
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

test.describe('数字化部主管(黄主管)角色走查', () => {

  test('首屏落点 + 菜单可见域 == AA-10 主管矩阵', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/', { waitUntil: 'networkidle' })
    // ROLE_HOME.manager = /dpr/workbench/todo
    await expect(page).toHaveURL(/\/dpr\/workbench\/todo/, { timeout: 15000 })
    const menu = page.locator('.el-aside, .el-menu').first()
    // 应见:产权信息/确权审核·卡片·证书/授权审核·批量清单/分析看板/待办
    for (const t of ['统一待办中心', '产权台账概览', '审核申请提交', '权益卡片生成', '权益证书管理', '授权审核提交', '批量授权清单', '数据产权全景']) {
      await expect(menu.getByText(t, { exact: false }).first(), `主管应可见菜单「${t}」`).toBeAttached()
    }
    // 不应见:申报入口(申报人专属)/合规校验/监测配置/授权配置/系统管理(非主管职责)
    for (const t of ['初始确权申请', '一事一议授权申请', '批量授权申请', '合规校验管理', '监测规则配置', '协议模板库', '用户管理', '操作日志']) {
      await expect(menu.getByText(t, { exact: false }), `主管不应见菜单「${t}」`).toHaveCount(0)
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('全路由扫雷:17 个主管页面 0 console 错误 / 0 api≥400', async ({ page }) => {
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

  test('待办中心:确权(主管复核中)+ 授权(主管审核中)双域待办均可见', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/workbench/todo', { waitUntil: 'networkidle' })
    // manager 双域:确权审批 + 授权审批 tab 均存在
    await expect(page.getByRole('tab', { name: /确权审批/ }), '主管应见确权审批 tab').toBeVisible()
    await expect(page.getByRole('tab', { name: /授权审批/ }), '主管应见授权审批 tab').toBeVisible()
    // 确权 tab 待办(若有)收敛为主管复核中
    const rowCount = await page.locator('.el-table__row').count()
    if (rowCount > 0) {
      await expect(page.getByText('主管复核中').first()).toBeVisible()
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('确权审核台:本节点「主管复核中」可审批 + 非本节点行禁用防越权', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/review', { waitUntil: 'networkidle' })
    await expect(page.locator('.prm-table-card, .el-table').first()).toBeVisible({ timeout: 10000 })
    const ownRow = page.locator('.el-table__row').filter({ hasText: '主管复核中' }).first()
    if (await ownRow.isVisible().catch(() => false)) {
      await expect(ownRow.getByRole('button', { name: '审批通过' })).toBeVisible()
      await expect(ownRow.getByRole('button', { name: '驳回' })).toBeVisible()
    }
    // 非本节点行(人工预审中/合规审核中/经理终审中)防越权:显示「非本人审批」
    const otherRow = page.locator('.el-table__row').filter({ hasText: /人工预审中|合规审核中|经理终审中/ }).first()
    if (await otherRow.isVisible().catch(() => false)) {
      await expect(otherRow.getByText('非本人审批')).toBeVisible()
      await expect(otherRow.getByRole('button', { name: '审批通过' })).toHaveCount(0)
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('授权审核台:本节点「主管审核中」可审批 + 详情表5 要素', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/review', { waitUntil: 'networkidle' })
    await expect(page.locator('.prm-table-card, .el-table').first()).toBeVisible({ timeout: 10000 })
    // 定位主管审核中行(AuthReviewDesk 硬收敛到本角色节点,manager 只见主管审核中;队列消费后为空则跳过)
    const ownRow = page.locator('.el-table__row').filter({ hasText: '主管审核中' }).first()
    if (await ownRow.isVisible().catch(() => false)) {
      await expect(ownRow.getByRole('button', { name: '审批通过' })).toBeVisible()
      await expect(ownRow.getByRole('button', { name: '驳回' })).toBeVisible()
      // 收敛正确性:审核台不应出现非本节点行(合规/业务/经理/副总审核中)——硬收敛防越权
      await expect(page.locator('.el-table__row').filter({ hasText: /合规审核中|业务审核中|经理审核中|副总审批中/ }),
        'manager 授权审核台不应出现非本节点行').toHaveCount(0)
      // 详情:表5《数据授权申请单》要素
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

  test('权益卡片生成 + 权益证书页:加载正常、可见卡片/证书', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/card', { waitUntil: 'networkidle' })
    await expect(page.locator('.el-table, .prm-table-card').first()).toBeVisible({ timeout: 10000 })
    await page.goto('/dpr/confirm/cert', { waitUntil: 'networkidle' })
    await expect(page.locator('.el-table, .prm-table-card').first()).toBeVisible({ timeout: 10000 })
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('批量授权清单 + 分析看板:加载正常', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/batch-list', { waitUntil: 'networkidle' })
    await expect(page.locator('.el-table, .prm-table-card').first()).toBeVisible({ timeout: 10000 })
    for (const p of ['/dpr/dashboard/overview', '/dpr/dashboard/confirm', '/dpr/dashboard/auth']) {
      await page.goto(p, { waitUntil: 'networkidle' })
      await page.waitForTimeout(500)
      await expect(page.locator('.el-card, canvas, .echarts, .prm-page').first()).toBeVisible({ timeout: 10000 })
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('确权主管复核:审批通过 → 流转至「经理终审中」(指引确权步骤60)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/review', { waitUntil: 'networkidle' })
    const row = page.locator('.el-table__row').filter({ hasText: '主管复核中' }).first()
    if (!(await row.isVisible().catch(() => false))) { test.skip(true, '无「主管复核中」在办单;跳过'); return }
    const applyNo = (await row.locator('td').nth(2).innerText()).trim()
    await row.getByRole('button', { name: '审批通过' }).click()
    const dlg = page.locator('.el-message-box')
    await expect(dlg).toBeVisible()
    const ta = dlg.locator('textarea, input[type="text"]').first()
    if (await ta.isVisible().catch(() => false)) await ta.fill('主管复核通过:确权结果清单与认定意见无异议')
    await dlg.getByRole('button', { name: /确定|确认/ }).click()
    await expect(page.getByText(/进入下一环节|审批通过|通过/).first()).toBeVisible({ timeout: 10000 })
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').filter({ hasText: applyNo }).filter({ hasText: '主管复核中' }),
      '审批后该单应离开主管复核队列').toHaveCount(0)
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('授权主管审核:审批通过 → 流转至「经理审核中」(指引批量步骤60/一事一议步骤80)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/review', { waitUntil: 'networkidle' })
    const row = page.locator('.el-table__row').filter({ hasText: '主管审核中' }).first()
    if (!(await row.isVisible().catch(() => false))) { test.skip(true, '无「主管审核中」在办单;跳过'); return }
    const applyNo = (await row.locator('td').nth(2).innerText()).trim()
    await row.getByRole('button', { name: '审批通过' }).click()
    const dlg = page.locator('.el-message-box')
    await expect(dlg).toBeVisible()
    const ta = dlg.locator('textarea, input[type="text"]').first()
    if (await ta.isVisible().catch(() => false)) await ta.fill('主管审核通过:授权申请合规、权属清晰,出具数字化部认定意见')
    await dlg.getByRole('button', { name: /确定|确认/ }).click()
    await expect(page.getByText(/进入下一环节|本级审批通过|审批通过|通过/).first()).toBeVisible({ timeout: 10000 })
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').filter({ hasText: applyNo }).filter({ hasText: '主管审核中' }),
      '审批后该单应离开主管审核队列').toHaveCount(0)
    expect(errs, errs.join('\n')).toEqual([])
  })
})
