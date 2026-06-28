<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!--
  权益卡片生成管理:确权制卡(节点80)产物的全生命周期管理。
  对齐系统级确权 + 三权分置 + 确权变更版本化 + 权益到期模型:
  卡片粒度 = (系统 × 单一权属);系统名称由 assetId「SYS:<系统名>」派生;到期预警联动确权变更「权益到期」触发;
  确权变更产生新版本卡片(vN)并使旧卡失效(被取代)。
-->
<template>
  <div class="prm-page">
    <!-- 概览统计条(按系统名/权属过滤聚合,忽略状态):点选状态卡下钻 -->
    <div class="stat-bar">
      <div v-for="c in statCards" :key="c.key" class="stat-card" :class="[c.cls, { active: activeStat === c.key, clickable: c.click !== false }]"
        @click="c.click !== false && onStatClick(c)">
        <div class="stat-num">{{ stat[c.field] || 0 }}</div>
        <div class="stat-label">{{ c.label }}</div>
      </div>
    </div>

    <div class="prm-query-bar">
      <el-input v-model="query.sysName" placeholder="系统名称" clearable style="width:160px" @keyup.enter="onSearch" />
      <el-input v-model="query.tableName" placeholder="库表名称" clearable style="width:160px;margin-left:12px" @keyup.enter="onSearch" />
      <el-select v-model="query.cardStatus" placeholder="状态" clearable style="width:120px;margin-left:12px">
        <el-option label="正常" value="正常" /><el-option label="冻结" value="冻结" /><el-option label="失效" value="失效" />
      </el-select>
      <el-select v-model="query.rightType" placeholder="权属类型" clearable style="width:130px;margin-left:12px">
        <el-option label="持有权" value="持有权" /><el-option label="使用权" value="使用权" /><el-option label="经营权" value="经营权" />
      </el-select>
      <el-button type="primary" style="margin-left:12px" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column label="权益卡片编码" min-width="210" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="font-weight:600">{{ row.cardNo }}</span>
            <el-tag v-if="row.version && row.version > 1" type="info" size="small" effect="plain" style="margin-left:6px">v{{ row.version }}</el-tag>
            <el-tooltip v-if="row.supersededCardNo" :content="`本卡为变更后新版,取代旧卡 ${row.supersededCardNo}`" placement="top">
              <el-tag type="success" size="small" effect="plain" style="margin-left:4px">变更新版</el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="系统名称" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ sysName(row) }}</template>
        </el-table-column>
        <el-table-column label="库表(数据资产卡片)" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.tableCode ? row.assetName : '全系统' }}</template>
        </el-table-column>
        <el-table-column label="权属类型" width="100" align="center">
          <template #default="{ row }">
            <el-tooltip :content="rightTag(row.rightType).full" placement="top">
              <el-tag :type="rightTag(row.rightType).type" size="small" effect="plain">{{ rightTag(row.rightType).short }}</el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="rightOwner" label="权益所有者" min-width="150" show-overflow-tooltip />
        <el-table-column prop="scope" label="覆盖范围" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.scope || '—' }}</template>
        </el-table-column>
        <el-table-column label="有效期至" width="160">
          <template #default="{ row }">
            {{ fmtDate(row.validDate) }}
            <el-tag v-if="isDueSoon(row)" type="warning" size="small" effect="plain" style="margin-left:4px">{{ daysToExpire(row) }}天到期</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cardStatus" label="状态" width="84" align="center">
          <template #default="{ row }"><el-tag :type="statusTag(row.cardStatus)">{{ row.cardStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="480" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onDetail(row)">详情</el-button>
            <el-button link type="primary" :disabled="row.cardStatus !== '正常' || !canInitiate()" @click="onChange(row)">发起变更</el-button>
            <el-button link type="success" :disabled="row.cardStatus !== '正常' || !canInitiate()" @click="onAuthorize(row)">发起授权</el-button>
            <el-button link type="primary" @click="onPreview(row)">预览证书</el-button>
            <el-button link type="warning" :disabled="row.cardStatus !== '正常' || !canRisk()" @click="onFreeze(row)">冻结</el-button>
            <el-button link type="success" :disabled="row.cardStatus !== '冻结' || !canRisk()" @click="onUnfreeze(row)">解冻</el-button>
            <el-button link type="danger" :disabled="row.cardStatus === '失效' || !canRisk()" @click="onRevoke(row)">注销</el-button>
            <el-button link type="primary" @click="onLogs(row)">变更史</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="prm-table-note">
        注:卡片粒度=系统×库表×权属(三权分置,打在每张数据资产卡片上);制卡由确权终审通过后系统自动生成(工作指引节点80);冻结/失效卡不可用于下游授权(风险熔断);确权变更生成新版卡片(vN)并使旧卡失效;全程留痕(权益可追溯 §6)。
        <br>当前角色「<b>{{ roleLabel }}</b>」:<b>风险处置(冻结/解冻/注销)</b>须「合规管控小组/管理员」(§3.3.3 权益风险处置);<b>发起变更/授权</b>须「申报人/管理员」(§3.3.2 重新确权 / §2 先确后授)。非授权角色按钮已置灰。
      </div>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="query.current" :page-size="query.size" @current-change="onPage" @size-change="s=>{query.size=s;query.current=1;load()}" />
    </div>

    <el-dialog v-model="logDlg" title="权益卡片变更历史" width="560px" align-center>
      <el-timeline>
        <el-timeline-item v-for="l in logs" :key="l.logId" :timestamp="String(l.createTime).replace('T',' ').slice(0,19)">
          <b>{{ l.action }}</b>　{{ l.fromStatus || '—' }} → {{ l.toStatus }}　<span style="color:var(--prm-color-text-weak)">{{ l.reason }}</span>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="!logs.length" description="暂无变更记录" />
    </el-dialog>

    <!-- 结构化凭证详情 -->
    <el-dialog v-model="detailDlg" title="权益卡片结构化详情" width="660px" align-center>
      <el-descriptions title="卡片信息" :column="2" border size="small">
        <el-descriptions-item label="卡片编码">{{ cur.cardNo }}</el-descriptions-item>
        <el-descriptions-item label="版本">v{{ cur.version || 1 }}<span v-if="cur.supersededCardNo" style="color:var(--prm-color-text-weak)"> · 取代 {{ cur.supersededCardNo }}</span></el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusTag(cur.cardStatus)">{{ cur.cardStatus }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="有效期至">{{ fmtDate(cur.validDate) }}<el-tag v-if="isDueSoon(cur)" type="warning" size="small" effect="plain" style="margin-left:6px">{{ daysToExpire(cur) }}天到期</el-tag></el-descriptions-item>
        <el-descriptions-item label="来源确权单" :span="2">{{ cur.applyId || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions title="确权范围(库表级)" :column="2" border size="small" style="margin-top:12px">
        <el-descriptions-item label="所属系统">{{ sysName(cur) }}</el-descriptions-item>
        <el-descriptions-item label="模式名称">{{ cur.schemaName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="库表(数据资产卡片)">{{ cur.tableCode ? cur.assetName : '全系统' }}{{ cur.tableCode ? '（' + cur.tableCode + '）' : '' }}</el-descriptions-item>
        <el-descriptions-item label="权属类型">
          <el-tag :type="rightTag(cur.rightType).type" size="small" effect="plain">{{ rightTag(cur.rightType).short }}</el-tag>
          <span style="color:var(--prm-color-text-weak);margin-left:6px">{{ rightTag(cur.rightType).full }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="覆盖范围" :span="2">{{ cur.scope || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions title="权益要素(表4 数据权益内部管理)" :column="2" border size="small" style="margin-top:12px">
        <el-descriptions-item label="权益主体">{{ cur.rightOwner }}</el-descriptions-item>
        <el-descriptions-item label="归口单位">{{ cur.consolidatedUnit || '-' }}</el-descriptions-item>
        <el-descriptions-item label="权益取得方式">{{ cur.acquireMode || '认定' }}</el-descriptions-item>
        <el-descriptions-item label="授权单位">{{ cur.authorizingUnit || '—（认定取得,无）' }}</el-descriptions-item>
        <el-descriptions-item label="权益内容摘要" :span="2">{{ cur.rightsContent || '-' }}</el-descriptions-item>
        <el-descriptions-item label="权益凭证附件/说明" :span="2">{{ cur.rightsCredential || cur.rightSource || '确权认定资料' }}</el-descriptions-item>
        <el-descriptions-item label="确权时间">{{ fmtDate(cur.confirmTime) }}</el-descriptions-item>
        <el-descriptions-item label="权益期限(有效期至)">{{ fmtDate(cur.validDate) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button type="primary" @click="onPreview(cur)">预览证书</el-button><el-button @click="detailDlg=false">关闭</el-button></template>
    </el-dialog>

    <!-- 在线预览证书 -->
    <el-dialog v-model="certDlg" title="数据资产确权权属凭证 · 在线预览" width="600px" align-center>
      <div class="cert" id="cert-print">
        <div class="cert-title">数据资产确权权属凭证</div>
        <div class="cert-sub">中国南方电网有限责任公司 · 数据产权管理平台</div>
        <table class="cert-tbl">
          <tr><td class="k">凭证编号</td><td>{{ cur.cardNo }}<span v-if="cur.version && cur.version > 1"> （v{{ cur.version }}）</span></td></tr>
          <tr><td class="k">所属系统 / 模式</td><td>{{ sysName(cur) }}{{ cur.schemaName ? ' / ' + cur.schemaName : '' }}</td></tr>
          <tr><td class="k">确权范围</td><td>{{ cur.scope || '全系统库表' }}</td></tr>
          <tr><td class="k">权属类型</td><td>{{ rightTag(cur.rightType).full }}</td></tr>
          <tr><td class="k">权益主体</td><td>{{ cur.rightOwner }} · 归口 {{ cur.consolidatedUnit || '中国南方电网有限责任公司' }}</td></tr>
          <tr><td class="k">权益取得方式</td><td>{{ cur.acquireMode || '认定' }}{{ cur.authorizingUnit ? '(授权单位:' + cur.authorizingUnit + ')' : '' }}</td></tr>
          <tr><td class="k">权益内容摘要</td><td>{{ cur.rightsContent || rightTag(cur.rightType).full }}</td></tr>
          <tr><td class="k">权益凭证</td><td>{{ cur.rightsCredential || '确权认定资料' }}</td></tr>
          <tr><td class="k">确权时间 / 有效期至</td><td>{{ fmtDate(cur.confirmTime) }} ～ {{ fmtDate(cur.validDate) }}</td></tr>
        </table>
        <div class="cert-foot">
          <div class="cert-note">本凭证由数据产权管理平台依"三权分置"确权认定生成，已经区块链 SM3 指纹存证，真伪可溯。</div>
          <div class="cert-seal">确权专用章</div>
        </div>
      </div>
      <template #footer><el-button @click="printCert">打印 / 另存</el-button><el-button @click="certDlg=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageEquityCard, statsEquityCard, freezeEquityCard, unfreezeEquityCard, revokeEquityCard, equityCardLogs } from '@/api/confirm'
import { currentRole, ROLES } from '@/lib/roles'

const router = useRouter()
// 权益卡片动作角色门禁(对齐工作指引):风险处置(冻结/解冻/注销)= 合规管控小组/管理员;发起变更/授权 = 申报人/管理员。
// 与后端 @RequiresRole 双层防护;currentRole()='all'(演示默认)放行全部。
function roleAllowed(allowed) { const r = currentRole(); return r === 'all' || allowed.includes(r) }
function canRisk() { return roleAllowed(['admin', 'review']) }                  // §3.3.3 权益风险处置(合规管控小组)
function canInitiate() { return roleAllowed(['apply', 'business', 'admin']) }   // §3.3.2 重新确权 / §2 先确后授(数字化部/业务部门)
const roleLabel = computed(() => (ROLES.find(x => x.key === currentRole()) || {}).label || currentRole())
// 先确后授一键衔接:正常卡直接发起授权,带资产+卡号到授权向导
function onAuthorize(row) {
  router.push({ path: '/dpr/auth/wizard', query: { assetId: row.assetId, assetName: sysName(row), cardNo: row.cardNo } })
}
// 已确权系统「发起变更」:跳确权变更申请页(系统级,登记类型=确权变更)
function onChange(row) {
  router.push({ path: '/dpr/confirm/change', query: { assetId: row.assetId } })
}

// 一份确权 = 一个系统:系统名称权威源 = assetId「SYS:<系统名>」;assetName 兜底
function sysName(row) {
  const id = (row && row.assetId) || ''
  return id.startsWith('SYS:') ? id.slice(4) : ((row && row.assetName) || '-')
}
// 权属类型(单权)→ 短名标签,兼容"数据资源持有权/数据持有权"等命名
function rightTag(rt) {
  const s = String(rt || '')
  if (s.includes('持有')) return { short: '持有权', type: 'primary', full: s }
  if (s.includes('使用') || s.includes('加工')) return { short: '使用权', type: 'success', full: s }
  if (s.includes('经营') || s.includes('产品')) return { short: '经营权', type: 'warning', full: s }
  return { short: s || '—', type: 'info', full: s }
}
function daysToExpire(row) {
  if (!row || !row.validDate) return null
  const d = new Date(String(row.validDate).replace(' ', 'T'))
  return Math.ceil((d - new Date()) / 86400000)
}
// 即将到期:正常卡 + 90 天内到期(联动确权变更「权益到期」触发)
function isDueSoon(row) {
  if (!row || row.cardStatus !== '正常') return false
  const d = daysToExpire(row)
  return d !== null && d > 0 && d <= 90
}

const query = reactive({ current: 1, size: 10, sysName: '', tableName: '', cardStatus: '', rightType: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const logDlg = ref(false)
const logs = ref([])
const detailDlg = ref(false); const certDlg = ref(false)
const cur = ref({})

// ===== 概览统计条 =====
const stat = reactive({ total: 0, normal: 0, frozen: 0, expired: 0, dueSoon: 0 })
const activeStat = ref('')
const statCards = [
  { key: 'total', label: '总卡片', field: 'total', cls: 'c-total', click: 'clear' },
  { key: 'normal', label: '正常', field: 'normal', cls: 'c-done', click: { cardStatus: '正常' } },
  { key: 'dueSoon', label: '即将到期', field: 'dueSoon', cls: 'c-review', click: false },
  { key: 'frozen', label: '冻结', field: 'frozen', cls: 'c-review', click: { cardStatus: '冻结' } },
  { key: 'expired', label: '失效', field: 'expired', cls: 'c-reject', click: { cardStatus: '失效' } }
]
function onStatClick(c) {
  if (c.click === 'clear') { query.cardStatus = ''; activeStat.value = '' }
  else { query.cardStatus = c.click.cardStatus; activeStat.value = c.key }
  query.current = 1; load()
}

function statusTag(s) { return { 正常: 'success', 冻结: 'warning', 失效: 'danger' }[s] || 'info' }
function fmtDate(t) { return t ? String(t).replace('T', ' ').slice(0, 10) : '长期' }
function onDetail(row) { cur.value = row; detailDlg.value = true }
function onPreview(row) { cur.value = row; certDlg.value = true }
function printCert() { window.print() }
async function load() {
  loading.value = true
  try {
    const filters = { sysName: query.sysName, tableName: query.tableName, rightType: query.rightType }
    const [res, s] = await Promise.all([
      pageEquityCard({ ...query }),
      statsEquityCard(filters)
    ])
    rows.value = res.records || []
    total.value = res.total || 0
    Object.assign(stat, s || {})
  } finally { loading.value = false }
}
function onSearch() { query.current = 1; activeStat.value = ''; load() }
function onReset() { query.sysName = ''; query.tableName = ''; query.cardStatus = ''; query.rightType = ''; query.current = 1; activeStat.value = ''; load() }
function onPage(p) { query.current = p; load() }
function onFreeze(row) {
  ElMessageBox.confirm(`确认冻结权益卡片"${row.cardNo}"吗,冻结后不可用于授权`, '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(async () => { await freezeEquityCard(row.cardId); ElMessage.success('已冻结'); load() }).catch(() => {})
}
function onUnfreeze(row) {
  ElMessageBox.confirm(`确认解冻"${row.cardNo}"恢复正常吗`, '提示', { type: 'warning' })
    .then(async () => { await unfreezeEquityCard(row.cardId); ElMessage.success('已解冻'); load() }).catch(() => {})
}
function onRevoke(row) {
  ElMessageBox.prompt(`注销不可逆,请输入注销原因`, `注销权益卡片 ${row.cardNo}`, { inputType: 'textarea' })
    .then(async ({ value }) => { await revokeEquityCard(row.cardId, value); ElMessage.success('已注销'); load() }).catch(() => {})
}
async function onLogs(row) { logs.value = await equityCardLogs(row.cardId); logDlg.value = true }

onMounted(load)
</script>

<style scoped>
.stat-bar { display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.stat-card {
  flex: 1; min-width: 132px; background: #fff; border: 1px solid #eef0f3; border-left: 3px solid var(--prm-color-text-disabled);
  border-radius: 6px; padding: 12px 16px; transition: box-shadow .15s, transform .15s;
}
.stat-card.clickable { cursor: pointer; }
.stat-card.clickable:hover { box-shadow: 0 2px 10px rgba(18, 108, 253, .1); transform: translateY(-1px); }
.stat-card.active { box-shadow: 0 0 0 2px var(--prm-color-primary) inset; }
.stat-num { font-size: 26px; font-weight: 700; line-height: 1.1; color: var(--prm-color-text); }
.stat-label { font-size: 12.5px; color: var(--prm-color-text-weak); margin-top: 4px; }
.c-total { border-left-color: var(--prm-color-primary); }
.c-review { border-left-color: var(--prm-color-warning); }
.c-done { border-left-color: var(--prm-color-success); }
.c-reject { border-left-color: var(--prm-color-danger); }

.cert { border: 2px solid #b8893a; border-radius: 8px; padding: 24px 28px; background: linear-gradient(180deg, #fffdf7, #fdf6e8); }
.cert-title { text-align: center; font-size: 22px; font-weight: 800; letter-spacing: 4px; color: #8a5a1a; }
.cert-sub { text-align: center; font-size: 12px; color: #a07b3a; margin: 6px 0 18px; }
.cert-tbl { width: 100%; border-collapse: collapse; }
.cert-tbl td { border: 1px solid #e3cfa0; padding: 9px 12px; font-size: 13px; }
.cert-tbl td.k { width: 130px; background: #faf3e2; color: #6b5320; font-weight: 600; }
.cert-foot { display: flex; align-items: flex-end; justify-content: space-between; margin-top: 18px; gap: 16px; }
.cert-note { font-size: 11px; color: #9a8048; max-width: 60%; line-height: 1.5; }
.cert-seal {
  width: 96px; height: 96px; border: 2px solid #c0392b; border-radius: 50%;
  color: #c0392b; display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700; transform: rotate(-12deg); opacity: 0.85; text-align: center; line-height: 1.2;
}
</style>
