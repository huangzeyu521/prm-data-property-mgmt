<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <el-popover placement="bottom-end" :width="380" trigger="click" popper-class="nc-pop" @show="reload">
    <template #reference>
      <el-badge :value="unread" :max="99" :hidden="unread === 0" class="nc-badge">
        <el-button text class="nc-bell" :title="`未读预警通知 ${notifyUnread} · 待办 ${todoTotal}`">
          <el-icon :size="18"><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>

    <div class="nc-panel">
      <div class="nc-head">
        <span class="nc-title">通知中心</span>
        <el-button text size="small" class="nc-refresh" :loading="loading" @click="reload">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>

      <el-tabs v-model="tab" class="nc-tabs">
        <el-tab-pane :label="`预警通知 (${notifyUnread})`" name="alert">
          <div v-if="notifications.length === 0" class="nc-empty"><el-empty :image-size="56" description="暂无未读预警通知" /></div>
          <ul v-else class="nc-list">
            <li v-for="n in notifications" :key="n.notifyId" class="nc-item" @click="goNotification(n)">
              <span class="nc-dot" :class="levelClass(n.alertLevel)"></span>
              <div class="nc-body">
                <div class="nc-desc">{{ n.content || n.title }}</div>
                <div class="nc-meta">
                  <span>{{ n.recipient || '责任人' }}</span>
                  <span class="nc-dotsep">·</span>
                  <span>{{ n.channel }}</span>
                  <span class="nc-dotsep">·</span>
                  <span>{{ timeAgo(n.pushTime) }}</span>
                </div>
              </div>
              <el-tag size="small" type="danger" effect="light">未读</el-tag>
            </li>
          </ul>
        </el-tab-pane>

        <el-tab-pane :label="`待办 (${todoTotal})`" name="todo">
          <div v-if="todos.length === 0" class="nc-empty"><el-empty :image-size="56" description="暂无待办" /></div>
          <ul v-else class="nc-list">
            <li v-for="t in todos" :key="t.domain + t.id" class="nc-item" @click="goTodo(t)">
              <el-tag size="small" class="nc-domain" :type="t.domain === '授权' ? 'success' : 'primary'" effect="plain">{{ t.domain }}</el-tag>
              <div class="nc-body">
                <div class="nc-desc">{{ t.assetName || t.no }}</div>
                <div class="nc-meta"><span>{{ t.no }}</span><span class="nc-dotsep">·</span><span>{{ t.party }}</span></div>
              </div>
              <el-tag size="small" type="warning" effect="light">{{ t.status }}</el-tag>
            </li>
          </ul>
        </el-tab-pane>
      </el-tabs>

      <div class="nc-foot">
        <el-button v-if="tab === 'alert' && notifications.length" text size="small" @click="onReadAll">全部已读</el-button>
        <el-button text type="primary" size="small" @click="goAll">
          {{ tab === 'alert' ? '查看全部预警' : '查看全部待办' }} →
        </el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageNotification, unreadNotifyCount, markNotifyRead, markAllNotifyRead } from '@/api/monitor'
import { getTodos } from '@/api/workbench'

const router = useRouter()
const tab = ref('alert')
const loading = ref(false)
const notifications = ref([])
const todos = ref([])
const notifyUnread = ref(0)   // 未读预警通知数
const todoTotal = ref(0)
const unread = ref(0)

const REFRESH_MS = 60000
let timer = null

function levelClass(l) {
  return { 紧急: 'lv-urgent', 重要: 'lv-major', 普通: 'lv-normal' }[l] || 'lv-normal'
}
function timeAgo(t) {
  if (!t) return ''
  const d = new Date(String(t).replace(' ', 'T'))
  const sec = (Date.now() - d.getTime()) / 1000
  if (sec < 60) return '刚刚'
  if (sec < 3600) return Math.floor(sec / 60) + ' 分钟前'
  if (sec < 86400) return Math.floor(sec / 3600) + ' 小时前'
  return Math.floor(sec / 86400) + ' 天前'
}

async function reload() {
  loading.value = true
  try {
    const [notifyPage, cnt, todoVo] = await Promise.all([
      pageNotification({ current: 1, size: 8, readStatus: '未读' }),
      unreadNotifyCount(),
      getTodos()
    ])
    notifications.value = notifyPage?.records || []
    notifyUnread.value = cnt || 0
    const t = todoVo || {}
    todos.value = [...(t.confirmTodos || []), ...(t.authTodos || [])].slice(0, 8)
    todoTotal.value = t.total || 0
    unread.value = notifyUnread.value + todoTotal.value
  } finally {
    loading.value = false
  }
}

async function goNotification(n) {
  if (n.notifyId) { await markNotifyRead(n.notifyId) }
  router.push({ path: '/dpr/monitor/alert', query: n.assetId ? { assetId: n.assetId } : {} })
}
async function onReadAll() {
  await markAllNotifyRead()
  reload()
}
function goTodo(t) {
  const path = t.domain === '授权' ? '/dpr/auth/review' : '/dpr/confirm/review'
  router.push({ path, query: t.assetId ? { assetId: t.assetId } : {} })
}
function goAll() {
  router.push(tab.value === 'alert' ? '/dpr/monitor/alert' : '/dpr/workbench/todo')
}

onMounted(() => {
  reload()
  timer = setInterval(reload, REFRESH_MS)
})
onUnmounted(() => timer && clearInterval(timer))
</script>

<style scoped>
.nc-bell { color: #71717a; padding: 6px; }
.nc-bell:hover { color: var(--prm-color-primary, #1e87f0); }
.nc-badge :deep(.el-badge__content) { border: none; }
</style>

<style>
/* 面板内容渲染在 body 下的 popper,需非 scoped */
.nc-pop { padding: 0 !important; }
.nc-panel { display: flex; flex-direction: column; }
.nc-head { display: flex; align-items: center; justify-content: space-between; padding: 12px 14px 4px; }
.nc-title { font-weight: 700; font-size: 15px; color: var(--prm-color-text, #1f2329); }
.nc-refresh { color: var(--prm-color-text-weak); }
.nc-tabs { padding: 0 6px; }
.nc-tabs .el-tabs__header { margin-bottom: 4px; }
.nc-tabs .el-tabs__nav-wrap::after { height: 1px; }
.nc-list { list-style: none; margin: 0; padding: 0; max-height: 360px; overflow-y: auto; }
.nc-item {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 10px 10px; border-radius: 8px; cursor: pointer;
  transition: background 0.15s;
}
.nc-item:hover { background: #f2f6ff; }
.nc-body { flex: 1; min-width: 0; }
.nc-desc {
  font-size: 13px; color: #1f2329; line-height: 1.35;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.nc-meta { margin-top: 3px; font-size: 11px; color: #99a; display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
.nc-dotsep { color: #cdd; }
.nc-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 5px; flex: none; }
.lv-urgent { background: #f5483b; box-shadow: 0 0 0 3px rgba(245,72,59,0.15); }
.lv-major { background: #ffc417; box-shadow: 0 0 0 3px rgba(240,160,32,0.15); }
.lv-normal { background: #1e87f0; box-shadow: 0 0 0 3px rgba(47,107,255,0.15); }
.nc-domain { flex: none; margin-top: 2px; }
.nc-empty { padding: 8px 0 14px; }
.nc-foot { border-top: 1px solid #eef0f3; padding: 8px 14px; text-align: center; }
</style>
