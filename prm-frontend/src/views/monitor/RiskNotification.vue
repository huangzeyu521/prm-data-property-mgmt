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
      <div class="prm-table-note" style="margin:0 0 12px 0">注:风险预警通知支持站内消息、南网 eLink 移动端推送;此处展示通知历史与送达状态。</div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="alertLevel" label="级别" width="90" align="center">
          <template #default="{ row }"><el-tag :type="levelTag(row.alertLevel)">{{ row.alertLevel }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="110" />
        <el-table-column prop="abnormalDesc" label="通知内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="disposeStatus" label="处置状态" width="100" align="center" />
        <el-table-column prop="alertTime" label="通知时间" width="170" />
        <el-table-column label="渠道" width="160">
          <template #default><el-tag size="small">站内消息</el-tag> <el-tag size="small" type="success">eLink</el-tag></template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, prev, pager, next"
        :total="total" :current-page="query.current" :page-size="query.size" @current-change="onPage" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageAlert } from '@/api/monitor'

const levels = ['紧急', '重要', '普通']
const query = reactive({ current: 1, size: 10, alertLevel: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
function levelTag(l) { return { 紧急: 'danger', 重要: 'warning', 普通: 'info' }[l] || 'info' }

async function load() {
  loading.value = true
  try {
    const res = await pageAlert({ ...query })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}
function onSearch() { query.current = 1; load() }
function onReset() { query.alertLevel = ''; onSearch() }
function onPage(p) { query.current = p; load() }
onMounted(load)
</script>
