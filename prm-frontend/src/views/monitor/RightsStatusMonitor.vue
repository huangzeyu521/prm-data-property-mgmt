<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <!-- 权益生命周期状态(确权·权益卡片 + 授权·授权证书):监控真实权益状态,而非仅告警 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>确权 · 权益卡片状态</span><el-link type="primary" style="float:right;font-size:12px" @click="go('/dpr/confirm/card')">去权益卡片</el-link></template>
          <div class="life">
            <div class="li" @click="go('/dpr/confirm/card')"><b>{{ cardStat.total || 0 }}</b><span>卡片总数</span></div>
            <div class="li"><b class="green">{{ cardStat.normal || 0 }}</b><span>正常</span></div>
            <div class="li"><b class="orange">{{ cardStat.dueSoon || 0 }}</b><span>即将到期</span></div>
            <div class="li"><b class="red">{{ cardStat.frozen || 0 }}</b><span>冻结</span></div>
            <div class="li"><b class="gray">{{ cardStat.expired || 0 }}</b><span>失效</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>授权 · 授权证书状态</span><el-link type="primary" style="float:right;font-size:12px" @click="go('/dpr/auth/cert')">去授权权益</el-link></template>
          <div class="life">
            <div class="li" @click="go('/dpr/auth/cert')"><b>{{ certStat.total || 0 }}</b><span>证书总数</span></div>
            <div class="li"><b class="green">{{ certStat.effective || 0 }}</b><span>生效</span></div>
            <div class="li"><b class="orange">{{ certStat.expiring || 0 }}</b><span>即将到期</span></div>
            <div class="li"><b class="red">{{ certStat.suspended || 0 }}</b><span>已暂停</span></div>
            <div class="li"><b class="gray">{{ certStat.revoked || 0 }}</b><span>已撤销</span></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 风险告警概览 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b>{{ stats.total }}</b><span>权益事件总数</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b class="red">{{ stats.pending }}</b><span>待处理</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b class="orange">{{ stats.processing }}</b><span>处理中</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b class="green">{{ stats.closureRate }}%</b><span>闭环率</span></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16">
      <el-col :span="10"><el-card header="风险等级分布"><div ref="levelRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="14">
        <el-card header="权益状态实时清单(异常告警)">
          <el-table :data="rows" v-loading="loading" border stripe height="300">
            <el-table-column type="index" label="序号" width="56" align="center" />
            <el-table-column prop="alertLevel" label="级别" width="76" align="center">
              <template #default="{ row }"><el-tag :type="levelTag(row.alertLevel)">{{ row.alertLevel }}</el-tag></template>
            </el-table-column>
            <el-table-column label="所属系统" min-width="120" show-overflow-tooltip><template #default="{ row }">{{ sysName(row) }}</template></el-table-column>
            <el-table-column prop="assetName" label="数据表" min-width="120" show-overflow-tooltip><template #default="{ row }">{{ row.assetName || '—' }}</template></el-table-column>
            <el-table-column prop="abnormalDesc" label="异常描述" min-width="160" show-overflow-tooltip />
            <el-table-column prop="disposeStatus" label="状态" width="84" align="center" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { initChart } from '@/lib/chartBase'
import { getAlertStats, pageAlert } from '@/api/monitor'
import { statsEquityCard } from '@/api/confirm'
import { pageAuthCert, expiringAuthCerts } from '@/api/authorize'

const router = useRouter()
const go = (p) => router.push(p)
const stats = reactive({ total: 0, pending: 0, processing: 0, closed: 0, closureRate: 0 })
// 确权·权益卡片生命周期(库表级);授权·授权证书生命周期
const cardStat = reactive({ total: 0, normal: 0, frozen: 0, expired: 0, dueSoon: 0 })
const certStat = reactive({ total: 0, effective: 0, suspended: 0, revoked: 0, expiring: 0 })
const rows = ref([])
const loading = ref(false)
const levelRef = ref()
function levelTag(l) { return { 紧急: 'danger', 重要: 'warning', 普通: 'info' }[l] || 'info' }
// 库表级:assetId=SYS:系统名 → 所属系统;非 SYS: 原样(兼容旧告警 assetId)
function sysName(row) { const a = (row && row.assetId) || ''; return a.startsWith('SYS:') ? a.slice(4) : (a || '—') }
const pairs = (m) => Object.entries(m || {}).map(([name, value]) => ({ name, value }))

// 授权证书生命周期:无专用 stats 接口,分页拉取后按状态聚合 + 到期接口取即将到期数
async function loadCertStat() {
  try {
    const r = await pageAuthCert({ current: 1, size: 100 })
    const recs = r.records || []
    certStat.total = r.total || recs.length
    certStat.effective = recs.filter(c => c.certStatus === '生效').length
    certStat.suspended = recs.filter(c => c.certStatus === '已暂停').length
    certStat.revoked = recs.filter(c => c.certStatus === '已撤销').length
    const exp = await expiringAuthCerts(30)
    certStat.expiring = (exp || []).length
  } catch { /* 监控页容错:某侧不可用不阻断整页 */ }
}

async function load() {
  loading.value = true
  try {
    // 权益生命周期(确权卡片 + 授权证书),best-effort 并行
    statsEquityCard({}).then(s => Object.assign(cardStat, s || {})).catch(() => {})
    loadCertStat()
    const s = await getAlertStats()
    Object.assign(stats, s)
    const res = await pageAlert({ current: 1, size: 50 })
    rows.value = res.records || []
    await nextTick()
    initChart(levelRef.value, {
      tooltip: { trigger: 'item' }, legend: { bottom: 0 },
      series: [{ name: '预警数', type: 'pie', radius: ['40%', '70%'], data: pairs(s.levelDistribution) }]
    })
  } finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.st { text-align: center; } .st b { display:block; font-size:26px; } .st span { color: var(--prm-color-text-secondary); font-size:12px; }
.life { display:flex; justify-content:space-around; text-align:center; }
.life .li { cursor:default; } .life .li b { display:block; font-size:24px; } .life .li span { color: var(--prm-color-text-secondary); font-size:12px; }
.red { color:#e21f0c; } .orange { color:#ffc417; } .green { color:#36b21d; } .gray { color: var(--prm-color-text-weak); }
</style>
