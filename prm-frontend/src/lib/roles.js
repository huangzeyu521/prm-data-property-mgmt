// 轻量角色模型(无登录;由顶栏切换器或生产 4A 角色映射写入 localStorage 'prm-role')。
// 第一性:业务有7类用户,系统原本只有1类无差别超级用户 → 按角色裁剪菜单+个性化首屏,补"相关性过滤"。

export const ROLES = [
  { key: 'all', label: '全部 · 管理员视图' },
  { key: 'apply', label: '申报人' },
  { key: 'review', label: '审核 / 审批' },
  { key: 'admin', label: '配置管理员' },
  { key: 'view', label: '管理层 · 只读' },
]

// 菜单配置:数据驱动侧栏。每项 roles=[] 表示对所有角色可见;否则仅列出的角色(+all)可见。
export const MENU = [
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
      { path: '/dpr/confirm/wizard', title: '⭐ 确权申请', roles: ['apply'] },
      { path: '/dpr/confirm/catalog', title: '确权目录管理', roles: ['apply', 'admin'] },
      { path: '/dpr/confirm/history', title: '申请查询', roles: ['apply', 'review', 'view'] },
      { path: '/dpr/confirm/review', title: '审核申请提交', roles: ['review'] },
      { path: '/dpr/confirm/card', title: '权益卡片生成', roles: ['admin', 'apply'] },
      { path: '/dpr/confirm/cert', title: '权益证书管理', roles: ['admin'] },
      { path: '/dpr/confirm/guidance', title: '确权指引管理', roles: ['admin'] },
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
      { path: '/dpr/auth/guidance', title: '授权指引管理', roles: ['admin'] },
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
  admin: '/dpr/auth/guidance',
  view: '/dpr/dashboard/overview',
}

export function currentRole() {
  return localStorage.getItem('prm-role') || 'all'
}

function itemVisible(item, role) {
  return role === 'all' || !item.roles || item.roles.length === 0 || item.roles.includes(role)
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
