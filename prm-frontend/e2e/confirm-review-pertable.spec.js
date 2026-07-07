import { test, expect } from '@playwright/test'

/**
 * P3 审核侧活页验证:审核工作台详情抽屉「逐表凭证 · 表2」区(投影 listTableItems·单一真源)。
 *  种子(API 直连 19102):SYS:客户服务系统,4 表(A+H / B / C / F)带逐表凭证 → push-review → 人工预审中;
 *  UI:审核台按 applyNo 找行 → 详情 → 断言逐表区 4 槽位 + 各表专属附件名;清理 withdraw→delete(best-effort)。
 */
const IGNORE = [/ResizeObserver/i, /favicon/i, /\[Vue warn\]/i, /Devtools/i]
const B = `http://127.0.0.1:${process.env.PRM_CONFIRM_PORT || '9102'}/api/dpr/confirm`

function tbl(code, name, src, extra = {}) {
  return {
    tableCode: code, tableName: name, schemaName: 'CS_DB01', instanceName: 'CS_ORA', secretLevel: '敏感信息',
    sourceType: src, sourceSubject: '外部来源方', sourceDesc: '来源说明', gFlag: '否', hFlag: '否', iFlag: '否', jFlag: '否',
    sourceAttachment: '', privacyAttachment: '', ...extra
  }
}

test('审核侧:详情抽屉逐表凭证区(表2 同源)', async ({ page, request }) => {
  const errs = []
  page.on('console', m => { if (m.type() === 'error' && !IGNORE.some(r => r.test(m.text()))) errs.push('console: ' + m.text()) })
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))

  // —— API 种数据:完整申请 → 人工预审中 ——
  const draft = await (await request.post(`${B}/apply/draft`, { data: {
    assetId: 'SYS:客户服务系统', assetName: '客户服务系统', rightHolder: '广东电网有限责任公司', subjectLevel: '分省公司',
    systemOwner: '陈静', contactInfo: '020-31006006', registerType: '初始确权', applyMode: '常规', regulated: '非管制',
    rightType: '持有权', status: '草稿', sourceIdentification: 'A,B,C,F', relationIdentification: 'H', involvesThirdParty: true,
    purpose: '【E2E 种子】审核侧逐表凭证自动测试数据;流程设计上提交后不可删(仅审批终态),重启容器即清'
  } })).json()
  const applyId = draft.data
  expect(applyId, '种子 draft 应成功').toBeTruthy()
  await request.post(`${B}/apply/${applyId}/table-items`, { data: [
    tbl('CS_COMPLAINT', '投诉记录表', 'A 自行生产数据', { hFlag: '是', hSubject: '家庭用电信息,仅用于电力服务', privacyAttachment: '用户入网协议(个人信息授权).pdf' }),
    tbl('CS_KB_FAQ', '服务知识表', 'B 公开采集数据', { sourceAttachment: '公共采集情况说明.pdf' }),
    tbl('CS_KB_POLICY', '政策法规表', 'C 公共授权数据', { sourceAttachment: '公共数据授权说明.pdf' }),
    tbl('CS_KB_EXT', '外部参考资料表', 'F 其他来源数据', { sourceAttachment: '其他来源情况说明.pdf' })
  ] })
  await request.post(`${B}/material/sync-platform?applyId=${applyId}`)
  await request.post(`${B}/material/check-run?applyId=${applyId}`)
  const pr = await (await request.post(`${B}/material/push-review?applyId=${applyId}`)).json()
  expect(pr.code, 'push-review 应成功(→人工预审中)').toBe(200)
  const pg = await (await request.post(`${B}/apply/page`, { data: { current: 1, size: 50, keyword: '客户服务系统' } })).json()
  const rec = (pg.data.records || []).find(r => r.applyId === applyId)
  const applyNo = rec && rec.applyNo
  expect(applyNo, '应取到 applyNo').toBeTruthy()

  try {
    // —— UI:审核台 → 详情抽屉 → 逐表凭证区 ——
    await page.goto('/dpr/confirm/review', { waitUntil: 'networkidle' })
    const row = page.locator('.el-table__row').filter({ hasText: applyNo })
    await expect(row.first()).toBeVisible({ timeout: 10000 })
    await row.first().getByRole('button', { name: '详情' }).click()
    const drawer = page.locator('.el-drawer').filter({ hasText: '审核详情' })
    await expect(drawer).toBeVisible()

    // 逐表凭证区:标题(4 槽位 = B/C/F 来源 + H 关联)+ 各表专属附件名
    await expect(drawer.getByText(/逐表凭证 · 表2（4）/)).toBeVisible({ timeout: 8000 })
    const credTable = drawer.locator('.el-table').filter({ hasText: '凭证槽位' })
    await expect(credTable.locator('.el-table__row')).toHaveCount(4)
    await expect(credTable.getByText('公共采集情况说明.pdf')).toBeVisible()
    await expect(credTable.getByText('公共数据授权说明.pdf')).toBeVisible()
    await expect(credTable.getByText('其他来源情况说明.pdf')).toBeVisible()
    await expect(credTable.getByText('用户入网协议(个人信息授权).pdf')).toBeVisible()

    expect(errs, `运行时错误:\n${errs.join('\n')}`).toEqual([])
  } finally {
    // 清理(best-effort)。诚实说明:提交后申请按流程设计不可删(delete 仅限草稿;withdraw→已撤回仍非草稿,
    // 且 withdraw 校验申请人本人)——种子会留一条「人工预审中」,已在确权说明标注 E2E,可作审核台演示数据,重启容器即清。
    await request.post(`${B}/apply/${applyId}/withdraw?reason=e2e-cleanup`).catch(() => {})
    await request.delete(`${B}/apply/${applyId}`).catch(() => {})
  }
})
