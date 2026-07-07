<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  数据授权流转进度时间轴(单一真相):节点严格对齐 办数字〔2025〕35号 工作指引 附录C
  表1《数据批量授权业务管理流程》/ 表2《一事一议数据授权业务管理流程》。
  - 批量 mode='batch' :合规→主管→经理→副总→领导小组决策→双签(附录D)→执行授权·归档(经营权另备案附录G)
  - 一事一议 mode='single':本单位初审→合规→业务部门→主管→经理→副总→双签(附录D)→执行授权·记录(无领导小组)
  口径:授权凭证=《数据运营授权协议(附录D)》双签 + (对外经营权)备案附录G;35号文全程无「证书/发证」。
-->
<template>
  <div class="afp">
    <el-alert v-if="rejected" type="error" :closable="false" show-icon
      title="已驳回" description="本申报被驳回,请按审核意见修订后重新提交。" style="margin-bottom:10px" />
    <el-steps direction="vertical" :active="activeIndex" :process-status="rejected ? 'error' : 'process'" finish-status="success">
      <el-step v-for="n in nodes" :key="n.key" :title="n.label">
        <template #description>
          <span class="afp-role">责任:{{ n.role }}</span>
        </template>
      </el-step>
    </el-steps>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 'batch' 批量授权 | 'single' 一事一议
  mode: { type: String, default: 'batch' },
  // 当前所处节点 key(由调用方按业务状态映射);留空=刚提交(停在首个审核节点)
  current: { type: String, default: '' },
  // 产品状态串(可选):传入则自动映射到节点 key,免调用方手算
  status: { type: String, default: '' },
  rejected: { type: Boolean, default: false }
})

// 批量:附录C 表1 节点 50–120(申报人视角合并上游征集/归集为「提交申报稿」)
const BATCH_NODES = [
  { key: 'submit', label: '提交申报稿(表6 批量授权清单)', role: '申报人 · 数字化部' },
  { key: 'compliance', label: '合规管控小组审核', role: '数据产权合规管控小组' },
  { key: 'manager', label: '数字化部主管审核', role: '主管' },
  { key: 'director', label: '经理 / 高级经理审核', role: '经理 / 高级经理' },
  { key: 'gm', label: '副总 / 总经理审批', role: '副总经理 / 总经理' },
  { key: 'leadership', label: '领导小组决策批准', role: '网络安全和数字化转型领导小组办公室' },
  { key: 'sign', label: '甲乙双签《数据运营授权协议(附录D)》', role: '授权方 / 被授权方' },
  { key: 'execute', label: '执行授权 · 归档(对外经营权另备案附录G)', role: '总部数字化部' }
]

// 一事一议:附录C 表2 节点 10–130(无领导小组;终点=副总/总经理审批后双签执行)
const SINGLE_NODES = [
  { key: 'submit', label: '提交申请(表5 数据授权申请单)', role: '申报人 · 业务管理部门' },
  { key: 'unit', label: '本单位初审(业务 / 数字化 / 分管领导)', role: '申报单位' },
  { key: 'compliance', label: '合规管控小组评审', role: '数据产权合规管控小组' },
  { key: 'business', label: '业务管理部门审核', role: '业务管理部门 经理 / 高级经理' },
  { key: 'manager', label: '数字化部主管审核', role: '主管' },
  { key: 'director', label: '数字化部经理 / 高级经理审核', role: '经理 / 高级经理' },
  { key: 'gm', label: '副总 / 总经理审批', role: '副总经理 / 总经理' },
  { key: 'sign', label: '甲乙双签《数据运营授权协议(附录D)》', role: '授权方 / 被授权方' },
  { key: 'execute', label: '执行授权 · 记录', role: '总部数字化部' }
]

// 产品状态串(AuthApply.status / BatchAuthList.listStatus)→ 节点 key
// 一事一议:单位初审中(表2 20-50 真实节点)→…→批准(待双签)→已生效(双签+承诺函归档后)
const STATUS_TO_KEY = {
  草案: 'submit', 草稿: 'submit', 申报稿: 'compliance',
  单位初审中: 'unit',
  合规审核中: 'compliance', 业务审核中: 'business', 主管审核中: 'manager',
  经理审核中: 'director', 副总审批中: 'gm', 领导小组审批中: 'leadership',
  批准: 'sign', 已生效: 'execute'
}

const nodes = computed(() => (props.mode === 'single' ? SINGLE_NODES : BATCH_NODES))

const activeIndex = computed(() => {
  const key = props.current || STATUS_TO_KEY[props.status] || 'compliance'
  const i = nodes.value.findIndex((n) => n.key === key)
  return i < 0 ? 1 : i
})
</script>

<style scoped>
.afp { padding: 4px 2px; }
.afp-role { font-size: 12px; color: var(--prm-color-text-secondary); }
</style>
