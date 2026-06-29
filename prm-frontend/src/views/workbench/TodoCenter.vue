<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-table-note" style="margin:0 0 12px 0">
      注:跨域汇聚确权审批、授权审批待办,一处看全、一键直达办理(对齐工作指引"一次都不跑/主动协同")。
    </div>

    <el-row :gutter="12" class="todo-stats">
      <el-col :span="8"><el-card shadow="never"><el-statistic title="待办合计" :value="view.total" /></el-card></el-col>
      <el-col v-if="doesConfirm" :span="8"><el-card shadow="never"><el-statistic title="确权审批待办" :value="view.confirmCount" /></el-card></el-col>
      <el-col v-if="doesAuth" :span="8"><el-card shadow="never"><el-statistic title="授权审批待办" :value="view.authCount" /></el-card></el-col>
    </el-row>

    <div class="prm-table-card" style="margin-top:12px" v-loading="loading">
      <el-tabs v-model="tab">
        <el-tab-pane v-if="doesConfirm" :label="`确权审批 (${view.confirmCount})`" name="confirm">
          <todo-table :rows="view.confirmTodos" domain="确权" @go="goHandle" />
        </el-tab-pane>
        <el-tab-pane v-if="doesAuth" :label="`授权审批 (${view.authCount})`" name="auth">
          <todo-table :rows="view.authTodos" domain="授权" @go="goHandle" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElTable, ElTableColumn, ElButton, ElTag, ElEmpty } from 'element-plus'
import { getTodos } from '@/api/workbench'
import { currentRole, AUTH_NODE_ROLE, CONFIRM_NODE_ROLE, handledStatuses } from '@/lib/roles'

const router = useRouter()
const loading = ref(false)
const vo = reactive({ confirmTodos: [], authTodos: [], confirmCount: 0, authCount: 0, total: 0 })

// 待办收敛到「本角色·本节点」:后端待办网关返回全节点在办,前端按当前角色裁剪为各自队列。
// 副总(gm)不涉确权 → 确权 tab 整体隐藏;授权只见「副总审批中」。admin/all 看全部(null)。
const role = currentRole()
const confirmStatuses = handledStatuses(role, CONFIRM_NODE_ROLE) // null=全部 / []=本域无本角色节点
const authStatuses = handledStatuses(role, AUTH_NODE_ROLE)
const doesConfirm = computed(() => confirmStatuses === null || confirmStatuses.length > 0)
const doesAuth = computed(() => authStatuses === null || authStatuses.length > 0)
function scope(list, statuses) {
  const arr = list || []
  return statuses === null ? arr : arr.filter(r => statuses.includes(r.status))
}
const view = computed(() => {
  const confirmTodos = doesConfirm.value ? scope(vo.confirmTodos, confirmStatuses) : []
  const authTodos = doesAuth.value ? scope(vo.authTodos, authStatuses) : []
  return { confirmTodos, authTodos, confirmCount: confirmTodos.length, authCount: authTodos.length, total: confirmTodos.length + authTodos.length }
})
// 默认落到本角色实际承担的队列(gm 直接进授权 tab,不停在空的确权 tab)
const tab = ref(handledStatuses(role, CONFIRM_NODE_ROLE)?.length === 0 ? 'auth' : 'confirm')

// 域 -> 办理页路由(路由不带 :id,统一跳到对应办理/列表页)
const HANDLE_ROUTE = { 确权: '/dpr/confirm/review', 授权: '/dpr/auth/review' }

function goHandle(row) {
  const path = HANDLE_ROUTE[row.domain]
  if (path) router.push({ path, query: row.assetId ? { assetId: row.assetId } : {} })
}

async function load() {
  loading.value = true
  try {
    const r = await getTodos()
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
</script>

<style scoped>
.todo-stats :deep(.el-card__body) { padding: 14px 18px; }
</style>
