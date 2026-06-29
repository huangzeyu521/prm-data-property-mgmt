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
    <div class="cat-tree-hd">确权范围(选系统 → 选模块 → 选库表)</div>
    <el-input v-model="kw" size="small" placeholder="搜索系统/模块/库表" clearable style="margin-bottom:8px" />
    <el-tree
      ref="treeRef" :props="treeProps" :load="loadNode" lazy node-key="id"
      show-checkbox :filter-node-method="filterNode" :expand-on-click-node="false" @check="onCheck">
      <template #default="{ data }">
        <span :class="{ 'is-table': data.type === 'table' }">{{ data.name }}</span>
        <span v-if="data.type !== 'table'" class="cat-count">{{ data.count }}</span>
        <!-- 系统级确权进度徽标(B):已确权 N/M —— 仅确权变更模式显示;初始确权树只列未确权范围,不显确权状态 -->
        <el-tag v-if="isChange && data.type === 'system' && data.totalCount"
          size="small" :type="data.confirmedCount >= data.totalCount ? 'success' : (data.confirmedCount > 0 ? 'warning' : 'info')"
          effect="plain" class="auth-badge">
          {{ data.confirmedCount >= data.totalCount ? '已确权' : (data.confirmedCount > 0 ? '部分确权' : '未确权') }} {{ data.confirmedCount }}/{{ data.totalCount }}
        </el-tag>
        <!-- 确权变更模式(C):叶子区分 已确权(可改基线)/新增(纳入确权) -->
        <el-tag v-if="data.type === 'table' && isChange" size="small" :type="data.confirmed ? 'success' : 'primary'" effect="plain" class="auth-badge">
          {{ data.confirmed ? '已确权' : '新增' }}
        </el-tag>
        <el-tag v-if="data.type === 'table' && data.authorized" size="small" type="danger" effect="plain" class="auth-badge">已授权</el-tag>
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
const emit = defineEmits(['select'])
const treeRef = ref()
const kw = ref('')
const lockedSys = ref('')
const treeProps = { label: 'name', isLeaf: 'leaf', children: 'children' }

async function loadNode(node, resolve) {
  const type = node.level === 0 ? 'root' : node.data.type
  const id = node.level === 0 ? '' : node.data.id
  try {
    const children = (await catalogTree(type, id, props.status)) || []
    resolve(children)
  } catch (e) { resolve([]) }
}

// 库表叶子归属的系统(table → module → system)
function sysOf(t) {
  return treeRef.value.getNode(t.id)?.parent?.parent?.data?.name
}

// 勾选变化:以「已勾库表叶子」为主驱动(可靠),收集选中库表;单系统硬约束——
// 选了第二个系统则自动取消其库表勾选,只保留一个系统,再向上 emit。
function onCheck() {
  const tables = treeRef.value.getCheckedNodes(false).filter((n) => n.type === 'table')
  if (!tables.length) { lockedSys.value = ''; emit('select', null, []); return }
  const bySys = {}
  tables.forEach((t) => { const s = sysOf(t); if (s) (bySys[s] = bySys[s] || []).push(t) })
  const systems = Object.keys(bySys)
  let keep = (lockedSys.value && bySys[lockedSys.value]) ? lockedSys.value : systems[0]
  if (systems.length > 1) {
    // 取消其它系统库表(setChecked 会联动其父模块/系统的勾选/半选态)
    tables.filter((t) => sysOf(t) !== keep).forEach((t) => treeRef.value.setChecked(t.id, false))
    ElMessage.warning(`一份确权申请只能选一个系统;已保留「${keep}」,其它系统已自动取消`)
  }
  lockedSys.value = keep
  // 业务域(table → module → system → domain),供父组件派生管制属性默认
  const firstId = (bySys[keep] || [])[0]?.id
  const domain = firstId ? treeRef.value.getNode(firstId)?.parent?.parent?.parent?.data?.name : ''
  emit('select', keep, (bySys[keep] || []).map((t) => t.id), domain || '')
}

watch(kw, (v) => treeRef.value?.filter(v))
function filterNode(value, data) { return !value || (data.name || '').includes(value) }
</script>

<style scoped>
.cat-tree { height: 100%; display: flex; flex-direction: column; }
.cat-tree-hd { font-size: 12.5px; font-weight: 600; color: var(--prm-color-text); margin-bottom: 8px; }
.cat-tree :deep(.el-tree) { flex: 1; overflow: auto; background: var(--prm-color-bg); border: 1px solid var(--prm-color-bg); border-radius: 6px; padding: 6px; max-height: 560px; }
.cat-count { margin-left: 6px; font-size: 12px; color: var(--prm-color-text-weak); }
.is-table { color: var(--prm-color-link); }
.auth-badge { margin-left: 6px; transform: scale(0.85); }
</style>
