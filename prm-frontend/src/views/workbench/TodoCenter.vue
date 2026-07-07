<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <PageNote>注:跨域汇聚确权审批、授权审批待办,一处看全、一键直达办理(对齐工作指引"一次都不跑/主动协同")。</PageNote>

    <el-row :gutter="12" class="todo-stats">
      <el-col :span="8"><el-card shadow="never"><el-statistic title="待办合计" :value="view.total" /></el-card></el-col>
      <el-col v-if="doesConfirm" :span="8"><el-card shadow="never"><el-statistic title="确权审批待办" :value="view.confirmCount" /></el-card></el-col>
      <el-col v-if="doesAuth" :span="8"><el-card shadow="never"><el-statistic title="授权审批待办" :value="view.authCount" /></el-card></el-col>
      <el-col v-if="isApplicantRole" :span="8"><el-card shadow="never"><el-statistic title="我的待办(需修改重提)" :value="view.myCount" /></el-card></el-col>
    </el-row>

    <div class="prm-table-card" style="margin-top:12px" v-loading="loading">
      <el-tabs v-model="tab">
        <el-tab-pane v-if="doesConfirm" :label="`确权审批 (${view.confirmCount})`" name="confirm">
          <todo-table :rows="view.confirmTodos" domain="确权" @go="goHandle" />
        </el-tab-pane>
        <el-tab-pane v-if="doesAuth" :label="`授权审批 (${view.authCount})`" name="auth">
          <todo-table :rows="view.authTodos" domain="授权" @go="goHandle" />
        </el-tab-pane>
        <!-- 申报人/业务管理部门:本人被驳回、待修改重提的申请(复用"我的申请"的重提机制),
             让统一待办中心对无审批职责的申报角色也有内容可看,而不是一片空白。 -->
        <el-tab-pane v-if="isApplicantRole" :label="`我的待办 (${view.myCount})`" name="mine">
          <my-todo-table :rows="view.myTodos" @reopen="goReopen" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import PageNote from '@/components/PageNote.vue'
import { computed, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElTable, ElTableColumn, ElButton, ElTag, ElEmpty } from 'element-plus'
import { getTodos } from '@/api/workbench'
import { pageConfirmApply } from '@/api/confirm'
import { pageAuthApply } from '@/api/authorize'
import { currentRole, AUTH_NODE_ROLE, CONFIRM_NODE_ROLE, handledStatuses } from '@/lib/roles'

const router = useRouter()
const loading = ref(false)
const vo = reactive({ confirmTodos: [], authTodos: [], confirmCount: 0, authCount: 0, total: 0, myTodos: [] })
const me = () => localStorage.getItem('X-User-Id') || ''

// 待办收敛到「本角色·本节点」:后端待办网关返回全节点在办,前端按当前角色裁剪为各自队列。
// 副总(gm)不涉确权 → 确权 tab 整体隐藏;授权只见「副总审批中」。admin/all 看全部(null)。
const role = currentRole()
const confirmStatuses = handledStatuses(role, CONFIRM_NODE_ROLE) // null=全部 / []=本域无本角色节点
const authStatuses = handledStatuses(role, AUTH_NODE_ROLE)
const doesConfirm = computed(() => confirmStatuses === null || confirmStatuses.length > 0)
const doesAuth = computed(() => authStatuses === null || authStatuses.length > 0)
// 申报人/业务管理部门:无审批节点(doesConfirm/doesAuth 恒为 false),但仍有"本人被驳回待修改重提"这类真实待办；
// 否则统一待办中心对这两个角色永远一片空白,只是页面能打开、没内容可看。
const isApplicantRole = computed(() => ['apply', 'business'].includes(role))
function scope(list, statuses) {
  const arr = list || []
  return statuses === null ? arr : arr.filter(r => statuses.includes(r.status))
}
const view = computed(() => {
  const confirmTodos = doesConfirm.value ? scope(vo.confirmTodos, confirmStatuses) : []
  const authTodos = doesAuth.value ? scope(vo.authTodos, authStatuses) : []
  const myTodos = isApplicantRole.value ? vo.myTodos : []
  return {
    confirmTodos, authTodos, myTodos,
    confirmCount: confirmTodos.length, authCount: authTodos.length, myCount: myTodos.length,
    total: confirmTodos.length + authTodos.length + myTodos.length,
  }
})
// 默认落到本角色实际承担的队列:有确权待办进确权 tab,否则有授权待办进授权 tab,
// 否则(申报人/业务管理部门这类无审批节点的角色)落到"我的待办"，不停在一个不存在/空的 tab 上。
const tab = ref(doesConfirm.value ? 'confirm' : (doesAuth.value ? 'auth' : (isApplicantRole.value ? 'mine' : 'confirm')))

// 域 -> 办理页路由(路由不带 :id,统一跳到对应办理/列表页)
const HANDLE_ROUTE = { 确权: '/dpr/confirm/review', 授权: '/dpr/auth/review' }

function goHandle(row) {
  const path = HANDLE_ROUTE[row.domain]
  // bug修复:审核台(ConfirmReviewDesk/AuthReviewDesk)按 applyId 高亮定位到具体那一条(rowHl)，
  // 此前只带 assetId(审核台根本不认这个参数名)，"一键直达"实际落到一个未过滤的列表，用户还要自己再找一遍。
  if (path) router.push({ path, query: row.id ? { applyId: row.id, assetId: row.assetId || '' } : {} })
}

