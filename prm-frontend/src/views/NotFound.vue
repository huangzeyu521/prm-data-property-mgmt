<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <!-- 异常数据页(规范 p122 强管制):无效路径兜底,在主框架内渲染(带导航/面包屑) -->
  <div class="nf-wrap">
    <el-result icon="warning" title="404" sub-title="抱歉,您访问的页面不存在或已被移除~">
      <template #extra>
        <el-button type="primary" @click="goHome">返回首页</el-button>
        <el-button @click="goBack">返回上一页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
function goHome() { router.push('/dpr/dashboard/overview') }
function goBack() { window.history.length > 1 ? router.back() : router.push('/dpr/dashboard/overview') }
</script>

<style scoped>
.nf-wrap { display: flex; align-items: center; justify-content: center; min-height: 60vh; }
</style>
