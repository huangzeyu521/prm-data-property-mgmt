<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="规则名称">
          <el-input v-model="query.ruleName" placeholder="请输入规则名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="生效状态">
          <el-select v-model="query.effectStatus" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="s in effectStatuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="prm-table-card">
      <div style="margin-bottom: 12px"><el-button type="primary" @click="onCreate">新增</el-button></div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="ruleName" label="规则名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ruleCategory" label="分类" width="120" />
        <el-table-column prop="threshold" label="阈值" width="90" />
        <el-table-column prop="notifyChannel" label="通知方式" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-for="c in (row.notifyChannel ? row.notifyChannel.split(',') : [])" :key="c" size="small" effect="plain" style="margin-right:4px">{{ c }}</el-tag>
            <span v-if="!row.notifyChannel">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="ruleVersion" label="版本" width="70" align="center" />
        <el-table-column label="联动熔断" width="100" align="center">
          <template #default="{ row }"><el-tag v-if="row.circuitBreak" type="danger">熔断</el-tag><span v-else>—</span></template>
        </el-table-column>
        <el-table-column prop="effectStatus" label="生效状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="statusTag(row.effectStatus)">{{ row.effectStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)">修改</el-button>
            <el-button link type="success" :disabled="row.effectStatus === '生效中'" @click="onEnable(row)">启用</el-button>
            <el-button link type="warning" :disabled="row.effectStatus === '停用'" @click="onDisable(row)">停用</el-button>
            <el-button link type="danger" :disabled="row.effectStatus !== '草稿'" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="prm-table-note">注:仅"草稿"规则可物理删除;生效中/历史规则不支持物理删除,仅可停用并生成新版本(守可审计红线)。</div>
      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]" :total="total"
        :current-page="query.current" :page-size="query.size" @current-change="onPage" @size-change="s=>{query.size=s;query.current=1;load()}" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" align-center :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="规则名称" prop="ruleName"><el-input v-model="form.ruleName" /></el-form-item>
        <el-form-item label="规则分类">
          <el-select v-model="form.ruleCategory" placeholder="请选择" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="监测对象"><el-input v-model="form.monitorTarget" /></el-form-item>
        <el-form-item label="阈值"><el-input v-model="form.threshold" placeholder="如:30(天)" /></el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" placeholder="请选择" style="width: 100%">
            <el-option v-for="p in priorities" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知对象"><el-input v-model="form.notifyTarget" /></el-form-item>
        <el-form-item label="通知方式">
          <el-select v-model="channelArr" multiple collapse-tags placeholder="选择通知方式(可多选)" style="width: 100%">
            <el-option v-for="c in channels" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="联动熔断">
          <el-switch v-model="form.circuitBreak" active-text="命中即暂停授权" inline-prompt />
          <span style="margin-left:10px;color:var(--prm-color-text-secondary);font-size:12px">开启后,违规命中将自动暂停被授权资产的生效证书并追责</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="onSubmit">确定</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageRule, createRule, updateRule, enableRule, disableRule, deleteRule } from '@/api/monitor'

const effectStatuses = ['草稿', '生效中', '停用']
const categories = ['权属变动', '调用异常', '到期提醒', '合规', '申请审核']
const priorities = ['高', '中', '低']
const channels = ['站内信', '邮件', '短信', 'eLink']
const channelArr = ref([])

const query = reactive({ current: 1, size: 10, ruleName: '', effectStatus: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('新增规则')
const formRef = ref()
const form = reactive(emptyForm())
const rules = { ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }] }

function emptyForm() {
  return { ruleId: '', ruleName: '', ruleCategory: '', monitorTarget: '', threshold: '', priority: '', notifyTarget: '', notifyChannel: '', circuitBreak: false }
}
function statusTag(s) {
  return { 生效中: 'success', 草稿: 'info', 停用: 'warning' }[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await pageRule({ ...query })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}
function onSearch() { query.current = 1; load() }
function onReset() { query.ruleName = ''; query.effectStatus = ''; onSearch() }
function onPage(p) { query.current = p; load() }

function onCreate() { Object.assign(form, emptyForm()); channelArr.value = []; dialogTitle.value = '新增规则'; dialogVisible.value = true }
function onEdit(row) { Object.assign(form, row); channelArr.value = row.notifyChannel ? row.notifyChannel.split(',') : []; dialogTitle.value = '修改规则'; dialogVisible.value = true }

async function onSubmit() {
  await formRef.value.validate()
  form.notifyChannel = channelArr.value.join(',')
  if (form.ruleId) { await updateRule({ ...form }); ElMessage.success('保存成功') }
  else { await createRule({ ...form }); ElMessage.success('新增成功') }
  dialogVisible.value = false
  load()
}
async function onEnable(row) { await enableRule(row.ruleId); ElMessage.success('已启用'); load() }
async function onDisable(row) { await disableRule(row.ruleId); ElMessage.success('已停用'); load() }
async function onDelete(row) {
  await ElMessageBox.confirm(`确认物理删除草稿规则「${row.ruleName}」?`, '删除确认', { type: 'warning' })
  await deleteRule(row.ruleId)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
