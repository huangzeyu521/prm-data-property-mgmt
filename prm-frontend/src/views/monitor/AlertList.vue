<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="5"><el-card shadow="hover"><div class="st"><b>{{ stats.total }}</b><span>预警总数</span></div></el-card></el-col>
      <el-col :span="5"><el-card shadow="hover"><div class="st"><b class="prm-c-danger">{{ stats.pending }}</b><span>待处理</span></div></el-card></el-col>
      <el-col :span="5"><el-card shadow="hover"><div class="st"><b class="prm-c-warning">{{ stats.processing }}</b><span>处理中</span></div></el-card></el-col>
      <el-col :span="5"><el-card shadow="hover"><div class="st"><b class="prm-c-success">{{ stats.closed }}</b><span>已关闭</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="prm-c-primary">{{ stats.closureRate }}%</b><span>整改闭环率</span></div></el-card></el-col>
    </el-row>

    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="预警级别">
          <el-select v-model="query.alertLevel" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="l in levels" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="处置状态">
          <el-select v-model="query.disposeStatus" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
          <el-button type="warning" @click="onRunCheck">合规巡检</el-button>
          <el-button type="danger" @click="vioDlg = true">违规上报熔断</el-button>
          <el-button type="primary" plain @click="rcDlg = true">权属变动→重确权</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-dialog v-model="rcDlg" title="权属变动 · 联动派生重确权" width="500px" align-center>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:10px"
        title="附录F §3.3.2 四触发:数据新增/数据来源变更/管理要求变更/权益到期 将生成重要预警并派生重确权工单(草稿)进入确权变更流程(按季度重确权)。" />
      <el-form :model="rc" label-width="100px">
        <el-form-item label="资产ID" required><el-input v-model="rc.assetId" placeholder="发生权属变动的资产ID" /></el-form-item>
        <el-form-item label="资产名称"><el-input v-model="rc.assetName" /></el-form-item>
        <el-form-item label="触发类型">
          <el-select v-model="rc.triggerType" style="width:100%">
            <el-option v-for="t in triggerTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="权属类型">
          <el-select v-model="rc.rightType" style="width:100%">
            <el-option label="持有权" value="持有权" /><el-option label="使用权" value="使用权" /><el-option label="经营权" value="经营权" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="rc.desc" type="textarea" maxlength="500" show-word-limit :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="confirmReConfirm">派生重确权</el-button><el-button @click="rcDlg = false">取消</el-button></template>
    </el-dialog>

    <el-dialog v-model="vioDlg" title="违规上报 · 联动熔断授权" width="500px" align-center>
      <el-alert type="error" :closable="false" show-icon style="margin-bottom:10px"
        title="附录F 3.4.5:确认后将生成紧急预警,并联动暂停该资产下全部生效授权证书 + 自动建追责。" />
      <el-form :model="vio" label-width="100px">
        <el-form-item label="资产ID" required><el-input v-model="vio.assetId" placeholder="涉事数据资产ID" /></el-form-item>
        <el-form-item label="违规类型">
          <el-select v-model="vio.violationType" style="width:100%">
            <el-option v-for="t in violationTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则ID"><el-input v-model="vio.ruleId" placeholder="可空;关联熔断规则则按规则配置" /></el-form-item>
        <el-form-item label="异常描述"><el-input v-model="vio.desc" type="textarea" maxlength="500" show-word-limit :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button type="danger" @click="confirmViolation">确认熔断</el-button><el-button @click="vioDlg = false">取消</el-button></template>
    </el-dialog>

    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="alertLevel" label="级别" width="90" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((levelTag(row.alertLevel)) || 'primary')">{{ row.alertLevel }}</span></template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column label="所属系统" min-width="120" show-overflow-tooltip><template #default="{ row }">{{ sysName(row) }}</template></el-table-column>
        <el-table-column prop="assetName" label="数据表" min-width="120" show-overflow-tooltip><template #default="{ row }">{{ row.assetName || '—' }}</template></el-table-column>
        <el-table-column prop="abnormalDesc" label="异常描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="disposeStatus" label="处置状态" width="100" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((statusTag(row.disposeStatus)) || 'primary')">{{ row.disposeStatus }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.disposeStatus === '已关闭'" @click="onDispose(row)">处置</el-button>
            <el-button link type="success" :disabled="row.disposeStatus === '已关闭'" @click="onClose(row)">关闭</el-button>
            <el-button link type="primary" @click="onPush(row)">推送</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 20px; justify-content: flex-end"
        background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]" :total="total"
        :current-page="query.current" :page-size="query.size" @current-change="onPage" @size-change="s=>{query.size=s;query.current=1;load()}" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { confirmAsync } from '@/utils/confirmAsync'
