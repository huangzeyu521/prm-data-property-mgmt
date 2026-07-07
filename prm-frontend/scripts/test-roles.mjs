// 角色菜单可见域守卫测试(对齐 AA-10 角色功能矩阵 + BA-03 节点责任人)。
// 纯函数 visibleMenu() 断言;无需浏览器/框架。运行:node scripts/test-roles.mjs
import { visibleMenu, handledStatuses, AUTH_NODE_ROLE, CONFIRM_NODE_ROLE } from '../src/lib/roles.js'

// 展平某角色可见的全部路径(顶层项 + 各分组子项)
function pathsOf(role) {
  const out = new Set()
  for (const node of visibleMenu(role)) {
    if (node.path) out.add(node.path)
    for (const it of node.items || []) out.add(it.path)
  }
  return out
}

let fails = 0
function check(role, { has = [], hasnt = [] }) {
  const p = pathsOf(role)
  for (const x of has) if (!p.has(x)) { console.error(`✗ [${role}] 应可见但缺失: ${x}`); fails++ }
  for (const x of hasnt) if (p.has(x)) { console.error(`✗ [${role}] 不应可见却出现: ${x}`); fails++ }
}

// P0:副总gm/经理director/主管manager/合规review 应能访问「产权信息管理」
const LEDGER = '/dpr/ledger/overview', ARCHIVE = '/dpr/ledger/archive'
const CONFIRM_REVIEW = '/dpr/confirm/review', AUTH_REVIEW = '/dpr/auth/review'
const DASH_AUTH = '/dpr/dashboard/auth', SYS = '/dpr/system/user'

// 副总/总经理:授权 + 产权信息 + 分析;不涉确权(审核台),非管理员
check('gm', {
  has: [LEDGER, ARCHIVE, AUTH_REVIEW, '/dpr/auth/batch-list', DASH_AUTH, '/dpr/ledger/statistics'],
  hasnt: [CONFIRM_REVIEW, '/dpr/confirm/wizard', SYS, '/dpr/auth/scenario'],
})
// 经理/高级经理:确权 + 授权 + 产权信息 + 分析(全四项)
check('director', {
  has: [LEDGER, CONFIRM_REVIEW, AUTH_REVIEW, DASH_AUTH],
  hasnt: [SYS, '/dpr/confirm/wizard'],
})
// 主管:全四项
check('manager', {
  has: [LEDGER, ARCHIVE, CONFIRM_REVIEW, AUTH_REVIEW, DASH_AUTH],
  hasnt: [SYS],
})
// 合规管控小组:确权 + 授权 + 产权信息(P0);AA-10 不含分析 → 看板不可见(P1)
check('review', {
  has: [LEDGER, CONFIRM_REVIEW, AUTH_REVIEW, '/dpr/monitor/compliance'],
  hasnt: [DASH_AUTH, SYS, '/dpr/confirm/wizard'],
})
// 申报人:确权/授权申报 + 数据owner台账;不涉审核台/分析/系统管理
check('apply', {
  has: ['/dpr/confirm/wizard', '/dpr/auth/wizard', LEDGER, '/dpr/workbench/my'],
  hasnt: [CONFIRM_REVIEW, AUTH_REVIEW, DASH_AUTH, SYS],
})
// 领导小组办公室:仅授权(批量末节点);不涉确权/产权信息/系统管理
check('leadership', {
  has: ['/dpr/auth/batch-list', '/dpr/workbench/todo'],
  hasnt: [CONFIRM_REVIEW, LEDGER, SYS, DASH_AUTH],
})
// 业务管理部门:一事一议发起(35号文 一事一议步骤10 发起人=业务管理部门团队)+ 授权审核台;不涉确权申报/系统管理
check('business', {
  has: ['/dpr/auth/wizard', AUTH_REVIEW, '/dpr/workbench/todo'],
  hasnt: [SYS, '/dpr/confirm/wizard'],
})
// 申报单位分管领导(unit):授权单位初审(审核台/待办/历史);不涉确权审核/分析/系统管理
check('unit', {
  has: [AUTH_REVIEW, '/dpr/workbench/todo', '/dpr/auth/history'],
  hasnt: [CONFIRM_REVIEW, DASH_AUTH, SYS],
})
// 管理员视图(all):全可见
check('all', { has: [SYS, LEDGER, CONFIRM_REVIEW, AUTH_REVIEW, DASH_AUTH] })

// 审批节点收敛守卫:审核台/待办按「本角色·本节点」裁剪(镜像后端 NODE_ROLE),杜绝越节点误点 403。
function setEq(actual, expected, msg) {
  const a = new Set(actual), e = new Set(expected)
  const ok = a.size === e.size && [...e].every(x => a.has(x))
  if (!ok) { console.error(`✗ ${msg}: 期望[${expected.join(',')}] 实得[${(actual||[]).join(',')}]`); fails++ }
}
// 副总/总经理:授权仅「副总审批中」;不涉确权(确权节点集为空 → 待办确权 tab 隐藏)
setEq(handledStatuses('gm', AUTH_NODE_ROLE), ['副总审批中'], 'gm 授权可办节点')
setEq(handledStatuses('gm', CONFIRM_NODE_ROLE), [], 'gm 确权可办节点(应为空,gm 不涉确权)')
// 合规/业务/主管/经理/领导小组:各自唯一授权节点
setEq(handledStatuses('unit', AUTH_NODE_ROLE), ['单位初审中'], 'unit 授权可办节点(表2 20-50 单位初审)')
setEq(handledStatuses('unit', CONFIRM_NODE_ROLE), [], 'unit 确权可办节点(应为空)')
setEq(handledStatuses('review', AUTH_NODE_ROLE), ['合规审核中'], 'review 授权可办节点')
setEq(handledStatuses('business', AUTH_NODE_ROLE), ['业务审核中'], 'business 授权可办节点')
setEq(handledStatuses('manager', AUTH_NODE_ROLE), ['主管审核中'], 'manager 授权可办节点')
setEq(handledStatuses('director', AUTH_NODE_ROLE), ['经理审核中'], 'director 授权可办节点')
setEq(handledStatuses('leadership', AUTH_NODE_ROLE), ['领导小组审批中'], 'leadership 授权可办节点')
// 确权侧:预审/合规/主管/经理各自节点
setEq(handledStatuses('precheck', CONFIRM_NODE_ROLE), ['人工预审中'], 'precheck 确权可办节点')
setEq(handledStatuses('manager', CONFIRM_NODE_ROLE), ['主管复核中'], 'manager 确权可办节点')
setEq(handledStatuses('director', CONFIRM_NODE_ROLE), ['经理终审中'], 'director 确权可办节点')
// 管理员/超级视角:不过滤(null = 看全部队列)
if (handledStatuses('admin', AUTH_NODE_ROLE) !== null) { console.error('✗ admin 应不过滤审批队列(null)'); fails++ }
if (handledStatuses('all', CONFIRM_NODE_ROLE) !== null) { console.error('✗ all 应不过滤审批队列(null)'); fails++ }

if (fails) {
  console.error(`\n角色守卫测试失败:${fails} 处不符 AA-10`)
  process.exit(1)
}
console.log('✓ 角色菜单可见域 + 审批节点收敛守卫测试通过(gm/director/review 产权信息已补;审核台/待办按本角色本节点裁剪)')
