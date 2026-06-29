<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
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
        <el-table-column prop="filingNo" label="备案编号" width="170" show-overflow-tooltip />
        <el-table-column prop="filingOrg" label="授权单位(备案单位)" min-width="150" show-overflow-tooltip />
        <el-table-column prop="granteeOrg" label="被授权单位" min-width="140" show-overflow-tooltip />
        <el-table-column prop="agreementNo" label="协议编号" min-width="150" show-overflow-tooltip><template #default="{ row }">{{ row.agreementNo || '—' }}</template></el-table-column>
        <el-table-column prop="rightType" label="产权类型" width="130" />
        <el-table-column prop="validDate" label="授权期限" width="120"><template #default="{ row }">{{ row.validDate ? String(row.validDate).slice(0,10) : '—' }}</template></el-table-column>
        <el-table-column prop="filingStatus" label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.filingStatus==='已备案'?'success':'warning'">{{ row.filingStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="filingTime" label="备案时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.filingStatus==='已备案'" @click="onFile(row)">完成备案</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>
    <el-dialog v-model="dlg" title="新增对外经营权授权备案(附录G)" width="560px" align-center>
      <el-alert type="info" :closable="false" style="margin-bottom:12px"
        title="备案对象=一份已生效的经营权授权协议。选取协议后自动带出 被授权单位/协议编号/授权期限。附件须提交《授权协议》及《数据授权清单》电子版(随协议存档)。" />
      <el-form :model="form" label-width="120px">
        <el-form-item label="选取经营权协议" required>
          <el-select v-model="form.agreementId" filterable placeholder="选择已生效的数据产品经营权授权协议" style="width:100%"
            :loading="agLoading" @change="onPickAgreement">
            <el-option v-for="a in agreementOpts" :key="a.agreementId" :label="`${a.agreementNo}（${a.granteeOrg || '—'}）`" :value="a.agreementId" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权单位(备案单位)"><el-input v-model="form.filingOrg" placeholder="本分子公司名称(授权方)" /></el-form-item>
        <el-form-item label="被授权单位"><el-input v-model="form.granteeOrg" placeholder="选协议后自动带出,可改" /></el-form-item>
        <el-form-item label="产权类型"><el-input model-value="数据产品经营权" disabled /></el-form-item>
        <template v-if="elem">
          <el-form-item label="协议编号"><el-input :model-value="elem.agreementNo" disabled /></el-form-item>
          <el-form-item label="授权期限"><el-input :model-value="elem.validDate || '—'" disabled /></el-form-item>
          <el-form-item label="授权数据"><el-input :model-value="`${elem.sysName || '—'} / ${elem.dataTable || '—'}`" disabled /></el-form-item>
        </template>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" maxlength="500" show-word-limit :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSave">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageFiling, createFiling, fileFiling, pageAgreement, getAgreementElements } from '@/api/authorize'
const statuses = ['待备案', '已备案']
const q = reactive({ current: 1, size: 10, filingStatus: '' })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const dlg = ref(false)
const form = reactive({ filingOrg: '', granteeOrg: '', rightType: '数据产品经营权', agreementId: '', applyId: '', remark: '' })
// 经营权协议选择器(已审核通过的数据产品经营权运营授权协议)+ 选后要素带出
const agreementOpts = ref([]); const agLoading = ref(false); const elem = ref(null)
async function loadAgreements() {
  agLoading.value = true
  try {
    const r = await pageAgreement({ current: 1, size: 100, agreementType: '数据产品经营权' })
    agreementOpts.value = (r.records || []).filter(a => a.reviewStatus === '审核通过')
  } catch { agreementOpts.value = [] } finally { agLoading.value = false }
}
// 选协议 → 复用 getAgreementElements 自动带出 被授权单位/协议编号/授权期限/系统/数据表
async function onPickAgreement(id) {
  elem.value = null
  if (!id) return
  const e = await getAgreementElements(id)
  elem.value = e
  if (e) {
    form.granteeOrg = e.granteeOrg || form.granteeOrg
    form.applyId = e.applyId || ''
  }
}
async function load() {
  loading.value = true
  try { const r = await pageFiling({ current: q.current, size: q.size, filingStatus: q.filingStatus || undefined }); rows.value = r.records || []; total.value = r.total || 0 }
  finally { loading.value = false }
}
function onAdd() {
  Object.assign(form, { filingOrg: '', granteeOrg: '', rightType: '数据产品经营权', agreementId: '', applyId: '', remark: '' })
  elem.value = null; dlg.value = true; loadAgreements()
}
async function onSave() {
  if (!form.agreementId) { ElMessage.warning('请选取要备案的经营权授权协议'); return }
  if (!form.granteeOrg) { ElMessage.warning('被授权单位为空(选协议后自动带出)'); return }
  await createFiling({ ...form }); ElMessage.success('已登记备案(待备案)'); dlg.value = false; load()
}
async function onFile(row) { await fileFiling(row.filingId); ElMessage.success('已完成备案'); load() }
onMounted(load)
</script>
