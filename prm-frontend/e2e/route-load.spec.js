import { test, expect } from '@playwright/test'

/**
 * 路由加载冒烟:逐路由真浏览器加载,断言无 console error / 未捕获异常 / API≥400。
 * 正是这道门此前揪出 initChart 返回 undefined 致全景/统计两页整页崩(见 lib/chartBase.js)。
 */
const ROUTES = [
  '/dpr/dashboard/overview', '/dpr/dashboard/confirm', '/dpr/dashboard/auth',
  '/dpr/workbench/my', '/dpr/workbench/todo',
  '/dpr/ledger/overview', '/dpr/ledger/dataset', '/dpr/ledger/archive', '/dpr/ledger/change', '/dpr/ledger/statistics',
  '/dpr/confirm/wizard', '/dpr/confirm/change', '/dpr/confirm/card', '/dpr/confirm/catalog',
  '/dpr/confirm/cert', '/dpr/confirm/guidance', '/dpr/confirm/history', '/dpr/confirm/review',
  '/dpr/auth/batch-wizard', '/dpr/auth/wizard', '/dpr/auth/batch-list', '/dpr/auth/cert', '/dpr/auth/compliance',
  '/dpr/auth/filing', '/dpr/auth/guidance', '/dpr/auth/history', '/dpr/auth/review', '/dpr/auth/scenario',
  '/dpr/auth/agreement', '/dpr/auth/form-template', '/dpr/auth/cert-template',
  '/dpr/monitor/alert', '/dpr/monitor/rule', '/dpr/monitor/status', '/dpr/monitor/notification', '/dpr/monitor/compliance',
  '/dpr/guidance', '/dpr/system/user', '/dpr/system/role', '/dpr/system/oplog'
]
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

for (const route of ROUTES) {
  test(`route loads clean: ${route}`, async ({ page, baseURL }) => {
    const errs = []
    page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
    page.on('pageerror', e => errs.push('pageerror: ' + e.message))
    page.on('response', r => { if (r.url().includes('/api/') && r.status() >= 400) errs.push('api' + r.status() + ': ' + r.url().replace(baseURL, '')) })
    await page.goto(route, { waitUntil: 'networkidle' })
    await page.waitForTimeout(800)
    expect(errs, `运行时错误 @ ${route}:\n${errs.join('\n')}`).toEqual([])
  })
}
