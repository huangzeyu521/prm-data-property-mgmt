<template>
  <div class="prm-page">
    <div class="prm-table-card">
      <div class="prm-table-note" style="margin:0 0 12px 0">注:授权审核工作台,审批通过自动签发授权权益证书并触发底层数据资源权限开通。</div>
      <el-table :data="reviewing" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="applyNo" label="申请编号" width="150" show-overflow-tooltip />
        <el-table-column prop="authMode" label="模式" width="100" align="center" />
        <el-table-column prop="assetName" label="资产名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="granteeOrg" label="被授权方" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" @click="onApprove(row)">审批通过</el-button>
            <el-button link type="danger" @click="onReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageAuthApply, approveAuth, rejectAuth } from '@/api/authorize'
const rows = ref([]); const loading = ref(false)
const PENDING = ['合规审核中', '业务审核中', '主管审核中', '经理审核中', '副总审批中', '数字化部认定中', '领导小组审批中']
const reviewing = computed(() => rows.value.filter(r => PENDING.includes(r.status)))
async function load() { loading.value = true; try { const r = await pageAuthApply({ current: 1, size: 100 }); rows.value = r.records || [] } finally { loading.value = false } }
async function onApprove(row) { const certId = await approveAuth(row.applyId); ElMessage.success(certId ? '终审通过,已签发授权证书' : '本级审批通过,进入下一环节'); load() }
function onReject(row) { ElMessageBox.prompt('请输入驳回原因', '驳回', {}).then(async ({ value }) => { await rejectAuth(row.applyId, value); ElMessage.success('已驳回'); load() }).catch(() => {}) }
onMounted(load)
</script>
