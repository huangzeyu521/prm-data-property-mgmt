import { test, expect } from '@playwright/test'

/**
 * 一事一议 step0 UX(P0+P1):
 *  P0 ① 使用场景 allow-create:可输入自定义场景(不在库也能申报)
 *  P1 ② 渐进披露:利益分配/安全保障/保密 默认折叠("更多")
 *      ③ 需保密承诺函开启 → step1 应交清单出现《保密承诺函(附录E)》上传项
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('一事一议 step0:自定义场景 + 协议要素折叠 + 保密承诺函 step1 上传', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  await page.goto('/dpr/auth/wizard', { waitUntil: 'networkidle' })

  // 权益类型 = 使用权(分段单选 radio)
  const rt = page.locator('.el-form-item', { hasText: '授权权益类型' }).first()
  await rt.locator('.el-radio-button', { hasText: '使用权' }).click()

  // 被授权方:外部主体 input
  const gr = page.locator('.el-form-item', { hasText: '申请主体(被授权方)' }).first()
  await gr.getByText('被授权方为外部主体').click()
  await gr.getByPlaceholder(/外部被授权主体名称/).fill('某外部数据经营企业')

  // P0:使用场景 allow-create 输入自定义(库里没有的特定事项)
  const sc = page.locator('.el-form-item', { hasText: '使用场景' }).first()
  await sc.locator('.el-select').click()
  await sc.locator('input').first().fill('联合风控试点专项')
  await page.keyboard.press('Enter')

  // 目的摘要(表5「使用场景及目的摘要」)必填:自定义场景无模板带出,须手填
  await page.locator('.el-form-item', { hasText: '目的摘要' }).first().locator('textarea')
    .fill('用于联合风控试点专项数据分析,数据不出域、仅限本次事项')

  // 主管 / 联系
  await page.locator('.el-form-item', { hasText: '申请单位主管' }).first().locator('input').fill('张三')
  await page.locator('.el-form-item', { hasText: '联系方式' }).first().locator('input').fill('020-66660000')

  // P1 渐进披露:利益分配默认折叠不可见 → 展开"更多"后可见
  const benefit = page.locator('.el-form-item', { hasText: '利益分配约定' }).first()
  await expect(benefit).not.toBeVisible()
  await page.locator('.el-collapse-item__header', { hasText: '更多' }).click()
  await expect(benefit).toBeVisible()

  // 折叠区内开启 需保密承诺函
  await page.locator('.el-form-item', { hasText: '需保密承诺函' }).first().locator('.el-switch').click()

  // 下一步(createForm;自定义场景被接受 → 能进入 step1 即证明 allow-create 生效)
  await page.getByRole('button', { name: '下一步' }).click()
  await expect(page.getByRole('button', { name: /从确权目录选取数据资产/ })).toBeVisible({ timeout: 15000 })

  // P1 ③:step1 应交清单出现《保密承诺函(附录E)》待上传项
  const confRow = page.locator('.el-table__row', { hasText: '保密承诺函' }).first()
  await expect(confRow).toBeVisible()
  await expect(confRow).toContainText('待上传')

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
