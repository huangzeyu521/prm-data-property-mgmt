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
        <el-table-column prop="sizeKb" label="大小" width="100" align="right"><template #default="{ row }">{{ fmtSize(row.sizeKb) }}</template></el-table-column>
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

    <el-dialog v-model="dlg" title="上传确权证明材料(真实文件 · 可批量)" width="600px" align-center @closed="resetUpload">
      <el-form label-width="100px">
        <el-form-item label="关联确权申请">
          <el-input v-model="uploadApplyId" placeholder="确权申请ID(用于解析后自动比对,可空)" clearable />
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload ref="uploadRef" drag multiple :auto-upload="false" :limit="MAX_BATCH"
            accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" :on-exceed="onExceed" :on-change="onFileChange" :on-remove="onFileChange">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此,或<em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 PDF / Word(.doc/.docx) / JPG / PNG;单文件 100KB–500MB;单次最多 {{ MAX_BATCH }} 个。</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="uploading" :disabled="!fileList.length" @click="onUpload">
          上传{{ fileList.length ? `(${fileList.length})` : '' }}
        </el-button>
        <el-button @click="dlg=false">取消</el-button>
      </template>
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
import { UploadFilled } from '@element-plus/icons-vue'
import { pageAitMaterial, uploadAitMaterialFile, uploadAitMaterialBatch, parseAitMaterial, getAitParse, aitTermCheck, aitCompares, aitProgress, aitParseExportUrl } from '@/api/aitool'

const MAX_BATCH = 50
const MIN_BYTES = 100 * 1024
const MAX_BYTES = 500 * 1024 * 1024
const ALLOWED_EXT = ['pdf', 'doc', 'docx', 'jpg', 'jpeg', 'png']

const q = reactive({ current: 1, size: 10 })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const dlg = ref(false)
const uploadRef = ref(); const fileList = ref([]); const uploadApplyId = ref(''); const uploading = ref(false)
const viewDlg = ref(false); const parse = ref(null); const terms = ref([]); const compares = ref([])

function stTag(s) { return { 成功: 'success', 失败: 'danger', 解析中: 'warning', 待解析: 'info' }[s] || 'info' }
function diffTag(d) { return { 一致: 'success', 不一致: 'danger', 缺失: 'warning' }[d] || 'info' }
function fmtSize(kb) { if (!kb) return '-'; return kb >= 1024 ? (kb / 1024).toFixed(1) + ' MB' : kb + ' KB' }

async function load() {
  loading.value = true
  try { const r = await pageAitMaterial({ ...q }); rows.value = r.records || []; total.value = r.total || 0 }
  finally { loading.value = false }
}

function onFileChange(_file, files) { fileList.value = files }
function onExceed() { ElMessage.warning(`单次最多上传 ${MAX_BATCH} 个文件`) }
function resetUpload() { fileList.value = []; uploadApplyId.value = ''; uploadRef.value?.clearFiles?.() }

// 客户端前置校验:格式 + 大小(后端仍强校验,双保险)
function precheck(raw) {
  const ext = (raw.name.split('.').pop() || '').toLowerCase()
  if (!ALLOWED_EXT.includes(ext)) return `${raw.name}:不支持的格式 .${ext}(仅 PDF/Word/JPG/PNG)`
  if (raw.size < MIN_BYTES) return `${raw.name}:文件过小(${(raw.size / 1024).toFixed(0)}KB),不低于 100KB`
  if (raw.size > MAX_BYTES) return `${raw.name}:文件过大(${(raw.size / 1024 / 1024).toFixed(0)}MB),不超过 500MB`
  return null
}

async function onUpload() {
  const raws = fileList.value.map(f => f.raw).filter(Boolean)
  if (!raws.length) { ElMessage.warning('请先选择文件'); return }
  if (raws.length > MAX_BATCH) { ElMessage.warning(`单次最多 ${MAX_BATCH} 个`); return }
  for (const r of raws) { const err = precheck(r); if (err) { ElMessage.error(err); return } }
  uploading.value = true
  try {
    const fd = new FormData()
    if (uploadApplyId.value) fd.append('applyId', uploadApplyId.value)
    if (raws.length === 1) {
      fd.append('file', raws[0])
      await uploadAitMaterialFile(fd)
    } else {
      raws.forEach(r => fd.append('files', r))
      await uploadAitMaterialBatch(fd)
    }
    ElMessage.success(`已上传 ${raws.length} 个文件(待解析)`)
    dlg.value = false
    load()
  } catch (e) {
    ElMessage.error('上传失败:' + (e?.response?.data?.message || e?.message || '请检查格式与大小'))
  } finally { uploading.value = false }
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
