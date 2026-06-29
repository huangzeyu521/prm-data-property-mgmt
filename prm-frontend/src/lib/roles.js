// 轻量角色模型(无登录;由顶栏切换器或生产 4A 角色映射写入 localStorage 'prm-role')。
// 第一性:业务有7类用户,系统原本只有1类无差别超级用户 → 按角色裁剪菜单+个性化首屏,补"相关性过滤"。

// 角色一一对齐架构(AA-10 角色功能矩阵 / BA-05 业务角色清单 / 工作指引流程节点)。
export const ROLES = [
  { key: 'all', label: '全部 · 管理员视图' },
  { key: 'apply', label: '申报人 · 数字化部团队' },
  { key: 'business', label: '业务管理部门团队' },
  { key: 'precheck', label: '归集预审 · 数字化部团队' },
  { key: 'review', label: '合规管控小组' },
  { key: 'manager', label: '数字化部主管' },
  { key: 'director', label: '经理 / 高级经理' },
  { key: 'gm', label: '副总经理 / 总经理' },
  { key: 'leadership', label: '领导小组办公室' },
  { key: 'admin', label: '配置管理员' },
  { key: 'view', label: '管理层 · 只读' },
]

// 可见域精确化(2026-06-28 按 AA-10 角色功能矩阵 + BA-03 逐节点责任人落地):
// 取消"细粒度审批角色一律继承 review 全量菜单"的粗放继承(它把确权审核台/合规越界暴露给副总/领导小组),
// 改为每个菜单项显式列出 AA-10 允许的角色 → 可见域 == 矩阵。MENU_GROUP 置空(保留机制以备特例)。
// 精细的"谁能审批哪个节点"仍由后端 assertNodeRole/@RequiresRole 强制,菜单只控页面入口。
const MENU_GROUP = {}

// AA-10 功能项 × 角色(应有访问):
//   产权信息管理: 副总gm/经理director/主管manager/合规review (+管理员/只读;申报人apply 作为数据owner保留台账·档案)
//   数据授权管理: 全部业务角色(apply/business/manager/director/gm/leadership/review)
//   数据确权管理: 申报apply/合规review/主管manager/经理director(+归集预审precheck);副总gm/领导小组leadership 不涉确权
//   数据产权分析: 副总gm/经理director/主管manager(+只读/管理员);合规小组/申报人 不在分析矩阵内
export const MENU = [
  { top: true, path: '/dpr/guidance', title: '指引中心', icon: 'Reading' },
  { top: true, path: '/dpr/workbench/my', title: '我的申请', icon: 'Tickets', roles: ['apply', 'business'] },
  // 待办中心:所有承担审批/审核/归集节点的角色(BA-03)
  { top: true, path: '/dpr/workbench/todo', title: '统一待办中心', icon: 'Compass', roles: ['review', 'business', 'precheck', 'manager', 'director', 'gm', 'leadership'] },
  {
    // P0:按 AA-10 给 副总gm/经理director/主管manager/合规review 补「产权信息管理」访问(原仅 view/admin/apply)
    group: '产权信息管理', icon: 'Document', index: '01', items: [
      { path: '/dpr/ledger/overview', title: '产权台账概览', roles: ['view', 'admin', 'apply', 'manager', 'director', 'gm', 'review'] },
      { path: '/dpr/ledger/archive', title: '数据集产权档案管理', roles: ['admin', 'apply', 'manager', 'director', 'gm', 'review'] },
      { path: '/dpr/ledger/change', title: '产权变更记录管理', roles: ['admin', 'view', 'manager', 'director', 'gm', 'review'] },
      { path: '/dpr/ledger/statistics', title: '产权台账统计分析', roles: ['view', 'admin', 'manager', 'director', 'gm', 'review'] },
    ],
  },
  {
    group: '权益动态监测', icon: 'Monitor', index: '02', items: [
      { path: '/dpr/monitor/status', title: '权益状态监控', roles: ['view', 'admin', 'review'] },
      { path: '/dpr/monitor/alert', title: '权益变动监测预警', roles: ['review', 'admin', 'view'] },
      { path: '/dpr/monitor/compliance', title: '合规性检查', roles: ['review', 'admin'] },
      { path: '/dpr/monitor/rule', title: '监测规则配置', roles: ['admin'] },
    ],
  },
  {
    // 确权:申报(apply)+审核台(合规review/主管manager/经理director/归集预审precheck);副总gm/领导小组leadership 不涉确权
    group: '数据确权管理', icon: 'Stamp', index: '03', items: [
      { path: '/dpr/confirm/wizard', title: '⭐ 初始确权申请', roles: ['apply'] },
      { path: '/dpr/confirm/change', title: '确权变更申请', roles: ['apply'] },
      { path: '/dpr/confirm/catalog', title: '确权目录管理', roles: ['apply', 'admin'] },
      { path: '/dpr/confirm/history', title: '申请查询', roles: ['apply', 'review', 'view', 'manager', 'director', 'precheck'] },
      { path: '/dpr/confirm/review', title: '审核申请提交', roles: ['review', 'precheck', 'manager', 'director'] },
      { path: '/dpr/confirm/card', title: '权益卡片生成', roles: ['admin', 'apply', 'manager'] },
      { path: '/dpr/confirm/cert', title: '权益证书管理', roles: ['admin', 'manager'] },
    ],
  },
  {
    // 授权:申报(apply)+审核台(合规review/业务business/主管manager/经理director/副总gm)+批量(含领导小组leadership)
    group: '数据授权管理', icon: 'Connection', index: '04', items: [
      { path: '/dpr/auth/wizard', title: '⭐ 一事一议授权申请', roles: ['apply'] },
      { path: '/dpr/auth/batch-wizard', title: '⭐ 批量授权申请', roles: ['apply'] },
      { path: '/dpr/auth/batch-list', title: '批量授权清单', roles: ['apply', 'review', 'manager', 'director', 'gm', 'leadership'] },
      { path: '/dpr/auth/compliance', title: '合规校验管理', roles: ['review', 'admin'] },
      { path: '/dpr/auth/history', title: '申请历史查询', roles: ['apply', 'review', 'view', 'manager', 'director', 'gm'] },
      { path: '/dpr/auth/review', title: '授权审核提交', roles: ['review', 'business', 'manager', 'director', 'gm'] },
      // 协议工作台(P1-4 签章/审核/存档合一);旧三路由保留兼容
      { path: '/dpr/auth/agreement', title: '协议工作台', roles: ['review', 'apply', 'gm'] },
      { path: '/dpr/auth/cert', title: '授权权益管理', roles: ['admin'] },
      { path: '/dpr/auth/filing', title: '对外经营权授权备案', roles: ['admin', 'apply'] },
    ],
  },
  {
    // P2-5:配置类(低频、管理员专属)单列二级区,与日常操作分层
    group: '授权配置', icon: 'Setting', index: '07', items: [
      { path: '/dpr/auth/form-template', title: '申请表单设计', roles: ['admin'] },
      { path: '/dpr/auth/scenario', title: '应用场景管理', roles: ['admin'] },
      { path: '/dpr/auth/agreement-template', title: '协议模板库', roles: ['admin'] },
      { path: '/dpr/auth/cert-template', title: '授权权益证书模板管理', roles: ['admin'] },
    ],
  },
  {
    // P1:数据产权分析按 AA-10 = 副总gm/经理director/主管manager(+只读/管理员);合规小组与申报人不在分析矩阵内
    group: '综合分析管理', icon: 'DataAnalysis', index: '05', items: [
      { path: '/dpr/dashboard/overview', title: '数据产权全景', roles: ['view', 'admin', 'gm', 'director', 'manager'] },
      { path: '/dpr/dashboard/confirm', title: '确权看板', roles: ['view', 'admin', 'gm', 'director', 'manager'] },
      { path: '/dpr/dashboard/auth', title: '授权看板', roles: ['view', 'admin', 'gm', 'director', 'manager'] },
    ],
  },
  {
    // 系统管理(管理员专属):用户/角色/操作日志
    group: '系统管理', icon: 'Setting', index: '08', items: [
      { path: '/dpr/system/user', title: '用户管理', roles: ['admin'] },
      { path: '/dpr/system/role', title: '角色管理', roles: ['admin'] },
      { path: '/dpr/system/oplog', title: '操作日志', roles: ['admin'] },
    ],
  },
]

