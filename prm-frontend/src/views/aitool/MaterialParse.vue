<template>
  <div class="prm-page">
    <div class="prm-table-card">
      <div style="margin-bottom:12px">
        <el-button type="primary" @click="dlg = true">上传材料</el-button>
        <span class="prm-table-note" style="margin-left:12px">支持 PDF/Word/图片/扫描件;解析抽取权利主体/客体/类型/期限/范围 + 印章识别 + 与确权表单自动比对。</span>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="fileName" label="文件" min-width="200" show-overflow-tooltip />
        <el-table-column prop="fileType" label="类型" width="80" align="center" />
        <el-table-column prop="batchNo" label="批次" width="130" show-overflow-tooltip />
        <el-table-column prop="fileHash" label="文档哈希(SM3)" width="150">
          <template #default="{ row }"><code class="hash">{{ row.fileHash ? row.fileHash.slice(0,12)+'…' : '-' }}</code></template>
        </el-table-column>
        <el-table-column prop="parseStatus" label="解析状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="stTag(row.parseStatus)">{{ row.parseStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column label="解析进度" width="160" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0"
              :status="row.parseStatus==='成功'?'success':(row.parseStatus==='失败'?'exception':undefined)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.parseStatus==='解析中'" @click="onParse(row)">解析</el-button>
            <el-button link type="success" :disabled="row.parseStatus!=='成功'" @click="onView(row)">查看结果</el-button>
            <el-button link type="warning" :disabled="row.parseStatus!=='成功'" @click="onExport(row)">导出Excel</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, prev, pager, next"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" />
    </div>

    <el-dialog v-model="dlg" title="上传确权证明材料" width="560px" align-center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="文件名" required><el-input v-model="form.fileName" placeholder="如:客户用电信息表-确权证明-盖章.pdf" /></el-form-item>
        <el-form-item label="关联确权申请"><el-input v-model="form.applyId" placeholder="确权申请ID(用于自动比对,可空)" /></el-form-item>
        <el-form-item label="文件大小KB"><el-input-number v-model="form.sizeKb" :min="1" /></el-form-item>
        <el-form-item label="材料正文(模拟)"><el-input v-model="form.content" type="textarea" :rows="3" placeholder="模拟OCR/解析正文,如:权利主体广东电网,数据持有权,有效期3年,自行生产,已盖章" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onUpload">上传</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>

    <el-dialog v-model="viewDlg" title="解析结果 · 要素抽取 / 印章 / 术语 / 表单比对" width="720px" align-center>
      <el-descriptions v-if="parse" title="确权要素(置信度 {{ (parse.confidence*100).toFixed(0) }}%)" :column="2" border size="small">
        <el-descriptions-item label="权利主体">{{ parse.rightSubject }}</el-descriptions-item>
        <el-descriptions-item label="权利客体">{{ parse.rightObject }}</el-descriptions-item>
        <el-descriptions-item label="权利类型">{{ parse.rightType }}</el-descriptions-item>
        <el-descriptions-item label="权利期限">{{ parse.rightTerm }}</el-descriptions-item>
        <el-descriptions-item label="授权范围">{{ parse.authScope }}</el-descriptions-item>
        <el-descriptions-item label="数据来源">{{ parse.dataSource }}</el-descriptions-item>
        <el-descriptions-item label="敏感类型">{{ parse.sensitiveType }}</el-descriptions-item>
        <el-descriptions-item label="印章识别">
          <el-tag :type="parse.sealValid==='有效'?'success':'warning'">{{ parse.sealValid }}</el-tag> {{ parse.sealDesc }}
        </el-descriptions-item>
      </el-descriptions>
      <div style="margin-top:14px;font-weight:600">术语库匹配</div>
      <el-table :data="terms" border size="small">
        <el-table-column prop="field" label="字段" width="120" />
        <el-table-column prop="value" label="抽取值" />
        <el-table-column prop="standardTerm" label="标准术语" />
        <el-table-column label="规范" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.standard?'success':'warning'">{{ row.standard?'标准':'建议修正' }}</el-tag></template>
        </el-table-column>
      </el-table>
      <div style="margin-top:14px;font-weight:600">与确权申请表单比对</div>
      <el-table :data="compares" border size="small">
        <el-table-column prop="field" label="字段" width="120" />
        <el-table-column prop="materialValue" label="材料解析值" />
        <el-table-column prop="formValue" label="表单填写值" />
        <el-table-column label="差异" width="90" align="center">
          <template #default="{ row }"><el-tag :type="diffTag(row.diffType)">{{ row.diffType }}</el-tag></template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageAitMaterial, uploadAitMaterial, parseAitMaterial, getAitParse, aitTermCheck, aitCompares, aitProgress, aitParseExportUrl } from '@/api/aitool'

const q = reactive({ current: 1, size: 10 })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const dlg = ref(false)
const form = reactive({ fileName: '', applyId: '', sizeKb: 1024, content: '' })
const viewDlg = ref(false); const parse = ref(null); const terms = ref([]); const compares = ref([])

function stTag(s) { return { 成功: 'success', 失败: 'danger', 解析中: 'warning', 待解析: 'info' }[s] || 'info' }
function diffTag(d) { return { 一致: 'success', 不一致: 'danger', 缺失: 'warning' }[d] || 'info' }

async function load() {
  loading.value = true
  try { const r = await pageAitMaterial({ ...q }); rows.value = r.records || []; total.value = r.total || 0 }
  finally { loading.value = false }
}
async function onUpload() {
  if (!form.fileName) { ElMessage.warning('请填写文件名'); return }
  await uploadAitMaterial({ ...form }); ElMessage.success('已上传(待解析)'); dlg.value = false; load()
}
async function onParse(row) {
  await parseAitMaterial(row.materialId)   // 异步触发
  ElMessage.info('已开始解析,进度实时更新中…')
  pollProgress(row.materialId)
}
// #2 轮询解析进度,实时刷新对应行的状态与进度条
function pollProgress(materialId) {
  const timer = setInterval(async () => {
    try {
      const m = await aitProgress(materialId)
      const row = rows.value.find(x => x.materialId === materialId)
      if (row) { row.progress = m.progress; row.parseStatus = m.parseStatus }
      if (m.parseStatus === '成功' || m.parseStatus === '失败') {
        clearInterval(timer)
        ElMessage[m.parseStatus === '成功' ? 'success' : 'error'](
          m.parseStatus === '成功' ? '解析完成' : ('解析失败:' + (m.failReason || '')))
      }
    } catch (e) { clearInterval(timer) }
  }, 800)
}
function onExport(row) {
  window.open(aitParseExportUrl(row.materialId), '_blank')
}
async function onView(row) {
  parse.value = await getAitParse(row.materialId)
  terms.value = await aitTermCheck(row.materialId)
  compares.value = await aitCompares(row.materialId)
  viewDlg.value = true
}
onMounted(load)
</script>

<style scoped>
.hash { font-family: ui-monospace, Consolas, monospace; font-size: 12px; color: #2f6bff; }
</style>
