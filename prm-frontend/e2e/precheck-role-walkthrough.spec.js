import { test, expect } from '@playwright/test'

/**
 * 归集预审·数字化部团队(周慎之,role=precheck)全功能走查 —— 运行须 PW_USER=precheck。
 * 对照35号文附录C《数据确权业务管理流程》步骤40:
 *   「网级/省级地市局或专业分子公司数字化管理部门 团队负责人/成员:归集/审查数据确权资料完整性,
 *     收集业务单位提供的数据确权资料。」→ 通过后进步骤50 数据产权合规管控小组审核。
 * 产品对应「人工预审」节点(复核 AI 校验结果 + 资料完整性),角色 precheck;不涉授权、不制卡。
 * 每页收集 console error + /api 响应≥400,任一即失败。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i, /sourcemap/i]

// role=precheck 可见的全部菜单路由(lib/roles.js MENU:指引 + 待办 + 确权申请查询 + 确权审核提交)
const ROUTES = [
  ['/dpr/workbench/todo', '统一待办中心'],
  ['/dpr/guidance', '指引中心'],
  ['/dpr/confirm/history', '确权申请查询'],
  ['/dpr/confirm/review', '确权审核申请提交'],
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

test.describe('归集预审·数字化部团队(周慎之,人工预审)角色走查', () => {

  test('首屏落点 + 菜单可见域 == AA-10 归集预审矩阵', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/', { waitUntil: 'networkidle' })
    // ROLE_HOME.precheck = /dpr/workbench/todo
    await expect(page).toHaveURL(/\/dpr\/workbench\/todo/, { timeout: 15000 })
    const menu = page.locator('.el-aside, .el-menu').first()
    // 应见:指引 + 待办 + 确权申请查询 + 确权审核提交
    for (const t of ['指引中心', '统一待办中心', '申请查询', '审核申请提交']) {
      await expect(menu.getByText(t, { exact: false }).first(), `归集预审应可见菜单「${t}」`).toBeAttached()
    }
    // 不应见:确权申报/权益卡片/授权全组/监测/系统管理(非归集预审职责)
    for (const t of ['初始确权申请', '确权变更申请', '权益卡片生成', '权益证书管理', '一事一议授权申请', '批量授权申请', '授权审核提交', '监测规则配置', '用户管理']) {
      await expect(menu.getByText(t, { exact: false }), `归集预审不应见菜单「${t}」`).toHaveCount(0)
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('全路由扫雷:4 个归集预审页面 0 console 错误 / 0 api≥400', async ({ page }) => {
    const bad = []
    for (const [path, name] of ROUTES) {
      const errs = collect(page)
      await page.goto(path, { waitUntil: 'networkidle' })
      await page.waitForTimeout(600)
      const hasContent = await page.locator('.el-card, .prm-page, .el-table, .el-form, .prm-table-card').first().isVisible().catch(() => false)
      if (!hasContent) bad.push(`${name}(${path}): 页面无主内容渲染`)
      if (errs.length) bad.push(`${name}(${path}):\n  ` + errs.join('\n  '))
      page.removeAllListeners('console'); page.removeAllListeners('pageerror'); page.removeAllListeners('response')
    }
    expect(bad, '页面走查发现问题:\n' + bad.join('\n')).toEqual([])
  })

  test('待办中心:授权 tab 隐藏 + 确权待办收敛到「人工预审中」', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/workbench/todo', { waitUntil: 'networkidle' })
    // precheck 不涉授权:授权审批 tab 应不存在(handledStatuses(precheck, AUTH)=[] → doesAuth=false)
    await expect(page.getByRole('tab', { name: /授权审批/ }), '归集预审不应见授权审批 tab').toHaveCount(0)
    // 确权审批 tab 存在
    await expect(page.getByRole('tab', { name: /确权审批/ })).toBeVisible()
    // 确权待办行(若有)状态必为「人工预审中」——收敛正确
    const rowCount = await page.locator('.el-table__row').count()
    if (rowCount > 0) {
      await expect(page.getByText('人工预审中').first(), '确权待办应含人工预审中队列').toBeVisible()
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('确权审核台:本节点「人工预审中」可审批 + 非本节点行禁用防越权 + AI校验依据呈现', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/review', { waitUntil: 'networkidle' })
    await expect(page.locator('.prm-table-card, .el-table').first()).toBeVisible({ timeout: 10000 })
    const ownRow = page.locator('.el-table__row').filter({ hasText: '人工预审中' }).first()
    if (await ownRow.isVisible().catch(() => false)) {
      // 本节点行:有「审批通过/驳回」动作(canAct=true)
      await expect(ownRow.getByRole('button', { name: '审批通过' })).toBeVisible()
      await expect(ownRow.getByRole('button', { name: '驳回' })).toBeVisible()
      // 详情抽屉:表1 要素 + 「AI 校验结果（人工预审依据）」(precheck 唯一职责=复核AI校验+资料完整性)
      await ownRow.getByRole('button', { name: '详情' }).click()
      const drawer = page.locator('.el-drawer')
      await expect(drawer).toBeVisible()
      await expect(drawer.getByText('AI 校验结果', { exact: false }).first(), '人工预审详情应含 AI 校验依据区').toBeVisible()
      // 抽屉「关闭」命中头部X+底部按钮两处,用 Escape 收起最稳
      await page.keyboard.press('Escape')
      await expect(drawer).toBeHidden()
    }
    // 非本节点行(合规/主管/经理审核中)防越权:禁用审批、显示「非本人审批」,杜绝误点 403
    const otherRow = page.locator('.el-table__row').filter({ hasText: /合规审核中|主管复核中|经理终审中/ }).first()
    if (await otherRow.isVisible().catch(() => false)) {
      await expect(otherRow.getByText('非本人审批'), '非本节点行应禁用并提示非本人审批').toBeVisible()
      await expect(otherRow.getByRole('button', { name: '审批通过' })).toHaveCount(0)
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('确权申请查询:加载正常、登记类型/进度可见', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/history', { waitUntil: 'networkidle' })
    await expect(page.locator('.el-table, .prm-table-card, .el-tabs').first()).toBeVisible({ timeout: 10000 })
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('归集预审动作:人工预审通过 → 流转至「合规审核中」(指引步骤40→50)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/confirm/review', { waitUntil: 'networkidle' })
    const row = page.locator('.el-table__row').filter({ hasText: '人工预审中' }).first()
    if (!(await row.isVisible().catch(() => false))) {
      test.skip(true, '无「人工预审中」在办单(可能已被前次审批消费);跳过流转验证')
      return
    }
    const applyNo = (await row.locator('td').nth(2).innerText()).trim()
    await row.getByRole('button', { name: '审批通过' }).click()
    // 弹审核意见框(人工预审:复核AI校验结果)→ 确认
    const dlg = page.locator('.el-message-box')
    await expect(dlg).toBeVisible()
    const ta = dlg.locator('textarea, input[type="text"]').first()
    if (await ta.isVisible().catch(() => false)) await ta.fill('归集预审通过:AI校验结果已复核、表1/表2资料完整,提交合规管控小组')
    await dlg.getByRole('button', { name: /确定|确认/ }).click()
    // 成功提示 + 该单离开人工预审队列(进入合规审核中)
    await expect(page.getByText(/进入下一环节|审批通过|通过/).first()).toBeVisible({ timeout: 10000 })
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').filter({ hasText: applyNo }).filter({ hasText: '人工预审中' }),
      '审批后该单应离开人工预审队列').toHaveCount(0)
    expect(errs, errs.join('\n')).toEqual([])
  })
})
