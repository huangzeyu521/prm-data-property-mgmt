<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="申请ID"><el-input v-model="checkApplyId" placeholder="授权申请ID" clearable style="width:200px" /></el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="riskLevel" style="width:110px">
            <el-option label="绿" value="绿" /><el-option label="黄" value="黄" /><el-option label="红" value="红" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="warning" :disabled="!checkApplyId" @click="onRun">执行合规校验</el-button><el-button type="primary" @click="load">刷新</el-button></el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <div class="prm-table-note" style="margin:0 0 12px 0">注:合规校验比对授权范围是否超出确权边界、权属冲突、材料完整性,输出红/黄/绿风险。</div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="applyId" label="申请ID" width="200" show-overflow-tooltip />
        <el-table-column prop="riskLevel" label="风险等级" width="100" align="center">
          <template #default="{ row }"><el-tag :type="tag(row.riskLevel)">{{ row.riskLevel }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="checkResult" label="校验结果" width="110" align="center" />
        <el-table-column prop="problemDesc" label="问题描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="checkTime" label="校验时间" width="170" />
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, prev, pager, next"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" />
    </div>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { runAuthCompliance, pageAuthCompliance } from '@/api/authorize'
const q = reactive({ current: 1, size: 10 })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const checkApplyId = ref(''); const riskLevel = ref('绿')
function tag(l) { return { 绿: 'success', 黄: 'warning', 红: 'danger' }[l] || 'info' }
async function load() { loading.value = true; try { const r = await pageAuthCompliance({ ...q }); rows.value = r.records || []; total.value = r.total || 0 } finally { loading.value = false } }
async function onRun() { await runAuthCompliance({ applyId: checkApplyId.value, riskLevel: riskLevel.value }); ElMessage.success('已生成合规校验结果'); load() }
onMounted(load)
</script>
