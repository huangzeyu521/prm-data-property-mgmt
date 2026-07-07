<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!--
  数据资产确权目录管理:只看用户权限下「系统 → 功能模块 → 库表」的各级确权状态。
  确权状态口径对齐初始确权/确权变更申请页(ConfirmCatalogTree):库表 已确权/未确权/申请中 + 已授权;
  系统/功能模块 展示确权进度(已确权 N/M)。不展示数据资产卡片内容(平台数据资产管理已支持)。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-input v-model="keyword" placeholder="检索系统/功能模块/库表" clearable style="width:240px" />
      <el-select v-model="statusFilter" style="width:140px;margin-left:14px" placeholder="确权状态">
        <el-option label="全部状态" value="" />
        <el-option label="未确权" value="未确权" />
        <el-option label="申请中" value="申请中" />
        <el-option label="已确权" value="已确权" />
      </el-select>
      <el-button style="margin-left:14px" @click="expandAll(true)">展开全部</el-button>
      <el-button @click="expandAll(false)">折叠全部</el-button>
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
      <span class="cat-legend">
        <span class="prm-c-primary">库表 {{ counts.card }}</span>
        <span class="prm-c-warning">未确权 {{ counts.pending }}</span>
        <span class="prm-c-primary">申请中 {{ counts.applying }}</span>
        <span class="prm-c-success">已确权 {{ counts.confirmed }}</span>
      </span>
    </div>

    <div class="prm-table-card">
      <div class="prm-table-note" style="margin:0 0 10px 0">
        树形展示用户权限下"业务域—系统—功能模块—库表"全量确权范围及各级确权状态(口径对齐初始确权/确权变更申请)。
        库表:<span class="prm-c-warning">未确权</span> 可发起确权,
        <span class="prm-c-primary">申请中</span> /
        <span class="prm-c-success">已确权</span> 可查看确权申请;
        <span class="prm-c-danger">已授权</span> 表示该库表已对外授权。
      </div>
      <el-tree
        ref="treeRef" :data="tree" :props="treeProps" node-key="id"
        :filter-node-method="filterNode" default-expand-all highlight-current v-loading="loading">
        <template #default="{ data }">
          <span class="cat-node">
            <el-icon v-if="data.type === 'domain'" class="cat-ic"><Grid /></el-icon>
            <el-icon v-else-if="data.type === 'system'" class="cat-ic"><Monitor /></el-icon>
            <el-icon v-else-if="data.type === 'module'" class="cat-ic"><Folder /></el-icon>
            <el-icon v-else class="cat-ic" style="color:var(--prm-color-link)"><Document /></el-icon>
            <span :class="{ 'cat-label': true, 'is-card': data.type === 'card' }">{{ data.label }}</span>

            <!-- 库表:确权状态 + 已授权 + 表代码 + 操作 -->
            <template v-if="data.type === 'card'">
              <span :class="'cat-badge prm-c-' + ((statusTag(cardStatus(data))) || 'primary')">{{ cardStatus(data) }}</span>
              <span v-if="data.authorized" class="cat-badge prm-c-danger">已授权</span>
              <span class="cat-code">{{ data.tableCode }}</span>
              <el-button v-if="cardStatus(data) === '未确权'" link type="primary" size="small" @click.stop="onConfirm(data)">发起确权</el-button>
              <el-button v-else link type="primary" size="small" @click.stop="goHistory">查看申请</el-button>
            </template>

            <!-- 系统/功能模块:确权进度(已确权 N/M)-->
            <template v-else-if="data.type === 'system' || data.type === 'module'">
              <span :class="'cat-badge prm-c-' + ((progBadge(data).type) || 'primary')">{{ progBadge(data).text }}</span>
            </template>
            <span v-else class="cat-cnt">{{ progress(data).total }} 张库表</span>
          </span>
        </template>
      </el-tree>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { confirmAsync } from '@/utils/confirmAsync'
import { Grid, Monitor, Folder, Document } from '@element-plus/icons-vue'
import { fullCatalogTree } from '@/api/assetCard'
import { pageConfirmApply } from '@/api/confirm'
import { currentRole } from '@/lib/roles'

