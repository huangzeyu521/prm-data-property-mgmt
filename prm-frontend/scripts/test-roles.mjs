// 角色菜单可见域守卫测试(对齐 AA-10 角色功能矩阵 + BA-03 节点责任人)。
// 纯函数 visibleMenu() 断言;无需浏览器/框架。运行:node scripts/test-roles.mjs
import { visibleMenu } from '../src/lib/roles.js'

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
// 业务管理部门:授权审核台;不涉系统管理
check('business', {
  has: [AUTH_REVIEW, '/dpr/workbench/todo'],
  hasnt: [SYS],
})
// 管理员视图(all):全可见
check('all', { has: [SYS, LEDGER, CONFIRM_REVIEW, AUTH_REVIEW, DASH_AUTH] })

if (fails) {
  console.error(`\n角色守卫测试失败:${fails} 处不符 AA-10`)
  process.exit(1)
}
console.log('✓ 角色菜单可见域守卫测试通过(对齐 AA-10:gm/director/review 产权信息已补,确权审核台/分析可见域已精确化)')
