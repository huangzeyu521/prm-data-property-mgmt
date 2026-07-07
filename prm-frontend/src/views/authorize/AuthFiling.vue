<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <PageNote>注:备案中心两类——① 附录G:取得经营权的单位对外授权须在公司数字化部<b>授权备案</b>;
      ② 附录D 附件2(协议第四章(三)):被授权方对外提供数据产品/服务须在甲方处<b>产品备案</b>(涉及数据表限协议附件1清单内,须附《安全合规评审意见》)。</PageNote>
    <el-tabs v-model="tab" type="border-card" @tab-change="onTabChange">
      <!-- ① 对外经营权授权备案(附录G) -->
      <el-tab-pane label="① 授权备案(附录G)" name="auth">
        <div style="margin-bottom:10px">
          <el-select v-model="q.filingStatus" placeholder="全部状态" clearable style="width:160px" @change="load">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
          <el-button type="primary" style="margin-left:8px" @click="onAdd">新增授权备案</el-button>
        </div>
        <el-table :data="rows" v-loading="loading" border stripe>
          <el-table-column type="index" label="序号" width="64" align="center" />
          <el-table-column prop="filingNo" label="备案编号" width="170" show-overflow-tooltip />
          <el-table-column prop="filingOrg" label="授权单位(备案单位)" min-width="150" show-overflow-tooltip />
          <el-table-column prop="granteeOrg" label="被授权单位" min-width="140" show-overflow-tooltip />
          <el-table-column prop="agreementNo" label="协议编号" min-width="150" show-overflow-tooltip><template #default="{ row }">{{ row.agreementNo || '—' }}</template></el-table-column>
          <el-table-column prop="rightType" label="产权类型" width="130" />
          <el-table-column prop="validDate" label="授权期限" width="120"><template #default="{ row }">{{ row.validDate ? String(row.validDate).slice(0,10) : '—' }}</template></el-table-column>
          <el-table-column prop="filingStatus" label="状态" width="90" align="center">
            <template #default="{ row }"><span :class="'prm-c-' + ((row.filingStatus==='已备案'?'success':'warning') || 'primary')">{{ row.filingStatus }}</span></template>
          </el-table-column>
          <el-table-column prop="filingTime" label="备案时间" width="170" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :disabled="row.filingStatus==='已备案'" @click="onFile(row)">完成备案</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination style="margin-top:20px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
          :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
      </el-tab-pane>

      <!-- ② 数据产品备案(附录D 附件2 表2) -->
      <el-tab-pane label="② 数据产品备案(附件2)" name="product">
        <div style="margin-bottom:10px">
          <el-select v-model="q.filingStatus" placeholder="全部状态" clearable style="width:160px" @change="load">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
          <el-button type="primary" style="margin-left:8px" @click="onAddProduct">新增产品备案</el-button>
        </div>
        <el-table :data="rows" v-loading="loading" border stripe>
          <el-table-column type="index" label="序号" width="56" align="center" />
          <el-table-column prop="filingNo" label="备案编号" width="160" show-overflow-tooltip />
          <el-table-column prop="filingOrg" label="单位名称" min-width="130" show-overflow-tooltip />
          <el-table-column prop="productName" label="产品名称" min-width="130" show-overflow-tooltip />
          <el-table-column prop="appScenario" label="应用场景" width="110" show-overflow-tooltip />
          <el-table-column prop="serviceTarget" label="服务对象" width="110" show-overflow-tooltip />
          <el-table-column prop="involvedTables" label="涉及授权数据表" min-width="160" show-overflow-tooltip />
          <el-table-column prop="agreementNo" label="协议编号" min-width="140" show-overflow-tooltip><template #default="{ row }">{{ row.agreementNo || '—' }}</template></el-table-column>
          <el-table-column prop="filingStatus" label="状态" width="86" align="center">
            <template #default="{ row }"><span :class="'prm-c-' + ((row.filingStatus==='已备案'?'success':'warning') || 'primary')">{{ row.filingStatus }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <!-- 附件2:安全合规评审意见附件(材料挂 filingId);完成备案的前置门禁 -->
              <el-upload :show-file-list="false" :http-request="(o)=>doUploadOpinion(row, o.file)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" style="display:inline-block">
                <el-button link type="warning" :disabled="row.filingStatus==='已备案'">上传评审意见</el-button>
              </el-upload>
              <el-button link type="primary" style="margin-left:6px" :disabled="row.filingStatus==='已备案'" @click="onFile(row)">完成备案</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination style="margin-top:20px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
          :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
      </el-tab-pane>
    </el-tabs>

    <!-- 新增授权备案(附录G) -->
    <el-dialog v-model="dlg" title="新增对外经营权授权备案(附录G)" width="560px" align-center>
      <el-alert type="info" :closable="false" style="margin-bottom:10px"
        title="备案对象=一份已生效的经营权授权协议。选取协议后自动带出 被授权单位/协议编号/授权期限。附件须提交《授权协议》及《数据授权清单》电子版(随协议存档)。" />
      <el-form :model="form" label-width="120px">
        <el-form-item label="选取经营权协议" required>
          <el-select v-model="form.agreementId" filterable placeholder="选择已生效的经营权授权协议" style="width:100%"
            :loading="agLoading" @change="onPickAgreement">
            <el-option v-for="a in agreementOpts" :key="a.agreementId" :label="`${a.agreementNo}（${a.granteeOrg || '—'}）`" :value="a.agreementId" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权单位(备案单位)"><el-input v-model="form.filingOrg" placeholder="本分子公司名称(授权方)" /></el-form-item>
        <el-form-item label="被授权单位"><el-input v-model="form.granteeOrg" placeholder="选协议后自动带出,可改" /></el-form-item>
        <el-form-item label="产权类型"><el-input model-value="经营权" disabled /></el-form-item>
        <template v-if="elem">
          <el-form-item label="协议编号"><el-input :model-value="elem.agreementNo" disabled /></el-form-item>
          <el-form-item label="授权数据"><el-input :model-value="agreementDataSummary" disabled /></el-form-item>
        </template>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" maxlength="500" show-word-limit :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSave">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>

    <!-- 新增数据产品备案(附录D 附件2 表2) -->
    <el-dialog v-model="pdlg" title="新增数据产品备案(附录D 附件2)" width="620px" align-center>
      <el-alert type="info" :closable="false" style="margin-bottom:10px"
        title="协议第四章(三):被授权方对外提供数据产品/服务须在甲方处备案。涉及数据表只能从关联协议附件1《数据授权清单》中选取(产品不得使用清单外数据);建档后须上传《安全合规评审意见》方可完成备案。" />
      <el-form :model="pform" label-width="130px">
        <el-form-item label="关联经营权协议" required>
          <el-select v-model="pform.agreementId" filterable placeholder="选择已归档生效的经营权授权协议" style="width:100%"
            :loading="agLoading" @change="onPickProductAgreement">
            <el-option v-for="a in agreementOpts" :key="a.agreementId" :label="`${a.agreementNo}（${a.granteeOrg || '—'}）`" :value="a.agreementId" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位名称(乙方)" required><el-input v-model="pform.filingOrg" placeholder="被授权单位(选协议自动带出,可改)" /></el-form-item>
        <el-form-item label="产品名称" required><el-input v-model="pform.productName" maxlength="100" placeholder="如:电力信用分产品" /></el-form-item>
        <el-form-item label="产品介绍"><el-input v-model="pform.productIntro" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
        <el-form-item label="应用场景"><el-input v-model="pform.appScenario" maxlength="200" placeholder="如:金融征信" /></el-form-item>
        <el-form-item label="服务对象"><el-input v-model="pform.serviceTarget" maxlength="100" placeholder="如:银行/金融机构" /></el-form-item>
        <el-form-item label="涉及授权数据表" required>
          <el-select v-model="pickedTables" multiple style="width:100%" placeholder="多选,限协议附件1《数据授权清单》内">
            <el-option v-for="t in agreementTables" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSaveProduct">确定(建档·待备案)</el-button><el-button @click="pdlg=false">取消</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import PageNote from '@/components/PageNote.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageFiling, createFiling, fileFiling, pageAgreement, getAgreementElements, uploadAuthMaterialFile } from '@/api/authorize'
