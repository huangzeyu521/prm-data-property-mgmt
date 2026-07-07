<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <PageNote>注:一站式批量授权——填清单基础信息 → 从确权资源池选授权数据 → 确认提交申报稿,一次办完申报。提交后进入审批链(合规审查(合规管控小组)→主管→经理→副总→领导小组决策),经甲乙双签《数据运营授权协议(附录D)》后执行授权·归档(对外经营权另备案附录G)。授权凭证为协议,非"证书"。</PageNote>

    <!-- 步骤条可点击回跳(el-step 无自身 click emit,@click 原生透传);仅已完成步骤可点,提交后终态不可再跳 -->
    <el-steps :active="submitted ? 3 : step" finish-status="success" align-center class="wz-steps">
      <el-step title="清单基础信息" description="被授权方 · 授权年度(表6 清单头)" :class="{ 'wz-step-clickable': stepJumpable(0) }" @click="onStepClick(0)" />
      <el-step title="选择授权数据" description="从确权资源池逐条加入(表6 明细)" :class="{ 'wz-step-clickable': stepJumpable(1) }" @click="onStepClick(1)" />
      <el-step title="确认并提交" description="提交前自检 → 提交申报稿" />
    </el-steps>

    <div class="wz-body">
      <!-- 步骤1:建清单 -->
      <el-card v-show="step === 0" shadow="never">
        <el-form :model="listForm" label-width="140px" style="max-width:580px">
          <el-alert v-if="!batchListId" type="info" :closable="false" style="margin-bottom:10px;max-width:680px">
            <template #title>
              第一次用?可
              <el-button link type="primary" style="vertical-align:baseline" :loading="demoFilling" @click="fillDemo">一键示例:建清单并自动加入可授明细(测试/演示)</el-button>
              资源池按权益类型过滤,生效卡片自动匹配,材料见 test/批量授权申请
            </template>
          </el-alert>
          <el-form-item label="授权年度" required><el-input v-model="listForm.listYear" placeholder="如 2026" /></el-form-item>
          <el-form-item label="申请主体(被授权方)" required>
            <el-select v-if="!externalGrantee" v-model="listForm.granteeOrg" filterable allow-create default-first-option clearable
              placeholder="选择被授权方(南网组织;搜不到可直接输入)— 本批数据统一授给谁" style="width:100%">
              <el-option v-for="o in orgOptions" :key="o.id" :label="o.bizOrgName" :value="o.bizOrgName" />
            </el-select>
            <el-input v-else v-model="listForm.granteeOrg" placeholder="外部被授权主体名称(政府/外部企业/社会组织)" clearable />
            <div style="margin-top:4px">
              <el-checkbox v-model="externalGrantee" @change="listForm.granteeOrg = ''">被授权方为外部主体(不在南网组织结构内;对外经营权另备案附录G)</el-checkbox>
            </div>
          </el-form-item>
          <el-form-item label="联系人/单位主管" required>
            <el-input v-model="listForm.contactPerson" placeholder="表6 联络人/单位主管(批量共享)" clearable />
          </el-form-item>
          <el-form-item label="联系方式" required>
            <el-input v-model="listForm.contactInfo" placeholder="表6 联系方式(电话/邮箱)" clearable />
          </el-form-item>
          <el-divider content-position="left" style="margin:8px 0">
            <span style="font-size:12px;color:var(--prm-color-text-weak)">批量默认(逐条加项时自动带入;在第2步明细里「使用场景/授权时效」可逐项微调,权益按选取确定)</span>
          </el-divider>
          <el-form-item label="默认权益类型(单选)">
            <el-select v-model="listForm.rightType" clearable style="width:100%" placeholder="整批默认权益类型(单选;不同表可逐项调整)">
              <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
            </el-select>
            <div style="font-size:12px;color:var(--prm-color-text-weak);line-height:1.5;margin-top:2px">
              授权仅授「使用权 / 经营权」两类;持有权经确权认定取得,不在授权范围(三权分置)。<br/>
              可授数据集合由确权资源池决定(先确后授);此处选整批默认,逐项可调。
            </div>
          </el-form-item>
          <el-form-item label="默认使用场景"><el-input v-model="listForm.scenario" placeholder="整批默认使用场景及目的" clearable /></el-form-item>
          <!-- 业务域=数据/资产属性(表5「所属业务域」),由 step2 选中的数据表从确权目录逐表带出,不在此手填(避免与真实数据域冲突) -->
          <el-form-item label="默认授权时效">
            <el-select v-model="listForm.validTerm" style="width:100%" placeholder="默认两年(时长)">
              <el-option v-for="t in validTerms" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="数据使用地理范围">
            <el-select v-model="listForm.geoScope" filterable allow-create default-first-option style="width:100%" placeholder="表5/表6「跨地域」判定来源">
              <el-option v-for="g in geoScopes" :key="g" :label="g" :value="g" />
            </el-select>
            <div style="font-size:12px;color:var(--prm-color-text-weak);line-height:1.5;margin-top:2px">
              超出授权方属地(广东省行政区域内)即判「跨地域」;同时作为《运营授权协议》附录D 表1 地理范围缺省值。
            </div>
          </el-form-item>
          <el-form-item label="清单备注"><el-input v-model="listForm.remark" type="textarea" maxlength="500" show-word-limit :rows="2" placeholder="如:综能板块年度批量授权" /></el-form-item>
        </el-form>
        <el-alert v-if="batchListId" type="success" :closable="false" show-icon :title="`清单已创建(${listNo})，开始逐条添加授权项`" style="max-width:520px;margin-top:8px" />
      </el-card>

      <!-- 步骤2:逐条加授权项 -->
      <el-card v-show="step === 1" shadow="never">
        <!-- 唯一录入入口:从确权目录批量选取(资源池已按 先确后授 + 权属可授 + 经营权对外开放 过滤) -->
        <div class="batch-primary">
          <el-button type="primary" size="large" @click="openPicker">① 从确权目录批量选取数据资产</el-button>
          <span class="batch-primary-hint">
            目录按「选系统 → 选模块 → 选库表」展示,且仅列<b>当前权益类型可授</b>的已确权数据表(经营权另需在对外开放目录);
            勾选后自动套用清单头默认(被授权方/场景/时效)+ 确权带出(业务域/第三方/隐私),可跨系统累加一次加入。
          </span>
        </div>
        <div v-if="requiredChecklist.length" style="margin:8px 0 4px">
          <div style="font-weight:600;margin-bottom:8px">应交材料清单 —《表5》由系统按明细自动生成;第三方凭证 / 信息授权协议见下方明细逐表列(确权带出·缺则就地补传)</div>
          <el-table :data="requiredChecklist" border size="small" style="max-width:880px">
            <el-table-column type="index" label="序号" width="56" align="center" />
            <el-table-column prop="materialName" label="应交材料" min-width="170" show-overflow-tooltip />
            <el-table-column prop="required" label="要求" width="84" align="center">
              <template #default="{ row }">
                <span :class="'prm-c-' + ((row.required === '必填' ? 'danger' : 'warning') || 'primary')">{{ row.required }}</span>
              </template>
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
                <!-- 《表5》:系统按已加入明细自动生成,不再手工填+传(消除重复) -->
                <el-tooltip v-if="isTable5(row)" :disabled="items.length>0" content="请先加入授权明细" placement="top">
                  <span><el-button link type="primary" :disabled="items.length===0" @click="genTable5">生成《表5》并下载</el-button></span>
                </el-tooltip>
                <template v-else>
                  <el-upload :show-file-list="false" :http-request="(o)=>doUploadBatchItem(row.materialName, o.file)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" style="display:inline-block">
                    <el-button link type="primary" :loading="matUploading">上传</el-button>
                  </el-upload>
                  <el-button v-if="row.uploaded" link type="success" style="margin-left:6px" @click="previewBatchItem(row.materialName)">预览</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top:6px;color:var(--prm-color-text-weak);font-size:12px;line-height:1.6">《表5》由系统按明细生成;涉第三方 / 涉隐私的许可凭证·信息授权协议在明细表逐表呈现(确权带出免传,确权缺失就地补传)。</div>
        </div>
        <!-- 已加入明细 段头(单行,含项数+口径说明;原 el-divider 与上方说明间距过小重叠,合并为一行) -->
        <div class="batch-detail-head">
          已加入明细({{ items.length }} 项)
          <span class="batch-detail-sub">数据表 × 权益,清单头默认逐条沿用</span>
        </div>
        <!-- 表6「是否跨系统域、跨地域」:跨系统域按已加入项自动判定;跨地域按清单头地理范围判定(均只读) -->
        <el-alert v-if="items.length" :type="crossSystemInfo.isCross || crossGeo ? 'warning' : 'info'" :closable="false" style="margin-bottom:8px;max-width:880px">
          <template #title>
            <span>跨系统域:</span>
            <span style="margin:0 6px" :class="'prm-c-' + ((crossSystemInfo.isCross ? 'warning' : 'info') || 'primary')">{{ crossSystemInfo.isCross ? '是(跨系统域)' : '否(单系统)' }}</span>
            <span>跨地域:</span>
            <span style="margin:0 6px" :class="'prm-c-' + ((crossGeo ? 'warning' : 'info') || 'primary')">{{ crossGeo ? '是(' + (listForm.geoScope || '') + ')' : '否(省内)' }}</span>
            本清单覆盖 {{ crossSystemInfo.systems.length }} 个系统{{ crossSystemInfo.systems.length ? '(' + crossSystemInfo.systems.join('、') + ')' : '' }};表6「是否跨系统域、跨地域」已自动写入各授权项。
          </template>
        </el-alert>
        <!-- 列序/列名严格对齐《表5 数据授权申请单》(申请主体名称/申请单位主管/联系方式为批量清单头共享字段,已在步骤1填,不在此逐行重复) -->
        <el-table :data="pagedItems" border size="small" :row-class-name="itemRowClass">
          <el-table-column type="index" label="#" width="44" align="center" :index="(i) => (itemsPage - 1) * itemsPageSize + i + 1" />
          <el-table-column prop="assetName" label="数据表名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="systemName" label="所属系统" min-width="120" show-overflow-tooltip
            :filters="sysFilterOpts" :filter-method="(v, row) => row.systemName === v">
            <template #default="{ row }">{{ row.systemName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="schemaName" label="模式名称" min-width="110" show-overflow-tooltip>
            <template #default="{ row }">{{ row.schemaName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="businessDomain" label="所属业务域" min-width="100" show-overflow-tooltip>
            <template #default="{ row }">{{ row.businessDomain || '—' }}</template>
          </el-table-column>
          <el-table-column prop="rightType" label="申请权益类型" width="140"
            :filters="[{ text: '使用权', value: '使用权' }, { text: '经营权', value: '经营权' }]" :filter-method="(v, row) => row.rightType === v" />
          <el-table-column prop="equityCardId" label="生效卡片" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.equityCardId || '—' }}</template>
          </el-table-column>
          <!-- 2-A 逐项可选微调:场景/时效默认沿用清单头,可逐行改(改后即存草稿);权益按选取确定不在此改 -->
          <el-table-column label="使用场景及目的摘要" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.scenario" size="small" placeholder="(沿用清单头默认)" @change="persistItem(row)" />
            </template>
          </el-table-column>
          <el-table-column label="权益时效" width="110" align="center">
            <template #default="{ row }">
              <el-select v-model="row.validTerm" size="small" style="width:94px" @change="onTermChange(row)">
                <el-option v-for="t in validTerms" :key="t" :label="t" :value="t" />
              </el-select>
            </template>
          </el-table-column>
          <!-- 表6 合规判定列:第三方/隐私商密 由确权事实带出(只读,堵人工低报);跨域按全清单系统并集判定 -->
          <el-table-column label="是否跨区域、跨域" width="130" align="center"
            :filters="[{ text: '是', value: true }, { text: '否', value: false }]" :filter-method="(v, row) => (crossSystemInfo.isCross || crossGeo) === v">
            <template #default>
              <el-tooltip :content="`跨系统域:${crossSystemInfo.isCross ? '是' : '否'};跨地域:${crossGeo ? '是' : '否'}`" placement="top">
                <span :class="'prm-c-' + ((crossSystemInfo.isCross || crossGeo ? 'warning' : 'info') || 'primary')">{{ crossSystemInfo.isCross || crossGeo ? '是' : '否' }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <!-- 涉及第三方来源方式:显示确权带出的实际来源方式文字(不是单纯是/否),筛选按"是否涉及"分组 -->
          <el-table-column label="涉及第三方来源方式" width="140"
            :filters="[{ text: '涉及', value: true }, { text: '不涉及', value: false }]"
            :filter-method="(v, row) => !!(row.thirdPartySource && String(row.thirdPartySource).trim()) === v">
            <template #default="{ row }">
              <span v-if="row.thirdPartySource && String(row.thirdPartySource).trim()" class="prm-c-warning">{{ row.thirdPartySource }}</span>
              <span v-else style="color:var(--prm-color-text-disabled)">不涉及</span>
            </template>
          </el-table-column>
          <!-- 1-B 第三方凭证逐表对应:涉三方→确权带出(只读·免重传)或待补传(确权缺时行级补);审核可逐表核对 -->
          <el-table-column label="第三方许可凭证或说明" width="140" align="center">
            <template #default="{ row }">
              <span v-if="!(row.thirdPartySource && String(row.thirdPartySource).trim())" style="color:var(--prm-color-text-weak)">—</span>
              <el-tooltip v-else-if="row.thirdPartyLicense && String(row.thirdPartyLicense).trim()" :content="`确权带出:${row.thirdPartyLicense}`" placement="top">
                <span class="prm-c-success">确权带出</span>
              </el-tooltip>
              <span v-else-if="row.thirdPartyMakeup"><span class="prm-c-success">已补传</span></span>
              <el-upload v-else :show-file-list="false" :http-request="(o)=>uploadRowCredential(row, o.file)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" style="display:inline-block">
                <el-button link type="warning" size="small">待补传</el-button>
              </el-upload>
            </template>
          </el-table-column>
          <el-table-column label="涉及个人隐私/商业秘密" width="150" align="center"
            :filters="[{ text: '涉及', value: true }, { text: '不涉及', value: false }]"
            :filter-method="(v, row) => !!(row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无') === v">
            <template #default="{ row }">
              <span :class="'prm-c-' + ((row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无' ? 'danger' : 'info') || 'primary')">
                {{ row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无' ? row.sensitiveType : '不涉及' }}
              </span>
            </template>
          </el-table-column>
          <!-- 1-B 对称:信息授权协议逐表对应(涉隐私→确权带出·免重传 / 待补传;与第三方凭证同模式) -->
          <el-table-column label="信息授权协议" width="124" align="center">
            <template #default="{ row }">
              <span v-if="!(row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无')" style="color:var(--prm-color-text-weak)">—</span>
              <el-tooltip v-else-if="row.infoAuthAgreement && String(row.infoAuthAgreement).trim()" :content="`确权带出:${row.infoAuthAgreement}`" placement="top">
                <span class="prm-c-success">确权带出</span>
              </el-tooltip>
              <span v-else-if="row.privacyMakeup"><span class="prm-c-success">已补传</span></span>
              <el-upload v-else :show-file-list="false" :http-request="(o)=>uploadRowPrivacy(row, o.file)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" style="display:inline-block">
                <el-button link type="warning" size="small">待补传</el-button>
              </el-upload>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" size="small" @click="removeItem(row, items.indexOf(row))">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-if="items.length" style="margin-top:12px;justify-content:flex-end" background
          layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
          :total="items.length" :current-page="itemsPage" :page-size="itemsPageSize"
          @current-change="p => itemsPage = p" @size-change="s => { itemsPageSize = s; itemsPage = 1 }" />
        <el-empty v-if="items.length===0" :image-size="60" description="尚未添加授权项 — 点上方「① 从确权目录批量选取数据资产」" />
      </el-card>

      <!-- 步骤3:提交前自检 + 提交(申报人侧自检,非合规小组正式审查;校验→提交同处一列,逐项明细可见可核对可定位) -->
      <el-card v-show="step === 2" shadow="never">
        <el-result icon="info" :title="`提交《批量授权清单》申报稿（${items.length} 项）`" sub-title="提交前自检 → 合规审查 → 清单审核审批 → 领导小组决策批准" />
        <!-- 单一裁决状态条:一句话说清"能不能提交、还差什么" -->
        <div style="text-align:center;margin:4px 0 10px">
          校验状态:<span :class="'prm-c-' + ((checkStatus.type) || 'primary')">{{ checkStatus.text }}</span>
        </div>
        <!-- 主操作:① 一键校验 → ② 提交(就近;校验通过才点亮提交,动线连贯) -->
        <div style="text-align:center">
          <el-button :type="complianceResult && !complianceResult.allPass ? 'danger' : 'primary'" :loading="complianceChecking" @click="runComplianceCheck">
            {{ complianceResult ? '② 重新一键校验' : '① 一键自检(全部明细)' }}
          </el-button>
          <el-tooltip :disabled="canSubmit" content="请先通过提交前自检(全部明细合规)" placement="top">
            <span style="margin-left:10px">
              <el-button type="success" :disabled="!canSubmit" :loading="submitting" @click="doSubmit">② 提交申报稿</el-button>
            </span>
          </el-tooltip>
        </div>
        <!-- 次要:AI 辅助(可选,不影响提交门禁) -->
        <div style="text-align:center;margin:4px 0 0;color:var(--prm-color-text-weak);font-size:12px">
          AI 辅助(可选,不影响提交):
          <el-button link type="primary" :loading="listReviewing" @click="runListPreReview">AI 清单预审(qwen3-max)</el-button>
        </div>
        <AiThinking v-bind="aiThink.state" />
        <!-- 逐项逐维度校验明细(通过项也展示;不再是只回被拦项的黑盒)→ 可核对·可定位·去修正回明细高亮 -->
        <div v-if="complianceResult && complianceResult.items && complianceResult.items.length" style="margin-top:20px">
          <div style="font-weight:600;margin-bottom:6px">校验明细(共 {{ complianceResult.items.length }} 项,其中 {{ complianceResult.blockedCount }} 项需处理)— 悬停维度看说明</div>
          <el-table :data="complianceResult.items" border size="small" max-height="320">
            <el-table-column type="index" label="#" width="44" align="center" />
            <el-table-column prop="assetName" label="数据资产" min-width="150" show-overflow-tooltip />
            <el-table-column label="校验维度（✓ 通过 / ✗ 不通过 / — 不涉及）" min-width="400">
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
        <el-alert v-if="listOpinion" type="info" :closable="false" style="margin-top:12px" title="AI 清单预审意见" :description="listOpinion" show-icon />
      </el-card>

      <!-- 提交成功页 + 后续流转进度时间轴(单一真相,对齐 35号文 附录C 表1) -->
      <el-card v-show="submitted" shadow="never">
        <el-result icon="success" title="批量授权清单申报稿已提交" :sub-title="`清单 ${listNo}（${items.length} 项）已进入审批链,当前待「合规管控小组审核」`">
          <template #extra>
            <div class="wz-progress">
              <div class="wz-progress-t">后续流转进度(实时以「清单管理」为准)</div>
              <AuthFlowProgress mode="batch" current="compliance" />
            </div>
            <div style="margin-top:6px">
              <el-button type="primary" @click="go('/dpr/auth/batch-list')">去清单管理查看进度</el-button>
              <el-button @click="go('/dpr/auth/filing')">对外经营权备案(附录G)</el-button>
              <el-button @click="reset">再建一份清单</el-button>
            </div>
          </template>
        </el-result>
      </el-card>
    </div>

    <!-- step3 的「提交申报稿」已就近放进卡片内(校验下方),此处仅留上一步/下一步,避免提交双入口 -->
    <PageActions v-if="!submitted">
      <el-button v-if="step > 0" @click="step--">上一步</el-button>
      <!-- PDD 8.1 独立「保存草稿」:宽进(仅需授权年度建清单草案,不校验其余、不前进);中途离开可续填 -->
      <el-button :loading="savingDraft" @click="saveDraftBatch">保存草稿</el-button>
      <el-button v-if="step === 0" type="primary" :loading="creating" @click="next0">下一步</el-button>
      <el-button v-if="step === 1" type="primary" :disabled="items.length===0" @click="step = 2">下一步</el-button>
    </PageActions>

    <!-- 从确权目录多选资产(资源池:先确后授 + 权属可授 + 经营权对外开放,展示对齐确权范围树) -->
    <el-dialog v-model="pickerDlg" title="从确权目录批量选取数据资产" width="600px" align-center>
      <el-alert type="info" :closable="false" style="margin-bottom:10px">
        仅列「{{ listForm.rightType || '所选权益' }}」可授的已确权数据表(有生效权益卡片{{ listForm.rightType === '经营权' ? ' + 在对外开放目录' : '' }});
        被授权方/场景/时效取清单头(批量共享),勾选多个可跨系统累加一次加入。
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
import { ElMessage } from 'element-plus'
import { confirmAsync } from '@/utils/confirmAsync'
import { createBatchList, saveAuthDraft, deleteAuthApply, submitBatchList, getBatchList, listAuthByBatch, aiBatchPreReview, listAuthMaterialRules, checkBatchCompliance, uploadAuthMaterialFile, listAuthMaterial, authMaterialFileUrl } from '@/api/authorize'
import { openFilePreview } from '@/composables/useFilePreview'
import AiThinking from '@/components/AiThinking.vue'
import PageActions from '@/components/PageActions.vue'
import AuthFlowProgress from '@/components/AuthFlowProgress.vue'
import GrantableCatalogTree from './GrantableCatalogTree.vue'
import { useAiThinking } from '@/composables/useAiThinking'
import { AI_PHASES } from '@/lib/aiPhases'
const aiThink = useAiThinking()
import { pageEquityCard, getRightsFacts } from '@/api/confirm'
import { listOrg } from '@/api/org'
import { downloadTable5 } from '@/lib/table5Export'

const router = useRouter()
const route = useRoute()
const savingDraft = ref(false)
const rightTypes = ['使用权', '经营权']
// 授权时效:申报阶段填「时长」(表5/表6 默认两年),保存时映射为预期到期日(validDate);
// 协议签订时按附录D「自签订日起+时长,一般3年最长5年」最终落定。
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
// 数据使用地理范围(表6「跨地域」判定来源 + 协议附录D 表1 地理范围缺省):超出授权方属地即跨地域
const geoScopes = ['广东省行政区域内', '南方电网经营区域(粤桂滇黔琼)', '中华人民共和国境内']
const GEO_HOME = '广东省行政区域内'
const step = ref(0)
// 提交后进入「提交成功页 + 进度时间轴」(创建/流转分离;流转属多主体跨周案件,不再当作向导第4步)
const submitted = ref(false)
// 步骤条回跳:仅"已完成"步骤(idx < 当前步)可点,提交完成后不可再跳;前进仍须走「下一步」校验
function stepJumpable(idx) { return !submitted.value && idx < step.value }
function onStepClick(idx) { if (stepJumpable(idx)) step.value = idx }
const creating = ref(false); const submitting = ref(false)
const batchListId = ref(''); const listNo = ref('')
const items = ref([])
// 已加入明细分页(纯前端,10行/页;数据全在内存 items,无需改接口)
const itemsPage = ref(1); const itemsPageSize = ref(10)
const pagedItems = computed(() => {
  const start = (itemsPage.value - 1) * itemsPageSize.value
  return items.value.slice(start, start + itemsPageSize.value)
})
watch(items, () => { if (itemsPage.value > 1 && (itemsPage.value - 1) * itemsPageSize.value >= items.value.length) itemsPage.value = 1 })
// 所属系统筛选选项(去重,随已加入明细动态变化)
const sysFilterOpts = computed(() => [...new Set(items.value.map(i => i.systemName).filter(Boolean))].map(name => ({ text: name, value: name })))
// 清单头(批量级):被授权方为批量共享;权益类型/场景/时效为整批默认,加项时带入(资源池按权益类型过滤)
const listForm = reactive({ listYear: '', granteeOrg: '', contactPerson: '', contactInfo: '', rightType: '', scenario: '', validTerm: '两年', geoScope: GEO_HOME, remark: '' })
// 跨地域(表6):数据使用地理范围超出授权方属地(广东省行政区域内)即判「是」
const crossGeo = computed(() => !!listForm.geoScope && listForm.geoScope !== GEO_HOME)
function emptyItem() {
  return { assetId: '', assetName: '', tableCode: '', systemName: '', schemaName: '', equityCardId: '', granteeOrg: '', rightType: '', scenario: '', validTerm: '两年', validDate: '', businessDomain: '', thirdPartySource: '', thirdPartyLicense: '', thirdPartyMakeup: '', sensitiveType: '', infoAuthAgreement: '', privacyMakeup: '', crossRegion: false, applicantManager: '', contactInfo: '' }
}
// 表6「是否跨系统域、跨地域」:批量可跨多系统 → 按清单已加入项的系统/业务域去重自动判定(只读呈现 + 写回 crossRegion)
const crossSystemInfo = computed(() => {
  const systems = [...new Set(items.value.map(x => x.systemName).filter(Boolean))]
  const domains = [...new Set(items.value.map(x => x.businessDomain).filter(Boolean))]
  return { systems, domains, isCross: systems.length > 1 || domains.length > 1 }
})
// 去重键:被授权方批量恒定,故行唯一性=库表+权益类型+场景(对齐表4/表6:同表可多权益/多场景,三者全同才是真重复)。
// 注:权益卡片库表级后 assetId=SYS:系统(同系统多库表共享),故须并入 tableCode/库表名 区分,避免同系统不同库表被误判重复。
function dupKey(x) {
  return `${x.assetId || ''}|${x.tableCode || x.assetName || ''}|${x.rightType || ''}|${(x.scenario || '').trim()}`
}

// 应交材料清单由后端可配置规则(单一真源·场景批量)生成;规则不可用时回退内置默认,保证申请人永远看得到"该传哪些材料"
const materialRules = ref([])
const matUploading = ref(false)
const batchMaterials = ref([]) // 清单级材料(挂在 batchListId 上)
// 内置兜底(对齐联调材料清单 Excel·批量):表5必填 + 涉三方→许可凭证 + 涉隐私商密→信息授权协议
// 应交材料只保留《表5》(系统生成);第三方凭证 / 信息授权协议改逐表呈现于明细表(确权带出/待补传),
// 故不再在此列 THIRD_PARTY / SENSITIVE 全单级项(requiredChecklist 也只取 ALWAYS),消除两处口径打架。
const FALLBACK_RULES = [
  { triggerType: 'ALWAYS', materialName: '《表5 数据授权申请单》', required: '必填', detail: '系统按已加入明细自动生成(申请主体/系统/模式/数据表/权益/场景/时效/跨域等),无需手工填写;点「生成」下载留痕或线下签署' }
]
const requiredChecklist = computed(() => {
  // 第三方许可凭证 / 信息授权协议 已逐表呈现于明细表(确权带出 / 待补传 行级补),不再在应交材料全单级重复,
  // 避免"上面待上传、下面确权带出"口径打架 → 应交材料只保留必交项(《表5》系统生成)。
  const hit = (r) => r.triggerType === 'ALWAYS'
  const src = materialRules.value.length ? materialRules.value : FALLBACK_RULES
  return src.filter(hit).map(r => ({
    ...r,
    uploaded: batchMaterials.value.some(m => {
      const mn = m.materialName || ''
      return mn === r.materialName || mn.includes(r.materialName) || r.materialName.includes(mn)
    })
  }))
})
async function refreshBatchMaterials() {
  if (batchListId.value) batchMaterials.value = await listAuthMaterial(batchListId.value) || []
}
// 《表5》= 系统生成项(不让用户手工填+传,消除与明细的重复)
function isTable5(row) { return (row.materialName || '').includes('表5') }
// 按已加入明细 + 清单头自动生成《表5 数据授权申请单》并下载(对齐 35号文 附录C 表5)
function genTable5() {
  if (!items.value.length) { ElMessage.warning('请先加入授权明细'); return }
  downloadTable5(listForm, items.value, crossSystemInfo.value.isCross || crossGeo.value)
  ElMessage.success('《表5》已按明细生成并下载,可留痕或线下签署')
}
// 在线预览已上传的清单级材料(按应交项名匹配)
function previewBatchItem(materialName) {
  const m = batchMaterials.value.find(x => {
    const mn = x.materialName || ''
    return mn === materialName || mn.includes(materialName) || materialName.includes(mn)
  })
  if (m && m.materialId) openFilePreview(authMaterialFileUrl(m.materialId), m.fileName || materialName)
}
// 按应交清单逐项上传(清单级材料挂 batchListId,材料名=应交项名)
async function doUploadBatchItem(materialName, file) {
  if (!batchListId.value) { ElMessage.warning('请先建批量清单'); return }
  matUploading.value = true
  try {
    const fd = new FormData(); fd.append('file', file); fd.append('applyId', batchListId.value); fd.append('materialName', materialName)
    await uploadAuthMaterialFile(fd)
    ElMessage.success(`已上传「${materialName}」`); refreshBatchMaterials()
  } finally { matUploading.value = false }
}
// 被授权方组织选择器:接南网真实组织树(listOrg);外部主体走自由文本例外(对外经营权附录G)
const orgOptions = ref([])
const externalGrantee = ref(false)
onMounted(async () => {
  try {
    const rules = await listAuthMaterialRules('批量')
    if (Array.isArray(rules) && rules.length) materialRules.value = rules
  } catch (e) { /* 规则接口不可用 → requiredChecklist 自动用内置兜底,不丢指引 */ }
  try {
    orgOptions.value = (await listOrg()) || []
  } catch (e) { /* 组织接口不可用 → 选择器降级为可输入(allow-create),不阻断申报 */ }
  applyRoutePrefill()
})

// 路由预填:① ?batchListId 草稿就地续填(保 batchListId 续存同一清单)② ?reopen 修改重提(复制清单头,新清单)
function applyRoutePrefill() {
  const q = route.query
  if (q.batchListId) { loadBatchDraft(String(q.batchListId)); return }
  if (q.reopen) {
    try {
      const o = JSON.parse(sessionStorage.getItem('prm-reopen') || '{}')
      if (o.domain === '授权' && o.raw) {
        const r = o.raw
        Object.assign(listForm, {
          listYear: String(new Date().getFullYear()),
          granteeOrg: r.granteeOrg || '', rightType: rightTypes.includes(r.rightType) ? r.rightType : listForm.rightType,
          scenario: r.scenario || '', contactPerson: r.applicantManager || '', contactInfo: r.contactInfo || ''
        })
        ElMessage.warning('已带入被驳回原单头,请重新选取授权数据表后提交(将作为新清单)')
      }
    } catch (e) { /* ignore */ }
    sessionStorage.removeItem('prm-reopen')
  }
}

// 用当前清单头刷新每条明细共享字段并落库
async function persistAllItems() {
  for (const it of items.value) {
    if (!it.applyId) continue
    Object.assign(it, {
      granteeOrg: listForm.granteeOrg, scenario: listForm.scenario, validTerm: listForm.validTerm,
      validDate: expiryOf(listForm.validTerm), applicantManager: listForm.contactPerson, contactInfo: listForm.contactInfo
    })
    await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...it })
  }
}

// 主动保存草稿(PDD 8.1 独立存草稿):宽进——落库底线=已填授权年度(据此建清单草案);不校验其余、不前进。
// 建清单(如未建)→刷新全部明细。可离开后在「我的申请」草稿续填(或「批量授权清单」页找草案)。
async function saveDraftBatch() {
  if (!batchListId.value && !listForm.listYear) {
    ElMessage.warning('请先填写授权年度以建立清单草案,方可暂存草稿')
    return
  }
  savingDraft.value = true
  try {
    if (!batchListId.value) {
      batchListId.value = await createBatchList({ listYear: listForm.listYear, remark: listForm.remark, geoScope: listForm.geoScope })
      listNo.value = listForm.listYear + ' 批量授权清单'
    }
    await persistAllItems()
    ElMessage.success(`草稿已保存(清单 ${listNo.value || batchListId.value})，可随时离开后在「我的申请」→ 草稿「编辑」继续`)
  } catch (e) { /* 拦截器已 toast */ } finally { savingDraft.value = false }
}

// 草稿就地续填:按 batchListId 回填清单头 + 明细(保各行 applyId,续存同一清单,不复制)
async function loadBatchDraft(id) {
  try {
    const l = await getBatchList(id)
    if (!l || !l.batchListId) { ElMessage.warning('草稿清单不存在或已提交'); return }
    if (l.listStatus && l.listStatus !== '草案') {
      ElMessage.warning('该清单已提交,非草案不可就地编辑;如需修改请走撤回'); return
    }
    batchListId.value = l.batchListId
    listNo.value = l.listNo || (l.listYear + ' 批量授权清单')
    Object.assign(listForm, { listYear: l.listYear || '', remark: l.remark || '', geoScope: l.geoScope || GEO_HOME })
    const rows = await listAuthByBatch(id)
    if (Array.isArray(rows) && rows.length) {
      const h = rows[0]
      Object.assign(listForm, {
        granteeOrg: h.granteeOrg || '', rightType: rightTypes.includes(h.rightType) ? h.rightType : listForm.rightType,
        scenario: h.scenario || '', validTerm: h.validTerm || '两年',
        contactPerson: h.applicantManager || '', contactInfo: h.contactInfo || ''
      })
      items.value = rows.map(r => ({ ...emptyItem(), ...r }))
    }
    step.value = 1
    ElMessage.success('已载入草稿清单,可继续加入授权项并「保存草稿」或提交')
  } catch (e) { ElMessage.warning('载入草稿失败,请重试') }
}

// 合规校验闭环:清单明细变化即让上次校验失效(必须重新校验才能提交)
const complianceResult = ref(null)
const complianceChecking = ref(false)
watch(items, () => { complianceResult.value = null }, { deep: true })

// 提交门禁:合规校验通过才放行(消灭"点了必然被拒"的死路)
const canSubmit = computed(() => !!complianceResult.value && complianceResult.value.allPass)
const checkStatus = computed(() => {
  if (!complianceResult.value) return { type: 'info', text: '未校验' }
  if (complianceResult.value.allPass) return { type: 'success', text: '全部合规,可提交' }
  return { type: 'danger', text: `未通过(${complianceResult.value.blockedCount} 项被拦)` }
})

// 只读合规校验(与提交门禁同源),失败项就地暴露,引导修正后重新校验直至通过
async function runComplianceCheck() {
  if (!batchListId.value) { ElMessage.warning('请先建清单并加入授权项'); return }
  if (!items.value.length) { ElMessage.warning('清单为空,请先加入授权项'); return }
  complianceChecking.value = true
  try {
    complianceResult.value = await checkBatchCompliance(batchListId.value)
    if (complianceResult.value.allPass) ElMessage.success('提交前自检通过,可提交清单审批')
    else ElMessage.warning(`提交前自检未通过:${complianceResult.value.blockedCount} 项被拦,请逐条修正后重新校验`)
  } finally { complianceChecking.value = false }
}
// 块3:从校验明细「去修正」→ 回明细步并高亮定位到该数据表行(按 applyId 匹配)
const highlightApplyId = ref('')
function itemRowClass({ row }) { return row.applyId && row.applyId === highlightApplyId.value ? 'fix-highlight' : '' }
function goFixItem(row) {
  highlightApplyId.value = row.applyId
  step.value = 1
  ElMessage.info(`已切到明细,请修正「${row.assetName}」(已高亮定位)`)
  setTimeout(() => { if (highlightApplyId.value === row.applyId) highlightApplyId.value = '' }, 5000)
}

async function next0() {
  if (!listForm.listYear) { ElMessage.warning('请填写授权年度'); return }
  if (!listForm.granteeOrg) { ElMessage.warning('请填写被授权方(本批数据统一授给谁)'); return }
  if (!listForm.contactPerson) { ElMessage.warning('请填写联系人/申请单位主管(表5/表6 必填)'); return }
  if (!listForm.contactInfo) { ElMessage.warning('请填写联系方式(表5/表6 必填)'); return }
  if (!listForm.rightType) { ElMessage.warning('请选择默认权益类型(确权目录资源池按权益类型过滤可授数据表)'); return }
  if (!batchListId.value) {
    creating.value = true
    try {
      batchListId.value = await createBatchList({ listYear: listForm.listYear, remark: listForm.remark, geoScope: listForm.geoScope })
      listNo.value = listForm.listYear + ' 批量授权清单'
    } finally { creating.value = false }
  }
  step.value = 1
}

// 唯一录入入口:从确权目录资源池多选(先确后授 + 权属可授 + 经营权对外开放,已在树侧过滤)
const pickerDlg = ref(false); const picking = ref(false); const pickedLeaves = ref([])
async function openPicker() {
  if (!listForm.granteeOrg) { ElMessage.warning('请先在步骤1清单头填写"被授权方"(批量共享)'); return }
  if (!listForm.rightType) { ElMessage.warning('请先在清单头选择"权益类型"(资源池据此过滤可授数据表)'); return }
  pickedLeaves.value = []
  pickerDlg.value = true
}
// 资源池树勾选回传(每个叶子带 assetId/equityCardId/rightType/系统/模块)
function onPickChange(leaves) { pickedLeaves.value = leaves || [] }
async function confirmPick() {
  if (!pickedLeaves.value.length) { ElMessage.warning('请至少勾选一张数据表'); return }
  picking.value = true
  try {
    let ok = 0
    const dups = []
    const seen = new Set(items.value.map(dupKey))
    // 跨域自动判定(表6「是否跨系统域、跨地域」):系统/业务域并集>1 或 地理范围超出属地(写回每条 crossRegion)
    const sysUnion = new Set([...items.value.map(x => x.systemName).filter(Boolean),
      ...pickedLeaves.value.map(l => l.systemName).filter(Boolean)])
    const isCross = sysUnion.size > 1 || crossGeo.value
    for (const lf of pickedLeaves.value) {
      const f = await deriveFacts(lf.assetId)
      const it = { ...emptyItem(), assetId: lf.assetId, assetName: lf.assetName, tableCode: lf.tableCode,
        systemName: lf.systemName, schemaName: lf.schemaName, equityCardId: lf.equityCardId,
        granteeOrg: listForm.granteeOrg, rightType: lf.rightType || listForm.rightType,
        scenario: listForm.scenario, validTerm: listForm.validTerm, validDate: expiryOf(listForm.validTerm),
        businessDomain: lf.businessDomain || f.businessDomain || '', // 业务域由确权目录(lf)优先、确权事实(f)兜底带出,不再用清单头手填默认
        thirdPartySource: f.thirdPartySource, thirdPartyLicense: f.thirdPartyLicense, sensitiveType: f.sensitiveType, infoAuthAgreement: f.infoAuthAgreement,
        crossRegion: isCross, applicantManager: listForm.contactPerson, contactInfo: listForm.contactInfo }
      const k = dupKey(it)
      if (seen.has(k)) { dups.push(lf.assetName); continue } // 同表+权益+场景已在清单,去重
      seen.add(k)
      const id = await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...it })
      items.value.push({ ...it, applyId: id })
      ok++
    }
    ElMessage[dups.length ? 'warning' : 'success'](
      `已加入 ${ok} 项(资源池均已确权+权属可授,生效卡自动带入)`
      + (dups.length ? `;跳过重复 ${dups.length} 项:${dups.slice(0, 3).join('、')}` : ''))
    pickerDlg.value = false
    pickedLeaves.value = []
  } finally { picking.value = false }
}

