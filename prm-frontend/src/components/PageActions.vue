<template>
  <!-- 把页面主操作按钮统一传送到面包屑行右上角的操作区(规范:操作按钮置于页面右上角,替代底部布局)。
       插槽内容仍在调用页的渲染作用域内,响应式/事件照常工作;仅 DOM 位置移动到 #prm-page-actions。 -->
  <Teleport to="#prm-page-actions">
    <slot />
  </Teleport>
</template>

<script setup>
// 无逻辑:纯 Teleport 容器。目标 #prm-page-actions 由主框架 App.vue 始终渲染。
</script>