import { useTablePage } from '@/composables/useTablePage'
const statuses = ['待备案', '已备案']
const tab = ref('auth')
const filingTypeOf = () => (tab.value === 'product' ? '产品备案' : '授权备案')
const { query: q, rows, total, loading, load } = useTablePage(
  (p) => pageFiling({ ...p, filingStatus: p.filingStatus || undefined, filingType: filingTypeOf() }), { filingStatus: '' })
function onTabChange() { q.filingStatus = ''; q.current = 1; load() }

// ===== ① 授权备案(附录G) =====
const dlg = ref(false)
const form = reactive({ filingOrg: '', granteeOrg: '', rightType: '经营权', filingType: '授权备案', agreementId: '', applyId: '', remark: '' })
// 经营权协议选择器(已审核通过的经营权运营授权协议)+ 选后要素带出
const agreementOpts = ref([]); const agLoading = ref(false); const elem = ref(null)
const agreementDataSummary = computed(() => {
  if (!elem.value) return '—'
  if (elem.value.items && elem.value.items.length) return `批量清单 ${elem.value.items.length} 张数据表`
  return `${elem.value.sysName || '—'} / ${elem.value.dataTable || '—'}`
})
async function loadAgreements() {
  agLoading.value = true
  try {
    const r = await pageAgreement({ current: 1, size: 100, agreementType: '经营权' })
    agreementOpts.value = (r.records || []).filter(a => a.reviewStatus === '审核通过')
  } catch { agreementOpts.value = [] } finally { agLoading.value = false }
}
// 选协议 → 复用 getAgreementElements 自动带出 被授权单位/协议编号/授权期限/系统/数据表
async function onPickAgreement(id) {
  elem.value = null
  if (!id) return
  const e = await getAgreementElements(id)
  elem.value = e
  if (e) {
    form.granteeOrg = e.granteeOrg || form.granteeOrg
    form.applyId = e.applyId || ''
  }
}
function onAdd() {
  Object.assign(form, { filingOrg: '', granteeOrg: '', rightType: '经营权', filingType: '授权备案', agreementId: '', applyId: '', remark: '' })
  elem.value = null; dlg.value = true; loadAgreements()
}
async function onSave() {
  if (!form.agreementId) { ElMessage.warning('请选取要备案的经营权授权协议'); return }
  if (!form.granteeOrg) { ElMessage.warning('被授权单位为空(选协议后自动带出)'); return }
  await createFiling({ ...form }); ElMessage.success('已登记备案(待备案)'); dlg.value = false; load()
}

