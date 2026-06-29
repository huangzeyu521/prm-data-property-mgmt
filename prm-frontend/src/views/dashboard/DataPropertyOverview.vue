<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <h3 style="margin:0 0 16px 0">数据产权全景概览</h3>
    <!-- 响应式断点(规范 p21 等效):≥md 宽屏多列,sm/xs 窄屏自动重排;Element 固定 24 栏,以断点 span 达成变栏等效 -->
    <el-row :gutter="16">
      <el-col :span="4" :sm="8" :xs="12"><el-card shadow="hover"><div class="st"><b>{{ ov.totalAssets }}</b><span>纳管资产数(覆盖率分母)</span></div></el-card></el-col>
      <el-col :span="4" :sm="8" :xs="12"><el-card shadow="hover"><div class="st"><b class="blue">{{ ov.confirmRate }}%</b><span>确权覆盖率</span></div></el-card></el-col>
      <el-col :span="4" :sm="8" :xs="12"><el-card shadow="hover"><div class="st"><b class="green">{{ cf.cardCount }}</b><span>权益卡片数</span></div></el-card></el-col>
      <el-col :span="4" :sm="8" :xs="12"><el-card shadow="hover"><div class="st"><b class="blue">{{ cf.passRate }}%</b><span>确权通过率</span></div></el-card></el-col>
      <el-col :span="4" :sm="8" :xs="12"><el-card shadow="hover"><div class="st"><b class="green">{{ au.certCount }}</b><span>授权证书数</span></div></el-card></el-col>
      <el-col :span="4" :sm="8" :xs="12"><el-card shadow="hover"><div class="st"><b class="blue">{{ au.effectiveRate }}%</b><span>授权生效率</span></div></el-card></el-col>
    </el-row>

    <div class="lk-title">权益闭环联动治理(确权↔授权↔监测)</div>
    <el-row :gutter="16">
      <el-col :span="6" :sm="12" :xs="24"><el-card shadow="hover" class="lk"><div class="st"><b class="orange">{{ lk.reConfirmCount }}</b><span>重确权工单(联动派生)</span></div></el-card></el-col>
      <el-col :span="6" :sm="12" :xs="24"><el-card shadow="hover" class="lk"><div class="st"><b class="red">{{ lk.suspendedCount }}</b><span>熔断暂停证书</span></div></el-card></el-col>
      <el-col :span="6" :sm="12" :xs="24"><el-card shadow="hover" class="lk"><div class="st"><b class="orange">{{ lk.expiring }}</b><span>授权到期预警(30天)</span></div></el-card></el-col>
      <el-col :span="6" :sm="12" :xs="24"><el-card shadow="hover" class="lk"><div class="st"><b class="green">{{ lk.closureRate }}%</b><span>风险整改闭环率</span></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="8" :sm="12" :xs="24"><el-card header="已确权 · 三权分置结构"><div ref="rtRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="8" :sm="12" :xs="24"><el-card header="确权状态分布"><div ref="csRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="8" :sm="12" :xs="24"><el-card header="授权模式分布"><div ref="amRef" style="height:300px"></div></el-card></el-col>
    </el-row>

    <div class="lk-title">趋势与多维统计(系统部署单位 · 时间 · 同比环比)</div>
    <el-row :gutter="16">
      <el-col :span="16" :sm="24" :xs="24"><el-card header="确权登记趋势 + 同比(YoY)/环比(MoM)（月度）"><div ref="trendRef" style="height:320px"></div></el-card></el-col>
      <el-col :span="8" :sm="24" :xs="24"><el-card header="授权状态分布"><div ref="authStatusRef" style="height:320px"></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24"><el-card header="各系统部署单位 确权覆盖率 / 授权率(总部 · 超高压 · 双调 · 五省网 · 广州 · 深圳)"><div ref="deployRef" style="height:300px"></div></el-card></el-col>
    </el-row>
    <div class="prm-table-note" style="margin-top:12px">注:数据产权概览跨域聚合确权、授权、台账核心指标。资产卡片由数据资产管理平台维护,本模块不增删资产;故部门维度以「确权覆盖率/授权率」(产权进度口径,分母=纳管资产数)呈现,而非资产库存数。含趋势/同比环比/系统部署单位多维统计(南网打√口径,不按地理省域统计),支撑公司数据产权全貌战略决策。</div>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import { initChart } from '@/lib/chartBase'
