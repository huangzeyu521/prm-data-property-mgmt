import { createRouter, createWebHistory } from 'vue-router'

// 根路径落点=当前角色首屏(ROLE_HOME):申报人落「我的申请」、审批角色落「统一待办」。
// 静态 redirect 到 dashboard/overview 会把 apply 等角色带出自己的菜单可见域(综合分析组不含申报人)。
import { ROLE_HOME, currentRole } from '@/lib/roles'
const roleHome = () => ROLE_HOME[currentRole()] || '/dpr/dashboard/overview'

const routes = [
  { path: '/', redirect: roleHome },
  { path: '/dpr', redirect: roleHome },
  // 统一待办中心(eLink 待办的 Web 实现)
  { path: '/dpr/workbench/todo', name: 'TodoCenter', component: () => import('@/views/workbench/TodoCenter.vue'), meta: { title: '统一待办中心', goal: '集中查看并处理确权/授权全流程待办与预警,一处办结' } },
  // 我的申请(申报人首屏:在途确权+授权一处看)
  { path: '/dpr/workbench/my', name: 'MyApplications', component: () => import('@/views/workbench/MyApplications.vue'), meta: { title: '我的申请', goal: '一处掌握我提交的确权/授权申请进度,驳回单可修改重提' } },
  // 协议工作台(P1-4:签章/审核/存档合一)
  { path: '/dpr/auth/agreement', name: 'AgreementWorkbench', component: () => import('@/views/authorize/AgreementWorkbench.vue'), meta: { title: '协议工作台', goal: '一份协议从签章到存档一处流转' } },
  {
    path: '/dpr/ledger/overview',
    name: 'LedgerOverview',
    component: () => import('@/views/ledger/LedgerOverview.vue'),
    meta: { title: '产权台账概览', goal: '总览全网数据产权台账与产权树,快速定位资产权属' }
  },
  {
    path: '/dpr/ledger/archive',
    name: 'PropertyArchive',
    component: () => import('@/views/ledger/PropertyArchiveList.vue'),
    meta: { title: '数据集产权档案管理', goal: '只读查询权限可见的数据资产卡片确权/授权信息(卡片来自数据资产管理平台,不在此新增)' }
  },
  { path: '/dpr/ledger/dataset', name: 'DatasetDetail', component: () => import('@/views/ledger/DatasetDetail.vue'), meta: { title: '数据集详情展示', goal: '查看数据集明细与产权要素,核对资产信息' } },
  { path: '/dpr/ledger/change', name: 'ChangeRecord', component: () => import('@/views/ledger/ChangeRecordList.vue'), meta: { title: '产权变更记录管理', goal: '追溯产权变更历史与上链凭证,保障全程可审计' } },
  { path: '/dpr/ledger/statistics', name: 'LedgerStatistics', component: () => import('@/views/ledger/LedgerStatistics.vue'), meta: { title: '产权台账统计分析', goal: '多维统计产权台账(类型/系统部署单位/趋势),支撑管理决策' } },
  { path: '/dpr/monitor/status', name: 'RightsStatusMonitor', component: () => import('@/views/monitor/RightsStatusMonitor.vue'), meta: { title: '权益状态监控', goal: '实时监控权益状态变化,发现异常及时处置' } },
  {
    path: '/dpr/monitor/alert',
    name: 'AlertList',
    component: () => import('@/views/monitor/AlertList.vue'),
    meta: { title: '权益变动监测预警', goal: '查看权益变动预警,跟进处置闭环' }
  },
  { path: '/dpr/monitor/compliance', name: 'ComplianceCheck', component: () => import('@/views/monitor/ComplianceCheck.vue'), meta: { title: '合规性检查', goal: '多维合规检查并生成报告,识别越权/超期等违规风险' } },
  {
    path: '/dpr/monitor/rule',
    name: 'MonitorRule',
    component: () => import('@/views/monitor/MonitorRuleList.vue'),
    meta: { title: '监测规则配置', goal: '配置监测规则与通知方式,定制监控策略' }
  },
  { path: '/dpr/monitor/notification', name: 'RiskNotification', component: () => import('@/views/monitor/RiskNotification.vue'), meta: { title: '风险预警通知', goal: '管理风险预警通知与定向推送' } },
  // 指引中心(三合一:工作指引/流程表单/确权指引/授权指引,取代顶栏抽屉+两套分散管理页)
  { path: '/dpr/guidance', name: 'GuidanceCenter', component: () => import('@/views/guidance/GuidanceCenter.vue'), meta: { title: '指引中心', goal: '一处查阅工作指引/流程表单与确权·授权指引材料,管理员在此统一维护' } },
  // F-02 数据确权管理
  { path: '/dpr/confirm/wizard', name: 'ConfirmWizard', component: () => import('@/views/confirm/ConfirmWizard.vue'), meta: { title: '初始确权申请(一站式)', goal: '系统级初始确权:左树选一个系统(整系统纳入,不下钻库表)→ 表1+A–J → 材料 → 校验 → 提交', mode: 'initial' } },
  // 确权变更与初始确权分菜单(对齐附录F §3.3.2 重新确权):载入已确权系统、聚焦变更维度
  { path: '/dpr/confirm/change', name: 'ConfirmChange', component: () => import('@/views/confirm/ConfirmWizard.vue'), meta: { title: '确权变更申请', goal: '对已确权系统的修订:选触发动因 → 基线对照 → 只改变动维度 → 提交', mode: 'change' } },
  // 季度重确权工单池(35号文§二(三)2 闭环:到期扫描/监测联动/变更生效联动 → 派生变更或复核无变化销号)
  // 确权指引管理已并入「指引中心」,旧路径重定向(保旧书签/深链)
  { path: '/dpr/confirm/guidance', redirect: '/dpr/guidance?tab=confirm' },
  { path: '/dpr/confirm/catalog', name: 'ConfirmCatalog', component: () => import('@/views/confirm/ConfirmCatalog.vue'), meta: { title: '数据资产确权目录管理', goal: '管理数据资产确权目录结构,明确应确权范围' } },
  { path: '/dpr/confirm/history', name: 'ConfirmHistory', component: () => import('@/views/confirm/ConfirmHistory.vue'), meta: { title: '确权申请查询(进度/历史)', goal: '查询确权申请审批进度与历史记录' } },
  { path: '/dpr/confirm/review', name: 'ConfirmReviewDesk', component: () => import('@/views/confirm/ConfirmReviewDesk.vue'), meta: { title: '审核申请提交管理', goal: '受理并审核确权申请,出具认定意见(表3/表4)' } },
  { path: '/dpr/confirm/card', name: 'EquityCard', component: () => import('@/views/confirm/EquityCardList.vue'), meta: { title: '权益卡片生成管理', goal: '确权通过后生成与管理权益卡片(先确后授前提)' } },
  { path: '/dpr/confirm/cert', name: 'EquityCert', component: () => import('@/views/confirm/EquityCertList.vue'), meta: { title: '权益证书管理', goal: '签发与管理确权权益证书' } },
  // F-03 数据授权管理
  { path: '/dpr/auth/wizard', name: 'AuthWizard', component: () => import('@/views/authorize/AuthWizard.vue'), meta: { title: '一事一议授权申请(一站式)', goal: '一站式发起一事一议授权申请并跟踪流转' } },
  { path: '/dpr/auth/batch-wizard', name: 'BatchAuthWizard', component: () => import('@/views/authorize/BatchAuthWizard.vue'), meta: { title: '批量授权申请(一站式)', goal: '一站式发起批量授权申请并跟踪流转' } },
  { path: '/dpr/auth/filing', name: 'AuthFiling', component: () => import('@/views/authorize/AuthFiling.vue'), meta: { title: '对外经营权授权备案(附录G)', goal: '办理对外经营权授权备案,留存备案记录' } },
  // 授权指引管理已并入「指引中心」,旧路径重定向(保旧书签/深链)
  { path: '/dpr/auth/guidance', redirect: '/dpr/guidance?tab=auth' },
  { path: '/dpr/auth/form-template', name: 'AuthFormTemplate', component: () => import('@/views/authorize/AuthFormTemplate.vue'), meta: { title: '授权申请表单设计管理', goal: '配置化设计授权申请表单模板', category: 'FORM_TEMPLATE' } },
  { path: '/dpr/auth/scenario', name: 'AuthScenario', component: () => import('@/views/authorize/AuthScenario.vue'), meta: { title: '应用场景管理', goal: '管理授权应用场景与场景准入', category: 'SCENARIO' } },
  { path: '/dpr/auth/batch-list', name: 'AuthBatchList', component: () => import('@/views/authorize/AuthBatchList.vue'), meta: { title: '批量授权清单', goal: '维护批量授权清单与范围' } },
  { path: '/dpr/auth/compliance', name: 'AuthCompliance', component: () => import('@/views/authorize/AuthCompliance.vue'), meta: { title: '合规校验管理', goal: '授权合规校验,确保授权⊆确权边界' } },
  { path: '/dpr/auth/history', name: 'AuthHistory', component: () => import('@/views/authorize/AuthApplyHistory.vue'), meta: { title: '授权申请历史查询管理', goal: '查询授权申请历史与状态' } },
  { path: '/dpr/auth/review', name: 'AuthReviewDesk', component: () => import('@/views/authorize/AuthReviewDesk.vue'), meta: { title: '授权审核申请提交管理', goal: '受理并审核授权申请,逐级审批' } },
  { path: '/dpr/auth/agreement-template', name: 'AgreementTemplate', component: () => import('@/views/authorize/AgreementTemplate.vue'), meta: { title: '协议模板库管理', goal: '维护授权协议模板库(配置化)', category: 'AGREEMENT_TEMPLATE' } },
  { path: '/dpr/auth/agreement-seal', name: 'AgreementSeal', component: () => import('@/views/authorize/AgreementManage.vue'), meta: { title: '协议签章上传管理', goal: '上传与管理协议签章件', action: 'seal' } },
  { path: '/dpr/auth/agreement-review', name: 'AgreementReview', component: () => import('@/views/authorize/AgreementManage.vue'), meta: { title: '协议审核申请提交管理', goal: '提交并跟踪协议审核', action: 'review' } },
  { path: '/dpr/auth/agreement-archive', name: 'AgreementArchive', component: () => import('@/views/authorize/AgreementManage.vue'), meta: { title: '协议存档管理', goal: '协议统一归档与检索', action: 'archive' } },
  { path: '/dpr/auth/cert', name: 'AuthCert', component: () => import('@/views/authorize/AuthCertList.vue'), meta: { title: '授权生效记录管理', goal: '管理授权生效记录(内部台账:续签/暂停/撤销;对外凭证=附录D协议)' } },
  { path: '/dpr/auth/cert-template', name: 'AuthCertTemplate', component: () => import('@/views/authorize/AuthCertTemplate.vue'), meta: { title: '授权生效记录样式模板', goal: '维护授权生效记录样式模板(配置化)' } },
  // F-04 综合分析管理
  { path: '/dpr/dashboard/overview', name: 'DataPropertyOverview', component: () => import('@/views/dashboard/DataPropertyOverview.vue'), meta: { title: '数据产权全景(综合)', goal: '全景洞察数据产权规模/趋势/分布,辅助决策' } },
  { path: '/dpr/dashboard/confirm', name: 'ConfirmDashboard', component: () => import('@/views/dashboard/ConfirmDashboard.vue'), meta: { title: '确权看板', goal: '看板跟踪确权进展与质量' } },
  { path: '/dpr/dashboard/auth', name: 'AuthDashboard', component: () => import('@/views/dashboard/AuthDashboard.vue'), meta: { title: '授权看板', goal: '看板跟踪授权运行情况' } },
  // 系统管理(管理员)
  { path: '/dpr/system/user', name: 'SysUser', component: () => import('@/views/system/UserList.vue'), meta: { title: '用户管理', goal: '维护系统账号:新增/编辑/启停/重置密码,分配角色' } },
  { path: '/dpr/system/role', name: 'SysRole', component: () => import('@/views/system/RoleList.vue'), meta: { title: '角色管理', goal: '查看角色目录与权限说明、各角色用户数统计' } },
  { path: '/dpr/system/oplog', name: 'SysOpLog', component: () => import('@/views/system/OpLogList.vue'), meta: { title: '操作日志', goal: '审计登录与用户管理类操作留痕,支持多维查询' } },
  // 智能确权辅助工具:独立工具(独立外壳,不挂主导航),业务流程可带 applyId/assetId 调用
  {
    path: '/aitool',
    component: () => import('@/views/aitool/AitoolShell.vue'),
    redirect: '/aitool/material',
    children: [
      { path: 'material', name: 'AitMaterial', component: () => import('@/views/aitool/MaterialParse.vue'), meta: { title: '材料智能解析', goal: '上传并智能解析确权材料:要素抽取/印章核验/术语匹配/表单比对' } },
      { path: 'conflict', name: 'AitConflict', component: () => import('@/views/aitool/ConflictDetect.vue'), meta: { title: '权属冲突识别', goal: '构建权属知识图谱,检测主体/范围/时效/历史冲突并出报告' } },
      { path: 'decision', name: 'AitDecision', component: () => import('@/views/aitool/DecisionSupport.vue'), meta: { title: '确权决策支持', goal: '综合RAG检索/因子分析生成确权决策建议与权益分割方案' } }
    ]
  },
  // 登录页(无平台布局)
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: '登录', public: true } },
  // 旧路径兼容重定向(保留历史链接/收藏)
  { path: '/dpr/aitool/:page(material|conflict|decision)', redirect: (to) => ({ path: '/aitool/' + to.params.page, query: to.query }) },
  // 404 兜底(规范 p122 强管制):无匹配路径 → 异常数据页(在主框架内渲染,带导航/面包屑)
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFound.vue'), meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫:未登录跳登录页(public 路由放行;aitool 独立工具也需登录)
router.beforeEach((to) => {
  if (to.meta && to.meta.public) return true
  const loggedIn = !!localStorage.getItem('prm-token')
  if (!loggedIn) return { path: '/login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : {} }
  return true
})

export default router
