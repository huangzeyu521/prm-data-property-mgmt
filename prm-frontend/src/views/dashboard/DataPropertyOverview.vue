<template>
  <div class="prm-page">
    <h3 style="margin:0 0 16px 0">数据产权全景概览</h3>
    <el-row :gutter="16">
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b>{{ ov.totalAssets }}</b><span>数据资产总数</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="blue">{{ ov.confirmRate }}%</b><span>确权覆盖率</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="green">{{ cf.cardCount }}</b><span>权益卡片数</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="blue">{{ cf.passRate }}%</b><span>确权通过率</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="green">{{ au.certCount }}</b><span>授权证书数</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="blue">{{ au.effectiveRate }}%</b><span>授权生效率</span></div></el-card></el-col>
    </el-row>

    <div class="lk-title">权益闭环联动治理(确权↔授权↔监测)</div>
    <el-row :gutter="16">
      <el-col :span="6"><el-card shadow="hover" class="lk"><div class="st"><b class="orange">{{ lk.reConfirmCount }}</b><span>重确权工单(联动派生)</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover" class="lk"><div class="st"><b class="red">{{ lk.suspendedCount }}</b><span>熔断暂停证书</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover" class="lk"><div class="st"><b class="orange">{{ lk.expiring }}</b><span>授权到期预警(30天)</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover" class="lk"><div class="st"><b class="green">{{ lk.closureRate }}%</b><span>风险整改闭环率</span></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="8"><el-card header="产权类型构成"><div ref="rtRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="8"><el-card header="确权状态分布"><div ref="csRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="8"><el-card header="授权模式分布"><div ref="amRef" style="height:300px"></div></el-card></el-col>
    </el-row>

    <div class="lk-title">趋势与多维统计（部门/地域 · 时间 · 同比环比）</div>
    <el-row :gutter="16">
      <el-col :span="16"><el-card header="确权登记趋势 + 同比(YoY)/环比(MoM)（月度）"><div ref="trendRef" style="height:320px"></div></el-card></el-col>
      <el-col :span="8"><el-card header="授权状态分布"><div ref="authStatusRef" style="height:320px"></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24"><el-card header="部门/地域分布（确权台账）"><div ref="regionRef" style="height:300px"></div></el-card></el-col>
    </el-row>
    <div class="prm-table-note" style="margin-top:12px">注:数据产权概览跨域聚合确权、授权、台账核心指标,含趋势/同比环比/部门地域多维统计,支撑公司数据产权全貌战略决策。</div>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getOverview, getLedgerStatistics } from '@/api/ledger'
import { getConfirmDashboard } from '@/api/confirm'
import { getAuthDashboard, expiringAuthCerts } from '@/api/authorize'
import { getAlertStats } from '@/api/monitor'
const ov = reactive({ totalAssets: 0, confirmRate: 0 })
const cf = reactive({ cardCount: 0, passRate: 0 })
const au = reactive({ certCount: 0, effectiveRate: 0 })
const lk = reactive({ reConfirmCount: 0, suspendedCount: 0, expiring: 0, closureRate: 0 })
const rtRef = ref(); const csRef = ref(); const amRef = ref()
const trendRef = ref(); const authStatusRef = ref(); const regionRef = ref()
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
  echarts.init(rtRef.value).setOption({ tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ type: 'pie', radius: ['40%', '70%'], data: pairs(o.rightTypeDistribution) }] })
  echarts.init(csRef.value).setOption({ tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ type: 'pie', radius: '65%', data: pairs(c.statusDistribution) }] })
  echarts.init(amRef.value).setOption({ tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ type: 'pie', radius: ['40%', '70%'], data: pairs(a.modeDistribution) }] })

  // 趋势 + 同比环比 + 部门/地域 维度(接入台账统计)
  const st = await getLedgerStatistics()
  const tr = st.trend || []
  echarts.init(trendRef.value).setOption({
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, data: ['确权登记数', '环比MoM%', '同比YoY%'] },
    grid: { left: 48, right: 48, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: tr.map(p => p.month) },
    yAxis: [{ type: 'value', name: '登记数' }, { type: 'value', name: '%', axisLabel: { formatter: '{value}%' } }],
    series: [
      { name: '确权登记数', type: 'bar', data: tr.map(p => p.count), itemStyle: { color: '#2f6bff' } },
      { name: '环比MoM%', type: 'line', yAxisIndex: 1, smooth: true, data: tr.map(p => p.momRate), itemStyle: { color: '#e6a23c' } },
      { name: '同比YoY%', type: 'line', yAxisIndex: 1, smooth: true, data: tr.map(p => p.yoyRate), itemStyle: { color: '#67c23a' } }
    ]
  })
  echarts.init(authStatusRef.value).setOption({ tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ type: 'pie', radius: '65%', data: pairs(st.byAuthStatus) }] })
  echarts.init(regionRef.value).setOption({
    tooltip: { trigger: 'axis' }, grid: { left: 48, right: 24, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: Object.keys(st.byRegion || {}) },
    yAxis: { type: 'value', name: '台账数' },
    series: [{ type: 'bar', data: Object.values(st.byRegion || {}), barWidth: '40%', itemStyle: { color: '#409eff' } }]
  })
}
onMounted(load)
</script>
<style scoped>
.st { text-align: center; } .st b { display:block; font-size:24px; font-weight:700; } .st span { color: var(--prm-color-text-secondary); font-size:12px; }
.blue { color:#2f6bff; } .green { color:#18a058; } .orange { color:#f0a020; } .red { color:#d03050; }
.lk-title { margin:18px 0 10px; font-weight:700; color: var(--prm-color-text); border-left:4px solid #2f6bff; padding-left:10px; }
.lk { background: linear-gradient(180deg, rgba(47,107,255,0.04), transparent); }
</style>
