import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/dpr/dashboard/overview' },
  { path: '/dpr', redirect: '/dpr/dashboard/overview' },
  // 统一待办中心(eLink 待办的 Web 实现)
  { path: '/dpr/workbench/todo', name: 'TodoCenter', component: () => import('@/views/workbench/TodoCenter.vue'), meta: { title: '统一待办中心' } },
  {
    path: '/dpr/ledger/overview',
    name: 'LedgerOverview',
    component: () => import('@/views/ledger/LedgerOverview.vue'),
    meta: { title: '产权台账概览(含产权树)' }
  },
  {
    path: '/dpr/ledger/archive',
    name: 'PropertyArchive',
    component: () => import('@/views/ledger/PropertyArchiveList.vue'),
    meta: { title: '数据集产权档案管理' }
  },
  { path: '/dpr/ledger/dataset', name: 'DatasetDetail', component: () => import('@/views/ledger/DatasetDetail.vue'), meta: { title: '数据集详情展示' } },
  { path: '/dpr/ledger/change', name: 'ChangeRecord', component: () => import('@/views/ledger/ChangeRecordList.vue'), meta: { title: '产权变更记录管理' } },
  { path: '/dpr/ledger/statistics', name: 'LedgerStatistics', component: () => import('@/views/ledger/LedgerStatistics.vue'), meta: { title: '产权台账统计分析' } },
  { path: '/dpr/monitor/status', name: 'RightsStatusMonitor', component: () => import('@/views/monitor/RightsStatusMonitor.vue'), meta: { title: '权益状态监控' } },
  {
    path: '/dpr/monitor/alert',
    name: 'AlertList',
    component: () => import('@/views/monitor/AlertList.vue'),
    meta: { title: '权益变动监测预警' }
  },
  { path: '/dpr/monitor/compliance', name: 'ComplianceCheck', component: () => import('@/views/monitor/ComplianceCheck.vue'), meta: { title: '合规性检查' } },
  {
    path: '/dpr/monitor/rule',
    name: 'MonitorRule',
    component: () => import('@/views/monitor/MonitorRuleList.vue'),
    meta: { title: '监测规则配置' }
  },
  { path: '/dpr/monitor/notification', name: 'RiskNotification', component: () => import('@/views/monitor/RiskNotification.vue'), meta: { title: '风险预警通知' } },
  // F-02 数据确权管理
  { path: '/dpr/confirm/wizard', name: 'ConfirmWizard', component: () => import('@/views/confirm/ConfirmWizard.vue'), meta: { title: '确权申请(一站式)' } },
  { path: '/dpr/confirm/guidance', name: 'ConfirmGuidance', component: () => import('@/views/confirm/ConfirmGuidance.vue'), meta: { title: '确权指引管理' } },
  { path: '/dpr/confirm/catalog', name: 'ConfirmCatalog', component: () => import('@/views/confirm/ConfirmCatalog.vue'), meta: { title: '数据资产确权目录管理' } },
  { path: '/dpr/confirm/history', name: 'ConfirmHistory', component: () => import('@/views/confirm/ConfirmHistory.vue'), meta: { title: '确权申请查询(进度/历史)' } },
  { path: '/dpr/confirm/review', name: 'ConfirmReviewDesk', component: () => import('@/views/confirm/ConfirmReviewDesk.vue'), meta: { title: '审核申请提交管理' } },
  { path: '/dpr/confirm/card', name: 'EquityCard', component: () => import('@/views/confirm/EquityCardList.vue'), meta: { title: '权益卡片生成管理' } },
  { path: '/dpr/confirm/cert', name: 'EquityCert', component: () => import('@/views/confirm/EquityCertList.vue'), meta: { title: '权益证书管理' } },
  // F-03 数据授权管理
  { path: '/dpr/auth/wizard', name: 'AuthWizard', component: () => import('@/views/authorize/AuthWizard.vue'), meta: { title: '一事一议授权申请(一站式)' } },
  { path: '/dpr/auth/batch-wizard', name: 'BatchAuthWizard', component: () => import('@/views/authorize/BatchAuthWizard.vue'), meta: { title: '批量授权申请(一站式)' } },
  { path: '/dpr/auth/filing', name: 'AuthFiling', component: () => import('@/views/authorize/AuthFiling.vue'), meta: { title: '对外经营权授权备案(附录G)' } },
  { path: '/dpr/auth/guidance', name: 'AuthGuidance', component: () => import('@/views/authorize/AuthGuidance.vue'), meta: { title: '授权指引管理', category: 'GUIDANCE' } },
  { path: '/dpr/auth/form-template', name: 'AuthFormTemplate', component: () => import('@/views/authorize/AuthFormTemplate.vue'), meta: { title: '授权申请表单设计管理', category: 'FORM_TEMPLATE' } },
  { path: '/dpr/auth/scenario', name: 'AuthScenario', component: () => import('@/views/authorize/AuthCatalogPage.vue'), meta: { title: '应用场景管理', category: 'SCENARIO' } },
  { path: '/dpr/auth/batch-list', name: 'AuthBatchList', component: () => import('@/views/authorize/AuthBatchList.vue'), meta: { title: '批量授权清单(表6)' } },
  { path: '/dpr/auth/compliance', name: 'AuthCompliance', component: () => import('@/views/authorize/AuthCompliance.vue'), meta: { title: '合规校验管理' } },
  { path: '/dpr/auth/history', name: 'AuthHistory', component: () => import('@/views/authorize/AuthApplyHistory.vue'), meta: { title: '授权申请历史查询管理' } },
  { path: '/dpr/auth/review', name: 'AuthReviewDesk', component: () => import('@/views/authorize/AuthReviewDesk.vue'), meta: { title: '授权审核申请提交管理' } },
  { path: '/dpr/auth/agreement-template', name: 'AgreementTemplate', component: () => import('@/views/authorize/AuthCatalogPage.vue'), meta: { title: '协议模板库管理', category: 'AGREEMENT_TEMPLATE' } },
  { path: '/dpr/auth/agreement-seal', name: 'AgreementSeal', component: () => import('@/views/authorize/AgreementManage.vue'), meta: { title: '协议签章上传管理', action: 'seal' } },
  { path: '/dpr/auth/agreement-review', name: 'AgreementReview', component: () => import('@/views/authorize/AgreementManage.vue'), meta: { title: '协议审核申请提交管理', action: 'review' } },
  { path: '/dpr/auth/agreement-archive', name: 'AgreementArchive', component: () => import('@/views/authorize/AgreementManage.vue'), meta: { title: '协议存档管理', action: 'archive' } },
  { path: '/dpr/auth/cert', name: 'AuthCert', component: () => import('@/views/authorize/AuthCertList.vue'), meta: { title: '授权权益管理' } },
  { path: '/dpr/auth/cert-template', name: 'AuthCertTemplate', component: () => import('@/views/authorize/AuthCertTemplate.vue'), meta: { title: '授权权益证书模板管理' } },
  // F-04 综合分析管理
  { path: '/dpr/dashboard/overview', name: 'DataPropertyOverview', component: () => import('@/views/dashboard/DataPropertyOverview.vue'), meta: { title: '数据产权全景(综合)' } },
  { path: '/dpr/dashboard/confirm', name: 'ConfirmDashboard', component: () => import('@/views/dashboard/ConfirmDashboard.vue'), meta: { title: '确权看板' } },
  { path: '/dpr/dashboard/auth', name: 'AuthDashboard', component: () => import('@/views/dashboard/AuthDashboard.vue'), meta: { title: '授权看板' } },
  // 智能确权辅助工具
  { path: '/dpr/aitool/material', name: 'AitMaterial', component: () => import('@/views/aitool/MaterialParse.vue'), meta: { title: '材料智能解析' } },
  { path: '/dpr/aitool/conflict', name: 'AitConflict', component: () => import('@/views/aitool/ConflictDetect.vue'), meta: { title: '权属冲突识别' } },
  { path: '/dpr/aitool/decision', name: 'AitDecision', component: () => import('@/views/aitool/DecisionSupport.vue'), meta: { title: '确权决策支持' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
