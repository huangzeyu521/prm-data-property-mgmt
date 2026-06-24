<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="资产ID"><el-input v-model="query.assetId" placeholder="资产ID" clearable style="width:150px" /></el-form-item>
        <el-form-item label="变更类型">
          <el-select v-model="query.changeType" placeholder="全部" clearable style="width:120px">
            <el-option v-for="t in changeTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源流程">
          <el-select v-model="query.sourceFlow" placeholder="全部" clearable style="width:130px">
            <el-option v-for="f in sourceFlows" :key="f" :label="f" :value="f" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人"><el-input v-model="query.operatorId" placeholder="操作人" clearable style="width:110px" /></el-form-item>
        <el-form-item label="变更时间">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" style="width:230px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="assetId" label="资产ID" width="150" show-overflow-tooltip />
        <el-table-column prop="changeType" label="变更类型" width="100" />
        <el-table-column prop="fieldName" label="变更字段" width="100" />
        <el-table-column prop="beforeValue" label="变更前" min-width="110" show-overflow-tooltip>
          <template #default="{ row }"><span style="color:#e21f0c">{{ row.beforeValue }}</span></template>
        </el-table-column>
        <el-table-column prop="afterValue" label="变更后" min-width="110" show-overflow-tooltip>
          <template #default="{ row }"><span style="color:#36b21d">{{ row.afterValue }}</span></template>
        </el-table-column>
        <el-table-column prop="changeReason" label="变更原因" min-width="130" show-overflow-tooltip />
        <el-table-column prop="sourceFlow" label="来源流程" width="110" show-overflow-tooltip />
        <el-table-column prop="sourceTicket" label="来源单号" width="120" show-overflow-tooltip />
        <el-table-column prop="operatorId" label="操作人" width="90" />
        <el-table-column prop="changeTime" label="变更时间" width="160" />
        <el-table-column label="上链凭证" width="96" align="center" fixed="right">
          <template #default="{ row }">
            <el-tooltip v-if="row.chainHash" :content="'SM3指纹/上链:' + row.chainHash" placement="top">
              <el-tag type="success" effect="plain" size="small">已上链</el-tag>
            </el-tooltip>
            <span v-else style="color:#bbb">-</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="prm-table-note">注:变更记录由确权/授权审批流自动生成,SM3 指纹上链(防篡改),不可人工删改;支持按 资产/类型/来源流程/操作人/时间 多维追溯。</div>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="query.current" :page-size="query.size" @current-change="onPage" @size-change="s=>{query.size=s;query.current=1;load()}" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageChangeRecord } from '@/api/ledger'

const changeTypes = ['确权', '授权', '权益变更', '重确权', '熔断']
const sourceFlows = ['确权流程', '授权流程', '重确权', '监测联动']
const query = reactive({ current: 1, size: 10, assetId: '', changeType: '', sourceFlow: '', operatorId: '', changeTimeStart: '', changeTimeEnd: '' })
const dateRange = ref([])
const rows = ref([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  query.changeTimeStart = dateRange.value?.[0] || ''
  query.changeTimeEnd = dateRange.value?.[1] ? dateRange.value[1] + ' 23:59:59' : ''
  try {
    const res = await pageChangeRecord({ ...query })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}
function onSearch() { query.current = 1; load() }
function onReset() {
  Object.assign(query, { assetId: '', changeType: '', sourceFlow: '', operatorId: '', changeTimeStart: '', changeTimeEnd: '' })
  dateRange.value = []
  onSearch()
}
function onPage(p) { query.current = p; load() }
onMounted(load)
</script>
