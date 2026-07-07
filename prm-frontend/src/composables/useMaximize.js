/*
 * Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
 * 中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
 */

/**
 * 列表最大化(高压线§11:工作区最大化):配合 .prm-maximized 类(见 tokens.css),
 * 令 .prm-table-card 变为 position:fixed 铺满视口,盖住侧栏/面包屑,不需要联动 App.vue 全局折叠态。
 */
import { ref } from 'vue'

export function useMaximize() {
  const maximized = ref(false)
  function toggle() { maximized.value = !maximized.value }
  return { maximized, toggle }
}
