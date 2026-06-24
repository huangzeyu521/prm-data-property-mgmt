<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <el-tabs v-model="tab">
      <el-tab-pane label="权益证书" name="cert">
        <div class="prm-table-card">
          <div style="margin-bottom:12px">
            <el-input v-model="cardId" placeholder="权益卡片ID" style="width:240px" />
            <el-button type="primary" :disabled="!cardId" @click="onIssue" style="margin-left:8px">签发证书</el-button>
          </div>
          <el-table :data="certs" v-loading="loading" border stripe>
            <el-table-column type="index" label="序号" width="64" align="center" />
            <el-table-column prop="certNo" label="证书编号" width="210" show-overflow-tooltip />
            <el-table-column prop="issueUnit" label="签发单位" min-width="180" show-overflow-tooltip />
            <el-table-column prop="issueTime" label="签发时间" width="170" />
            <el-table-column prop="certStatus" label="状态" width="100" align="center">
              <template #default="{ row }"><el-tag :type="row.certStatus==='生效'?'success':'danger'">{{ row.certStatus }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="160"><template #default="{ row }">
              <el-button link type="primary" @click="onPreviewCert(row)">预览</el-button>
              <el-button link type="danger" :disabled="row.certStatus!=='生效'" @click="onRevoke(row)">注销</el-button>
            </template></el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="证书模板" name="tpl">
        <div class="prm-table-card">
          <div style="margin-bottom:12px"><el-button type="primary" @click="onAddTpl">新增模板</el-button></div>
          <el-table :data="tpls" v-loading="loading2" border stripe>
            <el-table-column type="index" label="序号" width="64" align="center" />
            <el-table-column prop="templateName" label="模板名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="rightType" label="适用权益" width="150" />
            <el-table-column prop="templateVersion" label="版本" width="80" align="center" />
            <el-table-column prop="templateStatus" label="状态" width="90" align="center">
              <template #default="{ row }"><el-tag :type="row.templateStatus==='生效中'?'success':'warning'">{{ row.templateStatus }}</el-tag></template>
            </el-table-column>
            <el-table-column label="套版文件" min-width="150">
              <template #default="{ row }">
                <el-link v-if="row.fileName" type="primary" @click="onDownloadTpl(row)">{{ row.fileName }}</el-link>
                <span v-else style="color:#bbb">未上传</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="290" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="onEditTpl(row)">修改</el-button>
                <el-upload :show-file-list="false" :http-request="(o)=>doUploadTpl(row, o.file)" accept=".pdf,.doc,.docx,.png,.jpg,.jpeg" style="display:inline-block;margin:0 8px">
                  <el-button link type="primary">上传</el-button>
                </el-upload>
                <el-button link type="success" :disabled="row.templateStatus==='生效中'" @click="onEnable(row)">启用</el-button>
                <el-button link type="warning" :disabled="row.templateStatus==='停用'" @click="onDisable(row)">停用</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="prm-table-note">注:已用于出证的模板禁物理删除,仅可停用并生成新版本。</div>
        </div>
      </el-tab-pane>
    </el-tabs>
    <el-dialog v-model="dlg" :title="form.templateId ? '修改证书模板（保存自增版本）' : '新增证书模板'" width="560px" align-center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板名称"><el-input v-model="form.templateName" /></el-form-item>
        <el-form-item label="适用权益">
          <el-select v-model="form.rightType" style="width:100%">
            <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板内容">
          <el-input v-model="form.templateContent" type="textarea" :rows="5" placeholder="证书正文/格式说明（可含占位：数据资产、权益类型、权益所有者、有效期、签发单位等）" />
        </el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSaveTpl">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>

    <!-- 在线预览证书内容 -->
    <el-dialog v-model="certDlg" title="确权权益证书 · 在线预览" width="620px" align-center>
      <div class="cert" id="cert-print">
        <div class="cert-title">数据资产确权权益证书</div>
        <div class="cert-sub">{{ certVO.issueUnit || '中国南方电网有限责任公司' }}</div>
        <div class="cert-no">证书编号（唯一）：{{ certVO.certNo }}</div>
        <table class="cert-tbl">
          <tr><td class="k">数据资产</td><td>{{ certVO.assetName }}（{{ certVO.assetId }}）</td></tr>
          <tr><td class="k">权益类型</td><td>{{ certVO.rightType }}</td></tr>
          <tr><td class="k">权益所有者</td><td>{{ certVO.rightOwner }}</td></tr>
          <tr><td class="k">对应权益卡片</td><td>{{ certVO.cardNo }}</td></tr>
          <tr><td class="k">有效期至</td><td>{{ fmtDate(certVO.validDate) }}</td></tr>
          <tr><td class="k">适用模板</td><td>{{ certVO.templateName || '标准权益证书' }}</td></tr>
          <tr><td class="k">签发时间</td><td>{{ fmtTime(certVO.issueTime) }}</td></tr>
        </table>
        <div v-if="certVO.templateContent" class="cert-body">{{ certVO.templateContent }}</div>
        <div class="cert-foot">
          <div class="cert-note">本证书依确权审批结果与配置模板自动生成，与权益信息一一对应，已经区块链 SM3 指纹存证，真伪可溯。</div>
          <div class="cert-seal">确权专用章</div>
        </div>
      </div>
      <template #footer><el-button @click="printCert">打印 / 另存</el-button><el-button @click="certDlg=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageCert, issueCert, revokeCert, getCertRender, pageCertTemplate, createCertTemplate, updateCertTemplate, enableCertTemplate, disableCertTemplate, uploadCertTemplateFile, certTemplateFileUrl } from '@/api/confirm'