// ===== ② 数据产品备案(附录D 附件2):涉及表限协议附件1清单内 + 评审意见门禁 =====
const pdlg = ref(false)
const pform = reactive({ filingType: '产品备案', filingOrg: '', granteeOrg: '', rightType: '经营权', agreementId: '', productName: '', productIntro: '', appScenario: '', serviceTarget: '' })
const agreementTables = ref([]); const pickedTables = ref([])
function onAddProduct() {
  Object.assign(pform, { filingType: '产品备案', filingOrg: '', granteeOrg: '', rightType: '经营权', agreementId: '', productName: '', productIntro: '', appScenario: '', serviceTarget: '' })
  agreementTables.value = []; pickedTables.value = []
  pdlg.value = true; loadAgreements()
}
// 选协议 → 附件1《数据授权清单》的表集合作为多选项(单一真源,产品不得越界)
async function onPickProductAgreement(id) {
  agreementTables.value = []; pickedTables.value = []
  if (!id) return
  const e = await getAgreementElements(id)
  if (e) {
    pform.filingOrg = e.granteeOrg || pform.filingOrg
    pform.granteeOrg = e.granteeOrg || ''
    const tables = e.items && e.items.length ? e.items.map(i => i.dataTable) : [e.dataTable]
    agreementTables.value = [...new Set(tables.filter(Boolean))]
  }
}
async function onSaveProduct() {
  if (!pform.agreementId) { ElMessage.warning('请关联已归档的经营权授权协议'); return }
  if (!pform.productName) { ElMessage.warning('请填写产品名称(附件2)'); return }
  if (!pickedTables.value.length) { ElMessage.warning('请选择涉及授权数据表(限协议附件1清单内)'); return }
  await createFiling({ ...pform, involvedTables: pickedTables.value.join('、') })
  ElMessage.success('产品备案已建档(待备案);请上传《安全合规评审意见》后完成备案')
  pdlg.value = false; load()
}
// 附件2 评审意见附件:材料挂 filingId(完成备案的门禁)
async function doUploadOpinion(row, file) {
  const fd = new FormData(); fd.append('file', file); fd.append('applyId', row.filingId); fd.append('materialName', '安全合规评审意见')
  await uploadAuthMaterialFile(fd)
  ElMessage.success('《安全合规评审意见》已上传,可完成备案')
}
async function onFile(row) { await fileFiling(row.filingId); ElMessage.success('已完成备案'); load() }
onMounted(load)
</script>
