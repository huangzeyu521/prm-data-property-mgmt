<template>
  <div class="prm-page">
    <div class="prm-table-note" style="margin:0 0 12px">
      注:附录G / 附录F §3.4.6——取得数据产品经营权的单位对外提供数据产品/服务时,须与被授权单位签协议并在公司数字化部**备案**。仅经营权对外授权需备案。
    </div>
    <div class="prm-table-card">
      <div style="margin-bottom:12px">
        <el-select v-model="q.filingStatus" placeholder="全部状态" clearable style="width:160px" @change="load">
          <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" style="margin-left:8px" @click="onAdd">新增备案</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="filingNo" label="备案编号" width="180" show-overflow-tooltip />
        <el-table-column prop="filingOrg" label="备案单位" min-width="140" show-overflow-tooltip />
        <el-table-column prop="granteeOrg" label="被授权方" min-width="140" show-overflow-tooltip />
        <el-table-column prop="rightType" label="产权类型" width="140" />
        <el-table-column prop="filingStatus" label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="row.filingStatus==='已备案'?'success':'warning'">{{ row.filingStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="filingTime" label="备案时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.filingStatus==='已备案'" @click="onFile(row)">完成备案</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, prev, pager, next"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" />
    </div>
    <el-dialog v-model="dlg" title="新增对外经营权授权备案" width="520px" align-center>
      <el-form :model="form" label-width="110px">
        <el-form-item label="备案单位"><el-input v-model="form.filingOrg" placeholder="分子公司名称" /></el-form-item>
        <el-form-item label="被授权方"><el-input v-model="form.granteeOrg" /></el-form-item>
        <el-form-item label="产权类型">
          <el-input model-value="数据产品经营权" disabled />
        </el-form-item>
        <el-form-item label="关联协议ID"><el-input v-model="form.agreementId" placeholder="附录D 运营授权协议ID(可选)" /></el-form-item>
        <el-form-item label="关联申请ID"><el-input v-model="form.applyId" placeholder="可选" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSave">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageFiling, createFiling, fileFiling } from '@/api/authorize'
const statuses = ['待备案', '已备案']
const q = reactive({ current: 1, size: 10, filingStatus: '' })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const dlg = ref(false)
const form = reactive({ filingOrg: '', granteeOrg: '', rightType: '数据产品经营权', agreementId: '', applyId: '', remark: '' })
async function load() {
  loading.value = true
  try { const r = await pageFiling({ current: q.current, size: q.size, filingStatus: q.filingStatus || undefined }); rows.value = r.records || []; total.value = r.total || 0 }
  finally { loading.value = false }
}
function onAdd() { Object.assign(form, { filingOrg: '', granteeOrg: '', rightType: '数据产品经营权', agreementId: '', applyId: '', remark: '' }); dlg.value = true }
async function onSave() {
  if (!form.granteeOrg) { ElMessage.warning('请填写被授权方'); return }
  await createFiling({ ...form }); ElMessage.success('已登记备案(待备案)'); dlg.value = false; load()
}
async function onFile(row) { await fileFiling(row.filingId); ElMessage.success('已完成备案'); load() }
onMounted(load)
</script>
