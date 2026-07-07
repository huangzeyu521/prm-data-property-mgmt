<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <PageNote>注:一站式确权申请——填申请 → 传材料 → 校验 → 提交，一条流程办完，无需在菜单间来回切换(表2 权益风险列随申请一并编制提交)。本页为申报人填报入口(对齐 35 号文确权流程步骤20);确权认定工作由总部数字化部按季度/通知周期性发起,提交后进入下游审核。</PageNote>

    <!-- 步骤条可点击回跳(DUI/Element 官方认可交互:点击已完成步骤跳转,el-step 无自身 click emit,@click 原生透传);
         仅允许跳到"已完成"步骤(idx < 当前步),终态(提交完成页 step3)不可点击进出,前进仍须走「下一步」校验闸门。 -->
    <el-steps :active="step" finish-status="success" align-center class="wz-steps">
      <el-step title="填写申请" description="表1(系统级)+表2(逐表)·A–F来源/G–J关联识别" :class="{ 'wz-step-clickable': stepJumpable(0) }" @click="onStepClick(0)" />
      <el-step title="上传材料" description="按 A–J 应交清单" :class="{ 'wz-step-clickable': stepJumpable(1) }" @click="onStepClick(1)" />
      <el-step title="材料校验" description="完整性自检 + AI预检" :class="{ 'wz-step-clickable': stepJumpable(2) }" @click="onStepClick(2)" />
      <el-step title="提交审核" description="进入三级审批" />
    </el-steps>

    <div class="wz-body">
      <!-- 步骤1:填写申请 -->
      <el-card v-show="step === 0" shadow="never">
        <el-alert v-if="!applyId && isDemoEnv" type="info" :closable="false" style="margin-bottom:10px;max-width:640px">
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
            确权说明(选填)         → 表1 说明                        落库:复用 CEC_PURPOSE(原用途说明列)
          【表2 逐表·每张争议表一行(库表清单+编辑表2抽屉)】
            模式/数据表/来源类型/来源主体/限制摘要/凭证/关联类型/关联主体/关联附件/权益风险
                                  → 表2 各列                       来源:平台AU_TABLE_META_DATA 预填·可改
          【PRM 内部·非表1(均有 load-bearing 理由,经标注)】
            权属类型主张          → 辅助预判·默认全选三权(可改·非必填);经营权终以 事实推导+表4 审核为准
            管制属性             → 经营权归集输入(regulated)→ 5规则 → 经营权/权益卡片/表4
            责任部门             → 平台补充上下文(MGT_MNG_DEPT),非表1
            所属业务系统(只读)     → 已删冗余框:系统名已在确权范围 tag + 折叠标题出现,不再第三处重复
          【已删(单卡残肢/孤儿/违规):关联卡片搜索·元数据自动填充·资产名称·表2批量套用·库表手工批量导入·登记自动判定override·申请模式一事一议(确权无此划分,属授权概念)】
        -->
        <el-form ref="formRef" :model="form" :rules="rules" label-width="150px" :disabled="!!applyId">
          <el-form-item label="确权范围">
            <div v-if="form.systemName" style="width:100%">
              <span style="margin-right:8px" class="prm-c-primary">{{ form.systemName }}</span>
              <span v-if="tableItems.length" style="margin-right:8px" class="prm-c-success">已选 {{ tableItems.length }} 张库表</span>
              <span :class="'prm-c-' + ((changeMode ? 'warning' : 'info') || 'primary')">{{ changeMode ? '确权变更' : '初始确权' }}</span>
              <div v-if="changeMode" class="form-tip">范围由左侧范围树(选系统 → 一级功能模块 → 库表)带入;系统名/库表来自平台卡片元数据,只读。查找请用范围树顶部搜索框。</div>
              <div v-else class="form-tip">初始确权=系统级整体确权(35号文 表1 一系统一行):选定系统后,其全部未确权库表整体纳入,不可单挑子集;系统名/库表来自平台卡片元数据,只读。</div>
            </div>
            <el-empty v-else :image-size="48" :description="changeMode ? '请在左侧「确权范围」树勾选:系统 → 一级功能模块 → 库表(数据资产卡片);一份申请限定在一个系统内' : '请在左侧「确权范围」树选择一个系统(系统级整体确权);该系统下全部未确权库表将整体纳入,一份申请限定在一个系统内'" />
            <span v-if="quality !== null" style="margin-top:6px" :class="'prm-c-' + ((quality < 80 ? 'danger' : 'success') || 'primary')">
              元数据质量评分 {{ quality }}{{ quality < 80 ? ' · 低于80,提交将被自动驳回(请先治理元数据)' : '' }}
            </span>
          </el-form-item>
          <!-- A区·定性:登记类型由「进入的菜单」唯一确定(初始确权/确权变更两个独立菜单),并由左树 status 过滤强制保证,只读 -->
          <el-form-item label="登记类型">
            <el-alert v-if="changeMode" type="info" :closable="false" show-icon
              title="本次:确权变更 · 对已确权库表既有结论的修订(附录F §3.3.2 重新确权)" />
            <el-alert v-else type="success" :closable="false" show-icon
              title="本次:初始确权 · 左侧范围树仅列未确权库表(若需修订已确权,请走「确权变更申请」菜单)" />
          </el-form-item>
          <!-- 确权变更差异化(附录F §3.3.2):变更类型 = 由所选库表确权状态派生约束,单选(数据新增 insert / 来源·管理·到期 update 三选一) -->
          <el-form-item v-if="form.registerType === '确权变更'" required>
            <template #label><span style="color:var(--prm-color-danger)">*</span> 变更触发类型</template>
            <div style="width:100%">
              <!-- 混选拦截:新增表(insert)与已确权表(update)编辑模式冲突,不可同单——就地一键裁剪化解,避免死锁在此(曾致"变更类型不触发") -->
              <el-alert v-if="mixedSelection" type="error" :closable="false" show-icon>
                <template #title>所选库表混含「未确权」与「已确权」—— 二者编辑模式不同(未确权=首次登记 insert / 已确权=修订基线 update),不能在同一份变更申请处理。</template>
                <div style="margin-top:6px">
                  <el-button size="small" @click="keepOnlyConfirmedSel">仅保留已确权表(继续本次变更)</el-button>
                  <el-button size="small" @click="keepOnlyNewSel">仅保留未确权表(转数据新增)</el-button>
                </div>
              </el-alert>
              <!-- 数据新增:全为新增表 → 锁定,只读(insert) -->
              <el-alert v-else-if="isDataAdd" type="warning" :closable="false" show-icon
                title="变更类型:数据新增(所选均为未确权新表,自动锁定) —— 新表首次走完整确权登记,既有已确权库表不改动,不联动下游授权。" />
              <!-- 已确权表:来源/管理/到期 单选(update,三选一) -->
              <template v-else-if="allConfirmedSel">
                <el-radio-group v-model="changeTriggerSingle">
                  <el-radio v-for="o in CHANGE_TRIGGER_OPTS" :key="o.v" :value="o.v" border style="margin:0 8px 6px 0">
                    {{ o.v }}<span style="color:var(--prm-color-text-weak);font-size:12px;margin-left:4px">{{ o.t }}</span>
                  </el-radio>
                </el-radio-group>
                <div class="form-tip">对已确权库表的修订三选一(单选);勾中哪簇即只比对/收敛哪簇,未勾簇维持原值。</div>
              </template>
              <el-empty v-else :image-size="36" description="请先在左侧范围树勾选库表 —— 变更类型据所选库表确权状态自动判定(新增表→数据新增;已确权表→来源/管理/到期单选)" />
            </div>
          </el-form-item>
          <!-- 权益期限维(P0′,35号文"权益到期"触发):变更前期限只读带出,新期限申报 → 制卡落新卡 validDate -->
          <el-form-item v-if="form.registerType === '确权变更' && keepValidityCluster && !isDataAdd">
            <template #label>
              <span v-if="changeTriggers.includes('权益到期')" style="color:var(--prm-color-danger)">*</span> 申报权益有效期
            </template>
            <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
              <span style="font-size:13px;color:var(--prm-color-text-weak)">
                变更前(上一版卡片最早到期):<b style="color:var(--prm-color-text-secondary)">{{ baselineValidBefore || '无固定期限' }}</b>
                <el-icon style="margin:0 2px;vertical-align:-2px"><Right /></el-icon>
              </span>
              <el-date-picker v-model="form.validDate" type="date" value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="变更后新有效期(续期至)" style="width:200px" />
              <span class="form-tip" style="margin:0">到期续期须申报新期限并配套提交新签协议/凭证;不填=维持原期限</span>
            </div>
          </el-form-item>
          <!-- P1′ 期望变更点引导:按勾选触发列出"本类型应发生变更的对象",逐项 已变更/未变更(结果导向 diff 之前先给期望) -->
          <el-form-item v-if="form.registerType === '确权变更' && expectedPoints.length" label="期望变更点">
            <div style="width:100%">
              <div v-for="p in expectedPoints" :key="p.label" style="font-size:13px;line-height:1.9">
                <span style="width:64px;text-align:center" :class="'prm-c-' + ((p.met ? 'success' : 'warning') || 'primary')">
                  {{ p.met ? '已变更' : '未变更' }}
                </span>
                <span style="margin-left:8px">{{ p.label }}</span>
                <span v-if="!p.met" style="margin-left:6px;color:var(--prm-color-text-weak);font-size:12px">{{ p.tip }}</span>
              </div>
              <el-alert v-if="expectedNoneMet" type="warning" :closable="false" show-icon style="margin-top:6px"
                :title="`已勾选「${form.changeTrigger}」,但对应的期望变更对象均未修改 —— 请核对是否勾错触发类型,或先完成相应修订再提交。`" />
            </div>
          </el-form-item>
          <!-- 数据新增(insert):无 baseline diff(既有不动),提示新表首次走整套 -->
          <el-alert v-if="form.registerType === '确权变更' && isDataAdd" type="success" :closable="false" show-icon style="margin-bottom:18px"
            :title="`数据新增 · 新表首次确权登记(共 ${tableItems.length} 张新表)`">
            <div style="font-size:12px;color:var(--prm-color-text-secondary);margin-top:4px">
              <!-- P1′ 集合级前后对照:数据新增的"变更对象"是系统确权覆盖范围,给 前N张 → 后N+M张 摘要 -->
              <div v-if="baselineTableCount" style="margin-bottom:2px">
                变更前:系统内已确权 <b>{{ baselineTableCount }}</b> 张库表
                → 变更后:<b>{{ baselineTableCount + tableItems.length }}</b> 张(本次新增 <b class="prm-c-warning">{{ tableItems.length }}</b> 张首次登记)。
              </div>
              新表将首次走完整识别(A–F 来源 / G–J 关联 / 表2 / 表4 权益 / 生成新权益卡片);系统内既有已确权库表全部不动;新表尚未授权,不联动下游授权。
            </div>
          </el-alert>
          <!-- P0 变更前后对照(仅 update 模式):落在编辑区之前,先给基线语境(一眼看见改了什么/没改什么) -->
          <div v-if="form.registerType === '确权变更' && changeBaseline && !isDataAdd" style="margin-bottom:18px">
            <div style="font-size:12px;color:var(--prm-color-text-weak);margin-bottom:4px">
              基线:<b style="color:var(--prm-color-text-secondary)">{{ changeBaseline.sysName }} · 确权时间 {{ changeBaseline.authTime }} · v{{ changeBaseline.version }}</b>
              <span style="margin-left:8px" class="prm-c-primary">本次变更生成 v{{ (changeBaseline.version || 1) + 1 }}(可追溯·旧权益卡片转历史)</span>
              <span style="margin-left:6px" :class="'prm-c-' + ((baselineFromReal ? 'success' : 'info') || 'primary')">
                {{ baselineFromReal ? '底版=上一版真实确权(表3行+权益卡片)' : '底版=目录合成(无上一版确权,退回身份级)' }}
              </span>
            </div>
            <el-alert :type="changeDiff.length ? 'warning' : 'info'" :closable="false" :title="changeSummary" />
            <el-table v-if="changeDiff.length" :data="changeDiff" border size="small" style="margin-top:8px;max-width:840px">
              <el-table-column prop="key" label="变更维度" width="150" />
              <el-table-column label="原值(已有确权)" min-width="180">
                <template #default="{ row }"><span style="color:var(--prm-color-text-weak);text-decoration:line-through">{{ row.before || '空' }}</span></template>
              </el-table-column>
              <el-table-column label="新值(本次变更)" min-width="180">
                <template #default="{ row }"><span class="prm-c-warning" style="font-weight:600">{{ row.after || '空' }}</span></template>
              </el-table-column>
            </el-table>
            <div class="prm-c-success" style="font-size:12px;margin-top:4px">另有 {{ changeUnchanged }} 项维持原值(无需改动)。</div>
            <!-- 逐表来源/关联对照(选中库表·数据表名称级):系统级并集不再对照(避免子集选择的幻象删减 A、E→A / 并集掺旧 A→A、B);
                 来源簇下每张选中表恒显示「来源类型」基线→当前+状态(未改=A→A 维持),改动标已修订。 -->
            <div v-if="changedTableDetail.length" style="margin-top:8px">
              <div style="font-size:12px;color:var(--prm-color-text-weak);margin-bottom:4px">逐表来源/关联对照(选中库表·可在下方库表清单「编辑表2」修订):</div>
              <el-table :data="changedTableDetail" border size="small" style="max-width:900px">
                <el-table-column prop="tableName" label="数据表名称" min-width="150" show-overflow-tooltip />
                <el-table-column prop="dim" label="变更项" width="130" />
                <el-table-column label="原值(已有确权)" min-width="150">
                  <template #default="{ row }"><span :style="row.changed ? 'color:var(--prm-color-text-weak);text-decoration:line-through' : 'color:var(--prm-color-text-secondary)'">{{ row.before }}</span></template>
                </el-table-column>
                <el-table-column label="新值(本次变更)" min-width="150">
                  <template #default="{ row }"><span :class="row.changed ? 'prm-c-warning' : ''" :style="row.changed ? 'font-weight:600' : 'color:var(--prm-color-text-secondary)'">{{ row.after }}</span></template>
                </el-table-column>
                <el-table-column label="状态" width="92" align="center">
                  <template #default="{ row }"><span :class="'prm-c-' + (row.changed ? 'warning' : 'success')">{{ row.changed ? '已修订' : '维持原值' }}</span></template>
                </el-table-column>
              </el-table>
            </div>
            <!-- 「上一版权益卡片(底版卡)」只读罗列已删:系统级噪音且被下方「权益变更对照(表4)」覆盖;
                 baselineCards 数据保留,继续喂 table4Diff 对照 + 变更前最早到期提示。 -->
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
              <!-- 「所属业务系统」只读框已删:系统名已在顶部「确权范围」tag + 本折叠标题(identitySummary)出现,避免三处重复 -->
              <el-form-item label="系统负责人" prop="systemOwner">
                <el-input v-model="form.systemOwner" placeholder="平台带出·可改(MGT_USER 卡片责任人)">
                  <template #suffix><span v-if="form.systemOwner" class="prm-c-success" style="font-size:12px">平台带出</span></template>
                </el-input>
              </el-form-item>
              <el-form-item label="联系方式" prop="contactInfo">
                <el-input v-model="form.contactInfo" placeholder="平台带出·可改(MGT_USER_PHONE 责任人电话)">
                  <template #suffix><span v-if="form.contactInfo" class="prm-c-success" style="font-size:12px">平台带出</span></template>
                </el-input>
              </el-form-item>
              <el-form-item label="责任部门">
                <el-input v-model="form.respDept" placeholder="平台带出·可改(MGT_MNG_DEPT 管理部门;平台补充·非表1)">
                  <template #suffix><span v-if="form.respDept" class="prm-c-success" style="font-size:12px">平台带出</span></template>
                </el-input>
                <div v-if="dimMarks['责任部门']" class="form-tip" style="margin-top:2px">
                  <span v-if="dimMarks['责任部门'].changed" class="prm-c-warning">已修改 · 原值「{{ dimMarks['责任部门'].before || '空' }}」</span>
                  <span v-else class="prm-c-info">维持原值</span>
                </div>
              </el-form-item>
            </el-collapse-item>
          </el-collapse>
          <el-form-item label="申报权属主体" prop="rightHolder">
            <el-input v-model="form.rightHolder" placeholder="平台带出·可改(MGT_UNIT 管理单位 = 表1 公司主体);最终权属以审核为准">
              <template #suffix><span v-if="form.rightHolder" class="prm-c-success" style="font-size:12px">平台带出</span></template>
            </el-input>
            <div class="form-tip">分省上报的数据产权,确权通过后统一归口中国南方电网有限责任公司</div>
            <div v-if="dimMarks['权属主体']" class="form-tip" style="margin-top:2px">
              <span v-if="dimMarks['权属主体'].changed" class="prm-c-warning">已修改 · 原值「{{ dimMarks['权属主体'].before || '空' }}」</span>
              <span v-else class="prm-c-info">维持原值</span>
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
          <!-- 「表1 系统级并集 + 经营权归集判定」已下移到库表清单之后(因果顺序:逐表表2 → 系统级并集 → 经营权判定);见下方 -->
          <!-- 表1⑨「说明」(系统级·选填):复用 CEC_PURPOSE 列落库(原用途说明列已闲置),对齐 35 号文附录C 表1 说明列 -->
          <el-form-item label="确权说明">
            <el-input v-model="form.purpose" type="textarea" :rows="2" maxlength="1000" show-word-limit
              placeholder="表1 说明(选填):本系统确权的特殊情况/补充备注,如权属特殊约定、跨主体说明等" />
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
                <span v-if="disputedRows.length" :class="'prm-c-' + ((pendingT2Rows.length ? 'warning' : 'success') || 'primary')">
                  {{ pendingT2Rows.length ? `表2 待填 ${pendingT2Rows.length} 张` : '表2 已全部填写' }}
                </span>
              </div>
              <div class="form-tip">库表来自数据资产管理平台·由左侧范围树带入,<b>不可手工添加</b>(增删请在左树勾选/取消)。实例/schema/表代码/表名/密级平台只读;来源类型/来源主体/G–J/表2 可点行尾「编辑表2」逐表调整。</div>
            </div>
          </el-form-item>
          <el-empty v-if="!tableItems.length" :description="changeMode ? '尚无库表 — 请在左侧「确权范围」树勾选:系统 → 一级功能模块 → 库表' : '尚无库表 — 请在左侧「确权范围」树选择一个系统(系统级整体确权)'" :image-size="60" />
          <div v-else class="t2-list">
            <!-- 筛选条(规模化:系统级可能几百/几千张表):按来源类型/关联类型/密级/表2状态/关键字定位目标库表(如"B 类是哪几张") -->
            <div class="t2-filter">
              <el-select v-model="t2Filter.src" multiple collapse-tags collapse-tags-tooltip placeholder="来源类型 A–F" size="small" style="width:148px">
                <el-option v-for="s in sourceOpts" :key="s.v" :label="`${s.v} ${s.t}`" :value="s.v" />
              </el-select>
              <el-select v-model="t2Filter.rel" multiple collapse-tags collapse-tags-tooltip placeholder="信息关联 G–J" size="small" style="width:148px">
                <el-option v-for="r in relationOpts" :key="r.v" :label="`${r.v} ${r.t}`" :value="r.v" />
              </el-select>
              <el-select v-model="t2Filter.secret" clearable placeholder="密级" size="small" style="width:104px">
                <el-option v-for="s in secretOpts" :key="s" :label="s" :value="s" />
              </el-select>
              <el-select v-model="t2Filter.status" clearable placeholder="表2状态" size="small" style="width:106px">
                <el-option label="待填" value="待填" /><el-option label="已填" value="已填" /><el-option label="无需填" value="无需填" />
              </el-select>
              <el-input v-model="t2Filter.kw" clearable placeholder="搜 schema / 表代码 / 表名" size="small" style="width:180px" />
              <el-button size="small" @click="resetT2Filter">重置</el-button>
              <el-button size="small" type="primary" plain @click="t2Filter.status = '待填'">只看待填表2</el-button>
              <span class="form-tip t2-hit">命中 <b class="prm-c-primary">{{ t2Filtered.length }}</b> / {{ tableItems.length }} 张</span>
              <el-radio-group v-model="t2View" size="small">
                <el-radio-button label="compact">紧凑</el-radio-button>
                <el-radio-button label="full">表2 完整视图</el-radio-button>
              </el-radio-group>
            </div>

            <!-- 紧凑视图:核心列(官方表2列名);长文本截断悬浮,附件正本走「上传材料」步 -->
            <el-table v-if="t2View === 'compact'" :data="t2Paged" border size="small" style="margin-bottom:8px">
              <el-table-column type="index" label="序号" width="56" align="center" :index="rowSeq" />
              <el-table-column label="模式名称" width="112" show-overflow-tooltip><template #default="{ row }">{{ row.schemaName || '—' }}</template></el-table-column>
              <el-table-column label="数据表名称" min-width="180">
                <template #default="{ row }">
                  <div style="font-weight:600;color:var(--prm-color-text)">{{ row.tableName || row.tableCode }}</div>
                  <div style="font-size:12px;color:var(--prm-color-text-weak)">{{ row.schemaName }}.{{ row.tableCode }}<span v-if="row.instanceName"> · {{ row.instanceName }}</span></div>
                </template>
              </el-table-column>
              <el-table-column prop="secretLevel" label="密级" width="80" />
              <el-table-column v-if="changeMode" label="对外授权" width="88" align="center">
                <template #default="{ row }">
                  <el-tooltip v-if="row.authorized" placement="top" :content="`${row.authId} · ${row.authScope} · ${row.authStatus};本次变更将联动评估该授权`">
                    <span class="prm-c-danger">已授权</span>
                  </el-tooltip>
                  <span v-else style="color:var(--prm-color-text-disabled)">无</span>
                </template>
              </el-table-column>
              <el-table-column prop="sourceType" label="来源类型" width="112" />
              <el-table-column label="来源主体名称" min-width="106" show-overflow-tooltip><template #default="{ row }">{{ row.sourceSubject || '—' }}</template></el-table-column>
              <el-table-column label="来源权益限制摘要" min-width="130" show-overflow-tooltip><template #default="{ row }">{{ row.sourceDesc || '—' }}</template></el-table-column>
              <el-table-column label="信息关联类型(G–J)" width="128" align="center">
                <template #default="{ row }">
                  <span v-for="v in relLetters(row)" :key="v" style="margin:1px" :class="'prm-c-' + ((v === 'H' ? 'danger' : 'warning') || 'primary')">{{ v }}</span>
                  <span v-if="!relLetters(row).length" style="color:var(--prm-color-text-disabled)">—</span>
                </template>
              </el-table-column>
              <el-table-column label="权益风险" min-width="110" show-overflow-tooltip><template #default="{ row }">{{ row.riskDesc || '—' }}</template></el-table-column>
              <el-table-column label="表2状态" width="84" align="center">
                <template #default="{ row }"><span :class="'prm-c-' + ((rowTable2Status(row).type) || 'primary')">{{ rowTable2Status(row).text }}</span></template>
              </el-table-column>
              <el-table-column label="操作" width="118" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="rowDisputed(row)" link type="primary" size="small" @click="openT2(row)">编辑表2</el-button>
                  <el-button link type="danger" size="small" @click="removeTableItem(row)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 表2 完整视图:严格对齐指引《表2 数据确权信息清单(涉及第三方权益)》12 列,横向滚动,逐列原名供合规核对 -->
            <el-table v-else :data="t2Paged" border size="small" style="margin-bottom:8px">
              <el-table-column type="index" label="序号" width="56" align="center" :index="rowSeq" fixed="left" />
              <el-table-column label="系统名称" width="130" show-overflow-tooltip><template #default>{{ form.systemName || '—' }}</template></el-table-column>
              <el-table-column label="模式名称" width="120" show-overflow-tooltip><template #default="{ row }">{{ row.schemaName || '—' }}</template></el-table-column>
              <el-table-column label="数据表名称" width="150" show-overflow-tooltip><template #default="{ row }">{{ row.tableName || row.tableCode }}</template></el-table-column>
              <el-table-column label="来源类型(BCDEF)" width="122"><template #default="{ row }">{{ row.sourceType || '—' }}</template></el-table-column>
              <el-table-column label="来源主体名称" width="132" show-overflow-tooltip><template #default="{ row }">{{ row.sourceSubject || '—' }}</template></el-table-column>
              <el-table-column label="来源权益限制摘要" width="160" show-overflow-tooltip><template #default="{ row }">{{ row.sourceDesc || '—' }}</template></el-table-column>
              <el-table-column label="来源凭证附件或说明" width="152" show-overflow-tooltip><template #default="{ row }">{{ row.sourceAttachment || '—' }}</template></el-table-column>
              <el-table-column label="信息关联类型(GHIJ)" width="134"><template #default="{ row }">{{ relLetters(row).join('、') || '—' }}</template></el-table-column>
              <el-table-column label="信息识别关联主体说明" width="180" show-overflow-tooltip><template #default="{ row }">{{ relSubjectText(row) }}</template></el-table-column>
              <el-table-column label="信息识别关联资料附件" width="172" show-overflow-tooltip><template #default="{ row }">{{ relAttachText(row) }}</template></el-table-column>
              <el-table-column label="权益风险" width="150" show-overflow-tooltip><template #default="{ row }">{{ row.riskDesc || '—' }}</template></el-table-column>
              <el-table-column label="操作" width="118" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="rowDisputed(row)" link type="primary" size="small" @click="openT2(row)">编辑表2</el-button>
                  <el-button link type="danger" size="small" @click="removeTableItem(row)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-pagination style="justify-content:flex-end" background small layout="total, sizes, prev, pager, next, jumper"
              :page-sizes="[10, 20, 50]" :total="t2Filtered.length" :current-page="t2Page.current" :page-size="t2Page.size"
              @current-change="p => t2Page.current = p" @size-change="s => { t2Page.size = s; t2Page.current = 1 }" />
          </div>

          <!-- ↓ 因果顺序:上方库表清单(逐表表2)→ 表1 系统级并集(自动汇总)→ 经营权归集判定 -->
          <el-alert v-if="changeNarrowing" type="info" :closable="false" style="margin:6px 0 10px"
            :title="`已按变更触发「${form.changeTrigger}」聚焦相关识别维度,其余维持原值已折叠;如确有变动请展开修改`" />
          <el-divider content-position="left" style="font-size:12px;color:var(--prm-color-text-weak)">表1 系统级并集(由上方库表清单各表「表2」自动汇总·只读)</el-divider>
          <el-form-item label="来源权益识别">
            <div v-if="form.sourceIdent.length" style="display:flex;flex-wrap:wrap;gap:6px">
              <span v-for="s in sourceOpts.filter(o => form.sourceIdent.includes(o.v))" :key="s.v" class="prm-c-primary">{{ s.v }} {{ s.t }}</span>
            </div>
            <span v-else style="color:var(--prm-color-text-disabled)">—</span>
            <div class="form-tip">表1 系统级并集 = 上方库表清单各表「来源判定」的并集(随表2 自动重算·只读);如需调整请到对应库表「编辑表2」改其来源类型。</div>
          </el-form-item>
          <!-- 标签严格对齐35号文附录C表1列名「信息关联权益识别」 -->
          <el-form-item label="信息关联权益识别">
            <div v-if="form.relationIdent.length" style="display:flex;flex-wrap:wrap;gap:6px">
              <span v-for="r in relationOpts.filter(o => form.relationIdent.includes(o.v))" :key="r.v" :class="'prm-c-' + ((r.v === 'H' ? 'danger' : 'warning') || 'primary')">{{ r.v }} {{ r.t }}</span>
            </div>
            <span v-else style="color:var(--prm-color-text-disabled)">—(本系统库表均不涉 G/H/I/J)</span>
            <div class="form-tip">表1 系统级并集 = 上方各库表 G/H/I/J 的并集(随表2 自动重算·只读);如需调整请到对应库表「编辑表2」勾选。</div>
          </el-form-item>
          <el-divider content-position="left" style="font-size:12px;color:var(--prm-color-text-weak)">
            经营权归集判定
            <el-tooltip placement="top" content="非表1 采集项·辅助预判;经营权最终由 来源/管制/第三方 事实推导,结果进权益卡片与表4,以审核为准。规则依据《数据权益内部管理汇总表》说明页。">
              <el-icon style="margin-left:4px;vertical-align:-2px;color:var(--prm-color-text-disabled)"><QuestionFilled /></el-icon>
            </el-tooltip>
          </el-divider>
          <!-- TODO 终态:经营权纯推导,「权属类型主张」框降级为可选"经营意图"。现为过渡:初始确权默认全选三权,使经营权默认按事实约束推导(规则1.2 与管制1.1 拉齐,消除"漏勾→经营权被误抑制"),框保留可改、不再必填。 -->
          <el-form-item label="权属类型主张" prop="rightTypes">
            <el-select v-model="form.rightTypes" multiple style="width:100%" placeholder="默认主张全部三权(可改)">
              <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
            </el-select>
            <div class="form-tip">默认主张全部三权(可改);经营权最终由 <b style="color:var(--prm-color-link)">来源 × 管制 × 第三方</b> 事实推导 + 表4 审核为准,不以此主张为准。</div>
            <div v-if="dimMarks['权属类型']" class="form-tip" style="margin-top:2px">
              <span v-if="dimMarks['权属类型'].changed" class="prm-c-warning">已修改 · 原值「{{ dimMarks['权属类型'].before || '空' }}」</span>
              <span v-else class="prm-c-info">维持原值</span>
            </div>
          </el-form-item>
          <!-- UI评审#6:「管制业务」文字说明改小图标触发提示 -->
          <el-form-item>
            <template #label>
              管制属性
              <el-tooltip placement="top" content="已按业务域预选:输配电/调度→管制业务,营销/办公等→非管制;可改。">
                <el-icon style="margin-left:2px;vertical-align:-2px;color:var(--prm-color-text-disabled)"><QuestionFilled /></el-icon>
              </el-tooltip>
            </template>
            <!-- 输入区:单选 + 一行"随选变化"的后果提示(简练明了,对齐指引§授权原则);输出判定卡纵向堆叠、不再挤单选 -->
            <div style="width:100%">
              <el-radio-group v-model="form.regulated">
                <el-radio value="管制业务">管制业务</el-radio>
                <el-radio value="非管制">非管制</el-radio>
              </el-radio-group>
              <div class="form-tip reg-hint">{{ regulatedHint }}</div>
            </div>
          </el-form-item>

        </el-form>
        <!-- 表2 逐表编辑抽屉:该表的来源/各关联主体/权益风险。
             注意:必须位于 step1 大表单(el-form :disabled="!!applyId")之外——否则暂存后 form disabled 级联禁用抽屉,
             step2/step3「编辑表2」入口打开的将是只读死抽屉(B′ 逐表修复路径依赖此处可编辑,改动经 persistTableItems 落库)。 -->
        <el-drawer v-model="t2Drawer.visible" size="460px" append-to-body
            :title="`表2 第三方权益 · ${t2Drawer.row ? (t2Drawer.row.tableName || t2Drawer.row.tableCode) : ''}`">
            <div v-if="t2Drawer.row" style="padding:0 6px">
              <el-alert :closable="false" type="info" show-icon style="margin-bottom:10px"
                title="实例/schema/表代码/表名/密级/确权时间来自平台、只读;来源与各关联主体为平台元数据预填,请核实修正,改动会留痕。" />
              <el-form label-position="top">
                <!-- 锁定字段:平台为唯一真源,只读 -->
                <el-form-item label="数据表(平台·只读)">
                  <span style="color:var(--prm-color-text-secondary)">{{ t2Drawer.row.instanceName }} / {{ t2Drawer.row.schemaName }} / {{ t2Drawer.row.tableCode }}（{{ t2Drawer.row.tableName }}）</span>
                  <span style="margin-left:8px" class="prm-c-info">密级 {{ t2Drawer.row.secretLevel }}</span>
                  <span v-if="t2Drawer.row.authTime" style="margin-left:6px" class="prm-c-success">确权时间 {{ t2Drawer.row.authTime }}</span>
                </el-form-item>
                <el-form-item label="来源类型">
                  <el-select v-model="t2Drawer.row.sourceType" style="width:100%">
                    <el-option v-for="s in sourceOpts" :key="s.v" :label="`${s.v} ${s.t}`" :value="`${s.v} ${s.t}`" />
                  </el-select>
                  <span v-if="pfMark(t2Drawer.row,'sourceType').state==='changed'" class="pf-mark prm-c-warning">已修改 · 平台原值「{{ pfMark(t2Drawer.row,'sourceType').before || '空' }}」</span>
                  <span v-else-if="pfMark(t2Drawer.row,'sourceType').state==='prefilled'" class="pf-mark prm-c-success">平台预填</span>
                </el-form-item>
                <el-form-item label="来源主体名称" :required="'BCDEF'.includes((t2Drawer.row.sourceType || '').charAt(0))">
                  <el-input v-model="t2Drawer.row.sourceSubject" placeholder="B–F 来源须填(A 自行生产可空)" />
                  <span v-if="pfMark(t2Drawer.row,'sourceSubject').state==='changed'" class="pf-mark prm-c-warning">已修改 · 平台原值「{{ pfMark(t2Drawer.row,'sourceSubject').before || '空' }}」</span>
                  <span v-else-if="pfMark(t2Drawer.row,'sourceSubject').state==='prefilled'" class="pf-mark prm-c-success">平台预填</span>
                </el-form-item>
                <el-form-item label="来源权益限制摘要">
                  <el-input v-model="t2Drawer.row.sourceDesc" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="该表来源的权益限制/使用约束" />
                  <span v-if="pfMark(t2Drawer.row,'sourceDesc').state==='changed'" class="pf-mark prm-c-warning">已修改 · 平台原值「{{ pfMark(t2Drawer.row,'sourceDesc').before || '空' }}」</span>
                  <span v-else-if="pfMark(t2Drawer.row,'sourceDesc').state==='prefilled'" class="pf-mark prm-c-success">平台预填</span>
                </el-form-item>
                <el-form-item v-if="t2Drawer.row.sourceAttachment" label="来源凭证附件">
                  <span class="prm-c-info">{{ t2Drawer.row.sourceAttachment }}</span>
                  <span class="form-tip" style="margin-left:6px">平台已上传·只读;正本见「上传材料」步骤</span>
                </el-form-item>
                <el-divider content-position="left" style="font-size:12px;color:var(--prm-color-text-weak)">信息关联类型(G–J)·逐表勾选并填关联主体</el-divider>
                <div v-for="r in relationOpts" :key="r.v" style="margin-bottom:10px">
                  <el-checkbox :model-value="t2Drawer.row[r.v.toLowerCase() + 'Flag'] === '是'"
                    @change="v => (t2Drawer.row[r.v.toLowerCase() + 'Flag'] = v ? '是' : '否')">{{ r.v }} {{ r.t }}</el-checkbox>
                  <template v-if="t2Drawer.row[r.v.toLowerCase() + 'Flag'] === '是'">
                    <el-input v-model="t2Drawer.row[r.v.toLowerCase() + 'Subject']"
                      size="small" style="margin-top:4px" :placeholder="`${r.v} 信息识别关联主体说明${(r.v === 'H' || r.v === 'J') ? '(必填)' : ''}`" />
                    <span v-if="pfMark(t2Drawer.row, r.v.toLowerCase()+'Subject').state==='changed'" class="pf-mark prm-c-warning">已修改 · 平台原值「{{ pfMark(t2Drawer.row, r.v.toLowerCase()+'Subject').before || '空' }}」</span>
                    <span v-else-if="pfMark(t2Drawer.row, r.v.toLowerCase()+'Subject').state==='prefilled'" class="pf-mark prm-c-success">平台预填</span>
                    <div v-if="t2Drawer.row[relAttachKey(r.v)]" style="margin-top:3px;font-size:12px;color:var(--prm-color-text-weak)">
                      关联资料附件:<span class="prm-c-info">{{ t2Drawer.row[relAttachKey(r.v)] }}</span> 平台已上传·只读;正本见「上传材料」
                    </div>
                  </template>
                </div>
                <el-form-item label="权益风险(平台无源·申报人评估)">
                  <el-input v-model="t2Drawer.row.riskDesc" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="是否存在未清晰约定的潜在侵权风险(可选)" />
                </el-form-item>
              </el-form>
              <div style="text-align:right;margin-top:6px">
                <span style="margin-right:8px" :class="'prm-c-' + ((rowTable2Status(t2Drawer.row).type) || 'primary')">{{ rowTable2Status(t2Drawer.row).text }}</span>
                <el-button type="primary" @click="t2Drawer.visible = false">完成</el-button>
              </div>
            </div>
          </el-drawer>
          </div>
        </div>
        <el-alert v-if="applyId" type="success" :closable="false" show-icon
          :title="`申请已暂存(${applyNo || applyId})，进入材料上传`" style="max-width:640px;margin-top:8px" />
      </el-card>

      <!-- 步骤2:上传材料(先从平台元数据同步已上传材料,再补全缺口) -->
      <el-card v-show="step === 1" shadow="never">
        <el-alert v-if="form.registerType === '确权变更' && form.changeTrigger" type="info" :closable="false" style="margin-bottom:10px"
          :title="`确权变更(触发:${form.changeTrigger}）—— 应交材料已收敛为差异项,仅需提交与本次变更相关的材料,无需重复全套`" />
        <!-- P2′ 旧↔新凭证配对:上一版申请的凭证 vs 本次应交(指引:针对变动内容配套提交最新凭证;到期续签=旧协议→新协议) -->
        <el-collapse v-if="form.registerType === '确权变更' && priorMaterials.length" style="margin-bottom:10px">
          <el-collapse-item :title="`变更前凭证对照(上一版申请 ${priorMaterials.length} 份材料 → 本次更新状态)`" name="pm">
            <el-table :data="priorMaterials" size="small" border>
              <el-table-column prop="materialName" label="上一版凭证(变更前)" min-width="170" show-overflow-tooltip />
              <el-table-column prop="fileName" label="原附件" min-width="140" show-overflow-tooltip />
              <el-table-column label="本次状态(变更后)" width="180" align="center">
                <template #default="{ row }">
                  <span :class="'prm-c-' + ((priorMatchStatus(row).type) || 'primary')">{{ priorMatchStatus(row).label }}</span>
                </template>
              </el-table-column>
            </el-table>
            <div class="form-tip">列入本次应交清单的同名凭证,请上传变更后最新件替代旧件(如到期续签的新授权协议);不在清单内的旧凭证维持原值,无需重复提交。</div>
          </el-collapse-item>
        </el-collapse>
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
            <span v-for="s in syncReport.synced" :key="s.materialName" style="margin:2px 4px 2px 0" class="prm-c-success">
              {{ s.code }} · {{ s.attachment }}
            </span>
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
              <template #default="{ row }"><span v-if="row.sensitiveHit" class="prm-c-danger">敏感</span><span v-else>—</span></template>
            </el-table-column>
            <el-table-column label="查重" width="120" align="center">
              <template #default="{ row }"><span v-if="row.duplicateOf" class="prm-c-warning">疑与「{{ row.duplicateOf }}」重复</span><span v-else>—</span></template>
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
                <span class="prm-c-success">已同步·平台</span>
                <div v-if="row.fileName" class="prm-c-success" style="font-size:12px;margin-top:2px" :title="row.fileName">{{ row.fileName }}</div>
              </template>
              <template v-else-if="row.done && row.source === '系统生成'">
                <span class="prm-c-primary">系统生成</span>
                <div v-if="row.fileName" style="font-size:12px;color:var(--prm-color-link);margin-top:2px" :title="row.fileName">{{ row.fileName }}</div>
              </template>
              <span v-else-if="row.done" class="prm-c-success">已上传</span>
              <span v-else class="prm-c-warning">待补全</span>
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

        <!-- P1:逐表凭证材料(投影自表2 tableItems,与 step1 表2 抽屉同源;先只读展示,补全见「编辑表2」) -->
        <el-divider content-position="left" style="font-size:12px;color:var(--prm-color-text-weak)">
          逐表凭证材料(逐表 · 与表2 同源)
          <el-tooltip placement="top" content="对齐 35 号文表2:来源凭证附件(来源 B–F)/信息识别关联资料附件(G/H/I/J)按数据表逐张承载。此处投影自表2(单一真源);补全请点「编辑表2」。上方清单为系统级材料(表1/表2/权属凭证/A 说明)。">
            <el-icon style="margin-left:4px;vertical-align:-2px;color:var(--prm-color-text-disabled)"><QuestionFilled /></el-icon>
          </el-tooltip>
        </el-divider>
        <el-empty v-if="!perTableCredentials.length" description="本系统无涉第三方/敏感库表,无逐表凭证" :image-size="50" />
        <el-table v-else :data="perTableCredentials" border size="small" row-key="key">
          <el-table-column label="数据表" min-width="180">
            <template #default="{ row }">
              <div style="font-weight:600;color:var(--prm-color-text)">{{ row.schemaName }}.{{ row.tableCode }}</div>
              <div style="font-size:12px;color:var(--prm-color-text-weak)">{{ row.tableName }}</div>
            </template>
          </el-table-column>
          <el-table-column label="凭证槽位" width="210">
            <template #default="{ row }">
              <span :class="'prm-c-' + ((row.slot === '来源凭证' ? 'primary' : (row.letter === 'H' ? 'danger' : 'warning')) || 'primary')">{{ row.slot }} · {{ row.letter }} {{ row.typeName }}</span>
            </template>
          </el-table-column>
          <el-table-column label="平台附件 / 状态" min-width="220">
            <template #default="{ row }">
              <span v-if="row.attachment" class="prm-c-success" style="font-size:12px" :title="row.attachment">{{ row.attachment }}</span>
              <span v-else class="prm-c-warning">待补全</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="256" align="center">
            <template #default="{ row }">
              <el-upload :auto-upload="false" :show-file-list="false" :on-change="(f) => onPerTableUpload(row, f)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" style="display:inline-block">
                <el-button link type="success" size="small">{{ row.attachment ? '替换' : '上传原件' }}</el-button>
              </el-upload>
              <el-button link type="primary" size="small" :disabled="!!row.attachment" @click="onPerTableRegister(row)" style="margin-left:6px">仅登记</el-button>
              <el-button v-if="row.matId" link type="primary" size="small" @click="previewPerTable(row)" style="margin-left:6px">预览</el-button>
              <el-button link type="primary" size="small" @click="openT2(row.row)" style="margin-left:6px">编辑表2</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 步骤3:材料校验(规则校验 + AI 智能校验,评审8.4) -->
      <el-card v-show="step === 2" shadow="never">
        <div class="prm-table-note" style="margin-bottom:10px">一键完成【规则校验 + AI 材料校验(qwen3-max)】并给出<b>单一裁决</b>;有问题就地处理,全部通过方可提交审核。</div>
        <!-- 单一裁决状态条:一句话说清"能不能提交、还差什么" -->
        <div style="margin-bottom:10px">
          校验状态:<span :class="'prm-c-' + ((checkStatus.type) || 'primary')">{{ checkStatus.text }}</span>
          <!-- A′:逐表凭证汇总(投影自表2·与 step2 同源);缺失明细在下方需处理清单,可就地修 -->
          <span v-if="perTableCredentials.length" style="margin-left:8px" :class="'prm-c-' + ((perTableMissing.length ? 'warning' : 'success') || 'primary')">
            逐表凭证 {{ perTableCredentials.length - perTableMissing.length }}/{{ perTableCredentials.length }}{{ perTableMissing.length ? ` · 缺 ${perTableMissing.length}` : ' · 齐' }}
          </span>
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
        <el-alert v-if="aiResult" :type="aiResult.prediction === '建议通过' ? 'success' : 'warning'" :closable="false" style="margin-bottom:10px">
          <div><b>AI 决策研判:{{ aiResult.prediction }}</b>(综合评分 {{ aiResult.score }})</div>
          <div style="margin-top:4px">AI 预测:{{ aiResult.aiPrediction || '未生成' }}</div>
          <div v-if="aiResult.supplementMaterials && aiResult.supplementMaterials.length" style="margin-top:4px">需补材料:{{ aiResult.supplementMaterials.join('、') }}</div>
          <div v-if="aiResult.pendingConflicts && aiResult.pendingConflicts.length" style="margin-top:4px">待处理冲突:{{ aiResult.pendingConflicts.join('、') }}</div>
          <div style="margin-top:4px;color:var(--prm-color-text-weak)">依据:{{ aiResult.basis }}</div>
        </el-alert>
        <!-- 权属冲突识别:确权内生 AI(走 confirm-service /ai/conflict) -->
        <el-alert v-if="conflictResult" :type="conflictResult.hasConflict ? 'error' : 'success'" :closable="false" style="margin-bottom:10px">
          <div><b>权属冲突识别:{{ conflictResult.hasConflict ? '发现冲突' : '未发现冲突' }}</b>(风险:{{ conflictResult.riskLevel }})</div>
          <div v-if="conflictResult.conflicts && conflictResult.conflicts.length" style="margin-top:4px">冲突:{{ conflictResult.conflicts.join('、') }}</div>
          <div v-if="conflictResult.suggestion" style="margin-top:4px;color:var(--prm-color-text-weak)">建议:{{ conflictResult.suggestion }}</div>
        </el-alert>
        <!-- 统一待处理清单(单一闭环):规则缺失/不合规 就地上传;AI 存疑/不通过 复核或去修正 -->
        <el-card v-if="pendingItems.length" shadow="never" style="margin-bottom:10px;border:1px solid #fde2e2;background:#fff8f8">
          <div style="font-weight:600;color:var(--prm-color-danger);margin-bottom:8px">需处理以下 {{ pendingItems.length }} 项后方可提交(处理完点上方「重新一键校验」)</div>
          <el-table :data="pendingItems" border size="small">
            <el-table-column label="来源" width="76" align="center">
              <template #default="{ row }"><span :class="'prm-c-' + ((row.source === 'ai' ? 'warning' : 'danger') || 'primary')">{{ row.source === 'ai' ? 'AI' : (row.source === 'perTable' ? '逐表' : '规则') }}</span></template>
            </el-table-column>
            <el-table-column prop="name" label="材料 / 项" min-width="200" show-overflow-tooltip />
            <el-table-column prop="kind" label="问题" width="96" align="center">
              <template #default="{ row }"><span class="prm-c-danger">{{ row.kind }}</span></template>
            </el-table-column>
            <el-table-column prop="suggestion" label="说明 / 建议" min-width="240" show-overflow-tooltip />
            <el-table-column label="就地处理" width="230" align="center">
              <template #default="{ row }">
                <el-upload v-if="row.source === 'rule'" :auto-upload="false" :show-file-list="false" :on-change="(f) => onFixUpload(row, f)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" style="display:inline-block">
                  <el-button link type="primary">上传补充</el-button>
                </el-upload>
                <!-- B′:逐表缺失就地修——直写 ConfirmTableItem(单一真源),复用 step2 逐表上传/仅登记/表2抽屉,根除"补了也修不掉"死循环 -->
                <template v-else-if="row.source === 'perTable'">
                  <el-upload :auto-upload="false" :show-file-list="false" :on-change="(f) => onPerTableUpload(row, f)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" style="display:inline-block">
                    <el-button link type="success">上传原件</el-button>
                  </el-upload>
                  <el-button link type="primary" @click="onPerTableRegister(row)" style="margin-left:6px">仅登记</el-button>
                  <el-button link type="primary" @click="openT2(row.row)" style="margin-left:6px">编辑表2</el-button>
                </template>
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
        <el-alert v-if="aiMatResult" :type="aiMatResult.overall === '通过' ? 'success' : (aiMatResult.overall === '不通过' ? 'error' : 'warning')" :closable="false" style="margin-bottom:10px">
          <div><b>AI 材料校验:{{ aiMatResult.overall }}</b> — {{ aiMatResult.overallDesc }}</div>
        </el-alert>
        <el-alert v-if="checkReport" :type="checkReport.allPass ? 'success' : 'warning'" :closable="false" style="margin-bottom:10px">
          <div>{{ checkReport.summary }}</div>
        </el-alert>
        <!-- 两层材料呈现与 step2 对称:上=系统级材料(ConfirmMaterial),下=逐表凭证校验(投影 tableItems·与表2 同源)。
             此前逐表层仅一枚聚合 chip,"应交8项 vs 表格4行"数字不接,引发"丢材料"误解 —— 补齐逐行呈现。 -->
        <el-divider content-position="left" style="font-size:12px;color:var(--prm-color-text-weak)">系统级材料(表1 / 表2 / 权属凭证 / A 建设投入说明)</el-divider>
        <el-table :data="materials" border>
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="materialName" label="材料名称" min-width="240" />
          <el-table-column prop="materialType" label="类型" width="140" />
          <el-table-column label="校验结果" width="120" align="center">
            <template #default="{ row }">
              <span :class="'prm-c-' + ((row.checkResult === '通过' ? 'success' : (row.checkResult === '不通过' ? 'danger' : 'info')) || 'primary')">
                {{ row.checkResult || '待校验' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="110" align="center">
            <template #default="{ row }">
              <span :class="'prm-c-' + ((row.source === '平台同步' ? 'success' : 'info') || 'primary')">{{ row.source || '用户上传' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="原件" min-width="160">
            <template #default="{ row }">
              <!-- 平台同步且已落地原件字节(fileUrl)才可在线预览;否则纯文本标注,不给会 404 的入口 -->
              <el-link v-if="row.source === '平台同步' && row.fileUrl" type="success" @click="previewMaterial(row)" :title="row.fileName">{{ row.fileName }}（平台原件·预览）</el-link>
              <span v-else-if="row.source === '平台同步'" class="prm-c-success" :title="row.fileName">{{ row.fileName }}（平台原件）</span>
              <el-link v-else-if="row.fileName" type="primary" @click="previewMaterial(row)">{{ row.fileName }}（预览/下载）</el-link>
              <span v-else style="color:var(--prm-color-text-disabled)">占位/无原件</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 逐表凭证校验:与 step2 逐表区同构同源(perTableCredentials);齐=通过,缺=红标(缺失项另在需处理清单可就地修) -->
        <template v-if="perTableCredentials.length">
          <el-divider content-position="left" style="font-size:12px;color:var(--prm-color-text-weak)">
            逐表凭证校验({{ perTableCredentials.length }})· 与表2 同源
          </el-divider>
          <el-table :data="perTableCredentials" border size="small" row-key="key">
            <el-table-column label="数据表" min-width="180">
              <template #default="{ row }">
                <div style="font-weight:600;color:var(--prm-color-text)">{{ row.schemaName }}.{{ row.tableCode }}</div>
                <div style="font-size:12px;color:var(--prm-color-text-weak)">{{ row.tableName }}</div>
              </template>
            </el-table-column>
            <el-table-column label="凭证槽位" width="210">
              <template #default="{ row }">
                <span :class="'prm-c-' + ((row.slot === '来源凭证' ? 'primary' : (row.letter === 'H' ? 'danger' : 'warning')) || 'primary')">{{ row.slot }} · {{ row.letter }} {{ row.typeName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="校验结果" width="120" align="center">
              <template #default="{ row }">
                <span :class="'prm-c-' + ((row.attachment ? 'success' : 'danger') || 'primary')">{{ row.attachment ? '通过' : '缺失' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="附件" min-width="200">
              <template #default="{ row }">
                <el-link v-if="row.matId" type="primary" @click="previewPerTable(row)" :title="row.attachment">{{ row.attachment }}</el-link>
                <span v-else-if="row.attachment" class="prm-c-success" :title="row.attachment">{{ row.attachment }}（平台原件）</span>
                <span v-else style="color:var(--prm-color-text-disabled)">待补全 — 见上方需处理清单就地修</span>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <!-- 权益归集判定(分子公司共享网公司,《权益内部管理汇总表》说明页规则) -->
        <el-divider content-position="left" style="font-size:12px;color:var(--prm-color-text-weak)">权益归集判定(分子公司共享网公司)</el-divider>
        <el-descriptions v-if="consolidation" :column="4" border size="small" class="consol-panel">
          <el-descriptions-item label="命中规则">规则 {{ consolidation.rule }}</el-descriptions-item>
          <el-descriptions-item label="网公司持有权"><span :class="'prm-c-' + ((consolidation.holdRight === '有' ? 'success' : 'info') || 'primary')">{{ consolidation.holdRight }}</span></el-descriptions-item>
          <el-descriptions-item label="网公司使用权"><span :class="'prm-c-' + ((consolidation.useRight === '有' ? 'success' : 'info') || 'primary')">{{ consolidation.useRight }}</span></el-descriptions-item>
          <el-descriptions-item label="网公司经营权"><span :class="'prm-c-' + ((consolidation.operateRight === '有' ? 'success' : (consolidation.operateRight === '无' ? 'info' : 'warning')) || 'primary')">{{ consolidation.operateRight }}</span></el-descriptions-item>
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
                <span :class="'prm-c-' + ((row.before === '有' ? 'success' : 'info') || 'primary')">{{ row.before }}</span>
                <span v-if="row.before === '有' && row.scope" style="margin-left:6px;color:var(--prm-color-text-weak);font-size:12px">{{ row.scope }}{{ row.validDate ? ' · 至 ' + row.validDate : '' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="本次(归集判定)" min-width="120">
              <template #default="{ row }"><span :class="'prm-c-' + ((row.after === '有' ? 'success' : 'info') || 'primary')">{{ row.after }}</span></template>
            </el-table-column>
            <el-table-column label="变更" width="92" align="center">
              <template #default="{ row }">
                <span v-if="row.mark === '新增'" class="prm-c-success">新增</span>
                <span v-else-if="row.mark === '撤销'" class="prm-c-danger">撤销</span>
                <span v-else-if="row.mark === '待判定'" class="prm-c-info">待判定</span>
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
          <el-step title="经理/高级经理终审" description="逐级审批:管控小组 → 主管 → 经理" />
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
      <!-- PDD 8.1 状态机·独立「保存草稿」:step 0/1/2 均可主动暂存(宽进,不校验完成度、不前进),中途离开不丢 -->
      <el-button v-if="step < 3" :loading="saving" @click="saveDraft">保存草稿</el-button>
      <!-- 自动保存指示(防丢):本地即时 + 达底线后服务端同步,静默呈现 -->
      <span v-if="step < 3 && autoTip" style="margin-left:8px;color:var(--prm-color-text-weak);font-size:12px">{{ autoTip }}</span>
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
import PageNote from '@/components/PageNote.vue'
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { confirmAsync } from '@/utils/confirmAsync'
import { currentUser } from '@/api/auth'
import { useDraftAutosave, localDraftKey, readLocalDraft, removeLocalDraft } from '@/composables/useDraftAutosave'
import { filterRequiredRules } from '@/lib/confirmChecklist'
import { notifyDraftChanged } from '@/lib/draftCount'
import { markDraftSource, clearDraftSource } from '@/lib/draftSource'
import { autofillConfirm, saveConfirmDraft, uploadMaterial, uploadMaterialFile, materialFileUrl, listMaterialByApply, checkMaterial, runMaterialCheck, syncPlatformMaterials, pushMaterialReview, materialExportUrl, saveAiSnapshot, saveTableItems, listTableItems, getConfirmApply, getConsolidation, aiMaterialCheck, listMaterialRules, aiParseConfirm, aiDecisionConfirm, aiConflictConfirm } from '@/api/confirm'
import AiThinking from '@/components/AiThinking.vue'
import PageActions from '@/components/PageActions.vue'
import { useAiThinking } from '@/composables/useAiThinking'
import { openFilePreview } from '@/composables/useFilePreview'
import { AI_PHASES } from '@/lib/aiPhases'
const aiThink = useAiThinking()

const router = useRouter()
const route = useRoute()
const rightTypes = ['持有权', '使用权', '经营权']
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
// 步骤条回跳:仅"已完成"步骤(idx < 当前步)可点,且终态(提交完成页 step3)不可再跳;前进仍须走「下一步」校验
function stepJumpable(idx) { return step.value < 3 && idx < step.value }
function onStepClick(idx) { if (stepJumpable(idx)) step.value = idx }
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
  // B′:后端逐表缺失串(表·槽)由本地对象行替代(可就地写回真源),按固定槽位后缀去重,避免同一缺口双行
  const rule = !checkReport.value ? [] : [
    ...(checkReport.value.missing || []).filter(n => !PER_TABLE_MISS_RE.test(n))
      .map(n => ({ source: 'rule', name: n, kind: '缺失', suggestion: '未提交,请就地补充原件' })),
    ...(checkReport.value.nonCompliant || []).map(n => ({ source: 'rule', name: n, kind: '不合规', suggestion: '校验未通过,请重新上传' }))
  ]
  // B′:逐表凭证缺失(本地真源推导,无需等校验)——行携对象引用,就地 上传/仅登记/编辑表2 直写 ConfirmTableItem
  const perTable = perTableMissing.value.map(r => ({
    source: 'perTable', name: `${r.tableName || r.tableCode}·${r.letter} ${r.typeName}(${r.slot})`,
    kind: '缺失', suggestion: '逐表凭证未补全:就地上传原件/仅登记,或「编辑表2」核实来源与关联', ...r
  }))
  const ai = aiUnresolved.value.map(i => ({ source: 'ai', name: i.materialName,
    kind: i.verdict === '不通过' ? 'AI不通过' : 'AI存疑',
    suggestion: [i.issues, i.suggestion].filter(Boolean).join(' / ') || 'AI 提示需核实' }))
  const qa = qualityBlocked.value ? [{ source: 'quality', name: '元数据质量门禁', kind: '质量',
    suggestion: `元数据质量评分 ${quality.value} < ${QUALITY_MIN},提交将被自动驳回,请先治理元数据质量后重新一键校验` }] : []
  return [...rule, ...perTable, ...ai, ...qa]
})

// 单一裁决:规则全过 + 逐表凭证齐(本地真源兜底) + AI 已跑且无未结存疑/不通过 + 质量达标 + 无未结变更,才点亮提交
const canSubmit = computed(() => ruleDone.value && checkReport.value.allPass
  && perTableMissing.value.length === 0
  && aiDone.value && aiUnresolved.value.length === 0 && !qualityBlocked.value && !needRecheck.value)

// 统一状态条:一句话说清"能不能提交、还差什么"
const checkStatus = computed(() => {
  if (needRecheck.value) return { type: 'warning', text: '已变更,请重新一键校验' }
  if (!ruleDone.value && !aiDone.value) return { type: 'info', text: '未校验 — 请点「一键校验」' }
  if (canSubmit.value) return { type: 'success', text: '全部通过,可提交' }
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
  // C′:校验前先把内存 tableItems 落库(step2「编辑表2」可能改了表2 未持久化)——保证后端闸门评估的 = 用户眼前的状态
  try { await persistTableItems() } catch (e) { /* 落库失败拦截器已提示;仍继续校验(后端按既有库数据裁决) */ }
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
// 逐表上传件 materialId 字段(供预览):与 relAttachKey 一一对应
function relMatIdKey(v) {
  return { G: 'checkMatId', H: 'privacyMatId', I: 'busSecretMatId', J: 'equityMatId' }[v] || ''
}

// P1:逐表凭证材料——投影自 tableItems(与 step1 表2 抽屉同源:同一批响应式对象)。
// 每张争议表 × 应交凭证槽位:来源凭证(来源 B–F,A 自行生产不需)+ G/H/I/J 关联资料(逐类)。
// 对齐 35 号文表2「来源凭证附件 / 信息识别关联资料附件」逐表承载;单一真源 = tableItems。
const perTableCredentials = computed(() => {
  const rows = []
  for (const t of tableItems.value) {
    if (!rowDisputed(t)) continue
    const srcLetter = (t.sourceType || '').trim().charAt(0)
    if ('BCDEF'.includes(srcLetter)) {
      const opt = sourceOpts.find(o => o.v === srcLetter)
      rows.push({
        key: (t.tableCode || '') + ':SRC', tableCode: t.tableCode, tableName: t.tableName,
        schemaName: t.schemaName, slot: '来源凭证', letter: srcLetter, typeName: opt ? opt.t : '',
        attachment: t.sourceAttachment || '', attKey: 'sourceAttachment', matIdKey: 'sourceMatId',
        matId: t.sourceMatId || '', row: t
      })
    }
    for (const r of relationOpts) {
      if (t[r.v.toLowerCase() + 'Flag'] === '是') {
        rows.push({
          key: (t.tableCode || '') + ':' + r.v, tableCode: t.tableCode, tableName: t.tableName,
          schemaName: t.schemaName, slot: '关联资料', letter: r.v, typeName: r.t,
          attachment: t[relAttachKey(r.v)] || '', attKey: relAttachKey(r.v), matIdKey: relMatIdKey(r.v),
          matId: t[relMatIdKey(r.v)] || '', row: t
        })
      }
    }
  }
  return rows
})
// B′:逐表凭证缺失——直接从 P1 投影派生(谓词与后端 runCheck 逐表段同构,tableItems 即后端所读真源),
// 每条自带 row/attKey/matIdKey 对象引用 → step3 就地修复零字符串解析,写回 ConfirmTableItem(根除死循环)。
const perTableMissing = computed(() => perTableCredentials.value.filter(r => !r.attachment))
// 后端 runCheck 逐表缺失串的固定槽位后缀(与后端 label 拼装一致);pendingItems 用它去重,本地对象行替代展示
const PER_TABLE_MISS_RE = /·(来源凭证\([B-F]\)|G行政监管资料|H个人隐私资料|I商密资料|J协议资料)$/

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

async function onTreeSelect(sysName, tableCodes, domain, opts = {}) {
  const wholeSystem = !!opts.wholeSystem
  const moduleFilter = (opts.modules && opts.modules.length) ? opts.modules : null
  // 逐表路径须有 tableCodes;整系统/整模块路径无需逐码(库表由后端按系统/模块返回全量)
  if (!sysName || (!wholeSystem && !moduleFilter && (!tableCodes || !tableCodes.length))) { tableItems.value = []; return }
  form.assetId = 'SYS:' + sysName
  form.assetName = sysName
  form.systemName = sysName
  if (domain) form.regulated = regulatedByDomain(domain)
  metaLoading.value = true
  try {
    const all = (await cardsBySystem(sysName, moduleFilter, changeMode.value ? '' : 'unconfirmed')) || []
    // 整系统/整模块:后端已按系统(或模块名)返回全部未确权库表,直接全取;逐表:按勾选码过滤
    const picked = (wholeSystem || moduleFilter) ? all : all.filter(t => tableCodes.includes(t.tableCode))
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
    gSubject: row.gSubject, hSubject: row.hSubject, iSubject: row.iSubject, jSubject: row.jSubject,
    // G–J 标志纳入基线快照(与 1054 平台构造对齐):否则变更基线拿不到 G–J 原值,关联对照/标志改动检测失效
    gFlag: row.gFlag, hFlag: row.hFlag, iFlag: row.iFlag, jFlag: row.jFlag
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
    baselineTableCount.value = (full && full.tableItems ? full.tableItems.length : 0)
    // 真实上一版表3行作底版:替换目录合成的库表行(在权威结论上做差异编辑)。
    // 守卫(P1′ bug 修复):当前选中含「新增(未确权)」表时不替换 —— 数据新增(insert)的编辑对象是新表本身,
    // 用基线行覆盖会清掉用户选的新表并把 isDataAdd 误翻为 update。
    const pickedHasNew = tableItems.value.some(t => t.confirmed === false)
    if (baselineFromReal.value && full.tableItems && full.tableItems.length && !pickedHasNew) {
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

// ===== 库表清单:筛选 + 分页 + 视图(系统级几百/几千张表规模化)=====
// 均为展示层:disputedRows/pendingT2Rows/提交门禁仍作用于全量 tableItems,不受筛选分页影响。
const t2View = ref('compact')                                   // compact=紧凑核心列 | full=表2完整视图(严格12列横向滚动)
const t2Filter = reactive({ src: [], rel: [], secret: '', status: '', kw: '' })
const t2Page = reactive({ current: 1, size: 10 })
const secretOpts = computed(() => [...new Set(tableItems.value.map(r => r.secretLevel).filter(Boolean))])
// 信息关联类型 G–J 派生显示(供紧凑/完整视图)
function relLetters(row) { return relationOpts.filter(o => row[o.v.toLowerCase() + 'Flag'] === '是').map(o => o.v) }
function relSubjectText(row) { return relationOpts.filter(o => row[o.v.toLowerCase() + 'Flag'] === '是').map(o => `${o.v}:${row[o.v.toLowerCase() + 'Subject'] || '—'}`).join('；') || '—' }
function relAttachText(row) { return relationOpts.filter(o => row[o.v.toLowerCase() + 'Flag'] === '是' && row[relAttachKey(o.v)]).map(o => `${o.v}:${row[relAttachKey(o.v)]}`).join('；') || '—' }
const t2Filtered = computed(() => {
  const kw = t2Filter.kw.trim().toLowerCase()
  return tableItems.value.filter(r => {
    if (t2Filter.src.length && !t2Filter.src.includes((r.sourceType || '').trim().charAt(0))) return false
    if (t2Filter.rel.length && !t2Filter.rel.some(v => r[v.toLowerCase() + 'Flag'] === '是')) return false
    if (t2Filter.secret && r.secretLevel !== t2Filter.secret) return false
    if (t2Filter.status && rowTable2Status(r).text !== t2Filter.status) return false
    if (kw && !`${r.schemaName || ''} ${r.tableCode || ''} ${r.tableName || ''}`.toLowerCase().includes(kw)) return false
    return true
  })
})
const t2Paged = computed(() => {
  const start = (t2Page.current - 1) * t2Page.size
  return t2Filtered.value.slice(start, start + t2Page.size)
})
function rowSeq(i) { return (t2Page.current - 1) * t2Page.size + i + 1 }
function resetT2Filter() { t2Filter.src = []; t2Filter.rel = []; t2Filter.secret = ''; t2Filter.status = ''; t2Filter.kw = '' }
// 筛选/数据量变化回第 1 页(避免停在越界空页)
watch(() => [t2Filter.src.length, t2Filter.rel.length, t2Filter.secret, t2Filter.status, t2Filter.kw, tableItems.value.length], () => { t2Page.current = 1 })

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

// 管制属性·随选变化的一行后果(对齐 35 号文§数据授权原则:管制类经营权由公司统一管理仅授权取得;非管制类本单位可确权认定,跨单位仍需授权)
const regulatedHint = computed(() => form.regulated === '管制业务'
  ? '管制业务:经营权归公司统一管理,仅经公司授权取得(不可自行认定)'
  : '非管制:本单位经营权可确权认定;跨单位经营权仍需公司授权')

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

// 演示门控:示例填充按钮仅在开发/演示环境(或 ?demo=1)对真实申报人隐藏,防误点污染真实申报数据
const isDemoEnv = computed(() => import.meta.env.DEV || route.query.demo === '1')
// 初始确权默认主张全部三权(过渡方案):使经营权默认按事实约束推导,避免"漏勾→经营权被误抑制";确权变更走基线预填覆盖
const form = reactive({
  assetId: '', assetName: '', systemName: '', rightTypes: [...rightTypes], rightHolder: '', respDept: '', subjectLevel: '',
  systemOwner: '', contactInfo: '', assetSecretLevel: '', region: '',
  registerType: '初始确权', changeTrigger: '', applyMode: '常规', regulated: '非管制',
  purpose: '', thirdPartyInfo: '', sourceSubject: '', sourceLimit: '', relationSubject: '', equityRisk: '',
  privacyInfo: '', sourceIdent: [], relationIdent: [],
  // 权益期限维(35号文三类触发之"权益到期"):变更申报的新权益有效期;制卡直接落新卡 validDate
  validDate: ''
})
// 方案A:库表清单(表2)任意变化即重算表1 系统级 A–F/G–J 并集(派生只读·单一真源=表2)
watch(tableItems, () => aggregateIdentFromTables(), { deep: true })
// P1:确权变更——换触发动因即重算授权处置建议
watch(() => form.changeTrigger, () => { if (changeMode.value) refreshAuthImpact() })
const rules = {
  // 表1 公司主体必填(G1);主体层级=公司主体口径必填(G3);系统负责人/联系方式建议必填(G3)
  rightHolder: [{ required: true, message: '请填写申报权属主体(表1 公司主体)', trigger: 'blur' }],
  subjectLevel: [{ required: true, message: '请选择主体层级(公司总部/分省公司/专业子公司)', trigger: 'change' }],
  systemOwner: [{ required: true, message: '请填写系统负责人', trigger: 'blur' }],
  contactInfo: [{ required: true, message: '请填写联系方式', trigger: 'blur' }]
  // 权属类型主张已去必填:默认全选三权且为辅助预判,最终经营权由事实推导 + 表4 审核为准
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

// ===== P0/P1 变更类型 = 由所选库表确权状态派生约束 + 簇间单选 =====
// 第一性:四类变更 = (动谁:新表 insert / 已确权表 update) ×(动什么元素簇:来源A–F / 关联G–J / 时效)。
// 数据新增(insert,新表首次确权)与 来源/管理/到期变更(update,对已确权结论修订)两种编辑模式互斥,不可同单。
const hasNewTable = computed(() => changeMode.value && tableItems.value.some(t => t.confirmed === false))
const hasConfirmedTable = computed(() => changeMode.value && tableItems.value.some(t => t.confirmed === true))
const isDataAdd = computed(() => hasNewTable.value && !hasConfirmedTable.value)       // 全为新增表 → 锁定"数据新增"
const allConfirmedSel = computed(() => hasConfirmedTable.value && !hasNewTable.value)  // 全为已确权表 → 来源/管理/到期单选
const mixedSelection = computed(() => hasNewTable.value && hasConfirmedTable.value)    // 未确权+已确权混选 → 拦截(编辑模式冲突,可一键裁剪化解)

// 混选一键化解(替代死锁式拦截,修"变更类型不触发":整系统/整模块快捷勾选天然易混选,须给出路而非纯报错)
function keepOnlyConfirmedSel() { tableItems.value = tableItems.value.filter(t => t.confirmed !== false) }
function keepOnlyNewSel() { tableItems.value = tableItems.value.filter(t => t.confirmed === false) }

// 已确权表:来源/管理/到期 单选(update 模式,三选一);数据新增独占(由上方约束自动锁定,不进入本单选)
const CHANGE_TRIGGER_OPTS = [
  { v: '数据来源变更', t: '来源·生产方式重判(A–F)' },
  { v: '管理要求变更', t: '关联·合规重判(G–J)' },
  { v: '权益到期', t: '有效期处置(续止)' }
]
const changeTriggers = ref([])
// 单选控件桥接:UI 只暴露单值(el-radio-group),内部仍以单元素数组承载,下游 .some()/.includes() 逻辑零改动
const changeTriggerSingle = computed({
  get: () => changeTriggers.value[0] || '',
  set: (v) => { changeTriggers.value = v ? [v] : [] }
})
// changeTriggers[] → form.changeTrigger 字符串(单选下恒为单值,无需顿号拼接;沿用 join 兼容旧多值遗留数据)
watch(changeTriggers, (v) => { form.changeTrigger = (v || []).join('、') }, { deep: true })
// 选表确权状态变化 → 同步触发约束:数据新增锁定;离开数据新增态则清掉"数据新增"占位(由用户重选)
watch([isDataAdd, allConfirmedSel], () => {
  if (isDataAdd.value) { if (!(changeTriggers.value.length === 1 && changeTriggers.value[0] === '数据新增')) changeTriggers.value = ['数据新增'] }
  else if (changeTriggers.value.includes('数据新增')) { changeTriggers.value = changeTriggers.value.filter(x => x !== '数据新增') }
})
// 分簇门控:勾选了哪个簇,才比对/收敛哪个簇(消灭"选子集→系统并集幻象删减")
const keepSourceCluster = computed(() => changeTriggers.value.some(t => t.includes('来源')) || changeTriggers.value.includes('其他'))
const keepRelationCluster = computed(() => changeTriggers.value.some(t => t.includes('管理') || t.includes('监管')) || changeTriggers.value.includes('其他'))
// 权益期限簇(35号文三类触发之"权益到期"):勾到期才比对/要求申报新有效期
const keepValidityCluster = computed(() => changeTriggers.value.some(t => t.includes('到期')) || changeTriggers.value.includes('其他'))
onMounted(async () => {
  loadMaterialRules()
  form.registerType = changeMode.value ? '确权变更' : '初始确权'
  try {
    // 草稿就地续填(PDD 8.1):从草稿箱/我的申请「继续填写」带 applyId 进入,按 id 回填并保留 applyId(后续保存/下一步 UPDATE 同单,不复制新单)
    if (route.query.applyId) { await loadDraft(String(route.query.applyId)); return }
    // 主动入口「发起变更」(P1):列表页带 assetId 进入,预选资产并按确权状态自动定登记类型(已确权→确权变更+基线对照)
    if (route.query.assetId && !applyId.value) { await initAssetEntry(String(route.query.assetId)); return }
    if (!route.query.reopen) { await maybeRecoverLocalDraft(); return } // 全新进入:找回本地未提交草稿
    reopenFromRejected()
  } finally {
    autosaveReady.value = true // 回填/找回完成后再开启自动保存,避免载入即回写
  }
})
// 基于原单修改重提:从被驳回确权单带入字段(sessionStorage 'prm-reopen')
function reopenFromRejected() {
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
      // 还原变更触发(单选):原串如为旧多值遗留(顿号/逗号拼接),只取第一项,对齐当前单选模型
      changeTriggers.value = r.changeTrigger
        ? String(r.changeTrigger).split(/[、,，]/).map(s => s.trim()).filter(Boolean).slice(0, 1)
        : []
      ElMessage.warning('已带入被驳回原单内容,请修改后重新提交(将作为新申请)')
    }
  } catch (e) { /* ignore */ }
  sessionStorage.removeItem('prm-reopen')
}
// 草稿就地续填:按 id 回填 表单 + 表2逐表清单 + 已传材料,保留 applyId(后续保存走 UPDATE,不复制)
async function loadDraft(id) {
  try {
    const a = await getConfirmApply(id)
    if (!a || !a.applyId) { ElMessage.warning('草稿不存在或已删除'); return }
    if (a.status && a.status !== '草稿') {
      ElMessage.warning(`该申请当前为「${a.status}」,非草稿不可就地编辑;如需修改请走撤回/重提`)
      return
    }
    applyId.value = a.applyId
    applyNo.value = a.applyNo || ''
    const sysName = (a.assetId || '').startsWith('SYS:') ? a.assetId.slice(4) : (a.assetName || '')
    Object.assign(form, {
      assetId: a.assetId || '', assetName: a.assetName || '', systemName: sysName,
      rightHolder: a.rightHolder || '', respDept: a.respDept || '', systemOwner: a.systemOwner || '',
      contactInfo: a.contactInfo || '', subjectLevel: a.subjectLevel || '',
      registerType: a.registerType || '初始确权', regulated: a.regulated || '非管制',
      purpose: a.purpose || '', privacyInfo: a.privacyInfo || '',
      rightTypes: a.rightType ? String(a.rightType).split(/[、,，]/).map(s => s.trim()).filter(Boolean) : [...rightTypes],
      sourceIdent: parseIdentCodes(a.sourceIdentification, ['A', 'B', 'C', 'D', 'E', 'F']),
      relationIdent: parseIdentCodes(a.relationIdentification, ['G', 'H', 'I', 'J'])
    })
    // 表2 逐表清单回填(ConfirmTableItem → 可编辑行);表1 A–J 由 watch(tableItems) 自动按并集重算
    try {
      const items = await listTableItems(id)
      if (Array.isArray(items) && items.length) tableItems.value = items.map(rowFromBaseline)
    } catch (e) { /* 无表2清单:保持空,不阻断 */ }
    // 已传材料回填
    try { materials.value = (await listMaterialByApply(id)) || [] } catch (e) { materials.value = [] }
    buildChecklist()
    loadConsolidation()
    ElMessage.success('已载入草稿,可继续填写并「保存草稿」或「下一步」')
  } catch (e) { ElMessage.warning('载入草稿失败,请重试') }
}

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
const baselineTableCount = ref(0)      // 上一版已确权库表数(数据新增集合级摘要:前N张→后N+M张)
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
// A–F/G–J 系统级"新值":
//  - 底版=真实上一版确权(baselineFromReal):tableItems 即完整上一版逐表清单(loadChangeBaselineForSystem 整体替换),
//    聚合覆盖全量 → 直接用聚合值,删减是真实删减(可显式呈现"移除",修复只增不删盲区)
//  - 底版=目录合成桩:无法保证选表覆盖全量,维持 基线 ∪ 本次 并集(只增不删,杜绝选子集→幻象删减)
function unionClusterAfter(baseField, curArr) {
  if (baselineFromReal.value) {
    return canonTypes([...(curArr || [])].sort().join('、'))
  }
  const b = changeBaseline.value
  const baseCodes = b ? String(baseField(b) || '').split(/[、,，]/).map(s => s.trim()).filter(Boolean) : []
  const set = new Set([...baseCodes, ...(curArr || [])])
  return canonTypes([...set].sort().join('、'))
}
// 变更前权益期限(基线卡片口径):上一版正常权益卡片的最早到期日(多权利多卡取最早,即最紧的期限约束)
const baselineValidBefore = computed(() => {
  const dates = (baselineCards.value || [])
    .map(c => c.validDate ? String(c.validDate).slice(0, 10) : '')
    .filter(Boolean)
    .sort()
  return dates.length ? dates[0] : ''
})
// 系统级维度:5 个申报人可改维(从基线预填,无幻象风险,恒纳入对照)+ 3 个元素簇维(cluster,按勾选触发门控)
// 权益期限维:未申报新期限视为"维持原值"(避免误报"日期→空");到期触发未填由提交校验+期望清单拦截提示
const CHANGE_DIMS = [
  { key: '权属主体', cur: () => form.rightHolder, base: b => b.rightHolder },
  { key: '主体层级', cur: () => form.subjectLevel, base: b => b.subjectLevel },
  { key: '权属类型', cur: () => canonTypes((form.rightTypes || []).join('、')), base: b => canonTypes(b.rightType) },
  { key: '责任部门', cur: () => form.respDept, base: b => b.respDept },
  { key: '管制属性', cur: () => form.regulated, base: b => b.regulated },
  { key: '来源识别(A–F)', cluster: 'source', cur: () => unionClusterAfter(b => b.sourceIdent, form.sourceIdent), base: b => canonTypes(b.sourceIdent) },
  { key: '信息关联(G–J)', cluster: 'relation', cur: () => unionClusterAfter(b => b.relationIdent, form.relationIdent), base: b => canonTypes(b.relationIdent) },
  { key: '权益期限', cluster: 'validity',
    cur: () => (form.validDate ? String(form.validDate).slice(0, 10) : baselineValidBefore.value),
    base: () => baselineValidBefore.value }
]
const norm = (v) => (v == null ? '' : String(v).trim())
// 生效对照维度:簇维仅当对应触发被勾选才纳入(分簇 diff)
// 系统级来源/关联簇「始终」退出变更对照:form.sourceIdent 恒从选中的子集表派生,拿子集聚合去和全量基线对照
// 两头都错——未改误报删减(选1表 A,基线 A、E → 幻象 A、E→A)、已改并集掺旧码(A→B 变 A、B)。
// 来源/关联的真实对照一律走下方「逐表来源/关联对照(选中表)」明细(带数据表名称,基线→当前+状态)。
// 仅保留权益期限簇(validity)与非簇系统级维度(权属主体/层级/类型/责任部门/管制属性)。
const activeDims = computed(() => CHANGE_DIMS.filter(d =>
  !d.cluster
  || (d.cluster === 'validity' && keepValidityCluster.value)))
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
// 逐表改动明细(数据表名称级):把 changedTableRows 按改动字段展开成「数据表名称/变更项/原值/新值」,让申报人看清哪张表改了什么
const PF_LABELS = {
  sourceType: '来源类型', sourceSubject: '来源主体名称', sourceDesc: '来源权益限制摘要',
  gSubject: 'G 关联主体说明', hSubject: 'H 关联主体说明', iSubject: 'I 关联主体说明', jSubject: 'J 关联主体说明',
  gFlag: 'G 信息关联', hFlag: 'H 信息关联', iFlag: 'I 信息关联', jFlag: 'J 信息关联'
}
// 逐表来源/关联对照(选中库表):按触发簇,对每张选中表显示 基线值→当前值 + 状态(维持原值/已修订)。
// 来源簇:每张表恒显示「来源类型」(哪怕未改,原值→新值=A→A 标维持)——满足申报人"选了表就看到它来源现状";
// 关联簇:仅列改动的 G–J 关联主体(4 标志×N 表全列过噪)。其余改动字段(来源主体/说明)改动时补行。
const changedTableDetail = computed(() => {
  if (form.registerType !== '确权变更' || isDataAdd.value) return []
  const out = []
  // 某行对象的 G–J 关联类型串(是=纳入),基线用 _pf、当前用 row;空则「无」
  const gjOf = (o) => relationOpts.filter(r => (o[r.v.toLowerCase() + 'Flag'] || '') === '是').map(r => `${r.v} ${r.t}`).join('、') || '无'
  const add = (name, dim, b, a) => out.push({ tableName: name, dim, before: b || '空', after: a || '空', changed: (a || '') !== (b || '') })
  for (const row of tableItems.value) {
    if (!row._pf) continue
    const name = row.tableName || row.tableCode
    if (keepSourceCluster.value) {
      add(name, PF_LABELS.sourceType, row._pf.sourceType, row.sourceType)                // 来源类型恒显示(未改=维持)
      if ((row.sourceSubject || '') !== (row._pf.sourceSubject || '')) add(name, PF_LABELS.sourceSubject, row._pf.sourceSubject, row.sourceSubject)
      if ((row.sourceDesc || '') !== (row._pf.sourceDesc || '')) add(name, PF_LABELS.sourceDesc, row._pf.sourceDesc, row.sourceDesc)
    }
    if (keepRelationCluster.value) {
      add(name, '信息关联类型(G–J)', gjOf(row._pf), gjOf(row))                            // 信息关联类型恒显示(基线→当前,对称于来源)
      for (const k of ['gSubject', 'hSubject', 'iSubject', 'jSubject']) {                 // 关联主体说明改动补行
        if (row._pf[k] !== undefined && (row[k] || '') !== (row._pf[k] || '')) add(name, PF_LABELS[k], row._pf[k], row[k])
      }
    }
  }
  return out
})
const changeSummary = computed(() => {
  if (form.registerType !== '确权变更' || !changeBaseline.value) return ''
  const sys = changeDiff.value
  // 口径对齐:逐表改动数 = 明细表中「已修订」行涉及的去重库表数(与下方明细同源,杜绝"说改了却不显")
  const tblN = new Set(changedTableDetail.value.filter(r => r.changed).map(r => r.tableName)).size
  if (!sys.length && !tblN) return `确权变更(触发:${form.changeTrigger || '未选'})—— 暂未检测到与原确权结论的差异,请核对是否确需变更`
  const segs = sys.map(d => `${d.key}「${d.before || '空'}→${d.after || '空'}」`)
  if (tblN) segs.push(`${tblN} 张库表来源/关联要素逐表调整(前后留痕见「编辑表2」)`)
  return `本次确权变更(触发:${form.changeTrigger || '未选'})共修改 ${sys.length + tblN} 项:` + segs.join('、')
})
// P1′ 期望变更点:触发类型 → 应变更对象清单(结果导向 diff 之前先给期望;met=对应改动是否已实际发生)
const TBL_SOURCE_KEYS = ['sourceType', 'sourceSubject', 'sourceDesc']
const TBL_RELATION_KEYS = ['gFlag', 'hFlag', 'iFlag', 'jFlag', 'gSubject', 'hSubject', 'iSubject', 'jSubject']
const rowFieldChanged = (row, keys) => keys.some(k => row._pf && row._pf[k] !== undefined && (row[k] || '') !== (row._pf[k] || ''))
const tableSourceChanged = computed(() => changedTableRows.value.some(r => rowFieldChanged(r, TBL_SOURCE_KEYS)))
const tableRelationChanged = computed(() => changedTableRows.value.some(r => rowFieldChanged(r, TBL_RELATION_KEYS)))
const dimChanged = (key) => changeDiff.value.some(d => d.key === key)
const expectedPoints = computed(() => {
  if (form.registerType !== '确权变更' || isDataAdd.value || !changeTriggers.value.length) return []
  const pts = []
  if (changeTriggers.value.some(t => t.includes('来源'))) {
    pts.push({
      label: '数据来源要素已修订(逐表 来源类型/主体/说明,或系统级 A–F)',
      met: tableSourceChanged.value || dimChanged('来源识别(A–F)'),
      tip: '请到对应库表「编辑表2」修订来源要素,并在材料步骤提交新来源凭证'
    })
  }
  if (changeTriggers.value.some(t => t.includes('管理') || t.includes('监管'))) {
    pts.push({
      label: '管理要求要素已修订(逐表 G–J 标志/关联说明,或系统级 G–J)',
      met: tableRelationChanged.value || dimChanged('信息关联(G–J)'),
      tip: '请到「编辑表2」修订 G–J 关联要素与合规说明'
    })
  }
  if (changeTriggers.value.some(t => t.includes('到期'))) {
    pts.push({
      label: `新权益有效期已申报(变更前:${baselineValidBefore.value || '无固定期限'})`,
      met: !!form.validDate,
      tip: '请在「申报权益有效期」选择续期日期,并在材料步骤提交新签授权协议/凭证'
    })
  }
  return pts
})
const expectedNoneMet = computed(() => expectedPoints.value.length > 0 && expectedPoints.value.every(p => !p.met))

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
// P0 diff 物化随单:提交时把变更对照(七维/逐表/表4)固化为快照 JSON 存 CEC_CHANGE_DIFF。
// diff 是"申报时刻的事实陈述",随单流转 —— 审核/追溯永远看同一份,不随基线被他单取代而漂移。
const splitCodes = (s) => String(s || '').split(/[、,，]/).map(x => x.trim()).filter(Boolean)
function materializeChangeDiff() {
  if (form.registerType !== '确权变更' || !changeBaseline.value) return ''
  const b = changeBaseline.value
  const baseline = {
    sysName: b.sysName, version: b.version || 1,
    priorApplyId: baselinePriorApplyId.value || '', fromReal: baselineFromReal.value
  }
  if (isDataAdd.value) {
    return JSON.stringify({
      v: 1, mode: 'insert', trigger: form.changeTrigger, baseline,
      // P1′ 集合级前后对照:前 N 张已确权 → 后 N+M 张(审核侧同源展示)
      counts: { prior: baselineTableCount.value, added: tableItems.value.length },
      newTables: tableItems.value.map(t => ({ tableCode: t.tableCode, tableName: t.tableName })),
      dims: [], tables: []
    })
  }
  // 维度级(码值簇维带新增/移除码三态;权益期限簇是日期值,只做 before/after 不拆码)
  const dims = activeDims.value
    .map(d => {
      const before = norm(d.base(b))
      const after = norm(d.cur())
      if (before === after) return null
      const row = { key: d.key, before, after }
      if (d.cluster === 'source' || d.cluster === 'relation') {
        const bs = splitCodes(before)
        const as = splitCodes(after)
        row.added = as.filter(c => !bs.includes(c))
        row.removed = bs.filter(c => !as.includes(c))
      }
      return row
    })
    .filter(Boolean)
  // 逐表级(对照平台/基线快照 _pf 的字段级改动)
  const tables = changedTableRows.value.map(row => ({
    tableCode: row.tableCode, tableName: row.tableName,
    changes: PF_KEYS
      .filter(k => row._pf[k] !== undefined && (row[k] || '') !== (row._pf[k] || ''))
      .map(k => ({ field: k, before: row._pf[k] || '', after: row[k] || '' }))
  }))
  // 表4 权益对照(归集试算已出结果才随单;'待判定'不固化)
  const table4 = table4Diff.value.filter(r => r.mark !== '待判定')
  // P1′ 期望变更点(按触发类型的应变更对象+达成状态)随单固化,审核人先看期望再看结果
  const expected = expectedPoints.value.map(p => ({ label: p.label, met: p.met }))
  return JSON.stringify({ v: 1, mode: 'update', trigger: form.changeTrigger, baseline, dims, tables, table4, expected })
}

// R1 登记类型派生横幅:类型由资产确权状态唯一决定(已确权→变更,否则→初始),不做随手 radio。
// 已确权资产如确需"重新初始登记",经二次确认方可覆盖,杜绝误选造成重复确权。
function requestInitialOverride() {
  confirmAsync(
    '该资产已存在确权结论。「重新初始登记」将作为全新初始确权提交,可能与既有确权结论重复或冲突;如只是信息发生变动,请用「确权变更」。确认要重新初始登记吗?',
    '重新初始登记确认',
    async () => {
      form.registerType = '初始确权'
      changeTriggers.value = [] // 初始确权无变更触发(watch 同步 form.changeTrigger='')
    },
    { confirmButtonText: '确认重新初始登记', cancelButtonText: '取消' }
  ).catch(() => { /* 取消:维持确权变更 */ })
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
  // 路由守卫(P0):已确权资产误入 initial 路由 → 重定向 change 路由。
  // changeMode 门控(变更触发勾选/未确权过滤/基线载入/授权影响门禁/版本留痕)全部挂在 route.meta 上,
  // 只翻 registerType 不换路由会造成"类型=变更但触发不可选"的断头单。
  if (assetConfirmed.value && !changeMode.value) {
    ElMessage.info('该资产已确权,已转入「确权变更申请」')
    await router.replace({ path: '/dpr/confirm/change', query: { ...route.query, assetId } })
    return
  }
  if (!assetConfirmed.value) {
    ElMessage.warning('该资产尚未确权,已按「初始确权」进入;完成初始确权后方可发起确权变更')
  }
}

// 一键填充示例(测试/演示):系统级初始确权——选定「营销管理系统」整系统带入(对齐 35号文 表1 系统级,
// 不下钻库表,与真实左树选系统口径一致)。系统责任信息/申报主体/A–J 由平台元数据(cardsBySystem 桩)自动带出,
// 仅补演示专属字段(权属类型/登记类型/用途)+ 表2 争议表缓冲。
async function fillDemo() {
  // 整系统带入:等价于在左树勾「营销管理系统」(wholeSystem);自动拉该系统全部未确权库表 + 聚合 A–J + 带系统责任/申报主体
  await onTreeSelect('营销管理系统', [], '市场营销域', { wholeSystem: true })
  Object.assign(form, {
    rightTypes: ['持有权', '使用权'],
    registerType: '初始确权', applyMode: '常规', regulated: '管制业务',
    purpose: '营销域数据系统级初始确权(示例)'
  })
  // 表2 批量缓冲(示例):带库表后套用到争议表(本系统涉个人隐私 H 的库表),演示逐表表2
  Object.assign(b2, {
    sourceSubject: '中国南方电网有限责任公司',
    sourceDesc: '自行生产数据,由营销管理系统建设投入形成;涉个人信息字段对外提供须脱敏并经客户授权',
    hSubject: '用电客户个人信息,依据用户入网协议已取得对外提供授权,范围限定于结算与征信场景',
    jSubject: '', riskDesc: '未经授权对外提供个人信息存在合规风险'
  })
  if (disputedRows.value.length) applyBatchToDisputed(true)
  ElMessage.success(`已填充系统级示例「营销管理系统」(${tableItems.value.length} 张未确权库表),可直接"下一步"`)
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

// 表单 → 落库 payload(保存草稿 / 下一步 共用单一真源;避免两处序列化漂移)
function buildPayload() {
  return {
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
    ...(changeMode.value && changeBaseline.value ? {
      baselineRef: `${changeBaseline.value.sysName}#v${changeBaseline.value.version}` + (baselinePriorApplyId.value ? `@${baselinePriorApplyId.value}` : ''),
      changeVersion: isDataAdd.value ? 1 : (changeBaseline.value.version || 1) + 1,
      changeSummary: isDataAdd.value
        ? `数据新增:新增 ${tableItems.value.length} 张库表首次确权登记(既有已确权库表不动,不联动授权)`
        : changeSummary.value,
      changeDiff: materializeChangeDiff()
    } : {})
  }
}

// 主动保存草稿(PDD 8.1 状态机·独立存草稿):宽进——仅守"已选系统+库表"落库底线(后端 CEC_ASSET_NAME NOT NULL),
// 不做完成度校验、不推进步骤、不跳页。与「下一步」的严校验解耦,让半成品可随时暂存、离开后续填。
async function saveDraft() {
  if (!form.systemName || !tableItems.value.length) {
    ElMessage.warning('请先在左侧「确权范围」树勾选:系统 → 一级功能模块 → 库表,方可暂存草稿')
    return
  }
  saving.value = true
  try {
    applyId.value = await saveConfirmDraft(buildPayload())
    if (tableItems.value.length) await persistTableItems()
    removeLocalDraft(localDraftKey(draftScope.value, 'new', meId())) // 已服务端落库:清本地 'new' 键,转由草稿箱管理
    markDraftSource(applyId.value, 'manual') // 主动「保存草稿」= 手动来源
    notifyDraftChanged()
    ElMessage.success(`草稿已保存(编号 ${applyNo.value || applyId.value})，可随时离开后在「申请草稿箱」或「我的申请」继续`)
  } catch (e) { /* 请求拦截器已 toast 错误 */ } finally { saving.value = false }
}

// ===== 申请草稿·自动保存(防丢:中断/关页/崩溃不丢半成品)=====
// 本地即时写入 + 达"系统+库表"底线后 debounce 服务端同步(复用 saveDraft 去重);静默宽进,不校验、不前进。
const autosaveReady = ref(false)     // 初始回填期间不触发,避免"载入即回写 / 空存"
const draftScope = computed(() => (changeMode.value ? 'confirm-change' : 'confirm-initial'))
const meId = () => (currentUser() && currentUser().userId) || ''
const draftKey = () => localDraftKey(draftScope.value, applyId.value || 'new', meId())
const canServerAutosave = () => !!form.systemName && tableItems.value.length > 0 && !saving.value && !submitting.value
async function serverAutosave() {
  const first = !applyId.value
  applyId.value = await saveConfirmDraft(buildPayload())
  if (tableItems.value.length) await persistTableItems()
  markDraftSource(applyId.value, 'auto') // 自动保存来源
  if (first && applyId.value) { removeLocalDraft(localDraftKey(draftScope.value, 'new', meId())); notifyDraftChanged() }
}
const autosave = useDraftAutosave({
  getKey: draftKey,
  getSnapshot: () => {
    // 有实质内容才存:至少选了系统/库表 或 填了权属主体
    if (!form.systemName && !tableItems.value.length && !form.rightHolder) return { __skip: true }
    return {
      form: JSON.parse(JSON.stringify(form)),
      tableItems: JSON.parse(JSON.stringify(tableItems.value)),
      changeTriggers: [...changeTriggers.value],
      applyId: applyId.value, applyNo: applyNo.value, step: step.value,
      registerType: form.registerType,
      title: form.systemName ? `${form.systemName} · ${tableItems.value.length} 表` : '(未选系统)'
    }
  },
  serverSync: serverAutosave,
  canServer: canServerAutosave
})
const autoTip = computed(() => (autosave.syncing.value ? '正在自动保存…' : (autosave.lastSavedAt.value ? `已自动保存 ${autosave.lastSavedAt.value}` : '')))
// 表单/表2/触发动因变化 → 计划自动保存(composable 内部:本地即时 + 服务端 debounce)
watch([form, tableItems, changeTriggers], () => { if (autosaveReady.value) autosave.schedule() }, { deep: true })
// 切步即刻本地落一笔(仅本地,不打后端)
watch(step, () => { if (autosaveReady.value) autosave.schedule({ server: false }) })

// 找回:全新进入(无 applyId/assetId/reopen)时,提示恢复本地未提交草稿
async function maybeRecoverLocalDraft() {
  const snap = readLocalDraft(localDraftKey(draftScope.value, 'new', meId()))
  const hasContent = snap && (snap.form?.systemName || (snap.tableItems || []).length || snap.form?.rightHolder)
  if (!hasContent) return
  const when = snap.__ts ? new Date(snap.__ts).toLocaleString() : ''
  try {
    await ElMessageBox.confirm(
      `检测到一份未提交的${changeMode.value ? '确权变更' : '初始确权'}草稿${when ? `(最后编辑 ${when})` : ''},是否恢复继续填写?`,
      '恢复未完成的草稿', { confirmButtonText: '恢复', cancelButtonText: '丢弃', type: 'info' }
    )
    Object.assign(form, snap.form || {})
    tableItems.value = snap.tableItems || []
    changeTriggers.value = snap.changeTriggers || []
    ElMessage.success('已恢复本地草稿,可继续填写')
  } catch { removeLocalDraft(localDraftKey(draftScope.value, 'new', meId())) } // 丢弃
}

// 步骤1 -> 2:暂存草稿,生成 A–J 应交材料清单
async function next0() {
  // 系统级入口守卫:未在左树选系统/库表则 assetName/systemName 为空,挡在前端(后端 CEC_ASSET_NAME NOT NULL)
  if (!form.systemName || !tableItems.value.length) {
    ElMessage.warning('请先在左侧「确权范围」树勾选:系统 → 一级功能模块 → 库表'); return
  }
  try {
    await formRef.value.validate()
  } catch (fields) {
    identityPanel.value = ['id'] // 校验失败:展开身份摘要,避免折叠中的必填项错误不可见
    // P3 集中校验:一次性列出缺失必填项 + 定位首个,免长表单逐个找红字
    const labelMap = { rightHolder: '申报权属主体', subjectLevel: '主体层级', systemOwner: '系统负责人', contactInfo: '联系方式' }
    const missKeys = fields && typeof fields === 'object' ? Object.keys(fields) : []
    if (missKeys.length) {
      ElMessage.warning(`还差 ${missKeys.length} 项必填未完成:${missKeys.map(k => labelMap[k] || k).join('、')}`)
      formRef.value.scrollToField(missKeys[0])
    }
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
  // P0′ 权益期限维:到期触发的核心变更对象就是"新有效期",未申报即提交没有意义
  if (form.registerType === '确权变更' && changeTriggers.value.includes('权益到期') && !form.validDate) {
    ElMessage.warning(`权益到期变更须申报新的权益有效期(变更前:${baselineValidBefore.value || '无固定期限'}),请在「申报权益有效期」选择续期日期`)
    return
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
    applyId.value = await saveConfirmDraft(buildPayload())
    markDraftSource(applyId.value, 'manual') // 「下一步」为申报人主动推进 = 手动来源
    // 表级清单全量同步(后端按 applyId 删后插),复用 persistTableItems(逐表凭证附件/materialId 已落库·单一真源)
    if (tableItems.value.length) {
      const n = await persistTableItems()
      if (firstSave) ElMessage.success(`已保存 ${n} 张表级清单(M02)`)
    }
    loadConsolidation()
    buildChecklist()
    loadPriorMaterials() // P2′ 变更模式:带出上一版凭证做旧↔新配对(best-effort)
    // 先从平台元数据同步已上传材料(命中项免上传),并回填清单已上传状态(含再入 step1 不丢失)
    await doSyncPlatform(true)
    if (!firstSave && (checkReport.value || aiMatResult.value)) needRecheck.value = true // 申请要素已更新 → 需重新校验
  } finally { saving.value = false }
  step.value = 1
}

// P2′ 旧↔新凭证配对:上一版申请的凭证材料(变更前),与本次应交清单/已传状态按名匹配
const priorMaterials = ref([])
async function loadPriorMaterials() {
  priorMaterials.value = []
  if (form.registerType !== '确权变更' || !baselinePriorApplyId.value) return
  try {
    priorMaterials.value = (await listMaterialByApply(baselinePriorApplyId.value)) || []
  } catch (e) { /* 旧单材料缺失不阻断材料步骤 */ }
}
function priorMatchStatus(m) {
  const name = m.materialName || ''
  const hit = checklist.value.find(c => c.name === name || c.name.includes(name) || name.includes(c.name))
  if (!hit) return { label: '维持原值(不在本次清单)', type: 'info' }
  return hit.done ? { label: '已更新(本次已传新件)', type: 'success' } : { label: '待更新(本次应交)', type: 'warning' }
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
  // 规则过滤收敛至共享单一真源(lib/confirmChecklist),向导与草稿箱缺口计算同源
  const rules = filterRequiredRules(materialRules.value, {
    sourceIdent: form.sourceIdent, relationIdent: form.relationIdent,
    needTable2: needTable2.value, registerType: form.registerType, changeTrigger: form.changeTrigger
  })
  checklist.value = rules.map((r, i) => ({
    code: r.triggerCode || (r.triggerType === 'TABLE2' ? '表2' : '核心'),
    name: r.materialName,
    m: r.triggerLabel || r.evidenceType || '材料',
    required: r.required, detail: r.detail,
    id: 'ck' + i, done: false
  }))
}

// 规则接口不可用时的内置兜底(与默认规则一致),保证向导可离线生成清单
// P3:系统级清单只含 表1/表2/权属凭证 + A 自行生产说明;B–F(非A)/G–J 已逐表化(step2 逐表凭证区)
function buildChecklistFallback() {
  const base = [{ code: '表1', name: '《表1 数据确权信息清单(系统级)》', m: '表1' }, { code: '证明', name: '数据确权证明材料(权属/来源凭证)', m: '证明材料' }]
  if (needTable2.value) base.push({ code: '表2', name: '《表2 数据确权信息清单(涉及第三方权益)》', m: '表2' })
  // 系统级只留 A 自行生产说明(A 不入表2);B–F/G–J 凭证逐表承载
  const picked = sourceOpts.filter(s => s.v === 'A' && form.sourceIdent.includes('A')).map(o => ({ code: o.v, name: o.m, m: o.t }))
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

// 表级清单落库(单一真源);剥离纯前端态字段(逐表凭证附件名 *_Attachment + 上传件 *_MatId 已建列,不 strip)
async function persistTableItems() {
  if (!applyId.value || !tableItems.value.length) return 0
  const items = tableItems.value.map(({ sourceChannel, existTable, _pf, authTime, confirmed,
    authorized, authId, authScope, authStatus, ...rest }) => rest)
  await saveTableItems(applyId.value, items)
  return items.length
}
// P2:逐表凭证上传原件/替换 → 存字节(ConfirmMaterial)+ 写回该表附件名/materialId(ConfirmTableItem 单一真源)
async function onPerTableUpload(row, file) {
  if (!applyId.value) { ElMessage.warning('请先完成步骤1暂存申请'); return }
  const f = file && file.raw
  if (!f) return
  if (!/\.(pdf|docx?|jpe?g|png)$/i.test(f.name)) { ElMessage.warning('仅支持 PDF/Word/JPG/PNG'); return }
  const fd = new FormData()
  fd.append('file', f)
  fd.append('applyId', applyId.value)
  fd.append('materialName', `${row.tableName || row.tableCode}·${row.letter} ${row.slot}`)
  fd.append('materialType', row.slot)
  fd.append('owner', form.rightHolder || '')
  const mid = await uploadMaterialFile(fd) // 存字节,返回 materialId(格式不过会抛错·拦截器 toast)
  row.row[row.attKey] = f.name    // 写回该表附件名(逐表单一真源)
  row.row[row.matIdKey] = mid     // 写回上传件 id(供预览)
  await persistTableItems()
  if (checkReport.value || aiMatResult.value) needRecheck.value = true
  ElMessage.success('原件已上传并写回表2')
}
// P2:逐表仅登记(占位名,无字节)→ 写回该表附件名
function onPerTableRegister(row) {
  row.row[row.attKey] = `${row.tableName || row.tableCode}·${row.letter}${row.slot}(仅登记).pdf`
  row.row[row.matIdKey] = ''
  persistTableItems()
  if (checkReport.value || aiMatResult.value) needRecheck.value = true
  ElMessage.success('已登记')
}
function previewPerTable(row) {
  if (row.matId) openFilePreview(materialFileUrl(row.matId), row.attachment)
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
    autosave.clear() // 已提交:清本地自动保存缓存(applyId 键 + 'new' 键)
    removeLocalDraft(localDraftKey(draftScope.value, 'new', meId()))
    clearDraftSource(applyId.value) // 已提交:清来源记录
    notifyDraftChanged() // 草稿转在审 → 草稿数减一
  } finally { submitting.value = false }
}

function goProgress() { router.push('/dpr/workbench/my') }
function reset() {
  step.value = 0; applyId.value = ''; applyNo.value = ''; quality.value = null
  checklist.value = []; materials.value = []
  Object.assign(form, { assetId: '', assetName: '', systemName: '', rightTypes: [...rightTypes], rightHolder: '', respDept: '', subjectLevel: '', systemOwner: '', contactInfo: '', assetSecretLevel: '', region: '', registerType: '初始确权', changeTrigger: '', applyMode: '常规', purpose: '', thirdPartyInfo: '', sourceSubject: '', sourceLimit: '', relationSubject: '', equityRisk: '', privacyInfo: '', sourceIdent: [], relationIdent: [], validDate: '' })
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
.wz-steps :deep(.wz-step-clickable) { cursor: pointer; }
.wz-steps :deep(.wz-step-clickable .el-step__title),
.wz-steps :deep(.wz-step-clickable .el-step__description) { transition: opacity .15s; }
.wz-steps :deep(.wz-step-clickable:hover .el-step__title) { opacity: .72; }
.wz-body { min-height: 320px; }
.form-tip { font-size: 12px; color: var(--prm-color-text-weak); line-height: 1.6; }
/* 库表清单:筛选条 + 分页布局(规模化) */
.t2-list { width: 100%; }
.t2-filter { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 8px; }
.t2-filter .t2-hit { margin-left: auto; }
.pf-mark { margin-top: 3px; }
/* 经营权归集判定结果卡片:结论做主角,法条折叠 */
.reg-hint { margin-top: 4px; }
.consol-basis :deep(.el-collapse-item__header) { height: 26px; line-height: 26px; border: none; background: transparent; }
.consol-basis :deep(.el-collapse-item__wrap) { border: none; background: transparent; }
.consol-basis :deep(.el-collapse-item__content) { padding-bottom: 6px; }
.approve-chain { max-width: 880px; margin: 8px auto 0; }
/* R3 维持原值折叠区:与 form-item 行距对齐,弱化呈现 */
.change-fold { margin-bottom: 18px; border-top: none; border-bottom: none; }
.change-fold :deep(.el-collapse-item__header) { font-size: 13px; height: 36px; line-height: 36px; }
/* R2 资产身份摘要折叠条:弱化的只读身份摘要,展开可编辑平台带出字段 */
.identity-card { margin-bottom: 20px; border: 1px solid var(--el-border-color-lighter); border-radius: 6px; padding: 0 10px; background: #fafcff; }
.identity-card :deep(.el-collapse-item__header) { height: 40px; line-height: 40px; border-bottom: none; }
.identity-card :deep(.el-collapse-item__wrap) { border-bottom: none; background: transparent; }
/* 系统级确权:左范围树 + 右表单(master-detail) */
.step1-2col { display: flex; gap: 16px; align-items: flex-start; }
.step1-tree { flex: 0 0 280px; }
.step1-form { flex: 1; min-width: 0; }
@media (max-width: 1100px) { .step1-2col { flex-direction: column; } .step1-tree { flex: none; width: 100%; } }
</style>