import { pageAlert, disposeAlert, closeAlert, getAlertStats, runComplianceCheck, reportViolation, triggerReConfirm, pushAlert } from '@/api/monitor'
import { useTablePage } from '@/composables/useTablePage'

const levels = ['紧急', '重要', '普通']
const statuses = ['待处理', '处理中', '已关闭']
const violationTypes = ['越权调用', '违规使用', '超范围', '到期未续']
// 确权变更 §3.3.2 四触发(对齐 ConfirmWizard CHANGE_TRIGGER_OPTS):数据新增/数据来源变更/管理要求变更/权益到期
const triggerTypes = ['数据新增', '数据来源变更', '管理要求变更', '权益到期']
const vioDlg = ref(false)
const vio = reactive({ assetId: '', violationType: '越权调用', ruleId: '', desc: '' })
const rcDlg = ref(false)
const rc = reactive({ assetId: '', assetName: '', triggerType: '数据来源变更', rightType: '持有权', desc: '' })
// 库表级:assetId=SYS:系统名 → 所属系统;非 SYS: 原样(兼容旧告警 assetId)
function sysName(row) { const a = (row && row.assetId) || ''; return a.startsWith('SYS:') ? a.slice(4) : (a || '—') }
const stats = reactive({ total: 0, pending: 0, processing: 0, closed: 0, closureRate: 0 })
// 每次拉取后刷新顶部统计卡片(onLoaded 钩子:search/reset/onPage/受理处置后均覆盖)
const { query, rows, total, loading, load, search: onSearch, reset: onReset, onPage } = useTablePage(
  pageAlert, { alertLevel: '', disposeStatus: '' }, { onLoaded: loadStats }
)

function levelTag(l) {
  return { 紧急: 'danger', 重要: 'warning', 普通: 'info' }[l] || 'info'
}
function statusTag(s) {
  return { 已关闭: 'success', 处理中: 'warning', 待处理: 'danger' }[s] || 'info'
}

async function loadStats() {
  Object.assign(stats, await getAlertStats())
}

function onDispose(row) {
  ElMessageBox.prompt('请输入处置说明', '受理处置', { confirmButtonText: '确定', cancelButtonText: '取消' })
    .then(async ({ value }) => { await disposeAlert(row.alertId, value); ElMessage.success('已受理'); load() })
    .catch(() => {})
}
function onClose(row) {
  ElMessageBox.prompt('请输入整改闭环说明', '处置闭环', { confirmButtonText: '确定', cancelButtonText: '取消' })
    .then(async ({ value }) => { await closeAlert(row.alertId, value); ElMessage.success('已闭环'); load() })
    .catch(() => {})
}
async function onPush(row) {
  await pushAlert(row.alertId)
  ElMessage.success('已按规则通知方式定向推送责任人')
}
function onRunCheck() {
  confirmAsync('将对已确权档案执行到期合规巡检并生成预警,是否继续', '提示', async () => {
    const n = await runComplianceCheck(30)
    ElMessage.success(`巡检完成,发现 ${n} 条到期风险`)
    load()
  }).catch(() => {})
}
async function confirmViolation() {
  if (!vio.assetId) { ElMessage.warning('请填写资产ID'); return }
  const r = await reportViolation(vio.assetId, vio.ruleId, vio.violationType, vio.desc)
  if (r.circuitBroken) {
    ElMessage.success(`已熔断:暂停 ${r.suspendedCount} 张授权证书并建追责`)
  } else {
    ElMessage.warning('规则未启用熔断,仅生成预警留痕')
  }
  vioDlg.value = false
  Object.assign(vio, { assetId: '', violationType: '越权调用', ruleId: '', desc: '' })
  load()
}
async function confirmReConfirm() {
  if (!rc.assetId) { ElMessage.warning('请填写资产ID'); return }
  const r = await triggerReConfirm(rc.assetId, rc.assetName, rc.rightType, rc.triggerType, rc.desc)
  ElMessage.success(r.reConfirmId ? `已派生重确权工单 ${r.reConfirmId}` : '已生成预警,重确权工单已联动派生(本地桩)')
  rcDlg.value = false
  Object.assign(rc, { assetId: '', assetName: '', triggerType: '数据来源变更', rightType: '持有权', desc: '' })
  load()
}

onMounted(load)
</script>

<style scoped>
.st { text-align: center; }
.st b { display: block; font-size: 26px; }
.st span { color: var(--prm-color-text-secondary); font-size: 12px; }
</style>
