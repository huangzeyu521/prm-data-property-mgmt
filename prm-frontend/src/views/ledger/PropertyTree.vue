<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
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
            <span v-if="data.type === 'DATASET'" style="margin-left: 8px" :class="'prm-c-' + ((statusTag(data.confirmStatus)) || 'primary')">
              {{ data.confirmStatus }}
            </span>
          </span>
        </template>
      </el-tree>
      <PageNote>注:层级为"子公司—系统—模式—数据集",叶子节点标注确权状态。</PageNote>
    </div>
  </div>
</template>

<script setup>
import PageNote from '@/components/PageNote.vue'
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
