<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="预警级别">
          <el-select v-model="query.alertLevel" placeholder="全部" clearable style="width:130px">
            <el-option v-for="l in levels" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="onReset">重置</el-button></el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <PageNote>注:风险预警通知支持站内消息、南网 eLink 移动端推送;此处展示通知历史与送达状态。</PageNote>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="alertLevel" label="级别" width="90" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((levelTag(row.alertLevel)) || 'primary')">{{ row.alertLevel }}</span></template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="110" />
        <el-table-column prop="abnormalDesc" label="通知内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="disposeStatus" label="处置状态" width="100" align="center" />
        <el-table-column prop="alertTime" label="通知时间" width="170" />
        <el-table-column label="渠道" width="160">
          <template #default><span class="prm-c-primary">站内消息</span> <span class="prm-c-success">eLink</span></template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:20px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="query.current" :page-size="query.size" @current-change="onPage" @size-change="s=>{query.size=s;query.current=1;load()}" />
    </div>
  </div>
</template>

<script setup>
import PageNote from '@/components/PageNote.vue'
import { onMounted } from 'vue'
import { pageAlert } from '@/api/monitor'
import { useTablePage } from '@/composables/useTablePage'

const levels = ['紧急', '重要', '普通']
const { query, rows, total, loading, load, search: onSearch, reset: onReset, onPage } =
  useTablePage(pageAlert, { alertLevel: '' })
function levelTag(l) { return { 紧急: 'danger', 重要: 'warning', 普通: 'info' }[l] || 'info' }

onMounted(load)
</script>
