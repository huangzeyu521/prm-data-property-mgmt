<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
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
        <el-table-column prop="certNo" label="生效记录编号" width="200" show-overflow-tooltip />
        <el-table-column label="所属系统" min-width="130" show-overflow-tooltip><template #default="{ row }">{{ sysName(row) }}</template></el-table-column>
        <el-table-column prop="granteeOrg" label="被授权方" min-width="150" show-overflow-tooltip />
        <el-table-column prop="rightType" label="授权权益" width="120" />
        <el-table-column prop="templateName" label="记录样式" min-width="160" show-overflow-tooltip><template #default="{ row }">{{ row.templateName || '—' }}</template></el-table-column>
        <el-table-column prop="validDate" label="有效期至" width="150">
          <template #default="{ row }">{{ fmt(row.validDate) }}</template>
        </el-table-column>
        <el-table-column prop="certStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tooltip :disabled="!row.suspendReason" :content="row.suspendReason" placement="top">
              <span :class="'prm-c-' + ((statusType(row.certStatus)) || 'primary')">{{ row.certStatus }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onPreview(row)">查看记录</el-button>
            <el-button link type="primary" :disabled="row.certStatus === '已撤销'" @click="onRenew(row)">续签</el-button>
            <el-button link type="danger" :disabled="row.certStatus !== '生效'" @click="onRevoke(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="prm-table-note">
        注:本页为系统<strong>授权生效记录</strong>(内部台账,承载续签/暂停/撤销等动态跟踪);按 35号文,对外授权凭证为《数据运营授权协议(附录D)》。
        记录撤销后数据资源权限将被回收;<strong>已暂停</strong>为监测联动熔断(违规/越权),整改后可"续签"恢复生效。
      </div>
      <el-pagination v-if="!expiringMode" style="margin-top:20px;justify-content:flex-end" background
        layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]" :total="total" :current-page="query.current" :page-size="query.size"
        @current-change="onPage" @size-change="s=>{query.size=s;query.current=1;load()}" />
    </div>

    <el-dialog v-model="renewDlg" title="授权生效记录续签" width="460px" align-center>
      <el-form label-width="100px">
        <el-form-item label="记录编号">{{ current.certNo }}</el-form-item>
        <el-form-item label="当前状态"><span :class="'prm-c-' + ((statusType(current.certStatus)) || 'primary')">{{ current.certStatus }}</span></el-form-item>
        <el-form-item label="新有效期" required>
          <el-date-picker v-model="newValidDate" type="datetime" placeholder="选择续签到期时间"
            value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="confirmRenew">确定续签</el-button>
        <el-button @click="renewDlg = false">取消</el-button>
      </template>
    </el-dialog>

    <!-- 在线查看授权生效记录 -->
    <el-dialog v-model="certDlg" title="授权生效记录 · 在线查看" width="620px" align-center>
      <el-alert v-if="certVO" type="info" :closable="false" style="margin-bottom:6px">
        本记录为系统台账凭证(生效登记/动态跟踪);对外授权凭证为《数据运营授权协议(附录D)》。
      </el-alert>
      <el-alert v-if="certVO" :type="certVO.complianceOk ? 'success' : 'error'" :closable="false" style="margin-bottom:10px">
        合规校验：{{ certVO.complianceResult }}
      </el-alert>
      <div v-if="certVO" class="cert">
        <div class="cert-title">数据授权生效记录</div>
        <div class="cert-sub">{{ certVO.certType || '授权生效记录' }}　中国南方电网有限责任公司</div>
        <div class="cert-no">记录编号：{{ certVO.certNo }}</div>
        <table class="cert-tbl">
          <tbody>
          <tr><td class="k">被授权方</td><td>{{ certVO.granteeOrg }}</td></tr>
          <tr><td class="k">所属系统</td><td>{{ certVO.sysName || certVO.assetId || '—' }}</td></tr>
          <tr><td class="k">数据表</td><td>{{ certVO.assetName || '—' }}<span v-if="certVO.schemaName" style="color:#6a82aa">（模式 {{ certVO.schemaName }}）</span></td></tr>
          <tr><td class="k">授权权益类型</td><td>{{ certVO.rightType }}</td></tr>
          <tr><td class="k">使用场景及目的</td><td>{{ certVO.scenario || '—' }}</td></tr>
          <tr><td class="k">授权范围</td><td>{{ certVO.scope || '—' }}</td></tr>
          <tr><td class="k">有效期至</td><td>{{ fmt(certVO.validDate) }}</td></tr>
          <tr><td class="k">记录样式</td><td>{{ certVO.templateName || '—' }}</td></tr>
          <tr><td class="k">状态</td><td>{{ certVO.certStatus }}</td></tr>
          </tbody>
        </table>
        <div v-if="certVO.templateContent" class="cert-body">{{ certVO.templateContent }}</div>
        <div class="cert-foot">
          <div class="cert-note">本记录依双签归档的《数据运营授权协议(附录D)》与确权信息标准化登记,授权范围不超确权边界,已经区块链 SM3 存证;对外凭证以协议为准。</div>
          <div class="cert-seal">系统登记</div>
        </div>
      </div>
      <template #footer><el-button @click="printCert">打印</el-button><el-button @click="certDlg=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { confirmAsync } from '@/utils/confirmAsync'