// AI 清单预审(qwen3-max,stub 回退)
const listReviewing = ref(false); const listOpinion = ref('')
async function runListPreReview() {
  listReviewing.value = true
  try {
    listOpinion.value = await aiThink.run(() => aiBatchPreReview(batchListId.value),
      { phases: AI_PHASES.batchPreReview, title: '大模型清单预审中' })
  } catch (e) { ElMessage.warning('AI 预审失败') }
  finally { listReviewing.value = false }
}

// 自动配卡引擎:按资产匹配生效权益卡片(先确后授 + 权属可授:卡片权益==所选权益)。供一键示例配卡。
const CARD_OK = ['正常', '生效']
const cardOpts = ref([])
let cardsLoaded = false
async function loadCards() {
  if (cardsLoaded) return
  const r = await pageEquityCard({ current: 1, size: 100 })
  cardOpts.value = r.records || []
  cardsLoaded = true
}
// 权属可授:仅返回"卡片权益类型 == 指定权益类型"的生效卡片号(授权只授使用权/经营权)
function findUsableCard(assetId, right) {
  const c = cardOpts.value.find(x => x.assetId === assetId && CARD_OK.includes(x.cardStatus) && (!right || x.rightType === right))
  return c ? (c.cardNo || c.cardId) : ''
}
// 确权信息带出:按资产取最新已完成确权的第三方来源/隐私商密事实(只读,堵人工低报)
async function deriveFacts(assetId) {
  try {
    const f = await getRightsFacts(assetId)
    return { thirdPartySource: f?.thirdPartySource || '', thirdPartyLicense: f?.thirdPartyLicense || '', sensitiveType: f?.sensitiveType || '无', infoAuthAgreement: f?.infoAuthAgreement || '', businessDomain: f?.businessDomain || '' }
  } catch { return { thirdPartySource: '', thirdPartyLicense: '', sensitiveType: '无', infoAuthAgreement: '', businessDomain: '' } }
}

