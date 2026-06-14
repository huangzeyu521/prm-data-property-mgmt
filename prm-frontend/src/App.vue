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
      <el-tooltip content="按角色裁剪菜单与首屏(无登录环境;生产由 4A 角色映射)" placement="bottom">
        <el-select v-model="role" class="prm-role" size="small" @change="onRoleChange">
          <template #prefix><el-icon><UserFilled /></el-icon></template>
          <el-option v-for="r in ROLES" :key="r.key" :label="r.label" :value="r.key" />
        </el-select>
      </el-tooltip>
      <work-guide />
      <notification-center />
    </el-header>
    <el-container>
      <el-aside :width="collapse ? '64px' : '220px'" class="prm-aside">
        <el-menu :default-active="$route.path" :default-openeds="openeds" :collapse="collapse" unique-opened router>
          <template v-for="node in menu" :key="node.group || node.path">
            <el-menu-item v-if="node.top" :index="node.path" class="prm-top-item">
              <el-icon><component :is="node.icon" /></el-icon><span>{{ node.title }}</span>
            </el-menu-item>
            <el-sub-menu v-else :index="node.index">
              <template #title><el-icon><component :is="node.icon" /></el-icon><span>{{ node.group }}</span></template>
              <el-menu-item v-for="it in node.items" :key="it.path" :index="it.path">{{ it.title }}</el-menu-item>
            </el-sub-menu>
          </template>
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
import { ROLES, ROLE_HOME, currentRole, visibleMenu } from '@/lib/roles'

const route = useRoute()
const router = useRouter()
const collapse = ref(false)
const jump = ref('')

// 角色:无登录环境由顶栏切换器写入 localStorage;默认 all(管理员视图,向后兼容)
const role = ref(currentRole())
const menu = computed(() => visibleMenu(role.value))
function onRoleChange(r) {
  localStorage.setItem('prm-role', r)
  // 切换角色 → 落到该角色个性化首屏
  const home = ROLE_HOME[r] || '/dpr/dashboard/overview'
  if (route.path !== home) router.push(home)
}

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

const AUTH_CONFIG = ['/dpr/auth/guidance', '/dpr/auth/form-template', '/dpr/auth/scenario', '/dpr/auth/agreement-template', '/dpr/auth/cert-template']

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
.prm-role { width: 150px; flex: none; }
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
