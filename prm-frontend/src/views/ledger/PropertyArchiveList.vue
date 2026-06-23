<template>
  <div class="prm-page">
    <!-- 只读查询:当前账户权限可见的数据资产卡片的确权/授权信息。卡片来自数据资产管理平台,本模块不新增卡片。 -->
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="资产名称/ID">
          <el-input v-model="q.keyword" placeholder="按资产名称或资产ID检索" clearable style="width: 240px"
                    @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="确权状态">
          <el-select v-model="q.state" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="s in STATES" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="assetId" label="资产ID" min-width="150" show-overflow-tooltip />
        <el-table-column prop="assetName" label="资产名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="确权状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="stateTag(row.state)">{{ row.state }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rightType" label="权属类型" width="120" />
        <el-table-column prop="rightHolder" label="权利主体" min-width="140" show-overflow-tooltip />
        <el-table-column prop="registerType" label="登记类型" width="110" />
        <el-table-column prop="respDept" label="责任部门" min-width="120" show-overflow-tooltip />
        <el-table-column label="有效期" width="120" align="center">
          <template #default="{ row }">{{ fmtDate(row.validDate) }}</template>
        </el-table-column>
        <el-table-column prop="equityCount" label="权益条目" width="90" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onView(row)">查看</el-button>
            <el-button v-if="row.state === '已确权'" link type="primary" @click="onChange(row)">发起变更</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="prm-pager"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :current-page="q.current"
        :page-size="q.size"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="onPage"
        @size-change="onSize"
      />
      <div class="prm-table-note">注:本页仅只读展示数据资产卡片的确权/授权信息;卡片是数据资产管理平台主数据,产权模块不在此新增/批量新增。未确权资产显示"待确权"。</div>
    </div>

    <!-- 卡片产权/权益明细(只读) -->
    <prm-dialog v-model="dlg" variant="detail" :title="`产权档案 · ${cur?.assetName || cur?.assetId || ''}`" width="760px">
      <el-descriptions title="产权信息" :column="2" size="small" border v-loading="detailLoading">
        <el-descriptions-item label="确权状态">
          <el-tag size="small" :type="stateTag(prop?.state)">{{ prop?.state || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="登记类型">{{ prop?.registerType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="权属类型">{{ prop?.rightType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="权利主体">{{ prop?.rightHolder || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据责任部门">{{ prop?.respDept || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源方式">{{ prop?.sourceMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源主体">{{ prop?.sourceSubject || '-' }}</el-descriptions-item>
        <el-descriptions-item label="行政监管(G)">
          <el-tag size="small" :type="prop?.involvesRegulation ? 'warning' : 'info'">{{ prop?.involvesRegulation ? '是' : '否' }}</el-tag>
          <span v-if="prop?.regulated" class="dim-note">{{ prop.regulated }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="个人隐私(H)">
          <el-tag size="small" :type="prop?.involvesPrivacy ? 'warning' : 'info'">{{ prop?.involvesPrivacy ? '是' : '否' }}</el-tag>
          <span v-if="prop?.privacyInfo" class="dim-note">{{ prop.privacyInfo }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="第三方商密(I)">
          <el-tag size="small" :type="prop?.involvesTradeSecret ? 'warning' : 'info'">{{ prop?.involvesTradeSecret ? '是' : '否' }}</el-tag>
          <span v-if="prop?.thirdPartyInfo" class="dim-note">{{ prop.thirdPartyInfo }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="第三方协议(J)">
          <el-tag size="small" :type="prop?.involvesThirdPartyAgreement ? 'warning' : 'info'">{{ prop?.involvesThirdPartyAgreement ? '是' : '否' }}</el-tag>
          <span v-if="prop?.relationSubject" class="dim-note">{{ prop.relationSubject }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="有效期">{{ fmtDate(prop?.validDate) }}</el-descriptions-item>
        <el-descriptions-item label="确权时间">{{ fmtDateTime(prop?.confirmTime) }}</el-descriptions-item>
        <el-descriptions-item label="确权凭证(上链)">{{ prop?.evidenceRef || '-' }}</el-descriptions-item>
        <el-descriptions-item label="认定意见/确权结论" :span="2">{{ prop?.recognitionOpinion || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="prop?.message" label="说明" :span="2">{{ prop.message }}</el-descriptions-item>
      </el-descriptions>

      <div class="eq-title">权益基本信息</div>
      <el-table :data="equity" border size="small" empty-text="暂无权益条目">
        <el-table-column prop="cardNo" label="权益编号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="rightType" label="权益类型" width="120" />
        <el-table-column prop="rightOwner" label="权益主体" min-width="120" show-overflow-tooltip />
        <el-table-column prop="scope" label="权益范围" width="110" />
        <el-table-column label="有效期" width="120" align="center">
          <template #default="{ row }">{{ fmtDate(row.validDate) }}</template>
        </el-table-column>
        <el-table-column prop="cardStatus" label="状态" width="90" align="center" />
      </el-table>
    </prm-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageAssetArchive, getAssetProperty, getAssetEquity } from '@/api/assetCard'
import PrmDialog from '@/components/PrmDialog.vue'

const router = useRouter()
// 已确权资产「发起变更」:跳一站式确权向导并预选该资产,向导按确权状态自动定为「确权变更」(附录F §3.3.2)
function onChange(row) {
  router.push({ path: '/dpr/confirm/wizard', query: { assetId: row.assetId } })
}

const STATES = ['待确权', '确权中', '已确权', '已驳回']

const q = reactive({ keyword: '', state: '', current: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await pageAssetArchive({
      current: q.current, size: q.size,
      keyword: q.keyword || undefined, state: q.state || undefined
    })
    rows.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() { q.current = 1; load() }
function onReset() { q.keyword = ''; q.state = ''; q.current = 1; load() }
function onPage(p) { q.current = p; load() }
function onSize(s) { q.size = s; q.current = 1; load() }

// 明细
const dlg = ref(false)
const cur = ref(null)
const prop = ref(null)
const equity = ref([])
const detailLoading = ref(false)

async function onView(row) {
  cur.value = row
  prop.value = null
  equity.value = []
  dlg.value = true
  detailLoading.value = true
  try {
    const [p, e] = await Promise.all([getAssetProperty(row.assetId), getAssetEquity(row.assetId)])
    prop.value = p
    equity.value = e || []
  } finally {
    detailLoading.value = false
  }
}

function stateTag(state) {
  return { 已确权: 'success', 确权中: 'warning', 待确权: 'info', 已驳回: 'danger' }[state] || 'info'
}
function fmtDate(v) { return v ? String(v).slice(0, 10) : '-' }
function fmtDateTime(v) { return v ? String(v).replace('T', ' ').slice(0, 19) : '-' }

load()
</script>

<style scoped>
.prm-pager { margin-top: 12px; justify-content: flex-end; }
.eq-title { margin: 16px 0 8px; font-size: 14px; font-weight: 600; color: var(--prm-color-text); }
.dim-note { margin-left: 6px; font-size: 12px; color: var(--prm-color-text-secondary); }
</style>
