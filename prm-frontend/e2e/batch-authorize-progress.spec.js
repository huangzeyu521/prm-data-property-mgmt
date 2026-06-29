import { test, expect } from '@playwright/test'

/**
 * A+C′ 结构回归(确定性,不依赖资源池数据/合规门禁):
 * ① 向导收敛为 3 步,去表号化标题 ② 移除旧「完成/决策→双签→发证」步
 * ③ 进度时间轴 AuthFlowProgress 按 35号文 表1 渲染官方批量节点(含责任角色)
 * ④ 全程无「证书/发证 / 附录F §4.2」误述 ⑤ 运行时零错误。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('batch authorize A+C′: 3步向导 + 进度时间轴结构正确', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/auth/batch-wizard', { waitUntil: 'networkidle' })
  const body = page.locator('body')

  // ① 3 步去表号化标题
  await expect(body).toContainText('清单基础信息')
  await expect(body).toContainText('选择授权数据')
  await expect(body).toContainText('确认并提交')
  // ② 旧 4 步标题已移除
  await expect(body).not.toContainText('建批量清单')
  await expect(body).not.toContainText('逐条加授权项')
  await expect(body).not.toContainText('决策→双签→发证')

  // ③ 进度时间轴:官方批量节点 + 责任角色(AuthFlowProgress 已挂载渲染)
  await expect(body).toContainText('合规管控小组审核')
  await expect(body).toContainText('数据产权合规管控小组')
  await expect(body).toContainText('领导小组决策批准')
  await expect(body).toContainText('网络安全和数字化转型领导小组办公室')
  await expect(body).toContainText('甲乙双签《数据运营授权协议(附录D)》')
  await expect(body).toContainText('执行授权 · 归档(对外经营权另备案附录G)')

  // ④ 无「证书/发证 / 附录F §4.2」误述
  await expect(body).not.toContainText('自动签发授权证书')
  await expect(body).not.toContainText('自动发证')
  await expect(body).not.toContainText('附录F')

  expect(errs, `批量授权向导运行时错误:\n${errs.join('\n')}`).toEqual([])
})
