import { test, expect } from '@playwright/test'

/**
 * 协议工作台 操作列验证(协议自动生成 + 要素落定门禁 + 双签自动核验/归档,删人工审核节点):
 * ① 工作台 = 2 个 tab(① 签章上传 → ② 协议存档),不再有「协议审核」tab
 * ② 签章页:无「生成协议」按钮(协议系统自动形成);有 要素落定 + 甲乙签章 + 承诺函(附录E) + 核验记录
 * ③ 存档页:无「归档」按钮(双签生效系统自动归档);预览/下载/审计 + 续期/终止(动态跟踪)
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('协议工作台:2tab + 签章无生成按钮 + 存档无归档按钮', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/auth/agreement', { waitUntil: 'networkidle' })
  // 作用域到工作台自身的 border-card 标签(避免命中布局的 tags-view 标签栏)
  const wb = page.locator('.el-tabs--border-card').first()
  const tabs = wb.locator('.el-tabs__item')
  await expect(tabs).toHaveCount(2)                       // ① 仅 2 个 tab
  await expect(wb.locator('.el-tabs__item', { hasText: '签章上传' })).toBeVisible()
  await expect(wb.locator('.el-tabs__item', { hasText: '协议存档' })).toBeVisible()
  await expect(wb.locator('.el-tabs__item', { hasText: '协议审核' })).toHaveCount(0)  // 无审核 tab

  // ② 签章页:无「生成协议」,有 要素落定门禁 + 甲乙签章 + 承诺函(附录E) + 核验记录
  await expect(page.locator('.el-table')).toBeVisible({ timeout: 15000 })
  await expect(page.locator('body')).not.toContainText('生成协议')
  await expect(page.getByRole('button', { name: /要素落定|要素\(已锁定\)/ }).first()).toBeVisible()
  await expect(page.getByRole('button', { name: '甲方签章' }).first()).toBeVisible()
  await expect(page.getByRole('button', { name: '乙方签章' }).first()).toBeVisible()
  await expect(page.locator('body')).toContainText('承诺函')   // 附录E 收口入口(按钮或已收口标签)
  await expect(page.getByRole('button', { name: '核验记录' }).first()).toBeVisible()
  // 草案行(种子 AG-003/XY-0003)签章按钮禁用(要素落定门禁);要素落定弹窗可打开。
  // 注:行定位用协议编号,不能用「草案」(会命中每行的「下载协议草案(附录D)」按钮文本);
  //    fixed 列会把操作按钮复制到悬浮表,故按钮断言取 .first()。
  const draftRow = page.locator('.el-table__row', { hasText: 'XY-0003' }).first()
  if (await draftRow.count()) {
    // el-upload 外壳 div 也带 role=button 且恒 enabled,须断言内部真 <button>
    await expect(draftRow.locator('button', { hasText: '甲方签章' }).first()).toBeDisabled()
    await draftRow.locator('button', { hasText: '① 要素落定' }).first().click()
    const dlg = page.locator('.el-dialog', { hasText: '协议要素落定' })
    await expect(dlg).toBeVisible()
    await expect(dlg).toContainText('授权有效期止日')
    await expect(dlg).toContainText('违约金')
    await expect(dlg.getByRole('button', { name: '生成正式稿(锁定)' })).toBeVisible()
    await dlg.getByRole('button', { name: '关闭', exact: true }).click()
  }

  // ③ 存档页:无「归档」按钮,有 预览/下载 + 续期/终止(动态跟踪)
  await wb.locator('.el-tabs__item', { hasText: '协议存档' }).click()
  await expect(page.locator('.el-table')).toBeVisible({ timeout: 15000 })
  await expect(page.getByRole('button', { name: /^归档$/ })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '下载' }).first()).toBeVisible()
  await expect(page.getByRole('button', { name: '续期' }).first()).toBeVisible()
  await expect(page.getByRole('button', { name: '终止' }).first()).toBeVisible()

  expect(errs, `协议工作台运行时错误:\n${errs.join('\n')}`).toEqual([])
})
