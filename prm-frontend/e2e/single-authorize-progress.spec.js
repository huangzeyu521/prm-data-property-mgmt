import { test, expect } from '@playwright/test'

/**
 * A+C′ 一事一议结构回归(确定性):
 * ① 向导收敛为 3 步(去旧「完成/审批→双签→发证」步)
 * ② 进度时间轴 AuthFlowProgress mode='single' 按 35号文 表2 渲染(无领导小组,终点=副总/总经理)
 * ③ 无「证书/发证 / 附录F §4.3」误述 ④ 运行时零错误。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('single authorize A+C′: 3步向导 + 一事一议进度时间轴(无领导小组)', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/auth/wizard', { waitUntil: 'networkidle' })
  const body = page.locator('body')

  // ① 3 步
  await expect(body).toContainText('填写申请')
  await expect(body).toContainText('合规校验')
  await expect(body).toContainText('确认并提交')
  await expect(body).not.toContainText('审批→双签→发证')

  // ② 一事一议进度节点(表2):有「本单位初审」「业务管理部门审核」、终点副总/总经理 + 双签 + 执行
  await expect(body).toContainText('本单位初审')
  await expect(body).toContainText('业务管理部门审核')
  await expect(body).toContainText('甲乙双签《数据运营授权协议(附录D)》')
  await expect(body).toContainText('执行授权 · 记录')
  // 关键:一事一议无「领导小组决策批准」节点(与批量的区别)
  await expect(body).not.toContainText('领导小组决策批准')

  // ③ 无误述
  await expect(body).not.toContainText('自动签发授权证书')
  await expect(body).not.toContainText('自动发证')
  await expect(body).not.toContainText('附录F')

  expect(errs, `一事一议向导运行时错误:\n${errs.join('\n')}`).toEqual([])
})
