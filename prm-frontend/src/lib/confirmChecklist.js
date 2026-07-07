// 确权应交材料·规则过滤(单一真源):镜像后端 ConfirmMaterialRuleService。
// 供「确权向导 buildChecklist」与「申请草稿箱·材料缺口计算」共用,杜绝两处逻辑漂移(DRY)。
// 触发判定:ALWAYS 常交 / TABLE2 涉三方 / SOURCE·RELATION 选中码;B–F(非A)/G–J 已逐表化,系统级清单剔除。

// 系统级清单只含 表1/表2/权属凭证 + A 自行生产说明;B–F(非A)来源、G–J 关联凭证逐表承载(step2 逐表凭证区)
export function isPerTableRule(r) {
  return r.triggerType === 'RELATION' || (r.triggerType === 'SOURCE' && r.triggerCode !== 'A')
}

// 确权变更应交材料收敛(镜像后端 narrowForChange):ALWAYS 始终保留;来源类仅触发涉来源、关联类仅触发涉管理要求时保留;
// 表2仅当仍保留来源/关联差异材料时保留;未识别触发→保守不收敛返回全集。
export function narrowForChange(rules, trigger) {
  const known = ['来源', '新增', '管理', '监管', '到期'].some((k) => trigger.includes(k)) || trigger === '其他'
  if (!known) return rules
  const keepSrc = trigger.includes('来源') || trigger.includes('新增') || trigger === '其他'
  const keepRel = trigger.includes('管理') || trigger.includes('监管') || trigger === '其他'
  const out = []; let anyDiff = false
  for (const r of rules) {
    if (r.triggerType === 'ALWAYS') out.push(r)
    else if (r.triggerType === 'SOURCE') { if (keepSrc) { out.push(r); anyDiff = true } }
    else if (r.triggerType === 'RELATION') { if (keepRel) { out.push(r); anyDiff = true } }
  }
  if (anyDiff) out.push(...rules.filter((r) => r.triggerType === 'TABLE2'))
  return out
}

/**
 * 按场景×触发过滤出系统级应交规则。
 * @param {Array} rules 后端可配置规则(materialRules)
 * @param {{sourceIdent?:string[], relationIdent?:string[], needTable2?:boolean, registerType?:string, changeTrigger?:string}} ctx
 * @returns {Array} 命中的应交规则
 */
export function filterRequiredRules(rules, ctx = {}) {
  const src = ctx.sourceIdent || []
  const rel = ctx.relationIdent || []
  const hit = (r) => {
    if (r.triggerType === 'ALWAYS') return true
    if (r.triggerType === 'TABLE2') return !!ctx.needTable2
    if (r.triggerType === 'SOURCE') return src.includes(r.triggerCode)
    if (r.triggerType === 'RELATION') return rel.includes(r.triggerCode)
    return false
  }
  let out = rules.filter(hit).filter((r) => !isPerTableRule(r))
  if (ctx.registerType === '确权变更' && ctx.changeTrigger) out = narrowForChange(out, ctx.changeTrigger)
  return out
}
