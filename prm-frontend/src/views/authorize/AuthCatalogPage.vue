<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="名称"><el-input v-model="q.name" clearable style="width:200px" /></el-form-item>
        <el-form-item><el-button type="primary" @click="load">查询</el-button><el-button type="primary" @click="onAdd">新增</el-button></el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="itemType" label="类型" width="150" />
        <el-table-column prop="version" label="版本" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="row.status==='生效中'?'success':'warning'">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" :disabled="row.status==='生效中'" @click="onEnable(row)">启用</el-button>
            <el-button link type="warning" :disabled="row.status==='停用'" @click="onDisable(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>
    <el-dialog v-model="dlg" :title="'新增'+label" width="520px" align-center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="form.itemType" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSave">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pageCatalog, saveCatalog, enableCatalog, disableCatalog } from '@/api/authorize'
const route = useRoute()
const category = ref(route.meta.category)
const label = ref(route.meta.title || '目录项')
const q = reactive({ current: 1, size: 10, name: '', category: category.value })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const dlg = ref(false); const form = reactive({ name: '', itemType: '', content: '' })
async function load() { loading.value = true; try { const r = await pageCatalog({ ...q, category: category.value }); rows.value = r.records || []; total.value = r.total || 0 } finally { loading.value = false } }
function onAdd() { Object.assign(form, { name: '', itemType: '', content: '' }); dlg.value = true }
async function onSave() { await saveCatalog({ category: category.value, ...form }); ElMessage.success('已保存'); dlg.value = false; load() }
async function onEnable(row) { await enableCatalog(row.itemId); ElMessage.success('已启用'); load() }
async function onDisable(row) { await disableCatalog(row.itemId); ElMessage.success('已停用'); load() }
watch(() => route.meta.category, (c) => { category.value = c; label.value = route.meta.title; q.current = 1; load() })
onMounted(load)
</script>
