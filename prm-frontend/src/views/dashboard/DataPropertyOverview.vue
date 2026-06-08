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
    <div class="prm-table-note" style="margin-top:12px">注:数据产权概览跨域聚合确权、授权、台账核心指标,支撑公司数据产权全貌战略决策。</div>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getOverview } from '@/api/ledger'
import { getConfirmDashboard } from '@/api/confirm'
import { getAuthDashboard, expiringAuthCerts } from '@/api/authorize'
import { getAlertStats } from '@/api/monitor'
const ov = reactive({ totalAssets: 0, confirmRate: 0 })
const cf = reactive({ cardCount: 0, passRate: 0 })
const au = reactive({ certCount: 0, effectiveRate: 0 })
const lk = reactive({ reConfirmCount: 0, suspendedCount: 0, expiring: 0, closureRate: 0 })
const rtRef = ref(); const csRef = ref(); const amRef = ref()
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
}
onMounted(load)
</script>
<style scoped>
.st { text-align: center; } .st b { display:block; font-size:24px; font-weight:700; } .st span { color: var(--prm-color-text-secondary); font-size:12px; }
.blue { color:#2f6bff; } .green { color:#18a058; } .orange { color:#f0a020; } .red { color:#d03050; }
.lk-title { margin:18px 0 10px; font-weight:700; color: var(--prm-color-text); border-left:4px solid #2f6bff; padding-left:10px; }
.lk { background: linear-gradient(180deg, rgba(47,107,255,0.04), transparent); }
</style>
