<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-table-note" style="margin-bottom:8px">
      协议工作台:协议在授权批准/生效时由系统自动「形成」。用户只需上传双方签章——
      <b>双签完成即系统自动核验、开通数据权限、归档存证</b>(对齐 35号文:协议环节无独立人工审核)。
    </div>
    <el-tabs v-model="tab" type="border-card">
      <el-tab-pane label="① 签章上传" name="seal">
        <AgreementManage v-if="tab==='seal'" action-prop="seal" :key="'seal'+spin" />
      </el-tab-pane>
      <el-tab-pane label="② 协议存档" name="archive">
        <AgreementManage v-if="tab==='archive'" action-prop="archive" :key="'archive'+spin" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AgreementManage from '@/views/authorize/AgreementManage.vue'
const tab = ref('seal')
const spin = ref(0) // 切回标签时刷新内层数据
</script>
