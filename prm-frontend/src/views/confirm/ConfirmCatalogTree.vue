<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!--
  确权范围树(系统级确权入口):业务域 → 系统 → 一级功能模块 → 库表(卡片),懒加载。
  漏斗收敛:选系统 → 多选功能模块(勾选自动级联其库表)→ 多选库表。一份申请约束在一个系统内。
  勾选库表后向上 emit('select', sysName, tableCodes),父组件据此 cardsBySystem 取全量元数据填入 basket。
-->
<template>
  <div class="cat-tree">
    <div class="cat-tree-hd">{{ hdText }}</div>
    <!-- 一期范围边界提示(初始确权/确权变更共用组件,两处均显示,口径一致):仅原始数据确权,纯提示不拦截 -->
    <el-alert type="info" :closable="false" show-icon class="cat-scope-note"
      title="本期仅支持「原始数据」确权"
      description="明细数据、宽表数据(含跨系统来源的库表)将于二期支持,暂不在本期确权范围内。" />
    <el-input v-model="kw" size="small" :placeholder="isChange ? '搜索系统/模块/库表' : '搜索系统'" clearable style="margin-bottom:8px" />
    <!-- 单系统锁定提示(35号文:一份确权申请=一个系统);锁定后其它系统及业务域置灰不可选,换系统先清空 -->
    <div v-if="lockedSys" class="cat-lock">
      <span>已锁定系统:<b>{{ lockedSys }}</b></span>
      <el-button link type="primary" @click="clearAll">清空换系统</el-button>
    </div>
    <el-tree
      ref="treeRef" :props="treeProps" :load="loadNode" lazy node-key="id"
      show-checkbox :filter-node-method="filterNode" :expand-on-click-node="false" @check="onCheck">
      <template #default="{ data }">
        <span class="cat-node" :class="['lvl-' + data.type, { 'is-table': data.type === 'table' }]">{{ data.name }}</span>
        <span v-if="data.type !== 'table'" class="cat-count">{{ data.count }}</span>
        <!-- 系统级确权进度徽标(B):已确权 N/M —— 仅确权变更模式显示;初始确权树只列未确权范围,不显确权状态 -->
        <span v-if="isChange && data.type === 'system' && data.totalCount" :class="'auth-badge prm-c-' + ((data.confirmedCount >= data.totalCount ? 'success' : (data.confirmedCount > 0 ? 'warning' : 'info')) || 'primary')">
          {{ data.confirmedCount >= data.totalCount ? '已确权' : (data.confirmedCount > 0 ? '部分确权' : '未确权') }} {{ data.confirmedCount }}/{{ data.totalCount }}
        </span>
        <!-- 确权变更模式(C):叶子区分 已确权(可改基线)/新增(纳入确权) -->
        <span v-if="data.type === 'table' && isChange" :class="'auth-badge prm-c-' + ((data.confirmed ? 'success' : 'primary') || 'primary')">
          {{ data.confirmed ? '已确权' : '未确权' }}
        </span>
        <span v-if="data.type === 'table' && data.authorized" class="auth-badge prm-c-danger">已授权</span>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { catalogTree } from '@/api/assetCard'

const props = defineProps({
  // unconfirmed=仅未确权(初始确权);''=全部(确权变更:已确权可改基线 + 未确权可作"数据新增"纳入)
  status: { type: String, default: '' }
})
// 确权变更模式:status 为空(显示全部)时,叶子区分已确权/新增
const isChange = computed(() => props.status === '')
// 标题:初始确权=系统级整体确权(35号文 表1 一系统一行,不下钻库表);确权变更=可逐表/逐模块
const hdText = computed(() => isChange.value ? '确权范围(选系统 → 选模块 → 选库表)' : '确权范围(选择一个系统 · 系统级整体确权)')
const emit = defineEmits(['select'])
const treeRef = ref()
const kw = ref('')
const lockedSys = ref('')
const treeProps = { label: 'name', isLeaf: 'leaf', children: 'children', disabled: (data, node) => nodeDisabled(data, node) }

// 单系统硬约束(事前):业务域=纯导航不可勾;已锁定系统后,其它系统及其子树置灰不可选。
// EP 的 props.disabled 函数在节点渲染时求值,读取 lockedSys.value → Vue 登记为依赖,锁变即时重渲染置灰。
function nodeDisabled(data, node) {
  if (data.type === 'domain') return true
  // 初始确权=系统级:模块/库表不可勾(下钻已被 leaf 关闭,此为兜底)。确权变更才允许逐表/逐模块。
  if (!isChange.value && (data.type === 'module' || data.type === 'table')) return true
  if (!lockedSys.value) return false
  return systemNameOf(data, node) !== lockedSys.value
}
// 任意节点上溯所属系统名(system 自身 / module→system / table→module→system)
function systemNameOf(data, node) {
  let n = node
  while (n && n.data && n.data.type !== 'system') n = n.parent
  return n?.data?.name || ''
}
// 清空选择并解锁(换系统):取消全部勾选 + 复位 lockedSys + 通知父组件清空
function clearAll() {
  treeRef.value?.setCheckedKeys([])
  lockedSys.value = ''
  emit('select', null, [], '', {})
}

async function loadNode(node, resolve) {
  const type = node.level === 0 ? 'root' : node.data.type
  const id = node.level === 0 ? '' : node.data.id
  try {
    const children = (await catalogTree(type, id, props.status)) || []
    // 初始确权=系统级:系统节点设为叶子,关闭下钻到模块/库表(35号文 表1 一系统一行,整系统纳入)。
    // 确权变更保持可展开,因变更天然到表粒度(数据新增/改基线)。
    if (!isChange.value) children.forEach((c) => { if (c.type === 'system') c.leaf = true })
    resolve(children)
  } catch (e) { resolve([]) }
}

