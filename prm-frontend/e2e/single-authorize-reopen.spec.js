import { test, expect } from '@playwright/test'

/**
 * 一事一议 路由预填(reopen 重提)验证:
 *  sessionStorage『prm-reopen』原单 → 单头预填(权益/被授权方/主管/联系) + 暂存数据表
 *  →「下一步」(createForm) 后自动加入明细 → step1 含该数据表。
 * (权益卡片「发起授权」走同一 pendingAsset 通道,query 带 assetId/rightType。)
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]

test('一事一议:reopen 路由预填 → 单头预填 + 自动加入暂存数据表', async ({ page }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  // 预置被驳回原单(synthetic;卡号非 FROZEN → 桩可用,使用权+空范围 → 过提交门禁)
  await page.addInitScript(() => {
    sessionStorage.setItem('prm-reopen', JSON.stringify({
      domain: '授权',
      raw: {
        assetId: 'SYS:重提系统', assetName: '重提数据表', equityCardId: 'EC-REOPEN-1',
        rightType: '使用权', granteeOrg: '广州供电局', scenario: '电力金融征信',
        purposeNote: '用于电力金融征信专项分析(重提)', benefitAllocation: '按次计费', securityReq: '加密传输',
        applicantManager: '李主管', contactInfo: '020-66668888'
      }
    }))
  })
  await page.goto('/dpr/auth/wizard?reopen=1', { waitUntil: 'networkidle' })

  // ① 单头预填:权益类型 = 使用权;主管/联系已带入
  await expect(page.locator('.el-form-item', { hasText: '授权权益类型' }).first()).toContainText('使用权')
  await expect(page.locator('.el-form-item', { hasText: '申请单位主管' }).first().locator('input')).toHaveValue('李主管')
  await expect(page.locator('.el-form-item', { hasText: '联系方式' }).first().locator('input')).toHaveValue('020-66668888')

  // ②「下一步」(createForm) → 自动加入暂存数据表 → step1 明细含「重提数据表」
  await page.getByRole('button', { name: '下一步' }).click()
  const detailTable = page.locator('.el-table').filter({ hasText: '生效卡片' }).first()
  await expect(detailTable).toContainText('重提数据表', { timeout: 15000 })

  // ③ 自检全过(synthetic 使用权 + 空范围 + 卡可用)
  await page.getByRole('button', { name: '下一步' }).click()
  await page.getByRole('button', { name: /一键自检/ }).click()
  await expect(page.getByText('全部合规,可提交')).toBeVisible({ timeout: 20000 })

  expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
})
