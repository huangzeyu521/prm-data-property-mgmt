<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="dialogWidth"
    :align-center="variant === 'prompt'"
    :close-on-click-modal="variant !== 'form'"
    class="prm-dialog"
    :class="`prm-dialog--${variant}`"
    append-to-body
    @update:model-value="(v) => emit('update:modelValue', v)"
    @closed="emit('closed')"
  >
    <!-- 提示型:图标 + 文案 -->
    <div v-if="variant === 'prompt'" class="prm-dialog-prompt">
      <el-icon class="prm-dialog-prompt-icon" :class="`is-${iconType}`" :size="22">
        <component :is="iconComp" />
      </el-icon>
      <div class="prm-dialog-prompt-body">
        <slot>{{ message }}</slot>
      </div>
    </div>
    <!-- 表单型 / 详情只读型:正文走默认插槽 -->
    <div v-else class="prm-dialog-body">
      <slot />
    </div>

    <template #footer>
      <slot name="footer">
        <!-- 详情只读型:仅关闭 -->
        <template v-if="variant === 'detail'">
          <el-button @click="close">{{ cancelText || '关闭' }}</el-button>
        </template>
        <!-- 提示型 / 表单型:取消 + 主操作 -->
        <template v-else>
          <el-button @click="onCancel">{{ cancelText || '取消' }}</el-button>
          <el-button type="primary" :loading="loading" @click="emit('confirm')">
            {{ confirmText || (variant === 'form' ? '提交' : '确定') }}
          </el-button>
        </template>
      </slot>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { WarningFilled, SuccessFilled, InfoFilled, CircleCloseFilled } from '@element-plus/icons-vue'

/**
 * 数研院典型界面三型弹窗母版(切图:提示页 / 表单输入页 / 详情只读页)统一封装。
 * - variant="prompt":图标+文案,取消+确定,窄;close-on-click-modal=开。
 * - variant="form"  :表单插槽,取消+提交,中;close-on-click-modal=关(防误关丢数据)。
 * - variant="detail":只读插槽,仅关闭,宽。
 * 用法:<prm-dialog v-model="show" variant="form" title="新增" @confirm="save"> ...表单... </prm-dialog>
 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  variant: { type: String, default: 'form' }, // prompt | form | detail
  title: { type: String, default: '' },
  width: { type: [String, Number], default: '' },
  message: { type: String, default: '' },
  iconType: { type: String, default: 'warning' }, // warning | success | info | danger
  confirmText: { type: String, default: '' },
  cancelText: { type: String, default: '' },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'confirm', 'cancel', 'closed'])

const DEFAULT_WIDTH = { prompt: '420px', form: '600px', detail: '760px' }
const dialogWidth = computed(() => props.width || DEFAULT_WIDTH[props.variant] || '600px')

const ICONS = { warning: WarningFilled, success: SuccessFilled, info: InfoFilled, danger: CircleCloseFilled }
const iconComp = computed(() => ICONS[props.iconType] || WarningFilled)

function close() {
  emit('update:modelValue', false)
}
function onCancel() {
  emit('cancel')
  close()
}
</script>

<style scoped>
/* 统一头部/正文/按钮区:圆角与间距走令牌(切图三型母版) */
.prm-dialog :deep(.el-dialog__header) {
  margin-right: 0;
  padding: 16px 20px;
  border-bottom: 1px solid var(--prm-color-border, #d8d8d8);
  font-weight: 600;
}
.prm-dialog :deep(.el-dialog__title) {
  font-size: 16px;
  color: var(--prm-color-text);
}
.prm-dialog :deep(.el-dialog__body) { padding: 20px; }
.prm-dialog :deep(.el-dialog__footer) {
  padding: 12px 20px;
  border-top: 1px solid var(--prm-color-border);
}
.prm-dialog-prompt { display: flex; gap: 12px; align-items: flex-start; }
.prm-dialog-prompt-body { font-size: 14px; color: var(--prm-color-text, #262626); line-height: 1.7; padding-top: 1px; }
.prm-dialog-prompt-icon.is-warning { color: var(--prm-color-warning, #ffc417); }
.prm-dialog-prompt-icon.is-success { color: var(--el-color-success, #36b21d); }
.prm-dialog-prompt-icon.is-info { color: var(--prm-color-primary, #1e87f0); }
.prm-dialog-prompt-icon.is-danger { color: var(--prm-color-danger); }
.prm-dialog-body { font-size: 14px; }
</style>
