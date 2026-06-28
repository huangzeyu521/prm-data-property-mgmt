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

// 菜单可见性分组:细粒度审批角色继承 review 级页面访问(审批/待办/合规);主管/业务额外继承 apply 级(制卡/确权目录)。
// 精细的"谁能审批哪个节点"由后端 assertNodeRole/@RequiresRole 强制,菜单仅控页面入口。
const MENU_GROUP = {
  precheck: ['review'],
  director: ['review'],
  gm: ['review'],
  leadership: ['review'],
  manager: ['review', 'apply'],
  business: ['review', 'apply'],
}

// 菜单配置:数据驱动侧栏。每项 roles=[] 表示对所有角色可见;否则仅列出的角色(+all)可见。
export const MENU = [
  // 指引中心(三合一一级入口):全员可查阅工作指引/流程表单/确权·授权指引,admin 在此维护。取代原顶栏抽屉+两套分散管理页。
  { top: true, path: '/dpr/guidance', title: '指引中心', icon: 'Reading' },
  { top: true, path: '/dpr/workbench/my', title: '我的申请', icon: 'Tickets', roles: ['apply'] },
  { top: true, path: '/dpr/workbench/todo', title: '统一待办中心', icon: 'Compass', roles: ['review'] },
  {
    group: '产权信息管理', icon: 'Document', index: '01', items: [
      { path: '/dpr/ledger/overview', title: '产权台账概览', roles: ['view', 'admin', 'apply'] },
      { path: '/dpr/ledger/archive', title: '数据集产权档案管理', roles: ['admin', 'apply'] },
      { path: '/dpr/ledger/change', title: '产权变更记录管理', roles: ['admin', 'view'] },
      { path: '/dpr/ledger/statistics', title: '产权台账统计分析', roles: ['view', 'admin'] },
    ],
  },
  {
    group: '权益动态监测', icon: 'Monitor', index: '02', items: [
      { path: '/dpr/monitor/status', title: '权益状态监控', roles: ['view', 'admin'] },
      { path: '/dpr/monitor/alert', title: '权益变动监测预警', roles: ['review', 'admin', 'view'] },
      { path: '/dpr/monitor/compliance', title: '合规性检查', roles: ['review', 'admin'] },
      { path: '/dpr/monitor/rule', title: '监测规则配置', roles: ['admin'] },
    ],
  },
  {
    group: '数据确权管理', icon: 'Stamp', index: '03', items: [
      { path: '/dpr/confirm/wizard', title: '⭐ 初始确权申请', roles: ['apply'] },
      { path: '/dpr/confirm/change', title: '确权变更申请', roles: ['apply'] },
      { path: '/dpr/confirm/catalog', title: '确权目录管理', roles: ['apply', 'admin'] },
      { path: '/dpr/confirm/history', title: '申请查询', roles: ['apply', 'review', 'view'] },
      { path: '/dpr/confirm/review', title: '审核申请提交', roles: ['review'] },
      { path: '/dpr/confirm/card', title: '权益卡片生成', roles: ['admin', 'apply'] },
      { path: '/dpr/confirm/cert', title: '权益证书管理', roles: ['admin'] },
    ],
  },
  {
    group: '数据授权管理', icon: 'Connection', index: '04', items: [
      { path: '/dpr/auth/wizard', title: '⭐ 一事一议授权申请', roles: ['apply'] },
      { path: '/dpr/auth/batch-wizard', title: '⭐ 批量授权申请', roles: ['apply'] },
      { path: '/dpr/auth/batch-list', title: '批量授权清单', roles: ['apply', 'review'] },
      { path: '/dpr/auth/compliance', title: '合规校验管理', roles: ['review', 'admin'] },
      { path: '/dpr/auth/history', title: '申请历史查询', roles: ['apply', 'review', 'view'] },
      { path: '/dpr/auth/review', title: '授权审核提交', roles: ['review'] },
      // 协议工作台(P1-4 签章/审核/存档合一);旧三路由保留兼容
      { path: '/dpr/auth/agreement', title: '协议工作台', roles: ['review', 'apply'] },
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
    group: '综合分析管理', icon: 'DataAnalysis', index: '05', items: [
      { path: '/dpr/dashboard/overview', title: '数据产权全景', roles: ['view', 'review', 'admin'] },
      { path: '/dpr/dashboard/confirm', title: '确权看板', roles: ['view', 'review'] },
      { path: '/dpr/dashboard/auth', title: '授权看板', roles: ['view', 'review'] },
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
  review: '/dpr/workbench/todo',
  admin: '/dpr/guidance',
  view: '/dpr/dashboard/overview',
}

export function currentRole() {
  return localStorage.getItem('prm-role') || 'all'
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
