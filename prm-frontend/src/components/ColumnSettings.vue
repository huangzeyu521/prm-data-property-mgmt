<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!-- 列自定义弹出层(高压线:列表须支持列自定义功能)。纯展示组件,状态由 useColumnVisibility 管理。 -->
<template>
  <el-popover placement="bottom-end" width="220" trigger="click">
    <template #reference>
      <el-button :icon="Setting" circle size="small" title="列设置" />
    </template>
    <div style="font-weight:600;margin-bottom:6px;font-size:13px">显示列</div>
    <el-checkbox v-for="d in columns" :key="d.prop"
      :model-value="visible[d.prop] !== false" @change="v => $emit('toggle', d.prop, v)"
      style="display:block;margin:2px 0">{{ d.label }}</el-checkbox>
    <div style="margin-top:8px;text-align:right;border-top:1px solid var(--prm-color-bg);padding-top:6px">
      <el-button link type="primary" size="small" @click="$emit('reset')">恢复默认</el-button>
    </div>
  </el-popover>
</template>

<script setup>
import { Setting } from '@element-plus/icons-vue'

defineProps({
  columns: { type: Array, required: true }, // [{ prop, label }]
  visible: { type: Object, required: true }
})
defineEmits(['toggle', 'reset'])
</script>
