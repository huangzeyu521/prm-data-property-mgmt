<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!--
  授权申请查询(数据授权管理域内)。按 authMode 分类型呈现各自的审批流转(35号文 附录C 表1/表2):
    一事一议(专项):单位初审→合规→业务→主管→经理→副总→批准(待双签)→已生效(双签+承诺函归档后)
    批量:          合规→主管→经理→副总→领导小组→已生效
  二者流程不同,流转进度列按行 authMode 渲染对应链(不混用一套步骤)。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="资产名称"><el-input v-model="q.assetName" clearable style="width:160px" /></el-form-item>
        <el-form-item label="申请人"><el-input v-model="q.applicant" placeholder="申请单位主管" clearable style="width:140px" /></el-form-item>
        <el-form-item label="授权模式">
          <el-select v-model="q.authMode" placeholder="全部" clearable style="width:120px">
            <el-option label="一事一议" value="一事一议" /><el-option label="批量" value="批量" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="q.status" placeholder="全部" clearable style="width:120px">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="onSearch">查询</el-button><el-button @click="onReset">重置</el-button></el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="applyNo" label="申请编号" width="150" show-overflow-tooltip />
        <el-table-column prop="authMode" label="模式" width="90" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((row.authMode === '批量' ? 'warning' : 'primary') || 'primary')">{{ row.authMode || '一事一议' }}</span></template>
        </el-table-column>
        <el-table-column label="所属系统" min-width="120" show-overflow-tooltip><template #default="{ row }">{{ sysName(row) }}</template></el-table-column>
        <el-table-column prop="assetName" label="数据表" min-width="130" show-overflow-tooltip />
        <el-table-column prop="rightType" label="权益类型" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.rightType || '—' }}</template>
        </el-table-column>
        <el-table-column prop="granteeOrg" label="被授权方" min-width="120" show-overflow-tooltip />
        <el-table-column prop="applicantManager" label="申请人" width="110" show-overflow-tooltip />
        <el-table-column label="审核结果" width="92" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((reviewTag(row.status)) || 'primary')">{{ reviewResult(row.status) }}</span></template>
        </el-table-column>
        <el-table-column label="授权状态" width="92" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((authTag(row.status)) || 'primary')">{{ authStatus(row.status) }}</span></template>
        </el-table-column>
        <!-- 分类型流转进度:按行 authMode 渲染对应审批链 -->
        <el-table-column label="流转进度" min-width="330">
          <template #default="{ row }">
            <el-steps :active="stepOf(row)" align-center finish-status="success" simple style="margin:0" class="flow-mini">
              <el-step v-for="s in stepsOf(row)" :key="s" :title="s" />
            </el-steps>
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="处理意见" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.rejectReason || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="155" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onProgress(row)">进度详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:20px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>

    <!-- 进度详情:申请要素(表5/表6 + 附录D §3.4.4)+ 流转进度 两段 -->
    <el-drawer v-model="drawer" :title="`申请要素 + 进度跟踪 — ${curNo}`" size="52%">
      <div class="rv-h">申请要素(表5/表6 + 附录D §3.4.4)</div>
      <el-descriptions :column="2" border size="small" style="margin-bottom:16px">
        <el-descriptions-item label="授权模式">{{ curRow.authMode || '一事一议' }}</el-descriptions-item>
        <el-descriptions-item label="权益类型">{{ curRow.rightType || '—' }}</el-descriptions-item>
        <el-descriptions-item label="所属系统">{{ sysName(curRow) }}</el-descriptions-item>
        <el-descriptions-item label="模式名称">{{ curRow.schemaName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="数据表">{{ curRow.assetName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="业务域">{{ curRow.businessDomain || '—' }}</el-descriptions-item>
        <el-descriptions-item label="被授权方">{{ curRow.granteeOrg || '—' }}</el-descriptions-item>
        <el-descriptions-item label="生效卡片">{{ curRow.equityCardId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="使用场景及目的" :span="2">{{ curRow.scenario || '—' }}</el-descriptions-item>
        <el-descriptions-item label="授权范围">{{ curRow.scope || '—' }}</el-descriptions-item>
        <el-descriptions-item label="授权时效">{{ (curRow.validDate||'').slice(0,10) || '—' }}</el-descriptions-item>
        <el-descriptions-item label="涉第三方来源">{{ involvesThird(curRow) ? curRow.thirdPartySource : '不涉及' }}</el-descriptions-item>
        <el-descriptions-item label="涉隐私/商密">{{ involvesSensitive(curRow) ? curRow.sensitiveType : '不涉及' }}</el-descriptions-item>
        <el-descriptions-item label="是否跨域">{{ curRow.crossRegion ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="—">　</el-descriptions-item>
        <el-descriptions-item label="利益分配约定(§3.4.4)" :span="2">{{ curRow.benefitAllocation || (curRow.authMode === '批量' ? '批量:在《运营授权协议》(清单级)统一约定' : '— 未填(协议签订前须补充)') }}</el-descriptions-item>
        <el-descriptions-item label="安全保障要求(§3.4.4)" :span="2">{{ curRow.securityReq || (curRow.authMode === '批量' ? '批量:在《运营授权协议》(清单级)统一约定' : '— 未填(协议签订前须补充)') }}</el-descriptions-item>
      </el-descriptions>
      <div class="rv-h">流转进度</div>
      <div v-if="!logs.length" style="color:var(--prm-color-text-weak);padding:12px">暂无流转记录(草稿尚未提交)。</div>
      <el-timeline v-else style="padding:8px 6px">
        <el-timeline-item v-for="(l, i) in logs" :key="l.logId || i" :timestamp="fmt(l.createTime)" placement="top"
          :type="l.toStatus === '已驳回' ? 'danger' : (l.toStatus === '已生效' ? 'success' : 'primary')">
          <div style="font-weight:600">{{ l.nodeName || l.node || '流转' }}：{{ l.fromStatus }} → {{ l.toStatus }}</div>
          <div v-if="l.responder" style="font-size:12px;color:var(--prm-color-text-secondary);margin-top:2px">责任人：{{ l.responder }}</div>
          <div v-if="l.opinion" style="font-size:12px;color:var(--prm-color-text-secondary);margin-top:2px">意见：{{ l.opinion }}</div>
          <div v-if="l.notifyContent" class="prm-c-primary" style="font-size:12px;margin-top:4px">{{ l.pushChannel }}：{{ l.notifyContent }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { pageAuthApply, getAuthFlowLog } from '@/api/authorize'
import { useTablePage } from '@/composables/useTablePage'

const statuses = ['草稿', '单位初审中', '合规审核中', '业务审核中', '主管审核中', '经理审核中', '副总审批中', '领导小组审批中', '批准', '已生效', '已驳回', '已撤回']
const { query: q, rows, total, loading, load, search: onSearch, reset: onReset } = useTablePage(pageAuthApply, { assetName: '', applicant: '', authMode: '', status: '' })

// 分类型审批链(与后端 flowKeyOf(authMode) 一致):一事一议有"单位初审/业务审核/批准(待双签)"无"领导小组";批量反之
const STEPS_BY_MODE = {
  一事一议: { labels: ['提交', '单位初审', '合规', '业务', '主管', '经理', '副总', '双签', '生效'], idx: { 草稿: 0, 单位初审中: 1, 合规审核中: 2, 业务审核中: 3, 主管审核中: 4, 经理审核中: 5, 副总审批中: 6, 批准: 7, 已生效: 9, 已驳回: 1, 已撤回: 1 } },
  批量: { labels: ['提交', '合规', '主管', '经理', '副总', '领导小组', '生效'], idx: { 草稿: 0, 合规审核中: 1, 主管审核中: 2, 经理审核中: 3, 副总审批中: 4, 领导小组审批中: 5, 已生效: 7, 已驳回: 1, 已撤回: 1 } }
}
function chainOf(row) { return STEPS_BY_MODE[row.authMode === '批量' ? '批量' : '一事一议'] }
function stepsOf(row) { return chainOf(row).labels }
function stepOf(row) { return chainOf(row).idx[row.status] ?? 0 }

// 审核结果:未提交/审核中/通过(批准=终审已过待双签)/驳回
function reviewResult(s) { return s === '草稿' ? '未提交' : (s === '已生效' || s === '批准') ? '通过' : s === '已驳回' ? '驳回' : s === '已撤回' ? '已撤回' : '审核中' }
function reviewTag(s) { return (s === '已生效' || s === '批准') ? 'success' : s === '已驳回' ? 'danger' : (s === '草稿' || s === '已撤回') ? 'info' : 'warning' }
// 授权状态:已生效/未授权/双签中/待生效/已撤回(生效=协议双签+承诺函归档后,先签约后执行授权;已撤回=申请人撤回,终态)
function authStatus(s) { return s === '已生效' ? '已生效' : s === '批准' ? '双签中' : s === '已驳回' ? '未授权' : s === '已撤回' ? '已撤回' : s === '草稿' ? '—' : '待生效' }
function authTag(s) { return s === '已生效' ? 'success' : s === '批准' ? 'warning' : s === '已驳回' ? 'danger' : 'info' }

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }
// 授权域:assetId 是库表/卡片级 ID(不含 SYS: 前缀,不同于确权域),所属系统取向导落库的 systemName;
// 兼容遗留数据(升级前提交、systemName 未落库):回退按 SYS: 前缀解析,仍取不到则显示原始 ID。
function sysName(row) {
  if (row.systemName) return row.systemName
  const a = row.assetId || ''
  return a.startsWith('SYS:') ? a.slice(4) : (a || '—')
}
// 合规要素判定(与向导/审核台一致):空/「无」视为不涉。
function involvesThird(row) { return !!(row.thirdPartySource && String(row.thirdPartySource).trim()) }
function involvesSensitive(row) { return !!(row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无') }
const drawer = ref(false); const logs = ref([]); const curNo = ref(''); const curRow = ref({})
async function onProgress(row) {
  curNo.value = row.applyNo || row.applyId
  curRow.value = row
  logs.value = await getAuthFlowLog(row.applyId) || []
  drawer.value = true
}

onMounted(load)
</script>

<style scoped>
.flow-mini :deep(.el-step__title) { font-size: 12px; line-height: 1.2; }
.flow-mini :deep(.el-step__arrow) { display: none; }
.rv-h { font-weight: 600; font-size: 13px; color: var(--prm-color-text); margin: 4px 0 8px; padding-left: 8px; border-left: 3px solid var(--prm-color-primary); }
</style>
