<template>
  <el-container style="height: 100%">
    <el-header class="prm-header">
      <el-button text class="prm-collapse-btn" @click="collapse = !collapse">
        <el-icon :size="18"><component :is="collapse ? 'Expand' : 'Fold'" /></el-icon>
      </el-button>
      <span class="prm-logo">数据资产管理平台</span>
      <span class="prm-module">数据产权管理</span>
      <div class="prm-spacer"></div>
      <el-select
        v-model="jump"
        filterable
        clearable
        placeholder="搜索菜单 / 页面快速跳转"
        class="prm-search"
        @change="onJump"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
        <el-option v-for="p in pages" :key="p.value" :label="p.label" :value="p.value" />
      </el-select>
      <work-guide />
      <notification-center />
    </el-header>
    <el-container>
      <el-aside :width="collapse ? '64px' : '220px'" class="prm-aside">
        <el-menu :default-active="$route.path" :default-openeds="openeds" :collapse="collapse" unique-opened router>
          <el-menu-item index="/dpr/workbench/todo" class="prm-top-item"><el-icon><Compass /></el-icon><span>统一待办中心</span></el-menu-item>
          <el-sub-menu index="01">
            <template #title><el-icon><Document /></el-icon><span>产权信息管理</span></template>
            <el-menu-item index="/dpr/ledger/overview">产权台账概览(含产权树)</el-menu-item>
            <el-menu-item index="/dpr/ledger/archive">数据集产权档案管理</el-menu-item>
            <el-menu-item index="/dpr/ledger/change">产权变更记录管理</el-menu-item>
            <el-menu-item index="/dpr/ledger/statistics">产权台账统计分析</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="02">
            <template #title><el-icon><Monitor /></el-icon><span>权益动态监测</span></template>
            <el-menu-item index="/dpr/monitor/status">权益状态监控</el-menu-item>
            <el-menu-item index="/dpr/monitor/alert">权益变动监测预警</el-menu-item>
            <el-menu-item index="/dpr/monitor/compliance">合规性检查</el-menu-item>
            <el-menu-item index="/dpr/monitor/rule">监测规则配置</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="03">
            <template #title><el-icon><Stamp /></el-icon><span>数据确权管理</span></template>
            <el-menu-item index="/dpr/confirm/wizard">⭐ 确权申请(一站式)</el-menu-item>
            <el-menu-item index="/dpr/confirm/guidance">确权指引管理</el-menu-item>
            <el-menu-item index="/dpr/confirm/catalog">确权目录管理</el-menu-item>
            <el-menu-item index="/dpr/confirm/history">申请查询(进度/历史)</el-menu-item>
            <el-menu-item index="/dpr/confirm/review">审核申请提交</el-menu-item>
            <el-menu-item index="/dpr/confirm/card">权益卡片生成</el-menu-item>
            <el-menu-item index="/dpr/confirm/cert">权益证书管理</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="04">
            <template #title><el-icon><Connection /></el-icon><span>数据授权管理</span></template>
            <!-- 办事:一站式向导 -->
            <el-menu-item index="/dpr/auth/wizard">⭐ 一事一议授权申请(一站式)</el-menu-item>
            <el-menu-item index="/dpr/auth/batch-wizard">⭐ 批量授权申请(一站式)</el-menu-item>
            <!-- 过程:清单/校验/查询/审核 -->
            <el-menu-item index="/dpr/auth/batch-list">批量授权清单</el-menu-item>
            <el-menu-item index="/dpr/auth/compliance">合规校验管理</el-menu-item>
            <el-menu-item index="/dpr/auth/history">申请历史查询</el-menu-item>
            <el-menu-item index="/dpr/auth/review">授权审核提交</el-menu-item>
            <!-- 协议:签章/审核/存档 -->
            <el-menu-item index="/dpr/auth/agreement-seal">协议签章上传</el-menu-item>
            <el-menu-item index="/dpr/auth/agreement-review">协议审核提交</el-menu-item>
            <el-menu-item index="/dpr/auth/agreement-archive">协议存档管理</el-menu-item>
            <!-- 权益:证书/模板/备案 -->
            <el-menu-item index="/dpr/auth/cert">授权权益管理</el-menu-item>
            <el-menu-item index="/dpr/auth/cert-template">授权权益证书模板管理</el-menu-item>
            <el-menu-item index="/dpr/auth/filing">对外经营权授权备案(附录G)</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="07">
            <template #title><el-icon><Setting /></el-icon><span>授权配置</span></template>
            <el-menu-item index="/dpr/auth/guidance">授权指引管理</el-menu-item>
            <el-menu-item index="/dpr/auth/form-template">申请表单设计</el-menu-item>
            <el-menu-item index="/dpr/auth/scenario">应用场景管理</el-menu-item>
            <el-menu-item index="/dpr/auth/agreement-template">协议模板库</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="05">
            <template #title><el-icon><DataAnalysis /></el-icon><span>综合分析管理</span></template>
            <el-menu-item index="/dpr/dashboard/overview">数据产权全景(综合)</el-menu-item>
            <el-menu-item index="/dpr/dashboard/confirm">确权看板</el-menu-item>
            <el-menu-item index="/dpr/dashboard/auth">授权看板</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="06">
            <template #title><el-icon><MagicStick /></el-icon><span>智能确权辅助工具</span></template>
            <el-menu-item index="/dpr/aitool/material">材料智能解析</el-menu-item>
            <el-menu-item index="/dpr/aitool/conflict">权属冲突识别</el-menu-item>
            <el-menu-item index="/dpr/aitool/decision">确权决策支持</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      <el-main class="prm-main">
        <div class="prm-crumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dpr/dashboard/overview' }">数据产权管理</el-breadcrumb-item>
            <el-breadcrumb-item v-if="groupName">{{ groupName }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="prm-content">
          <router-view :key="$route.fullPath" />
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NotificationCenter from '@/components/NotificationCenter.vue'
import WorkGuide from '@/components/WorkGuide.vue'

const route = useRoute()
const router = useRouter()
const collapse = ref(false)
const jump = ref('')

// 全部可跳转页面(供顶部搜索),取自路由表 meta.title
const pages = router.getRoutes()
  .filter((r) => r.meta && r.meta.title)
  .map((r) => ({ value: r.path, label: r.meta.title }))

function onJump(path) {
  if (path) {
    router.push(path)
    jump.value = ''
  }
}

const AUTH_CONFIG = ['/dpr/auth/guidance', '/dpr/auth/form-template', '/dpr/auth/scenario', '/dpr/auth/agreement-template']

const GROUP_NAMES = {
  '/dpr/ledger': '产权信息管理',
  '/dpr/monitor': '权益动态监测',
  '/dpr/confirm': '数据确权管理',
  '/dpr/auth': '数据授权管理',
  '/dpr/dashboard': '综合分析管理',
  '/dpr/aitool': '智能确权辅助工具'
}

// 当前一级分组名(面包屑第二级)
const groupName = computed(() => {
  if (AUTH_CONFIG.includes(route.path)) return '授权配置'
  const hit = Object.keys(GROUP_NAMES).find((k) => route.path.startsWith(k))
  return hit ? GROUP_NAMES[hit] : ''
})

// 当前页面名(面包屑第三级)
const pageTitle = computed(() => route.meta.title || '')

// 手风琴:根据当前路由自动展开所属一级分组(unique-opened 负责切换时互斥折叠)
const openeds = computed(() => {
  const p = route.path
  if (p.startsWith('/dpr/workbench')) return []   // 待办为顶层项,不展开任何组
  if (p.startsWith('/dpr/ledger')) return ['01']
  if (p.startsWith('/dpr/monitor')) return ['02']
  if (p.startsWith('/dpr/confirm')) return ['03']
  if (AUTH_CONFIG.includes(p)) return ['07']
  if (p.startsWith('/dpr/auth')) return ['04']
  if (p.startsWith('/dpr/dashboard')) return ['05']
  if (p.startsWith('/dpr/aitool')) return ['06']
  return ['01']
})
</script>

<style scoped>
.prm-header {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #1f2329;
  color: #fff;
}
.prm-collapse-btn { color: #c9cdd4; padding: 4px; }
.prm-collapse-btn:hover { color: #fff; }
.prm-logo { font-size: 18px; font-weight: 600; }
.prm-module { font-size: 14px; color: #c9cdd4; }
.prm-spacer { flex: 1; }
.prm-search { width: 260px; }
.prm-aside { background: #fff; border-right: 1px solid var(--prm-color-border); transition: width 0.25s; overflow-x: hidden; }
/* 折叠态菜单不显示分组右侧多余边框 */
.prm-aside :deep(.el-menu) { border-right: none; }
.prm-main { padding: 0; display: flex; flex-direction: column; }
.prm-crumb {
  position: sticky;
  top: 0;
  z-index: 10;
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid var(--prm-color-border);
}
.prm-content { flex: 1; overflow: auto; }
</style>
