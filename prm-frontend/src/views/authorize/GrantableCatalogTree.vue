<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!--
  可授权资源池树(批量授权目录入口):展示格式对齐"初始确权(一站式)"左侧确权范围树
  —— 选系统 → 选模块 → 选库表(多选)。但数据为"过滤后的可授权资源池":
    先确后授   : 仅保留有生效权益卡片(正常/生效)的资产;
    权属可授   : 仅保留"卡片权益类型 == 所选授权权益类型"的资产(授权只授使用权/经营权);
    经营权对外开放: 所选为经营权时,另按对外开放目录(后端 /grantable/open-filter)裁剪。
  与确权左树不同:批量授权允许跨系统累加,不做"一份申请限定一个系统"的单系统锁。
  勾选库表后 emit('change', leaves);每个叶子带 assetId/equityCardId/rightType 等下游 join 键。
-->
<template>
  <div class="cat-tree">
    <div class="cat-tree-hd">确权目录(选系统 → 选模块 → 选库表)</div>
    <el-input v-model="kw" size="small" placeholder="搜索系统/模块/库表" clearable style="margin-bottom:8px" />
    <el-alert v-if="!loading && !treeData.length" type="info" :closable="false" :title="emptyHint" />
    <el-tree
      v-else
      ref="treeRef" v-loading="loading" :data="treeData" :props="treeProps" node-key="id"
      show-checkbox :filter-node-method="filterNode" :expand-on-click-node="false" default-expand-all
      @check="onCheck">
      <template #default="{ data }">
        <span :class="{ 'is-table': data.type === 'table' }">{{ data.name }}</span>
        <span v-if="data.type !== 'table'" class="cat-count">{{ data.count }}</span>
        <span v-else class="leaf-card">{{ data.equityCardId }}</span>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { fullCatalogTree } from '@/api/assetCard'
import { pageEquityCard } from '@/api/confirm'
import { grantableOpenFilter } from '@/api/authorize'

const props = defineProps({
  // 所选授权权益类型:数据加工使用权 / 数据产品经营权(资源池据此做"权属可授"过滤)
  rightType: { type: String, default: '' }
})
const emit = defineEmits(['change'])

const RIGHT_OPERATION = '数据产品经营权'
const CARD_OK = ['正常', '生效']
const UNKNOWN_SYS = '未分类系统'
const UNKNOWN_MOD = '未分类模块'

const treeRef = ref()
const kw = ref('')
const loading = ref(false)
const treeData = ref([])
const treeProps = { label: 'name', children: 'children' }

const emptyHint = computed(() => {
  if (!props.rightType) return '请先在清单头选择"权益类型",再选取可授数据资产'
  const base = `当前「${props.rightType}」暂无可授数据表:需已确权(有生效权益卡片)且卡片权益与所选一致`
  return props.rightType === RIGHT_OPERATION ? base + ',并在对外开放目录中' : base
})

// 确权目录全树(业务域→系统→模块→库表「card」叶)递归收集库表叶子,沿途带业务域/系统/模块名。
// 与「数据资产确权目录管理」同源(fullCatalogTree),叶键 = sysName + tableCode(对齐库表级权益卡片)。
function collectLeaves(nodes, domain, sys, mod, out) {
  for (const n of (nodes || [])) {
    if (n.type === 'card' && n.tableCode) {
      out.push({
        sysName: n.sysName || sys || UNKNOWN_SYS,
        tableCode: n.tableCode,
        tableName: n.label, // 确权目录全树叶名在 label(record 组件名),非 name
        moduleName: mod || UNKNOWN_MOD,
        businessDomain: domain || ''
      })
    } else {
      const nd = n.type === 'domain' ? n.label : domain
      const ns = n.type === 'system' ? n.label : sys
      const nm = n.type === 'module' ? n.label : mod
      collectLeaves(n.children, nd, ns, nm, out)
    }
  }
}

