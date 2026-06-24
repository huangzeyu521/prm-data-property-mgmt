<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-table-note" style="margin-bottom:8px">
      协议工作台:一份协议从「签章上传 → 协议审核 → 协议存档」在此一处流转,无需在多个菜单间切换。
    </div>
    <el-tabs v-model="tab" type="border-card">
      <el-tab-pane label="① 签章上传" name="seal">
        <AgreementManage v-if="tab==='seal'" action-prop="seal" :key="'seal'+spin" />
      </el-tab-pane>
      <el-tab-pane label="② 协议审核" name="review">
        <AgreementManage v-if="tab==='review'" action-prop="review" :key="'review'+spin" />
      </el-tab-pane>
      <el-tab-pane label="③ 协议存档" name="archive">
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