import { openFilePreview } from '@/composables/useFilePreview'
const rightTypes = ['数据资源持有权', '数据加工使用权', '数据产品经营权']
const tab = ref('cert')
const cardId = ref('')
const certs = ref([]); const loading = ref(false)
const tpls = ref([]); const loading2 = ref(false)
const dlg = ref(false); const form = reactive({ templateId: '', templateName: '', rightType: '', templateContent: '' })
const certDlg = ref(false); const certVO = ref({})
function fmtDate(t) { return t ? String(t).replace('T', ' ').slice(0, 10) : '长期' }
function fmtTime(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }
async function onPreviewCert(row) { certVO.value = await getCertRender(row.certId) || {}; certDlg.value = true }
function printCert() { window.print() }
async function loadCerts() { loading.value = true; try { const r = await pageCert({ current: 1, size: 50 }); certs.value = r.records || [] } finally { loading.value = false } }
async function loadTpls() { loading2.value = true; try { const r = await pageCertTemplate({ current: 1, size: 50 }); tpls.value = r.records || [] } finally { loading2.value = false } }
async function onIssue() { await issueCert({ cardId: cardId.value }); ElMessage.success('已签发证书'); loadCerts() }
async function onRevoke(row) { await revokeCert(row.certId); ElMessage.success('已注销'); loadCerts() }
function onAddTpl() { Object.assign(form, { templateId: '', templateName: '', rightType: '', templateContent: '' }); dlg.value = true }
function onEditTpl(row) { Object.assign(form, { templateId: row.templateId, templateName: row.templateName, rightType: row.rightType, templateContent: row.templateContent || '' }); dlg.value = true }
async function onSaveTpl() {
  if (form.templateId) { await updateCertTemplate({ ...form }); ElMessage.success('已修改，版本自增') }
  else { await createCertTemplate({ ...form }); ElMessage.success('已新增') }
  dlg.value = false; loadTpls()
}
async function doUploadTpl(row, file) {
  const fd = new FormData(); fd.append('file', file)
  await uploadCertTemplateFile(row.templateId, fd)
  ElMessage.success('套版文件已上传'); loadTpls()
}
function onDownloadTpl(row) { if (row.fileName) openFilePreview(certTemplateFileUrl(row.templateId), row.fileName) }
async function onEnable(row) { await enableCertTemplate(row.templateId); ElMessage.success('已启用'); loadTpls() }
async function onDisable(row) { await disableCertTemplate(row.templateId); ElMessage.success('已停用'); loadTpls() }
onMounted(() => { loadCerts(); loadTpls() })
</script>

<style scoped>
.cert { border: 2px solid #b8893a; border-radius: 8px; padding: 24px 28px; background: linear-gradient(180deg, #fffdf7, #fdf6e8); }
.cert-title { text-align: center; font-size: 22px; font-weight: 800; letter-spacing: 4px; color: #8a5a1a; }
.cert-sub { text-align: center; font-size: 13px; color: #a07b3a; margin: 6px 0 4px; }
.cert-no { text-align: center; font-size: 12px; color: #c0392b; font-weight: 600; margin-bottom: 16px; letter-spacing: 1px; }
.cert-tbl { width: 100%; border-collapse: collapse; }
.cert-tbl td { border: 1px solid #e3cfa0; padding: 9px 12px; font-size: 13px; }
.cert-tbl td.k { width: 120px; background: #faf3e2; color: #6b5320; font-weight: 600; }
.cert-body { margin-top: 12px; font-size: 12px; color: #6b5320; line-height: 1.7; white-space: pre-wrap; }
.cert-foot { display: flex; align-items: flex-end; justify-content: space-between; margin-top: 18px; gap: 16px; }
.cert-note { font-size: 11px; color: #9a8048; max-width: 62%; line-height: 1.5; }
.cert-seal {
  width: 96px; height: 96px; border: 2px solid #c0392b; border-radius: 50%;
  color: #c0392b; display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700; transform: rotate(-12deg); opacity: 0.85; text-align: center; line-height: 1.2;
}
</style>
