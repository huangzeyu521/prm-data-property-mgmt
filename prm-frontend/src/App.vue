<template>
  <!-- 智能确权辅助工具:独立外壳,不渲染主平台布局 -->
  <router-view v-if="isAitool" />
  <el-container v-else style="height: 100%">
    <el-header class="prm-header">
      <el-button text class="prm-collapse-btn" @click="collapse = !collapse">
        <el-icon :size="18"><component :is="collapse ? 'Expand' : 'Fold'" /></el-icon>
      </el-button>
      <span class="prm-logo"><span class="prm-logo-csg">中国南方电网</span><span class="prm-logo-name">数据资产管理平台</span></span>
      <!-- 平台一级导航模拟(评审2):数据产权管理为平台内一级子菜单,非独立工具;其余为平台既有功能集成位 -->
      <nav class="prm-plat-nav">
        <el-tooltip v-for="m in PLAT_MENUS" :key="m" content="平台既有功能(集成位,待与数据资产管理平台对接)" placement="bottom">
          <span class="plat-item disabled">{{ m }}</span>
        </el-tooltip>
        <span class="plat-item active">数据产权管理</span>
      </nav>
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
      <el-tooltip content="独立智能工具,新标签打开(业务流程亦可带上下文调用)" placement="bottom">
        <el-button text class="prm-ait-btn" @click="openAitool">
          <el-icon :size="16"><MagicStick /></el-icon><span>智能确权辅助工具</span>
        </el-button>
      </el-tooltip>
      <work-guide />
      <notification-center />
    </el-header>
    <el-container>
      <el-aside :width="collapse ? '64px' : '220px'" class="prm-aside">
        <el-menu :default-active="$route.path" :default-openeds="openeds" :collapse="collapse" unique-opened router>
          <el-menu-item index="/dpr/workbench/todo" class="prm-top-item"><el-icon><Compass /></el-icon><span>统一待办中心</span></el-menu-item>
          <el-sub-menu index="01">
            <template #title><el-icon><Document /></el-icon><span>产权信息管理</span></template>
            <el-menu-item index="/dpr/ledger/overview">产权台账概览</el-menu-item>
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
            <el-menu-item index="/dpr/confirm/wizard">⭐ 确权申请</el-menu-item>
            <el-menu-item index="/dpr/confirm/guidance">确权指引管理</el-menu-item>
            <el-menu-item index="/dpr/confirm/catalog">确权目录管理</el-menu-item>
            <el-menu-item index="/dpr/confirm/history">申请查询</el-menu-item>
            <el-menu-item index="/dpr/confirm/review">审核申请提交</el-menu-item>
            <el-menu-item index="/dpr/confirm/card">权益卡片生成</el-menu-item>
            <el-menu-item index="/dpr/confirm/cert">权益证书管理</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="04">
            <template #title><el-icon><Connection /></el-icon><span>数据授权管理</span></template>
            <!-- 办事:一站式向导 -->
            <el-menu-item index="/dpr/auth/wizard">⭐ 一事一议授权申请</el-menu-item>
            <el-menu-item index="/dpr/auth/batch-wizard">⭐ 批量授权申请</el-menu-item>
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
            <el-menu-item index="/dpr/auth/filing">对外经营权授权备案</el-menu-item>
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
            <el-menu-item index="/dpr/dashboard/overview">数据产权全景</el-menu-item>
            <el-menu-item index="/dpr/dashboard/confirm">确权看板</el-menu-item>
            <el-menu-item index="/dpr/dashboard/auth">授权看板</el-menu-item>
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
          <span v-if="pageGoal" class="prm-goal">本页目标:{{ pageGoal }}</span>
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

// 智能确权辅助工具走独立外壳(AitoolShell),不渲染主平台布局
const isAitool = computed(() => route.path.startsWith('/aitool'))

// 顶栏调用入口:新标签打开独立工具
function openAitool() {
  window.open('/aitool/material', '_blank')
}

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
  '/dpr/dashboard': '综合分析管理'
}

// 当前一级分组名(面包屑第二级)
const groupName = computed(() => {
  if (AUTH_CONFIG.includes(route.path)) return '授权配置'
  const hit = Object.keys(GROUP_NAMES).find((k) => route.path.startsWith(k))
  return hit ? GROUP_NAMES[hit] : ''
})

// 当前页面名(面包屑第三级)
const pageTitle = computed(() => route.meta.title || '')

// 本页目标(评审1:讲清用户在每个页面要达成什么)
const pageGoal = computed(() => route.meta.goal || '')

// 平台既有功能集成位(评审2:体现"平台内一级子菜单"而非独立工具)
const PLAT_MENUS = ['数据资产目录', '数据资产卡片', '数据服务']

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
  return ['01']
})
</script>

<style scoped>
/* 顶栏:白底(数研院典型界面),左 logo+蓝色平台名,水平平台导航,右侧深色图标组 */
.prm-header {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  color: var(--prm-color-text);
  border-bottom: 1px solid #e8e8e8;
}
.prm-collapse-btn { color: #666; padding: 4px; }
.prm-collapse-btn:hover { color: var(--prm-color-primary); }
.prm-logo { display: flex; flex-direction: column; line-height: 1.15; }
.prm-logo-csg { font-size: 11px; color: #8c8c8c; letter-spacing: 1px; }
.prm-logo-name { font-size: 17px; font-weight: 700; color: var(--prm-color-primary); white-space: nowrap; }
/* 平台一级导航:水平菜单,激活项蓝字+底部蓝条(典型界面顶部导航) */
.prm-plat-nav { display: flex; align-items: stretch; gap: 2px; margin-left: 20px; height: 100%; }
.plat-item {
  font-size: 14px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  white-space: nowrap;
  position: relative;
}
.plat-item.disabled { color: #8c8c8c; cursor: not-allowed; }
.plat-item.active { color: var(--prm-color-primary); font-weight: 600; }
.plat-item.active::after {
  content: "";
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 0;
  height: 2px;
  background: var(--prm-color-primary);
}
.prm-goal { margin-left: auto; font-size: 12px; color: #8c8c8c; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 52%; }
.prm-spacer { flex: 1; }
.prm-search { width: 260px; }
.prm-ait-btn { color: var(--prm-color-primary); font-size: 13px; }
.prm-ait-btn:hover { color: var(--prm-color-primary-light); }
/* 侧栏:深蓝渐变+白字菜单(典型界面左导航),激活项亮蓝高亮 */
.prm-aside {
  background: linear-gradient(180deg, #2b62d9 0%, #1d4ab4 55%, #16357f 100%);
  transition: width 0.25s;
  overflow-x: hidden;
}
.prm-aside :deep(.el-menu) {
  border-right: none;
  background: transparent;
  --el-menu-text-color: rgba(255, 255, 255, 0.85);
  --el-menu-hover-text-color: #fff;
  --el-menu-active-color: #fff;
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.12);
}
.prm-aside :deep(.el-sub-menu .el-menu) { background: rgba(0, 0, 0, 0.12); }
.prm-aside :deep(.el-menu-item.is-active) {
  background: var(--prm-color-primary);
  color: #fff;
  position: relative;
}
.prm-aside :deep(.el-menu-item.is-active)::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: #82b2ff;
}
.prm-aside :deep(.el-sub-menu__title:hover),
.prm-aside :deep(.el-menu-item:hover) { background: rgba(255, 255, 255, 0.12); }
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
