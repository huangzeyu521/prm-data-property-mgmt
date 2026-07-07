<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" :model="query">
        <el-form-item label="操作人">
          <el-input v-model="query.userName" placeholder="姓名" clearable style="width: 150px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="动作">
          <el-select v-model="query.action" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="a in ACTIONS" :key="a" :label="a" :value="a" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.result" placeholder="全部" clearable style="width: 110px">
            <el-option label="成功" value="成功" />
            <el-option label="失败" value="失败" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="range"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column prop="userName" label="操作人" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.userName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="action" label="动作" min-width="110">
          <template #default="{ row }"><span class="prm-c-primary">{{ row.action }}</span></template>
        </el-table-column>
        <el-table-column prop="target" label="对象" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.target || '—' }}</template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.detail || '—' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <span :class="'prm-c-' + ((row.result === '成功' ? 'success' : 'danger') || 'primary')">{{ row.result || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :current-page="query.current"
        :page-size="query.size"
        :page-sizes="[10, 20, 50, 100]"
        style="margin-top: 12px; justify-content: flex-end"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { pageOpLog } from '@/api/system'
import { useTablePage } from '@/composables/useTablePage'

const ACTIONS = ['登录', '新增用户', '编辑用户', '删除用户', '重置密码', '启用用户', '停用用户']

const range = ref([])
const { query, rows, total, loading, load, search: onSearch, reset: doReset, onPage: onPageChange, onSizeChange } = useTablePage(
  pageOpLog, { userName: '', action: '', result: '', createTimeStart: '', createTimeEnd: '' }
)

watch(range, (v) => {
  query.createTimeStart = v && v[0] ? v[0] : ''
  query.createTimeEnd = v && v[1] ? v[1] : ''
})

function onReset() { range.value = []; doReset() }

onMounted(load)
</script>

<style scoped>
.prm-page { padding: 16px; }
.prm-query-bar { margin-bottom: 16px; }
.prm-table-card { background: #fff; padding: 16px; border-radius: var(--prm-radius); }
</style>