// 一键示例:建清单 + 自动加入可授明细(对齐资源池过滤:使用权下仅 AST-002 台区负荷数据有匹配生效卡片)
const demoFilling = ref(false)
async function fillDemo() {
  demoFilling.value = true
  try {
    listForm.listYear = '2026'
    listForm.granteeOrg = '南网综合能源股份有限公司'
    listForm.contactPerson = '张三'
    listForm.contactInfo = '020-31000000'
    listForm.rightType = '使用权'
    listForm.scenario = '综合能源服务'
    listForm.remark = '综能板块年度批量授权(示例)'
    await next0()
    await loadCards()
    const demo = [['AST-002', '台区负荷数据', '计量系统']]
    let ok = 0
    const seen = new Set(items.value.map(dupKey))
    for (const [aid, name, sys] of demo) {
      const card = findUsableCard(aid, listForm.rightType) // 权属可授:卡片权益须==所选权益
      if (!card) continue
      const f = await deriveFacts(aid)
      const it = { ...emptyItem(), assetId: aid, assetName: name, systemName: sys, equityCardId: card,
        granteeOrg: listForm.granteeOrg, rightType: listForm.rightType, scenario: listForm.scenario, validTerm: listForm.validTerm, validDate: expiryOf(listForm.validTerm),
        businessDomain: f.businessDomain, thirdPartySource: f.thirdPartySource, thirdPartyLicense: f.thirdPartyLicense, sensitiveType: f.sensitiveType, infoAuthAgreement: f.infoAuthAgreement,
        applicantManager: listForm.contactPerson, contactInfo: listForm.contactInfo }
      const k = dupKey(it)
      if (seen.has(k)) continue // 去重:同表+权益+场景已在清单
      seen.add(k)
      const id = await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...it })
      items.value.push({ ...it, applyId: id })
      ok++
    }
    ElMessage.success(`示例完成:清单已建,自动加入 ${ok} 条可授明细(生效卡已配),可直接"提交清单审批"`)
  } finally { demoFilling.value = false }
}

