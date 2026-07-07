<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <!-- 登录页 / 智能确权辅助工具:独立外壳,不渲染主平台布局 -->
  <router-view v-if="isAitool || isLogin" />
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
      <el-tooltip content="切换视角(演示):按角色裁剪菜单与首屏;真实角色由登录身份决定" placement="bottom">
        <el-select v-model="role" class="prm-role" size="small" @change="onRoleChange">
          <template #prefix><el-icon><UserFilled /></el-icon></template>
          <el-option v-for="r in ROLES" :key="r.key" :label="r.label" :value="r.key" />
        </el-select>
      </el-tooltip>
      <notification-center />
      <el-dropdown @command="onUserCmd">
        <span class="prm-user"><el-icon><Avatar /></el-icon>{{ userName }}</span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>{{ userName }}（{{ roleLabel }}）</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-header>
    <el-container>
      <el-aside :width="collapse ? '64px' : '220px'" class="prm-aside">
        <el-menu ref="menuRef" :default-active="$route.path" :default-openeds="openeds" :collapse="collapse" unique-opened router>
          <template v-for="node in menu" :key="node.group || node.path">
            <el-menu-item v-if="node.top" :index="node.path" class="prm-top-item">
              <el-icon><component :is="node.icon" /></el-icon><span>{{ node.title }}</span>
            </el-menu-item>
            <el-sub-menu v-else :index="node.index">
              <template #title><el-icon><component :is="node.icon" /></el-icon><span>{{ node.group }}</span></template>
              <el-menu-item v-for="it in node.items" :key="it.path" :index="it.path">
                <span>{{ it.title }}</span>
                <span v-if="draftBadge(it.path) > 0" class="menu-count">{{ draftBadge(it.path) }}</span>
              </el-menu-item>
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
          <div class="prm-crumb-right">
            <span v-if="pageGoal" class="prm-goal" :title="pageGoal">本页目标:{{ pageGoal }}</span>
          </div>
        </div>
        <!-- 统一页面操作区(UI评审#2:按钮不与面包屑同行,置于其下方右上角);吸顶跟随,空则整行隐藏(:empty) -->
        <div id="prm-page-actions" class="prm-page-actions"></div>
        <div class="prm-content">
          <router-view :key="$route.fullPath" />
        </div>
        <!-- 全局页脚(规范 p37 强管制:底部版权信息);登录/aitool 走裸 router-view 不含 -->
        <footer class="prm-footer">© 2026 中国南方电网有限责任公司　数据资产管理平台 V3.6 · 数据产权管理模块</footer>
      </el-main>
    </el-container>
  </el-container>
  <!-- 全局在线文件预览(任意页 openFilePreview 触发) -->
  <FilePreview />
</template>

<script setup>
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NotificationCenter from '@/components/NotificationCenter.vue'
import FilePreview from '@/components/FilePreview.vue'
import { ROLES, ROLE_HOME, currentRole, visibleMenu, canJumpTo } from '@/lib/roles'
import { currentUser, clearSession } from '@/api/auth'
import { confirmDraftCount, refreshConfirmDraftCount, authDraftCount, refreshAuthDraftCount } from '@/lib/draftCount'

const route = useRoute()
const router = useRouter()
const collapse = ref(false)
const jump = ref('')
const isLogin = computed(() => route.path === '/login')

// 角色:登录身份决定真实角色(saveSession 写入 prm-role);顶栏切换器=演示视角
const role = ref(currentRole())
const menu = computed(() => visibleMenu(role.value))
function onRoleChange(r) {
  localStorage.setItem('prm-role', r)
  const home = ROLE_HOME[r] || '/dpr/dashboard/overview'
  if (route.path !== home) router.push(home)
}

// 登录用户展示 + 退出
const userName = computed(() => { const u = currentUser(); return (u && u.realName) || '未登录' })
const roleLabel = computed(() => { const r = ROLES.find((x) => x.key === role.value); return r ? r.label : role.value })
function onUserCmd(cmd) {
  if (cmd === 'logout') {
    clearSession()
    window.location.href = '/login' // 整页跳转,清干净登录态
  }
}

// 智能确权辅助工具为独立部署工具,走独立外壳(AitoolShell),不渲染主平台布局;
// 顶栏入口已移除,仅保留 /aitool 直链时的外壳渲染,不影响数据产权管理模块
const isAitool = computed(() => route.path.startsWith('/aitool'))

// 全部可跳转页面(供顶部搜索),取自路由表 meta.title;排除独立 aitool 页面(顶栏不再暴露其入口)
// 页面快速跳转:按当前角色裁剪(菜单受控页与侧栏一致;非菜单深层页放行),随角色切换而变。
const pages = computed(() => router.getRoutes()
  .filter((r) => r.meta && r.meta.title && !r.path.startsWith('/aitool') && canJumpTo(role.value, r.path))
  .map((r) => ({ value: r.path, label: r.meta.title })))

// 草稿数徽标(确权/授权两个草稿箱):仅在对应「申请草稿箱」对当前角色可见时统计;路由切换/草稿增删事件刷新。
function draftBadge(path) {
  if (path === '/dpr/confirm/draft-box') return confirmDraftCount.value
  if (path === '/dpr/auth/draft-box') return authDraftCount.value
  return 0
}
function maybeRefreshDraftCount() {
  if (isLogin.value) return
  if (canJumpTo(role.value, '/dpr/confirm/draft-box')) refreshConfirmDraftCount()
  if (canJumpTo(role.value, '/dpr/auth/draft-box')) refreshAuthDraftCount()
}
watch(() => route.path, maybeRefreshDraftCount, { immediate: true })
const onDraftChanged = () => maybeRefreshDraftCount()
onMounted(() => window.addEventListener('prm-draft-changed', onDraftChanged))
onUnmounted(() => window.removeEventListener('prm-draft-changed', onDraftChanged))

function onJump(path) {
  if (path) {
    router.push(path)
    jump.value = ''
  }
}

const AUTH_CONFIG = ['/dpr/auth/form-template', '/dpr/auth/scenario', '/dpr/auth/agreement-template', '/dpr/auth/cert-template']

const GROUP_NAMES = {
  '/dpr/ledger': '产权信息管理',
  '/dpr/monitor': '权益动态监测',
  '/dpr/confirm': '数据确权管理',
  '/dpr/auth': '数据授权管理',
  '/dpr/dashboard': '综合分析管理',
  '/dpr/system': '系统管理'
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
const PLAT_MENUS = []

// 手风琴:根据当前路由自动展开所属一级分组(unique-opened 负责切换时互斥折叠)
const openeds = computed(() => {
  const p = route.path
  // 待办中心跨确权/授权/监测多域聚合,无单一归属组可展开;精确匹配(而非前缀),避免误伤同前缀的「我的申请」
  if (p === '/dpr/workbench/todo') return []
  // 我的申请(apply/business 共享主页):横跨确权+授权申请进度,但 unique-opened 下只能展开一组——
  // 按角色主职责默认展开:apply 以确权为主(先确后授)开 03;business 在 03 组无任何可见项(会
  // 触发 el-menu.open() 对不存在分组解构报错),故开其唯一可见的授权组 04。两组均一键可点开,不影响可发现性。
  if (p === '/dpr/workbench/my') return role.value === 'business' ? ['04'] : ['03']
  if (p.startsWith('/dpr/ledger')) return ['01']
  if (p.startsWith('/dpr/monitor')) return ['02']
  if (p.startsWith('/dpr/confirm')) return ['03']
  if (AUTH_CONFIG.includes(p)) return ['07']
  if (p.startsWith('/dpr/auth')) return ['04']
  if (p.startsWith('/dpr/dashboard')) return ['05']
  if (p.startsWith('/dpr/system')) return ['08']
  return ['01']
})
// Element Plus el-menu 的 :default-openeds 只在挂载时读一次(其内部 openedMenus 无 watch 跟随 prop 变化),
// SPA 内路由跳转(不重新挂载 el-menu)时分组不会随之自动展开 —— 用暴露的 open() 方法接管路由切换后的展开。
const menuRef = ref()
// 防御:目标分组若对当前角色不可见(视觉上未渲染 el-sub-menu),Element 内部 open() 解构会抛异常;
// try/catch 兜底,避免未来新增角色/路由分支时因组合遗漏而炸掉整个路由监听
watch(openeds, (idxs) => { idxs.forEach(i => { try { menuRef.value?.open(i) } catch (e) { /* 分组对当前角色不可见,忽略 */ } }) }, { flush: 'post' })
</script>

<style scoped>
/* 顶栏:主蓝通栏(数研院典型界面母版),左 logo+白字平台名,水平平台导航,右侧白色图标组 */
.prm-header {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--prm-color-primary);
  color: #fff;
  border-bottom: none;
}
.prm-collapse-btn { color: #fff; padding: 4px; }
.prm-collapse-btn:hover { color: rgba(255, 255, 255, 0.75); }
.prm-logo { display: flex; flex-direction: column; line-height: 1.15; }
.prm-logo-csg { font-size: 11px; color: rgba(255, 255, 255, 0.75); letter-spacing: 1px; }
.prm-logo-name { font-size: 17px; font-weight: 700; color: #fff; white-space: nowrap; }
/* 平台一级导航:水平菜单,激活项白字+底部白条(蓝顶导航) */
.prm-plat-nav { display: flex; align-items: stretch; gap: 2px; margin-left: 20px; height: 100%; }
.plat-item {
  font-size: 14px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  white-space: nowrap;
  position: relative;
}
.plat-item.disabled { color: rgba(255, 255, 255, 0.5); cursor: not-allowed; }
.plat-item.active { color: #fff; font-weight: 600; }
.plat-item.active::after {
  content: "";
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 0;
  height: 2px;
  background: #fff;
}
.prm-crumb-right { margin-left: auto; display: flex; align-items: center; gap: 12px; min-width: 0; }
.prm-goal { font-size: 12px; color: var(--prm-color-text-weak); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 460px; }
/* 统一页面操作区:面包屑行右上角,各页主操作按钮传送至此 */
/* UI评审#2:操作区=面包屑下方独立操作带(非同行);sticky 紧贴面包屑(top=其高44px),长页滚动主按钮仍可见。
   评审追加:带底色与页面浅蓝一致(去白色带与下方撞色),不再压底边线 → 与内容区融为一体 */
.prm-page-actions {
  position: sticky;
  top: 44px;
  z-index: 9;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 8px 16px;
  background: var(--prm-color-page-bg);
}
.prm-page-actions:empty { display: none; }
.prm-spacer { flex: 1; }
.prm-search { width: 260px; }
.prm-role { width: 150px; flex: none; }
.prm-user { display: inline-flex; align-items: center; gap: 4px; color: #fff; cursor: pointer; font-size: 13px; padding: 0 4px; }
.prm-user:hover { color: rgba(255, 255, 255, 0.8); }
/* 顶栏内铃铛:蓝底上改白 */
.prm-header :deep(.nc-bell) { color: #fff; }
.prm-header :deep(.nc-bell:hover) { color: rgba(255, 255, 255, 0.8); }
/* 侧栏:白底菜单(数研院典型界面母版),激活项主色文字+浅蓝底+左主色竖条 */
.prm-aside {
  background: #fff;
  border-right: 1px solid var(--prm-color-border);
  transition: width 0.25s;
  overflow-x: hidden;
}
.prm-aside :deep(.el-menu) {
  border-right: none;
  background: transparent;
  --el-menu-text-color: var(--prm-color-text);
  --el-menu-hover-text-color: var(--prm-color-primary);
  --el-menu-active-color: var(--prm-color-primary);
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: var(--prm-color-selected-bg);
}
.prm-aside :deep(.el-sub-menu .el-menu) { background: var(--prm-color-bg); }
.prm-aside :deep(.el-menu-item.is-active) {
  background: var(--prm-color-selected-bg);
  color: var(--prm-color-primary);
  font-weight: 600;
  position: relative;
}
.prm-aside :deep(.el-menu-item.is-active)::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--prm-color-primary);
}
.prm-aside :deep(.el-sub-menu__title:hover),
.prm-aside :deep(.el-menu-item:hover) { background: var(--prm-color-selected-bg); }
/* 确权草稿数徽标:未完成申请提醒 */
.menu-count {
  display: inline-block; min-width: 16px; height: 16px; line-height: 16px; padding: 0 5px;
  margin-left: 8px; border-radius: 8px; font-size: 11px; text-align: center;
  background: var(--prm-color-link); color: #fff;
}
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
/* 全局页脚(规范 p37):工作区底部常显,白底+顶部细线、居中弱化文字 */
.prm-footer {
  flex: 0 0 auto;
  text-align: center;
  padding: 9px 16px;
  font-size: 12px;
  color: var(--prm-color-text-weak);
  background: #fff;
  border-top: 1px solid var(--prm-color-border);
}
</style>
