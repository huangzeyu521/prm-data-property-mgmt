<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <el-container class="ait-shell">
    <el-header class="ait-header">
      <el-icon :size="22" class="ait-logo-icon"><MagicStick /></el-icon>
      <span class="ait-title">智能确权辅助工具</span>
      <span class="ait-sub">独立智能工具 · 可由业务流程带上下文调用</span>
      <el-menu mode="horizontal" :default-active="$route.path" class="ait-nav" :ellipsis="false">
        <el-menu-item v-for="t in TABS" :key="t.path" :index="t.path" @click="go(t.path)">
          {{ t.label }}
        </el-menu-item>
      </el-menu>
      <div class="ait-spacer"></div>
      <span v-if="ctx" class="ait-ctx prm-c-success">调用上下文:{{ ctx }}</span>
      <el-link class="ait-back" :underline="false" @click="backToPlatform">
        <el-icon><Back /></el-icon> 返回数据产权管理
      </el-link>
    </el-header>
    <el-main class="ait-main">
      <router-view :key="$route.fullPath" />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const TABS = [
  { path: '/aitool/material', label: '材料智能解析' },
  { path: '/aitool/conflict', label: '权属冲突识别' },
  { path: '/aitool/decision', label: '确权决策支持' }
]

// 页签切换保留调用方传入的上下文(applyId/assetId)
function go(path) {
  if (path !== route.path) router.push({ path, query: route.query })
}

const ctx = computed(() => {
  const { applyId, assetId } = route.query
  if (applyId) return '申请 ' + String(applyId).slice(0, 12) + (String(applyId).length > 12 ? '…' : '')
  if (assetId) return '资产 ' + assetId
  return ''
})

function backToPlatform() {
  router.push('/dpr/dashboard/overview')
}
</script>

<style scoped>
/* 对齐数研院典型界面母版:主蓝顶栏 + 白色标题 + 白下划线页签 */
.ait-shell { height: 100%; }
.ait-header {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 56px;
  padding: 0 20px;
  color: #fff;
  background: var(--prm-color-primary, #1e87f0);
  border-bottom: none;
}
.ait-logo-icon { color: #fff; }
.ait-title { font-size: 17px; font-weight: 700; color: #fff; white-space: nowrap; }
.ait-sub { font-size: 12px; color: rgba(255, 255, 255, 0.75); white-space: nowrap; }
.ait-nav {
  margin-left: 16px;
  background: transparent;
  border-bottom: none;
  height: 100%;
}
.ait-nav :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.8);
  border-bottom: 2px solid transparent;
}
.ait-nav :deep(.el-menu-item.is-active) {
  color: #fff;
  font-weight: 600;
  border-bottom-color: #fff;
  background: transparent;
}
.ait-nav :deep(.el-menu-item:hover) { background: rgba(255, 255, 255, 0.12); color: #fff; }
.ait-spacer { flex: 1; }
.ait-ctx { white-space: nowrap; }
.ait-back { color: #fff; white-space: nowrap; }
.ait-back:hover { color: rgba(255, 255, 255, 0.8); }
.ait-main { padding: 0; overflow: auto; background: var(--prm-color-bg, #f5f5f6); }
</style>
