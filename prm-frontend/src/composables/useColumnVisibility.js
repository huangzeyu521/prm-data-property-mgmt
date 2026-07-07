/*
 * Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
 * 中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
 */

/**
 * 列自定义(高压线:列表须支持列自定义功能):按 route.path + tableKey 记忆各可选列的显隐,
 * 存 localStorage(跨会话保留,区别于查询筛选态用 sessionStorage 只记当次会话)。
 *
 * @param {string} tableKey 同页多表时用于区分存储键;单表页可传固定字符串如 'main'
 * @param {Array<{prop:string, label:string, default?:boolean}>} defs 可选列定义(default 缺省为 true = 默认显示)
 */
import { reactive, watch } from 'vue'
import { useRoute } from 'vue-router'

export function useColumnVisibility(tableKey, defs) {
  const route = useRoute()
  const storageKey = `prm-col-vis:${route.path}:${tableKey}`
  const saved = (() => { try { return JSON.parse(localStorage.getItem(storageKey) || 'null') } catch (e) { return null } })()
  const visible = reactive({})
  for (const d of defs) visible[d.prop] = (saved && saved[d.prop] !== undefined) ? saved[d.prop] : (d.default !== false)

  watch(visible, (v) => {
    try { localStorage.setItem(storageKey, JSON.stringify(v)) } catch (e) { /* 存储不可用时静默降级 */ }
  }, { deep: true })

  function isVisible(prop) { return visible[prop] !== false }
  function setVisible(prop, v) { visible[prop] = v }
  function reset() { for (const d of defs) visible[d.prop] = d.default !== false }

  return { visible, isVisible, setVisible, reset }
}
