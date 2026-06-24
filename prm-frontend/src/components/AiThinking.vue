<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <transition name="ai-fade">
    <div v-if="active" class="ai-thinking" role="status" aria-live="polite">
      <div class="ai-head">
        <span class="ai-spinner" aria-hidden="true"></span>
        <span class="ai-title">{{ title }}</span>
        <span class="ai-elapsed">已用时 {{ elapsed }}s</span>
      </div>
      <div class="ai-hint">大模型正在后台思考与运算(通常约 20–30 秒),进度与思考过程如下,请稍候…</div>
      <ul class="ai-phases">
        <li v-for="(p, i) in phases" :key="i" :class="phaseClass(i)">
          <span class="ai-dot"></span>
          <span class="ai-label">{{ p }}</span>
          <span v-if="i < phaseIndex" class="ai-ok">✓ 完成</span>
          <span v-else-if="i === phaseIndex" class="ai-running">思考中…</span>
        </li>
      </ul>
    </div>
  </transition>
</template>

<script setup>
const props = defineProps({
  active: { type: Boolean, default: false },
  title: { type: String, default: '大模型处理中' },
  elapsed: { type: Number, default: 0 },
  phaseIndex: { type: Number, default: 0 },
  phases: { type: Array, default: () => [] },
})

function phaseClass(i) {
  return { passed: i < props.phaseIndex, cur: i === props.phaseIndex }
}
</script>

<style scoped>
.ai-thinking {
  border: 1px solid var(--prm-color-border, #d8d8d8);
  border-left: 3px solid var(--prm-color-primary, #1e87f0);
  border-radius: var(--prm-radius, 2px);
  background: var(--prm-color-bg, #f5f5f6);
  padding: 12px 16px;
  margin: 10px 0;
  max-width: 760px;
}
.ai-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ai-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--prm-color-primary, #1e87f0);
  border-top-color: transparent;
  border-radius: 50%;
  animation: ai-spin 0.8s linear infinite;
  flex: none;
}
@keyframes ai-spin {
  to { transform: rotate(360deg); }
}
.ai-title {
  font-weight: 600;
  color: var(--prm-color-text, #262626);
  font-size: 14px;
}
.ai-elapsed {
  margin-left: auto;
  color: var(--prm-color-primary, #1e87f0);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}
.ai-hint {
  color: var(--prm-color-text-weak, #8a8a8a);
  font-size: 12px;
  margin: 6px 0 8px;
}
.ai-phases {
  list-style: none;
  margin: 0;
  padding: 0;
}
.ai-phases li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0;
  font-size: 13px;
  color: var(--prm-color-text-weak, #8a8a8a);
  transition: color 0.2s;
}
.ai-phases li .ai-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #c4c8cf;
  flex: none;
}
.ai-phases li.cur {
  color: var(--prm-color-text, #262626);
  font-weight: 600;
}
.ai-phases li.cur .ai-dot {
  background: var(--prm-color-primary, #1e87f0);
  box-shadow: 0 0 0 3px rgba(18, 108, 253, 0.18);
  animation: ai-pulse 1s ease-in-out infinite;
}
.ai-phases li.passed {
  color: #52606d;
}
.ai-phases li.passed .ai-dot {
  background: #36b21d;
}
@keyframes ai-pulse {
  50% { box-shadow: 0 0 0 5px rgba(18, 108, 253, 0.08); }
}
.ai-ok {
  margin-left: auto;
  color: #36b21d;
  font-size: 12px;
}
.ai-running {
  margin-left: auto;
  color: var(--prm-color-primary, #1e87f0);
  font-size: 12px;
}
.ai-fade-enter-active,
.ai-fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.ai-fade-enter-from,
.ai-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