// 重组为 系统 → 模块 → 库表;叶子带 (assetId=SYS:系统, tableCode, 生效卡片, 卡片权益) 等下游 join 键
function regroup(candidates) {
  const sysMap = new Map()
  for (const c of candidates) {
    if (!sysMap.has(c.sysName)) sysMap.set(c.sysName, new Map())
    const modMap = sysMap.get(c.sysName)
    if (!modMap.has(c.moduleName)) modMap.set(c.moduleName, [])
    modMap.get(c.moduleName).push({
      id: 'DS:' + c.assetId + ':' + c.tableCode, name: c.tableName, type: 'table',
      assetId: c.assetId, assetName: c.tableName, tableCode: c.tableCode,
      systemName: c.sysName, schemaName: c.card.schemaName || '',
      businessDomain: c.businessDomain, // 所属业务域:确权目录业务域逐表带出(表5/表6)
      equityCardId: c.card.cardNo || c.card.cardId, rightType: c.card.rightType
    })
  }
  const out = []
  for (const [sys, modMap] of sysMap) {
    const sysNode = { id: 'SYS:' + sys, name: sys, type: 'system', count: 0, children: [] }
    for (const [mod, tbls] of modMap) {
      sysNode.children.push({ id: 'SYS:' + sys + '|MOD:' + mod, name: mod, type: 'module', count: tbls.length, children: tbls })
      sysNode.count += tbls.length
    }
    out.push(sysNode)
  }
  return out
}

async function build() {
  emit('change', [])
  if (!props.rightType) { treeData.value = []; return }
  loading.value = true
  try {
    const [tree, cardPage] = await Promise.all([
      fullCatalogTree(),
      pageEquityCard({ current: 1, size: 500 })
    ])
    // 先确后授 + 权属可授:仅"可用 + 卡片权益==所选权益"的生效卡片;库表级按 (assetId, tableCode) 配对,
    // 缺 tableCode 的系统级卡片作为该系统兜底(覆盖未拆库表的旧制卡)。
    const exact = new Map()      // "SYS:系统|tableCode" → card
    const sysFallback = new Map() // "SYS:系统" → card(tableCode 为空)
    for (const c of (cardPage?.records || [])) {
      if (!CARD_OK.includes(c.cardStatus)) continue
      if (c.rightType !== props.rightType) continue
      const key = c.assetId + '|' + (c.tableCode || '')
      if (!exact.has(key)) exact.set(key, c)
      if (!c.tableCode && !sysFallback.has(c.assetId)) sysFallback.set(c.assetId, c)
    }
    const cardFor = (assetId, tableCode) => exact.get(assetId + '|' + tableCode) || sysFallback.get(assetId)

    const leaves = []
    collectLeaves(tree, '', '', '', leaves)
    let candidates = leaves
      .map(l => {
        const assetId = 'SYS:' + l.sysName // 卡片/开放目录/确权事实统一以 SYS:系统 为键
        const card = cardFor(assetId, l.tableCode)
        return card ? { ...l, assetId, card } : null
      })
      .filter(Boolean)
    // 经营权:对外开放目录前置裁剪(后端权威,键=SYS:系统,与 saveDraft 一致)
    if (props.rightType === RIGHT_OPERATION && candidates.length) {
      const open = await grantableOpenFilter([...new Set(candidates.map(c => c.assetId))])
      const openSet = new Set(open || [])
      candidates = candidates.filter(c => openSet.has(c.assetId))
    }
    treeData.value = regroup(candidates)
  } catch (e) {
    treeData.value = []
  } finally {
    loading.value = false
  }
}

function onCheck() {
  const leaves = treeRef.value.getCheckedNodes(false).filter(n => n.type === 'table')
  emit('change', leaves)
}

watch(kw, (v) => treeRef.value?.filter(v))
watch(() => props.rightType, () => build(), { immediate: true })
function filterNode(value, data) { return !value || (data.name || '').includes(value) }
</script>

<style scoped>
.cat-tree { height: 100%; display: flex; flex-direction: column; }
.cat-tree-hd { font-size: 12.5px; font-weight: 600; color: var(--prm-color-text); margin-bottom: 8px; }
.cat-tree :deep(.el-tree) { flex: 1; overflow: auto; background: var(--prm-color-bg); border: 1px solid var(--prm-color-bg); border-radius: 6px; padding: 6px; max-height: 420px; }
.cat-count { margin-left: 6px; font-size: 12px; color: var(--prm-color-text-weak); }
.is-table { color: var(--prm-color-link); }
.leaf-card { margin-left: 8px; font-size: 12px; color: var(--prm-color-success); }
</style>