// 2-A:逐行可选微调(场景/时效)→ 即存草稿,避免提交丢编辑(saveDraft 带 applyId 即更新)
async function persistItem(row) {
  if (!row.applyId) return
  try { await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...row }) }
  catch (e) { ElMessage.warning('保存修改失败:' + (e?.message || '')) }
}
function onTermChange(row) {
  row.validDate = expiryOf(row.validTerm) // 时效改 → 重算到期日
  persistItem(row)
}
// 1-B 兜底:确权侧缺第三方凭证时,该数据表行级补传(挂资产名,逐表对应,审核可核)
async function uploadRowCredential(row, file) {
  try {
    await doUploadBatchItem(`第三方许可凭证·${row.assetName}`, file)
    row.thirdPartyMakeup = file.name
    ElMessage.success(`「${row.assetName}」第三方凭证已补传`)
  } catch (e) { ElMessage.warning('补传失败:' + (e?.message || '')) }
}
// 1-B 对称:确权侧缺信息授权协议时,该数据表行级补传(涉隐私,挂资产名,逐表对应)
async function uploadRowPrivacy(row, file) {
  try {
    await doUploadBatchItem(`信息授权协议·${row.assetName}`, file)
    row.privacyMakeup = file.name
    ElMessage.success(`「${row.assetName}」信息授权协议已补传`)
  } catch (e) { ElMessage.warning('补传失败:' + (e?.message || '')) }
}

