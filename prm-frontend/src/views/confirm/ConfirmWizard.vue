<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-table-note" style="margin:0 0 12px">
      注:一站式确权申请——填申请 → 传材料 → 校验 → 提交，一条流程办完，无需在菜单间来回切换(对齐附录F"资料随申请一并编制提交")。
    </div>

    <el-steps :active="step" finish-status="success" align-center class="wz-steps">
      <el-step title="填写申请" description="表1 + A–J 来源识别" />
      <el-step title="上传材料" description="按 A–J 应交清单" />
      <el-step title="材料校验" description="完整性/合规校验" />
      <el-step title="提交审核" description="进入三级审批" />
    </el-steps>

    <div class="wz-body">
      <!-- 步骤1:填写申请 -->
      <el-card v-show="step === 0" shadow="never">
        <el-alert v-if="!applyId" type="info" :closable="false" style="margin-bottom:12px;max-width:640px">
          <template #title>
            第一次填写?可
            <el-button link type="primary" style="vertical-align:baseline" @click="fillDemo">一键填充示例(AST-001,测试/演示用)</el-button>
            体验完整流程,材料包见 test/确权申请 目录
          </template>
        </el-alert>
        <div class="step1-2col">
          <div class="step1-tree">
            <ConfirmCatalogTree :status="changeMode ? '' : 'unconfirmed'" @select="onTreeSelect" />
          </div>
          <div class="step1-form">
        <!--
          ===== 字段 → 35号文规范 对照表(防退化:任何新增字段须能对位下表,否则不加)=====
          规范唯一真源:附录C 表1(系统级,8列)/ 表2(逐表,12列);审核侧延伸表3(逐表)/表4(逐表×权益)。
          【表1 系统级·一份申请一套】
            确权范围(系统名)      → 表1 系统名称(从元数据·唯一ID)   来源:平台·左树   只读
            登记类型(初始/变更)    → 表1 登记类型(二选一)            来源:菜单派生   只读
            申报权属主体          → 表1 公司主体                    来源:平台MGT_UNIT 可改
            主体层级(总部/分省/子) → 表1 公司主体口径                来源:SYS_ORG.ORG_TYPE 可改
            系统负责人/联系方式     → 表1 系统负责人/联系方式          来源:平台MGT_USER/PHONE 可改
            来源权益识别 A–F       → 表1 数据来源权益识别(并集)       来源:表2 派生   只读
            信息关联识别 G–J       → 表1 信息关联权益识别(并集)       来源:表2 派生   只读
          【表2 逐表·每张争议表一行(库表清单+编辑表2抽屉)】
            模式/数据表/来源类型/来源主体/限制摘要/凭证/关联类型/关联主体/关联附件/权益风险
                                  → 表2 各列                       来源:平台AU_TABLE_META_DATA 预填·可改
          【PRM 内部·非表1(均有 load-bearing 理由,经标注)】
            权属类型主张          → 经营权归集输入(hasOperateClaim);最终以表4 审核为准
            管制属性             → 经营权归集输入(regulated)→ 5规则 → 经营权/权益卡片/表4
            申请模式(常规/一事一议) → 审批流程选择
            责任部门             → 平台补充上下文(MGT_MNG_DEPT),非表1
          【已删(单卡残肢/孤儿/违规):关联卡片搜索·元数据自动填充·资产名称·用途说明·表2批量套用·库表手工批量导入·登记自动判定override】
        -->
        <el-form ref="formRef" :model="form" :rules="rules" label-width="150px" :disabled="!!applyId">
          <el-form-item label="确权范围">
            <div v-if="form.systemName" style="width:100%">
              <el-tag type="primary" effect="dark" size="large" style="margin-right:8px">{{ form.systemName }}</el-tag>
              <el-tag v-if="tableItems.length" type="success" effect="plain" style="margin-right:8px">已选 {{ tableItems.length }} 张库表</el-tag>
              <el-tag :type="changeMode ? 'warning' : 'info'" effect="plain">{{ changeMode ? '确权变更' : '初始确权' }}</el-tag>
              <div class="form-tip">范围由左侧范围树(选系统 → 一级功能模块 → 库表)带入;系统名/库表来自平台卡片元数据,只读。查找请用范围树顶部搜索框。</div>
            </div>
            <el-empty v-else :image-size="48" description="请在左侧「确权范围」树勾选:系统 → 一级功能模块 → 库表(数据资产卡片);一份申请限定在一个系统内" />
            <el-tag v-if="quality !== null" :type="quality < 80 ? 'danger' : 'success'" effect="plain" style="margin-top:6px">
              元数据质量评分 {{ quality }}{{ quality < 80 ? ' · 低于80,提交将被自动驳回(请先治理元数据)' : '' }}
            </el-tag>
          </el-form-item>
          <!-- A区·定性:登记类型由「进入的菜单」唯一确定(初始确权/确权变更两个独立菜单),并由左树 status 过滤强制保证,只读 -->
          <el-form-item label="登记类型">
            <el-alert v-if="changeMode" type="info" :closable="false" show-icon
              title="本次:确权变更 · 对已确权库表既有结论的修订(附录F §3.3.2 重新确权)" />
            <el-alert v-else type="success" :closable="false" show-icon
              title="本次:初始确权 · 左侧范围树仅列未确权库表(若需修订已确权,请走「确权变更申请」菜单)" />
          </el-form-item>
          <!-- 确权变更差异化(附录F §3.3.2):变更类型 = 由所选库表确权状态派生约束 + 簇内多选(数据新增 insert / 来源·管理·到期 update) -->
          <el-form-item v-if="form.registerType === '确权变更'" required>
            <template #label><span style="color:var(--prm-color-danger)">*</span> 变更触发类型</template>
            <div style="width:100%">
              <!-- 混选拦截:新增表(insert)与已确权表(update)编辑模式冲突,不可同单 -->
              <el-alert v-if="mixedSelection" type="error" :closable="false" show-icon
                title="所选库表混含「新增(未确权)」与「已确权」—— 二者编辑模式不同(新增=首次登记 insert / 已确权=修订基线 update),不能在同一份变更申请处理。请只勾选其一:新增表单独走「数据新增」,已确权表的修订另起一单。" />
              <!-- 数据新增:全为新增表 → 锁定,只读(insert) -->
              <el-alert v-else-if="isDataAdd" type="warning" :closable="false" show-icon
                title="变更类型:数据新增(所选均为未确权新表,自动锁定) —— 新表首次走完整确权登记,既有已确权库表不改动,不联动下游授权。" />
              <!-- 已确权表:来源/管理/到期 簇内多选(update,可同时命中多簇) -->
              <template v-else-if="allConfirmedSel">
                <el-checkbox-group v-model="changeTriggers">
                  <el-checkbox v-for="o in CHANGE_TRIGGER_OPTS" :key="o.v" :value="o.v" border style="margin:0 8px 6px 0">
                    {{ o.v }}<span style="color:var(--prm-color-text-weak);font-size:12px;margin-left:4px">{{ o.t }}</span>
                  </el-checkbox>
                </el-checkbox-group>
                <div class="form-tip">对已确权库表的修订可<b>同时命中多个元素簇</b>(如来源凭证更新 + 合规要求升级);勾哪簇即只比对/收敛哪簇,未勾簇维持原值。</div>
              </template>
              <el-empty v-else :image-size="36" description="请先在左侧范围树勾选库表 —— 变更类型据所选库表确权状态自动判定(新增表→数据新增;已确权表→来源/管理/到期可多选)" />
            </div>
          </el-form-item>
          <!-- 数据新增(insert):无 baseline diff(既有不动),提示新表首次走整套 -->
          <el-alert v-if="form.registerType === '确权变更' && isDataAdd" type="success" :closable="false" show-icon style="margin-bottom:18px"
            :title="`数据新增 · 新表首次确权登记(共 ${tableItems.length} 张新表)`">
            <div style="font-size:12px;color:var(--prm-color-text-secondary);margin-top:4px">
              新表将首次走完整识别(A–F 来源 / G–J 关联 / 表2 / 表4 权益 / 生成新权益卡片);系统内既有已确权库表全部不动;新表尚未授权,不联动下游授权。
            </div>
          </el-alert>
          <!-- P0 变更前后对照(仅 update 模式):落在编辑区之前,先给基线语境(一眼看见改了什么/没改什么) -->
          <div v-if="form.registerType === '确权变更' && changeBaseline && !isDataAdd" style="margin-bottom:18px">
            <div style="font-size:12px;color:var(--prm-color-text-weak);margin-bottom:4px">
              基线:<b style="color:var(--prm-color-text-secondary)">{{ changeBaseline.sysName }} · 确权时间 {{ changeBaseline.authTime }} · v{{ changeBaseline.version }}</b>
              <el-tag size="small" type="primary" effect="plain" style="margin-left:8px">本次变更生成 v{{ (changeBaseline.version || 1) + 1 }}(可追溯·旧权益卡片转历史)</el-tag>
              <el-tag size="small" :type="baselineFromReal ? 'success' : 'info'" effect="plain" style="margin-left:6px">
                {{ baselineFromReal ? '底版=上一版真实确权(表3行+权益卡片)' : '底版=目录合成(无上一版确权,退回身份级)' }}
              </el-tag>
            </div>
            <el-alert :type="changeDiff.length ? 'warning' : 'info'" :closable="false" :title="changeSummary" />
            <el-table v-if="changeDiff.length" :data="changeDiff" border size="small" style="margin-top:8px;max-width:840px">
              <el-table-column prop="key" label="变更维度" width="150" />
              <el-table-column label="原值(已有确权)" min-width="180">
                <template #default="{ row }"><span style="color:var(--prm-color-text-weak);text-decoration:line-through">{{ row.before || '空' }}</span></template>
              </el-table-column>
              <el-table-column label="新值(本次变更)" min-width="180">
                <template #default="{ row }"><span style="color:#ffc417;font-weight:600">{{ row.after || '空' }}</span></template>
              </el-table-column>
            </el-table>
            <div style="font-size:12px;color:#36b21d;margin-top:4px">另有 {{ changeUnchanged }} 项维持原值(无需改动)。</div>
            <div v-if="changedTableRows.length" style="font-size:12px;color:var(--prm-color-warning);margin-top:4px">
              另有 {{ changedTableRows.length }} 张库表的来源/关联明细本次调整(逐表前后留痕见下方库表清单「编辑表2」,系统级不再做并集对照以免误报)。
            </div>
            <!-- 表4/确权边界底版:上一版权益卡片(只读,变更须 ⊆ 此边界;新增权益经审核扩边界) -->
            <div v-if="baselineCards.length" style="margin-top:10px">
              <div style="font-size:12px;color:var(--prm-color-text-weak);margin-bottom:4px">上一版权益卡片(表4·确权边界底版,只读):</div>
              <el-table :data="baselineCards" border size="small" style="max-width:840px">
                <el-table-column prop="rightType" label="权益类型" width="150" />
                <el-table-column prop="scope" label="确权范围(边界)" min-width="140">
                  <template #default="{ row }">{{ row.scope || '全字段' }}</template>
                </el-table-column>
                <el-table-column label="有效期" min-width="120">
                  <template #default="{ row }">{{ row.validDate ? String(row.validDate).slice(0,10) : '无固定期限' }}</template>
                </el-table-column>
                <el-table-column prop="cardNo" label="权益卡片号" min-width="130" show-overflow-tooltip />
                <el-table-column prop="cardStatus" label="状态" width="80" align="center">
                  <template #default="{ row }"><el-tag size="small" type="success" effect="plain">{{ row.cardStatus || '正常' }}</el-tag></template>
                </el-table-column>
              </el-table>
            </div>
            <div v-if="baselineSummaries.length" style="font-size:12px;color:var(--prm-color-text-weak);margin-top:6px">
              上一版认定意见:
              <span v-for="(s, i) in baselineSummaries" :key="i" style="margin-right:10px">「{{ s.summaryType }}」{{ s.content }}</span>
            </div>
          </div>
          <!-- P1 授权影响检查(逐表精确·合规红线):本次选中的库表里有已授权者→精确列出受影响授权 + 按触发动因给处置建议;须确认方可提交 -->
          <el-alert v-if="changeMode && authImpact && authImpact.hasActive" type="error" :closable="false" show-icon style="margin-bottom:18px"
            :title="`授权影响:本次变更涉及 ${authImpact.items.length} 张已对外授权的库表,可能影响其有效性,须联动评估处置`">
            <div v-for="it in authImpact.items" :key="it.authId" style="font-size:12px;color:var(--prm-color-text-secondary);margin-top:4px">
              · <b>{{ it.tableName }}</b> → {{ it.scope }}(状态:{{ it.authStatus }})—— 处置建议:<b style="color:var(--prm-color-danger)">{{ it.suggestion }}</b>
            </div>
            <el-checkbox v-model="ackAuthImpact" style="margin-top:8px">我已知悉上述受影响授权,并将按建议在授权侧评估暂停/续签/终止</el-checkbox>
          </el-alert>
          <!-- 系统责任信息(表1 系统负责人/联系方式):平台 TW_DATA_CARD 带出、可改;系统名只读。资产名称=系统名,后台保留不再单列 -->
          <el-collapse v-model="identityPanel" class="identity-card">
            <el-collapse-item name="id">
              <template #title>
                <span style="font-size:13px;color:var(--prm-color-text-secondary)"><b style="color:var(--prm-color-text)">系统责任信息</b>　{{ identitySummary }}</span>
              </template>
              <el-form-item label="所属业务系统">
                <el-input v-model="form.systemName" readonly placeholder="由左侧范围树带入(表1 系统名称)">
                  <template #suffix><span v-if="form.systemName" style="color:#36b21d;font-size:12px">平台·只读</span></template>
                </el-input>
              </el-form-item>
              <el-form-item label="系统负责人" prop="systemOwner">
                <el-input v-model="form.systemOwner" placeholder="平台带出·可改(MGT_USER 卡片责任人)">
                  <template #suffix><span v-if="form.systemOwner" style="color:#36b21d;font-size:12px">平台带出</span></template>
                </el-input>
              </el-form-item>
              <el-form-item label="联系方式" prop="contactInfo">
                <el-input v-model="form.contactInfo" placeholder="平台带出·可改(MGT_USER_PHONE 责任人电话)">
                  <template #suffix><span v-if="form.contactInfo" style="color:#36b21d;font-size:12px">平台带出</span></template>
                </el-input>
              </el-form-item>
              <el-form-item label="责任部门">
                <el-input v-model="form.respDept" placeholder="平台带出·可改(MGT_MNG_DEPT 管理部门;平台补充·非表1)">
                  <template #suffix><span v-if="form.respDept" style="color:#36b21d;font-size:12px">平台带出</span></template>
                </el-input>
                <div v-if="dimMarks['责任部门']" class="form-tip" style="margin-top:2px">
                  <el-tag v-if="dimMarks['责任部门'].changed" size="small" type="warning" effect="plain">已修改 · 原值「{{ dimMarks['责任部门'].before || '空' }}」</el-tag>
                  <el-tag v-else size="small" type="info" effect="plain">维持原值</el-tag>
                </div>
              </el-form-item>
            </el-collapse-item>
          </el-collapse>
          <el-form-item label="申报权属主体" prop="rightHolder">
            <el-input v-model="form.rightHolder" placeholder="平台带出·可改(MGT_UNIT 管理单位 = 表1 公司主体);最终权属以审核为准">
              <template #suffix><span v-if="form.rightHolder" style="color:#36b21d;font-size:12px">平台带出</span></template>
            </el-input>
            <div class="form-tip">分省上报的数据产权,确权通过后统一归口中国南方电网有限责任公司</div>
            <div v-if="dimMarks['权属主体']" class="form-tip" style="margin-top:2px">
              <el-tag v-if="dimMarks['权属主体'].changed" size="small" type="warning" effect="plain">已修改 · 原值「{{ dimMarks['权属主体'].before || '空' }}」</el-tag>
              <el-tag v-else size="small" type="info" effect="plain">维持原值</el-tag>
            </div>
          </el-form-item>
          <el-form-item label="主体层级" prop="subjectLevel">
            <el-select v-model="form.subjectLevel" style="width:100%" clearable placeholder="平台带出·可改(SYS_ORGANIZATION 单位层级)">
              <el-option label="公司总部" value="公司总部" />
              <el-option label="分省公司" value="分省公司" />
              <el-option label="专业子公司" value="专业子公司" />
            </el-select>
            <div class="form-tip">35 号文表1 公司主体口径:公司总部 / 分省公司 / 专业子公司(取自组织结构 ORG_TYPE,可改)</div>
          </el-form-item>
          <el-divider content-position="left" style="font-size:12px;color:var(--prm-color-text-weak)">
            经营权归集判定
            <el-tooltip placement="top" content="非表1 采集项;以下「权属类型主张 + 管制属性」共同决定网公司经营权,结果直接进权益卡片与表4。规则依据《数据权益内部管理汇总表》说明页。">
              <el-icon style="margin-left:4px;vertical-align:-2px;color:var(--prm-color-text-disabled)"><QuestionFilled /></el-icon>
            </el-tooltip>
          </el-divider>
          <el-form-item label="权属类型主张" prop="rightTypes">
            <el-select v-model="form.rightTypes" multiple style="width:100%" placeholder="申报人主张的权属类型(可多选)">
              <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
            </el-select>
            <div class="form-tip">申报人主张持有/使用/经营权(可多选,合并一份申请);<b style="color:var(--prm-color-link)">是否含「经营权」直接影响下方归集判定</b>;最终三权以确权审核(表4)结果为准。</div>
            <div v-if="dimMarks['权属类型']" class="form-tip" style="margin-top:2px">
              <el-tag v-if="dimMarks['权属类型'].changed" size="small" type="warning" effect="plain">已修改 · 原值「{{ dimMarks['权属类型'].before || '空' }}」</el-tag>
              <el-tag v-else size="small" type="info" effect="plain">维持原值</el-tag>
            </div>
          </el-form-item>
          <el-form-item label="管制属性">
            <el-radio-group v-model="form.regulated">
              <el-radio value="管制业务">管制业务</el-radio>
              <el-radio value="非管制">非管制</el-radio>
            </el-radio-group>
            <div class="form-tip">已按业务域预选(输配电/调度→管制业务,营销/办公等→非管制),可改</div>
            <div v-if="consolPreview" class="consol-card" :class="opClass(consolPreview.operateRight)">
              <div class="consol-hero">
                网公司经营权
                <el-tag :type="opTag(consolPreview.operateRight)" effect="dark" size="large" style="margin-left:8px;font-size:14px">{{ consolPreview.operateRight }}</el-tag>
              </div>
              <div class="consol-plain">{{ consolPlain }}</div>
              <div class="consol-sub">持有权 {{ consolPreview.holdRight }} · 使用权 {{ consolPreview.useRight }} · 命中规则 {{ consolPreview.rule }}</div>
              <el-collapse class="consol-basis">
                <el-collapse-item>
                  <template #title><span style="font-size:12px;color:var(--prm-color-text-weak)">查看判定依据</span></template>
                  <div style="font-size:12px;color:var(--prm-color-text-secondary);line-height:1.7">{{ consolPreview.reason }}</div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </el-form-item>
          <el-form-item label="申请模式">
            <el-radio-group v-model="form.applyMode">
              <el-radio value="常规">常规</el-radio>
              <el-radio value="一事一议">一事一议(特殊事项单独审议)</el-radio>
            </el-radio-group>
            <div class="form-tip">权属复杂/跨主体/全网统一转让等特殊场景选择"一事一议",由合规管控小组单独组织审议</div>
          </el-form-item>
          <!-- R3 渐进披露:确权变更按触发聚焦相关识别维度,其余折叠为"维持原值"(只折叠不隐藏,与后端材料收敛同源) -->
          <el-alert v-if="changeNarrowing" type="info" :closable="false" style="margin-bottom:12px"
            :title="`已按变更触发「${form.changeTrigger}」聚焦相关识别维度,其余维持原值已折叠;如确有变动请展开修改`" />
          <el-form-item label="来源权益识别">
            <div v-if="form.sourceIdent.length" style="display:flex;flex-wrap:wrap;gap:6px">
              <el-tag v-for="s in sourceOpts.filter(o => form.sourceIdent.includes(o.v))" :key="s.v" type="primary" effect="plain">{{ s.v }} {{ s.t }}</el-tag>
            </div>
            <span v-else style="color:var(--prm-color-text-disabled)">—</span>
            <div class="form-tip">表1 系统级并集 = 库表清单各表「来源判定」的并集(随表2 自动重算·只读);如需调整请到对应库表「编辑表2」改其来源类型。</div>
          </el-form-item>
          <el-form-item label="信息关联识别">
            <div v-if="form.relationIdent.length" style="display:flex;flex-wrap:wrap;gap:6px">
              <el-tag v-for="r in relationOpts.filter(o => form.relationIdent.includes(o.v))" :key="r.v" :type="r.v === 'H' ? 'danger' : 'warning'" effect="plain">{{ r.v }} {{ r.t }}</el-tag>
            </div>
            <span v-else style="color:var(--prm-color-text-disabled)">—(本系统库表均不涉 G/H/I/J)</span>
            <div class="form-tip">表1 系统级并集 = 各库表 G/H/I/J 的并集(随表2 自动重算·只读);如需调整请到对应库表「编辑表2」勾选。</div>
          </el-form-item>
          <el-divider content-position="left" style="font-size:12px;color:var(--prm-color-text-weak)">
            库表清单 · 逐表确权(含表2 第三方权益)
            <el-tooltip placement="top" content="确权粒度到库表,每张库表一行。涉第三方/敏感(来源 B–F 或 G/H/I/J)的库表需逐表填「表2」;对齐 35 号文附录C 表2 / 附录F 表3。">
              <el-icon style="margin-left:4px;vertical-align:-2px;color:var(--prm-color-text-disabled)"><QuestionFilled /></el-icon>
            </el-tooltip>
          </el-divider>
          <el-form-item label="库表清单">
            <div style="width:100%">
              <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
                <span v-if="tableItems.length" class="form-tip">共 {{ tableItems.length }} 张库表<template v-if="disputedRows.length"> · {{ disputedRows.length }} 张涉第三方/敏感需填表2 </template></span>
                <el-tag v-if="disputedRows.length" :type="pendingT2Rows.length ? 'warning' : 'success'" size="small" effect="plain">
                  {{ pendingT2Rows.length ? `表2 待填 ${pendingT2Rows.length} 张` : '表2 已全部填写' }}
                </el-tag>
              </div>
              <div class="form-tip">库表来自数据资产管理平台·由左侧范围树带入,<b>不可手工添加</b>(增删请在左树勾选/取消)。实例/schema/表代码/表名/密级平台只读;来源类型/来源主体/G–J/表2 可点行尾「编辑表2」逐表调整。</div>
            </div>
          </el-form-item>
          <el-empty v-if="!tableItems.length" description="尚无库表 — 请在左侧「确权范围」树勾选:系统 → 一级功能模块 → 库表" :image-size="60" />
          <el-table v-else :data="tableItems" border size="small" style="margin-bottom:8px">
            <el-table-column label="库表(schema.表代码)" min-width="200">
              <template #default="{ row }">
                <div style="font-weight:600;color:var(--prm-color-text)">{{ row.schemaName }}.{{ row.tableCode }}</div>
                <div style="font-size:12px;color:var(--prm-color-text-weak)">{{ row.tableName }}<span v-if="row.instanceName" style="margin-left:6px">· {{ row.instanceName }}</span></div>
              </template>
            </el-table-column>
            <el-table-column prop="secretLevel" label="密级" width="84" />
            <el-table-column v-if="changeMode" label="对外授权" width="96" align="center">
              <template #default="{ row }">
                <el-tooltip v-if="row.authorized" placement="top" :content="`${row.authId} · ${row.authScope} · ${row.authStatus};本次变更将联动评估该授权`">
                  <el-tag type="danger" size="small" effect="dark">已授权</el-tag>
                </el-tooltip>
                <span v-else style="color:var(--prm-color-text-disabled)">无</span>
              </template>
            </el-table-column>
            <el-table-column prop="sourceType" label="来源类型" width="118" />
            <el-table-column prop="sourceSubject" label="来源主体" min-width="100" show-overflow-tooltip />
            <el-table-column label="敏感标记 G–J" width="120" align="center">
              <template #default="{ row }">
                <template v-for="r in relationOpts" :key="r.v">
                  <el-tag v-if="row[r.v.toLowerCase() + 'Flag'] === '是'" size="small" :type="r.v === 'H' ? 'danger' : 'warning'" effect="plain" style="margin:1px">{{ r.v }}</el-tag>
                </template>
                <span v-if="!(row.gFlag === '是' || row.hFlag === '是' || row.iFlag === '是' || row.jFlag === '是')" style="color:var(--prm-color-text-disabled)">—</span>
              </template>
            </el-table-column>
            <el-table-column label="表2状态" width="84" align="center">
              <template #default="{ row }">
                <el-tag :type="rowTable2Status(row).type" size="small" effect="plain">{{ rowTable2Status(row).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="录入" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.sourceChannel === 'MANUAL' ? 'info' : 'success'" size="small" effect="plain">
                  {{ row.sourceChannel === 'MANUAL' ? '手工' : '平台' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="118" align="center" fixed="right">
              <template #default="{ row }">
                <el-button v-if="rowDisputed(row)" link type="primary" size="small" @click="openT2(row)">编辑表2</el-button>
                <el-button link type="danger" size="small" @click="removeTableItem(row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 表2 逐表编辑抽屉:该表的来源/各关联主体/权益风险 -->
          <el-drawer v-model="t2Drawer.visible" size="460px" append-to-body
            :title="`表2 第三方权益 · ${t2Drawer.row ? (t2Drawer.row.tableName || t2Drawer.row.tableCode) : ''}`">
            <div v-if="t2Drawer.row" style="padding:0 6px">
              <el-alert :closable="false" type="info" show-icon style="margin-bottom:10px"
                title="实例/schema/表代码/表名/密级/确权时间来自平台、只读;来源与各关联主体为平台元数据预填,请核实修正,改动会留痕。" />
              <el-form label-position="top">
                <!-- 锁定字段:平台为唯一真源,只读 -->
                <el-form-item label="数据表(平台·只读)">
                  <span style="color:var(--prm-color-text-secondary)">{{ t2Drawer.row.instanceName }} / {{ t2Drawer.row.schemaName }} / {{ t2Drawer.row.tableCode }}（{{ t2Drawer.row.tableName }}）</span>
                  <el-tag size="small" type="info" effect="plain" style="margin-left:8px">密级 {{ t2Drawer.row.secretLevel }}</el-tag>
                  <el-tag v-if="t2Drawer.row.authTime" size="small" type="success" effect="plain" style="margin-left:6px">确权时间 {{ t2Drawer.row.authTime }}</el-tag>
                </el-form-item>
                <el-form-item label="来源类型">
                  <el-select v-model="t2Drawer.row.sourceType" style="width:100%">
                    <el-option v-for="s in sourceOpts" :key="s.v" :label="`${s.v} ${s.t}`" :value="`${s.v} ${s.t}`" />
                  </el-select>
                  <el-tag v-if="pfMark(t2Drawer.row,'sourceType').state==='changed'" size="small" type="warning" effect="plain" class="pf-mark">已修改 · 平台原值「{{ pfMark(t2Drawer.row,'sourceType').before || '空' }}」</el-tag>
                  <el-tag v-else-if="pfMark(t2Drawer.row,'sourceType').state==='prefilled'" size="small" type="success" effect="plain" class="pf-mark">平台预填</el-tag>
                </el-form-item>
                <el-form-item label="来源主体名称" :required="'BCDEF'.includes((t2Drawer.row.sourceType || '').charAt(0))">
                  <el-input v-model="t2Drawer.row.sourceSubject" placeholder="B–F 来源须填(A 自行生产可空)" />
                  <el-tag v-if="pfMark(t2Drawer.row,'sourceSubject').state==='changed'" size="small" type="warning" effect="plain" class="pf-mark">已修改 · 平台原值「{{ pfMark(t2Drawer.row,'sourceSubject').before || '空' }}」</el-tag>
                  <el-tag v-else-if="pfMark(t2Drawer.row,'sourceSubject').state==='prefilled'" size="small" type="success" effect="plain" class="pf-mark">平台预填</el-tag>
                </el-form-item>
                <el-form-item label="来源权益限制摘要">
                  <el-input v-model="t2Drawer.row.sourceDesc" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="该表来源的权益限制/使用约束" />
                  <el-tag v-if="pfMark(t2Drawer.row,'sourceDesc').state==='changed'" size="small" type="warning" effect="plain" class="pf-mark">已修改 · 平台原值「{{ pfMark(t2Drawer.row,'sourceDesc').before || '空' }}」</el-tag>
                  <el-tag v-else-if="pfMark(t2Drawer.row,'sourceDesc').state==='prefilled'" size="small" type="success" effect="plain" class="pf-mark">平台预填</el-tag>
                </el-form-item>
                <el-form-item v-if="t2Drawer.row.sourceAttachment" label="来源凭证附件">
                  <el-tag size="small" type="info" effect="plain">{{ t2Drawer.row.sourceAttachment }}</el-tag>
                  <span class="form-tip" style="margin-left:6px">平台已上传·只读;正本见「上传材料」步骤</span>
                </el-form-item>
                <el-divider content-position="left" style="font-size:12px;color:var(--prm-color-text-weak)">信息关联类型(G–J)·逐表勾选并填关联主体</el-divider>
                <div v-for="r in relationOpts" :key="r.v" style="margin-bottom:10px">
                  <el-checkbox :model-value="t2Drawer.row[r.v.toLowerCase() + 'Flag'] === '是'"
                    @change="v => (t2Drawer.row[r.v.toLowerCase() + 'Flag'] = v ? '是' : '否')">{{ r.v }} {{ r.t }}</el-checkbox>
                  <template v-if="t2Drawer.row[r.v.toLowerCase() + 'Flag'] === '是'">
                    <el-input v-model="t2Drawer.row[r.v.toLowerCase() + 'Subject']"
                      size="small" style="margin-top:4px" :placeholder="`${r.v} 信息识别关联主体说明${(r.v === 'H' || r.v === 'J') ? '(必填)' : ''}`" />
                    <el-tag v-if="pfMark(t2Drawer.row, r.v.toLowerCase()+'Subject').state==='changed'" size="small" type="warning" effect="plain" class="pf-mark">已修改 · 平台原值「{{ pfMark(t2Drawer.row, r.v.toLowerCase()+'Subject').before || '空' }}」</el-tag>
                    <el-tag v-else-if="pfMark(t2Drawer.row, r.v.toLowerCase()+'Subject').state==='prefilled'" size="small" type="success" effect="plain" class="pf-mark">平台预填</el-tag>
                    <div v-if="t2Drawer.row[relAttachKey(r.v)]" style="margin-top:3px;font-size:12px;color:var(--prm-color-text-weak)">
                      关联资料附件:<el-tag size="small" type="info" effect="plain">{{ t2Drawer.row[relAttachKey(r.v)] }}</el-tag> 平台已上传·只读;正本见「上传材料」
                    </div>
                  </template>
                </div>
                <el-form-item label="权益风险(平台无源·申报人评估)">
                  <el-input v-model="t2Drawer.row.riskDesc" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="是否存在未清晰约定的潜在侵权风险(可选)" />
                </el-form-item>
              </el-form>
              <div style="text-align:right;margin-top:6px">
                <el-tag :type="rowTable2Status(t2Drawer.row).type" effect="plain" style="margin-right:8px">{{ rowTable2Status(t2Drawer.row).text }}</el-tag>
                <el-button type="primary" @click="t2Drawer.visible = false">完成</el-button>
              </div>
            </div>
          </el-drawer>
        </el-form>
          </div>
        </div>
        <el-alert v-if="applyId" type="success" :closable="false" show-icon
          :title="`申请已暂存(${applyNo || applyId})，进入材料上传`" style="max-width:640px;margin-top:8px" />
      </el-card>

      <!-- 步骤2:上传材料(先从平台元数据同步已上传材料,再补全缺口) -->
      <el-card v-show="step === 1" shadow="never">
        <el-alert v-if="form.registerType === '确权变更' && form.changeTrigger" type="info" :closable="false" style="margin-bottom:10px"
          :title="`确权变更(触发:${form.changeTrigger}）—— 应交材料已收敛为差异项,仅需提交与本次变更相关的材料,无需重复全套`" />
        <div class="prm-table-note" style="margin-bottom:10px">
          材料优先<b>从数据资产管理平台元数据同步已上传项</b>(标「已同步·平台」免上传),仅需补全平台未覆盖的缺口。补全时"上传原件"(仅 PDF/Word/JPG/PNG,自动格式验证)或"仅登记"占位。
          <el-button size="small" type="primary" plain style="margin-left:12px" :loading="syncing" @click="doSyncPlatform(false)" title="从数据资产管理平台元数据同步已上传材料">
            <el-icon><Refresh /></el-icon> 平台同步
          </el-button>
          <el-button size="small" type="primary" plain style="margin-left:8px" :loading="parsing" @click="runParse" title="智能解析材料:要素抽取 / 敏感判定 / 内容查重">
            <el-icon><MagicStick /></el-icon> 智能解析
          </el-button>
        </div>
        <!-- 平台同步报告:已同步 N 项(平台已上传)/ 待补全 M 项 -->
        <el-alert v-if="syncReport" :type="(syncReport.stillMissing && syncReport.stillMissing.length) ? 'info' : 'success'"
                  :closable="false" style="margin-bottom:10px">
          <div><b>平台材料同步:</b>{{ syncReport.summary }}</div>
          <div v-if="syncReport.synced && syncReport.synced.length" style="margin-top:4px">
            已同步(平台已上传):
            <el-tag v-for="s in syncReport.synced" :key="s.materialName" type="success" size="small" effect="light" style="margin:2px 4px 2px 0">
              {{ s.code }} · {{ s.attachment }}
            </el-tag>
          </div>
          <div v-if="syncReport.stillMissing && syncReport.stillMissing.length" style="margin-top:4px">
            待补全:{{ syncReport.stillMissing.join('、') }}
          </div>
        </el-alert>
        <!-- 智能解析:确权内生 AI(走 confirm-service /ai/parse,不依赖独立工具) -->
        <el-alert v-if="parseResult" type="info" :closable="false" style="margin-bottom:10px">
          <div><b>智能解析:</b>{{ parseResult.summary }}</div>
          <el-table :data="parseResult.items" border size="small" style="margin-top:8px">
            <el-table-column prop="materialName" label="材料" min-width="160" />
            <el-table-column prop="rightHolder" label="识别权属主体" min-width="140" />
            <el-table-column prop="rightType" label="识别权类" width="110" />
            <el-table-column label="敏感" width="72" align="center">
              <template #default="{ row }"><el-tag v-if="row.sensitiveHit" type="danger" size="small">敏感</el-tag><span v-else>—</span></template>
            </el-table-column>
            <el-table-column label="查重" width="120" align="center">
              <template #default="{ row }"><el-tag v-if="row.duplicateOf" type="warning" size="small">疑与「{{ row.duplicateOf }}」重复</el-tag><span v-else>—</span></template>
            </el-table-column>
          </el-table>
        </el-alert>
        <el-table :data="checklist" border>
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="code" label="标识" width="70" align="center" />
          <el-table-column prop="name" label="应交材料" min-width="240" />
          <el-table-column label="状态" width="180" align="center">
            <template #default="{ row }">
              <template v-if="row.done && row.source === '平台同步'">
                <el-tag type="success" effect="dark">已同步·平台</el-tag>
                <div v-if="row.fileName" style="font-size:12px;color:#36b21d;margin-top:2px" :title="row.fileName">{{ row.fileName }}</div>
              </template>
              <template v-else-if="row.done && row.source === '系统生成'">
                <el-tag type="primary" effect="dark">系统生成</el-tag>
                <div v-if="row.fileName" style="font-size:12px;color:var(--prm-color-link);margin-top:2px" :title="row.fileName">{{ row.fileName }}</div>
              </template>
              <el-tag v-else-if="row.done" type="success" effect="light">已上传</el-tag>
              <el-tag v-else type="warning" effect="light">待补全</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" align="center">
            <template #default="{ row }">
              <span v-if="row.done && row.source === '平台同步'" style="color:var(--prm-color-text-weak);font-size:12px;margin-right:8px">平台原件(免上传)</span>
              <span v-else-if="row.done && row.source === '系统生成'" style="color:var(--prm-color-text-weak);font-size:12px;margin-right:8px">系统据申报生成(免上传)</span>
              <el-upload v-if="row.source !== '系统生成'" :auto-upload="false" :show-file-list="false" :on-change="(f) => onUploadFile(row, f)" style="display:inline-block">
                <el-button link type="success">{{ row.done && row.source === '平台同步' ? '改用本地原件' : '上传原件' }}</el-button>
              </el-upload>
              <el-button v-if="row.source !== '系统生成'" link type="primary" :disabled="row.done" @click="registerMaterial(row)" style="margin-left:8px">仅登记</el-button>
              <!-- 平台同步材料已落地平台原件字节(fileUrl 存在)才可预览;无字节则不显示,避免点开 404 -->
              <el-button v-if="row.materialId && (row.source !== '平台同步' || row.fileUrl)" link type="primary" style="margin-left:8px" @click="openFilePreview(materialFileUrl(row.materialId), row.fileName)">预览</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 步骤3:材料校验(规则校验 + AI 智能校验,评审8.4) -->
      <el-card v-show="step === 2" shadow="never">
        <div class="prm-table-note" style="margin-bottom:10px">一键完成【规则校验 + AI 材料校验(qwen3-max)】并给出<b>单一裁决</b>;有问题就地处理,全部通过方可提交审核。</div>
        <!-- 单一裁决状态条:一句话说清"能不能提交、还差什么" -->
        <div style="margin-bottom:10px">
          校验状态:<el-tag :type="checkStatus.type" effect="dark">{{ checkStatus.text }}</el-tag>
        </div>
        <!-- 主操作:一键校验(规则+AI),降认知、显性正确路径 -->
        <el-button :type="needRecheck || pendingItems.length ? 'danger' : 'primary'" :loading="checking || aiMatChecking" @click="runFullCheck" style="margin-bottom:6px">
          <el-icon><MagicStick /></el-icon> {{ (ruleDone || aiDone) ? '重新一键校验' : '一键校验(规则 + AI 材料)' }}
        </el-button>
        <el-button :disabled="!checkReport" @click="onExportCheck" style="margin-bottom:6px;margin-left:8px">导出校验结果</el-button>
        <!-- 次要:AI 辅助研判(可选,不影响提交门禁) -->
        <div style="margin:2px 0 12px;color:var(--prm-color-text-weak);font-size:12px">
          AI 辅助研判(可选,不影响提交):
          <el-button link type="primary" :loading="aiChecking" @click="runAiCheck">AI 决策研判</el-button>
          <el-button link type="primary" :loading="conflictChecking" @click="runConflict">权属冲突识别</el-button>
        </div>
        <AiThinking v-bind="aiThink.state" />
        <!-- AI 决策研判:确权内生 AI(走 confirm-service /ai/decision) -->
        <el-alert v-if="aiResult" :type="aiResult.prediction === '建议通过' ? 'success' : 'warning'" :closable="false" style="margin-bottom:12px">
          <div><b>AI 决策研判:{{ aiResult.prediction }}</b>(综合评分 {{ aiResult.score }})</div>
          <div style="margin-top:4px">AI 预测:{{ aiResult.aiPrediction || '未生成' }}</div>
          <div v-if="aiResult.supplementMaterials && aiResult.supplementMaterials.length" style="margin-top:4px">需补材料:{{ aiResult.supplementMaterials.join('、') }}</div>
          <div v-if="aiResult.pendingConflicts && aiResult.pendingConflicts.length" style="margin-top:4px">待处理冲突:{{ aiResult.pendingConflicts.join('、') }}</div>
          <div style="margin-top:4px;color:var(--prm-color-text-weak)">依据:{{ aiResult.basis }}</div>
        </el-alert>
        <!-- 权属冲突识别:确权内生 AI(走 confirm-service /ai/conflict) -->
        <el-alert v-if="conflictResult" :type="conflictResult.hasConflict ? 'error' : 'success'" :closable="false" style="margin-bottom:12px">
          <div><b>权属冲突识别:{{ conflictResult.hasConflict ? '发现冲突' : '未发现冲突' }}</b>(风险:{{ conflictResult.riskLevel }})</div>
          <div v-if="conflictResult.conflicts && conflictResult.conflicts.length" style="margin-top:4px">冲突:{{ conflictResult.conflicts.join('、') }}</div>
          <div v-if="conflictResult.suggestion" style="margin-top:4px;color:var(--prm-color-text-weak)">建议:{{ conflictResult.suggestion }}</div>
        </el-alert>
        <!-- 统一待处理清单(单一闭环):规则缺失/不合规 就地上传;AI 存疑/不通过 复核或去修正 -->
        <el-card v-if="pendingItems.length" shadow="never" style="margin-bottom:12px;border:1px solid #fde2e2;background:#fff8f8">
          <div style="font-weight:600;color:var(--prm-color-danger);margin-bottom:8px">需处理以下 {{ pendingItems.length }} 项后方可提交(处理完点上方「重新一键校验」)</div>
          <el-table :data="pendingItems" border size="small">
            <el-table-column label="来源" width="76" align="center">
              <template #default="{ row }"><el-tag :type="row.source === 'ai' ? 'warning' : 'danger'" size="small">{{ row.source === 'ai' ? 'AI' : '规则' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="name" label="材料 / 项" min-width="200" show-overflow-tooltip />
            <el-table-column prop="kind" label="问题" width="96" align="center">
              <template #default="{ row }"><el-tag type="danger" size="small">{{ row.kind }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="suggestion" label="说明 / 建议" min-width="240" show-overflow-tooltip />
            <el-table-column label="就地处理" width="200" align="center">
              <template #default="{ row }">
                <el-upload v-if="row.source === 'rule'" :auto-upload="false" :show-file-list="false" :on-change="(f) => onFixUpload(row, f)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" style="display:inline-block">
                  <el-button link type="primary">上传补充</el-button>
                </el-upload>
                <template v-else-if="row.source === 'ai'">
                  <el-button link type="primary" @click="goFix">去修正</el-button>
                  <el-button link type="success" @click="ackAi(row.name)" style="margin-left:6px">复核确认</el-button>
                </template>
                <span v-else style="color:var(--prm-color-text-weak);font-size:12px">需治理元数据后重校</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
        <!-- 佐证摘要(明细已并入上方待处理清单) -->
        <el-alert v-if="aiMatResult" :type="aiMatResult.overall === '通过' ? 'success' : (aiMatResult.overall === '不通过' ? 'error' : 'warning')" :closable="false" style="margin-bottom:12px">
          <div><b>AI 材料校验:{{ aiMatResult.overall }}</b> — {{ aiMatResult.overallDesc }}</div>
        </el-alert>
        <el-alert v-if="checkReport" :type="checkReport.allPass ? 'success' : 'warning'" :closable="false" style="margin-bottom:12px">
          <div>{{ checkReport.summary }}</div>
        </el-alert>
        <el-table :data="materials" border>
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="materialName" label="材料名称" min-width="240" />
          <el-table-column prop="materialType" label="类型" width="140" />
          <el-table-column label="校验结果" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.checkResult === '通过' ? 'success' : (row.checkResult === '不通过' ? 'danger' : 'info')" effect="light">
                {{ row.checkResult || '待校验' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="row.source === '平台同步' ? 'success' : 'info'" effect="light" size="small">{{ row.source || '用户上传' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="原件" min-width="160">
            <template #default="{ row }">
              <!-- 平台同步且已落地原件字节(fileUrl)才可在线预览;否则纯文本标注,不给会 404 的入口 -->
              <el-link v-if="row.source === '平台同步' && row.fileUrl" type="success" @click="previewMaterial(row)" :title="row.fileName">{{ row.fileName }}（平台原件·预览）</el-link>
              <span v-else-if="row.source === '平台同步'" style="color:#36b21d" :title="row.fileName">{{ row.fileName }}（平台原件）</span>
              <el-link v-else-if="row.fileName" type="primary" @click="previewMaterial(row)">{{ row.fileName }}（预览/下载）</el-link>
              <span v-else style="color:var(--prm-color-text-disabled)">占位/无原件</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 权益归集判定(分子公司共享网公司,《权益内部管理汇总表》说明页规则) -->
        <el-divider content-position="left" style="font-size:12px;color:var(--prm-color-text-weak)">权益归集判定(分子公司共享网公司)</el-divider>
        <el-descriptions v-if="consolidation" :column="4" border size="small" class="consol-panel">
          <el-descriptions-item label="命中规则">规则 {{ consolidation.rule }}</el-descriptions-item>
          <el-descriptions-item label="网公司持有权"><el-tag :type="consolidation.holdRight === '有' ? 'success' : 'info'" size="small">{{ consolidation.holdRight }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="网公司使用权"><el-tag :type="consolidation.useRight === '有' ? 'success' : 'info'" size="small">{{ consolidation.useRight }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="网公司经营权"><el-tag :type="consolidation.operateRight === '有' ? 'success' : (consolidation.operateRight === '无' ? 'info' : 'warning')" size="small">{{ consolidation.operateRight }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="共享判定原因" :span="4">{{ consolidation.reason }}</el-descriptions-item>
        </el-descriptions>
        <el-alert v-else type="info" :closable="false" title="暂存申请后自动按管制属性/来源判定/第三方识别给出网公司权益归集判定" style="margin-bottom:8px" />

        <!-- P1 表4 逐权益对照(确权变更):上一版权益卡片(底版)→ 本次归集判定,标 新增/撤销/维持 -->
        <template v-if="changeMode && table4Diff.length">
          <el-divider content-position="left" style="font-size:12px;color:var(--prm-color-text-weak)">权益变更对照(表4·上一版权益卡片 → 本次归集判定)</el-divider>
          <el-table :data="table4Diff" border size="small" style="max-width:840px;margin-bottom:8px">
            <el-table-column prop="label" label="权益类型" width="110" />
            <el-table-column label="上一版(确权底版)" min-width="150">
              <template #default="{ row }">
                <el-tag :type="row.before === '有' ? 'success' : 'info'" size="small" effect="plain">{{ row.before }}</el-tag>
                <span v-if="row.before === '有' && row.scope" style="margin-left:6px;color:var(--prm-color-text-weak);font-size:12px">{{ row.scope }}{{ row.validDate ? ' · 至 ' + row.validDate : '' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="本次(归集判定)" min-width="120">
              <template #default="{ row }"><el-tag :type="row.after === '有' ? 'success' : 'info'" size="small">{{ row.after }}</el-tag></template>
            </el-table-column>
            <el-table-column label="变更" width="92" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.mark === '新增'" type="success" size="small">新增</el-tag>
                <el-tag v-else-if="row.mark === '撤销'" type="danger" size="small">撤销</el-tag>
                <el-tag v-else-if="row.mark === '待判定'" type="info" size="small" effect="plain">待判定</el-tag>
                <span v-else style="color:var(--prm-color-text-weak)">维持</span>
              </template>
            </el-table-column>
          </el-table>
          <div style="font-size:12px;color:var(--prm-color-text-weak);margin-bottom:8px">
            注:撤销/新增权益须经审核;授权范围须 ⊆ 上一版确权边界(scope),扩边界由本次确权重新认定。
          </div>
        </template>

        <!-- 审批重要节点显式化(评审8.5) -->
        <el-divider content-position="left" style="font-size:12px;color:var(--prm-color-text-weak)">提交后审批链(重要节点)</el-divider>
        <el-steps :active="0" align-center class="approve-chain">
          <el-step title="人工预审" description="人工复核 AI 校验结果" />
          <el-step title="合规管控小组审核" description="生成表3/表4及认定意见" />
          <el-step title="数据管理部门主管复核" description="权属边界与责任复核" />
          <el-step title="经理/高级经理终审" :description="(form.applyMode === '一事一议' ? '一事一议:单独组织审议' : '逐级审批')" />
          <el-step title="制卡归集" description="生成权益卡片并归集" />
        </el-steps>
      </el-card>

      <!-- 步骤4:完成 -->
      <el-card v-show="step === 3" shadow="never">
        <el-result icon="success" title="确权申请已提交" :sub-title="`申请编号 ${applyNo || applyId}，已进入：人工预审 → 合规审核 → 主管复核 → 经理终审`">
          <template #extra>
            <el-button type="primary" @click="goProgress">查看进度</el-button>
            <el-button type="success" @click="router.push('/dpr/confirm/review?applyId=' + applyId)">去审核(审核申请提交)</el-button>
            <el-button @click="reset">再发起一笔</el-button>
          </template>
        </el-result>
      </el-card>
    </div>

    <PageActions>
      <el-button v-if="step > 0 && step < 3" @click="step--">上一步</el-button>
      <el-button v-if="step === 0" type="primary" :loading="saving" @click="next0">下一步</el-button>
      <el-button v-if="step === 1" type="primary" @click="next1">下一步</el-button>
      <el-tooltip v-if="step === 2 && !canSubmit" content="请先通过材料校验(全部应交项完整且合规)" placement="top">
        <span><el-button type="primary" disabled>提交审核</el-button></span>
      </el-tooltip>
      <el-button v-else-if="step === 2" type="primary" :loading="submitting" @click="next2">提交审核</el-button>
    </PageActions>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { autofillConfirm, saveConfirmDraft, uploadMaterial, uploadMaterialFile, materialFileUrl, listMaterialByApply, checkMaterial, runMaterialCheck, syncPlatformMaterials, pushMaterialReview, materialExportUrl, submitConfirm, saveAiSnapshot, saveTableItems, getConsolidation, previewConsolidation, aiMaterialCheck, listMaterialRules, aiParseConfirm, aiDecisionConfirm, aiConflictConfirm } from '@/api/confirm'
import AiThinking from '@/components/AiThinking.vue'
import PageActions from '@/components/PageActions.vue'
import { useAiThinking } from '@/composables/useAiThinking'
import { openFilePreview } from '@/composables/useFilePreview'
import { AI_PHASES } from '@/lib/aiPhases'
const aiThink = useAiThinking()

const router = useRouter()
const route = useRoute()
const rightTypes = ['数据资源持有权', '数据加工使用权', '数据产品经营权']
const sourceOpts = [
  { v: 'A', t: '自行生产', m: '数据来源设备/系统建设投入情况说明' },
  { v: 'B', t: '公开采集', m: '公共采集情况说明(方式/方法/来源)' },
  { v: 'C', t: '公共数据授权', m: '公共数据授权说明' },
  { v: 'D', t: '共同生产', m: '共享/共同生产情况说明' },
  { v: 'E', t: '交易采购', m: '交易采购情况说明' },
  { v: 'F', t: '其他', m: '其他来源情况说明' }
]
const relationOpts = [
  { v: 'G', t: '行政监管', m: '行政监管要求补充说明' },
  { v: 'H', t: '个人/家庭隐私', m: '个人/家庭隐私授权说明(如用户入网协议)' },
  { v: 'I', t: '第三方商业机密', m: '第三方商业机密授权说明' },
  { v: 'J', t: '其他第三方协议', m: '其他第三方机构协议' }
]

const step = ref(0)
const formRef = ref()
const autoLoading = ref(false)
const saving = ref(false)
const checking = ref(false)
const submitting = ref(false)
const quality = ref(null)
const applyId = ref('')
const applyNo = ref('')

// AI 材料校验:qwen3-max 逐份校验 完整性/合规性/与表单一致性(stub 回退)
const aiMatChecking = ref(false)
const aiMatResult = ref(null)
async function runAiMaterialCheck() {
  if (!applyId.value) { ElMessage.warning('请先完成步骤1暂存申请'); return }
  aiMatChecking.value = true
  try {
    const raw = await aiThink.run(() => aiMaterialCheck(applyId.value),
      { phases: AI_PHASES.materialCheck, title: '大模型材料校验中' })
    aiMatResult.value = typeof raw === 'string' ? JSON.parse(raw) : raw
    ElMessage.success('AI 材料校验完成')
  } catch (e) {
    ElMessage.warning('AI 材料校验失败:' + (e?.response?.data?.message || e?.message || '请先上传材料'))
  } finally { aiMatChecking.value = false }
}

// 智能解析材料(确权内生 AI,走 confirm-service /ai/parse):要素抽取/敏感判定/内容查重
const parsing = ref(false)
const parseResult = ref(null)
async function runParse() {
  if (!applyId.value) { ElMessage.warning('请先完成步骤1暂存申请并上传材料'); return }
  parsing.value = true
  try {
    parseResult.value = await aiThink.run(() => aiParseConfirm(applyId.value),
      { phases: AI_PHASES.materialCheck, title: '大模型智能解析中' })
    ElMessage.success('智能解析完成')
  } catch (e) {
    ElMessage.warning('智能解析失败:' + (e?.response?.data?.message || e?.message || '请先上传材料'))
  } finally { parsing.value = false }
}

// AI 决策研判(确权内生 AI,走 confirm-service /ai/decision):预测/需补材料/冲突/评分
const aiChecking = ref(false)
const aiResult = ref(null)
async function runAiCheck() {
  if (!applyId.value) { ElMessage.warning('请先完成步骤1暂存申请'); return }
  aiChecking.value = true
  try {
    aiResult.value = await aiThink.run(() => aiDecisionConfirm(applyId.value),
      { phases: AI_PHASES.analyze, title: '大模型决策研判中' })
    ElMessage.success('AI 决策研判完成')
  } catch (e) {
    ElMessage.warning('AI 决策研判失败:' + (e?.response?.data?.message || e?.message || '请先暂存申请'))
  } finally { aiChecking.value = false }
}

// 权属冲突识别(确权内生 AI,走 confirm-service /ai/conflict)
const conflictChecking = ref(false)
const conflictResult = ref(null)
async function runConflict() {
  if (!applyId.value) { ElMessage.warning('请先完成步骤1暂存申请'); return }
  conflictChecking.value = true
  try {
    conflictResult.value = await aiThink.run(() => aiConflictConfirm(applyId.value),
      { phases: AI_PHASES.analyze, title: '大模型权属冲突识别中' })
    ElMessage.success('权属冲突识别完成')
  } catch (e) {
    ElMessage.warning('权属冲突识别失败:' + (e?.response?.data?.message || e?.message || '请先暂存申请'))
  } finally { conflictChecking.value = false }
}

const checklist = ref([])
const materialRules = ref([]) // 后端可配置应交材料规则(单一真源)
const syncReport = ref(null)  // 平台元数据同步报告(已同步/待补全)
const syncing = ref(false)
const checkReport = ref(null)
const needRecheck = ref(false) // 规则/AI/材料变更后置脏:必须重新校验才能提交(闭环)
const aiAck = ref([])          // 已复核接受的 AI 存疑/不通过项(材料名),解除其阻断

const ruleDone = computed(() => !!checkReport.value)
const aiDone = computed(() => !!aiMatResult.value)

// AI 材料校验的存疑/不通过项(含是否已复核)
const aiIssues = computed(() => {
  const items = aiMatResult.value && Array.isArray(aiMatResult.value.items) ? aiMatResult.value.items : []
  return items.filter(it => it.verdict === '存疑' || it.verdict === '不通过')
    .map(it => ({ ...it, acked: aiAck.value.includes(it.materialName) }))
})
const aiUnresolved = computed(() => aiIssues.value.filter(i => !i.acked))

// 统一"待处理清单"(单一闭环):规则缺失/不合规 + AI 存疑/不通过(未复核)
// 元数据质量门禁(后端 submit 会自动驳回 score<80):前移为提交前显式拦截,杜绝"提交即被自动驳回"
const QUALITY_MIN = 80
const qualityBlocked = computed(() => quality.value != null && quality.value < QUALITY_MIN)

const pendingItems = computed(() => {
  const rule = !checkReport.value ? [] : [
    ...(checkReport.value.missing || []).map(n => ({ source: 'rule', name: n, kind: '缺失', suggestion: '未提交,请就地补充原件' })),
    ...(checkReport.value.nonCompliant || []).map(n => ({ source: 'rule', name: n, kind: '不合规', suggestion: '校验未通过,请重新上传' }))
  ]
  const ai = aiUnresolved.value.map(i => ({ source: 'ai', name: i.materialName,
    kind: i.verdict === '不通过' ? 'AI不通过' : 'AI存疑',
    suggestion: [i.issues, i.suggestion].filter(Boolean).join(' / ') || 'AI 提示需核实' }))
  const qa = qualityBlocked.value ? [{ source: 'quality', name: '元数据质量门禁', kind: '质量',
    suggestion: `元数据质量评分 ${quality.value} < ${QUALITY_MIN},提交将被自动驳回,请先治理元数据质量后重新一键校验` }] : []
  return [...rule, ...ai, ...qa]
})

// 单一裁决:规则全过 + AI 已跑且无未结存疑/不通过 + 质量达标 + 无未结变更,才点亮提交
const canSubmit = computed(() => ruleDone.value && checkReport.value.allPass
  && aiDone.value && aiUnresolved.value.length === 0 && !qualityBlocked.value && !needRecheck.value)

// 统一状态条:一句话说清"能不能提交、还差什么"
const checkStatus = computed(() => {
  if (needRecheck.value) return { type: 'warning', text: '已变更,请重新一键校验' }
  if (!ruleDone.value && !aiDone.value) return { type: 'info', text: '未校验 — 请点「一键校验」' }
  if (canSubmit.value) return { type: 'success', text: '✅ 全部通过,可提交' }
  const lack = []
  if (!ruleDone.value || !checkReport.value.allPass) lack.push('规则校验')
  if (!aiDone.value) lack.push('AI材料校验')
  if (qualityBlocked.value) lack.push('元数据质量')
  if (pendingItems.value.length) lack.push(`${pendingItems.value.length} 项待处理`)
  return { type: 'danger', text: '还差:' + (lack.join(' · ') || '处理待办') }
})

// 一键校验:依次跑 规则校验 + AI 材料校验(降认知,正确路径一步到位)
async function runFullCheck() {
  if (!applyId.value) { ElMessage.warning('请先完成步骤1暂存申请'); return }
  aiAck.value = [] // 重新校验,既往复核作废,需对新结果重新判断
  try { await runCheck() } catch (e) { /* 拦截器已提示;不阻断 AI 校验 */ }
  await runAiMaterialCheck() // 自带 try/catch,不抛
}
// 复核确认:申请人对该 AI 存疑/不通过项已核实接受 → 解除阻断
function ackAi(name) {
  if (!aiAck.value.includes(name)) aiAck.value.push(name)
  ElMessage.success('已复核确认:' + name)
}
// 去修正:返回材料步重新上传修正(资产名/内容不一致等),修正后需重新一键校验
function goFix() { step.value = 1 }
const materials = ref([])

// 表级清单(M02)
const tableItemText = ref('')
const tableItems = ref([])
const metaLoading = ref(false)

// G–J 标记取上方"信息关联识别"勾选(逐表可覆盖)
function gjFromForm() {
  return {
    gFlag: form.relationIdent.includes('G') ? '是' : '否', hFlag: form.relationIdent.includes('H') ? '是' : '否',
    iFlag: form.relationIdent.includes('I') ? '是' : '否', jFlag: form.relationIdent.includes('J') ? '是' : '否'
  }
}
// 来源判定缺省:取系统级第一个勾选项的完整标签(如 "A 自行生产数据"),否则默认 A
function defaultSourceLabel() {
  const code = form.sourceIdent[0]
  const opt = sourceOpts.find(s => s.v === code)
  return opt ? `${opt.v} ${opt.t}` : 'A 自行生产数据'
}

// 平台元数据 → 库表行(统一带入);保留平台预填快照 _pf,供"平台预填/已修改·原值"留痕。
// 锁定字段(实例/schema/表代码/表名/密级/确权时间)平台为源、只读;表2 明细预填、可改。
function rowFromMeta(t, channel) {
  const row = {
    instanceName: t.instanceName || '', schemaName: t.schemaName || '',
    tableCode: t.tableCode || '', tableName: t.tableName || '',
    tableComment: t.tableComment || t.tableName || '', secretLevel: t.secretLevel || '不涉密',
    sourceType: t.sourceType || defaultSourceLabel(), sourceSubject: t.sourceSubject || '',
    existTable: t.existTable !== false, sourceChannel: channel,
    gFlag: t.gFlag ? '是' : '否', hFlag: t.hFlag ? '是' : '否',
    iFlag: t.iFlag ? '是' : '否', jFlag: t.jFlag ? '是' : '否',
    sourceDesc: t.sourceDesc || '', gSubject: t.gSubject || '', hSubject: t.hSubject || '',
    iSubject: t.iSubject || '', jSubject: t.jSubject || '', riskDesc: t.riskDesc || '',
    authTime: t.authTime || '',
    // 平台已上传材料附件名(G2·只读;正本在「上传材料」步同步)
    sourceAttachment: t.sourceAttachment || '', checkAttachment: t.checkAttachment || '',
    privacyAttachment: t.privacyAttachment || '', busSecretAttachment: t.busSecretAttachment || '',
    equityAttachment: t.equityAttachment || '',
    // 逐表对外授权状态(确权变更精确影响)
    authorized: !!t.authorized, authId: t.authId || '', authScope: t.authScope || '', authStatus: t.authStatus || '',
    // 确权状态(确权变更:已确权可改基线 / 新增纳入);已确权表平台带确权时间
    confirmed: !!t.authTime
  }
  // 平台预填快照(表2 可改字段 + G–J 勾选),用于抽屉"平台预填 vs 已修改"留痕 + 逐表改动检测(P1 per-table 汇总)
  row._pf = {
    sourceType: row.sourceType, sourceSubject: row.sourceSubject, sourceDesc: row.sourceDesc,
    gSubject: row.gSubject, hSubject: row.hSubject, iSubject: row.iSubject, jSubject: row.jSubject,
    gFlag: row.gFlag, hFlag: row.hFlag, iFlag: row.iFlag, jFlag: row.jFlag
  }
  return row
}

// 表2 字段留痕:平台预填且未改=prefilled、改过=changed(带原值)、平台无值=manual
function pfMark(row, key) {
  const before = row && row._pf ? (row._pf[key] || '') : ''
  const cur = (row && row[key]) || ''
  if (before && cur !== before) return { state: 'changed', before }
  if (before) return { state: 'prefilled' }
  return { state: 'manual' }
}
// G2:G/H/I/J → 对应"信息识别关联资料附件"字段(AU_TABLE_META_DATA.*_NAME)
function relAttachKey(v) {
  return { G: 'checkAttachment', H: 'privacyAttachment', I: 'busSecretAttachment', J: 'equityAttachment' }[v] || ''
}

// 选卡片→自动带库表清单(平台元数据,未接入时后端桩合成);只读预填,不覆盖已有手工/已存项
async function loadTablesFromMeta(silent = false) {
  if (!form.assetId) { if (!silent) ElMessage.warning('请先选取数据资产卡片'); return }
  metaLoading.value = true
  try {
    const tbls = await listAssetTables(form.assetId, form.assetName) || []
    const existCodes = new Set(tableItems.value.map(t => t.tableCode).filter(Boolean))
    const mapped = tbls.filter(t => !existCodes.has(t.tableCode)).map(t => rowFromMeta(t, 'META'))
    if (mapped.length) {
      tableItems.value = [...tableItems.value, ...mapped]
      if (!silent) ElMessage.success(`已从平台元数据带入 ${mapped.length} 张库表 + 聚合 A–J 来源/关联识别(可改)`)
    } else if (!silent) {
      ElMessage.info('平台元数据未返回新库表')
    }
    // 系统级 A–F/G–J 并集需在 tableItems 更新后重算(派生只读·单一真源=表2)
    aggregateIdentFromTables()
    // 申报主体/系统责任信息平台带出(与 onTreeSelect 一致,确保单卡/演示/?assetId 路径也填满必填项)
    const first = tbls[0]
    if (first) {
      if (first.mgtUser) form.systemOwner = first.mgtUser
      if (first.mgtUserPhone) form.contactInfo = first.mgtUserPhone
      if (first.mgtUnit) form.rightHolder = first.mgtUnit
      if (first.mgtDept) form.respDept = first.mgtDept
      if (first.subjectLevel) form.subjectLevel = first.subjectLevel
    }
  } catch (e) {
    if (!silent) ElMessage.warning('平台元数据暂不可用,可用下方"批量导入"手工补充库表')
  } finally { metaLoading.value = false }
}

// 系统级确权入口:左侧范围树勾选库表(同一系统)→ 关联键置为系统、带出选中库表的真实元数据(替代"一卡假合成多表")
// 业务域 → 管制属性默认:强监管自然垄断域(输配电/调度/电网运行)默认管制业务,竞争/职能域默认非管制(可改)
function regulatedByDomain(domain) {
  return /输配电|调度|电网运行|输电|配电/.test(domain || '') ? '管制业务' : '非管制'
}

async function onTreeSelect(sysName, tableCodes, domain) {
  if (!sysName || !tableCodes || !tableCodes.length) { tableItems.value = []; return }
  form.assetId = 'SYS:' + sysName
  form.assetName = sysName
  form.systemName = sysName
  if (domain) form.regulated = regulatedByDomain(domain)
  metaLoading.value = true
  try {
    const all = (await cardsBySystem(sysName, null, changeMode.value ? '' : 'unconfirmed')) || []
    const picked = all.filter(t => tableCodes.includes(t.tableCode))
    tableItems.value = picked.map(t => rowFromMeta(t, 'CATALOG'))
    // 系统责任信息 + 申报主体 平台带出(表1 系统负责人/联系方式/公司主体/部门/单位层级,系统内一致;可改)
    const first = picked[0] || all[0]
    if (first) {
      if (first.mgtUser) form.systemOwner = first.mgtUser
      if (first.mgtUserPhone) form.contactInfo = first.mgtUserPhone
      if (first.mgtUnit) form.rightHolder = first.mgtUnit
      if (first.mgtDept) form.respDept = first.mgtDept
      if (first.subjectLevel) form.subjectLevel = first.subjectLevel
    }
    aggregateIdentFromTables(picked)
    // P0 确权变更:载入该系统现有确权基线 + 预填系统级字段(权属类型/管制),激活变更前→后 diff
    if (changeMode.value) {
      await loadChangeBaselineForSystem(sysName)
    }
    ElMessage.success(`已按系统「${sysName}」带入 ${picked.length} 张库表 + 聚合 A–J(可改)`)
  } catch (e) {
    ElMessage.warning('按系统带库表失败,请重试或用单卡片/批量导入')
  } finally { metaLoading.value = false }
}

// 上一版表3逐表行 → 可编辑库表行(保留 是/否 原值,不按布尔误判;带 _pf 快照供"已修改·原值"diff)
function rowFromBaseline(t) {
  const yn = (v) => (v === '是' || v === true) ? '是' : '否'
  const row = {
    instanceName: t.instanceName || '', schemaName: t.schemaName || '',
    tableCode: t.tableCode || '', tableName: t.tableName || '',
    tableComment: t.tableComment || t.tableName || '', secretLevel: t.secretLevel || '不涉密',
    sourceType: t.sourceType || defaultSourceLabel(), sourceSubject: t.sourceSubject || '',
    existTable: true, sourceChannel: 'BASELINE', confirmed: true,
    gFlag: yn(t.gFlag), hFlag: yn(t.hFlag), iFlag: yn(t.iFlag), jFlag: yn(t.jFlag),
    sourceDesc: t.sourceDesc || '', gSubject: t.gSubject || '', hSubject: t.hSubject || '',
    iSubject: t.iSubject || '', jSubject: t.jSubject || '', riskDesc: t.riskDesc || '', authTime: ''
  }
  row._pf = {
    sourceType: row.sourceType, sourceSubject: row.sourceSubject, sourceDesc: row.sourceDesc,
    gSubject: row.gSubject, hSubject: row.hSubject, iSubject: row.iSubject, jSubject: row.jSubject
  }
  return row
}

// P0:确权变更——反查上一版真实确权结论(表3行+权益卡片)作变更底版,在其上做差异编辑
async function loadChangeBaselineForSystem(sysName) {
  try {
    const full = await fetchChangeBaselineFull(sysName, form.assetId)
    const b = full && full.base ? full.base : null
    baselineFromReal.value = !!(full && full.fromRealConfirm)
    baselineCards.value = (full && full.cards) || []
    baselineSummaries.value = (full && full.summaries) || []
    baselinePriorApplyId.value = (full && full.priorApplyId) || ''
    // 真实上一版表3行作底版:替换目录合成的库表行(在权威结论上做差异编辑)
    if (baselineFromReal.value && full.tableItems && full.tableItems.length) {
      tableItems.value = full.tableItems.map(rowFromBaseline)
    }
    if (!b) { changeBaseline.value = null; return }
    changeBaseline.value = b
    // 预填:系统级字段从基线起(用户只改变动维度)
    if (b.rightHolder) form.rightHolder = b.rightHolder
    if (b.subjectLevel) form.subjectLevel = b.subjectLevel
    if (b.respDept) form.respDept = b.respDept
    if (b.regulated) form.regulated = b.regulated
    if (b.rightType) form.rightTypes = String(b.rightType).split(/[、,，]/).map(s => s.trim()).filter(Boolean)
  } catch (e) { changeBaseline.value = null; baselineFromReal.value = false; baselineCards.value = []; baselineSummaries.value = []; baselinePriorApplyId.value = '' }
  // P1:取该系统授权影响(逐表精确:只看本次选中的库表;按触发动因给处置建议)
  await refreshAuthImpact()
}
// P1:授权影响 + 申报人已知悉处置确认
const authImpact = ref(null)
const ackAuthImpact = ref(false)
// 逐表精确:入参=当前已选库表代码 + 变更触发;选表/换触发即重算
async function refreshAuthImpact() {
  if (!changeMode.value || !form.systemName) { authImpact.value = null; return }
  try {
    const codes = tableItems.value.map(t => t.tableCode).filter(Boolean)
    authImpact.value = await fetchAuthImpact(form.systemName, codes, form.changeTrigger)
    ackAuthImpact.value = false
  } catch (e) { authImpact.value = null }
}

// 方案A:表1 系统级 A–F/G–J = 库表清单(表2)各表 flags 的并集,派生只读、随表2 自动重算(单一真源=表2,replace 而非 merge)
function aggregateIdentFromTables() {
  const srcSet = new Set()
  const relSet = new Set()
  for (const t of tableItems.value) {
    const c = (t.sourceType || '').trim().charAt(0)
    if ('ABCDEF'.includes(c)) srcSet.add(c)
    if (t.gFlag === '是') relSet.add('G')
    if (t.hFlag === '是') relSet.add('H')
    if (t.iFlag === '是') relSet.add('I')
    if (t.jFlag === '是') relSet.add('J')
  }
  form.sourceIdent = [...srcSet].sort()
  form.relationIdent = [...relSet].sort()
  // 涉第三方且来源主体尚空时,带入库表里第一条来源主体
  if (!form.sourceSubject) {
    const s = tableItems.value.find(t => t.sourceSubject)
    if (s) form.sourceSubject = s.sourceSubject
  }
}

// 批量粘贴(兜底/补充):平台未接入或离线整理时手工导入,追加并入同一清单(按表代码去重)
function parseTableItems() {
  const parsed = tableItemText.value.split('\n').map(l => l.trim()).filter(Boolean).map(line => {
    const [instanceName, schemaName, tableCode, tableName, secretLevel, sourceType, sourceSubject] =
      line.split(/[,，]/).map(s => (s || '').trim())
    return { instanceName, schemaName, tableCode, tableName, tableComment: tableName,
      secretLevel: secretLevel || '不涉密', sourceType: sourceType || defaultSourceLabel(), sourceSubject: sourceSubject || '',
      existTable: true, sourceChannel: 'MANUAL', ...gjFromForm(),
      sourceDesc: '', gSubject: '', hSubject: '', iSubject: '', jSubject: '', riskDesc: '', authTime: '',
      _pf: {} }
  }).filter(it => it.tableCode || it.tableName)
  if (!parsed.length) { ElMessage.warning('未解析到有效表级记录,请检查格式'); return }
  const existCodes = new Set(tableItems.value.map(t => t.tableCode).filter(Boolean))
  const fresh = parsed.filter(p => !p.tableCode || !existCodes.has(p.tableCode))
  tableItems.value = [...tableItems.value, ...fresh]
  tableItemText.value = ''
  ElMessage.success(`已导入 ${fresh.length} 张表(并入清单,共 ${tableItems.value.length} 张)`)
}
function removeTableItem(row) { tableItems.value = tableItems.value.filter(t => t !== row) }
function clearTableItems() { tableItems.value = [] }

// ===== 表2 逐表(方案A):每张库表各自承载第三方权益明细,替代旧"系统级单份" =====
// 该表是否涉第三方/敏感、需填表2:来源 B–F,或任一 G/H/I/J = 是
function rowDisputed(row) {
  const c = (row.sourceType || '').trim().charAt(0)
  return 'BCDEF'.includes(c) || row.gFlag === '是' || row.hFlag === '是' || row.iFlag === '是' || row.jFlag === '是'
}
// 争议行表2 必填项是否齐:B–F→来源主体;H→隐私关联主体;J→协议关联主体
function rowTable2Done(row) {
  if (!rowDisputed(row)) return true
  const c = (row.sourceType || '').trim().charAt(0)
  if ('BCDEF'.includes(c) && !(row.sourceSubject || '').trim()) return false
  if (row.hFlag === '是' && !(row.hSubject || '').trim()) return false
  if (row.jFlag === '是' && !(row.jSubject || '').trim()) return false
  return true
}
function rowTable2Status(row) {
  if (!rowDisputed(row)) return { type: 'info', text: '无需填' }
  return rowTable2Done(row) ? { type: 'success', text: '已填' } : { type: 'danger', text: '待填' }
}
const disputedRows = computed(() => tableItems.value.filter(rowDisputed))
const pendingT2Rows = computed(() => disputedRows.value.filter(r => !rowTable2Done(r)))

// 逐表编辑抽屉
const t2Drawer = reactive({ visible: false, row: null })
function openT2(row) { t2Drawer.row = row; t2Drawer.visible = true }

// 批量套用缓冲:一次填写,套用到所有"争议表"(便捷工具,非唯一真源;各表可再单独覆盖)
const b2 = reactive({ sourceSubject: '', sourceDesc: '', hSubject: '', jSubject: '', riskDesc: '' })
function applyBatchToDisputed(overwrite = false) {
  const rows = disputedRows.value
  if (!rows.length) { ElMessage.info('当前没有涉第三方/敏感的库表,无需套用'); return }
  for (const row of rows) {
    const set = (k) => { if (b2[k] && (overwrite || !(row[k] || '').trim())) row[k] = b2[k] }
    const c = (row.sourceType || '').trim().charAt(0)
    if ('BCDEF'.includes(c)) set('sourceSubject')
    set('sourceDesc'); set('riskDesc')
    if (row.hFlag === '是') set('hSubject')
    if (row.jFlag === '是') set('jSubject')
  }
  ElMessage.success(`已套用到 ${rows.length} 张争议表(${overwrite ? '覆盖' : '仅空值'})`)
}

// 权益归集判定·内联试算(管制属性的"后果"显性化):选项变更即预览经营权判定,不落库
const consolPreview = ref(null)
let consolTimer = null
async function refreshConsolPreview() {
  if (!form.systemName || !tableItems.value.length) { consolPreview.value = null; return }
  try {
    consolPreview.value = await previewConsolidation({
      regulated: form.regulated === '管制业务',
      involvesThird: disputedRows.value.length > 0,
      hasOperateClaim: form.rightTypes.some(t => String(t).includes('经营')),
      otherRestriction: tableItems.value.some(t => t.iFlag === '是' || t.jFlag === '是')
    })
  } catch (e) { consolPreview.value = null }
}
// 经营权状态 → 配色(有=绿/无=红/依权益判定=蓝)
function opTag(op) { return op === '有' ? 'success' : (op === '无' ? 'danger' : 'primary') }
function opClass(op) { return op === '有' ? 'op-yes' : (op === '无' ? 'op-no' : 'op-depend') }
// 把命中规则翻成"人话"结论(给申报人看,替代法条墙)
const consolPlain = computed(() => {
  const cp = consolPreview.value
  if (!cp) return ''
  switch (cp.rule) {
    case '1.1': return '自行生产、不涉第三方,管制单位经营权调整为有 → 经营权归网公司'
    case '1.2': return cp.operateRight === '有'
      ? '自行生产、不涉第三方且主张经营权 → 经营权归网公司'
      : '自行生产、不涉第三方但未主张经营权 → 网公司无经营权'
    case '2.1': return '涉第三方且主张经营权 → 经营权须依权益逐项判定后归网公司'
    case '2.2': return '涉第三方且未主张经营权 → 网公司无经营权'
    case '3.1': return cp.operateRight === '无'
      ? '涉第三方、管制单位恢复经营权后仍存在其他无经营权约束 → 网公司无经营权'
      : '涉第三方、管制单位恢复经营权后 → 经营权依权益判定归网公司'
    default: return ''
  }
})

// 权益归集判定结果(分子公司共享网公司)
const consolidation = ref(null)
async function loadConsolidation() {
  if (!applyId.value) return
  try { consolidation.value = await getConsolidation(applyId.value) } catch (e) { consolidation.value = null }
}

// P1 表4 逐权益对照:上一版权益卡片(底版) → 本次归集判定,标 新增/撤销/边界变化/维持(确权变更专用)
const RIGHT_DIMS = [
  { label: '持有权', match: (t) => (t || '').includes('持有'), cur: (c) => c.holdRight },
  { label: '使用权', match: (t) => (t || '').includes('使用') || (t || '').includes('加工'), cur: (c) => c.useRight },
  { label: '经营权', match: (t) => (t || '').includes('经营'), cur: (c) => c.operateRight }
]
const table4Diff = computed(() => {
  if (!changeMode.value || !baselineCards.value.length) return []
  return RIGHT_DIMS.map(d => {
    const card = baselineCards.value.find(c => d.match(c.rightType))
    const before = card ? '有' : '无'
    const after = consolidation.value ? (d.cur(consolidation.value) || '无') : '—'
    let mark = '维持'
    if (before === '无' && after === '有') mark = '新增'
    else if (before === '有' && after === '无') mark = '撤销'
    else if (after === '—') mark = '待判定'
    return {
      label: d.label, before, after, mark,
      scope: card ? (card.scope || '全字段') : '',
      validDate: card && card.validDate ? String(card.validDate).slice(0, 10) : ''
    }
  })
})

const form = reactive({
  assetId: '', assetName: '', systemName: '', rightTypes: [], rightHolder: '', respDept: '', subjectLevel: '',
  systemOwner: '', contactInfo: '', assetSecretLevel: '', region: '',
  registerType: '初始确权', changeTrigger: '', applyMode: '常规', regulated: '非管制',
  purpose: '', thirdPartyInfo: '', sourceSubject: '', sourceLimit: '', relationSubject: '', equityRisk: '',
  privacyInfo: '', sourceIdent: [], relationIdent: []
})
// 归集试算监听:须在 form 定义之后注册(watch getter 在 setup 即求值,提前引用 form 会 TDZ 崩溃→白屏)
watch(
  () => [form.regulated, form.rightTypes, disputedRows.value.length, tableItems.value.length, form.systemName],
  () => { clearTimeout(consolTimer); consolTimer = setTimeout(refreshConsolPreview, 200) },
  { deep: true }
)
// 方案A:库表清单(表2)任意变化即重算表1 系统级 A–F/G–J 并集(派生只读·单一真源=表2)
watch(tableItems, () => aggregateIdentFromTables(), { deep: true })
// P1:确权变更——换触发动因即重算授权处置建议
watch(() => form.changeTrigger, () => { if (changeMode.value) refreshAuthImpact() })
const rules = {
  // 表1 公司主体必填(G1);主体层级=公司主体口径必填(G3);系统负责人/联系方式建议必填(G3)
  rightHolder: [{ required: true, message: '请填写申报权属主体(表1 公司主体)', trigger: 'blur' }],
  subjectLevel: [{ required: true, message: '请选择主体层级(公司总部/分省公司/专业子公司)', trigger: 'change' }],
  systemOwner: [{ required: true, message: '请填写系统负责人', trigger: 'blur' }],
  contactInfo: [{ required: true, message: '请填写联系方式', trigger: 'blur' }],
  rightTypes: [{ required: true, type: 'array', min: 1, message: '请至少主张一种权属类型', trigger: 'change' }]
}

// 基于原单修改重提:从被驳回确权单带入字段(新申请,旧单保留已驳回)
// 拉取可配置应交材料规则(单一真源);失败则回退内置默认,保证向导可用
async function loadMaterialRules() {
  try {
    const rules = await listMaterialRules('确权')
    if (Array.isArray(rules) && rules.length) materialRules.value = rules
  } catch (e) { /* 回退内置默认 */ }
}

// 从识别串(逗号分隔,token 可能是 "A" 或 "A自行生产数据")抽取允许的 A–J 码
function parseIdentCodes(s, allowed) {
  if (!s) return []
  return String(s).split(/[,，]/)
    .map(t => (t.trim().match(/^[A-J]/) || [''])[0])
    .filter(c => allowed.includes(c))
}

// 初始/确权变更分菜单:由路由 meta.mode 决定本页登记类型(initial=初始确权 / change=确权变更),不再二选一混用
const changeMode = computed(() => route.meta?.mode === 'change')

// ===== P0/P1 变更类型 = 由所选库表确权状态派生约束 + 簇内多选 =====
// 第一性:四类变更 = (动谁:新表 insert / 已确权表 update) ×(动什么元素簇:来源A–F / 关联G–J / 时效)。
// 数据新增(insert,新表首次确权)与 来源/管理/到期变更(update,对已确权结论修订)两种编辑模式互斥,不可同单。
const hasNewTable = computed(() => changeMode.value && tableItems.value.some(t => t.confirmed === false))
const hasConfirmedTable = computed(() => changeMode.value && tableItems.value.some(t => t.confirmed === true))
const isDataAdd = computed(() => hasNewTable.value && !hasConfirmedTable.value)       // 全为新增表 → 锁定"数据新增"
const allConfirmedSel = computed(() => hasConfirmedTable.value && !hasNewTable.value)  // 全为已确权表 → 来源/管理/到期可多选
const mixedSelection = computed(() => hasNewTable.value && hasConfirmedTable.value)    // 新增+已确权混选 → 拦截(编辑模式冲突)

// 簇内多选(update 模式):同一已确权表的不同元素簇可同时变更;数据新增独占(由上方约束自动锁定)
const CHANGE_TRIGGER_OPTS = [
  { v: '数据来源变更', t: '来源·生产方式重判(A–F)' },
  { v: '管理要求变更', t: '关联·合规重判(G–J)' },
  { v: '权益到期', t: '有效期处置(续止)' },
  { v: '其他', t: '其他(保守全展开)' }
]
const changeTriggers = ref([])
// changeTriggers[] → form.changeTrigger 顿号拼接串(后端 narrowForChange/authImpact/validate 关键词匹配天然支持多触发)
watch(changeTriggers, (v) => { form.changeTrigger = (v || []).join('、') }, { deep: true })
// 选表确权状态变化 → 同步触发约束:数据新增锁定;离开数据新增态则清掉"数据新增"占位(由用户在多选里重选)
watch([isDataAdd, allConfirmedSel], () => {
  if (isDataAdd.value) { if (!(changeTriggers.value.length === 1 && changeTriggers.value[0] === '数据新增')) changeTriggers.value = ['数据新增'] }
  else if (changeTriggers.value.includes('数据新增')) { changeTriggers.value = changeTriggers.value.filter(x => x !== '数据新增') }
})
// 分簇门控:勾选了哪个簇,才比对/收敛哪个簇(消灭"选子集→系统并集幻象删减")
const keepSourceCluster = computed(() => changeTriggers.value.some(t => t.includes('来源')) || changeTriggers.value.includes('其他'))
const keepRelationCluster = computed(() => changeTriggers.value.some(t => t.includes('管理') || t.includes('监管')) || changeTriggers.value.includes('其他'))
onMounted(async () => {
  loadMaterialRules()
  form.registerType = changeMode.value ? '确权变更' : '初始确权'
  // 主动入口「发起变更」(P1):列表页带 assetId 进入,预选资产并按确权状态自动定登记类型(已确权→确权变更+基线对照)
  if (route.query.assetId && !applyId.value) {
    await initAssetEntry(String(route.query.assetId))
    return
  }
  if (!route.query.reopen) return
  try {
    const o = JSON.parse(sessionStorage.getItem('prm-reopen') || '{}')
    if (o.domain === '确权' && o.raw) {
      const r = o.raw
      Object.assign(form, {
        assetId: r.assetId || '', assetName: r.assetName || '', systemName: r.systemName || '', rightHolder: r.rightHolder || '',
        respDept: r.respDept || '', systemOwner: r.systemOwner || '', contactInfo: r.contactInfo || '', subjectLevel: r.subjectLevel || '',
        registerType: r.registerType || '初始确权', changeTrigger: r.changeTrigger || '', regulated: r.regulated || '非管制',
        purpose: r.purpose || '', sourceSubject: r.sourceSubject || '', sourceLimit: r.sourceLimit || '',
        relationSubject: r.relationSubject || '', equityRisk: r.equityRisk || '', privacyInfo: r.privacyInfo || '',
        rightTypes: r.rightType ? String(r.rightType).split(/[、,，]/).map(s => s.trim()).filter(Boolean) : [],
        // 还原 A–J 来源/关联识别勾选(原串如 "A,B" / "A自行生产数据"),否则重提时勾选全丢、next0 被拦
        sourceIdent: parseIdentCodes(r.sourceIdentification, ['A', 'B', 'C', 'D', 'E', 'F']),
        relationIdent: parseIdentCodes(r.relationIdentification, ['G', 'H', 'I', 'J']),
      })
      // 还原变更触发多选(原串为顿号/逗号拼接),与 changeTriggers 多选模型对齐
      changeTriggers.value = r.changeTrigger
        ? String(r.changeTrigger).split(/[、,，]/).map(s => s.trim()).filter(Boolean)
        : []
      ElMessage.warning('已带入被驳回原单内容,请修改后重新提交(将作为新申请)')
    }
  } catch (e) { /* ignore */ }
  sessionStorage.removeItem('prm-reopen')
})
const needTable2 = computed(() =>
  disputedRows.value.length > 0 ||
  form.sourceIdent.some(c => ['B', 'C', 'D', 'E', 'F'].includes(c)) ||
  form.relationIdent.some(c => ['G', 'H', 'I', 'J'].includes(c)))

// 关联数据资产卡片:按 名称/编码/系统 搜索平台卡片并选取(平台为源,台账兜底);存ID、带出名,不手填
import { searchAssetCards, listAssetTables, getAssetProperty, cardsBySystem, fetchChangeBaseline, fetchChangeBaselineFull, fetchAuthImpact } from '@/api/assetCard'
import ConfirmCatalogTree from '@/views/confirm/ConfirmCatalogTree.vue'
const assetOpts = ref([])
const assetSearching = ref(false)
const pickedCard = ref(null)
// 选卡后查该资产确权状态:已确权→默认确权变更,未确权→默认初始确权(登记类型二选一)
const assetConfirmState = ref('')      // 待确权/确权中/已确权/已驳回
const assetConfirmed = ref(false)      // 是否已确权(state==='已确权')
const changeBaseline = ref(null)       // 确权变更基线:已有确权结论,用于变更前后 diff
// P0 变更底版:反查上一版真实确权结论 —— 权益卡片(表4/边界)+ 认定意见(表3/表4 来源留痕)
const baselineCards = ref([])          // 上一版权益卡片(rightType/scope/validDate/cardStatus)
const baselineSummaries = ref([])      // 上一版表3/表4认定意见文本
const baselineFromReal = ref(false)    // 基线是否来自真实上一版确权(false=合成桩兜底)
const baselinePriorApplyId = ref('')   // 被取代的上一版申请ID(写入 baselineRef,版本链留痕)
async function checkAssetConfirmState(assetId) {
  assetConfirmState.value = ''; assetConfirmed.value = false; changeBaseline.value = null
  baselineFromReal.value = false; baselineCards.value = []; baselineSummaries.value = []; baselinePriorApplyId.value = ''
  if (!assetId) return
  try {
    const p = await getAssetProperty(assetId)
    const st = p && p.state ? p.state : '待确权'
    assetConfirmState.value = st
    assetConfirmed.value = st === '已确权'
    // 自动默认登记类型:已确权→确权变更;未确权→初始确权(reConfirm 在保存时由 registerType 派生)
    form.registerType = assetConfirmed.value ? '确权变更' : '初始确权'
    if (assetConfirmed.value) {
      changeBaseline.value = p // 留存基线,供"变更前后对照"
      // 确权变更=对已有确权结论的修订:基于现状预填全部对照维度(覆盖平台带出值),
      // 使初始 diff 为空,用户改动哪一维即真实高亮哪一维(否则未预填维度会误报"X→空")
      if (p.rightHolder) form.rightHolder = p.rightHolder
      if (p.rightType) form.rightTypes = String(p.rightType).split(/[、,，]/).map(s => s.trim()).filter(Boolean)
      if (p.respDept) form.respDept = p.respDept
      if (p.sourceSubject) form.sourceSubject = p.sourceSubject
      if (p.sourceLimit) form.sourceLimit = p.sourceLimit
      if (p.privacyInfo) form.privacyInfo = p.privacyInfo
      if (p.thirdPartyInfo) form.thirdPartyInfo = p.thirdPartyInfo
      if (p.relationSubject) form.relationSubject = p.relationSubject
      ElMessage.info('该资产已确权,已默认「确权变更」并基于现有确权结论预填,请只改动发生变动的维度')
    } else {
      changeTriggers.value = [] // 未确权回到初始确权:清空变更触发(watch 同步 form.changeTrigger='')
    }
  } catch (e) { /* 平台/产权查询失败:不阻断,保留用户当前选择 */ }
}

// R2 资产身份摘要:平台带出的身份字段默认折叠为只读摘要条,聚焦权属研判项;可展开编辑
const identityPanel = ref([]) // 默认折叠([]);展开为 ['id']
const identitySummary = computed(() => {
  const parts = [form.assetName, form.systemName,
    form.systemOwner && ('负责人 ' + form.systemOwner), form.contactInfo].filter(Boolean)
  return parts.length ? parts.join(' · ') : '选取资产后自动带出(系统 / 负责人 / 联系方式)'
})

// 变更前后对照(P0):确权变更时,8 个关键维度当前值 ↔ 基线值实时比对,仅列变化项
// 权属类型基线串可能用 ,/，/、 任一分隔:统一规范化为「、」拼接,避免分隔符差异误报"已修改"
const canonTypes = (v) => String(v == null ? '' : v).split(/[、,，]/).map(s => s.trim()).filter(Boolean).join('、')
// A–F/G–J 系统级"新值"= 基线该簇 ∪ 本次选中库表该簇(只增不删):新增表带来新增项,选子集不误删既有,杜绝幻象
function unionClusterAfter(baseField, curArr) {
  const b = changeBaseline.value
  const baseCodes = b ? String(baseField(b) || '').split(/[、,，]/).map(s => s.trim()).filter(Boolean) : []
  const set = new Set([...baseCodes, ...(curArr || [])])
  return canonTypes([...set].sort().join('、'))
}
// 系统级维度:5 个申报人可改维(从基线预填,无幻象风险,恒纳入对照)+ 2 个元素簇维(cluster,按勾选触发门控)
const CHANGE_DIMS = [
  { key: '权属主体', cur: () => form.rightHolder, base: b => b.rightHolder },
  { key: '主体层级', cur: () => form.subjectLevel, base: b => b.subjectLevel },
  { key: '权属类型', cur: () => canonTypes((form.rightTypes || []).join('、')), base: b => canonTypes(b.rightType) },
  { key: '责任部门', cur: () => form.respDept, base: b => b.respDept },
  { key: '管制属性', cur: () => form.regulated, base: b => b.regulated },
  { key: '来源识别(A–F)', cluster: 'source', cur: () => unionClusterAfter(b => b.sourceIdent, form.sourceIdent), base: b => canonTypes(b.sourceIdent) },
  { key: '信息关联(G–J)', cluster: 'relation', cur: () => unionClusterAfter(b => b.relationIdent, form.relationIdent), base: b => canonTypes(b.relationIdent) }
]
const norm = (v) => (v == null ? '' : String(v).trim())
// 生效对照维度:簇维仅当对应触发被勾选才纳入(分簇 diff)
const activeDims = computed(() => CHANGE_DIMS.filter(d =>
  !d.cluster
  || (d.cluster === 'source' && keepSourceCluster.value)
  || (d.cluster === 'relation' && keepRelationCluster.value)))
const changeDiff = computed(() => {
  if (form.registerType !== '确权变更' || !changeBaseline.value || isDataAdd.value) return []
  const b = changeBaseline.value
  return activeDims.value
    .map(d => ({ key: d.key, before: norm(d.base(b)), after: norm(d.cur()) }))
    .filter(x => x.before !== x.after)
})
const changeUnchanged = computed(() => {
  if (form.registerType !== '确权变更' || !changeBaseline.value || isDataAdd.value) return 0
  return activeDims.value.length - changeDiff.value.length
})
// 逐表来源/关联明细改动(对照平台预填快照 _pf):系统级 A–F/G–J 不再做并集 diff,逐表真实改动由此汇总 + 表2 留痕
const PF_KEYS = ['sourceType', 'sourceSubject', 'sourceDesc', 'gSubject', 'hSubject', 'iSubject', 'jSubject', 'gFlag', 'hFlag', 'iFlag', 'jFlag']
const changedTableRows = computed(() => {
  if (form.registerType !== '确权变更' || isDataAdd.value) return []
  return tableItems.value.filter(row => row._pf && PF_KEYS.some(k => row._pf[k] !== undefined && (row[k] || '') !== (row._pf[k] || '')))
})
const changeSummary = computed(() => {
  if (form.registerType !== '确权变更' || !changeBaseline.value) return ''
  if (!changeDiff.value.length) return `确权变更(触发:${form.changeTrigger || '未选'})—— 暂未检测到与原确权结论的差异,请核对是否确需变更`
  return `本次确权变更(触发:${form.changeTrigger || '未选'})共修改 ${changeDiff.value.length} 项:`
    + changeDiff.value.map(d => `${d.key}「${d.before || '空'}→${d.after || '空'}」`).join('、')
})
// 变更点引导(P1):各维度字段旁的内联标记 —— 已改(高亮原值)/维持原值(弱化),按维度键取
const dimMarks = computed(() => {
  const map = {}
  if (form.registerType !== '确权变更' || !changeBaseline.value || isDataAdd.value) return map
  const b = changeBaseline.value
  for (const d of activeDims.value) {
    const before = norm(d.base(b))
    map[d.key] = { changed: before !== norm(d.cur()), before }
  }
  return map
})
// R1 登记类型派生横幅:类型由资产确权状态唯一决定(已确权→变更,否则→初始),不做随手 radio。
// 已确权资产如确需"重新初始登记",经二次确认方可覆盖,杜绝误选造成重复确权。
async function requestInitialOverride() {
  try {
    await ElMessageBox.confirm(
      '该资产已存在确权结论。「重新初始登记」将作为全新初始确权提交,可能与既有确权结论重复或冲突;如只是信息发生变动,请用「确权变更」。确认要重新初始登记吗?',
      '重新初始登记确认', { type: 'warning', confirmButtonText: '确认重新初始登记', cancelButtonText: '取消' })
    form.registerType = '初始确权'
    changeTriggers.value = [] // 初始确权无变更触发(watch 同步 form.changeTrigger='')
  } catch (e) { /* 取消:维持确权变更 */ }
}
function revertToChange() {
  form.registerType = '确权变更'
}

// R3 渐进披露:确权变更按触发类型聚焦相关识别维度,其余折叠为"维持原值"(只折叠不隐藏)。
// 与后端 ConfirmMaterialRuleService.narrowForChange 同源:来源/新增→留来源;管理/监管→留关联;
// 其他/未知→保守全展开;到期→均不聚焦(仅核心,与后端材料收敛口径一致)。
const changeKnownTrigger = computed(() => {
  const t = form.changeTrigger || ''
  return ['来源', '新增', '管理', '监管', '到期'].some(k => t.includes(k)) || t === '其他'
})
const changeNarrowing = computed(() =>
  form.registerType === '确权变更' && !isDataAdd.value && !!form.changeTrigger && changeKnownTrigger.value && form.changeTrigger !== '其他')
const focusSource = computed(() => {
  if (!changeNarrowing.value) return true
  const t = form.changeTrigger
  return t.includes('来源') || t.includes('新增')
})
const focusRelation = computed(() => {
  if (!changeNarrowing.value) return true
  const t = form.changeTrigger
  return t.includes('管理') || t.includes('监管')
})
async function searchAssets(kw) {
  if (!kw) { assetOpts.value = []; return }
  assetSearching.value = true
  try {
    assetOpts.value = (await searchAssetCards(kw, 10)) || []
  } finally { assetSearching.value = false }
}
async function onAssetPicked(id) {
  const hit = assetOpts.value.find(a => a.assetId === id)
  pickedCard.value = hit || null
  if (hit) form.assetName = hit.assetName || hit.assetId
  // 顺序:先平台带出,再按确权状态(确权变更时以已有确权结论现值预填覆盖)
  if (id) { await onAutofill(true); await checkAssetConfirmState(id) }
}

// 主动入口「发起变更」:从列表带 assetId 进入,补齐选择器选项并复用选卡逻辑(自动定登记类型+基线)
async function initAssetEntry(assetId) {
  form.assetId = assetId
  try { assetOpts.value = (await searchAssetCards(assetId, 5)) || [] } catch (e) { /* 选择器选项兜底 */ }
  await onAssetPicked(assetId)
  if (!assetConfirmed.value) {
    ElMessage.warning('该资产尚未确权,已按「初始确权」进入;完成初始确权后方可发起确权变更')
  }
}

// 一键填充示例(测试/演示):对齐 test/确权申请 手册 AST-001 全套数据
function fillDemo() {
  Object.assign(form, {
    assetId: 'AST-001', assetName: '客户用电信息表',
    rightTypes: ['数据资源持有权', '数据加工使用权'],
    rightHolder: '广东电网有限责任公司', respDept: '数字化部', subjectLevel: '分省公司',
    systemOwner: '张工', contactInfo: '020-88886666',
    registerType: '初始确权', applyMode: '常规', regulated: '管制业务',
    sourceIdent: ['A'], relationIdent: ['G', 'H'],
    sourceSubject: '用电客户', sourceLimit: '涉个人信息字段对外提供须脱敏并经客户授权',
    relationSubject: '国家能源局南方监管局;用电客户', equityRisk: '未经授权对外提供个人信息存在合规风险',
    privacyInfo: '用电客户个人信息,依据用户入网协议第X条已取得对外提供授权,范围限定于结算与征信场景',
    purpose: '营销域购售电数据确权(示例)'
  })
  // 表2 批量缓冲(示例):带库表后套用到争议表,演示逐表表2
  Object.assign(b2, {
    sourceSubject: '用电客户', sourceDesc: '涉个人信息字段对外提供须脱敏并经客户授权',
    hSubject: '用电客户个人信息,依据用户入网协议第X条已取得对外提供授权,范围限定于结算与征信场景',
    jSubject: '', riskDesc: '未经授权对外提供个人信息存在合规风险'
  })
  // autofill 取质量评分 + 自动带库表清单(AST-001 → 客户用电信息表);桩会回写申报主体为网公司,完成后恢复示例主体(分省申报口径)
  onAutofill(true).then(() => {
    form.rightHolder = '广东电网有限责任公司'
    form.respDept = '数字化部'
    if (disputedRows.value.length) applyBatchToDisputed(true)
  })
  ElMessage.success('已填充 AST-001 示例,可直接"下一步";材料文件在 test/确权申请 目录')
}

let lastAutofillId = ''
async function onAutofill(silent = false) {
  if (!form.assetId) { if (!silent) ElMessage.warning('请先填写关联资产ID'); return }
  autoLoading.value = true
  try {
    const r = await autofillConfirm(form.assetId)
    // P0:平台卡片字段一次带出(系统/负责人/电话等,不再让用户手填空字段)
    form.assetName = r.assetName; form.rightHolder = r.rightHolder; form.respDept = r.respDept
    form.systemName = r.systemName || form.systemName
    form.systemOwner = r.systemOwner || form.systemOwner
    form.contactInfo = r.contactInfo || form.contactInfo
    form.assetSecretLevel = r.secretLevel || form.assetSecretLevel
    form.region = r.region || form.region
    if (!form.rightTypes.length && r.rightType) form.rightTypes = [r.rightType]
    quality.value = r.qualityScore
    lastAutofillId = form.assetId
    await loadTablesFromMeta(true)
    ElMessage.success(silent ? `已实时同步元数据(${form.assetId})` : '已按平台卡片自动填充(系统/负责人/电话/A–J 等)')
  } finally { autoLoading.value = false }
}
// 资产ID 失焦即实时同步元数据(变化且非空才同步,避免重复)
function onAutoFillSilent() {
  if (form.assetId && form.assetId !== lastAutofillId) onAutofill(true)
}

// 步骤1 -> 2:暂存草稿,生成 A–J 应交材料清单
async function next0() {
  // 系统级入口守卫:未在左树选系统/库表则 assetName/systemName 为空,挡在前端(后端 CEC_ASSET_NAME NOT NULL)
  if (!form.systemName || !tableItems.value.length) {
    ElMessage.warning('请先在左侧「确权范围」树勾选:系统 → 一级功能模块 → 库表'); return
  }
  try {
    await formRef.value.validate()
  } catch (e) {
    identityPanel.value = ['id'] // 校验失败:展开身份摘要,避免折叠中的必填项错误不可见
    return
  }
  // 申请要素逐维必填(与后端 validateRegistration 同源,前移到填报,杜绝"材料全过却提交被拒")
  // 混选拦截:新增表(insert)与已确权表(update)编辑模式冲突,不可同单
  if (form.registerType === '确权变更' && mixedSelection.value) {
    ElMessage.warning('所选库表混含「新增」与「已确权」—— 编辑模式冲突,请分两单:新增表走「数据新增」,已确权表的修订另起一单'); return
  }
  if (form.registerType === '确权变更' && !form.changeTrigger) {
    ElMessage.warning(isDataAdd.value ? '请稍候,正在按所选新表锁定「数据新增」' : '确权变更须勾选至少一种变更触发(数据来源变更/管理要求变更/权益到期/其他)'); return
  }
  // P1 授权影响门禁:有在用对外授权且未确认处置 → 拦截
  if (changeMode.value && authImpact.value && authImpact.value.hasActive && !ackAuthImpact.value) {
    ElMessage.warning('本系统存在在用对外授权,本次变更须先确认"已知悉并将按建议处置受影响授权"方可提交')
    return
  }
  if (!form.sourceIdent.length) { ElMessage.warning('库表来源类型未识别,请到对应库表「编辑表2」指定来源类型(A–F)'); return }
  // 表2 逐表完整性(方案A):每张争议表必填项需齐(B–F→来源主体;H→隐私关联;J→协议关联)
  if (pendingT2Rows.value.length) {
    const r = pendingT2Rows.value[0]
    ElMessage.warning(`「${r.tableName || r.tableCode}」等 ${pendingT2Rows.value.length} 张表的表2 信息未填全(B–F来源主体 / H隐私关联 / J协议关联),请点「编辑表2」补全`)
    openT2(r); return
  }
  // 始终保存(首次插入 / 已有则按 applyId 更新草稿),确保填报修正(如 H 隐私说明)落库
  saving.value = true
  try {
    const firstSave = !applyId.value
    const payload = {
      ...form,
      ...(applyId.value ? { applyId: applyId.value } : {}),
      // 多权属类型合并一份申请(评审8.1):拼接保存,兼容单类型
      rightType: form.rightTypes.join('、'),
      sourceIdentification: form.sourceIdent.join(','),
      relationIdentification: form.relationIdent.join(','),
      involvesThirdParty: disputedRows.value.length > 0,
      reConfirm: form.registerType === '确权变更',
      thirdPartyInfo: disputedRows.value.length ? `涉第三方/敏感库表 ${disputedRows.value.length} 张(逐表明细见表级清单 表2 字段)` : '',
      // P2 确权变更版本化留痕:基线引用 + 版本号 + 摘要。
      //   数据新增(insert):新表自身首次确权 → 版本 v1(非系统基线+1),既有不动、不联动授权;
      //   来源/管理/到期(update):对已确权结论修订 → 基线版本 +1。
      ...(changeMode.value && changeBaseline.value ? {
        baselineRef: `${changeBaseline.value.sysName}#v${changeBaseline.value.version}` + (baselinePriorApplyId.value ? `@${baselinePriorApplyId.value}` : ''),
        changeVersion: isDataAdd.value ? 1 : (changeBaseline.value.version || 1) + 1,
        changeSummary: isDataAdd.value
          ? `数据新增:新增 ${tableItems.value.length} 张库表首次确权登记(既有已确权库表不动,不联动授权)`
          : changeSummary.value
      } : {})
    }
    applyId.value = await saveConfirmDraft(payload)
    // 表级清单全量同步(后端按 applyId 删后插);剥离前端态字段(sourceChannel/existTable),避免后端未知属性
    if (tableItems.value.length) {
      const items = tableItems.value.map(({ sourceChannel, existTable, _pf, authTime, confirmed,
        sourceAttachment, checkAttachment, privacyAttachment, busSecretAttachment, equityAttachment,
        authorized, authId, authScope, authStatus, ...rest }) => rest)
      await saveTableItems(applyId.value, items)
      if (firstSave) ElMessage.success(`已保存 ${items.length} 张表级清单(M02)`)
    }
    loadConsolidation()
    buildChecklist()
    // 先从平台元数据同步已上传材料(命中项免上传),并回填清单已上传状态(含再入 step1 不丢失)
    await doSyncPlatform(true)
    if (!firstSave && (checkReport.value || aiMatResult.value)) needRecheck.value = true // 申请要素已更新 → 需重新校验
  } finally { saving.value = false }
  step.value = 1
}

// 按材料名把已上传材料回填到清单行(done/materialId/fileName),避免再入 step1 时上传状态丢失
async function syncChecklistUploaded() {
  if (!applyId.value) return
  const mats = await listMaterialByApply(applyId.value) || []
  for (const row of checklist.value) {
    const m = mats.find(x => {
      const mn = x.materialName || ''
      return mn === row.name || mn.includes(row.name) || row.name.includes(mn)
    })
    if (m) {
      row.done = true
      if (m.materialId) row.materialId = m.materialId
      if (m.fileName) row.fileName = m.fileName
      row.fileUrl = m.fileUrl || ''                    // 有原件字节才有 fileUrl(平台同步无字节则空)
      row.source = m.source || row.source || '用户上传' // 标记来源:平台同步 / 用户上传
    }
  }
}

// 先从数据资产管理平台元数据(AU_TABLE_META_DATA)同步已上传材料:平台命中项自动登记免上传,仅补全缺口
async function doSyncPlatform(silent = false) {
  if (!applyId.value) { if (!silent) ElMessage.warning('请先完成步骤1暂存申请'); return }
  syncing.value = true
  try {
    syncReport.value = await syncPlatformMaterials(applyId.value)
    await syncChecklistUploaded() // 把平台同步登记的材料回填到清单行(done + source=平台同步)
    if (!silent) {
      const n = syncReport.value?.syncedCount || 0
      if (n > 0) ElMessage.success(`已从平台同步 ${n} 项已上传材料,仅需补全缺口`)
      else ElMessage.info('平台元数据暂无可同步的已上传材料,请按清单补全')
    }
    if (checkReport.value || aiMatResult.value) needRecheck.value = true // 材料集变化 → 需重新校验
  } catch (e) {
    if (!silent) ElMessage.warning('平台材料同步暂不可用,请按清单手动补全:' + (e?.response?.data?.message || e?.message || ''))
  } finally { syncing.value = false }
}

// 应交清单由后端可配置规则(单一真源)按 场景×触发条件(A–J/涉三方)生成,前端仅渲染。
// 触发判定与后端 ConfirmMaterialRuleService 一致:ALWAYS 常交 / TABLE2 涉三方 / SOURCE-RELATION 选中码。
function buildChecklist() {
  if (!materialRules.value.length) { buildChecklistFallback(); return }
  const t2 = needTable2.value
  const hit = (r) => {
    if (r.triggerType === 'ALWAYS') return true
    if (r.triggerType === 'TABLE2') return t2
    if (r.triggerType === 'SOURCE') return form.sourceIdent.includes(r.triggerCode)
    if (r.triggerType === 'RELATION') return form.relationIdent.includes(r.triggerCode)
    return false
  }
  let rules = materialRules.value.filter(hit)
  // 确权变更:按变更触发类型收敛为差异项(与后端 narrowForChange 一致),不必重复提交全套
  if (form.registerType === '确权变更' && form.changeTrigger) rules = narrowForChange(rules, form.changeTrigger)
  checklist.value = rules.map((r, i) => ({
    code: r.triggerCode || (r.triggerType === 'TABLE2' ? '表2' : '核心'),
    name: r.materialName,
    m: r.triggerLabel || r.evidenceType || '材料',
    required: r.required, detail: r.detail,
    id: 'ck' + i, done: false
  }))
}
// 确权变更应交材料收敛(镜像后端 ConfirmMaterialRuleService.narrowForChange):核心表单/凭证(ALWAYS)始终保留;
// 来源类仅当触发涉来源、关联类仅当触发涉管理要求时保留;表2仅当仍保留来源/关联差异材料时保留。
function narrowForChange(rules, trigger) {
  // 容错匹配(与后端一致):触发可组合表述,按关键词判定;未识别触发→保守不收敛返回全集
  const known = ['来源', '新增', '管理', '监管', '到期'].some(k => trigger.includes(k)) || trigger === '其他'
  if (!known) return rules
  const keepSrc = trigger.includes('来源') || trigger.includes('新增') || trigger === '其他'
  const keepRel = trigger.includes('管理') || trigger.includes('监管') || trigger === '其他'
  const out = []; let anyDiff = false
  for (const r of rules) {
    if (r.triggerType === 'ALWAYS') out.push(r)
    else if (r.triggerType === 'SOURCE') { if (keepSrc) { out.push(r); anyDiff = true } }
    else if (r.triggerType === 'RELATION') { if (keepRel) { out.push(r); anyDiff = true } }
  }
  if (anyDiff) out.push(...rules.filter(r => r.triggerType === 'TABLE2'))
  return out
}

// 规则接口不可用时的内置兜底(与默认规则一致),保证向导可离线生成清单
function buildChecklistFallback() {
  const base = [{ code: '表1', name: '《表1 数据确权信息清单(系统级)》', m: '表1' }, { code: '证明', name: '数据确权证明材料(权属/来源凭证)', m: '证明材料' }]
  if (needTable2.value) base.push({ code: '表2', name: '《表2 数据确权信息清单(涉及第三方权益)》', m: '表2' })
  const picked = [
    ...sourceOpts.filter(s => form.sourceIdent.includes(s.v)),
    ...relationOpts.filter(r => form.relationIdent.includes(r.v))
  ].map(o => ({ code: o.v, name: o.m, m: o.t }))
  checklist.value = [...base, ...picked].map((x, i) => ({ ...x, id: 'ck' + i, done: false }))
}

async function registerMaterial(row) {
  await uploadMaterial({
    applyId: applyId.value, materialName: row.name, materialType: row.m,
    fileUrl: `/files/dev/${applyId.value}-${row.code}.pdf`, owner: form.rightHolder, source: '用户上传'
  })
  row.done = true
  row.source = '用户上传'
  if (checkReport.value || aiMatResult.value) needRecheck.value = true // 已校验过又改材料 → 置脏
  ElMessage.success('已登记')
}

async function onUploadFile(row, file) {
  if (!file || !file.raw) return
  const fd = new FormData()
  fd.append('file', file.raw)
  fd.append('applyId', applyId.value)
  fd.append('materialName', row.name)
  fd.append('materialType', row.m)
  fd.append('owner', form.rightHolder || '')
  const mid = await uploadMaterialFile(fd) // 后端格式验证不通过会抛错(拦截器toast);返回 materialId
  row.done = true
  row.source = '用户上传'        // 本地上传原件:覆盖/补全为用户上传
  row.materialId = mid          // 回填,使本行可在线预览
  row.fileName = file.raw.name  // 真实文件名,预览按扩展名渲染
  if (checkReport.value || aiMatResult.value) needRecheck.value = true // 已校验过又改材料 → 置脏
  ElMessage.success('原件已上传并通过格式验证')
}

function previewMaterial(row) {
  if (row.materialId) openFilePreview(materialFileUrl(row.materialId), row.fileName)
}
function onExportCheck() {
  if (applyId.value) window.open(materialExportUrl(applyId.value), '_blank')
}

async function next1() {
  materials.value = await listMaterialByApply(applyId.value) || []
  // ②→③ 门禁:所有"必填"应交项须先登记,挡住明显漏交(应交清单 required 单一真源)
  const missRequired = checklist.value.filter(c => c.required === '必填' && !c.done)
  if (missRequired.length) { ElMessage.warning('必填材料未齐,请先登记/上传:' + missRequired.map(c => c.name).join('、')); return }
  if (materials.value.length === 0) { ElMessage.warning('请先至少登记一项材料'); return }
  step.value = 2
}

async function runCheck() {
  checking.value = true
  try {
    checkReport.value = await runMaterialCheck(applyId.value) // 后端规则校验:缺失/不合规自动识别
    materials.value = await listMaterialByApply(applyId.value) || []
    needRecheck.value = false // 校验已刷新,清除脏标
    if (checkReport.value.allPass) ElMessage.success('材料校验全部通过,可推送审核')
    else ElMessage.warning(`校验未通过:缺失 ${checkReport.value.missing.length} / 不合规 ${checkReport.value.failCount} 项,请就地补充后重新校验`)
  } finally { checking.value = false }
}

// 闭环:校验失败项就地补充原件 → 置脏 → 引导重新校验,直至通过
async function onFixUpload(item, file) {
  if (!file || !file.raw) return
  const fd = new FormData()
  fd.append('file', file.raw)
  fd.append('applyId', applyId.value)
  fd.append('materialName', item.name)
  fd.append('materialType', '补充')
  fd.append('owner', form.rightHolder || '')
  await uploadMaterialFile(fd) // 后端格式验证不过会抛错
  needRecheck.value = true
  materials.value = await listMaterialByApply(applyId.value) || []
  ElMessage.success(`已补充「${item.name}」,请点上方「重新校验」`)
}

async function next2() {
  submitting.value = true
  try {
    // 提交前固化 AI 校验结果快照(材料AI校验 + 规则完整性 + 权益归集),供人工预审完整复核·可追溯
    try {
      const snapshot = {
        checkedAt: new Date().toISOString(),
        qualityScore: quality.value,
        materialCheck: aiMatResult.value || null,
        ruleReport: checkReport.value || null,
        consolidation: consolidation.value || null
      }
      await saveAiSnapshot(applyId.value, JSON.stringify(snapshot))
    } catch (e) { /* 快照失败不阻断提交 */ }
    await pushMaterialReview(applyId.value) // 后端门禁:校验全通过才提交审核,否则抛错(拦截器toast缺失/不合规)
    step.value = 3
  } finally { submitting.value = false }
}

function goProgress() { router.push('/dpr/confirm/history') }
function reset() {
  step.value = 0; applyId.value = ''; applyNo.value = ''; quality.value = null
  checklist.value = []; materials.value = []
  Object.assign(form, { assetId: '', assetName: '', systemName: '', rightTypes: [], rightHolder: '', respDept: '', subjectLevel: '', systemOwner: '', contactInfo: '', assetSecretLevel: '', region: '', registerType: '初始确权', changeTrigger: '', applyMode: '常规', purpose: '', thirdPartyInfo: '', sourceSubject: '', sourceLimit: '', relationSubject: '', equityRisk: '', privacyInfo: '', sourceIdent: [], relationIdent: [] })
  assetConfirmState.value = ''; assetConfirmed.value = false; changeBaseline.value = null
  baselineFromReal.value = false; baselineCards.value = []; baselineSummaries.value = []; baselinePriorApplyId.value = ''
  changeTriggers.value = []; ackAuthImpact.value = false; authImpact.value = null
  identityPanel.value = []
  tableItems.value = []; tableItemText.value = ''
  // 校验闸门态必须一并清零,否则"再发起一笔"会继承上一笔的"已校验通过",新表单未校验即点亮提交(闭环失效)
  checkReport.value = null; aiMatResult.value = null; needRecheck.value = false; aiAck.value = []
}
</script>

<style scoped>
.wz-steps { max-width: 900px; margin: 8px auto 20px; }
.wz-body { min-height: 320px; }
.form-tip { font-size: 12px; color: var(--prm-color-text-weak); line-height: 1.6; }
.pf-mark { margin-top: 3px; }
/* 经营权归集判定结果卡片:结论做主角,法条折叠 */
.consol-card { margin-top: 10px; padding: 12px 14px; border-radius: 8px; background: var(--prm-color-bg); border: 1px solid var(--prm-color-bg); border-left: 4px solid var(--prm-color-text-weak); }
.consol-card.op-yes { border-left-color: #36b21d; background: #f3faf1; }
.consol-card.op-no { border-left-color: var(--prm-color-danger); background: #fef4f4; }
.consol-card.op-depend { border-left-color: #126cfd; background: #f2f7ff; }
.consol-hero { font-size: 15px; font-weight: 600; color: var(--prm-color-text); display: flex; align-items: center; }
.consol-plain { margin-top: 6px; font-size: 13px; color: var(--prm-color-text); line-height: 1.6; }
.consol-sub { margin-top: 4px; font-size: 12px; color: var(--prm-color-text-weak); }
.consol-basis { margin-top: 4px; }
.consol-basis :deep(.el-collapse) { border: none; }
.consol-basis :deep(.el-collapse-item__header) { height: 26px; line-height: 26px; border: none; background: transparent; }
.consol-basis :deep(.el-collapse-item__wrap) { border: none; background: transparent; }
.consol-basis :deep(.el-collapse-item__content) { padding-bottom: 6px; }
.approve-chain { max-width: 880px; margin: 8px auto 0; }
/* R3 维持原值折叠区:与 form-item 行距对齐,弱化呈现 */
.change-fold { margin-bottom: 18px; border-top: none; border-bottom: none; }
.change-fold :deep(.el-collapse-item__header) { font-size: 13px; height: 36px; line-height: 36px; }
/* R2 资产身份摘要折叠条:弱化的只读身份摘要,展开可编辑平台带出字段 */
.identity-card { margin-bottom: 18px; border: 1px solid var(--el-border-color-lighter); border-radius: 6px; padding: 0 12px; background: #fafcff; }
.identity-card :deep(.el-collapse-item__header) { height: 40px; line-height: 40px; border-bottom: none; }
.identity-card :deep(.el-collapse-item__wrap) { border-bottom: none; background: transparent; }
/* 系统级确权:左范围树 + 右表单(master-detail) */
.step1-2col { display: flex; gap: 16px; align-items: flex-start; }
.step1-tree { flex: 0 0 280px; }
.step1-form { flex: 1; min-width: 0; }
@media (max-width: 1100px) { .step1-2col { flex-direction: column; } .step1-tree { flex: none; width: 100%; } }
</style>
