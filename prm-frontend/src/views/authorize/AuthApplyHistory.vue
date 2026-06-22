<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="资产名称"><el-input v-model="q.assetName" clearable style="width:160px" /></el-form-item>
        <el-form-item label="申请人"><el-input v-model="q.applicant" placeholder="申请单位主管" clearable style="width:140px" /></el-form-item>
        <el-form-item label="授权模式">
          <el-select v-model="q.authMode" placeholder="全部" clearable style="width:120px">
            <el-option label="一事一议" value="一事一议" /><el-option label="批量" value="批量" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="q.status" placeholder="全部" clearable style="width:120px">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="onReset">重置</el-button></el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="applyNo" label="申请编号" width="150" show-overflow-tooltip />
        <el-table-column prop="authMode" label="模式" width="100" align="center" />
        <el-table-column prop="assetName" label="资产名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="granteeOrg" label="被授权方" min-width="130" show-overflow-tooltip />
        <el-table-column prop="applicantManager" label="申请人" width="120" show-overflow-tooltip />
        <el-table-column label="审核结果" width="100" align="center">
          <template #default="{ row }"><el-tag :type="reviewTag(row.status)">{{ reviewResult(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="授权状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="authTag(row.status)" effect="plain">{{ authStatus(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="处理意见" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.rejectReason || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160" />
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, prev, pager, next"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" />
    </div>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageAuthApply } from '@/api/authorize'
const statuses = ['草稿', '合规审核中', '业务审核中', '主管审核中', '经理审核中', '副总审批中', '领导小组审批中', '已生效', '已驳回']
const q = reactive({ current: 1, size: 10, assetName: '', applicant: '', authMode: '', status: '' })
const rows = ref([]); const total = ref(0); const loading = ref(false)
// 审核结果:未提交/审核中/通过/驳回
function reviewResult(s) { return s === '草稿' ? '未提交' : s === '已生效' ? '通过' : s === '已驳回' ? '驳回' : '审核中' }
function reviewTag(s) { return s === '已生效' ? 'success' : s === '已驳回' ? 'danger' : s === '草稿' ? 'info' : 'warning' }
// 授权状态:已生效/未授权/待生效
function authStatus(s) { return s === '已生效' ? '已生效' : s === '已驳回' ? '未授权' : s === '草稿' ? '—' : '待生效' }
function authTag(s) { return s === '已生效' ? 'success' : s === '已驳回' ? 'danger' : 'info' }
async function load() { loading.value = true; try { const r = await pageAuthApply({ ...q }); rows.value = r.records || []; total.value = r.total || 0 } finally { loading.value = false } }
function onSearch() { q.current = 1; load() }
function onReset() { Object.assign(q, { assetName: '', applicant: '', authMode: '', status: '' }); onSearch() }
onMounted(load)
</script>