// 我的待办(已驳回待修改重提):复用"我的申请"同款重提机制——存原始记录到 sessionStorage，
// 带 reopen=1 跳回对应向导，向导据此预填回填(与 MyApplications.vue 的 goReopen 保持一致)。
function goReopen(row) {
  sessionStorage.setItem('prm-reopen', JSON.stringify({ domain: row.domain, raw: row.raw || {} }))
  let base = '/dpr/confirm/wizard'
  if (row.domain === '授权') base = row.mode === '批量' ? '/dpr/auth/batch-wizard' : '/dpr/auth/wizard'
  router.push({ path: base, query: { reopen: '1' } })
}

async function loadMyTodos() {
  if (!isApplicantRole.value) return
  const uid = me()
  // business 角色实际不提交确权申请，但一起查也无妨——creatorId 过滤后自然是空，
  // 比按角色分支决定调哪个接口更简单、更不容易出错。
  const [c, a] = await Promise.all([
    pageConfirmApply({ current: 1, size: 100 }),
    pageAuthApply({ current: 1, size: 100 }),
  ])
  const cf = (c.records || [])
    .filter(r => r.status === '已驳回' && (!uid || r.creatorId === uid))
    .map(r => ({ domain: '确权', mode: '', no: r.applyNo, assetName: r.assetName, rejectReason: r.rejectReason, updateTime: r.updateTime || r.createTime, raw: r }))
  const af = (a.records || [])
    .filter(r => r.status === '已驳回' && (!uid || r.creatorId === uid))
    .map(r => ({ domain: '授权', mode: r.authMode, no: r.applyNo, assetName: r.assetName, rejectReason: r.rejectReason, updateTime: r.updateTime || r.createTime, raw: r }))
  vo.myTodos = [...cf, ...af].sort((x, y) => String(y.updateTime || '').localeCompare(String(x.updateTime || '')))
}

async function load() {
  loading.value = true
  try {
    const [r] = await Promise.all([getTodos(), loadMyTodos()])
    Object.assign(vo, r || {})
  } finally {
    loading.value = false
  }
}
onMounted(load)

// 内联待办表格组件:确权/授权/风险共用一套列
const TodoTable = {
  props: { rows: { type: Array, default: () => [] }, domain: String },
  emits: ['go'],
  setup(props, { emit }) {
    return () => {
      if (!props.rows || props.rows.length === 0) return h(ElEmpty, { description: '暂无待办' })
      return h(ElTable, { data: props.rows, border: true, stripe: true }, () => [
        h(ElTableColumn, { type: 'index', label: '序号', width: 64, align: 'center' }),
        h(ElTableColumn, { prop: 'no', label: '单号', width: 170, showOverflowTooltip: true }),
        h(ElTableColumn, { prop: 'assetName', label: '资产名称', minWidth: 160, showOverflowTooltip: true }),
        h(ElTableColumn, { prop: 'party', label: props.domain === '授权' ? '被授权方' : '权属主体', minWidth: 140, showOverflowTooltip: true }),
        h(ElTableColumn, { label: '当前状态', width: 130 }, { default: ({ row }) => h(ElTag, { type: 'warning' }, () => row.status) }),
        h(ElTableColumn, { label: '操作', width: 120, align: 'center' }, {
          default: ({ row }) => h(ElButton, { type: 'primary', link: true, onClick: () => emit('go', row) }, () => '前往办理')
        })
      ])
    }
  }
}

// 内联"我的待办"表格:本人被驳回、待修改重提的申请(确权/授权合并展示)
const MyTodoTable = {
  props: { rows: { type: Array, default: () => [] } },
  emits: ['reopen'],
  setup(props, { emit }) {
    return () => {
      if (!props.rows || props.rows.length === 0) return h(ElEmpty, { description: '暂无待处理(没有被驳回、待修改重提的申请)' })
      return h(ElTable, { data: props.rows, border: true, stripe: true }, () => [
        h(ElTableColumn, { type: 'index', label: '序号', width: 64, align: 'center' }),
        h(ElTableColumn, { prop: 'no', label: '单号', width: 170, showOverflowTooltip: true }),
        h(ElTableColumn, { label: '类型', width: 90 }, { default: ({ row }) => h(ElTag, {}, () => row.mode ? `${row.domain}·${row.mode}` : row.domain) }),
        h(ElTableColumn, { prop: 'assetName', label: '资产名称', minWidth: 150, showOverflowTooltip: true }),
        h(ElTableColumn, { prop: 'rejectReason', label: '驳回原因', minWidth: 200, showOverflowTooltip: true }, {
          default: ({ row }) => row.rejectReason || '—'
        }),
        h(ElTableColumn, { prop: 'updateTime', label: '驳回时间', width: 160 }),
        h(ElTableColumn, { label: '操作', width: 110, align: 'center' }, {
          default: ({ row }) => h(ElButton, { type: 'warning', link: true, onClick: () => emit('reopen', row) }, () => '去修改重提')
        })
      ])
    }
  }
}
</script>

<style scoped>
.todo-stats :deep(.el-card__body) { padding: 14px 18px; }
</style>
