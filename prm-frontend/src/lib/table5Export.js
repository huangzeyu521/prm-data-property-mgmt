// 《表5 数据授权申请单》系统生成(对齐 办数字〔2025〕35号 附录C 表5,16 列)。
// 由批量授权"已加入明细 + 清单头"自动生成,消除"用户重填重传表5"的冗余;输出 Excel 可打开的 .xls(HTML 表)。
// 字段映射:申请主体/主管/联系方式取清单头(批量共享);数据表/系统/模式/业务域/权益/场景/第三方/隐私取明细(确权带出+清单头沿用)。

const COLS = [
  '序号', '申请主体名称', '模式名称', '数据表名称', '所属系统', '所属业务域',
  '申请权益类型', '使用场景及目的摘要', '权益时效', '是否跨区域、跨域',
  '涉及第三方来源方式', '第三方许可凭证或说明', '涉及个人隐私/商业秘密',
  '信息授权协议', '申请单位主管', '联系方式'
]

const esc = (v) => String(v == null ? '' : v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

function rowsOf(listForm, items, crossDomain) {
  return (items || []).map((it, i) => {
    const third = it.thirdPartySource && String(it.thirdPartySource).trim() ? String(it.thirdPartySource).trim() : '不涉及'
    const sens = it.sensitiveType && String(it.sensitiveType).trim() && it.sensitiveType !== '无' ? it.sensitiveType : '不涉及'
    return [
      i + 1,
      listForm.granteeOrg || '',
      it.schemaName || '',
      it.assetName || '',
      it.systemName || '',
      it.businessDomain || '',
      it.rightType || '',
      // 使用场景及目的摘要(表5):场景 + 目的摘要(一事一议 purposeNote;批量无则仅场景)
      ((it.scenario || listForm.scenario || '') + ((it.purposeNote || listForm.purposeNote) ? '：' + (it.purposeNote || listForm.purposeNote) : '')),
      it.validTerm || listForm.validTerm || '两年',
      // 跨区域、跨域:清单级跨系统域 或 行级跨地域(被授权方省≠数据归属主体省)任一命中即"是"
      (crossDomain || it.crossGeo) ? '是' : '否',
      third,
      third === '不涉及' ? '—' : '见随附第三方许可凭证',
      sens,
      sens === '不涉及' ? '—' : '见随附信息授权协议',
      listForm.contactPerson || '',
      listForm.contactInfo || ''
    ]
  })
}

function buildHtml(listForm, items, crossDomain, opts = {}) {
  const rows = rowsOf(listForm, items, crossDomain)
  const title = opts.title || `表5 数据授权申请单（${listForm.listYear || ''}年度批量授权·被授权方：${esc(listForm.granteeOrg || '')}）`
  const th = COLS.map((c) => `<th style="border:1px solid #888;background:#f0f4ff;padding:4px;font-weight:700">${esc(c)}</th>`).join('')
  const trs = rows.map((r) =>
    '<tr>' + r.map((c) => `<td style="border:1px solid #888;padding:4px">${esc(c)}</td>`).join('') + '</tr>'
  ).join('')
  return `<html><head><meta charset="utf-8"></head><body>
<h3>${esc(title)}</h3>
<table style="border-collapse:collapse;font-size:12px"><thead><tr>${th}</tr></thead><tbody>${trs}</tbody></table>
<p style="font-size:12px;color:#666">注:本表由数据资产管理平台按已加入授权明细自动生成,对齐《数据确权授权工作指引》附录C 表5;第三方/隐私事实由确权信息带出。</p>
</body></html>`
}

/** 生成并下载《表5》(.xls，Excel 可打开)。opts:{title,fileName} 供一事一议/批量复用同一导出器。 */
export function downloadTable5(listForm, items, crossDomain, opts = {}) {
  const html = buildHtml(listForm, items, crossDomain, opts)
  const blob = new Blob(['﻿' + html], { type: 'application/vnd.ms-excel;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = opts.fileName || `表5_数据授权申请单_${listForm.listYear || ''}_${(listForm.granteeOrg || '批量').slice(0, 12)}.xls`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
