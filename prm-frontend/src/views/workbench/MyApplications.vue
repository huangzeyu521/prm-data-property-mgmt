<template>
  <div class="prm-page">
    <div class="prm-toolbar">
      <h3 style="margin:0 12px 0 0">我的申请</h3>
      <el-radio-group v-model="tab">
        <el-radio-button v-for="t in tabs" :key="t" :value="t" :label="t">{{ t }}</el-radio-button>
      </el-radio-group>
      <el-input v-model="kw" placeholder="按资产名称筛选" clearable style="width:200px;margin-left:auto" @input="render" />
      <el-checkbox v-model="onlyMine" style="margin-left:12px" @change="load">仅看我提交的</el-checkbox>
      <el-button :loading="loading" @click="load" style="margin-left:8px">刷新</el-button>
    </div>
    <div class="prm-table-note">汇聚我在途与历史的确权 / 授权申请,一处掌握进度;驳回单可一键"修改重提"。</div>

    <el-table :data="view" v-loading="loading" border stripe>
      <el-table-column prop="domain" label="类型" width="90" align="center">
        <template #default="{ row }"><el-tag :type="row.domain==='确权'?'primary':'success'" effect="plain">{{ row.domain }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="no" label="申请编号" min-width="170" show-overflow-tooltip />
      <el-table-column prop="assetName" label="数据资产" min-width="160" show-overflow-tooltip />
      <el-table-column prop="status" label="当前状态" width="130" align="center">
        <template #default="{ row }"><el-tag :type="stTag(row.status)">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="goProgress(row)">查看进度</el-button>
          <el-button v-if="row.status==='已驳回'" link type="warning" @click="goReopen(row)">修改重提</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && view.length===0" style="text-align:center;padding:32px;color:#8c8c8c">
      暂无申请。可前往
      <el-link type="primary" @click="$router.push('/dpr/confirm/wizard')">确权申请</el-link> 或
      <el-link type="primary" @click="$router.push('/dpr/auth/wizard')">一事一议授权申请</el-link> 发起。
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { pageConfirmApply } from '@/api/confirm'
import { pageAuthApply } from '@/api/authorize'

const router = useRouter()
const tabs = ['全部', '确权', '授权']
const tab = ref('全部')
const kw = ref('')
const onlyMine = ref(true)
const loading = ref(false)
const rows = ref([])

const me = () => localStorage.getItem('X-User-Id') || ''

async function load() {
  loading.value = true
  try {
    const [c, a] = await Promise.all([
      pageConfirmApply({ pageNum: 1, pageSize: 200 }),
      pageAuthApply({ pageNum: 1, pageSize: 200 }),
    ])
    const cf = (c.records || []).map((r) => ({ domain: '确权', no: r.applyNo, assetName: r.assetName, status: r.status, createTime: r.createTime, id: r.applyId, creatorId: r.creatorId, raw: r }))
    const af = (a.records || []).map((r) => ({ domain: '授权', no: r.applyNo, assetName: r.assetName, status: r.status, createTime: r.createTime, id: r.applyId, creatorId: r.creatorId, raw: r }))
    let all = [...cf, ...af]
    if (onlyMine.value && me()) all = all.filter((r) => r.creatorId === me())
    all.sort((x, y) => String(y.createTime || '').localeCompare(String(x.createTime || '')))
    rows.value = all
  } finally { loading.value = false }
}

const view = computed(() => rows.value.filter((r) =>
  (tab.value === '全部' || r.domain === tab.value) &&
  (!kw.value || (r.assetName || '').includes(kw.value))))

function render() {}

function stTag(s) {
  if (s === '已完成' || s === '已生效') return 'success'
  if (s === '已驳回') return 'danger'
  if (s === '草稿') return 'info'
  return 'warning'
}

function goProgress(row) {
  router.push(row.domain === '确权' ? '/dpr/confirm/history' : '/dpr/auth/history')
}
function goReopen(row) {
  // 基于原单修改重提:暂存原单字段,向导读出预填为新申请(旧单保留已驳回终态,不重打)
  sessionStorage.setItem('prm-reopen', JSON.stringify({ domain: row.domain, raw: row.raw || {} }))
  const base = row.domain === '确权' ? '/dpr/confirm/wizard' : '/dpr/auth/wizard'
  router.push({ path: base, query: { reopen: '1' } })
}

onMounted(load)
</script>