const router = useRouter()
const IN_PROGRESS = ['草稿', '人工预审中', '合规审核中', '主管复核中', '经理终审中']
const treeProps = { label: 'label', children: 'children' }
const treeRef = ref()
const tree = ref([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const counts = reactive({ card: 0, pending: 0, applying: 0, confirmed: 0 })
// 确权为系统级(assetId=SYS:<系统名>):系统在途→该系统下库表均"申请中"
const applyingSys = ref(new Set())

function statusTag(s) { return { 未确权: 'warning', 申请中: '', 已确权: 'success' }[s] || 'info' }
// 库表确权状态(口径对齐申请页):系统在途→申请中;否则按库表 confirmed
function cardStatus(data) {
  if (applyingSys.value.has(data.sysName)) return '申请中'
  return data.confirmed ? '已确权' : '未确权'
}
// 节点确权进度:其下库表 已确权数 / 总数
function progress(node) {
  let total = 0, confirmed = 0
  const walk = (nodes) => (nodes || []).forEach(x => {
    if (x.type === 'card') { total++; if (cardStatus(x) === '已确权') confirmed++ }
    else walk(x.children)
  })
  walk(node.children)
  return { total, confirmed }
}
function progBadge(node) {
  const { total, confirmed } = progress(node)
  const type = confirmed >= total && total > 0 ? 'success' : (confirmed > 0 ? 'warning' : 'info')
  const label = confirmed >= total && total > 0 ? '已确权' : (confirmed > 0 ? '部分确权' : '未确权')
  return { type, text: `${label} ${confirmed}/${total}` }
}

async function load() {
  loading.value = true
  counts.card = counts.pending = counts.applying = counts.confirmed = 0
  try {
    const [treeData, applyPage] = await Promise.all([
      fullCatalogTree(),
      pageConfirmApply({ current: 1, size: 100 })
    ])
    const set = new Set()
    for (const a of (applyPage.records || [])) {
      const id = a.assetId || ''
      if (id.startsWith('SYS:') && IN_PROGRESS.includes(a.status)) set.add(id.slice(4))
    }
    applyingSys.value = set
    tree.value = treeData || []
    countCards(tree.value)
    await nextTick()
    applyFilter()
  } finally { loading.value = false }
}

function countCards(nodes) {
  for (const n of (nodes || [])) {
    if (n.type === 'card') {
      counts.card++
      const st = cardStatus(n)
      if (st === '申请中') counts.applying++
      else if (st === '已确权') counts.confirmed++
      else counts.pending++
    } else countCards(n.children)
  }
}

function filterNode(value, data) {
  if (data.type !== 'card') return false // 非叶:由是否有可见库表决定
  const kwOk = !keyword.value || (data.label || '').includes(keyword.value) || (data.tableCode || '').includes(keyword.value)
  const stOk = !statusFilter.value || cardStatus(data) === statusFilter.value
  return kwOk && stOk
}
function applyFilter() { treeRef.value && treeRef.value.filter('') }
watch([keyword, statusFilter], applyFilter)

function expandAll(on) {
  const nodes = treeRef.value?.store?.nodesMap || {}
  Object.values(nodes).forEach((n) => { n.expanded = on })
}

function onConfirm(data) {
  confirmAsync(`数据确权以「系统」为单元(一份确权申请 = 一个系统)。是否前往「初始确权申请」,在确权范围树中选择「${data.sysName}」及库表发起确权?`, '发起确权',
    async () => { await router.push({ path: '/dpr/confirm/wizard' }) },
    { type: 'info', confirmButtonText: '前往初始确权' }).catch(() => {})
}
// 申报人查询进度走「我的申请」发件箱(其确权申请查询已从申报人菜单移除);其他角色仍去确权申请查询台。
function goHistory() { router.push({ path: currentRole() === 'apply' ? '/dpr/workbench/my' : '/dpr/confirm/history' }) }

onMounted(load)
</script>

<style scoped>
.cat-legend { margin-left: 16px; display: inline-flex; gap: 6px; }
.cat-node { display: inline-flex; align-items: center; gap: 5px; }
.cat-ic { font-size: 14px; color: var(--prm-color-text-weak); }
.cat-label { font-size: 13px; }
.cat-label.is-card { color: var(--prm-color-link); }
.cat-badge { transform: scale(0.86); }
.cat-code { color: var(--prm-color-text-disabled); font-size: 11px; margin-left: 4px; }
.cat-cnt { color: var(--prm-color-text-weak); font-size: 12px; margin-left: 6px; }
</style>