import { CHART_COLORS, C } from '@/lib/chartPalette'
import { getOverview, getLedgerStatistics } from '@/api/ledger'
import { getConfirmDashboard } from '@/api/confirm'
import { getAuthDashboard, expiringAuthCerts } from '@/api/authorize'
import { getAlertStats } from '@/api/monitor'
const ov = reactive({ totalAssets: 0, confirmRate: 0 })
const cf = reactive({ cardCount: 0, passRate: 0 })
const au = reactive({ certCount: 0, effectiveRate: 0 })
const lk = reactive({ reConfirmCount: 0, suspendedCount: 0, expiring: 0, closureRate: 0 })
const rtRef = ref(); const csRef = ref(); const amRef = ref()
const trendRef = ref(); const authStatusRef = ref(); const deployRef = ref()
const pairs = (m) => Object.entries(m || {}).map(([name, value]) => ({ name, value }))
async function loadLinkage(c, a) {
  const [exp, alert] = await Promise.all([
    expiringAuthCerts(30),
    getAlertStats()
  ])
  lk.reConfirmCount = c.reConfirmCount || 0
  lk.suspendedCount = a.suspendedCount || 0
  lk.expiring = (exp || []).length
  lk.closureRate = alert.closureRate || 0
}
async function load() {
  const [o, c, a] = await Promise.all([getOverview(), getConfirmDashboard(), getAuthDashboard()])
  Object.assign(ov, o); Object.assign(cf, c); Object.assign(au, a)
  loadLinkage(c, a)
  await nextTick()
  initChart(rtRef.value,{ color: CHART_COLORS, tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ name: '产权类型', type: 'pie', radius: ['40%', '70%'], data: pairs(o.rightTypeDistribution) }] })
  initChart(csRef.value,{ color: CHART_COLORS, tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ name: '确权状态', type: 'pie', radius: '65%', data: pairs(c.statusDistribution) }] })
  initChart(amRef.value,{ color: CHART_COLORS, tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ name: '授权模式', type: 'pie', radius: ['40%', '70%'], data: pairs(a.modeDistribution) }] })

  // 趋势 + 同比环比 + 系统部署单位 维度(接入台账统计;不按地理省域)
  const st = await getLedgerStatistics()
  const tr = st.trend || []
  initChart(trendRef.value,{
    color: CHART_COLORS, tooltip: { trigger: 'axis' },
    legend: { bottom: 0, data: ['确权登记数', '环比MoM%', '同比YoY%'] },
    grid: { left: 48, right: 48, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: tr.map(p => p.month) },
    yAxis: [{ type: 'value', name: '登记数' }, { type: 'value', name: '%', axisLabel: { formatter: '{value}%' } }],
    series: [
      { name: '确权登记数', type: 'bar', data: tr.map(p => p.count), itemStyle: { color: C.blue } },
      { name: '环比MoM%', type: 'line', yAxisIndex: 1, smooth: true, data: tr.map(p => p.momRate), itemStyle: { color: C.orange } },
      { name: '同比YoY%', type: 'line', yAxisIndex: 1, smooth: true, data: tr.map(p => p.yoyRate), itemStyle: { color: C.green } }
    ]
  })
  initChart(authStatusRef.value,{ color: CHART_COLORS, tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ name: '授权状态', type: 'pie', radius: '65%', data: pairs(st.byAuthStatus) }] })

  // 系统部署单位(打√口径固定10桶,服务端零填充恒显;保持服务端顺序)——不按地理省域统计
  // 口径:资产卡片由资产平台维护,本模块只做确权/授权,故按「确权覆盖率/授权率」(率)而非资产库存数呈现
  const du = st.coverageByDeploymentUnit || []
  initChart(deployRef.value,{
    color: CHART_COLORS, tooltip: { trigger: 'axis', valueFormatter: (v) => (v == null ? '—' : v + '%') },
    legend: { bottom: 0, data: ['确权覆盖率', '授权率'] }, grid: { left: 48, right: 24, top: 20, bottom: 60 },
    xAxis: { type: 'category', data: du.map(d => d.name), axisLabel: { interval: 0, rotate: 20 } },
    yAxis: { type: 'value', name: '%', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      { name: '确权覆盖率', type: 'bar', data: du.map(d => d.confirmRate), barMaxWidth: 28, itemStyle: { color: C.blue } },
      { name: '授权率', type: 'bar', data: du.map(d => d.authRate), barMaxWidth: 28, itemStyle: { color: C.green } }
    ]
  })
}
onMounted(load)
</script>
<style scoped>
.st { text-align: center; } .st b { display:block; font-size:24px; font-weight:700; } .st span { color: var(--prm-color-text-secondary); font-size:12px; }
.blue { color:#1e87f0; } .green { color:#36b21d; } .orange { color:#ffc417; } .red { color:#e21f0c; }
.lk-title { margin:18px 0 10px; font-weight:700; color: var(--prm-color-text); border-left:4px solid #1e87f0; padding-left:10px; }
.lk { background: linear-gradient(180deg, rgba(47,107,255,0.04), transparent); }
</style>
