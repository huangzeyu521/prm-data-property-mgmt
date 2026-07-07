import { test, expect } from '@playwright/test'

/**
 * 数据产权合规管控小组(李天天,role=review)全功能走查 —— 运行须 PW_USER=review。
 * 对照35号文三大流程中「数据产权合规管控小组」的三个法定职责节点:
 *   - 确权流程步骤50「审核数据确权资料」:审核确权资料合规性,记录数据确权结果并归集,形成数据权益认定意见。
 *   - 批量授权流程步骤50「审核数据批量授权资料」:收集整合《数据批量授权清单》,进行合规审核,出具合规决策意见。
 *   - 一事一议流程步骤60「审核数据授权申请合规性」:对数据授权申请进行合规评审。
 * 及第151行:「数据产权合规管控小组应当对持有权、数据使用权、数据经营权可能面临的风险进行评估告知」。
 * 每页收集 console error + /api 响应≥400,任一即失败。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i, /sourcemap/i]

// role=review 可见的全部菜单路由(lib/roles.js MENU)
const ROUTES = [
  ['/dpr/workbench/todo', '统一待办中心'],
  ['/dpr/guidance', '指引中心'],
  ['/dpr/ledger/overview', '产权台账概览'],
  ['/dpr/ledger/archive', '数据集产权档案管理'],
  ['/dpr/ledger/change', '产权变更记录管理'],
  ['/dpr/ledger/statistics', '产权台账统计分析'],
  ['/dpr/monitor/status', '权益状态监控'],
  ['/dpr/monitor/alert', '权益变动监测预警'],
  ['/dpr/monitor/compliance', '合规性检查'],
  ['/dpr/confirm/history', '确权申请查询'],
  ['/dpr/confirm/review', '确权审核申请提交'],
  ['/dpr/auth/batch-list', '批量授权清单'],
  ['/dpr/auth/compliance', '合规校验管理'],
  ['/dpr/auth/history', '授权申请历史查询'],
  ['/dpr/auth/review', '授权审核提交'],
  ['/dpr/auth/agreement', '协议工作台'],
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

test.describe('数据产权合规管控小组(李天天,review)角色走查', () => {

  test('首屏落点 + 菜单可见域 == AA-10 合规管控小组矩阵', async ({ page }) => {
    const errs = collect(page)
    await page.goto('/', { waitUntil: 'networkidle' })
    // ROLE_HOME.review = /dpr/workbench/todo
    await expect(page).toHaveURL(/\/dpr\/workbench\/todo/, { timeout: 15000 })
    const menu = page.locator('.el-aside, .el-menu').first()
    // 折叠子菜单内的项存在于 DOM 但不可见(el-sub-menu 默认收起,unique-opened);断言"存在"即角色过滤放行,不苛求当前展开可见
    for (const t of ['指引中心', '统一待办中心', '产权台账概览', '权益变动监测预警', '合规性检查', '申请查询', '批量授权清单', '合规校验管理', '授权审核提交']) {
      await expect(menu.getByText(t, { exact: false }).first(), `菜单应含「${t}」`).toBeAttached()
    }
    // 越权菜单不应可见:初始确权申请/确权变更申请(申报人专属)、一事一议/批量授权申请(申报人/business发起)、系统管理(admin专属)
    for (const t of ['⭐ 初始确权申请', '⭐ 一事一议授权申请', '⭐ 批量授权申请', '用户管理', '角色管理']) {
      await expect(menu.getByText(t, { exact: true }), `菜单不应含「${t}」(越权)`).toHaveCount(0)
    }
    expect(errs, errs.join('\n')).toEqual([])
  })

  test('全路由扫雷:17 个合规管控小组页面 0 console 错误 / 0 api≥400', async ({ page }) => {
    for (const [path, title] of ROUTES) {
      const errs = collect(page)
      await page.goto(path, { waitUntil: 'networkidle' })
      await page.waitForTimeout(500)
      expect(errs, `${title}(${path}):\n` + errs.join('\n')).toEqual([])
    }
  })

})
