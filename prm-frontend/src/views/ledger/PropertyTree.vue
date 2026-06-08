<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-input v-model="keyword" placeholder="输入资产名称检索" clearable style="width: 260px" />
      <el-button type="primary" style="margin-left: 8px" @click="onExpandAll">展开全部</el-button>
      <el-button @click="onCollapseAll">折叠全部</el-button>
    </div>
    <div class="prm-table-card">
      <el-tree
        ref="treeRef"
        :data="tree"
        :props="treeProps"
        node-key="id"
        :filter-node-method="filterNode"
        default-expand-all
        v-loading="loading"
      >
        <template #default="{ data }">
          <span class="tree-node">
            <span>{{ data.label }}</span>
            <el-tag v-if="data.type === 'DATASET'" size="small" :type="statusTag(data.confirmStatus)" style="margin-left: 8px">
              {{ data.confirmStatus }}
            </el-tag>
          </span>
        </template>
      </el-tree>
      <div class="prm-table-note">注:层级为"子公司—系统—模式—数据集",叶子节点标注确权状态。</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { getPropertyTree } from '@/api/ledger'

const treeProps = { label: 'label', children: 'children' }
const tree = ref([])
const loading = ref(false)
const keyword = ref('')
const treeRef = ref()

function statusTag(s) {
  return { 已确权: 'success', 申请中: 'warning', 失败: 'danger' }[s] || 'info'
}
function filterNode(value, data) {
  if (!value) return true
  return (data.label || '').includes(value)
}
function onExpandAll() {
  toggleAll(true)
}
function onCollapseAll() {
  toggleAll(false)
}
function toggleAll(expand) {
  const nodes = treeRef.value?.store?._getAllNodes() || []
  nodes.forEach((n) => (n.expanded = expand))
}

watch(keyword, (v) => treeRef.value?.filter(v))

async function load() {
  loading.value = true
  try {
    tree.value = await getPropertyTree()
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.tree-node { display: inline-flex; align-items: center; }
</style>
