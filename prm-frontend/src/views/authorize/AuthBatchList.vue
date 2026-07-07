<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="年度"><el-input v-model="q.listYear" placeholder="如 2026" clearable style="width:140px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="q.listStatus" placeholder="全部" clearable style="width:130px">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="load">查询</el-button><el-button v-if="isMaintainer" type="primary" @click="onAdd">新增清单</el-button></el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <PageNote>注:表6《数据批量授权清单》流转 草案 → 申报稿 → 领导小组办公室批准。</PageNote>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="listNo" label="清单编号" width="180" show-overflow-tooltip />
        <el-table-column prop="listYear" label="年度" width="100" align="center" />
        <el-table-column prop="itemCount" label="条目数" width="100" align="center" />
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="listStatus" label="状态" width="110" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((tag(row.listStatus)) || 'primary')">{{ row.listStatus }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="info" @click="onDetail(row)">明细(表6)</el-button>
            <el-button v-if="isMaintainer" link type="primary" :disabled="row.listStatus !== '草案'" @click="onSubmit(row)">提交申报稿</el-button>
            <el-button v-if="isApprover" link type="success" :disabled="row.listStatus !== '申报稿'" @click="onApprove(row)">领导小组批准</el-button>
            <el-button v-if="isMaintainer" link type="warning" :disabled="row.listStatus !== '批准'" @click="onGoAgreement(row)">去协议双签</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:20px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>
    <el-dialog v-model="dlg" title="新增批量授权清单" width="500px" align-center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="年度"><el-input v-model="form.listYear" placeholder="如 2026" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSave">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailDrawer" :title="`批量授权清单明细(表6) · ${curList.listNo||''}`" size="80%">
      <!-- 流转进度时间轴(单一真相,对齐 35号文 附录C 表1 批量流程)。创建在向导;此处看「办到哪、卡在谁」 -->
      <el-collapse v-model="flowOpen" style="margin-bottom:10px">
        <el-collapse-item name="flow">
          <template #title><b>流转进度</b>　<span style="margin-left:8px" :class="'prm-c-' + ((curList.listStatus==='批准' ? 'success' : 'info') || 'primary')">{{ flowHint }}</span></template>
          <AuthFlowProgress mode="batch" :current="flowCurrent" />
        </el-collapse-item>
      </el-collapse>
      <div class="prm-table-note" style="margin-bottom:8px">表6 明细行:本清单(batchListId)下的所有批量授权项。每项=一个库表的授权,逐项含 系统/模式/权益/场景 + 确权带出的第三方·隐私 + 先确后授生效卡片。</div>
      <!-- 清单级「是否跨系统域」判定(表6 专设;批量可跨多系统聚合),与一站式向导同口径 -->
      <el-alert v-if="detailRows.length" :type="crossSystemInfo.isCross ? 'warning' : 'info'" :closable="false" style="margin-bottom:10px">
        <template #title>
          <span>是否跨系统域:</span>
          <span style="margin:0 6px" :class="'prm-c-' + ((crossSystemInfo.isCross ? 'warning' : 'info') || 'primary')">{{ crossSystemInfo.isCross ? '是(跨系统域)' : '否(单系统)' }}</span>
          本清单覆盖 {{ crossSystemInfo.systems.length }} 个系统{{ crossSystemInfo.systems.length ? '(' + crossSystemInfo.systems.join('、') + ')' : '' }}。
        </template>
      </el-alert>
      <el-table :data="detailRows" v-loading="detailLoading" border stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="padding:6px 18px;line-height:1.9;color:var(--prm-color-text)">
              <div><b>业务域:</b>{{ row.businessDomain || '—' }}　<b>授权范围:</b>{{ row.scope || '—' }}　<b>授权时效:</b>{{ (row.validDate||'').slice(0,10) || '—' }}</div>
              <div><b>利益分配约定(附录D §3.4.4):</b>{{ row.benefitAllocation || '批量:批准后在《运营授权协议》要素落定环节(清单级)统一约定' }}</div>
              <div><b>安全保障要求(附录D §3.4.4):</b>{{ row.securityReq || '批量:批准后在《运营授权协议》要素落定环节(表2 三行)统一约定' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column type="index" label="序号" width="52" align="center" />
        <el-table-column prop="granteeOrg" label="被授权方" min-width="140" show-overflow-tooltip />
        <el-table-column label="所属系统" min-width="120" show-overflow-tooltip><template #default="{ row }">{{ sysName(row) }}</template></el-table-column>
        <el-table-column prop="schemaName" label="模式名称" width="100" show-overflow-tooltip><template #default="{ row }">{{ row.schemaName || '—' }}</template></el-table-column>
        <el-table-column prop="assetName" label="数据表" min-width="130" show-overflow-tooltip />
        <el-table-column prop="rightType" label="权益类型" width="130" />
        <el-table-column prop="scenario" label="使用场景" min-width="110" show-overflow-tooltip><template #default="{ row }">{{ row.scenario || '—' }}</template></el-table-column>
        <el-table-column label="涉第三方" width="88" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((involvesThird(row) ? 'warning' : 'info') || 'primary')">{{ involvesThird(row) ? '涉' : '否' }}</span></template>
        </el-table-column>
        <el-table-column label="涉隐私/商密" width="104" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((involvesSensitive(row) ? 'danger' : 'info') || 'primary')">{{ involvesSensitive(row) ? row.sensitiveType : '否' }}</span></template>
        </el-table-column>
        <el-table-column label="跨域" width="68" align="center">
          <template #default><span :class="'prm-c-' + ((crossSystemInfo.isCross ? 'warning' : 'info') || 'primary')">{{ crossSystemInfo.isCross ? '是' : '否' }}</span></template>
        </el-table-column>
        <el-table-column prop="equityCardId" label="生效卡片" width="120" show-overflow-tooltip><template #default="{ row }">{{ row.equityCardId || '—' }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((tag(row.status)) || 'primary')">{{ row.status }}</span></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!detailLoading && detailRows.length===0" description="该清单暂无明细项(可在'批量授权一站式向导'中逐条添加)" />
    </el-drawer>
  </div>
</template>
<script setup>
import PageNote from '@/components/PageNote.vue'
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { confirmAsync } from '@/utils/confirmAsync'
import { useRouter } from 'vue-router'
import { pageBatchList, createBatchList, submitBatchList, approveBatchList, listAuthByBatch, generateAgreementForBatch } from '@/api/authorize'
import { currentRole } from '@/lib/roles'
import AuthFlowProgress from '@/components/AuthFlowProgress.vue'
import { useTablePage } from '@/composables/useTablePage'
// 申报人只做 新增/提交申报稿;批准属审批角色(领导小组办公室),与后端 @RequiresRole 一致,隐藏批准按钮避免误点 403
// 清单级终批=领导小组办公室专属(BA-03 node90);数字化部主管/经理/副总的审核在明细链逐项完成,不在清单级
const isApprover = ['leadership', 'admin', 'all'].includes(currentRole())
// 清单维护(新增/提交申报稿/生成协议)是申报人(数字化部)职责;副总/经理/主管/合规/领导小组在此页只查看,
// 其授权审核在「授权审核台」明细链逐项完成。隐藏维护按钮,避免越职误操作(与 isApprover 同口径分层)。
const isMaintainer = ['apply', 'admin', 'all'].includes(currentRole())
const statuses = ['草案', '申报稿', '批准']
const { query: q, rows, total, loading, load } = useTablePage(pageBatchList, { listYear: '', listStatus: '' })
const dlg = ref(false); const form = reactive({ listYear: '', remark: '' })
const detailDrawer = ref(false); const detailRows = ref([]); const detailLoading = ref(false); const curList = ref({})
// 流转进度时间轴(对齐 35号文 表1):清单级状态(草案/申报稿/批准)+ 明细审批链聚合 → 当前节点
const flowOpen = ref(['flow'])
const FLOW_KEY = { 合规审核中: 'compliance', 主管审核中: 'manager', 经理审核中: 'director', 副总审批中: 'gm', 领导小组审批中: 'leadership', 已生效: 'execute' }
const FLOW_RANK = { 合规审核中: 1, 主管审核中: 2, 经理审核中: 3, 副总审批中: 4, 领导小组审批中: 5, 已生效: 6 }
const flowCurrent = computed(() => {
  const ls = curList.value.listStatus
  if (ls === '草案') return 'submit'
  if (ls === '批准') return 'sign' // 领导小组已批准 → 待甲乙双签协议
  // 申报稿:取明细中"最早仍在审"的节点为当前进度
  const inReview = (detailRows.value || []).map((r) => r.status).filter((s) => FLOW_RANK[s])
  if (!inReview.length) return 'compliance'
  const earliest = inReview.reduce((a, b) => (FLOW_RANK[b] < FLOW_RANK[a] ? b : a))
  return FLOW_KEY[earliest] || 'compliance'
})
const flowHint = computed(() => {
  const ls = curList.value.listStatus
  if (ls === '批准') return '领导小组已批准 · 待双签协议(附录D)'
  if (ls === '草案') return '草案 · 未提交'
  return '审批链进行中'
})
function tag(s) { return { 批准: 'success', 申报稿: 'warning', 草案: 'info', 已生效: 'success', 已驳回: 'danger' }[s] || 'warning' }
// 授权域:assetId 是库表/卡片级 ID(不含 SYS: 前缀,不同于确权域),所属系统取向导落库的 systemName;
// 兼容遗留数据(升级前提交、systemName 未落库):回退按 SYS: 前缀解析,仍取不到则显示原始 ID。
function sysName(row) {
  if (row.systemName) return row.systemName
  const a = row.assetId || ''
  return a.startsWith('SYS:') ? a.slice(4) : (a || '—')
}
// 合规判定(与向导逐项表一致):第三方/隐私商密 由确权事实带出,空/「无」视为不涉。
function involvesThird(row) { return !!(row.thirdPartySource && String(row.thirdPartySource).trim()) }
function involvesSensitive(row) { return !!(row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无') }
// 表6「是否跨系统域」是清单级属性(全清单系统并集>1),与向导 crossSystemInfo 同源,避免逐行 crossRegion 陈旧不一致。
const crossSystemInfo = computed(() => {
  const systems = [...new Set(detailRows.value.map(sysName).filter(s => s && s !== '—'))]
  const domains = [...new Set(detailRows.value.map(r => r.businessDomain).filter(Boolean))]
  return { systems, domains, isCross: systems.length > 1 || domains.length > 1 }
})
async function onDetail(row) {
  curList.value = row; detailDrawer.value = true; detailLoading.value = true
  try { detailRows.value = await listAuthByBatch(row.batchListId) || [] } finally { detailLoading.value = false }
}
function onAdd() { Object.assign(form, { listYear: '', remark: '' }); dlg.value = true }
async function onSave() { await createBatchList({ ...form }); ElMessage.success('已新增清单(草案)'); dlg.value = false; load() }
const router = useRouter()
async function onSubmit(row) { await submitBatchList(row.batchListId); ElMessage.success('已提交为申报稿'); load() }
// #2 领导小组终批=重决策,加确认弹窗防误点;批准后后端自动「形成」《运营授权协议》草案(35号文 step100)
function onApprove(row) {
  confirmAsync(
    `领导小组办公室终批清单「${row.listNo}」?批准后将自动生成《运营授权协议》草案,进入甲乙双签。`,
    '领导小组决策批准',
    async () => {
      try {
        await approveBatchList(row.batchListId)
        ElMessage.success('已批准,《运营授权协议》草案已自动生成,可去协议工作台双签')
        load()
      } catch { /* 拦截器已弹出后端原因(链未走完/清单为空),此处不重复提示 */ }
    },
    { confirmButtonText: '批准并生成协议', cancelButtonText: '取消' }
  ).catch(() => {})
}
// #1 协议系统自动生成 → 此处不再手动「生成」,改为「去协议双签」;幂等 generate 兜底确保协议存在再跳转
async function onGoAgreement(row) {
  try { await generateAgreementForBatch(row.batchListId) } catch { /* 已自动生成则幂等返回;失败也不阻断跳转 */ }
  router.push('/dpr/auth/agreement')
}
onMounted(load)
</script>
