<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="年度"><el-input v-model="q.listYear" placeholder="如 2026" clearable style="width:140px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="q.listStatus" placeholder="全部" clearable style="width:130px">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="load">查询</el-button><el-button type="primary" @click="onAdd">新增清单</el-button></el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <div class="prm-table-note" style="margin:0 0 12px 0">注:表6《数据批量授权清单》流转 草案 → 申报稿 → 领导小组办公室批准。</div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="listNo" label="清单编号" width="180" show-overflow-tooltip />
        <el-table-column prop="listYear" label="年度" width="100" align="center" />
        <el-table-column prop="itemCount" label="条目数" width="100" align="center" />
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="listStatus" label="状态" width="110" align="center">
          <template #default="{ row }"><el-tag :type="tag(row.listStatus)">{{ row.listStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="info" @click="onDetail(row)">明细(表6)</el-button>
            <el-button link type="primary" :disabled="row.listStatus !== '草案'" @click="onSubmit(row)">提交申报稿</el-button>
            <el-button link type="success" :disabled="row.listStatus !== '申报稿'" @click="onApprove(row)">领导小组批准</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, prev, pager, next"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" />
    </div>
    <el-dialog v-model="dlg" title="新增批量授权清单" width="500px" align-center>
      <el-form :model="form" label-width="80px">
        <el-form-item label="年度"><el-input v-model="form.listYear" placeholder="如 2026" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSave">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailDrawer" :title="`批量授权清单明细(表6) · ${curList.listNo||''}`" size="72%">
      <div class="prm-table-note" style="margin-bottom:10px">表6 明细行:本清单(batchListId)下的所有批量授权项。每项=一个数据表的授权,含第三方/隐私/跨域/权益。</div>
      <el-table :data="detailRows" v-loading="detailLoading" border stripe>
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="granteeOrg" label="申请单位/被授权方" min-width="150" show-overflow-tooltip />
        <el-table-column prop="assetName" label="数据表" min-width="140" show-overflow-tooltip />
        <el-table-column prop="rightType" label="权益类型" width="130" />
        <el-table-column prop="scenario" label="使用场景" min-width="120" show-overflow-tooltip />
        <el-table-column prop="thirdPartySource" label="第三方来源" width="120" show-overflow-tooltip />
        <el-table-column prop="sensitiveType" label="隐私/商密" width="110" />
        <el-table-column label="跨域" width="80" align="center"><template #default="{ row }">{{ row.crossRegion ? '是' : '否' }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }"><el-tag :type="tag(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!detailLoading && detailRows.length===0" description="该清单暂无明细项(可在'批量授权一站式向导'中逐条添加)" />
    </el-drawer>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageBatchList, createBatchList, submitBatchList, approveBatchList, listAuthByBatch } from '@/api/authorize'
const statuses = ['草案', '申报稿', '批准']
const q = reactive({ current: 1, size: 10, listYear: '', listStatus: '' })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const dlg = ref(false); const form = reactive({ listYear: '', remark: '' })
const detailDrawer = ref(false); const detailRows = ref([]); const detailLoading = ref(false); const curList = ref({})
function tag(s) { return { 批准: 'success', 申报稿: 'warning', 草案: 'info', 已生效: 'success', 已驳回: 'danger' }[s] || 'warning' }
async function onDetail(row) {
  curList.value = row; detailDrawer.value = true; detailLoading.value = true
  try { detailRows.value = await listAuthByBatch(row.batchListId) || [] } finally { detailLoading.value = false }
}
async function load() { loading.value = true; try { const r = await pageBatchList({ ...q }); rows.value = r.records || []; total.value = r.total || 0 } finally { loading.value = false } }
function onAdd() { Object.assign(form, { listYear: '', remark: '' }); dlg.value = true }
async function onSave() { await createBatchList({ ...form }); ElMessage.success('已新增清单(草案)'); dlg.value = false; load() }
async function onSubmit(row) { await submitBatchList(row.batchListId); ElMessage.success('已提交为申报稿'); load() }
async function onApprove(row) { await approveBatchList(row.batchListId); ElMessage.success('领导小组办公室已批准'); load() }
onMounted(load)
</script>
