<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-table-card">
      <PageNote>注:审核工作台展示待我审核的确权申请,支持单条/批量审批,可查看详情(申请+材料+进度轨迹)。</PageNote>
      <div style="margin-bottom:10px">
        <el-button type="success" :disabled="!sel.length" @click="onBatchApprove">批量通过（{{ sel.length }}）</el-button>
        <el-button type="danger" :disabled="!sel.length" @click="onBatchReject">批量驳回（{{ sel.length }}）</el-button>
      </div>
      <el-table :row-class-name="rowHl" :data="reviewing" v-loading="loading" border stripe @selection-change="s => sel = s">
        <el-table-column type="selection" width="46" :selectable="row => canAct(row.status)" />
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="applyNo" label="申请编号" width="150" show-overflow-tooltip />
        <el-table-column label="系统名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ sysName(row) }}</template>
        </el-table-column>
        <el-table-column label="登记类型" width="98" align="center">
          <template #default="{ row }">
            <span :class="'prm-c-' + ((row.registerType === '确权变更' ? 'warning' : 'primary') || 'primary')">{{ row.registerType === '确权变更' ? '确权变更' : '初始确权' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="变更要素" min-width="170">
          <template #default="{ row }">
            <template v-if="row.registerType === '确权变更'">
              <span v-for="t in triggerTags(row)" :key="t" style="margin:1px" :class="'prm-c-' + ((t === '数据新增' ? 'success' : 'warning') || 'primary')">{{ t }}</span>
              <span v-if="row.changeVersion" style="margin:1px" class="prm-c-info">v{{ row.changeVersion }}</span>
            </template>
            <span v-else style="color:var(--prm-color-text-disabled)">—（初始）</span>
          </template>
        </el-table-column>
        <el-table-column label="权属类型" width="160">
          <template #default="{ row }">
            <el-tooltip v-for="r in rightTags(row.rightType)" :key="r.full" :content="r.full" placement="top">
              <span style="margin:1px" :class="'prm-c-' + ((r.type) || 'primary')">{{ r.short }}</span>
            </el-tooltip>
            <span v-if="!rightTags(row.rightType).length" style="color:var(--prm-color-text-disabled)">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="当前环节" width="120" align="center">
          <template #default="{ row }"><span class="prm-c-warning">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onDetail(row)">详情</el-button>
            <template v-if="canAct(row.status)">
              <el-button link type="success" @click="onApprove(row)">审批通过</el-button>
              <el-button link type="danger" @click="onReject(row)">驳回</el-button>
            </template>
            <el-tooltip v-else :content="`本节点须「${needRoleLabel(row.status)}」处理`" placement="top">
              <span style="color:var(--prm-color-text-disabled);font-size:12px;margin-left:6px">非本人审批</span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="drawer" :title="`审核详情 — ${cur.applyNo || ''}`" size="48%">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="系统名称">{{ sysName(cur) }}</el-descriptions-item>
        <el-descriptions-item label="登记类型">
          <span :class="'prm-c-' + ((isChangeApply ? 'warning' : 'primary') || 'primary')">{{ isChangeApply ? '确权变更' : '初始确权' }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="isChangeApply" label="变更要素" :span="2">
          <span v-for="t in triggerTags(cur)" :key="t" style="margin:1px" :class="'prm-c-' + ((t === '数据新增' ? 'success' : 'warning') || 'primary')">{{ t }}</span>
          <span v-if="cur.changeVersion" style="margin:1px" class="prm-c-info">v{{ cur.changeVersion }}</span>
          <span v-if="cur.baselineRef" style="color:var(--prm-color-text-weak);font-size:12px;margin-left:6px">基线 {{ cur.baselineRef }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="权属类型">
          <el-tooltip v-for="r in rightTags(cur.rightType)" :key="r.full" :content="r.full" placement="top">
            <span style="margin:1px" :class="'prm-c-' + ((r.type) || 'primary')">{{ r.short }}</span>
          </el-tooltip>
          <span v-if="!rightTags(cur.rightType).length" style="color:var(--prm-color-text-disabled)">—</span>
        </el-descriptions-item>
        <el-descriptions-item label="权属主体">{{ cur.rightHolder || '-' }}</el-descriptions-item>
        <el-descriptions-item label="主体层级">{{ cur.subjectLevel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="责任部门">{{ cur.respDept || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源识别(A–F)">
          <span v-for="c in codeList(cur.sourceIdentification)" :key="c" style="margin:1px" class="prm-c-primary">{{ c }}</span>
          <span v-if="!codeList(cur.sourceIdentification).length" style="color:var(--prm-color-text-disabled)">—</span>
        </el-descriptions-item>
        <el-descriptions-item label="信息关联(G–J)">
          <span v-for="c in codeList(cur.relationIdentification)" :key="c" style="margin:1px" :class="'prm-c-' + ((c === 'H' ? 'danger' : 'warning') || 'primary')">{{ c }}</span>
          <span v-if="!codeList(cur.relationIdentification).length" style="color:var(--prm-color-text-disabled)">—</span>
        </el-descriptions-item>
        <el-descriptions-item label="涉第三方/敏感">{{ cur.involvesThirdParty ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="当前环节">{{ cur.status }}</el-descriptions-item>
      </el-descriptions>

      <!-- P0 审批侧变更对照:优先渲染「申报时刻固化快照」(与申报人所见同源,审核期间不随基线漂移);
           旧单无快照回退实时计算(基线同源 baseline-full)。含三态:修改/新增码/移除码 + 逐表 + 表4。 -->
      <template v-if="isChangeApply">
        <div class="rv-h">
          确权变更对照
          <span style="margin-left:8px" class="prm-c-warning">触发:{{ cur.changeTrigger || '—' }}{{ cur.changeVersion ? ' · v' + cur.changeVersion : '' }}{{ cur.baselineRef ? ' · 基线 ' + cur.baselineRef : '' }}</span>
          <span v-if="diffSnap" style="margin-left:6px" class="prm-c-success">申报时刻固化快照·审申同源</span>
          <el-button v-if="diffSnap && !isDataAddApply" size="small" plain style="margin-left:8px" :loading="rechecking" @click="recheckBaseline">
            按当前基线重算校核
          </el-button>
        </div>
        <!-- 基线校核结果:验证申报所依据的基线是否仍是当前最新版(乐观锁的人工视角) -->
        <el-alert v-if="recheckResult" :type="recheckResult.ok ? 'success' : 'error'" :closable="false" style="margin-bottom:8px"
          :title="recheckResult.msg" />
        <!-- 数据新增:insert 模式,既有不动、无基线 diff;快照带集合级前后计数(前N张→后N+M张) -->
        <el-alert v-if="isDataAddApply" type="success" :closable="false"
          :title="cur.changeSummary || '数据新增:新表首次确权登记(系统内既有已确权库表不动,不联动授权),按本次申报内容审核。'">
          <div v-if="diffSnap && diffSnap.counts" style="font-size:12px">
            变更前:系统内已确权 <b>{{ diffSnap.counts.prior }}</b> 张库表
            → 变更后:<b>{{ diffSnap.counts.prior + diffSnap.counts.added }}</b> 张(本次新增 {{ diffSnap.counts.added }} 张首次登记)。
          </div>
        </el-alert>
        <template v-else-if="diffSnap">
          <!-- P1′ 期望变更点(申报时固化):先看"该触发类型应变更什么、是否变了",再看逐维结果 -->
          <div v-if="diffSnap.expected && diffSnap.expected.length" style="margin-bottom:8px">
            <span v-for="p in diffSnap.expected" :key="p.label" style="margin:2px 6px 2px 0" :class="'prm-c-' + ((p.met ? 'success' : 'warning') || 'primary')">
              {{ p.met ? '已变更' : '未变更' }} · {{ p.label }}
            </span>
            <el-alert v-if="diffSnap.expected.every(p => !p.met)" type="warning" :closable="false" style="margin-top:4px"
              :title="`申报触发「${cur.changeTrigger}」的期望变更对象均未修改,请重点核对本单是否确有变更实质。`" />
          </div>
          <el-alert v-if="cur.changeSummary" type="warning" :closable="false" :title="cur.changeSummary" style="margin-bottom:8px" />
          <el-table v-if="diffSnap.dims && diffSnap.dims.length" :data="diffSnap.dims" border size="small" style="margin-top:8px">
            <el-table-column prop="key" label="变更维度" width="150" />
            <el-table-column label="原值(申报时基线)" min-width="160">
              <template #default="{ row }"><span style="color:var(--prm-color-text-weak);text-decoration:line-through">{{ row.before || '空' }}</span></template>
            </el-table-column>
            <el-table-column label="新值(本次申报)" min-width="160">
              <template #default="{ row }"><span class="prm-c-warning" style="font-weight:600">{{ row.after || '空' }}</span></template>
            </el-table-column>
            <el-table-column label="变化" width="170">
              <template #default="{ row }">
                <span v-for="c in row.added || []" :key="'a' + c" style="margin:1px" class="prm-c-success">+{{ c }} 新增</span>
                <span v-for="c in row.removed || []" :key="'r' + c" style="margin:1px" class="prm-c-danger">−{{ c }} 移除</span>
                <span v-if="!(row.added && row.added.length) && !(row.removed && row.removed.length)" class="prm-c-primary">修改</span>
              </template>
            </el-table-column>
          </el-table>
          <el-alert v-else type="info" :closable="false" title="快照:系统级维度与申报时基线无差异(改动或仅在逐表明细)。" />
          <template v-if="snapTableChanges.length">
            <div style="font-size:12px;color:var(--prm-color-text-weak);margin:8px 0 4px">逐表明细改动({{ (diffSnap.tables || []).length }} 张表)</div>
            <el-table :data="snapTableChanges" border size="small">
              <el-table-column prop="tableName" label="库表" min-width="140" show-overflow-tooltip />
              <el-table-column prop="fieldLabel" label="字段" width="110" />
              <el-table-column label="原值" min-width="140">
                <template #default="{ row }"><span style="color:var(--prm-color-text-weak);text-decoration:line-through">{{ row.before || '空' }}</span></template>
              </el-table-column>
              <el-table-column label="新值" min-width="140">
                <template #default="{ row }"><span class="prm-c-warning">{{ row.after || '空' }}</span></template>
              </el-table-column>
            </el-table>
          </template>
          <template v-if="diffSnap.table4 && diffSnap.table4.length">
            <div style="font-size:12px;color:var(--prm-color-text-weak);margin:8px 0 4px">表4 权益对照(上一版卡片 → 本次归集判定)</div>
            <el-table :data="diffSnap.table4" border size="small">
              <el-table-column prop="label" label="权益" width="90" />
              <el-table-column prop="before" label="变更前" width="90" align="center" />
              <el-table-column prop="after" label="变更后" width="90" align="center" />
              <el-table-column label="判定" width="90" align="center">
                <template #default="{ row }">
                  <span :class="'prm-c-' + ((row.mark === '新增' ? 'success' : (row.mark === '撤销' ? 'danger' : 'info')) || 'primary')">{{ row.mark }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="scope" label="原确权范围" min-width="120" show-overflow-tooltip />
            </el-table>
          </template>
        </template>
        <template v-else>
          <el-alert v-if="cur.changeSummary" type="warning" :closable="false" :title="cur.changeSummary" style="margin-bottom:8px" />
          <el-alert v-if="!changeBaseline" type="info" :closable="false" title="未取到该系统上一版已确权基线(可能首次确权后直接变更或平台未接入),仅按本次申报内容审核。" />
          <template v-else>
            <el-table v-if="changeDiff.length" :data="changeDiff" border size="small" style="margin-top:8px">
              <el-table-column prop="key" label="变更维度" width="150" />
              <el-table-column label="原值(上一版确权)" min-width="170">
                <template #default="{ row }"><span style="color:var(--prm-color-text-weak);text-decoration:line-through">{{ row.before || '空' }}</span></template>
              </el-table-column>
              <el-table-column label="新值(本次申报)" min-width="170">
                <template #default="{ row }"><span class="prm-c-warning" style="font-weight:600">{{ row.after || '空' }}</span></template>
              </el-table-column>
            </el-table>
            <el-alert v-else type="info" :closable="false" title="本次确权变更在勾选的变更簇上与上一版结论无字段差异,请核对是否确需变更。" />
          </template>
        </template>
      </template>

      <div class="rv-h">
        AI 校验结果（人工预审依据）
        <span v-if="cur.status === '人工预审中'" style="margin-left:8px" class="prm-c-warning">本环节须人工复核 AI 结果</span>
      </div>
      <div v-if="aiSnap">
        <el-alert :type="aiSnap.materialCheck && aiSnap.materialCheck.overall === '通过' ? 'success' : 'warning'" :closable="false" style="margin-bottom:8px">
          <div><b>AI 材料校验：{{ aiSnap.materialCheck && aiSnap.materialCheck.overall || '—' }}</b> {{ aiSnap.materialCheck && aiSnap.materialCheck.overallDesc || '' }}</div>
        </el-alert>
        <el-table v-if="aiSnap.materialCheck && aiSnap.materialCheck.items && aiSnap.materialCheck.items.length" :data="aiSnap.materialCheck.items" border size="small" style="margin-bottom:8px">
          <el-table-column prop="materialName" label="材料" min-width="150" show-overflow-tooltip />
          <el-table-column prop="rightHolder" label="识别权属主体" min-width="110" show-overflow-tooltip />
          <el-table-column prop="rightType" label="识别权类" width="96" />
          <el-table-column label="敏感" width="60" align="center">
            <template #default="{ row }"><span v-if="row.sensitiveHit" class="prm-c-danger">敏感</span><span v-else>—</span></template>
          </el-table-column>
          <el-table-column label="AI 结论" min-width="110" show-overflow-tooltip>
            <template #default="{ row }">{{ row.conclusion || row.aiResult || row.suggestion || '—' }}</template>
          </el-table-column>
        </el-table>
        <el-alert v-if="aiSnap.ruleReport" :type="aiSnap.ruleReport.allPass ? 'success' : 'warning'" :closable="false" style="margin-bottom:8px">
          <div>规则完整性：{{ aiSnap.ruleReport.summary || (aiSnap.ruleReport.allPass ? '全部通过' : '存在缺失/不合规') }}</div>
          <div v-if="aiSnap.ruleReport.missing && aiSnap.ruleReport.missing.length" style="font-size:12px">缺失：{{ aiSnap.ruleReport.missing.join('、') }}</div>
        </el-alert>
        <el-descriptions v-if="aiSnap.consolidation" :column="3" border size="small">
          <el-descriptions-item label="命中规则">规则 {{ aiSnap.consolidation.rule }}</el-descriptions-item>
          <el-descriptions-item label="网公司持有权">{{ aiSnap.consolidation.holdRight }}</el-descriptions-item>
          <el-descriptions-item label="网公司经营权">{{ aiSnap.consolidation.operateRight }}</el-descriptions-item>
        </el-descriptions>
        <div style="font-size:12px;color:var(--prm-color-text-weak);margin-top:4px">
          校验时间 {{ fmt(aiSnap.checkedAt) }} · 元数据质量 {{ aiSnap.qualityScore ?? '—' }} · 提交时固化快照,供预审完整复核·可追溯
        </div>
        <!-- 快照完整性验真(防篡改):重算 SM3 比对上链存证 -->
        <div v-if="snapVerify" style="margin-top:6px">
          <span :class="'prm-c-' + ((snapVerify.verified ? 'success' : (snapVerify.payloadSm3 ? 'danger' : 'info')) || 'primary')">
            {{ snapVerify.verified ? '✔ 快照完整·未被篡改' : (snapVerify.payloadSm3 ? '✘ 快照与存证不一致·疑似篡改' : '无防篡改快照') }}
          </span>
          <span v-if="snapVerify.evidenceId" style="font-size:12px;color:var(--prm-color-text-weak);margin-left:8px">存证 {{ snapVerify.evidenceId.slice(0, 12) }}… · SM3 {{ (snapVerify.payloadSm3 || '').slice(0, 12) }}… · 留痕 {{ snapVerify.aiRunCount ?? 0 }} 次</span>
        </div>
      </div>
      <el-empty v-else :image-size="40" description="该申请无 AI 校验快照(旧数据 / 未经一键校验提交)" />

      <!-- §1 校验规则可视化:逐应交项的校验逻辑 + 规则明细 + AI 判定依据(透明可信) -->
      <div v-if="checkLogic && checkLogic.items && checkLogic.items.length" class="rv-h" style="margin-top:10px">
        校验规则可视化（校验逻辑 / 规则明细 / 判定依据）
      </div>
      <el-collapse v-if="checkLogic && checkLogic.items && checkLogic.items.length" accordion>
        <el-collapse-item v-for="(it, i) in checkLogic.items" :key="i">
          <template #title>
            <span style="margin-right:6px" class="prm-c-primary">{{ it.code }}</span>
            <span style="font-weight:600">{{ it.materialName }}</span>
            <span style="margin-left:8px" :class="'prm-c-' + ((it.materialPresent ? 'success' : 'warning') || 'primary')">{{ it.materialPresent ? '已交' : '待补' }}</span>
            <span v-if="it.aiVerdict" style="margin-left:6px" :class="'prm-c-' + ((it.aiVerdict === '通过' ? 'success' : (it.aiVerdict === '不通过' ? 'danger' : 'info')) || 'primary')">AI:{{ it.aiVerdict }}</span>
          </template>
          <div style="font-size:13px;line-height:1.8">
            <div><b>校验逻辑(触发规则):</b>{{ it.triggerLabel }}<span style="margin-left:6px" class="prm-c-primary">{{ it.required }}</span></div>
            <div><b>规则明细:</b>{{ it.ruleDetail || '—' }}</div>
            <div><b>判定依据(AI):</b>{{ it.aiIssues || (it.aiVerdict ? ('AI 结论:' + it.aiVerdict) : '该项尚无 AI 判定留痕(可在向导一键校验后提交)') }}</div>
          </div>
        </el-collapse-item>
      </el-collapse>
      <div v-if="checkLogic && checkLogic.summary" style="font-size:12px;color:var(--prm-color-text-weak);margin-top:4px">
        {{ checkLogic.summary }} · 模型 {{ checkLogic.aiModel }}
      </div>

      <!-- §2 AI 校验过程回放:全部大模型操作留痕时间线(可复盘·可审计) -->
      <div v-if="aiRunlog && aiRunlog.length" class="rv-h" style="margin-top:10px">
        AI 校验过程回放（{{ aiRunlog.length }} 次操作 · 留痕可审计）
      </div>
      <el-timeline v-if="aiRunlog && aiRunlog.length" style="padding:6px">
        <el-timeline-item v-for="(l, i) in aiRunlog" :key="i" :timestamp="fmt(l.createTime)" placement="top" type="primary">
          <div style="font-size:13px">
            <span style="margin-right:6px" class="prm-c-primary">{{ l.capability }}</span>
            <span style="color:var(--prm-color-text-secondary)">模型 {{ l.model }} · 耗时 {{ l.durationMs }}ms · 触发 {{ l.triggerUser }}</span>
            <div style="font-size:12px;color:var(--prm-color-text-weak)">输入:{{ l.inputSummary || '—' }}</div>
            <div style="font-size:12px;color:var(--prm-color-text-weak)">SM3 {{ (l.sm3Hash || '').slice(0, 16) }}…(输出防篡改指纹)</div>
          </div>
        </el-timeline-item>
      </el-timeline>

      <div class="rv-h">申请材料（{{ materials.length }}）</div>
      <el-table :data="materials" border size="small">
        <el-table-column prop="materialName" label="材料名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="checkResult" label="校验" width="80" align="center" />
        <el-table-column label="来源" width="92" align="center">
          <template #default="{ row }">
            <span :class="'prm-c-' + ((row.source === '平台同步' ? 'success' : 'info') || 'primary')">{{ row.source || '用户上传' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="原件" width="150">
          <template #default="{ row }">
            <!-- 平台同步且已落地原件字节(fileUrl)才可在线预览;否则纯文本标注,不给会 404 的入口 -->
            <el-link v-if="row.source === '平台同步' && row.fileUrl" type="success" @click="preview(row)" :title="row.fileName">平台原件·查看</el-link>
            <span v-else-if="row.source === '平台同步'" class="prm-c-success" :title="row.fileName">平台原件</span>
            <el-link v-else-if="row.fileName" type="primary" @click="preview(row)">查看</el-link>
            <span v-else style="color:var(--prm-color-text-disabled)">-</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- P3:逐表凭证(表2 来源凭证 B–F / 关联资料 G–J,逐表承载;单一真源 ConfirmTableItem) -->
      <div class="rv-h" v-if="reviewCreds.length">逐表凭证 · 表2（{{ reviewCreds.length }}）</div>
      <el-table v-if="reviewCreds.length" :data="reviewCreds" border size="small" row-key="key">
        <el-table-column label="数据表" min-width="150">
          <template #default="{ row }">
            <div style="font-weight:600">{{ row.schemaName }}.{{ row.tableCode }}</div>
            <div style="font-size:12px;color:var(--prm-color-text-weak)">{{ row.tableName }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="slot" label="凭证槽位" width="160" />
        <el-table-column label="附件" min-width="180">
          <template #default="{ row }">
            <el-link v-if="row.matId" type="primary" @click="previewMid(row)">{{ row.attachment }}</el-link>
            <span v-else-if="row.attachment" class="prm-c-success">{{ row.attachment }}</span>
            <span v-else style="color:var(--prm-color-text-disabled)">待补全</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="rv-h">进度轨迹</div>
      <el-timeline v-if="logs.length" style="padding:6px">
        <el-timeline-item v-for="l in logs" :key="l.logId" :timestamp="fmt(l.createTime)" placement="top"
          :type="l.toStatus === '已驳回' ? 'danger' : (l.toStatus === '已完成' ? 'success' : 'primary')">
          {{ l.nodeName }}：{{ l.fromStatus }} → {{ l.toStatus }} · 责任人 {{ l.responder }}
          <div v-if="l.opinion" style="font-size:12px;color:var(--prm-color-text-secondary)">意见：{{ l.opinion }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else :image-size="50" description="暂无数据" />

      <template #footer>
        <template v-if="canAct(cur.status)">
          <el-button type="success" @click="onApprove(cur, true)">审批通过</el-button>
          <el-button type="danger" @click="onReject(cur, true)">驳回</el-button>
        </template>
        <span v-else style="margin-right:8px" class="prm-c-info">本节点须「{{ needRoleLabel(cur.status) }}」处理</span>
        <el-button @click="drawer = false">关闭</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import PageNote from '@/components/PageNote.vue'
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageConfirmApply, approveConfirm, rejectConfirm, batchApproveConfirm, batchRejectConfirm, listMaterialByApply, getConfirmFlowLog, materialFileUrl, getAiCheckLogic, getAiRunlog, verifyAiSnapshot, listTableItems } from '@/api/confirm'
import { fetchChangeBaselineFull } from '@/api/assetCard'
import { openFilePreview } from '@/composables/useFilePreview'
import { currentRole } from '@/lib/roles'

// 逐节点角色门禁(与后端一致):每个节点仅对应角色(及 all/admin)可审批/驳回
const NODE_ROLE = { 人工预审中: 'precheck', 合规审核中: 'review', 主管复核中: 'manager', 经理终审中: 'director' }
const ROLE_LABEL = { precheck: '人工预审员', review: '合规管控小组', manager: '数字化部主管', director: '经理/高级经理' }
function canAct(status) {
  const r = currentRole()
  if (r === 'all' || r === 'admin') return true
  const need = NODE_ROLE[status]
  return !need || need === r
}
function needRoleLabel(status) { return ROLE_LABEL[NODE_ROLE[status]] || '' }

const rows = ref([]); const loading = ref(false); const sel = ref([])
const drawer = ref(false); const cur = ref({}); const materials = ref([]); const logs = ref([])
// P3:审核侧逐表凭证读——B–J 凭证已逐表化(ConfirmTableItem),审核详情投影展示(单一真源)
const reviewItems = ref([])
const reviewCreds = computed(() => {
  const rows = []
  const rel = [['gFlag', 'checkAttachment', 'checkMatId', 'G 行政监管'], ['hFlag', 'privacyAttachment', 'privacyMatId', 'H 个人隐私'],
    ['iFlag', 'busSecretAttachment', 'busSecretMatId', 'I 商密'], ['jFlag', 'equityAttachment', 'equityMatId', 'J 协议']]
  for (const t of reviewItems.value) {
    const sc = (t.sourceType || '').trim().charAt(0)
    if ('BCDEF'.includes(sc)) {
      rows.push({ key: t.tableCode + ':SRC', tableCode: t.tableCode, tableName: t.tableName, schemaName: t.schemaName, slot: '来源凭证 · ' + sc, attachment: t.sourceAttachment || '', matId: t.sourceMatId || '' })
    }
    for (const [flag, att, mid, label] of rel) {
      if (t[flag] === '是') {
        rows.push({ key: t.tableCode + ':' + label, tableCode: t.tableCode, tableName: t.tableName, schemaName: t.schemaName, slot: '关联资料 · ' + label, attachment: t[att] || '', matId: t[mid] || '' })
      }
    }
  }
  return rows
})
function previewMid(row) { if (row.matId) openFilePreview(materialFileUrl(row.matId), row.attachment) }
// 大模型校验机制完善:校验逻辑可视化 / 校验过程回放 / 快照防篡改验真
const checkLogic = ref(null); const aiRunlog = ref([]); const snapVerify = ref(null)
// ===== 与升级后申请/查询页同步:系统名称 / 三权多选 / 多触发 / 系统级变更对照 =====
// 一份确权申请 = 一个系统:系统名称权威源 = assetId「SYS:<系统名>」;assetName 仅旧单卡兜底
function sysName(row) {
  const id = (row && row.assetId) || ''
  return id.startsWith('SYS:') ? id.slice(4) : ((row && row.assetName) || '-')
}
// 权属类型=三权多选(持有/使用/经营)→ 标签;变更触发多触发拼接 → 标签;A–F/G–J 码列
const RIGHT_MAP = {
  持有权: { short: '持有权', type: 'primary' },
  使用权: { short: '使用权', type: 'success' },
  经营权: { short: '经营权', type: 'warning' }
}
function rightTags(rt) {
  return String(rt || '').split(/[、,，]/).map(s => s.trim()).filter(Boolean)
    .map(f => RIGHT_MAP[f] ? { ...RIGHT_MAP[f], full: f } : { short: f, type: 'info', full: f })
}
function triggerTags(row) { return String((row && row.changeTrigger) || '').split(/[、,，]/).map(s => s.trim()).filter(Boolean) }
function codeList(s) { return String(s || '').split(/[、,，]/).map(x => x.trim()).filter(Boolean) }

// 审批侧变更对照(P0):优先"申报时刻固化快照"(CEC_CHANGE_DIFF,审申同源);旧单回退实时计算(baseline-full 同源)
const changeBaseline = ref(null)
const isChangeApply = computed(() => cur.value && cur.value.registerType === '确权变更')
// 物化快照解析(容错:非法 JSON 视为无快照,走回退)
const diffSnap = computed(() => {
  const s = cur.value && cur.value.changeDiff
  if (!s) return null
  try { const o = JSON.parse(s); return o && o.v ? o : null } catch (e) { return null }
})
// 逐表改动扁平化(快照 tables → 行级渲染),字段码翻译
const PF_LABELS = {
  sourceType: '来源类型', sourceSubject: '来源主体', sourceDesc: '来源说明',
  gSubject: 'G监管说明', hSubject: 'H隐私说明', iSubject: 'I商密说明', jSubject: 'J协议说明',
  gFlag: 'G监管', hFlag: 'H隐私', iFlag: 'I商密', jFlag: 'J协议'
}
const snapTableChanges = computed(() => {
  if (!diffSnap.value || !Array.isArray(diffSnap.value.tables)) return []
  return diffSnap.value.tables.flatMap(t => (t.changes || []).map(c => ({
    tableName: t.tableName || t.tableCode, fieldLabel: PF_LABELS[c.field] || c.field,
    before: c.before, after: c.after
  })))
})
// 基线校核:验证申报所依据的基线(版本/上一版申请)是否仍是当前最新 —— 乐观锁的审核人视角
const rechecking = ref(false)
const recheckResult = ref(null)
async function recheckBaseline() {
  if (!diffSnap.value || !diffSnap.value.baseline) return
  rechecking.value = true
  recheckResult.value = null
  try {
    const full = await fetchChangeBaselineFull(sysName(cur.value), cur.value.assetId)
    const snap = diffSnap.value.baseline
    const curVer = full && full.base ? (full.base.version || 1) : null
    const curPrior = (full && full.priorApplyId) || ''
    const ok = curVer === (snap.version || 1) && (!snap.priorApplyId || snap.priorApplyId === curPrior)
    recheckResult.value = ok
      ? { ok: true, msg: `校核通过:申报基线(v${snap.version})仍是当前最新已确权版本,对照未漂移。` }
      : { ok: false, msg: `基线已漂移:申报时基于 v${snap.version},当前最新为 v${curVer ?? '未知'}。建议驳回本单,由申请人基于最新结论重新发起变更。` }
  } catch (e) {
    recheckResult.value = { ok: false, msg: '校核失败:无法获取当前基线,请稍后重试' }
  } finally { rechecking.value = false }
}
// 数据新增=insert(新表首次确权),不做基线 diff
const isDataAddApply = computed(() => isChangeApply.value && triggerTags(cur.value).includes('数据新增'))
// 权属类型分隔符可能为 ,/，/、:统一规范化比对,避免分隔符差异误报"已修改"
const canonTypes = (v) => String(v == null ? '' : v).split(/[、,，]/).map(s => s.trim()).filter(Boolean).join('、')
// 分簇门控(对齐申请页):勾选来源→比 A–F;勾选管理→比 G–J
const keepSrc = computed(() => triggerTags(cur.value).some(t => t.includes('来源')) || triggerTags(cur.value).includes('其他'))
const keepRel = computed(() => triggerTags(cur.value).some(t => t.includes('管理') || t.includes('监管')) || triggerTags(cur.value).includes('其他'))
// A–F/G–J 系统级"新值" = 基线该簇 ∪ 本次申报(只增不删,杜绝子集幻象删减)
function unionAfter(baseStr, curStr) {
  return [...new Set([...codeList(baseStr), ...codeList(curStr)])].sort().join('、')
}
// 系统级 7 维(对齐确权变更申请页):5 申报维 + 2 元素簇维(门控)
const CHANGE_DIMS = [
  { key: '权属主体', cur: () => cur.value.rightHolder, base: b => b.rightHolder },
  { key: '主体层级', cur: () => cur.value.subjectLevel, base: b => b.subjectLevel },
  { key: '权属类型', cur: () => canonTypes(cur.value.rightType), base: b => canonTypes(b.rightType) },
  { key: '责任部门', cur: () => cur.value.respDept, base: b => b.respDept },
  { key: '管制属性', cur: () => cur.value.regulated, base: b => b.regulated },
  { key: '来源识别(A–F)', cluster: 'src', cur: () => unionAfter(changeBaseline.value && changeBaseline.value.sourceIdent, cur.value.sourceIdentification), base: b => canonTypes(b.sourceIdent) },
  { key: '信息关联(G–J)', cluster: 'rel', cur: () => unionAfter(changeBaseline.value && changeBaseline.value.relationIdent, cur.value.relationIdentification), base: b => canonTypes(b.relationIdent) }
]
const activeDims = computed(() => CHANGE_DIMS.filter(d =>
  !d.cluster || (d.cluster === 'src' && keepSrc.value) || (d.cluster === 'rel' && keepRel.value)))
const rvNorm = (v) => (v == null ? '' : String(v).trim())
const changeDiff = computed(() => {
  if (!isChangeApply.value || !changeBaseline.value || isDataAddApply.value) return []
  const b = changeBaseline.value
  return activeDims.value
    .map(d => ({ key: d.key, before: rvNorm(d.base(b)), after: rvNorm(d.cur()) }))
    .filter(x => x.before !== x.after)
})
const reviewing = computed(() => rows.value.filter(r => ['人工预审中', '合规审核中', '主管复核中', '经理终审中'].includes(r.status)))
// 人工预审依据:解析提交时固化的 AI 校验快照(防篡改包:取 payload;兼容旧顶层格式)
const aiSnap = computed(() => {
  const s = cur.value && cur.value.aiSnapshot
  if (!s) return null
  try { const o = typeof s === 'string' ? JSON.parse(s) : s; return (o && o.payload) || o } catch (e) { return null }
})
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }

async function load() {
  loading.value = true
  try { const r = await pageConfirmApply({ current: 1, size: 100 }); rows.value = r.records || [] } finally { loading.value = false }
}
async function onApprove(row, fromDrawer) {
  // 合规审核节点录入「权益认定意见」(BA-03 节点50 合规小组核心产出),其余节点录入「审核意见」;可空则用规范默认
  const isCompliance = row.status === '合规审核中'
  const label = isCompliance ? '权益认定意见' : '审核意见'
  const { value } = await ElMessageBox.prompt(`请输入${label}(可空,默认按规范填充)`, label,
    { inputType: 'textarea', inputValue: isCompliance ? '权属认定符合三权分置要求,确权资料合规、权益识别完整' : '同意' }).catch(() => ({ value: undefined }))
  if (value === undefined) return // 取消
  const cardId = await approveConfirm(row.applyId, value || '')
  ElMessage.success(cardId ? '终审通过,已生成权益卡片' : (isCompliance ? '合规审核通过,已生成表3/表4及认定意见' : '审批通过,进入下一环节'))
  if (fromDrawer) drawer.value = false
  load()
}
function onReject(row, fromDrawer) {
  ElMessageBox.prompt('请输入驳回原因', '驳回', {})
    .then(async ({ value }) => { await rejectConfirm(row.applyId, value); ElMessage.success('已驳回'); if (fromDrawer) drawer.value = false; load() }).catch(() => {})
}
async function onBatchApprove() {
  const r = await batchApproveConfirm(sel.value.map(x => x.applyId))
  ElMessage[r.failed ? 'warning' : 'success'](`批量通过:成功 ${r.success}/${r.total}${r.failed ? '，失败 ' + r.failed : ''}`)
  load()
}
function onBatchReject() {
  ElMessageBox.prompt('请输入统一驳回原因', '批量驳回', {})
    .then(async ({ value }) => {
      const r = await batchRejectConfirm(sel.value.map(x => x.applyId), value)
      ElMessage[r.failed ? 'warning' : 'success'](`批量驳回:成功 ${r.success}/${r.total}${r.failed ? '，失败 ' + r.failed : ''}`)
      load()
    }).catch(() => {})
}
async function onDetail(row) {
  cur.value = row
  checkLogic.value = null; aiRunlog.value = []; snapVerify.value = null; changeBaseline.value = null; recheckResult.value = null
  const [m, l, ti] = await Promise.all([listMaterialByApply(row.applyId), getConfirmFlowLog(row.applyId), listTableItems(row.applyId)])
  materials.value = m || []; logs.value = l || []; reviewItems.value = ti || []
  drawer.value = true
  // 确权变更(非数据新增)且无固化快照(旧单):回退实时对照,基线用 baseline-full(真实上一版,与申报页同源)
  if (row.registerType === '确权变更' && !triggerTags(row).includes('数据新增') && !row.changeDiff) {
    fetchChangeBaselineFull(sysName(row), row.assetId)
      .then(full => { changeBaseline.value = (full && full.base) || null })
      .catch(() => { changeBaseline.value = null })
  }
  // 大模型校验机制(规则可视化 / 回放 / 快照验真):best-effort,任一失败不影响详情
  getAiCheckLogic(row.applyId).then(r => { checkLogic.value = r }).catch(() => {})
  getAiRunlog(row.applyId).then(r => { aiRunlog.value = r || [] }).catch(() => {})
  verifyAiSnapshot(row.applyId).then(r => { snapVerify.value = r }).catch(() => {})
}
function preview(row) { if (row.materialId) openFilePreview(materialFileUrl(row.materialId), row.fileName) }
// 从向导"去审核"带 applyId 跳入时高亮目标单
import { useRoute } from 'vue-router'
const route = useRoute()
function rowHl({ row }) { return route.query.applyId && row.applyId === route.query.applyId ? 'hl-row' : '' }

onMounted(load)
</script>

<style scoped>
.rv-h { font-weight: 600; margin: 20px 0 8px; }
:deep(.hl-row) { background: var(--prm-color-selected-bg, #eff7ff) !important; outline: 1px solid var(--prm-color-primary, #1e87f0); }
</style>
