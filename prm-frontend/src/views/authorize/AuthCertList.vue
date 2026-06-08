<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item>
          <el-button type="warning" plain @click="loadExpiring">到期预警(30天)</el-button>
          <el-button v-if="expiringMode" @click="exitExpiring">返回全部</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="certNo" label="授权证书编号" width="200" show-overflow-tooltip />
        <el-table-column prop="assetId" label="资产ID" width="140" show-overflow-tooltip />
        <el-table-column prop="granteeOrg" label="被授权方" min-width="150" show-overflow-tooltip />
        <el-table-column prop="rightType" label="授权权益" width="120" />
        <el-table-column prop="validDate" label="有效期至" width="160">
          <template #default="{ row }">{{ fmt(row.validDate) }}</template>
        </el-table-column>
        <el-table-column prop="certStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tooltip :disabled="!row.suspendReason" :content="row.suspendReason" placement="top">
              <el-tag :type="statusType(row.certStatus)">{{ row.certStatus }}</el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.certStatus === '已撤销'" @click="onRenew(row)">续签</el-button>
            <el-button link type="danger" :disabled="row.certStatus !== '生效'" @click="onRevoke(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="prm-table-note">
        注:证书撤销后数据资源权限将被回收;<strong>已暂停</strong>为监测联动熔断(违规/越权),整改后可"续签"恢复生效。
      </div>
      <el-pagination v-if="!expiringMode" style="margin-top:16px;justify-content:flex-end" background
        layout="total, prev, pager, next" :total="total" :current-page="query.current" :page-size="query.size"
        @current-change="onPage" />
    </div>

    <el-dialog v-model="renewDlg" title="授权证书续签" width="460px" align-center>
      <el-form label-width="100px">
        <el-form-item label="证书编号">{{ current.certNo }}</el-form-item>
        <el-form-item label="当前状态"><el-tag :type="statusType(current.certStatus)">{{ current.certStatus }}</el-tag></el-form-item>
        <el-form-item label="新有效期" required>
          <el-date-picker v-model="newValidDate" type="datetime" placeholder="选择续签到期时间"
            value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="confirmRenew">确定续签</el-button>
        <el-button @click="renewDlg = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageAuthCert, revokeAuthCert, renewAuthCert, expiringAuthCerts } from '@/api/authorize'

const query = reactive({ current: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const expiringMode = ref(false)

const renewDlg = ref(false)
const current = reactive({ certId: '', certNo: '', certStatus: '' })
const newValidDate = ref('')

function statusType(s) {
  if (s === '生效') return 'success'
  if (s === '已暂停') return 'warning'
  return 'danger'
}
function fmt(v) { return v ? String(v).replace('T', ' ').slice(0, 16) : '-' }

async function load() {
  loading.value = true
  try {
    const res = await pageAuthCert({ ...query })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}
async function loadExpiring() {
  loading.value = true
  try {
    expiringMode.value = true
    rows.value = await expiringAuthCerts(30)
    total.value = rows.value.length
  } finally { loading.value = false }
}
function exitExpiring() { expiringMode.value = false; query.current = 1; load() }
function onPage(p) { query.current = p; load() }

function onRevoke(row) {
  ElMessageBox.confirm(`确认撤销授权证书"${row.certNo}"吗`, '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(async () => { await revokeAuthCert(row.certId); ElMessage.success('已撤销'); reload() }).catch(() => {})
}
function onRenew(row) {
  Object.assign(current, { certId: row.certId, certNo: row.certNo, certStatus: row.certStatus })
  newValidDate.value = ''
  renewDlg.value = true
}
async function confirmRenew() {
  if (!newValidDate.value) { ElMessage.warning('请选择新有效期'); return }
  await renewAuthCert(current.certId, newValidDate.value)
  ElMessage.success(current.certStatus === '已暂停' ? '已续签并恢复生效' : '续签成功')
  renewDlg.value = false
  reload()
}
function reload() { expiringMode.value ? loadExpiring() : load() }

onMounted(load)
</script>