// 个性化首屏:每角色登录/切换后落到最相关的页面
export const ROLE_HOME = {
  all: '/dpr/dashboard/overview',
  apply: '/dpr/workbench/my',
  business: '/dpr/workbench/my',
  review: '/dpr/workbench/todo',
  precheck: '/dpr/workbench/todo',
  manager: '/dpr/workbench/todo',
  director: '/dpr/workbench/todo',
  gm: '/dpr/workbench/todo',
  leadership: '/dpr/workbench/todo',
  admin: '/dpr/guidance',
  view: '/dpr/dashboard/overview',
}

export function currentRole() {
  return localStorage.getItem('prm-role') || 'all'
}

// 审批节点(状态名)→ 处理角色:镜像后端 AuthApplyServiceImpl.NODE_ROLE / ConfirmApplyServiceImpl.NODE_ROLE。
// 用途:把「授权审核台 / 统一待办中心」收敛到「本角色·本节点」,让每个审批人只见自己队列。
// 后端 assertNodeRole 仍是硬门禁(越节点审批 403);此处是呈现层对齐,杜绝 gm 误见确权待办 / 误点非本节点单据被 403。
export const AUTH_NODE_ROLE = {
  合规审核中: 'review',
  业务审核中: 'business',
  主管审核中: 'manager',
  经理审核中: 'director',
  副总审批中: 'gm',
  领导小组审批中: 'leadership',
}
export const CONFIRM_NODE_ROLE = {
  人工预审中: 'precheck',
  合规审核中: 'review',
  主管复核中: 'manager',
  经理终审中: 'director',
}

/**
 * 当前角色在某审批域可处理的节点状态集合。
 * 返回 null = 不过滤(admin/all/超级视角看全部队列);返回数组 = 仅这些状态归本角色办理(可能为空数组=本域无本角色节点)。
 */
export function handledStatuses(role, nodeRole) {
  if (!role || role === 'all' || role === 'admin') return null
  return Object.entries(nodeRole).filter(([, r]) => r === role).map(([s]) => s)
}

function itemVisible(item, role) {
  if (role === 'all' || !item.roles || item.roles.length === 0) return true
  const eff = [role, ...(MENU_GROUP[role] || [])]
  return item.roles.some((r) => eff.includes(r))
}

/** 按角色裁剪菜单:保留可见顶层项与非空分组 */
export function visibleMenu(role) {
  const out = []
  for (const node of MENU) {
    if (node.top) {
      if (itemVisible(node, role)) out.push(node)
    } else {
      const items = node.items.filter((it) => itemVisible(it, role))
      if (items.length) out.push({ ...node, items })
    }
  }
  return out
}
