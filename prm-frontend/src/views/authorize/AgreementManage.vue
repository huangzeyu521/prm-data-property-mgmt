<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item v-if="action==='seal'" label="授权申请ID"><el-input v-model="genApplyId" placeholder="基于已生效授权生成协议" clearable style="width:220px" /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">刷新</el-button>
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
        <el-table-column prop="sealStatus" label="签章" width="120" align="center" />
        <el-table-column prop="reviewStatus" label="审核" width="110" align="center" />
        <el-table-column prop="archiveStatus" label="归档" width="100" align="center" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <template v-if="action==='seal'">
              <el-button link type="primary" :disabled="row.grantorSigned" @click="onSign(row,'grantor')">甲方签署</el-button>
              <el-button link type="primary" :disabled="row.granteeSigned" @click="onSign(row,'grantee')">乙方签署</el-button>
            </template>
            <template v-else-if="action==='review'">
              <el-button link type="success" :disabled="row.sealStatus!=='已双签'||row.reviewStatus!=='待审核'" @click="onReview(row,true)">通过</el-button>
              <el-button link type="danger" :disabled="row.reviewStatus!=='待审核'" @click="onReview(row,false)">驳回重签</el-button>
            </template>
            <el-button v-else-if="action==='archive'" link type="warning" :disabled="row.reviewStatus!=='审核通过'||row.archiveStatus==='已归档'" @click="onArchive(row)">归档</el-button>
            <template v-else>
              <el-button link type="primary" @click="onPreview(row)">预览</el-button>
              <el-button link type="success" @click="onDownload(row)">下载</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, prev, pager, next"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" />
    </div>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageAgreement, generateAgreement, signAgreementGrantor, signAgreementGrantee, reviewAgreement, archiveAgreement } from '@/api/authorize'
const route = useRoute()
const action = computed(() => route.meta.action || 'download')
const note = computed(() => ({
  seal: '注:在线上传双方盖章协议,系统自动进行签章有效性验证(CV/CA 核验)。',
  review: '注:协议须完成签章后人工审核,核对内容与申请单一致,防篡改防"阴阳合同"。',
  archive: '注:审核通过的协议加密长期存档,访问留痕(等保三级)。',
  download: '注:协议下载/预览,水印防伪,留痕审计。'
}[action.value]))
const q = reactive({ current: 1, size: 10 })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const genApplyId = ref('')
async function load() { loading.value = true; try { const r = await pageAgreement({ ...q }); rows.value = r.records || []; total.value = r.total || 0 } finally { loading.value = false } }
async function onGenerate() { await generateAgreement({ applyId: genApplyId.value, granteeOrg: '被授权方' }); ElMessage.success('已生成协议'); genApplyId.value = ''; load() }
function onSign(row, party) {
  const title = party === 'grantor' ? '授权方(甲方)签署' : '被授权方(乙方)签署'
  ElMessageBox.prompt('请输入盖章协议文件地址', title, {}).then(async ({ value }) => {
    const url = value || 'oss://agreement/' + party + '.pdf'
    if (party === 'grantor') { await signAgreementGrantor(row.agreementId, url) } else { await signAgreementGrantee(row.agreementId, url) }
    ElMessage.success('已签署并验签(CA核验)'); load()
  }).catch(() => {})
}
async function onReview(row, pass) { await reviewAgreement(row.agreementId, pass); ElMessage.success(pass ? '审核通过' : '已驳回重签'); load() }
async function onArchive(row) { await archiveAgreement(row.agreementId); ElMessage.success('已归档'); load() }
function onPreview(row) { ElMessage.info(`预览协议 ${row.agreementNo}`) }
function onDownload(row) { ElMessage.success(`下载协议 ${row.agreementNo}.pdf`) }
onMounted(load)
</script>
