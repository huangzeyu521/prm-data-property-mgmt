<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-input v-model="keyword" placeholder="检索数据集/系统定位" clearable style="width:240px" />
      <el-switch v-model="onlyPending" active-text="仅看未确权/申请中" style="margin-left:14px" />
      <el-button style="margin-left:14px" @click="expandAll(true)">展开全部</el-button>
      <el-button @click="expandAll(false)">折叠全部</el-button>
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
      <span class="cat-legend">
        <el-tag size="small" type="warning" effect="plain">待确权 {{ counts.pending }}</el-tag>
        <el-tag size="small" effect="plain">申请中 {{ counts.applying }}</el-tag>
        <el-tag size="small" type="success" effect="plain">已确权 {{ counts.confirmed }}</el-tag>
      </span>
    </div>
    <div class="prm-table-card">
      <div class="prm-table-note" style="margin:0 0 12px 0">注:树形展示"子公司—系统—模式—数据集"全量资产及确权状态;待确权可一键发起确权,申请中/已确权可查看申请信息。</div>
      <el-tree
        ref="treeRef" :data="tree" :props="treeProps" node-key="id"
        :filter-node-method="filterNode" default-expand-all v-loading="loading">
        <template #default="{ data }">
          <span class="cat-node">
            <span class="cat-label">{{ data.label }}</span>
            <template v-if="data.type === 'DATASET'">
              <el-tag size="small" :type="statusTag(data.status)" effect="plain" style="margin-left:8px">{{ data.status }}</el-tag>
              <span class="cat-aid">{{ data.assetId }}</span>
              <el-button v-if="data.status === '待确权'" link type="primary" size="small" @click.stop="onConfirm(data)">发起确权</el-button>
              <el-button v-else link type="primary" size="small" @click.stop="onView(data)">查看申请</el-button>
            </template>
          </span>
        </template>
      </el-tree>
    </div>

    <el-dialog v-model="viewDlg" title="确权申请信息" width="520px" align-center>
      <el-descriptions v-if="curApply" :column="2" border size="small">
        <el-descriptions-item label="数据集" :span="2">{{ curApply.assetName }}（{{ curApply.assetId }}）</el-descriptions-item>
        <el-descriptions-item label="申请编号">{{ curApply.applyNo }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="applyTag(curApply.status)">{{ curApply.status }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="权属类型">{{ curApply.rightType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="权利人">{{ curApply.rightHolder || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间" :span="2">{{ fmt(curApply.createTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="curApply.rejectReason" label="驳回原因" :span="2">{{ curApply.rejectReason }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else :image-size="60" description="该数据集已确权,暂无在途确权申请" />
      <template #footer>
        <el-button v-if="curApply" type="primary" @click="goHistory">去申请查询</el-button>
        <el-button @click="viewDlg = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPropertyTree } from '@/api/ledger'
import { pageConfirmApply, saveConfirmDraft, submitConfirm } from '@/api/confirm'

const router = useRouter()
const IN_PROGRESS = ['草稿', '人工预审中', '合规审核中', '主管复核中', '经理终审中']
const treeProps = { label: 'label', children: 'children' }
const treeRef = ref()
const tree = ref([])
const loading = ref(false)
const keyword = ref('')
const onlyPending = ref(false)
const counts = reactive({ pending: 0, applying: 0, confirmed: 0 })
const viewDlg = ref(false)
const curApply = ref(null)

function statusTag(s) { return { 待确权: 'warning', 申请中: '', 已确权: 'success' }[s] || 'info' }
function applyTag(s) { return { 已完成: 'success', 已驳回: 'danger', 已撤回: 'info', 草稿: 'info' }[s] || 'warning' }
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }

function transform(nodes, applyMap) {
  return (nodes || []).map((n) => {
    const node = { id: n.id, label: n.label, type: n.type, assetId: n.assetId, children: transform(n.children, applyMap) }
    if (n.type === 'DATASET') {
      const apply = applyMap[n.assetId]
      if (apply && IN_PROGRESS.includes(apply.status)) { node.status = '申请中'; node.apply = apply }
      else node.status = n.confirmStatus === '已确权' ? '已确权' : '待确权'
      node.apply = node.apply || apply || null
      if (node.status === '待确权') counts.pending++
      else if (node.status === '申请中') counts.applying++
      else counts.confirmed++
    }
    return node
  })
}

async function load() {
  loading.value = true
  counts.pending = counts.applying = counts.confirmed = 0
  try {
    const [treeData, applyPage] = await Promise.all([
      getPropertyTree(),
      pageConfirmApply({ current: 1, size: 500 })
    ])
    const applyMap = {}
    for (const a of (applyPage.records || [])) {
      if (a.assetId && !applyMap[a.assetId]) applyMap[a.assetId] = a // 列表按时间倒序,取最新
    }
    tree.value = transform(treeData || [], applyMap)
    await nextTick()
    applyFilter()
  } finally { loading.value = false }
}

function filterNode(value, data) {
  if (data.type !== 'DATASET') return false // 非叶:由是否有可见子节点决定
  const kwOk = !keyword.value || (data.label || '').includes(keyword.value) || (data.assetId || '').includes(keyword.value)
  const stOk = !onlyPending.value || data.status === '待确权' || data.status === '申请中'
  return kwOk && stOk
}
function applyFilter() { treeRef.value && treeRef.value.filter('') }
watch([keyword, onlyPending], applyFilter)

function expandAll(on) {
  const nodes = treeRef.value?.store?.nodesMap || {}
  Object.values(nodes).forEach((n) => { n.expanded = on })
}

function onConfirm(data) {
  ElMessageBox.confirm(`对数据集"${data.label}"(${data.assetId})发起数据确权申请?`, '一键发起确权', { type: 'info' })
    .then(async () => {
      const id = await saveConfirmDraft({ assetId: data.assetId, assetName: data.label, rightType: '数据资源持有权' })
      await submitConfirm(id)
      ElMessage.success('已发起确权申请')
      load()
    }).catch(() => {})
}
function onView(data) { curApply.value = data.apply || null; viewDlg.value = true }
function goHistory() { router.push({ path: '/dpr/confirm/history' }) }

onMounted(load)
</script>

<style scoped>
.cat-legend { margin-left: 16px; display: inline-flex; gap: 6px; }
.cat-node { display: inline-flex; align-items: center; gap: 6px; }
.cat-label { font-size: 13px; }
.cat-aid { color: #aab; font-size: 11px; }
</style>
