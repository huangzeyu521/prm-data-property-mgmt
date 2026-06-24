<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <el-page-header content="数据集产权详情" @back="goBack" style="margin-bottom:16px" />
    <el-card v-loading="loading">
      <el-tabs v-model="tab">
        <el-tab-pane label="基本信息" name="base">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="资产名称">{{ a.assetName }}</el-descriptions-item>
            <el-descriptions-item label="资产ID">{{ a.assetId }}</el-descriptions-item>
            <el-descriptions-item label="确权状态"><el-tag :type="tag(a.confirmStatus)">{{ a.confirmStatus }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="授权状态">{{ a.authStatus || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="权属信息" name="right">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="产权类型">{{ a.rightType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="权利主体">{{ a.rightSubject || '-' }}</el-descriptions-item>
            <el-descriptions-item label="权利客体">{{ a.rightObject || '-' }}</el-descriptions-item>
            <el-descriptions-item label="取得方式">{{ a.acquireMode || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="权限范围" name="scope">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="使用范围">{{ a.useScope || '-' }}</el-descriptions-item>
            <el-descriptions-item label="有效期">{{ a.validDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="关联权益卡片">{{ a.equityCardId || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="责任部门" name="dept">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="责任部门">{{ a.respDept || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="技术属性" name="tech">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="使用状态"><el-tag :type="usageTag(asset.assetStatus)">{{ asset.assetStatus || '-' }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="所属系统">{{ asset.systemName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="模式名称">{{ asset.schemaName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="资产类型">{{ asset.assetType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="安全等级">{{ asset.securityLevel || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属子公司">{{ asset.subsidiaryName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="资产来源">{{ asset.assetSource || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="prm-table-note" style="margin-top:10px">注:技术属性取自数据资产元数据(IM_DPM_DATA_ASSET_INFO),与上方业务维度合成"技术+业务"双维度产权档案。</div>
        </el-tab-pane>
        <el-tab-pane label="变更历史" name="history">
          <el-timeline>
            <el-timeline-item v-for="r in changes" :key="r.changeId" :timestamp="r.changeTime" placement="top">
              {{ r.changeType }} · {{ r.fieldName }}:<span style="color:#e21f0c">{{ r.beforeValue }}</span> → <span style="color:#36b21d">{{ r.afterValue }}</span>
            </el-timeline-item>
            <el-empty v-if="!changes.length" description="暂无变更记录" />
          </el-timeline>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArchive } from '@/api/propertyArchive'
import { pageChangeRecord, getAsset } from '@/api/ledger'

const route = useRoute()
const router = useRouter()
const a = reactive({})
const asset = reactive({})
const changes = ref([])
const loading = ref(false)
const tab = ref('base')
function tag(s) { return { 已确权: 'success', 申请中: 'warning', 失败: 'danger' }[s] || 'info' }
function usageTag(s) { return { 在用: 'success', 停用: 'info', 已下线: 'danger' }[s] || 'warning' }
function goBack() { router.back() }

async function load() {
  const id = route.query.archiveId
  if (!id) return
  loading.value = true
  try {
    const detail = await getArchive(id)
    Object.assign(a, detail)
    if (detail.assetId) {
      const [res, ast] = await Promise.all([
        pageChangeRecord({ current: 1, size: 50, assetId: detail.assetId }),
        getAsset(detail.assetId).catch(() => ({}))
      ])
      changes.value = res.records || []
      Object.assign(asset, ast || {})
    }
  } finally { loading.value = false }
}
onMounted(load)
</script>
