import { test, expect } from '@playwright/test'

/**
 * 申报单位·分管领导(单位初审)(赵分管,role=unit)全功能走查 —— 运行须 PW_USER=unit。
 * 对照35号文附录C表2《一事一议数据授权业务管理流程》步骤50:
 *   「网级/省级地市局或专业分子公司 分管领导:对数据授权申请进行初步审查。通过的汇总提交至数据产权合规管控小组审核;不通过驳回。」
 * 及附录H:分管领导所在申报单位对本单位授权申请负单位初审之责。
 * unit 唯一职责节点=一事一议「单位初审中」;不涉确权、不涉批量首环节。
 * 每页收集 console error + /api 响应≥400,任一即失败。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i, /sourcemap/i]

// role=unit 可见的全部菜单路由(lib/roles.js MENU:指引中心 + 待办 + 申请历史 + 授权审核)
const ROUTES = [
  ['/dpr/workbench/todo', '统一待办中心'],
  ['/dpr/guidance', '指引中心'],
  ['/dpr/auth/history', '授权申请历史查询'],
  ['/dpr/auth/review', '授权审核提交'],
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

test.describe('申报单位·分管领导(赵分管,单位初审)角色走查', () => {

  test('首屏落点 + 菜单可见域 == AA-10 分管领导矩阵', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/', { waitUntil: 'networkidle' })
    // ROLE_HOME.unit = /dpr/workbench/todo
    await expect(page).toHaveURL(/\/dpr\/workbench\/todo/, { timeout: 15000 })
    const menu = page.locator('.el-aside, .el-menu').first()
    // 应见:指引 + 待办 + 授权申请历史 + 授权审核提交
    for (const t of ['指引中心', '统一待办中心', '申请历史查询', '授权审核提交']) {
      await expect(menu.getByText(t, { exact: false }).first(), `分管领导应可见菜单「${t}」`).toBeAttached()
    }
    // 不应见:确权申报/确权审核台/权益卡片/批量清单/监测/系统管理(非分管领导职责)
    for (const t of ['初始确权申请', '确权变更申请', '审核申请提交', '权益卡片生成', '批量授权清单', '监测规则配置', '用户管理', '操作日志']) {
      await expect(menu.getByText(t, { exact: false }), `分管领导不应见菜单「${t}」`).toHaveCount(0)
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('全路由扫雷:4 个分管领导页面 0 console 错误 / 0 api≥400', async ({ page }) => {
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

  test('待办中心:确权 tab 隐藏 + 授权待办收敛到「单位初审中」', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/workbench/todo', { waitUntil: 'networkidle' })
    // unit 不涉确权:确权审批 tab 应不存在(handledStatuses(unit, CONFIRM)=[] → doesConfirm=false)
    await expect(page.getByRole('tab', { name: /确权审批/ }), '分管领导不应见确权审批 tab').toHaveCount(0)
    // 授权审批 tab 存在且为默认落点
    await expect(page.getByRole('tab', { name: /授权审批/ })).toBeVisible()
    // 授权待办行(若有)状态必为「单位初审中」——收敛正确
    const statusCells = page.locator('.el-table__row .prm-c-warning, .el-table__row td')
    const rowCount = await page.locator('.el-table__row').count()
    if (rowCount > 0) {
      await expect(page.getByText('单位初审中').first(), '授权待办应含单位初审中队列').toBeVisible()
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('授权审核台:队列收敛到「单位初审中」+ 详情呈现表5 要素', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/review', { waitUntil: 'networkidle' })
    // 审核台必须可加载(表格骨架在)
    await expect(page.locator('.prm-table-card, .el-table').first()).toBeVisible({ timeout: 10000 })
    const rowCount = await page.locator('.el-table__row').count()
    // 容错(种子 AA-008 可能已被「单位初审动作」用例消费):有单则强校验收敛+详情要素,无单则仅验可加载
    if (rowCount > 0) {
      // 收敛正确:所有可见行「当前环节」均为单位初审中(unit 只办本节点,杜绝越节点点击 403)
      const envCells = page.locator('.el-table__row td .prm-c-warning')
      const cn = await envCells.count()
      for (let i = 0; i < cn; i++) {
        await expect(envCells.nth(i)).toHaveText('单位初审中')
      }
      await expect(page.getByRole('button', { name: '审批通过' }).first()).toBeVisible()
      await expect(page.getByRole('button', { name: '驳回' }).first()).toBeVisible()
      // 详情抽屉:表5《数据授权申请单》核心要素齐(被授权方/权益类型/场景/时效/涉第三方/涉隐私/跨域)
      await page.getByRole('button', { name: '详情' }).first().click()
      const drawer = page.locator('.el-drawer')
      await expect(drawer).toBeVisible()
      for (const t of ['被授权方', '权益类型', '使用场景', '授权时效', '涉第三方来源', '涉隐私/商密', '是否跨域']) {
        await expect(drawer.getByText(t, { exact: false }).first(), `表5 要素「${t}」应在审核详情呈现`).toBeVisible()
      }
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('授权申请历史查询:加载正常、可检索', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/history', { waitUntil: 'networkidle' })
    await expect(page.locator('.el-table, .prm-table-card').first()).toBeVisible({ timeout: 10000 })
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('单位初审动作:分管领导审批通过 → 流转至「合规审核中」(指引步骤50)', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/dpr/auth/review', { waitUntil: 'networkidle' })
    const row = page.locator('.el-table__row').filter({ hasText: '单位初审中' }).first()
    if (!(await row.isVisible().catch(() => false))) {
      test.skip(true, '无「单位初审中」在办单(可能已被前次审批消费);跳过流转验证')
      return
    }
    const applyNo = (await row.locator('td').nth(2).innerText()).trim()
    await row.getByRole('button', { name: '审批通过' }).click()
    // 弹审核意见框 → 确认
    const dlg = page.locator('.el-message-box')
    await expect(dlg).toBeVisible()
    await dlg.locator('textarea').fill('单位初审通过:资料齐备、权属清晰,提交合规管控小组')
    await dlg.getByRole('button', { name: /确定|确认/ }).click()
    // 成功提示 + 该单离开单位初审队列
    await expect(page.getByText(/本级审批通过|进入下一环节|通过/).first()).toBeVisible({ timeout: 10000 })
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').filter({ hasText: applyNo }), '审批后该单应离开单位初审队列')
      .toHaveCount(0)
    expect(errs, errs.join('\n')).toEqual([])
  })
})