import { pageAuthCert, getAuthCertRender, revokeAuthCert, renewAuthCert, expiringAuthCerts } from '@/api/authorize'
import { useTablePage } from '@/composables/useTablePage'

const { query, rows, total, loading, load, onPage } = useTablePage(pageAuthCert, {})
const expiringMode = ref(false)

const renewDlg = ref(false)
const current = reactive({ certId: '', certNo: '', certStatus: '' })
const newValidDate = ref('')

function statusType(s) {
  if (s === '生效') return 'success'
  if (s === '已暂停') return 'warning'
  return 'danger'
}
// 库表级:assetId=SYS:系统名 → 所属系统(凭证不暴露 raw assetId)。与向导/审核台/历史页同一派生。
function sysName(row) { const a = (row && row.assetId) || ''; return a.startsWith('SYS:') ? a.slice(4) : (a || '—') }
function fmt(v) { return v ? String(v).replace('T', ' ').slice(0, 16) : '-' }

async function loadExpiring() {
  loading.value = true
  try {
    expiringMode.value = true
    rows.value = await expiringAuthCerts(30)
    total.value = rows.value.length
  } finally { loading.value = false }
}
function exitExpiring() { expiringMode.value = false; query.current = 1; load() }

function onRevoke(row) {
  confirmAsync(`确认撤销授权生效记录"${row.certNo}"吗(数据资源权限将被回收)`, '提示',
    async () => { await revokeAuthCert(row.certId); ElMessage.success('已撤销'); reload() }).catch(() => {})
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

const certDlg = ref(false); const certVO = ref(null)
async function onPreview(row) { certVO.value = await getAuthCertRender(row.certId); certDlg.value = true }
function printCert() { window.print() }

onMounted(load)
</script>

<style scoped>
.cert { border: 2px solid #1e87f0; border-radius: 8px; padding: 24px 28px; background: linear-gradient(180deg, #f7faff, #eef4ff); }
.cert-title { text-align: center; font-size: 22px; font-weight: 800; letter-spacing: 4px; color: #1c4ec2; }
.cert-sub { text-align: center; font-size: 13px; color: #4a72c5; margin: 6px 0 4px; }
.cert-no { text-align: center; font-size: 12px; color: #c0392b; font-weight: 600; margin-bottom: 16px; }
.cert-tbl { width: 100%; border-collapse: collapse; }
.cert-tbl td { border: 1px solid #c7d8f5; padding: 9px 12px; font-size: 13px; }
.cert-tbl td.k { width: 120px; background: #eaf1ff; color: #2a456f; font-weight: 600; }
.cert-body { margin-top: 12px; font-size: 12px; color: #3a4f6f; line-height: 1.7; white-space: pre-wrap; }
.cert-foot { display: flex; align-items: flex-end; justify-content: space-between; margin-top: 18px; gap: 16px; }
.cert-note { font-size: 11px; color: #6a82aa; max-width: 62%; line-height: 1.5; }
.cert-seal { width: 96px; height: 96px; border: 2px solid #c0392b; border-radius: 50%; color: #c0392b; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; transform: rotate(-12deg); opacity: 0.85; text-align: center; line-height: 1.2; }
</style>
