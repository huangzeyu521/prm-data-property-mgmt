/*
 * Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
 * 中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
 */

/**
 * 列表页分页管线(治理总纲·维度2 复合模块去重):收编 23 个列表页重复的
 * query(current/size+filters) / rows / total / loading + load/search/reset/onPage/onSizeChange。
 * 纯逻辑、无 DOM 依赖;页面保留各自 el-table + 筛选 + 原生 el-pagination 标记(本就异构)。
 *
 * 约定贴合本仓库实情:分页入参 current/size,响应 { records, total }(可经 options 覆盖)。
 *
 * 用法(destructure 时重命名以贴合既有模板 handler,模板可零改动):
 *   const { query, rows, total, loading, load, search: onSearch, reset: onReset, onPage, onSizeChange }
 *     = useTablePage(pageRule, { ruleName: '', effectStatus: '' })
 *   onMounted(load)
 * 模板:<el-pagination :current-page="query.current" :page-size="query.size" :total="total"
 *         @current-change="onPage" @size-change="onSizeChange" ... />
 *
 * 需要附加固定参数(非 query 字段)的页面,包一层 loader 即可:
 *   useTablePage((p) => pageCatalog({ ...p, category: category.value }), { name: '' })
 *
 * 高压线§13(返回列表页须保留查询条件/页码):query 按 route.path 写 sessionStorage,
 * 挂载时若有记忆值则还原。用 sessionStorage 而非同步进 URL query,是因为 App.vue 的
 * <router-view :key="$route.fullPath"> 会在 query 变化时整页重挂载——同步进 URL 会在
 * 保存的瞬间被自己触发的重挂载冲掉,且会连带影响多个页面依赖"query 变化即重新 mount"
 * 拿最新 applyId/reopen 预填的既有逻辑(ConfirmWizard/AuthWizard 等)。sessionStorage
 * 不改 URL,不触发重挂载,纯加法、零风险。
 *
 * @param {(params: object) => Promise<{records?: any[], total?: number}>} loader 分页接口
 * @param {object} [filters={}] 初始筛选字段(reset 回此基线;不含 current/size)
 * @param {{ pageSize?: number, immediate?: boolean, recordsKey?: string, totalKey?: string, onLoaded?: () => void, persist?: boolean }} [options]
 *        onLoaded:每次拉取成功后触发(如刷新统计卡片);search/reset/onPage/onSizeChange 均覆盖。
 *        persist:是否记忆查询态(默认 true),同一 route.path 下个别页面如不需要可传 false 关闭。
 */
import { reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

export function useTablePage(loader, filters = {}, options = {}) {
  const { pageSize = 10, immediate = false, recordsKey = 'records', totalKey = 'total', onLoaded, persist = true } = options
  const initialFilters = { ...filters }
  // useRoute() 在非组件上下文(如单测直接调用本函数)会抛错,捕获后按不记忆处理,不影响核心分页逻辑
  let storageKey = null
  try { storageKey = persist ? ('prm-list-query:' + useRoute().path) : null } catch (e) { storageKey = null }

  function restoreQuery() {
    const base = { current: 1, size: pageSize, ...filters }
    if (!storageKey) return base
    try {
      const saved = JSON.parse(sessionStorage.getItem(storageKey) || 'null')
      return saved && typeof saved === 'object' ? { ...base, ...saved } : base
    } catch (e) { return base }
  }

  const query = reactive(restoreQuery())
  const rows = ref([])
  const total = ref(0)
  const loading = ref(false)

  if (storageKey) {
    watch(() => ({ ...query }), (v) => {
      try { sessionStorage.setItem(storageKey, JSON.stringify(v)) } catch (e) { /* 存储满/隐私模式等,静默降级为不记忆 */ }
    }, { deep: true })
  }

  async function load() {
    loading.value = true
    try {
      const res = await loader({ ...query })
      rows.value = (res && res[recordsKey]) || []
      total.value = (res && res[totalKey]) || 0
      if (onLoaded) onLoaded()
    } finally {
      loading.value = false
    }
  }

  // 查询:回到第 1 页再拉取(筛选条件变更后调用)
  function search() {
    query.current = 1
    return load()
  }

  // 重置:筛选字段回初始基线 + 回第 1 页 + 拉取
  function reset() {
    Object.assign(query, { current: 1, size: pageSize, ...initialFilters })
    return load()
  }

  function onPage(p) {
    query.current = p
    return load()
  }

  function onSizeChange(s) {
    query.size = s
    query.current = 1
    return load()
  }

  if (immediate) load()

  return { query, rows, total, loading, load, search, reset, onPage, onSizeChange }
}
