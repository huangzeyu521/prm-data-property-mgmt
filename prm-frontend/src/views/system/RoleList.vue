<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="角色目录与系统权限校验(RBAC)硬绑定,为只读;角色分配请在「用户管理」中完成。"
      style="margin-bottom: 16px"
    />

    <div class="prm-table-card">
      <el-table :data="roles" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="code" label="角色编码" width="120">
          <template #default="{ row }"><span :class="'prm-c-' + ((tagType(row.code)) || 'primary')">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="角色名称" min-width="140" />
        <el-table-column prop="description" label="权限说明" min-width="320" show-overflow-tooltip />
        <el-table-column prop="userCount" label="用户数" width="100" align="center">
          <template #default="{ row }"><span class="prm-c-primary">{{ row.userCount }}</span></template>
        </el-table-column>
      </el-table>
      <PageNote>注:用户数为当前各角色在册启用/停用账号合计。</PageNote>
    </div>
  </div>
</template>

<script setup>
import PageNote from '@/components/PageNote.vue'
import { ref, onMounted } from 'vue'
import { listRoles } from '@/api/system'

const roles = ref([])
const loading = ref(false)

function tagType(code) {
  return { admin: 'danger', all: 'danger', review: 'warning', apply: 'primary', view: 'info' }[code] || ''
}

async function load() {
  loading.value = true
  try {
    roles.value = (await listRoles()) || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.prm-page { padding: 16px; }
.prm-table-card { background: #fff; padding: 16px; border-radius: var(--prm-radius); }
.prm-table-note { margin-top: 12px; font-size: 12px; color: var(--prm-color-text-weak); }
</style>
