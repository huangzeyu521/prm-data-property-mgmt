<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
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
            <el-button link type="warning" :disabled="row.listStatus !== '批准'" @click="onGenAgreement(row)">生成运营授权协议</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>
    <el-dialog v-model="dlg" title="新增批量授权清单" width="500px" align-center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="年度"><el-input v-model="form.listYear" placeholder="如 2026" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSave">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailDrawer" :title="`批量授权清单明细(表6) · ${curList.listNo||''}`" size="80%">
      <div class="prm-table-note" style="margin-bottom:8px">表6 明细行:本清单(batchListId)下的所有批量授权项。每项=一个库表的授权,逐项含 系统/模式/权益/场景 + 确权带出的第三方·隐私 + 先确后授生效卡片。</div>
      <!-- 清单级「是否跨系统域」判定(表6 专设;批量可跨多系统聚合),与一站式向导同口径 -->
      <el-alert v-if="detailRows.length" :type="crossSystemInfo.isCross ? 'warning' : 'info'" :closable="false" style="margin-bottom:10px">
        <template #title>
          <span>是否跨系统域:</span>
          <el-tag :type="crossSystemInfo.isCross ? 'warning' : 'info'" size="small" effect="dark" style="margin:0 6px">{{ crossSystemInfo.isCross ? '是(跨系统域)' : '否(单系统)' }}</el-tag>
          本清单覆盖 {{ crossSystemInfo.systems.length }} 个系统{{ crossSystemInfo.systems.length ? '(' + crossSystemInfo.systems.join('、') + ')' : '' }}。
        </template>
      </el-alert>
      <el-table :data="detailRows" v-loading="detailLoading" border stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="padding:6px 18px;line-height:1.9;color:var(--prm-color-text)">
              <div><b>业务域:</b>{{ row.businessDomain || '—' }}　<b>授权范围:</b>{{ row.scope || '—' }}　<b>授权时效:</b>{{ (row.validDate||'').slice(0,10) || '—' }}</div>
              <div><b>利益分配约定(附录D §3.4.4):</b>{{ row.benefitAllocation || '— 未填(协议签订前须补充)' }}</div>
              <div><b>安全保障要求(附录D §3.4.4):</b>{{ row.securityReq || '— 未填(协议签订前须补充)' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column type="index" label="序号" width="52" align="center" />
        <el-table-column prop="granteeOrg" label="被授权方" min-width="140" show-overflow-tooltip />
        <el-table-column label="所属系统" min-width="120" show-overflow-tooltip><template #default="{ row }">{{ sysName(row) }}</template></el-table-column>
        <el-table-column prop="schemaName" label="模式名称" width="100" show-overflow-tooltip><template #default="{ row }">{{ row.schemaName || '—' }}</template></el-table-column>
        <el-table-column prop="assetName" label="数据表" min-width="130" show-overflow-tooltip />
        <el-table-column prop="rightType" label="权益类型" width="130" />
        <el-table-column prop="scenario" label="使用场景" min-width="110" show-overflow-tooltip><template #default="{ row }">{{ row.scenario || '—' }}</template></el-table-column>
        <el-table-column label="涉第三方" width="88" align="center">
          <template #default="{ row }"><el-tag :type="involvesThird(row) ? 'warning' : 'info'" size="small" effect="plain">{{ involvesThird(row) ? '涉' : '否' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="涉隐私/商密" width="104" align="center">
          <template #default="{ row }"><el-tag :type="involvesSensitive(row) ? 'danger' : 'info'" size="small" effect="plain">{{ involvesSensitive(row) ? row.sensitiveType : '否' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="跨域" width="68" align="center">
          <template #default><el-tag :type="crossSystemInfo.isCross ? 'warning' : 'info'" size="small" effect="plain">{{ crossSystemInfo.isCross ? '是' : '否' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="equityCardId" label="生效卡片" width="120" show-overflow-tooltip><template #default="{ row }">{{ row.equityCardId || '—' }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }"><el-tag :type="tag(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!detailLoading && detailRows.length===0" description="该清单暂无明细项(可在'批量授权一站式向导'中逐条添加)" />
    </el-drawer>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { pageBatchList, createBatchList, submitBatchList, approveBatchList, listAuthByBatch, generateAgreementForBatch } from '@/api/authorize'
const statuses = ['草案', '申报稿', '批准']
const q = reactive({ current: 1, size: 10, listYear: '', listStatus: '' })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const dlg = ref(false); const form = reactive({ listYear: '', remark: '' })
const detailDrawer = ref(false); const detailRows = ref([]); const detailLoading = ref(false); const curList = ref({})
function tag(s) { return { 批准: 'success', 申报稿: 'warning', 草案: 'info', 已生效: 'success', 已驳回: 'danger' }[s] || 'warning' }
// 库表级:assetId=SYS:系统名 → 系统名;数据表名=assetName(库表名)。与向导/确权目录同一派生。
function sysName(row) { const a = row.assetId || ''; return a.startsWith('SYS:') ? a.slice(4) : (a || '—') }
// 合规判定(与向导逐项表一致):第三方/隐私商密 由确权事实带出,空/「无」视为不涉。
function involvesThird(row) { return !!(row.thirdPartySource && String(row.thirdPartySource).trim()) }
function involvesSensitive(row) { return !!(row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无') }
// 表6「是否跨系统域」是清单级属性(全清单系统并集>1),与向导 crossSystemInfo 同源,避免逐行 crossRegion 陈旧不一致。
const crossSystemInfo = computed(() => {
  const systems = [...new Set(detailRows.value.map(sysName).filter(s => s && s !== '—'))]
  const domains = [...new Set(detailRows.value.map(r => r.businessDomain).filter(Boolean))]
  return { systems, domains, isCross: systems.length > 1 || domains.length > 1 }
})
async function onDetail(row) {
  curList.value = row; detailDrawer.value = true; detailLoading.value = true
  try { detailRows.value = await listAuthByBatch(row.batchListId) || [] } finally { detailLoading.value = false }
}
async function load() { loading.value = true; try { const r = await pageBatchList({ ...q }); rows.value = r.records || []; total.value = r.total || 0 } finally { loading.value = false } }
function onAdd() { Object.assign(form, { listYear: '', remark: '' }); dlg.value = true }
async function onSave() { await createBatchList({ ...form }); ElMessage.success('已新增清单(草案)'); dlg.value = false; load() }
async function onSubmit(row) { await submitBatchList(row.batchListId); ElMessage.success('已提交为申报稿'); load() }
async function onApprove(row) { await approveBatchList(row.batchListId); ElMessage.success('领导小组办公室已批准'); load() }
// 一清单一协议:批准的批量清单 → 生成一份《运营授权协议》(清单各项=协议附件),幂等防重
async function onGenAgreement(row) {
  const id = await generateAgreementForBatch(row.batchListId)
  ElMessage.success(`已生成本清单的《运营授权协议》(${id});请到「协议工作台」签章/审核/存档`)
}
onMounted(load)
</script>
