<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
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
        </el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <div class="prm-table-note" style="margin:0 0 10px 0">{{ note }}</div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="agreementNo" label="协议编号" width="180" show-overflow-tooltip />
        <el-table-column prop="granteeOrg" label="被授权方" min-width="140" show-overflow-tooltip />
        <el-table-column v-if="action==='archive'" prop="agreementType" label="协议类型" width="130" align="center" />
        <el-table-column v-if="action==='archive'" prop="deptName" label="部门" width="120" show-overflow-tooltip />
        <el-table-column v-if="action==='seal'" label="文本" width="86" align="center">
          <template #default="{ row }">
            <span v-if="row.terminated" class="prm-c-danger">已终止</span>
            <span v-else :class="'prm-c-' + ((row.docStatus==='正式稿' ? 'success' : 'info') || 'primary')">{{ row.docStatus || '草案' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sealStatus" label="签章" width="110" align="center" />
        <el-table-column prop="reviewStatus" label="核验" width="100" align="center" />
        <el-table-column prop="archiveStatus" label="归档" width="90" align="center" />
        <el-table-column v-if="action==='archive'" label="有效期至" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.terminated" style="color:var(--prm-color-text-weak)">已终止</span>
            <template v-else>{{ row.validUntil ? String(row.validUntil).slice(0,10) : '—' }}</template>
          </template>
        </el-table-column>
        <el-table-column v-if="action==='archive'" prop="archiveTime" label="归档时间" width="160"><template #default="{ row }">{{ (row.archiveTime||'').replace('T',' ').slice(0,19) || '—' }}</template></el-table-column>
        <el-table-column label="操作" :width="action==='seal'?760:(action==='archive'?400:340)" fixed="right">
          <template #default="{ row }">
            <el-button link type="warning" @click="onElements(row)">协议要素核对</el-button>
            <template v-if="action==='seal'">
              <!-- ① 要素落定(附录D 协商项填空→正式稿锁定):正式稿才开放签章 -->
              <el-button link :type="row.docStatus==='正式稿' ? 'info' : 'danger'" @click="onNegotiation(row)">
                {{ row.docStatus==='正式稿' ? '要素(已锁定)' : '① 要素落定' }}
              </el-button>
              <el-tooltip :disabled="row.docStatus==='正式稿'" content="协议要素未落定(草案),请先「① 要素落定」生成正式稿" placement="top">
                <span>
                  <el-upload :show-file-list="false" :http-request="(o)=>doUploadSeal(row,'授权方',o.file)" accept=".pdf,.png,.jpg,.jpeg,.doc,.docx" style="display:inline-block">
                    <el-button link type="primary" :disabled="row.grantorSigned || row.docStatus!=='正式稿'">甲方签章</el-button>
                  </el-upload>
                  <el-upload :show-file-list="false" :http-request="(o)=>doUploadSeal(row,'被授权方',o.file)" accept=".pdf,.png,.jpg,.jpeg,.doc,.docx" style="display:inline-block;margin:0 6px">
                    <el-button link type="primary" :disabled="row.granteeSigned || row.docStatus!=='正式稿'">乙方签章</el-button>
                  </el-upload>
                </span>
              </el-tooltip>
              <!-- 保密承诺函(附录E,乙方必签):双签✚承诺函齐才自动归档开权限 -->
              <span v-if="row.confidentialityFile" style="margin-right:6px" class="prm-c-success">承诺函已收口</span>
              <el-upload v-else :show-file-list="false" :http-request="(o)=>doUploadConfidentiality(row,o.file)" accept=".pdf,.png,.jpg,.jpeg,.doc,.docx" style="display:inline-block;margin-right:6px">
                <el-tooltip :disabled="row.docStatus==='正式稿'" content="先要素落定生成正式稿" placement="top">
                  <span><el-button link type="warning" :disabled="row.docStatus!=='正式稿'">承诺函(附录E)</el-button></span>
                </el-tooltip>
              </el-upload>
              <el-button link type="success" @click="dlAppendixD(row)">{{ row.docStatus==='正式稿' ? '下载协议正式稿(附录D)' : '下载协议草案(附录D)' }}</el-button>
              <el-button link type="info" @click="onSealLogs(row)">上传记录</el-button>
              <el-button link type="info" @click="onReviewLogs(row)">核验记录</el-button>
            </template>
            <template v-else-if="action==='archive'">
              <el-button link type="primary" @click="onPreview(row)">预览</el-button>
              <el-button link type="success" @click="onDownload(row)">下载</el-button>
              <!-- 期限管理(动态跟踪):续期(经甲方书面同意,≤今日+5年)/终止(第七章情形,回收数据权限) -->
              <el-button v-if="!row.terminated" link type="warning" @click="onRenew(row)">续期</el-button>
              <el-button v-if="!row.terminated" link type="danger" @click="onTerminate(row)">终止</el-button>
              <el-button link type="info" @click="onReviewLogs(row)">核验记录</el-button>
              <el-button link type="info" @click="onArchiveLogs(row)">审计日志</el-button>
            </template>
            <template v-else>
              <el-button link type="info" @click="onReviewLogs(row)">核验记录</el-button>
              <el-button link type="primary" @click="onPreview(row)">预览</el-button>
              <el-button link type="success" @click="onDownload(row)">下载</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:20px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>

    <el-dialog v-model="sealLogDlg" :title="`签章上传记录 — ${curNo}`" width="720px" align-center>
      <el-table :data="sealLogs" border size="small">
        <el-table-column prop="uploaderRole" label="角色" width="100" align="center" />
        <el-table-column prop="fileName" label="签章文件" min-width="160" show-overflow-tooltip />
        <el-table-column label="格式校验" width="90" align="center"><template #default="{ row }"><span :class="'prm-c-' + ((row.formatOk?'success':'danger') || 'primary')">{{ row.formatOk?'通过':'不通过' }}</span></template></el-table-column>
        <el-table-column label="签章有效性" width="100" align="center"><template #default="{ row }"><span :class="'prm-c-' + ((row.sealValid?'success':'danger') || 'primary')">{{ row.sealValid?'有效':'无效' }}</span></template></el-table-column>
        <el-table-column prop="verifyResult" label="验证结果" min-width="220" show-overflow-tooltip />
        <el-table-column prop="uploadTime" label="上传时间" width="160"><template #default="{ row }">{{ (row.uploadTime||'').replace('T',' ').slice(0,19) }}</template></el-table-column>
        <el-table-column label="下载" width="70" align="center"><template #default="{ row }"><el-link type="primary" @click="dlSeal(row)">下载</el-link></template></el-table-column>
      </el-table>
      <el-empty v-if="!sealLogs.length" :image-size="50" description="暂无数据" />
    </el-dialog>

    <el-dialog v-model="reviewLogDlg" :title="`协议核验记录 — ${curNo}`" width="640px" align-center>
      <el-timeline v-if="reviewLogs.length" style="padding:6px">
        <el-timeline-item v-for="l in reviewLogs" :key="l.logId" :timestamp="(l.reviewTime||'').replace('T',' ').slice(0,19)" placement="top"
          :type="l.result==='审核通过' ? 'success' : 'danger'">
          <div style="font-weight:600">{{ l.result }}　<span style="font-weight:400;color:var(--prm-color-text-weak)">审核人：{{ l.reviewer }}</span></div>
          <div style="font-size:12px;color:var(--prm-color-text-secondary);margin-top:2px">意见：{{ l.opinion || '—' }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else :image-size="50" description="暂无数据" />
    </el-dialog>

    <el-dialog v-model="archiveLogDlg" :title="`存档审计日志 — ${curNo}`" width="620px" align-center>
      <el-table :data="archiveLogs" border size="small">
        <el-table-column prop="action" label="操作" width="100" align="center"><template #default="{ row }"><span :class="'prm-c-' + ((row.action==='归档'?'success':'info') || 'primary')">{{ row.action }}</span></template></el-table-column>
        <el-table-column prop="operator" label="操作人" width="140" />
        <el-table-column prop="operateTime" label="时间"><template #default="{ row }">{{ (row.operateTime||'').replace('T',' ').slice(0,19) }}</template></el-table-column>
      </el-table>
      <el-empty v-if="!archiveLogs.length" :image-size="50" description="暂无数据" />
    </el-dialog>

    <!-- 协议要素核对(附录D §3.4.4):协议 vs 来源申请单,审核人据此核对内容一致、防阴阳合同 -->
    <el-dialog v-model="elemDlg" :title="`协议要素核对(附录D §3.4.4) — ${curNo}`" width="720px" align-center>
      <el-alert type="info" :closable="false" style="margin-bottom:10px" title="核对要点:上传的协议文件中约定的 数据范围 / 使用场景及目的 / 利益分配 / 安全保障,须与下方来源申请单一致(防篡改、防阴阳合同)。" />
      <!-- 批量协议:一清单一协议,附件=《数据授权清单》,核对覆盖的多张数据表 -->
      <template v-if="elem && elem.batchListId">
        <el-descriptions :column="2" border size="small" style="margin-bottom:10px">
          <el-descriptions-item label="授权方式">批量授权(一清单一协议)</el-descriptions-item>
          <el-descriptions-item label="权益类型">{{ elem.rightType || '—' }}</el-descriptions-item>
          <el-descriptions-item label="被授权方" :span="2">{{ elem.granteeOrg || '—' }}</el-descriptions-item>
          <el-descriptions-item label="利益分配/安全保障(§3.4.4)" :span="2">批量:在本《运营授权协议》(清单级)统一约定</el-descriptions-item>
        </el-descriptions>
        <div class="prm-table-note" style="margin-bottom:6px">协议附件《数据授权清单》— 本协议覆盖 {{ elem.itemCount || (elem.items && elem.items.length) || 0 }} 张数据表</div>
        <el-table :data="elem.items || []" border size="small" max-height="320">
          <el-table-column type="index" label="#" width="46" align="center" />
          <el-table-column prop="sysName" label="所属系统" min-width="110" show-overflow-tooltip />
          <el-table-column prop="schemaName" label="模式" width="90" show-overflow-tooltip><template #default="{ row }">{{ row.schemaName || '—' }}</template></el-table-column>
          <el-table-column prop="dataTable" label="数据表" min-width="120" show-overflow-tooltip />
          <el-table-column prop="rightType" label="权益" width="120" />
          <el-table-column prop="scenario" label="使用场景" min-width="110" show-overflow-tooltip><template #default="{ row }">{{ row.scenario || '—' }}</template></el-table-column>
          <el-table-column prop="validDate" label="授权期限" width="110"><template #default="{ row }">{{ row.validDate || '—' }}</template></el-table-column>
        </el-table>
      </template>
      <el-descriptions v-else-if="elem" :column="2" border size="small">
        <el-descriptions-item label="所属系统">{{ elem.sysName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="模式名称">{{ elem.schemaName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="数据表">{{ elem.dataTable || '—' }}</el-descriptions-item>
        <el-descriptions-item label="业务域">{{ elem.businessDomain || '—' }}</el-descriptions-item>
        <el-descriptions-item label="授权方式">{{ elem.authMode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="权益类型">{{ elem.rightType || '—' }}</el-descriptions-item>
        <el-descriptions-item label="被授权方" :span="2">{{ elem.granteeOrg || '—' }}</el-descriptions-item>
        <el-descriptions-item label="使用场景及目的(§3.4.4)" :span="2">{{ elem.scenario || '—' }}</el-descriptions-item>
        <el-descriptions-item label="数据范围(§3.4.4)" :span="2">{{ elem.scope || '—' }}</el-descriptions-item>
        <el-descriptions-item label="授权时效">{{ elem.validDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="是否跨域">{{ elem.crossRegion ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="涉第三方来源">{{ elem.thirdPartySource && String(elem.thirdPartySource).trim() ? elem.thirdPartySource : '不涉及' }}</el-descriptions-item>
        <el-descriptions-item label="涉隐私/商密">{{ elem.sensitiveType && String(elem.sensitiveType).trim() && elem.sensitiveType !== '无' ? elem.sensitiveType : '不涉及' }}</el-descriptions-item>
        <el-descriptions-item label="利益分配约定(§3.4.4)" :span="2">{{ elem.benefitAllocation || (elem.authMode === '批量' ? '批量:在《运营授权协议》(清单级)统一约定' : '— 未填(协议签订前须补充)') }}</el-descriptions-item>
        <el-descriptions-item label="安全保障要求(§3.4.4)" :span="2">{{ elem.securityReq || (elem.authMode === '批量' ? '批量:在《运营授权协议》(清单级)统一约定' : '— 未填(协议签订前须补充)') }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else :image-size="50" description="未取到来源申请单要素" />
    </el-dialog>

    <!-- 协议要素落定(附录D 协商项):只填空不改条款,全部落定→生成正式稿(正文快照锁定)→才可签章 -->
    <el-dialog v-model="negDlg" :title="`协议要素落定(附录D 协商项) — ${curNo}`" width="680px" align-center>
      <el-alert type="info" :closable="false" style="margin-bottom:10px"
        :title="negLocked ? '正式稿已锁定:要素不可再改。如需修改且双方均未签章,可退回草案。' : '附录D 范本中留白待协商的要素在此落定(只填空,不改条款)。全部落定后生成正式稿,正文快照锁定,才开放双方签章。'" />
      <el-form v-if="neg" :model="neg" label-width="150px" :disabled="negLocked">
        <el-form-item label="授权有效期止日" required>
          <el-date-picker v-model="neg.validUntil" type="date" value-format="YYYY-MM-DD" style="width:200px" placeholder="一般3年,最长5年" />
          <span class="neg-hint">表1:自签订日一般3年、最长不超过5年;须覆盖清单明细最长时效</span>
        </el-form-item>
        <el-form-item label="数据使用地理范围" required>
          <el-select v-model="neg.geoScope" filterable allow-create default-first-option style="width:100%" placeholder="表1 地理范围">
            <el-option v-for="g in geoOptions" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="表2·数据加密" required><el-input v-model="neg.securityEncrypt" maxlength="200" /></el-form-item>
        <el-form-item label="表2·访问控制" required><el-input v-model="neg.securityAccess" maxlength="200" /></el-form-item>
        <el-form-item label="表2·操作审计" required><el-input v-model="neg.securityAudit" maxlength="200" /></el-form-item>
        <el-form-item label="收益分配补充约定">
          <el-input v-model="neg.benefitAllocation" type="textarea" :rows="2" maxlength="500" show-word-limit
            placeholder="第六章(可选):不填则按范本缺省条款(按双方投入友好协商,参照流通细则)" />
        </el-form-item>
        <el-form-item label="违约金(万元)" required>
          <el-input v-model="neg.penaltyAmount" style="width:160px" placeholder="第九章,数字" />
        </el-form-item>
        <el-form-item label="争议解决方式" required>
          <el-select v-model="neg.disputeMethod" filterable allow-create default-first-option style="width:100%" placeholder="第十章(一)">
            <el-option label="向甲方所在地人民法院起诉" value="向甲方所在地人民法院起诉" />
            <el-option label="提交广州仲裁委员会(仲裁地点为广州)仲裁" value="提交广州仲裁委员会(仲裁地点为广州)仲裁" />
          </el-select>
        </el-form-item>
        <el-form-item label="乙方送达信息" required>
          <el-input v-model="neg.serviceDelivery" maxlength="300" placeholder="第十章(二):手机/邮箱/邮寄地址至少其一,如 邮箱:xx@csg.cn;地址:…" />
        </el-form-item>
        <el-form-item label="正本份数">
          <el-select v-model="neg.copiesCount" style="width:160px">
            <el-option v-for="n in [2,4,6]" :key="n" :label="`一式${n}份(双方各${n/2}份)`" :value="n" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <template v-if="!negLocked">
          <el-button :loading="negSaving" @click="saveNeg(false)">保存草稿</el-button>
          <el-button type="primary" :loading="negSaving" @click="saveNeg(true)">生成正式稿(锁定)</el-button>
        </template>
        <el-button v-else-if="neg && !neg.grantorSigned && !neg.granteeSigned" type="warning" @click="revertNeg">退回草案(要素可改)</el-button>
        <el-button @click="negDlg=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { openFilePreview, fetchFileBlob } from '@/composables/useFilePreview'
import { useTablePage } from '@/composables/useTablePage'
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ElMessageBox } from 'element-plus'
import { pageAgreement, uploadAgreementSeal, getAgreementSealLogs, agreementSealFileUrl, agreementAppendixDUrl, getAgreementReviewLogs, getAgreementArchiveLogs, recordAgreementAccess, getAgreementElements, getAgreementNegotiation, saveAgreementNegotiation, finalizeAgreementDoc, revertAgreementDraft, uploadAgreementConfidentiality, renewAgreement, terminateAgreement } from '@/api/authorize'
const route = useRoute()
// 协议工作台(标签页)以 actionProp 复用本组件;独立路由仍读 route.meta.action
const props = defineProps({ actionProp: { type: String, default: '' } })
const action = computed(() => props.actionProp || route.meta.action || 'download')
const note = computed(() => ({
  seal: '注:协议由系统在授权批准/生效时自动形成(草案·要素预填)。动线:① 要素落定(附录D 协商项填空)→ 生成正式稿(正文快照锁定)→ ② 甲乙签章 + 上传《保密承诺函(附录E)》→ 三者齐即系统自动核验、开通数据权限并归档,无需人工审核。',
  archive: '注:协议双签生效后由系统自动归档(加密长期存档,等保三级),本页用于检索/预览/下载、到期续期与终止(动态跟踪)及审计留痕。',
  download: '注:协议下载/预览,水印防伪,留痕审计。'
}[action.value]))
const agreementTypes = ['使用权', '经营权']
const archRange = ref([])
const { query: q, rows, total, loading, load, search: onSearch } = useTablePage(
  (p) => pageAgreement({ ...p, archiveStart: archRange.value?.[0] || '', archiveEnd: archRange.value?.[1] ? archRange.value[1] + ' 23:59:59' : '' }),
  { agreementType: '', deptName: '' }
)
const archiveLogDlg = ref(false); const archiveLogs = ref([])
async function onArchiveLogs(row) { curNo.value = row.agreementNo; archiveLogs.value = await getAgreementArchiveLogs(row.agreementId) || []; archiveLogDlg.value = true }
// 真实签章文件上传:格式校验 + 签章有效性检查 + 记录;双签完成后端自动核验+开权限+归档
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
// 下载附录D协议(系统按《南方电网数据授权运营协议》生成;正式稿=锁定快照,草案=实时渲染)
async function dlAppendixD(row) {
  try {
    const stage = row.docStatus === '正式稿' ? '正式稿' : '草案'
    const blob = await fetchFileBlob(agreementAppendixDUrl(row.agreementId))
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `数据授权运营协议${stage}_${row.agreementNo || row.agreementId}.doc`
    document.body.appendChild(a); a.click(); a.remove()
    URL.revokeObjectURL(url)
    ElMessage.success(`协议${stage}(附录D)已下载${stage === '正式稿' ? '(锁定快照)' : ',要素落定后生成正式稿'}`)
  } catch (e) { ElMessage.error('协议下载失败:' + (e?.message || '')) }
}
const reviewLogDlg = ref(false); const reviewLogs = ref([])
async function onReviewLogs(row) { curNo.value = row.agreementNo; reviewLogs.value = await getAgreementReviewLogs(row.agreementId) || []; reviewLogDlg.value = true }
// 协议要素核对(§3.4.4):取协议来源申请单要素,供审核人对照协议文件
const elemDlg = ref(false); const elem = ref(null)
async function onElements(row) {
  curNo.value = row.agreementNo
  elem.value = null
  elemDlg.value = true
  elem.value = await getAgreementElements(row.agreementId)
}
function onPreview(row) { ElMessage.info(`预览协议 ${row.agreementNo}`) }
async function onDownload(row) { try { await recordAgreementAccess(row.agreementId, '下载') } catch { /* 审计失败不阻断 */ } ElMessage.success(`下载协议 ${row.agreementNo}.pdf（已留痕）`) }

// ===== 协议要素落定(附录D 协商项:草案填空→正式稿锁定→才可签章) =====
const negDlg = ref(false); const neg = ref(null); const negSaving = ref(false); const curId = ref('')
const negLocked = computed(() => neg.value && neg.value.docStatus === '正式稿')
const geoOptions = ['广东省行政区域内', '南方电网经营区域(粤桂滇黔琼)', '中华人民共和国境内']
async function onNegotiation(row) {
  curNo.value = row.agreementNo; curId.value = row.agreementId
  neg.value = null; negDlg.value = true
  neg.value = await getAgreementNegotiation(row.agreementId)
}
async function saveNeg(finalize) {
  if (!neg.value) return
  negSaving.value = true
  try {
    await saveAgreementNegotiation(curId.value, neg.value)
    if (finalize) {
      await finalizeAgreementDoc(curId.value)
      ElMessage.success('正式稿已生成并锁定(正文快照落库),可开始双方签章')
      negDlg.value = false
    } else {
      ElMessage.success('要素草稿已保存')
    }
    load()
  } finally { negSaving.value = false }
}
async function revertNeg() {
  await revertAgreementDraft(curId.value)
  ElMessage.success('已退回草案,要素可修改')
  neg.value = await getAgreementNegotiation(curId.value)
  load()
}
// 保密承诺函(附录E,乙方必签):双签✚承诺函齐才自动归档开权限
async function doUploadConfidentiality(row, file) {
  const fd = new FormData(); fd.append('file', file)
  await uploadAgreementConfidentiality(row.agreementId, fd)
  ElMessage.success('《保密承诺函(附录E)》已收口;若已双签,系统将自动核验归档并开通数据权限')
  load()
}
// ===== 期限管理(动态跟踪):续期/终止 =====
async function onRenew(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      `原有效期至:${row.validUntil ? String(row.validUntil).slice(0, 10) : '—'}。输入新止日(yyyy-MM-dd,须晚于原止日且≤今日+5年,经甲方书面同意):`,
      `协议续期 — ${row.agreementNo}`,
      { confirmButtonText: '续期', cancelButtonText: '取消', inputPattern: /^\d{4}-\d{2}-\d{2}$/, inputErrorMessage: '格式 yyyy-MM-dd' })
    await renewAgreement(row.agreementId, value)
    ElMessage.success(`已续期至 ${value}(留痕上链)`)
    load()
  } catch { /* 用户取消 */ }
}
async function onTerminate(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      '终止将回收被授权方底层数据权限并留痕上链(附录D 第七章)。请输入终止原因:',
      `终止协议 — ${row.agreementNo}`,
      { confirmButtonText: '确认终止', cancelButtonText: '取消', type: 'warning', inputValidator: v => !!(v && v.trim()) || '终止原因不能为空' })
    await terminateAgreement(row.agreementId, value.trim())
    ElMessage.success('协议已终止,数据权限已回收(留痕上链)')
    load()
  } catch { /* 用户取消 */ }
}
onMounted(load)
</script>
