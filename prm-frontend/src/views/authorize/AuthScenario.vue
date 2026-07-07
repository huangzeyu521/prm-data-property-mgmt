<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词"><el-input v-model="q.keyword" placeholder="场景名称/描述" clearable style="width:180px" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="q.category" placeholder="全部" clearable style="width:140px">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用权益">
          <el-select v-model="q.rightType" placeholder="全部" clearable style="width:150px">
            <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="q.status" placeholder="全部" clearable style="width:120px">
            <el-option label="生效中" value="生效中" /><el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
          <el-button type="primary" @click="onAdd">新增场景</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="scenarioName" label="场景名称" width="150" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="110" align="center"><template #default="{ row }"><span class="prm-c-primary">{{ row.category }}</span></template></el-table-column>
        <el-table-column prop="rightType" label="适用权益类型" width="140" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((row.rightType==='经营权'?'warning':(row.rightType==='使用权'?'primary':'info')) || 'primary')">{{ row.rightType || '通用' }}</span></template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="reasonTemplate" label="申请原因模板" min-width="220" show-overflow-tooltip />
        <el-table-column prop="scenarioStatus" label="状态" width="90" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((row.scenarioStatus==='生效中'?'success':'warning') || 'primary')">{{ row.scenarioStatus }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)">修改</el-button>
            <el-button link type="success" :disabled="row.scenarioStatus==='生效中'" @click="onEnable(row)">启用</el-button>
            <el-button link type="warning" :disabled="row.scenarioStatus==='停用'" @click="onDisable(row)">停用</el-button>
            <el-button link type="danger" @click="onDel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="prm-table-note">注:应用场景与申请原因模板供授权申请时"选择/搜索"使用;停用的场景不出现在申请选择列表中。</div>
      <el-pagination style="margin-top:20px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>

    <el-dialog v-model="dlg" :title="form.scenarioId ? '修改应用场景' : '新增应用场景'" width="560px" align-center>
      <el-form :model="form" label-width="150px">
        <el-form-item label="场景名称"><el-input v-model="form.scenarioName" /></el-form-item>
        <el-form-item label="场景分类">
          <el-select v-model="form.category" allow-create filterable default-first-option style="width:100%" placeholder="选择或输入分类">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用权益类型">
          <el-select v-model="form.rightType" style="width:100%" placeholder="该场景适用的授权权益(向导按此过滤场景)">
            <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
          </el-select>
          <div style="font-size:12px;color:var(--prm-color-text-weak);line-height:1.5;margin-top:2px">通用=使用权/经营权均可选;经营场景须经营权(对外经营仅限对外开放目录 §3.4.3)</div>
        </el-form-item>
        <el-form-item label="场景描述"><el-input v-model="form.description" type="textarea" maxlength="500" show-word-limit :rows="2" /></el-form-item>
        <el-form-item label="申请原因模板"><el-input v-model="form.reasonTemplate" type="textarea" maxlength="500" show-word-limit :rows="3" placeholder="申请人选中本场景时自动带出的申请原因" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" :loading="saving" @click="onSave">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { confirmAsync } from '@/utils/confirmAsync'
import { pageScenario, createScenario, updateScenario, deleteScenario, enableScenario, disableScenario } from '@/api/authorize'
import { useTablePage } from '@/composables/useTablePage'

const categories = ['内部分析', '对外服务', '联合建模', '监管报送']
// 适用权益类型:对齐授权权益(使用权/经营权)+ 通用,供一站式向导按权益类型过滤场景
const rightTypes = ['使用权', '经营权', '通用']
const { query: q, rows, total, loading, load, search: onSearch, reset: onReset } = useTablePage(pageScenario, { keyword: '', category: '', status: '', rightType: '' })
const dlg = ref(false); const saving = ref(false)
const form = reactive({ scenarioId: '', scenarioName: '', category: '内部分析', rightType: '通用', description: '', reasonTemplate: '' })

function onAdd() { Object.assign(form, { scenarioId: '', scenarioName: '', category: '内部分析', rightType: '通用', description: '', reasonTemplate: '' }); dlg.value = true }
function onEdit(row) { Object.assign(form, { scenarioId: row.scenarioId, scenarioName: row.scenarioName, category: row.category, rightType: row.rightType || '通用', description: row.description || '', reasonTemplate: row.reasonTemplate || '' }); dlg.value = true }
async function onSave() {
  if (!form.scenarioName) { ElMessage.warning('请填写场景名称'); return }
  saving.value = true
  try {
    if (form.scenarioId) { await updateScenario({ ...form }); ElMessage.success('已修改') }
    else { await createScenario({ ...form }); ElMessage.success('已新增') }
    dlg.value = false; load()
  } finally { saving.value = false }
}
async function onEnable(row) { await enableScenario(row.scenarioId); ElMessage.success('已启用'); load() }
async function onDisable(row) { await disableScenario(row.scenarioId); ElMessage.success('已停用'); load() }
function onDel(row) {
  confirmAsync('确认删除该应用场景吗', '提示',
    async () => { await deleteScenario(row.scenarioId); ElMessage.success('已删除'); load() }).catch(() => {})
}
onMounted(load)
</script>
