<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="申请ID"><el-input v-model="checkApplyId" placeholder="授权申请ID" clearable style="width:200px" /></el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="q.riskLevel" placeholder="全部" clearable style="width:110px">
            <el-option label="绿" value="绿" /><el-option label="黄" value="黄" /><el-option label="红" value="红" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :disabled="!checkApplyId" :loading="running" @click="onRun">规则化校验</el-button>
          <el-button @click="onSearch">查询</el-button>
          <el-button @click="onExport">导出</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <div class="prm-table-note" style="margin:0 0 12px 0">注:依据规则自动校验【材料完整性 / 权限合理性(先确后授·权益类型·授权范围) / 合规性(第三方许可·敏感数据·跨域) / 授权协议要素(使用场景·目的·利益分配·安全保障 附录D §3.4.4)】，输出红/黄/绿报告，记录可查询导出。</div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="applyId" label="申请ID" width="190" show-overflow-tooltip />
        <el-table-column prop="riskLevel" label="风险等级" width="100" align="center">
          <template #default="{ row }"><el-tag :type="tag(row.riskLevel)">{{ row.riskLevel }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="checkResult" label="校验结果" width="100" align="center" />
        <el-table-column prop="problemDesc" label="问题描述(不符合项)" min-width="240" show-overflow-tooltip />
        <el-table-column prop="checkTime" label="校验时间" width="170" />
        <el-table-column label="报告" width="90" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="viewReport(row)">查看报告</el-button></template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>

    <el-dialog v-model="reportDlg" title="合规校验报告（四维）" width="640px" align-center>
      <el-alert v-if="report" :type="report.riskLevel==='红'?'error':(report.riskLevel==='黄'?'warning':'success')" :closable="false" style="margin-bottom:12px">
        风险等级：{{ report.riskLevel }}　校验结果：{{ report.checkResult }}
      </el-alert>
      <el-table :data="report?.items || []" border size="small">
        <el-table-column prop="dimension" label="维度" width="110">
          <template #default="{ row }"><el-tag effect="plain">{{ row.dimension }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="item" label="校验项" width="180" />
        <el-table-column label="结果" width="80" align="center">
          <template #default="{ row }"><el-tag :type="row.pass?'success':'danger'" size="small">{{ row.pass?'通过':'不符' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { runAuthCompliance, pageAuthCompliance, authComplianceExportUrl } from '@/api/authorize'

const q = reactive({ current: 1, size: 10, riskLevel: '' })
const rows = ref([]); const total = ref(0); const loading = ref(false); const running = ref(false)
const checkApplyId = ref('')
const reportDlg = ref(false); const report = ref(null)

function tag(l) { return { 绿: 'success', 黄: 'warning', 红: 'danger' }[l] || 'info' }
async function load() { loading.value = true; try { const r = await pageAuthCompliance({ ...q }); rows.value = r.records || []; total.value = r.total || 0 } finally { loading.value = false } }
function onSearch() { q.current = 1; load() }
async function onRun() {
  running.value = true
  try {
    report.value = await runAuthCompliance({ applyId: checkApplyId.value })
    reportDlg.value = true
    ElMessage[report.value.riskLevel === '红' ? 'error' : (report.value.riskLevel === '黄' ? 'warning' : 'success')](`校验完成：${report.value.checkResult}（${report.value.riskLevel}）`)
    load()
  } finally { running.value = false }
}
function viewReport(row) {
  let items = []
  try { items = JSON.parse(row.checkReport || '[]') } catch { items = [] }
  report.value = { riskLevel: row.riskLevel, checkResult: row.checkResult, items }
  reportDlg.value = true
}
function onExport() { window.open(authComplianceExportUrl({ applyId: checkApplyId.value, riskLevel: q.riskLevel }), '_blank') }
onMounted(load)
</script>
