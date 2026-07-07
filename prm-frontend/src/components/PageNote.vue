<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!--
  页顶说明(UI评审#6:清理页面冗余文案,长段说明改小图标触发提示):
  原页顶常显「注:…」长段落收进 hover 弹层,文案一字不改、按需查看。
  仅用于页面顶部流程说明;列表下方「注:」注释按治理规范(高压线第十条)保持常显,勿用本组件。
-->
<template>
  <span class="prm-page-note">
    <el-popover placement="bottom-start" :width="520" trigger="hover" popper-class="prm-page-note-pop">
      <template #reference>
        <span class="prm-page-note-trigger"><el-icon><QuestionFilled /></el-icon>说明</span>
      </template>
      <div class="prm-page-note-body"><slot /></div>
    </el-popover>
  </span>
</template>

<script setup>
import { QuestionFilled } from '@element-plus/icons-vue'
</script>

<style scoped>
.prm-page-note { display: inline-flex; align-items: center; }
.prm-page-note-trigger {
  display: inline-flex; align-items: center; gap: 3px; cursor: help;
  font-size: 12px; color: var(--prm-color-text-weak);
}
.prm-page-note-trigger:hover { color: var(--prm-color-primary); }
</style>

<style>
/* popper 挂 body,须全局作用域;正文沿用弱文字规范 */
.prm-page-note-pop .prm-page-note-body { font-size: 12.5px; line-height: 1.8; color: var(--prm-color-text-secondary); }
</style>
