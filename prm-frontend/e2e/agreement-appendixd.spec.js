import { test, expect } from '@playwright/test'

/**
 * 附录D协议文档生成验证:
 * ① 协议工作台 签章页 有「下载协议草案/正式稿(附录D)」按钮(草案=实时渲染,正式稿=锁定快照)
 * ② 点击触发下载(后端按附录D《南方电网数据授权运营协议》生成 .doc)
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('协议工作台 签章页:下载附录D协议草案', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/auth/agreement', { waitUntil: 'networkidle' })
  await expect(page.locator('.el-table')).toBeVisible({ timeout: 15000 })

  // ① 按钮存在(草案/正式稿两态)
  const btn = page.getByRole('button', { name: /下载协议(草案|正式稿)\(附录D\)/ }).first()
  await expect(btn).toBeVisible()

  // ② 点击触发下载
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 15000 }),
    btn.click()
  ])
  expect(download.suggestedFilename()).toMatch(/数据授权运营协议(草案|正式稿).*\.doc/)

  expect(errs, `附录D草案页运行时错误:\n${errs.join('\n')}`).toEqual([])
})