// 库表叶子归属的系统(table → module → system)
function sysOf(t) {
  return treeRef.value.getNode(t.id)?.parent?.parent?.data?.name
}
// 模块归属的系统(module → system)
function sysOfMod(m) {
  return treeRef.value.getNode(m.id)?.parent?.data?.name
}

// 单系统硬约束:从「带系统归属的勾选节点」里保留一个系统,其余 setChecked(false) 取消并提示。
// 返回保留的系统名;keyFn 取节点的系统名。
function enforceSingleSystem(nodes, keyFn) {
  const bySys = {}
  nodes.forEach((n) => { const s = keyFn(n); if (s) (bySys[s] = bySys[s] || []).push(n) })
  const systems = Object.keys(bySys)
  if (!systems.length) return { keep: '', bySys }
  const keep = (lockedSys.value && bySys[lockedSys.value]) ? lockedSys.value : systems[0]
  if (systems.length > 1) {
    nodes.filter((n) => keyFn(n) !== keep).forEach((n) => treeRef.value.setChecked(n.id, false))
    ElMessage.warning(`一份确权申请只能选一个系统;已保留「${keep}」,其它系统已自动取消`)
  }
  return { keep, bySys }
}

// 勾选变化:三种驱动(优先级 叶子 > 系统节点 > 模块节点),均施加单系统硬约束。
// 懒加载下勾「收起的系统/模块」父节点时子库表尚未加载,无叶子可数 —— 故走整系统/整模块路径,
// 由父组件凭 cardsBySystem 解析全量库表(不依赖 DOM 是否加载)。
function onCheck() {
  const checked = treeRef.value.getCheckedNodes(false)
  // 路径①:已勾库表叶子 —— 逐表(展开后级联或手勾)
  const tables = checked.filter((n) => n.type === 'table')
  if (tables.length) {
    const { keep, bySys } = enforceSingleSystem(tables, sysOf)
    lockedSys.value = keep
    const firstId = (bySys[keep] || [])[0]?.id
    const domain = firstId ? treeRef.value.getNode(firstId)?.parent?.parent?.parent?.data?.name : ''
    emit('select', keep, (bySys[keep] || []).map((t) => t.id), domain || '', {})
    return
  }
  // 路径②:无叶子但勾了「系统」节点(收起)—— 整系统:该系统全部未确权库表
  const sysNodes = checked.filter((n) => n.type === 'system')
  if (sysNodes.length) {
    const { keep } = enforceSingleSystem(sysNodes, (n) => n.name)
    lockedSys.value = keep
    const keepNode = sysNodes.find((n) => n.name === keep)
    const domain = keepNode ? treeRef.value.getNode(keepNode.id)?.parent?.data?.name : ''
    emit('select', keep, [], domain || '', { wholeSystem: true })
    return
  }
  // 路径③:无叶子但勾了「功能模块」节点(收起)—— 整模块:这些模块下全部未确权库表
  const modNodes = checked.filter((n) => n.type === 'module')
  if (modNodes.length) {
    const { keep, bySys } = enforceSingleSystem(modNodes, sysOfMod)
    lockedSys.value = keep
    const keepMods = bySys[keep] || []
    const domain = keepMods[0] ? treeRef.value.getNode(keepMods[0].id)?.parent?.parent?.data?.name : ''
    emit('select', keep, [], domain || '', { modules: keepMods.map((m) => m.name) })
    return
  }
  // 全空
  lockedSys.value = ''
  emit('select', null, [], '', {})
}

watch(kw, (v) => treeRef.value?.filter(v))
function filterNode(value, data) { return !value || (data.name || '').includes(value) }
</script>

<style scoped>
.cat-tree { height: 100%; display: flex; flex-direction: column; }
.cat-tree-hd { font-size: 12.5px; font-weight: 600; color: var(--prm-color-text); margin-bottom: 8px; }
/* 一期范围边界提示:紧凑收窄,贴合面板节奏 */
.cat-scope-note { margin-bottom: 8px; padding: 6px 10px; }
.cat-scope-note :deep(.el-alert__title) { font-size: 12px; font-weight: 600; }
.cat-scope-note :deep(.el-alert__description) { font-size: 11.5px; margin: 2px 0 0; line-height: 1.5; }
.cat-tree :deep(.el-tree) { flex: 1; overflow: auto; background: var(--prm-color-bg); border: 1px solid var(--prm-color-bg); border-radius: 6px; padding: 6px; max-height: 560px; }
.cat-count { margin-left: 6px; font-size: 12px; color: var(--prm-color-text-weak); }
.is-table { color: var(--prm-color-link); }
/* 业务域=纯导航,隐藏其勾选框(现代 Chromium :has;nodeDisabled 兜底置灰) */
.cat-tree :deep(.el-tree-node__content:has(.lvl-domain)) > .el-checkbox { display: none; }
/* 单系统锁定提示条 */
.cat-lock { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 8px; padding: 6px 10px; font-size: 12px; color: var(--prm-color-text-secondary); background: var(--prm-color-selected-bg); border: 1px solid var(--prm-color-border); border-radius: 4px; }
.cat-lock b { color: var(--prm-color-primary); }
.auth-badge { margin-left: 6px; transform: scale(0.85); }
</style>
