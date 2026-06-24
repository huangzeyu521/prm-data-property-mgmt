<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item v-if="action==='seal'" label="授权申请ID"><el-input v-model="genApplyId" placeholder="基于已生效授权生成协议" clearable style="width:220px" /></el-form-item>
        <template v-if="action==='archive'">
          <el-form-item label="协议类型">
            <el-select v-model="q.agreementType" placeholder="全部" clearable style="width:150px">
              <el-option v-for="t in agreementTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="部门"><el-input v-model="q.deptName" placeholder="部门/业务域" clearable style="width:140px" /></el-form-item>
          <el-form-item label="归档时间">
            <el-date-picker v-model="archRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width:230px" />
          </el-form-item>
        </template>
        <el-form-item>
          <el-button type="primary" @click="onSearch">{{ action==='archive' ? '检索' : '刷新' }}</el-button>
          <el-button v-if="action==='seal'" type="primary" :disabled="!genApplyId" @click="onGenerate">生成协议</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <div class="prm-table-note" style="margin:0 0 12px 0">{{ note }}</div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="agreementNo" label="协议编号" width="180" show-overflow-tooltip />
        <el-table-column prop="granteeOrg" label="被授权方" min-width="140" show-overflow-tooltip />
        <el-table-column v-if="action==='archive'" prop="agreementType" label="协议类型" width="130" align="center" />
        <el-table-column v-if="action==='archive'" prop="deptName" label="部门" width="120" show-overflow-tooltip />
        <el-table-column prop="sealStatus" label="签章" width="110" align="center" />
        <el-table-column prop="reviewStatus" label="审核" width="100" align="center" />
        <el-table-column prop="archiveStatus" label="归档" width="90" align="center" />
        <el-table-column v-if="action==='archive'" prop="archiveTime" label="归档时间" width="160"><template #default="{ row }">{{ (row.archiveTime||'').replace('T',' ').slice(0,19) || '—' }}</template></el-table-column>
        <el-table-column label="操作" :width="action==='seal'?330:(action==='archive'?180:260)" fixed="right">
          <template #default="{ row }">
            <template v-if="action==='seal'">
              <el-upload :show-file-list="false" :http-request="(o)=>doUploadSeal(row,'授权方',o.file)" accept=".pdf,.png,.jpg,.jpeg,.doc,.docx" style="display:inline-block">
                <el-button link type="primary" :disabled="row.grantorSigned">甲方签章上传</el-button>
              </el-upload>
              <el-upload :show-file-list="false" :http-request="(o)=>doUploadSeal(row,'被授权方',o.file)" accept=".pdf,.png,.jpg,.jpeg,.doc,.docx" style="display:inline-block;margin:0 6px">
                <el-button link type="primary" :disabled="row.granteeSigned">乙方签章上传</el-button>
              </el-upload>
              <el-button link type="info" @click="onSealLogs(row)">上传记录</el-button>
            </template>
            <template v-else-if="action==='review'">
              <el-button link type="success" :disabled="row.sealStatus!=='已双签'||row.reviewStatus!=='待审核'" @click="onReview(row,true)">通过</el-button>
              <el-button link type="danger" :disabled="row.reviewStatus!=='待审核'" @click="onReview(row,false)">驳回重签</el-button>
              <el-button link type="info" @click="onReviewLogs(row)">审核记录</el-button>
            </template>
            <template v-else-if="action==='archive'">
              <el-button link type="primary" :disabled="row.reviewStatus!=='审核通过'||row.archiveStatus==='已归档'" @click="onArchive(row)">归档</el-button>
              <el-button link type="info" @click="onArchiveLogs(row)">审计日志</el-button>
            </template>
            <template v-else>
              <el-button link type="primary" @click="onPreview(row)">预览</el-button>
              <el-button link type="success" @click="onDownload(row)">下载</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>

    <el-dialog v-model="sealLogDlg" :title="`签章上传记录 — ${curNo}`" width="720px" align-center>
      <el-table :data="sealLogs" border size="small">
        <el-table-column prop="uploaderRole" label="角色" width="100" align="center" />
        <el-table-column prop="fileName" label="签章文件" min-width="160" show-overflow-tooltip />
        <el-table-column label="格式校验" width="90" align="center"><template #default="{ row }"><el-tag :type="row.formatOk?'success':'danger'" size="small">{{ row.formatOk?'通过':'不通过' }}</el-tag></template></el-table-column>
        <el-table-column label="签章有效性" width="100" align="center"><template #default="{ row }"><el-tag :type="row.sealValid?'success':'danger'" size="small">{{ row.sealValid?'有效':'无效' }}</el-tag></template></el-table-column>
        <el-table-column prop="verifyResult" label="验证结果" min-width="220" show-overflow-tooltip />
        <el-table-column prop="uploadTime" label="上传时间" width="160"><template #default="{ row }">{{ (row.uploadTime||'').replace('T',' ').slice(0,19) }}</template></el-table-column>
        <el-table-column label="下载" width="70" align="center"><template #default="{ row }"><el-link type="primary" @click="dlSeal(row)">下载</el-link></template></el-table-column>
      </el-table>
      <el-empty v-if="!sealLogs.length" :image-size="50" description="暂无上传记录" />
    </el-dialog>

    <el-dialog v-model="reviewLogDlg" :title="`协议审核记录 — ${curNo}`" width="640px" align-center>
      <el-timeline v-if="reviewLogs.length" style="padding:6px">
        <el-timeline-item v-for="l in reviewLogs" :key="l.logId" :timestamp="(l.reviewTime||'').replace('T',' ').slice(0,19)" placement="top"
          :type="l.result==='审核通过' ? 'success' : 'danger'">
          <div style="font-weight:600">{{ l.result }}　<span style="font-weight:400;color:#909399">审核人：{{ l.reviewer }}</span></div>
          <div style="font-size:12px;color:#71717a;margin-top:2px">意见：{{ l.opinion || '—' }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else :image-size="50" description="暂无审核记录" />
    </el-dialog>

    <el-dialog v-model="archiveLogDlg" :title="`存档审计日志 — ${curNo}`" width="620px" align-center>
      <el-table :data="archiveLogs" border size="small">
        <el-table-column prop="action" label="操作" width="100" align="center"><template #default="{ row }"><el-tag :type="row.action==='归档'?'success':'info'" size="small">{{ row.action }}</el-tag></template></el-table-column>
        <el-table-column prop="operator" label="操作人" width="140" />
        <el-table-column prop="operateTime" label="时间"><template #default="{ row }">{{ (row.operateTime||'').replace('T',' ').slice(0,19) }}</template></el-table-column>
      </el-table>
      <el-empty v-if="!archiveLogs.length" :image-size="50" description="暂无审计日志" />
    </el-dialog>
  </div>
</template>
<script setup>
import { openFilePreview } from '@/composables/useFilePreview'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageAgreement, generateAgreement, signAgreementGrantor, signAgreementGrantee, reviewAgreement, archiveAgreement, uploadAgreementSeal, getAgreementSealLogs, agreementSealFileUrl, getAgreementReviewLogs, getAgreementArchiveLogs, recordAgreementAccess } from '@/api/authorize'
const route = useRoute()
// 协议工作台(标签页)以 actionProp 复用本组件;独立路由仍读 route.meta.action
const props = defineProps({ actionProp: { type: String, default: '' } })
const action = computed(() => props.actionProp || route.meta.action || 'download')
const note = computed(() => ({
  seal: '注:在线上传双方盖章协议,系统自动进行签章有效性验证(CV/CA 核验)。',
  review: '注:协议须完成签章后人工审核,核对内容与申请单一致,防篡改防"阴阳合同"。',
  archive: '注:审核通过的协议加密长期存档,访问留痕(等保三级)。',
  download: '注:协议下载/预览,水印防伪,留痕审计。'
}[action.value]))
const agreementTypes = ['数据加工使用权', '数据产品经营权']
const q = reactive({ current: 1, size: 10, agreementType: '', deptName: '' })
const archRange = ref([])
const rows = ref([]); const total = ref(0); const loading = ref(false)
const genApplyId = ref('')
async function load() {
  loading.value = true
  const p = { ...q, archiveStart: archRange.value?.[0] || '', archiveEnd: archRange.value?.[1] ? archRange.value[1] + ' 23:59:59' : '' }
  try { const r = await pageAgreement(p); rows.value = r.records || []; total.value = r.total || 0 } finally { loading.value = false }
}
function onSearch() { q.current = 1; load() }
const archiveLogDlg = ref(false); const archiveLogs = ref([])
async function onArchiveLogs(row) { curNo.value = row.agreementNo; archiveLogs.value = await getAgreementArchiveLogs(row.agreementId) || []; archiveLogDlg.value = true }
async function onGenerate() { await generateAgreement({ applyId: genApplyId.value, granteeOrg: '被授权方' }); ElMessage.success('已生成协议'); genApplyId.value = ''; load() }
// 真实签章文件上传:格式校验 + 签章有效性检查 + 记录
const sealLogDlg = ref(false); const sealLogs = ref([]); const curNo = ref('')
async function doUploadSeal(row, role, file) {
  const fd = new FormData(); fd.append('file', file); fd.append('role', role)
  const log = await uploadAgreementSeal(row.agreementId, fd)
  if (log && log.sealValid) ElMessage.success(`${role}签章上传成功:${log.verifyResult}`)
  else ElMessage.warning(`签章核验未通过:${log ? log.verifyResult : ''}`)
  load()
}
async function onSealLogs(row) {
  curNo.value = row.agreementNo
  sealLogs.value = await getAgreementSealLogs(row.agreementId) || []
  sealLogDlg.value = true
}
function dlSeal(row) { openFilePreview(agreementSealFileUrl(row.logId), row.fileName || ('印章-' + (row.logId||'') + '.png')) }
function onReview(row, pass) {
  ElMessageBox.prompt(pass ? '请输入审核意见(可空)' : '请输入驳回意见', pass ? '审核通过' : '驳回重签', { inputType: 'textarea' })
    .then(async ({ value }) => { await reviewAgreement(row.agreementId, pass, value || ''); ElMessage.success(pass ? '审核通过' : '已驳回重签'); load() }).catch(() => {})
}
const reviewLogDlg = ref(false); const reviewLogs = ref([])
async function onReviewLogs(row) { curNo.value = row.agreementNo; reviewLogs.value = await getAgreementReviewLogs(row.agreementId) || []; reviewLogDlg.value = true }
async function onArchive(row) { await archiveAgreement(row.agreementId); ElMessage.success('已归档'); load() }
function onPreview(row) { ElMessage.info(`预览协议 ${row.agreementNo}`) }
async function onDownload(row) { try { await recordAgreementAccess(row.agreementId, '下载') } catch { /* 审计失败不阻断 */ } ElMessage.success(`下载协议 ${row.agreementNo}.pdf（已留痕）`) }
onMounted(load)
</script>
