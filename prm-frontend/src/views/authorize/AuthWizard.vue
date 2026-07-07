<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!--
  一事一议(专项)授权一站式:单场景·多表。对齐 35号文 表5《数据授权申请单》——多行(序号)申请单。
  一份申请单(formNo) = 同一被授权方 + 同一使用场景(本次"事") + N 张数据表;
  逐表进入审批链(表2 20-100:单位初审→合规→业务→主管→经理→副总→批准),批准后双签附录D+承诺函归档方生效(先签约后执行授权)。
-->
<template>
  <div class="prm-page">
    <PageNote>注:一站式一事一议(专项)授权——填申请单基础信息(本次事项) → 从确权资源池选授权数据(可多张) → 提交前自检并提交,一次办完申报。提交后逐表进入多级审批(本单位初审→合规审查(合规管控小组)→业务部门→主管→经理→副总/总经理批准),批准后经甲乙双签《数据运营授权协议(附录D)》+《保密承诺函》归档方才生效并执行授权·记录(先签约后执行)。授权凭证为协议,非"证书"。</PageNote>

    <!-- 步骤条可点击回跳(el-step 无自身 click emit,@click 原生透传);仅已完成步骤可点,提交后终态不可再跳 -->
    <el-steps :active="submitted ? 3 : step" finish-status="success" align-center class="wz-steps">
      <el-step title="申请单基础信息" description="本次事项:被授权方·场景·权益(表5 共享头)" :class="{ 'wz-step-clickable': stepJumpable(0) }" @click="onStepClick(0)" />
      <el-step title="选择授权数据" description="从确权资源池选取(表5 逐行·可多张)" :class="{ 'wz-step-clickable': stepJumpable(1) }" @click="onStepClick(1)" />
      <el-step title="确认并提交" description="提交前自检 → 逐表进入多级审批" />
    </el-steps>

    <div class="wz-body">
      <!-- 步骤1:申请单基础信息(共享"一事") -->
      <el-card v-show="step === 0" shadow="never">
        <el-form :model="listForm" label-width="160px" style="max-width:660px">
          <el-alert v-if="!formNo" type="info" :closable="false" style="margin-bottom:10px;max-width:700px">
            <template #title>
              第一次填写?可
              <el-button link type="primary" style="vertical-align:baseline" :loading="demoFilling" @click="fillDemo">一键示例:建单并加入示例数据表(测试/演示)</el-button>
              材料包见 test/一事一议授权申请 目录
            </template>
          </el-alert>
          <!-- 字段顺序贴心智:授给谁(被授权方) → 什么权益 → 干什么(场景);权益类型须先于场景过滤/树过滤,故置其后 -->
          <el-form-item label="申请主体(被授权方)" required>
            <el-select v-if="!externalGrantee" v-model="listForm.granteeOrg" filterable allow-create default-first-option clearable
              placeholder="从南网组织树选取(搜不到可直接输入)" style="width:100%">
              <el-option v-for="o in orgOptions" :key="o.id" :label="o.bizOrgName" :value="o.bizOrgName" />
            </el-select>
            <el-input v-else v-model="listForm.granteeOrg" placeholder="外部被授权主体名称(政府/外部企业/社会组织)" clearable />
            <div style="margin-top:4px">
              <el-checkbox v-model="externalGrantee" @change="listForm.granteeOrg = ''">被授权方为外部主体(不在南网组织结构内;对外经营权另备案附录G)</el-checkbox>
            </div>
          </el-form-item>
          <el-form-item label="授权权益类型(单选)" required>
            <el-radio-group v-model="listForm.rightType">
              <el-radio-button v-for="t in rightTypes" :key="t" :label="t">{{ t }}</el-radio-button>
            </el-radio-group>
            <div class="auth-tip">决定下方可选场景与数据范围(资源池:先确后授 + 权属可授 + 经营权对外开放);授权仅授使用权/经营权,持有权经确权认定取得</div>
          </el-form-item>
          <el-form-item label="使用场景(本次事项)" required>
            <el-select v-model="listForm.scenario" filterable allow-create default-first-option clearable
              :placeholder="listForm.rightType ? `选择「${listForm.rightType}」适用场景,或直接输入自定义` : '选择应用场景,或直接输入自定义(一事一议特定事项)'"
              style="width:100%" @change="onScenarioChange">
              <el-option v-for="s in filteredScenarios" :key="s.scenarioId" :label="`${s.scenarioName}（${s.category}）`" :value="s.scenarioName">
                <span>{{ s.scenarioName }}</span>
                <span style="float:right;color:var(--prm-color-text-weak);font-size:12px">{{ s.category }}</span>
              </el-option>
            </el-select>
            <div class="auth-tip">一事一议 = 一个场景/事项;库中没有可<b>直接输入</b>(自定义)。本单全部数据表共享此场景(多场景请走批量授权)</div>
          </el-form-item>
          <el-form-item label="目的摘要(表5)" required>
            <el-input v-model="listForm.purposeNote" type="textarea" :rows="2" maxlength="500" show-word-limit
              placeholder="本次授权数据的使用/经营目的摘要(合规评审判断「特定场景、仅限本次」的依据);选场景后默认带出模板,可编辑" />
          </el-form-item>
          <el-form-item label="授权时效">
            <el-select v-model="listForm.validTerm" style="width:100%" placeholder="默认两年(时长)">
              <el-option v-for="t in validTerms" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-divider content-position="left" style="margin:8px 0"><span style="font-size:12px;color:var(--prm-color-text-weak)">申报联系</span></el-divider>
          <el-form-item label="申请单位主管" required><el-input v-model="listForm.applicantManager" /></el-form-item>
          <el-form-item label="联系方式" required><el-input v-model="listForm.contactInfo" placeholder="电话 / 邮箱" /></el-form-item>
          <!-- 渐进披露:协议要素(附录D §3.4.4)与保密为选填/下游项,默认折叠,降申报摩擦 -->
          <el-collapse style="margin:6px 0 0">
            <el-collapse-item name="more">
              <template #title><span style="font-size:13px;color:var(--prm-color-primary)">更多：协议要素与保密（选填 · 协议签订前可补）</span></template>
              <el-form-item label="利益分配约定">
                <el-input v-model="listForm.benefitAllocation" type="textarea" :rows="2" maxlength="500" show-word-limit
                  placeholder="附录D §3.4.4:如 免费内部共享 / 按调用次数计费 / 收益按比例分成 等(协议签订前可补)" />
              </el-form-item>
              <el-form-item label="安全保障要求">
                <el-input v-model="listForm.securityReq" type="textarea" :rows="2" maxlength="500" show-word-limit
                  placeholder="附录D §3.4.4:如 加密传输、最小授权访问控制、操作留痕审计、数据脱敏、不得转授第三方 等(协议签订前可补)" />
              </el-form-item>
              <el-form-item label="需保密承诺函">
                <el-switch v-model="listForm.needConfidentiality" />
                <span style="margin-left:8px;color:var(--prm-color-text-weak);font-size:12px">附录E;开启后在「选择授权数据」步骤上传《保密承诺函》</span>
              </el-form-item>
            </el-collapse-item>
          </el-collapse>
        </el-form>
        <el-alert v-if="formNo" type="success" :closable="false" show-icon title="申请单已创建,开始从确权目录加入数据表" style="max-width:560px;margin-top:8px">
          <span style="font-size:12px;color:var(--prm-color-text-weak)">单号 {{ formNo }}(系统留痕,无需记忆)</span>
        </el-alert>
      </el-card>

      <!-- 步骤2:选择授权数据(表5 逐行,可多张) -->
      <el-card v-show="step === 1" shadow="never">
        <div class="batch-primary">
          <el-button type="primary" size="large" @click="openPicker">① 从确权目录选取数据资产(可多张)</el-button>
          <span class="batch-primary-hint">
            目录按「选系统 → 选模块 → 选库表」展示,仅列<b>当前权益类型可授</b>的已确权数据表(经营权另需在对外开放目录);
            勾选后自动套用单头(被授权方/场景/时效/权益/协议要素)+ 确权带出(业务域/第三方/隐私),可跨系统一次加入。
          </span>
        </div>
        <div v-if="requiredChecklist.length" style="margin:8px 0 4px">
          <div style="font-weight:600;margin-bottom:8px">应交材料清单 —《表5》由系统按数据表多行自动生成;第三方凭证 / 信息授权协议见下方明细逐表列(确权带出·缺则就地补传)</div>
          <el-table :data="requiredChecklist" border size="small" style="max-width:880px">
            <el-table-column type="index" label="序号" width="56" align="center" />
            <el-table-column prop="materialName" label="应交材料" min-width="170" show-overflow-tooltip />
            <el-table-column prop="required" label="要求" width="84" align="center">
              <template #default="{ row }"><span :class="'prm-c-' + ((row.required === '必填' ? 'danger' : 'warning') || 'primary')">{{ row.required }}</span></template>
            </el-table-column>
            <el-table-column prop="detail" label="内容与要求明细" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="86" align="center">
              <template #default="{ row }">
                <span v-if="isTable5(row)" class="prm-c-success">系统生成</span>
                <span v-else :class="'prm-c-' + ((row.uploaded ? 'success' : 'info') || 'primary')">{{ row.uploaded ? '已上传' : '待上传' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="186" align="center">
              <template #default="{ row }">
                <el-tooltip v-if="isTable5(row)" :disabled="items.length>0" content="请先加入数据表" placement="top">
                  <span><el-button link type="primary" :disabled="items.length===0" @click="genTable5">生成《表5》并下载</el-button></span>
                </el-tooltip>
                <el-upload v-else :show-file-list="false" :http-request="(o)=>uploadConfFile(o.file)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" style="display:inline-block">
                  <el-button link type="primary" :loading="confUploading">{{ row.uploaded ? '重新上传' : '上传' }}</el-button>
                </el-upload>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="batch-detail-head">
          已加入数据表({{ items.length }} 张)
          <span class="batch-detail-sub">单头默认逐行沿用,可逐表微调场景/时效</span>
        </div>
        <el-alert v-if="items.length" :type="crossSystemInfo.isCross || crossSystemInfo.anyGeo ? 'warning' : 'info'" :closable="false" style="margin-bottom:8px;max-width:880px">
          <template #title>
            <span>跨系统域:</span>
            <span style="margin:0 6px" :class="'prm-c-' + ((crossSystemInfo.isCross ? 'warning' : 'info') || 'primary')">{{ crossSystemInfo.isCross ? '是(跨系统域)' : '否(单系统)' }}</span>
            <span>跨地域:</span>
            <span style="margin:0 6px" :class="'prm-c-' + ((crossSystemInfo.anyGeo ? 'warning' : 'info') || 'primary')">{{ externalGrantee ? '涉外部主体' : (crossSystemInfo.anyGeo ? '是(被授权方省≠数据归属省)' : '否') }}</span>
            本单覆盖 {{ crossSystemInfo.systems.length }} 个系统{{ crossSystemInfo.systems.length ? '(' + crossSystemInfo.systems.join('、') + ')' : '' }};均已自动判定并写入各数据表行(表5「是否跨区域、跨域」)。
          </template>
        </el-alert>
        <!-- 列序/列名严格对齐《表5 数据授权申请单》(申请主体名称/申请单位主管/联系方式为单头共享字段,已在步骤1填,不在此逐行重复) -->
        <el-table :data="pagedItems" border size="small" :row-class-name="itemRowClass" @filter-change="onItemsFilterChange">
          <el-table-column type="index" label="#" width="44" align="center" :index="(i) => (itemsPage - 1) * itemsPageSize + i + 1" />
          <el-table-column prop="assetName" label="数据表名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="systemName" label="所属系统" min-width="120" show-overflow-tooltip column-key="sys"
            :filters="sysFilterOpts" :filter-method="matchSystemFilter">
            <template #default="{ row }">{{ row.systemName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="schemaName" label="模式名称" min-width="110" show-overflow-tooltip><template #default="{ row }">{{ row.schemaName || '—' }}</template></el-table-column>
          <el-table-column prop="businessDomain" label="所属业务域" min-width="100" show-overflow-tooltip><template #default="{ row }">{{ row.businessDomain || '—' }}</template></el-table-column>
          <el-table-column prop="rightType" label="申请权益类型" width="140" column-key="rightType"
            :filters="[{ text: '使用权', value: '使用权' }, { text: '经营权', value: '经营权' }]" :filter-method="matchRightTypeFilter" />
          <el-table-column prop="equityCardId" label="生效卡片" min-width="120" show-overflow-tooltip><template #default="{ row }">{{ row.equityCardId || '—' }}</template></el-table-column>
          <el-table-column label="使用场景及目的摘要" min-width="150">
            <template #default="{ row }"><el-input v-model="row.scenario" size="small" placeholder="(沿用单头默认)" @change="persistItem(row)" /></template>
          </el-table-column>
          <el-table-column label="权益时效" width="110" align="center">
            <template #default="{ row }">
              <el-select v-model="row.validTerm" size="small" style="width:94px" @change="onTermChange(row)">
                <el-option v-for="t in validTerms" :key="t" :label="t" :value="t" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="是否跨区域、跨域" width="130" align="center" column-key="cross"
            :filters="[{ text: '是', value: true }, { text: '否', value: false }]"
            :filter-method="matchCrossFilter">
            <template #default="{ row }">
              <el-tooltip :content="externalGrantee ? '被授权方为外部主体,省域归属不可解析,请合规审查关注' : (row.crossGeo ? `跨地域:被授权方省 ≠ 归属主体省(${row.ownerOrg || '—'})` : (crossSystemInfo.isCross ? '跨系统域(本单覆盖多系统)' : '单系统·同省'))" placement="top">
                <span :class="'prm-c-' + ((externalGrantee ? 'danger' : (crossSystemInfo.isCross || row.crossGeo ? 'warning' : 'info')) || 'primary')">
                  {{ externalGrantee ? '涉外部' : (crossSystemInfo.isCross || row.crossGeo ? '是' : '否') }}
                </span>
              </el-tooltip>
            </template>
          </el-table-column>
          <!-- 涉及第三方来源方式:显示确权带出的实际来源方式文字(不是单纯是/否),筛选按"是否涉及"分组 -->
          <el-table-column label="涉及第三方来源方式" width="140" column-key="thirdParty"
            :filters="[{ text: '涉及', value: true }, { text: '不涉及', value: false }]"
            :filter-method="matchThirdPartyFilter">
            <template #default="{ row }">
              <span v-if="row.thirdPartySource && String(row.thirdPartySource).trim()" class="prm-c-warning">{{ row.thirdPartySource }}</span>
              <span v-else style="color:var(--prm-color-text-disabled)">不涉及</span>
            </template>
          </el-table-column>
          <el-table-column label="第三方许可凭证或说明" width="140" align="center">
            <template #default="{ row }">
              <span v-if="!(row.thirdPartySource && String(row.thirdPartySource).trim())" style="color:var(--prm-color-text-weak)">—</span>
              <el-tooltip v-else-if="row.thirdPartyLicense && String(row.thirdPartyLicense).trim()" :content="`已具备:${row.thirdPartyLicense}`" placement="top">
                <span class="prm-c-success">已具备</span>
              </el-tooltip>
              <el-upload v-else :show-file-list="false" :http-request="(o)=>uploadRowCredential(row, o.file)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" style="display:inline-block">
                <el-button link type="warning" size="small">待补传</el-button>
              </el-upload>
            </template>
          </el-table-column>
          <el-table-column label="涉及个人隐私/商业秘密" width="150" align="center" column-key="sensitive"
            :filters="[{ text: '涉及', value: true }, { text: '不涉及', value: false }]"
            :filter-method="matchSensitiveFilter">
            <template #default="{ row }">
              <span :class="'prm-c-' + ((row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无' ? 'danger' : 'info') || 'primary')">
                {{ row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无' ? row.sensitiveType : '不涉及' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="信息授权协议" width="124" align="center">
            <template #default="{ row }">
              <span v-if="!(row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无')" style="color:var(--prm-color-text-weak)">—</span>
              <el-tooltip v-else-if="row.infoAuthAgreement && String(row.infoAuthAgreement).trim()" :content="`已具备:${row.infoAuthAgreement}`" placement="top">
                <span class="prm-c-success">已具备</span>
              </el-tooltip>
              <el-upload v-else :show-file-list="false" :http-request="(o)=>uploadRowPrivacy(row, o.file)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" style="display:inline-block">
                <el-button link type="warning" size="small">待补传</el-button>
              </el-upload>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center" fixed="right">
            <template #default="{ row }"><el-button link type="danger" size="small" @click="removeItem(row, items.indexOf(row))">删除</el-button></template>
          </el-table-column>
        </el-table>
        <el-pagination v-if="items.length" style="margin-top:12px;justify-content:flex-end" background
          layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
          :total="filteredItems.length" :current-page="itemsPage" :page-size="itemsPageSize"
          @current-change="p => itemsPage = p" @size-change="s => { itemsPageSize = s; itemsPage = 1 }" />
        <el-empty v-if="items.length===0" :image-size="60" description="尚未加入数据表 — 点上方「① 从确权目录选取数据资产」" />
      </el-card>

      <!-- 步骤3:提交前自检 + 提交 -->
      <el-card v-show="step === 2" shadow="never">
        <el-result icon="info" :title="`提交一事一议授权申请单（${items.length} 张数据表）`" sub-title="提交前自检 → 本单位初审 → 合规审查 → 业务部门审核 → 主管/经理/副总批准 → 双签生效" />
        <div style="text-align:center;margin:4px 0 10px">校验状态:<span :class="'prm-c-' + ((checkStatus.type) || 'primary')">{{ checkStatus.text }}</span></div>
        <div style="text-align:center">
          <el-button :type="complianceResult && !complianceResult.allPass ? 'danger' : 'primary'" :loading="complianceChecking" @click="runComplianceCheck">
            {{ complianceResult ? '② 重新一键自检' : '① 一键自检(全部数据表)' }}
          </el-button>
          <el-tooltip :disabled="canSubmit" content="请先通过提交前自检(全部数据表合规)" placement="top">
            <span style="margin-left:10px"><el-button type="success" :disabled="!canSubmit" :loading="submitting" @click="doSubmit">② 提交申请单</el-button></span>
          </el-tooltip>
        </div>
        <div v-if="complianceResult && complianceResult.items && complianceResult.items.length" style="margin-top:20px">
          <div style="font-weight:600;margin-bottom:6px">校验明细(共 {{ complianceResult.items.length }} 张,其中 {{ complianceResult.blockedCount }} 张需处理)— 悬停维度看说明</div>
          <el-table :data="complianceResult.items" border size="small" max-height="320">
            <el-table-column type="index" label="#" width="44" align="center" />
            <el-table-column prop="assetName" label="数据表" min-width="150" show-overflow-tooltip />
            <el-table-column label="校验维度（✓ 通过 / ✗ 不通过 / — 不涉及）" min-width="420">
              <template #default="{ row }">
                <el-tooltip v-for="dd in row.dims" :key="dd.name" :content="`${dd.name}:${dd.note}`" placement="top">
                  <span style="margin:2px 4px 2px 0" :class="'prm-c-' + ((dd.note === '—' ? 'info' : (dd.ok ? 'success' : 'danger')) || 'primary')">
                    {{ dd.note === '—' ? '—' : (dd.ok ? '✓' : '✗') }} {{ dd.name }}
                  </span>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="结论" width="84" align="center">
              <template #default="{ row }"><span :class="'prm-c-' + ((row.pass ? 'success' : 'danger') || 'primary')">{{ row.pass ? '通过' : '不通过' }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="92" align="center">
              <template #default="{ row }">
                <el-button v-if="!row.pass" link type="primary" @click="goFixItem(row)">去修正</el-button>
                <span v-else style="color:var(--prm-color-text-weak)">—</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>

      <!-- 提交成功页 + 后续流转进度时间轴(对齐 35号文 附录C 表2 一事一议) -->
      <el-card v-show="submitted" shadow="never">
        <el-result icon="success" title="一事一议授权申请单已提交" :sub-title="`申请单 ${formNo}（${items.length} 张数据表）已逐表进入审批链,当前待「本单位初审」`">
          <template #extra>
            <div class="wz-progress">
              <div class="wz-progress-t">后续流转进度(实时以「我的申请」为准)</div>
              <AuthFlowProgress mode="single" current="unit" />
            </div>
            <div style="margin-top:6px">
              <el-button type="primary" @click="go('/dpr/workbench/my')">去「我的申请」查看进度</el-button>
              <el-button @click="go('/dpr/auth/agreement-seal')">协议双签(附录D)</el-button>
              <el-button @click="reset">再发起一笔</el-button>
            </div>
          </template>
        </el-result>
      </el-card>
    </div>

    <PageActions v-if="!submitted">
      <el-button v-if="step > 0" @click="step--">上一步</el-button>
      <!-- PDD 8.1 独立「保存草稿」:step 0/1/2 均可主动暂存(宽进,不校验单头完整度、不前进);中途离开可在草稿箱续填 -->
      <el-button :loading="savingDraft" @click="saveDraftForm()">保存草稿</el-button>
      <span v-if="autoTip" class="prm-c-weak" style="margin-left:8px;font-size:12px">{{ autoTip }}</span>
      <el-button v-if="step === 0" type="primary" :loading="creating" @click="next0">下一步</el-button>
      <el-button v-if="step === 1" type="primary" :disabled="items.length===0" @click="step = 2">下一步</el-button>
    </PageActions>

    <!-- 从确权目录多选数据资产(资源池:先确后授 + 权属可授 + 经营权对外开放) -->
    <el-dialog v-model="pickerDlg" title="从确权目录选取数据资产(可多张)" width="600px" align-center>
      <el-alert type="info" :closable="false" style="margin-bottom:10px">
        仅列「{{ listForm.rightType || '所选权益' }}」可授的已确权数据表(有生效卡片{{ listForm.rightType === '经营权' ? ' + 在对外开放目录' : '' }});
        被授权方/场景/时效/权益取单头,勾选多个可跨系统一次加入。
      </el-alert>
      <GrantableCatalogTree v-if="pickerDlg" :right-type="listForm.rightType" @change="onPickChange" />
      <template #footer>
        <span style="float:left;color:var(--prm-color-text-weak);font-size:12px;line-height:32px">已勾选 {{ pickedLeaves.length }} 张数据表</span>
        <el-button type="primary" :loading="picking" :disabled="!pickedLeaves.length" @click="confirmPick">加入选中资产</el-button>
        <el-button @click="pickerDlg=false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import PageNote from '@/components/PageNote.vue'
import { reactive, ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { confirmAsync } from '@/utils/confirmAsync'
import { currentUser } from '@/api/auth'
import { useDraftAutosave, localDraftKey, readLocalDraft, removeLocalDraft } from '@/composables/useDraftAutosave'
import { notifyDraftChanged } from '@/lib/draftCount'
import { markDraftSource, clearDraftSource } from '@/lib/draftSource'
import { createAuthForm, saveAuthDraft, deleteAuthApply, submitAuthForm, checkAuthFormCompliance,
  listAuthByForm, listAuthMaterialRules, uploadAuthMaterialFile, pageScenario } from '@/api/authorize'
import { pageEquityCard, getRightsFacts } from '@/api/confirm'
import { listOrg, resolveOrg } from '@/api/org'
import { downloadTable5 } from '@/lib/table5Export'
import GrantableCatalogTree from './GrantableCatalogTree.vue'
import PageActions from '@/components/PageActions.vue'
import AuthFlowProgress from '@/components/AuthFlowProgress.vue'

const router = useRouter()
const route = useRoute()
const rightTypes = ['使用权', '经营权']
const validTerms = ['两年', '三年', '五年']
const TERM_YEARS = { 两年: 2, 三年: 3, 五年: 5 }
function expiryOf(term) {
  const years = TERM_YEARS[term]
  if (!years) return ''
  const d = new Date()
  d.setFullYear(d.getFullYear() + years)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const step = ref(0)
const submitted = ref(false)
// 步骤条回跳:仅"已完成"步骤(idx < 当前步)可点,提交完成后不可再跳;前进仍须走「下一步」校验
function stepJumpable(idx) { return !submitted.value && idx < step.value }
function onStepClick(idx) { if (stepJumpable(idx)) step.value = idx }
const creating = ref(false); const submitting = ref(false)
const formNo = ref('')
const items = ref([])
// 已加入数据表分页 + 筛选(纯前端,10行/页;数据全在内存 items,无需改接口)
// 注意:el-table 的 :filter-method 只对送入 :data 的数组生效——若直接把已分页的 pagedItems 喂给 :data,
// 筛选就只能筛当前页(且分页 total 仍是全量),跨页数据会被"看似筛没了"。故筛选状态(itemFilters)与全量 items
// 一起算出 filteredItems,分页只在 filteredItems 之上切片,total 也绑 filteredItems.length。
const itemsPage = ref(1); const itemsPageSize = ref(10)
const itemFilters = reactive({})
function matchSystemFilter(v, row) { return row.systemName === v }
function matchRightTypeFilter(v, row) { return row.rightType === v }
function matchCrossFilter(v, row) { return !!(externalGrantee.value || crossSystemInfo.value.isCross || row.crossGeo) === v }
function matchThirdPartyFilter(v, row) { return !!(row.thirdPartySource && String(row.thirdPartySource).trim()) === v }
function matchSensitiveFilter(v, row) { return !!(row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无') === v }
const ITEM_FILTER_PREDICATES = { sys: matchSystemFilter, rightType: matchRightTypeFilter, cross: matchCrossFilter, thirdParty: matchThirdPartyFilter, sensitive: matchSensitiveFilter }
function onItemsFilterChange(filters) { Object.assign(itemFilters, filters); itemsPage.value = 1 }
const filteredItems = computed(() => {
  let list = items.value
  for (const key of Object.keys(itemFilters)) {
    const vals = itemFilters[key]
    if (!vals || !vals.length) continue
    const pred = ITEM_FILTER_PREDICATES[key]
    if (!pred) continue
    list = list.filter(row => vals.some(v => pred(v, row)))
  }
  return list
})
const pagedItems = computed(() => {
  const start = (itemsPage.value - 1) * itemsPageSize.value
  return filteredItems.value.slice(start, start + itemsPageSize.value)
})
watch(items, () => { if (itemsPage.value > 1 && (itemsPage.value - 1) * itemsPageSize.value >= filteredItems.value.length) itemsPage.value = 1 })
// 所属系统筛选选项(去重,随已加入数据表动态变化;基于全量 items,不受当前筛选/分页影响)
const sysFilterOpts = computed(() => [...new Set(items.value.map(i => i.systemName).filter(Boolean))].map(name => ({ text: name, value: name })))
// 申请单头(共享"一事":同一被授权方 + 同一场景 + 权益/时效/协议要素/联系)
const listForm = reactive({ rightType: '', granteeOrg: '', scenario: '', purposeNote: '', validTerm: '两年', benefitAllocation: '', securityReq: '', applicantManager: '', contactInfo: '', needConfidentiality: false, confidentialityFile: '' })
const externalGrantee = ref(false)
const savingDraft = ref(false)
// 路由预填暂存:权益卡片「发起授权」/ reopen 重提 带入的一张数据表,填完单头(createForm)后自动加入明细
const pendingAsset = ref(null)

function emptyItem() {
  return { assetId: '', assetName: '', tableCode: '', systemName: '', schemaName: '', equityCardId: '', granteeOrg: '', rightType: '', scenario: '', purposeNote: '', validTerm: '两年', validDate: '', businessDomain: '', thirdPartySource: '', thirdPartyLicense: '', sensitiveType: '', infoAuthAgreement: '', crossRegion: false, ownerOrg: '', crossGeo: false, applicantManager: '', contactInfo: '', benefitAllocation: '', securityReq: '', needConfidentiality: false, confidentialityFile: '' }
}

// 跨地域判定(表5「是否跨区域、跨域」的"跨区域"维):被授权方省码 vs 数据归属主体(确权 表1 公司主体)省码。
// 复用后端组织归口解析 /dpr/org/resolve;省码解析不到(外部主体/网级总部)不判跨,仅标注供合规审查关注。
const provCache = new Map()
async function provinceOf(name) {
  const key = (name || '').trim()
  if (!key) return ''
  if (provCache.has(key)) return provCache.get(key)
  let p = ''
  try { const j = await resolveOrg(key); p = (j && j.provinceCode) || '' } catch (e) { /* 解析失败按未知处理 */ }
  provCache.set(key, p)
  return p
}
async function computeCrossGeo(ownerOrg) {
  if (externalGrantee.value) return false // 外部主体:省码不可解析,以「涉外部主体」标注而非误判跨地域
  const [gp, op] = await Promise.all([provinceOf(listForm.granteeOrg), provinceOf(ownerOrg)])
  return !!(gp && op && gp !== op)
}

// 跨系统域:多表可跨多系统 → 按已加入项的系统去重自动判定(只读 + 写回 crossRegion)
// 跨地域(anyGeo):任一行 被授权方省 ≠ 数据归属主体省(逐行 crossGeo,加入时判定)
const crossSystemInfo = computed(() => {
  const systems = [...new Set(items.value.map(x => x.systemName).filter(Boolean))]
  const domains = [...new Set(items.value.map(x => x.businessDomain).filter(Boolean))]
  const anyGeo = items.value.some(x => x.crossGeo)
  return { systems, domains, anyGeo, isCross: systems.length > 1 || domains.length > 1 }
})
// 去重键:被授权方/场景单头恒定,行唯一性 = 库表 + 权益(同表同权益视为重复)
function dupKey(x) { return `${x.assetId || ''}|${x.tableCode || x.assetName || ''}|${x.rightType || ''}` }

// 应用场景(按权益类型过滤,与应用场景管理「适用权益类型」联动)
const scenarioOpts = ref([])
const filteredScenarios = computed(() => {
  if (!listForm.rightType) return scenarioOpts.value
  return scenarioOpts.value.filter(s => !s.rightType || s.rightType === '通用' || s.rightType === listForm.rightType)
})
// 选场景默认带出该场景的目的模板到「目的摘要」(可编辑);仅在摘要为空时填,避免覆盖用户已写
function onScenarioChange(name) {
  const s = scenarioOpts.value.find(x => x.scenarioName === name)
  const tpl = s && s.reasonTemplate ? s.reasonTemplate : ''
  if (tpl && !listForm.purposeNote) listForm.purposeNote = tpl
}

// 应交材料:《表5》系统按数据表多行生成(不上传);第三方凭证/信息授权协议逐表呈现于明细
const materialRules = ref([])
const FALLBACK_RULES = [
  { triggerType: 'ALWAYS', materialName: '《表5 数据授权申请单》', required: '必填', detail: '系统按已加入数据表多行自动生成(申请主体/系统/模式/数据表/权益/场景/时效/跨域等),点「生成」下载留痕或线下签署' }
]
// 保密承诺函(附录E)上传:申报单级,挂 formNo;needConfidentiality 开启时作应交项(留痕,不阻断提交)
const confUploading = ref(false)
const confUploaded = ref(false)
const CONF_MAT = '《保密承诺函(附录E)》'
const requiredChecklist = computed(() => {
  const src = materialRules.value.length ? materialRules.value : FALLBACK_RULES
  const list = src.filter(r => r.triggerType === 'ALWAYS').map(r => ({ ...r }))
  if (listForm.needConfidentiality) {
    list.push({ triggerType: 'CONFIDENTIALITY', materialName: CONF_MAT, required: '必填', detail: '被授权方签署的《保密承诺函》(附录E);涉敏感/第三方数据授权须提供', uploaded: confUploaded.value })
  }
  return list
})
function isTable5(row) { return (row.materialName || '').includes('表5') }
async function uploadConfFile(file) {
  if (!formNo.value) { ElMessage.warning('请先「下一步」创建申请单'); return }
  confUploading.value = true
  try {
    const fd = new FormData(); fd.append('file', file); fd.append('applyId', formNo.value); fd.append('materialName', CONF_MAT)
    await uploadAuthMaterialFile(fd)
    confUploaded.value = true
    ElMessage.success('《保密承诺函》已上传')
  } catch (e) { ElMessage.warning('上传失败:' + (e?.response?.data?.message || e?.message || '')) }
  finally { confUploading.value = false }
}
function genTable5() {
  if (!items.value.length) { ElMessage.warning('请先加入数据表'); return }
  const head = { granteeOrg: listForm.granteeOrg, scenario: listForm.scenario, purposeNote: listForm.purposeNote, validTerm: listForm.validTerm, contactPerson: listForm.applicantManager, contactInfo: listForm.contactInfo }
  downloadTable5(head, items.value, crossSystemInfo.value.isCross, {
    title: `表5 数据授权申请单（一事一议·被授权方：${listForm.granteeOrg || ''}）`,
    fileName: `表5_数据授权申请单_一事一议_${(listForm.granteeOrg || '').slice(0, 12)}.xls`
  })
  ElMessage.success('《表5》已按数据表多行生成并下载,可留痕或线下签署')
}

const orgOptions = ref([])
onMounted(async () => {
  try { const rules = await listAuthMaterialRules('一事一议'); if (Array.isArray(rules) && rules.length) materialRules.value = rules } catch (e) { /* 兜底内置 */ }
  try { orgOptions.value = (await listOrg()) || [] } catch (e) { /* 选择器降级为可输入 */ }
  try { const r = await pageScenario({ status: '生效中', size: 100 }); scenarioOpts.value = r.records || [] } catch (e) { scenarioOpts.value = [] }
  try { await applyRoutePrefill() } finally { autosaveReady.value = true } // 回填/找回完成后再开启自动保存
})

// 路由预填:① 权益卡片「发起授权」(query 带 assetId/cardNo/rightType) ② reopen 重提(sessionStorage 原单)
// 统一暂存为 pendingAsset,落 step0;用户填完单头「下一步」(createForm)后由 addPendingAsset 自动加入明细。
async function applyRoutePrefill() {
  const q = route.query
  // 草稿就地续填(从草稿箱/我的申请「继续填写」带 formNo 进入):按 formNo 回填,保留 applyId 续存同一申请单
  if (q.formNo) { await loadFormDraft(String(q.formNo)); return }
  if (!q.assetId && !q.reopen) { await maybeRecoverLocalDraft(); return } // 全新进入:找回本地未提交草稿
  if (q.assetId) {
    if (rightTypes.includes(q.rightType)) listForm.rightType = String(q.rightType)
    pendingAsset.value = { assetId: String(q.assetId), assetName: String(q.assetName || ''), equityCardId: String(q.cardNo || ''), rightType: rightTypes.includes(q.rightType) ? String(q.rightType) : '' }
    onScenarioChange(listForm.scenario)
    ElMessage.info('已从权益卡片带入数据表,请填写被授权方/场景后「下一步」,该表将自动加入申请单')
  }
  if (q.reopen) {
    try {
      const o = JSON.parse(sessionStorage.getItem('prm-reopen') || '{}')
      if (o.domain === '授权' && o.raw) {
        const r = o.raw
        Object.assign(listForm, {
          rightType: rightTypes.includes(r.rightType) ? r.rightType : listForm.rightType,
          granteeOrg: r.granteeOrg || '', scenario: r.scenario || '', purposeNote: r.purposeNote || '',
          benefitAllocation: r.benefitAllocation || '', securityReq: r.securityReq || '',
          applicantManager: r.applicantManager || '', contactInfo: r.contactInfo || '',
          needConfidentiality: !!r.needConfidentiality, confidentialityFile: r.confidentialityFile || ''
        })
        pendingAsset.value = { assetId: r.assetId || '', assetName: r.assetName || '', equityCardId: r.equityCardId || '', rightType: r.rightType || '',
          systemName: r.systemName || '', schemaName: r.schemaName || '', businessDomain: r.businessDomain || '',
          thirdPartySource: r.thirdPartySource || '', thirdPartyLicense: r.thirdPartyLicense || '', sensitiveType: r.sensitiveType || '', infoAuthAgreement: r.infoAuthAgreement || '' }
        // 目的摘要必填:原单未留存摘要(历史数据)时按场景模板兜底带出,避免重提被必填卡住
        if (!listForm.purposeNote) onScenarioChange(listForm.scenario)
        ElMessage.warning('已带入被驳回原单内容,请修改后重新提交(将作为新申请单)')
      }
    } catch (e) { /* ignore */ }
    sessionStorage.removeItem('prm-reopen')
  }
}

// 填完单头(已建 formNo)后,把暂存的数据表自动加入明细(复用确权带出 + saveAuthDraft 路径)
async function addPendingAsset() {
  const p = pendingAsset.value
  pendingAsset.value = null
  if (!p || !p.assetId) return
  const f = await deriveFacts(p.assetId)
  const crossGeo = await computeCrossGeo(f.ownerOrg)
  const it = { ...emptyItem(), assetId: p.assetId, assetName: p.assetName, equityCardId: p.equityCardId,
    systemName: p.systemName || '', schemaName: p.schemaName || '',
    granteeOrg: listForm.granteeOrg, rightType: p.rightType || listForm.rightType,
    scenario: listForm.scenario, validTerm: listForm.validTerm, validDate: expiryOf(listForm.validTerm),
    businessDomain: p.businessDomain || f.businessDomain || '',
    thirdPartySource: p.thirdPartySource || f.thirdPartySource, thirdPartyLicense: p.thirdPartyLicense || f.thirdPartyLicense,
    sensitiveType: p.sensitiveType || f.sensitiveType, infoAuthAgreement: p.infoAuthAgreement || f.infoAuthAgreement,
    ownerOrg: f.ownerOrg, crossGeo,
    crossRegion: crossGeo, applicantManager: listForm.applicantManager, contactInfo: listForm.contactInfo,
    benefitAllocation: listForm.benefitAllocation, securityReq: listForm.securityReq,
    needConfidentiality: listForm.needConfidentiality, confidentialityFile: listForm.confidentialityFile }
  try {
    const id = await saveAuthDraft({ authMode: '一事一议', formNo: formNo.value, ...it })
    items.value.push({ ...it, applyId: id })
    ElMessage.success(`已自动加入数据表「${p.assetName}」,可继续从确权目录加入更多`)
  } catch (e) { ElMessage.warning('自动加入数据表失败,请在下方手动从确权目录选取:' + (e?.response?.data?.message || e?.message || '')) }
}

// 提交前自检闭环:明细变化即让上次自检失效(必须重新自检才能提交)
const complianceResult = ref(null)
const complianceChecking = ref(false)
watch(items, () => { complianceResult.value = null }, { deep: true })
const canSubmit = computed(() => !!complianceResult.value && complianceResult.value.allPass)
const checkStatus = computed(() => {
  if (!complianceResult.value) return { type: 'info', text: '未自检' }
  if (complianceResult.value.allPass) return { type: 'success', text: '全部合规,可提交' }
  return { type: 'danger', text: `未通过(${complianceResult.value.blockedCount} 张被拦)` }
})
async function runComplianceCheck() {
  if (!formNo.value) { ElMessage.warning('请先建单并加入数据表'); return }
  if (!items.value.length) { ElMessage.warning('申请单为空,请先加入数据表'); return }
  complianceChecking.value = true
  try {
    complianceResult.value = await checkAuthFormCompliance(formNo.value)
    if (complianceResult.value.allPass) ElMessage.success('提交前自检通过,可提交申请单')
    else ElMessage.warning(`提交前自检未通过:${complianceResult.value.blockedCount} 张被拦,请逐表修正后重新自检`)
  } finally { complianceChecking.value = false }
}
const highlightApplyId = ref('')
function itemRowClass({ row }) { return row.applyId && row.applyId === highlightApplyId.value ? 'fix-highlight' : '' }
function goFixItem(row) {
  highlightApplyId.value = row.applyId
  step.value = 1
  ElMessage.info(`已切到明细,请修正「${row.assetName}」(已高亮定位)`)
  setTimeout(() => { if (highlightApplyId.value === row.applyId) highlightApplyId.value = '' }, 5000)
}

async function next0() {
  if (!listForm.rightType) { ElMessage.warning('请选择授权权益类型'); return }
  if (!listForm.granteeOrg) { ElMessage.warning('请填写被授权方(本次授给谁)'); return }
  if (!listForm.scenario) { ElMessage.warning('请填写使用场景及目的(本次事项)'); return }
  // 表5「使用场景及目的摘要」为合规评审判断"特定场景、仅限本次"的文本依据,必填(选场景已默认带出模板)
  if (!listForm.purposeNote) { ElMessage.warning('请填写目的摘要(表5「使用场景及目的摘要」,合规评审依据)'); return }
  if (!listForm.applicantManager) { ElMessage.warning('请填写申请单位主管(表5 必填)'); return }
  if (!listForm.contactInfo) { ElMessage.warning('请填写联系方式(表5 必填)'); return }
  if (!formNo.value) {
    creating.value = true
    try { formNo.value = await createAuthForm() } finally { creating.value = false }
  }
  if (pendingAsset.value) await addPendingAsset() // 路由预填:单头就绪后自动加入暂存数据表
  step.value = 1
}

// 确权事实带出:第三方来源/许可凭证/隐私/信息授权协议/业务域/归属主体(只读,堵人工低报)
async function deriveFacts(assetId) {
  try {
    const f = await getRightsFacts(assetId)
    return { thirdPartySource: f?.thirdPartySource || '', thirdPartyLicense: f?.thirdPartyLicense || '', sensitiveType: f?.sensitiveType || '无', infoAuthAgreement: f?.infoAuthAgreement || '', businessDomain: f?.businessDomain || '', ownerOrg: f?.ownerOrg || '' }
  } catch { return { thirdPartySource: '', thirdPartyLicense: '', sensitiveType: '无', infoAuthAgreement: '', businessDomain: '', ownerOrg: '' } }
}

// 唯一录入入口:从确权目录资源池多选
const pickerDlg = ref(false); const picking = ref(false); const pickedLeaves = ref([])
function openPicker() {
  if (!listForm.rightType) { ElMessage.warning('请先在单头选择"权益类型"(资源池据此过滤)'); return }
  if (!listForm.granteeOrg) { ElMessage.warning('请先在单头填写"被授权方"'); return }
  pickedLeaves.value = []
  pickerDlg.value = true
}
function onPickChange(leaves) { pickedLeaves.value = leaves || [] }
async function confirmPick() {
  if (!pickedLeaves.value.length) { ElMessage.warning('请至少勾选一张数据表'); return }
  picking.value = true
  try {
    let ok = 0; const dups = []
    const seen = new Set(items.value.map(dupKey))
    const sysUnion = new Set([...items.value.map(x => x.systemName).filter(Boolean), ...pickedLeaves.value.map(l => l.systemName).filter(Boolean)])
    const isCross = sysUnion.size > 1
    for (const lf of pickedLeaves.value) {
      const f = await deriveFacts(lf.assetId)
      const crossGeo = await computeCrossGeo(f.ownerOrg)
      const it = { ...emptyItem(), assetId: lf.assetId, assetName: lf.assetName, tableCode: lf.tableCode,
        systemName: lf.systemName, schemaName: lf.schemaName, equityCardId: lf.equityCardId,
        granteeOrg: listForm.granteeOrg, rightType: lf.rightType || listForm.rightType,
        scenario: listForm.scenario, purposeNote: listForm.purposeNote, validTerm: listForm.validTerm, validDate: expiryOf(listForm.validTerm),
        businessDomain: lf.businessDomain || f.businessDomain || '',
        thirdPartySource: f.thirdPartySource, thirdPartyLicense: f.thirdPartyLicense, sensitiveType: f.sensitiveType, infoAuthAgreement: f.infoAuthAgreement,
        ownerOrg: f.ownerOrg, crossGeo,
        crossRegion: isCross || crossGeo, applicantManager: listForm.applicantManager, contactInfo: listForm.contactInfo,
        benefitAllocation: listForm.benefitAllocation, securityReq: listForm.securityReq,
        needConfidentiality: listForm.needConfidentiality, confidentialityFile: listForm.confidentialityFile }
      const k = dupKey(it)
      if (seen.has(k)) { dups.push(lf.assetName); continue }
      seen.add(k)
      const id = await saveAuthDraft({ authMode: '一事一议', formNo: formNo.value, ...it })
      items.value.push({ ...it, applyId: id })
      ok++
    }
    ElMessage[dups.length ? 'warning' : 'success'](`已加入 ${ok} 张数据表(资源池均已确权+权属可授,生效卡自动带入)` + (dups.length ? `;跳过重复 ${dups.length} 张:${dups.slice(0, 3).join('、')}` : ''))
    pickerDlg.value = false; pickedLeaves.value = []
  } finally { picking.value = false }
}

// 逐行可选微调(场景/时效)→ 即存草稿
async function persistItem(row) {
  if (!row.applyId) return
  try { await saveAuthDraft({ authMode: '一事一议', formNo: formNo.value, ...row }) }
  catch (e) { ElMessage.warning('保存修改失败:' + (e?.message || '')) }
}

// 用当前单头刷新每条明细的共享字段(被授权方/场景/时效/协议要素/联系/保密)并落库
async function persistAllItems() {
  for (const it of items.value) {
    if (!it.applyId) continue
    Object.assign(it, {
      granteeOrg: listForm.granteeOrg, scenario: listForm.scenario, purposeNote: listForm.purposeNote,
      validTerm: listForm.validTerm, validDate: expiryOf(listForm.validTerm),
      benefitAllocation: listForm.benefitAllocation, securityReq: listForm.securityReq,
      applicantManager: listForm.applicantManager, contactInfo: listForm.contactInfo,
      needConfidentiality: listForm.needConfidentiality, confidentialityFile: listForm.confidentialityFile
    })
    await saveAuthDraft({ authMode: '一事一议', formNo: formNo.value, ...it })
  }
}

// 主动保存草稿(PDD 8.1 状态机·独立存草稿):宽进——落库底线=至少已选一张授权数据表(申请单否则为空,无意义)。
// 不校验单头完整度、不推进步骤;建单(如未建)→加入暂存表→用当前单头刷新全部明细。可离开后在「我的申请」草稿续填。
async function saveDraftForm(silent = false) {
  if (!formNo.value && !pendingAsset.value && items.value.length === 0) {
    if (!silent) ElMessage.warning('请先从确权目录选取至少一张授权数据表,方可暂存草稿')
    return
  }
  savingDraft.value = true
  try {
    const first = !formNo.value
    if (!formNo.value) formNo.value = await createAuthForm()
    if (pendingAsset.value) await addPendingAsset()
    await persistAllItems()
    markDraftSource(formNo.value, silent ? 'auto' : 'manual')
    if (first) { removeLocalDraft(localDraftKey('auth-special', 'new', meId())); notifyDraftChanged() }
    if (!silent) {
      notifyDraftChanged()
      ElMessage.success(`草稿已保存(申请单 ${formNo.value})，可随时离开后在「申请草稿箱」或「我的申请」继续`)
    }
  } catch (e) { /* 拦截器已 toast */ } finally { savingDraft.value = false }
}

// ===== 申请草稿·自动保存(一事一议:本地即时防丢 + 达底线后静默 server-sync via saveDraftForm)=====
const autosaveReady = ref(false)
const meId = () => (currentUser() && currentUser().userId) || ''
const draftKey = () => localDraftKey('auth-special', formNo.value || 'new', meId())
// 仅在申请单已建(formNo 存在)后才服务端 UPDATE;绝不由自动保存 CREATE 申请单(避免与建单竞态、防幽灵草稿)。建单前本地快照兜底防丢。
const canServerAutosave = () => !!formNo.value && !savingDraft.value && !submitting.value
const autosave = useDraftAutosave({
  getKey: draftKey,
  getSnapshot: () => {
    if (!listForm.granteeOrg && !pendingAsset.value && !items.value.length) return { __skip: true }
    return {
      listForm: JSON.parse(JSON.stringify(listForm)),
      pendingAsset: pendingAsset.value ? JSON.parse(JSON.stringify(pendingAsset.value)) : null,
      items: JSON.parse(JSON.stringify(items.value)),
      formNo: formNo.value, step: step.value,
      title: [listForm.granteeOrg, listForm.scenario].filter(Boolean).join(' · ') || '(未填被授权方)'
    }
  },
  serverSync: () => saveDraftForm(true),
  canServer: canServerAutosave
})
const autoTip = computed(() => (autosave.syncing.value ? '正在自动保存…' : (autosave.lastSavedAt.value ? `已自动保存 ${autosave.lastSavedAt.value}` : '')))
watch([listForm, items, pendingAsset], () => { if (autosaveReady.value) autosave.schedule() }, { deep: true })
watch(step, () => { if (autosaveReady.value) autosave.schedule({ server: false }) })

// 找回:全新进入(无 formNo/assetId/reopen)时提示恢复本地未提交草稿
async function maybeRecoverLocalDraft() {
  const snap = readLocalDraft(localDraftKey('auth-special', 'new', meId()))
  if (!snap || (!snap.listForm?.granteeOrg && !snap.pendingAsset && !(snap.items || []).length)) return
  const when = snap.__ts ? new Date(snap.__ts).toLocaleString() : ''
  try {
    await ElMessageBox.confirm(`检测到一份未提交的一事一议授权草稿${when ? `(最后编辑 ${when})` : ''},是否恢复继续填写?`,
      '恢复未完成的草稿', { confirmButtonText: '恢复', cancelButtonText: '丢弃', type: 'info' })
    Object.assign(listForm, snap.listForm || {})
    pendingAsset.value = snap.pendingAsset || null
    if ((snap.items || []).length) items.value = snap.items
    ElMessage.success('已恢复本地草稿,可继续填写')
  } catch { removeLocalDraft(localDraftKey('auth-special', 'new', meId())) }
}

// 草稿就地续填:按 formNo 回填单头 + 明细(保留各行 applyId,续存/提交同一申请单,不复制)
async function loadFormDraft(fn) {
  try {
    const rows = await listAuthByForm(fn)
    if (!Array.isArray(rows) || !rows.length) { ElMessage.warning('草稿申请单不存在或已提交'); return }
    if (rows.some(r => r.status && r.status !== '草稿')) {
      ElMessage.warning('该申请单已提交,非草稿不可就地编辑;如需修改请走撤回/修改重提'); return
    }
    formNo.value = fn
    const h = rows[0]
    Object.assign(listForm, {
      rightType: rightTypes.includes(h.rightType) ? h.rightType : listForm.rightType,
      granteeOrg: h.granteeOrg || '', scenario: h.scenario || '', purposeNote: h.purposeNote || '',
      validTerm: h.validTerm || '两年', benefitAllocation: h.benefitAllocation || '', securityReq: h.securityReq || '',
      applicantManager: h.applicantManager || '', contactInfo: h.contactInfo || '',
      needConfidentiality: !!h.needConfidentiality, confidentialityFile: h.confidentialityFile || ''
    })
    items.value = rows.map(r => ({ ...emptyItem(), ...r }))
    step.value = 1
    ElMessage.success('已载入草稿申请单,可继续加入数据表并「保存草稿」或提交')
  } catch (e) { ElMessage.warning('载入草稿失败,请重试') }
}
function onTermChange(row) { row.validDate = expiryOf(row.validTerm); persistItem(row) }
// 确权侧缺第三方凭证 → 该表行级补传(回填字段使自检通过)
async function uploadRowCredential(row, file) {
  try {
    const fd = new FormData(); fd.append('file', file); fd.append('applyId', row.applyId); fd.append('materialName', `第三方许可凭证·${row.assetName}`)
    await uploadAuthMaterialFile(fd)
    row.thirdPartyLicense = '见附件:' + file.name
    await persistItem(row)
    complianceResult.value = null
    ElMessage.success(`「${row.assetName}」第三方凭证已补传`)
  } catch (e) { ElMessage.warning('补传失败:' + (e?.message || '')) }
}
async function uploadRowPrivacy(row, file) {
  try {
    const fd = new FormData(); fd.append('file', file); fd.append('applyId', row.applyId); fd.append('materialName', `信息授权协议·${row.assetName}`)
    await uploadAuthMaterialFile(fd)
    row.infoAuthAgreement = '见附件:' + file.name
    await persistItem(row)
    complianceResult.value = null
    ElMessage.success(`「${row.assetName}」信息授权协议已补传`)
  } catch (e) { ElMessage.warning('补传失败:' + (e?.message || '')) }
}

function removeItem(row, idx) {
  confirmAsync(`确认从申请单移除「${row.assetName}」?`, '删除数据表', async () => {
    try {
      if (row.applyId) await deleteAuthApply(row.applyId)
      items.value.splice(idx, 1)
      ElMessage.success('已移除')
    } catch (e) { ElMessage.warning('删除失败:' + (e?.message || '')) }
  }, { confirmButtonText: '删除', cancelButtonText: '取消' }).catch(() => {})
}

async function doSubmit() {
  submitting.value = true
  try {
    await submitAuthForm(formNo.value); submitted.value = true
    autosave.clear(); clearDraftSource(formNo.value); notifyDraftChanged() // 已提交:清本地缓存/来源,草稿数减一
  } finally { submitting.value = false }
}

// 自动配卡:按资产匹配生效权益卡片(权属可授:卡片权益==所选)。供一键示例。
const CARD_OK = ['正常', '生效']
const cardOpts = ref([]); let cardsLoaded = false
async function loadCards() {
  if (cardsLoaded) return
  const r = await pageEquityCard({ current: 1, size: 100 })
  cardOpts.value = r.records || []
  cardsLoaded = true
}
function findUsableCard(assetId, right) {
  const c = cardOpts.value.find(x => x.assetId === assetId && CARD_OK.includes(x.cardStatus) && (!right || x.rightType === right))
  return c ? (c.cardNo || c.cardId) : ''
}
const demoFilling = ref(false)
async function fillDemo() {
  demoFilling.value = true
  try {
    listForm.rightType = '使用权'
    listForm.granteeOrg = '南网综合能源股份有限公司'
    listForm.scenario = '综合能源服务'
    listForm.purposeNote = '用于综合能源板块内部经营分析与服务,数据不出域、不对外提供'
    listForm.applicantManager = '张三'
    listForm.contactInfo = '020-31000000'
    listForm.benefitAllocation = '按调用次数计费,收益按 7:3 分成'
    listForm.securityReq = '加密传输 + 最小授权访问控制 + 操作留痕审计'
    await next0()
    await loadCards()
    const demo = [['AST-002', '台区负荷数据', '计量系统']]
    let ok = 0
    const seen = new Set(items.value.map(dupKey))
    for (const [aid, name, sys] of demo) {
      const card = findUsableCard(aid, listForm.rightType)
      if (!card) continue
      const f = await deriveFacts(aid)
      const it = { ...emptyItem(), assetId: aid, assetName: name, systemName: sys, equityCardId: card,
        granteeOrg: listForm.granteeOrg, rightType: listForm.rightType, scenario: listForm.scenario, purposeNote: listForm.purposeNote, validTerm: listForm.validTerm, validDate: expiryOf(listForm.validTerm),
        businessDomain: f.businessDomain, thirdPartySource: f.thirdPartySource, thirdPartyLicense: f.thirdPartyLicense, sensitiveType: f.sensitiveType, infoAuthAgreement: f.infoAuthAgreement,
        applicantManager: listForm.applicantManager, contactInfo: listForm.contactInfo, benefitAllocation: listForm.benefitAllocation, securityReq: listForm.securityReq }
      const k = dupKey(it)
      if (seen.has(k)) continue
      seen.add(k)
      const id = await saveAuthDraft({ authMode: '一事一议', formNo: formNo.value, ...it })
      items.value.push({ ...it, applyId: id })
      ok++
    }
    ElMessage.success(`示例完成:申请单已建,自动加入 ${ok} 张可授数据表,可直接"提交前自检"`)
  } finally { demoFilling.value = false }
}

function go(p) { router.push(p) }
function reset() {
  step.value = 0; submitted.value = false; externalGrantee.value = false; formNo.value = ''; items.value = []; pickedLeaves.value = []; complianceResult.value = null; pendingAsset.value = null; confUploaded.value = false
  Object.assign(listForm, { rightType: '', granteeOrg: '', scenario: '', purposeNote: '', validTerm: '两年', benefitAllocation: '', securityReq: '', applicantManager: '', contactInfo: '', needConfidentiality: false, confidentialityFile: '' })
}
</script>

<style scoped>
.wz-steps { max-width: 900px; margin: 8px auto 20px; }
.wz-steps :deep(.wz-step-clickable) { cursor: pointer; }
.wz-steps :deep(.wz-step-clickable:hover .el-step__title) { opacity: .72; }
.wz-body { min-height: 340px; }
.wz-progress { background: #f7f9ff; border-radius: 8px; padding: 10px 20px 0; margin: 0 auto 10px; max-width: 560px; text-align: left; }
.wz-progress-t { font-weight: 600; font-size: 13px; color: var(--prm-color-text); margin-bottom: 10px; }
.auth-tip { font-size: 12px; color: var(--prm-color-text-weak); line-height: 1.6; margin-top: 2px; }
.batch-primary { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; padding: 10px 20px; margin-bottom: 10px; background: linear-gradient(180deg, #eef4ff, #f7faff); border: 1px solid #d6e4ff; border-radius: 8px; }
.batch-primary-hint { color: var(--prm-color-text-secondary); font-size: 13px; line-height: 1.5; flex: 1; min-width: 240px; }
.batch-detail-head { margin: 18px 0 8px; padding-left: 10px; border-left: 4px solid var(--prm-color-primary); font-weight: 600; color: var(--prm-color-text); line-height: 1.5; }
.batch-detail-sub { margin-left: 8px; font-weight: 400; font-size: 12px; color: var(--prm-color-text-weak); }
:deep(.el-table .fix-highlight) { background: #fff7e6 !important; animation: fixflash 1s ease-in-out 2; }
@keyframes fixflash { 0%,100% { background: #fff7e6; } 50% { background: #ffe7ba; } }
</style>
