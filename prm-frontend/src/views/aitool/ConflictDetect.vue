<template>
  <div class="prm-page">
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card header="登记权属主张(构建知识图谱)" shadow="hover">
          <el-form :model="claim" label-width="90px">
            <el-form-item label="资产ID"><el-input v-model="claim.assetId" /></el-form-item>
            <el-form-item label="权利主体"><el-input v-model="claim.subject" /></el-form-item>
            <el-form-item label="权利类型">
              <el-select v-model="claim.rightType" style="width:100%">
                <el-option v-for="t in rts" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
            <el-form-item label="授权范围"><el-input v-model="claim.authScope" placeholder="全字段/约定字段" /></el-form-item>
            <el-form-item label="排他主张"><el-switch v-model="claim.exclusive" /></el-form-item>
            <el-form-item label="来源">
              <el-radio-group v-model="claim.sourceType"><el-radio value="历史确权">历史确权</el-radio><el-radio value="当前申请">当前申请</el-radio></el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button @click="onAdd">仅登记主张</el-button>
              <el-button type="primary" @click="onDetect">登记并冲突检测</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>权属冲突 · {{ assetId || '请检测' }}</span>
              <el-input v-model="qAsset" placeholder="资产ID查询" style="width:200px" @keyup.enter="loadByAsset">
                <template #append><el-button @click="loadByAsset">查询</el-button></template>
              </el-input>
            </div>
          </template>
          <!-- #17 多维筛选 + 报告导出 -->
          <div class="conflict-filter">
            <el-select v-model="filters.conflictType" placeholder="冲突类型" clearable size="small" style="width:130px">
              <el-option v-for="t in conflictTypes" :key="t" :label="t" :value="t" />
            </el-select>
            <el-select v-model="filters.riskLevel" placeholder="风险" clearable size="small" style="width:90px">
              <el-option v-for="l in riskLevels" :key="l" :label="l" :value="l" />
            </el-select>
            <el-date-picker v-model="filters.startTime" type="date" placeholder="开始" value-format="YYYY-MM-DD" size="small" style="width:130px" />
            <el-date-picker v-model="filters.endTime" type="date" placeholder="结束" value-format="YYYY-MM-DD" size="small" style="width:130px" />
            <el-button size="small" type="primary" :disabled="!assetId" @click="refresh(assetId)">筛选</el-button>
            <el-button size="small" type="warning" :disabled="!assetId" @click="onExportWord">导出Word</el-button>
            <el-button size="small" :disabled="!assetId" @click="onPrintPdf">打印/PDF</el-button>
          </div>
          <el-alert v-if="report" :title="report.conclusion" :type="report.total>0?'warning':'success'" :closable="false" show-icon style="margin-bottom:10px" />
          <el-table :data="conflicts" border stripe size="small">
            <el-table-column prop="conflictType" label="冲突类型" width="110">
              <template #default="{ row }"><el-tag type="danger" effect="plain">{{ row.conflictType }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="conflictDesc" label="冲突描述" min-width="220" show-overflow-tooltip />
            <el-table-column prop="riskLevel" label="风险" width="70" align="center">
              <template #default="{ row }"><el-tag :type="row.riskLevel==='高'?'danger':'warning'">{{ row.riskLevel }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="suggestion" label="处置建议" min-width="200" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="84" align="center">
              <template #default="{ row }"><el-tag :type="row.status==='已处置'?'success':'info'">{{ row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }"><el-button link type="success" :disabled="row.status==='已处置'" @click="onResolve(row)">处置</el-button></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aitAddClaim, aitDetectConflict, aitConflicts, aitResolveConflict, aitConflictReport, aitConflictReportExportUrl } from '@/api/aitool'

const rts = ['数据持有权', '数据加工使用权', '数据产品经营权', '所有权', '使用权']
const claim = reactive({ assetId: 'DA-DEMO-1', subject: '', rightType: '数据持有权', authScope: '全字段', exclusive: false, sourceType: '当前申请' })
const assetId = ref(''); const qAsset = ref(''); const conflicts = ref([]); const report = ref(null)
// #17 多维筛选
const filters = reactive({ conflictType: '', riskLevel: '', startTime: '', endTime: '' })
const conflictTypes = ['主体冲突', '范围冲突', '时效冲突', '历史记录冲突']
const riskLevels = ['高', '中', '低']

async function onAdd() {
  if (!claim.assetId || !claim.subject) { ElMessage.warning('资产ID与权利主体必填'); return }
  await aitAddClaim({ ...claim }); ElMessage.success('已登记权属主张')
}
async function onDetect() {
  if (!claim.assetId || !claim.subject) { ElMessage.warning('资产ID与权利主体必填'); return }
  const found = await aitDetectConflict({ ...claim })
  assetId.value = claim.assetId
  ElMessage[found.length ? 'warning' : 'success'](found.length ? `检出 ${found.length} 项冲突` : '未检出冲突')
  await refresh(claim.assetId)
}
async function loadByAsset() { if (qAsset.value) { assetId.value = qAsset.value; await refresh(qAsset.value) } }
function params(a) { return { assetId: a, conflictType: filters.conflictType, riskLevel: filters.riskLevel, startTime: filters.startTime, endTime: filters.endTime } }
async function refresh(a) { conflicts.value = await aitConflicts(params(a)); report.value = await aitConflictReport(params(a)) }
function onExportWord() {
  if (!assetId.value) { ElMessage.warning('请先检测或查询某资产的冲突'); return }
  window.open(aitConflictReportExportUrl(params(assetId.value)), '_blank')
}
function onPrintPdf() {
  if (!assetId.value) { ElMessage.warning('请先检测或查询某资产的冲突'); return }
  window.print()   // 浏览器打印 -> 另存为 PDF
}
function onResolve(row) {
  ElMessageBox.prompt('请输入处置说明', '处置冲突', { inputType: 'textarea' })
    .then(async ({ value }) => { await aitResolveConflict(row.conflictId, value); ElMessage.success('已处置'); refresh(assetId.value) }).catch(() => {})
}
</script>

<style scoped>
.conflict-filter { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 10px; }
</style>