// 删行:从清单移除一条授权项(草稿态,删后端 + 移本地);加错/重复项可撤掉
function removeItem(row, idx) {
  confirmAsync(`确认从清单移除「${row.assetName}」?`, '删除授权项', async () => {
    try {
      if (row.applyId) await deleteAuthApply(row.applyId)
      items.value.splice(idx, 1)
      ElMessage.success('已移除')
    } catch (e) { ElMessage.warning('删除失败:' + (e?.message || '')) }
  }, { confirmButtonText: '删除', cancelButtonText: '取消' }).catch(() => {})
}

async function doSubmit() {
  submitting.value = true
  try { await submitBatchList(batchListId.value); submitted.value = true }
  finally { submitting.value = false }
}

function go(p) { router.push(p) }
function reset() {
  step.value = 0; submitted.value = false; externalGrantee.value = false; batchListId.value = ''; listNo.value = ''; items.value = []; pickedLeaves.value = []
  Object.assign(listForm, { listYear: '', granteeOrg: '', contactPerson: '', contactInfo: '', rightType: '', scenario: '', validTerm: '两年', geoScope: GEO_HOME, remark: '' })
}
</script>

<style scoped>
.wz-steps { max-width: 900px; margin: 8px auto 20px; }
.wz-steps :deep(.wz-step-clickable) { cursor: pointer; }
.wz-steps :deep(.wz-step-clickable:hover .el-step__title) { opacity: .72; }
.wz-body { min-height: 340px; }
.wz-progress { background: #f7f9ff; border-radius: 8px; padding: 10px 20px 0; margin: 0 auto 10px; max-width: 560px; text-align: left; }
.wz-progress-t { font-weight: 600; font-size: 13px; color: var(--prm-color-text); margin-bottom: 10px; }
.batch-primary { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; padding: 10px 20px; margin-bottom: 10px; background: linear-gradient(180deg, #eef4ff, #f7faff); border: 1px solid #d6e4ff; border-radius: 8px; }
.batch-primary-hint { color: var(--prm-color-text-secondary); font-size: 13px; line-height: 1.5; flex: 1; min-width: 240px; }
.batch-detail-head { margin: 18px 0 8px; padding-left: 10px; border-left: 4px solid var(--prm-color-primary); font-weight: 600; color: var(--prm-color-text); line-height: 1.5; }
.batch-detail-sub { margin-left: 8px; font-weight: 400; font-size: 12px; color: var(--prm-color-text-weak); }
/* 「去修正」定位高亮:明细行短暂高亮,引导用户改对应项 */
:deep(.el-table .fix-highlight) { background: #fff7e6 !important; animation: fixflash 1s ease-in-out 2; }
@keyframes fixflash { 0%,100% { background: #fff7e6; } 50% { background: #ffe7ba; } }
</style>
