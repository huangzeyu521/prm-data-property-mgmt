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
        <el-form ref="formRef" :model="form" :rules="rules" label-width="150px" :disabled="!!applyId" style="max-width:640px">
          <el-form-item label="关联数据资产卡片" prop="assetId">
            <div style="display:flex;gap:8px;width:100%">
              <el-select v-model="form.assetId" filterable remote clearable
                :remote-method="searchAssets" :loading="assetSearching" style="flex:1"
                placeholder="搜索卡片名称/编码/系统,选取数据资产卡片" @change="onAssetPicked">
                <el-option v-for="a in assetOpts" :key="a.assetId" :value="a.assetId" :label="a.assetName || a.assetId">
                  <span>{{ a.assetName || a.assetId }}</span>
                  <span style="float:right;color:#8a8a8a;font-size:12px">{{ a.cardCode || a.systemName || a.assetId }}</span>
                </el-option>
              </el-select>
              <el-button type="primary" plain :loading="autoLoading" @click="onAutofill">元数据自动填充</el-button>
            </div>
            <div class="form-tip">在数据资产管理平台卡片中按 名称/编码/系统 搜索选取;关联键为平台卡片ID,选取后自动带出名称/系统/表(不手填)。平台未接入时回退产权台账。</div>
            <div v-if="pickedCard && (pickedCard.systemName || pickedCard.schemaName || pickedCard.tableName)" class="form-tip" style="color:#71717a">
              已选卡片:{{ [pickedCard.systemName, pickedCard.schemaName, pickedCard.tableName].filter(Boolean).join(' / ') }}
            </div>
            <el-tag v-if="quality !== null" :type="quality < 80 ? 'danger' : 'success'" effect="plain" style="margin-top:6px">
              元数据质量评分 {{ quality }}{{ quality < 80 ? ' · 低于80,提交将被自动驳回(请先治理元数据)' : '' }}
            </el-tag>
          </el-form-item>
          <el-form-item label="资产名称" prop="assetName"><el-input v-model="form.assetName" readonly placeholder="选取卡片后自动带出" /></el-form-item>
          <el-form-item label="所属业务系统">
            <el-input v-model="form.systemName" placeholder="选取卡片后自动带出(附录F表1 系统名称)">
              <template #suffix><span v-if="form.systemName" style="color:#67c23a;font-size:12px">平台卡片</span></template>
            </el-input>
          </el-form-item>
          <el-form-item label="权属类型" prop="rightTypes">
            <el-select v-model="form.rightTypes" multiple style="width:100%" placeholder="可多选,多种权属类型合并一份申请">
              <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
            </el-select>
            <div class="form-tip">支持多选:多种权属类型合并发起同一份申请(评审优化)</div>
          </el-form-item>
          <el-form-item label="申报权属主体">
            <el-input v-model="form.rightHolder" placeholder="当前申报主体,最终权属以确权审核结果为准" />
            <div class="form-tip">分省上报的数据产权,确权通过后统一归口中国南方电网有限责任公司</div>
          </el-form-item>
          <el-form-item label="责任部门"><el-input v-model="form.respDept" placeholder="选取卡片后自动带出(卡片管理部门)" /></el-form-item>
          <el-form-item label="系统负责人">
            <el-input v-model="form.systemOwner" placeholder="选取卡片后自动带出(卡片责任人)">
              <template #suffix><span v-if="form.systemOwner" style="color:#67c23a;font-size:12px">平台卡片</span></template>
            </el-input>
          </el-form-item>
          <el-form-item label="联系方式">
            <el-input v-model="form.contactInfo" placeholder="选取卡片后自动带出(责任人电话)">
              <template #suffix><span v-if="form.contactInfo" style="color:#67c23a;font-size:12px">平台卡片</span></template>
            </el-input>
          </el-form-item>
          <el-form-item label="登记类型">
            <el-radio-group v-model="form.registerType">
              <el-radio value="初始确权">初始确权</el-radio>
              <el-radio value="确权变更">确权变更</el-radio>
              <el-radio value="产权补录">产权补录(存量系统 MDAU 工单)</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="管制属性">
            <el-radio-group v-model="form.regulated">
              <el-radio value="管制业务">管制业务</el-radio>
              <el-radio value="非管制">非管制</el-radio>
            </el-radio-group>
            <div class="form-tip">权益归集判定关键输入:管制单位默认没有限经营权;自行生产且不涉第三方时,管制单位经营权调整为有,确权时直接归属网公司(无转让动作)</div>
          </el-form-item>
          <el-form-item label="申请模式">
            <el-radio-group v-model="form.applyMode">
              <el-radio value="常规">常规</el-radio>
              <el-radio value="一事一议">一事一议(特殊事项单独审议)</el-radio>
            </el-radio-group>
            <div class="form-tip">权属复杂/跨主体/全网统一转让等特殊场景选择"一事一议",由合规管控小组单独组织审议</div>
          </el-form-item>
          <el-form-item label="来源权益识别">
            <el-checkbox-group v-model="form.sourceIdent">
              <el-checkbox v-for="s in sourceOpts" :key="s.v" :value="s.v">{{ s.v }} {{ s.t }}</el-checkbox>
            </el-checkbox-group>
            <div class="form-tip">已按所选卡片的库表元数据(来源判定)自动聚合预勾,可手动调整</div>
          </el-form-item>
          <el-form-item label="信息关联识别">
            <el-checkbox-group v-model="form.relationIdent">
              <el-checkbox v-for="r in relationOpts" :key="r.v" :value="r.v">{{ r.v }} {{ r.t }}</el-checkbox>
            </el-checkbox-group>
            <div class="form-tip">已按库表元数据(G行政监管/H个人隐私/I第三方商密/J其他协议)自动聚合预勾,可手动调整</div>
          </el-form-item>
          <template v-if="needTable2">
            <el-divider content-position="left" style="font-size:12px;color:#909399">表2 涉及第三方权益(结构化)</el-divider>
            <el-form-item label="来源主体名称" prop="sourceSubject"><el-input v-model="form.sourceSubject" placeholder="表2:第三方来源主体" /></el-form-item>
            <el-form-item label="来源权益限制摘要"><el-input v-model="form.sourceLimit" type="textarea" :rows="2" placeholder="表2:BCDEF 来源权益限制说明" /></el-form-item>
            <el-form-item label="信息识别关联主体"><el-input v-model="form.relationSubject" placeholder="表2:GHIJ 信息识别关联主体说明(其他第三方协议 J 必填)" /></el-form-item>
            <el-form-item v-if="form.relationIdent.includes('H')" label="隐私关联主体说明" required>
              <el-input v-model="form.privacyInfo" type="textarea" :rows="2" placeholder="涉个人/家庭隐私(H)必填:隐私关联主体及授权情况说明(如用户入网协议授权范围)" />
            </el-form-item>
            <el-form-item label="权益风险说明"><el-input v-model="form.equityRisk" type="textarea" :rows="2" placeholder="表2:权益风险说明" /></el-form-item>
          </template>
          <el-form-item label="用途说明"><el-input v-model="form.purpose" type="textarea" /></el-form-item>

          <el-divider content-position="left" style="font-size:12px;color:#909399">表级数据确权清单(确权粒度到库表,对齐附录F表2/表3;补录工单归类 M02)</el-divider>
          <el-form-item label="库表清单">
            <div style="width:100%">
              <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
                <el-button size="small" type="primary" :loading="metaLoading" :disabled="!form.assetId" @click="loadTablesFromMeta(false)">
                  <el-icon><Download /></el-icon> 从平台元数据带入库表
                </el-button>
                <span v-if="tableItems.length" class="form-tip">共 {{ tableItems.length }} 张表,暂存申请时一并保存</span>
                <el-button v-if="tableItems.length" size="small" link type="danger" @click="clearTableItems">清空</el-button>
              </div>
              <div class="form-tip">
                选取数据资产卡片后自动带出实例/schema/表代码/表名/密级(平台为源,只读预填);来源判定、来源主体、G–J 可逐表调整。平台未接入时由系统按卡片合成桩清单。
              </div>
            </div>
          </el-form-item>
          <el-empty v-if="!tableItems.length" description="尚无库表 — 选取上方资产卡片即自动带入,或用下方批量导入补充" :image-size="60" />
          <el-table v-else :data="tableItems" border size="small" style="margin-bottom:8px">
            <el-table-column prop="instanceName" label="实例TNS" width="120" />
            <el-table-column prop="schemaName" label="schema" width="100" />
            <el-table-column prop="tableCode" label="表代码" min-width="150" />
            <el-table-column prop="tableName" label="表名称" min-width="120" />
            <el-table-column prop="secretLevel" label="密级" width="84" />
            <el-table-column prop="sourceType" label="来源判定" width="120" />
            <el-table-column prop="sourceSubject" label="来源主体" min-width="120" />
            <el-table-column label="来源" width="96" align="center">
              <template #default="{ row }">
                <el-tag :type="row.sourceChannel === 'META' ? 'success' : 'info'" size="small" effect="plain">
                  {{ row.sourceChannel === 'META' ? '平台元数据' : '手工导入' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="64" align="center">
              <template #default="{ row }"><el-button link type="danger" size="small" @click="removeTableItem(row)">移除</el-button></template>
            </el-table-column>
          </el-table>

          <el-collapse style="margin-top:4px">
            <el-collapse-item name="adv">
              <template #title><span style="font-size:12px;color:#909399">高级:批量导入(平台未接入 / 补充库表 / 离线整理)</span></template>
              <el-input v-model="tableItemText" type="textarea" :rows="4"
                placeholder="每行一张表:实例TNS,schema,表代码,表名称[,密级][,来源判定][,来源主体]&#10;XC_ORA_ZH01,NCLAIMUSER,PRPLNEGOTIATIONS,谈判表,敏感信息,A 自行生产数据,鼎和保险" />
              <div class="form-tip">密级:不涉密/核心商密/普通商密/工作秘密/敏感信息;来源判定:A自行生产/B公开采集/C公共授权/D公共生产/E交易采购/F其他;G–J 识别默认取上方"信息关联识别"勾选</div>
              <el-button size="small" type="primary" plain style="margin-top:6px" @click="parseTableItems">导入并入清单</el-button>
            </el-collapse-item>
          </el-collapse>
        </el-form>
        <el-alert v-if="applyId" type="success" :closable="false" show-icon
          :title="`申请已暂存(${applyNo || applyId})，进入材料上传`" style="max-width:640px;margin-top:8px" />
      </el-card>

      <!-- 步骤2:上传材料(先从平台元数据同步已上传材料,再补全缺口) -->
      <el-card v-show="step === 1" shadow="never">
        <div class="prm-table-note" style="margin-bottom:10px">
          材料优先<b>从数据资产管理平台元数据同步已上传项</b>(标「已同步·平台」免上传),仅需补全平台未覆盖的缺口。补全时"上传原件"(仅 PDF/Word/JPG/PNG,自动格式验证)或"仅登记"占位。
          <el-button size="small" type="primary" plain style="margin-left:12px" :loading="syncing" @click="doSyncPlatform(false)">
            <el-icon><Refresh /></el-icon> 从平台同步已上传材料
          </el-button>
          <el-button size="small" type="warning" plain style="margin-left:8px" :loading="parsing" @click="runParse">
            <el-icon><MagicStick /></el-icon> 智能解析材料(要素抽取/敏感判定/内容查重)
          </el-button>
        </div>
        <!-- 平台同步报告:已同步 N 项(平台已上传)/ 待补全 M 项 -->
        <el-alert v-if="syncReport" :type="(syncReport.stillMissing && syncReport.stillMissing.length) ? 'warning' : 'success'"
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
                <div v-if="row.fileName" style="font-size:12px;color:#67c23a;margin-top:2px" :title="row.fileName">{{ row.fileName }}</div>
              </template>
              <el-tag v-else-if="row.done" type="success" effect="light">已上传</el-tag>
              <el-tag v-else type="warning" effect="light">待补全</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" align="center">
            <template #default="{ row }">
              <span v-if="row.done && row.source === '平台同步'" style="color:#909399;font-size:12px;margin-right:8px">平台原件(免上传)</span>
              <el-upload :auto-upload="false" :show-file-list="false" :on-change="(f) => onUploadFile(row, f)" style="display:inline-block">
                <el-button link type="success">{{ row.done && row.source === '平台同步' ? '改用本地原件' : '上传原件' }}</el-button>
              </el-upload>
              <el-button link type="primary" :disabled="row.done" @click="registerMaterial(row)" style="margin-left:8px">仅登记</el-button>
              <!-- 平台同步材料已落地平台原件字节(fileUrl 存在)才可预览;无字节则不显示,避免点开 404 -->
              <el-button v-if="row.materialId && (row.source !== '平台同步' || row.fileUrl)" link type="warning" style="margin-left:8px" @click="openFilePreview(materialFileUrl(row.materialId), row.fileName)">预览</el-button>
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
        <div style="margin:2px 0 12px;color:#909399;font-size:12px">
          AI 辅助研判(可选,不影响提交):
          <el-button link type="warning" :loading="aiChecking" @click="runAiCheck">AI 决策研判</el-button>
          <el-button link type="warning" :loading="conflictChecking" @click="runConflict">权属冲突识别</el-button>
        </div>
        <AiThinking v-bind="aiThink.state" />
        <!-- AI 决策研判:确权内生 AI(走 confirm-service /ai/decision) -->
        <el-alert v-if="aiResult" :type="aiResult.prediction === '建议通过' ? 'success' : 'warning'" :closable="false" style="margin-bottom:12px">
          <div><b>AI 决策研判:{{ aiResult.prediction }}</b>(综合评分 {{ aiResult.score }})</div>
          <div style="margin-top:4px">AI 预测:{{ aiResult.aiPrediction || '未生成' }}</div>
          <div v-if="aiResult.supplementMaterials && aiResult.supplementMaterials.length" style="margin-top:4px">需补材料:{{ aiResult.supplementMaterials.join('、') }}</div>
          <div v-if="aiResult.pendingConflicts && aiResult.pendingConflicts.length" style="margin-top:4px">待处理冲突:{{ aiResult.pendingConflicts.join('、') }}</div>
          <div style="margin-top:4px;color:#909399">依据:{{ aiResult.basis }}</div>
        </el-alert>
        <!-- 权属冲突识别:确权内生 AI(走 confirm-service /ai/conflict) -->
        <el-alert v-if="conflictResult" :type="conflictResult.hasConflict ? 'error' : 'success'" :closable="false" style="margin-bottom:12px">
          <div><b>权属冲突识别:{{ conflictResult.hasConflict ? '发现冲突' : '未发现冲突' }}</b>(风险:{{ conflictResult.riskLevel }})</div>
          <div v-if="conflictResult.conflicts && conflictResult.conflicts.length" style="margin-top:4px">冲突:{{ conflictResult.conflicts.join('、') }}</div>
          <div v-if="conflictResult.suggestion" style="margin-top:4px;color:#909399">建议:{{ conflictResult.suggestion }}</div>
        </el-alert>
        <!-- 统一待处理清单(单一闭环):规则缺失/不合规 就地上传;AI 存疑/不通过 复核或去修正 -->
        <el-card v-if="pendingItems.length" shadow="never" style="margin-bottom:12px;border:1px solid #fde2e2;background:#fff8f8">
          <div style="font-weight:600;color:#f56c6c;margin-bottom:8px">需处理以下 {{ pendingItems.length }} 项后方可提交(处理完点上方「重新一键校验」)</div>
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
                <span v-else style="color:#909399;font-size:12px">需治理元数据后重校</span>
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
              <span v-else-if="row.source === '平台同步'" style="color:#67c23a" :title="row.fileName">{{ row.fileName }}（平台原件）</span>
              <el-link v-else-if="row.fileName" type="primary" @click="previewMaterial(row)">{{ row.fileName }}（预览/下载）</el-link>
              <span v-else style="color:#bbb">占位/无原件</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 权益归集判定(分子公司共享网公司,《权益内部管理汇总表》说明页规则) -->
        <el-divider content-position="left" style="font-size:12px;color:#909399">权益归集判定(分子公司共享网公司)</el-divider>
        <el-descriptions v-if="consolidation" :column="4" border size="small" class="consol-panel">
          <el-descriptions-item label="命中规则">规则 {{ consolidation.rule }}</el-descriptions-item>
          <el-descriptions-item label="网公司持有权"><el-tag :type="consolidation.holdRight === '有' ? 'success' : 'info'" size="small">{{ consolidation.holdRight }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="网公司使用权"><el-tag :type="consolidation.useRight === '有' ? 'success' : 'info'" size="small">{{ consolidation.useRight }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="网公司经营权"><el-tag :type="consolidation.operateRight === '有' ? 'success' : (consolidation.operateRight === '无' ? 'info' : 'warning')" size="small">{{ consolidation.operateRight }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="共享判定原因" :span="4">{{ consolidation.reason }}</el-descriptions-item>
        </el-descriptions>
        <el-alert v-else type="info" :closable="false" title="暂存申请后自动按管制属性/来源判定/第三方识别给出网公司权益归集判定" style="margin-bottom:8px" />

        <!-- 审批重要节点显式化(评审8.5) -->
        <el-divider content-position="left" style="font-size:12px;color:#909399">提交后审批链(重要节点)</el-divider>
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

    <div class="wz-foot">
      <el-button v-if="step > 0 && step < 3" @click="step--">上一步</el-button>
      <el-button v-if="step === 0" type="primary" :loading="saving" @click="next0">下一步:上传材料</el-button>
      <el-button v-if="step === 1" type="primary" @click="next1">下一步:材料校验</el-button>
      <el-tooltip v-if="step === 2 && !canSubmit" content="请先通过材料校验(全部应交项完整且合规)" placement="top">
        <span><el-button type="primary" disabled>提交审核</el-button></span>
      </el-tooltip>
      <el-button v-else-if="step === 2" type="primary" :loading="submitting" @click="next2">提交审核</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { autofillConfirm, saveConfirmDraft, uploadMaterial, uploadMaterialFile, materialFileUrl, listMaterialByApply, checkMaterial, runMaterialCheck, syncPlatformMaterials, pushMaterialReview, materialExportUrl, submitConfirm, saveAiSnapshot, saveTableItems, getConsolidation, aiMaterialCheck, listMaterialRules, aiParseConfirm, aiDecisionConfirm, aiConflictConfirm } from '@/api/confirm'
import AiThinking from '@/components/AiThinking.vue'
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

// 选卡片→自动带库表清单(平台元数据,未接入时后端桩合成);只读预填,不覆盖已有手工/已存项
async function loadTablesFromMeta(silent = false) {
  if (!form.assetId) { if (!silent) ElMessage.warning('请先选取数据资产卡片'); return }
  metaLoading.value = true
  try {
    const tbls = await listAssetTables(form.assetId, form.assetName) || []
    const existCodes = new Set(tableItems.value.map(t => t.tableCode).filter(Boolean))
    const mapped = tbls.filter(t => !existCodes.has(t.tableCode)).map(t => ({
      instanceName: t.instanceName || '', schemaName: t.schemaName || '',
      tableCode: t.tableCode || '', tableName: t.tableName || '',
      tableComment: t.tableComment || t.tableName || '',
      secretLevel: t.secretLevel || '不涉密',
      sourceType: t.sourceType || defaultSourceLabel(),
      sourceSubject: t.sourceSubject || form.sourceSubject || '',
      existTable: t.existTable !== false, sourceChannel: 'META',
      // 逐表 G–J 取该表自身元数据(AU_TABLE_META_DATA),非系统级勾选
      gFlag: t.gFlag ? '是' : '否', hFlag: t.hFlag ? '是' : '否',
      iFlag: t.iFlag ? '是' : '否', jFlag: t.jFlag ? '是' : '否'
    }))
    // P1:从库表元数据聚合系统级 A–F / G–J(并集预勾,用户可改);来源主体缺省也带一个
    aggregateIdentFromTables(tbls)
    if (mapped.length) {
      tableItems.value = [...tableItems.value, ...mapped]
      if (!silent) ElMessage.success(`已从平台元数据带入 ${mapped.length} 张库表 + 聚合 A–J 来源/关联识别(可改)`)
    } else if (!silent) {
      ElMessage.info('平台元数据未返回新库表,可用下方"批量导入"补充')
    }
  } catch (e) {
    if (!silent) ElMessage.warning('平台元数据暂不可用,可用下方"批量导入"手工补充库表')
  } finally { metaLoading.value = false }
}

// P1:库表元数据聚合到系统级 A–F 来源识别 / G–J 信息关联识别(并集预勾,免逐项手勾;用户仍可改)
function aggregateIdentFromTables(tbls) {
  if (!Array.isArray(tbls) || !tbls.length) return
  const srcSet = new Set(form.sourceIdent)
  const relSet = new Set(form.relationIdent)
  for (const t of tbls) {
    const c = (t.sourceType || '').trim().charAt(0)
    if ('ABCDEF'.includes(c)) srcSet.add(c)
    if (t.gFlag) relSet.add('G')
    if (t.hFlag) relSet.add('H')
    if (t.iFlag) relSet.add('I')
    if (t.jFlag) relSet.add('J')
  }
  form.sourceIdent = [...srcSet].sort()
  form.relationIdent = [...relSet].sort()
  // 涉第三方且来源主体尚空时,带入库表里第一条来源主体
  if (!form.sourceSubject) {
    const s = tbls.find(t => t.sourceSubject)
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
      existTable: true, sourceChannel: 'MANUAL', ...gjFromForm() }
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

// 权益归集判定结果(分子公司共享网公司)
const consolidation = ref(null)
async function loadConsolidation() {
  if (!applyId.value) return
  try { consolidation.value = await getConsolidation(applyId.value) } catch (e) { consolidation.value = null }
}

const form = reactive({
  assetId: '', assetName: '', systemName: '', rightTypes: [], rightHolder: '', respDept: '',
  systemOwner: '', contactInfo: '', assetSecretLevel: '', region: '',
  registerType: '初始确权', applyMode: '常规', regulated: '非管制',
  purpose: '', thirdPartyInfo: '', sourceSubject: '', sourceLimit: '', relationSubject: '', equityRisk: '',
  privacyInfo: '', sourceIdent: [], relationIdent: []
})
const rules = {
  assetId: [{ required: true, message: '请输入关联资产ID', trigger: 'blur' }],
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  rightTypes: [{ required: true, type: 'array', min: 1, message: '请至少选择一种权属类型', trigger: 'change' }]
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

onMounted(() => {
  loadMaterialRules()
  if (!route.query.reopen) return
  try {
    const o = JSON.parse(sessionStorage.getItem('prm-reopen') || '{}')
    if (o.domain === '确权' && o.raw) {
      const r = o.raw
      Object.assign(form, {
        assetId: r.assetId || '', assetName: r.assetName || '', systemName: r.systemName || '', rightHolder: r.rightHolder || '',
        respDept: r.respDept || '', systemOwner: r.systemOwner || '', contactInfo: r.contactInfo || '',
        registerType: r.registerType || '初始确权', regulated: r.regulated || '非管制',
        purpose: r.purpose || '', sourceSubject: r.sourceSubject || '', sourceLimit: r.sourceLimit || '',
        relationSubject: r.relationSubject || '', equityRisk: r.equityRisk || '', privacyInfo: r.privacyInfo || '',
        rightTypes: r.rightType ? String(r.rightType).split(/[、,，]/).map(s => s.trim()).filter(Boolean) : [],
        // 还原 A–J 来源/关联识别勾选(原串如 "A,B" / "A自行生产数据"),否则重提时勾选全丢、next0 被拦
        sourceIdent: parseIdentCodes(r.sourceIdentification, ['A', 'B', 'C', 'D', 'E', 'F']),
        relationIdent: parseIdentCodes(r.relationIdentification, ['G', 'H', 'I', 'J']),
      })
      ElMessage.warning('已带入被驳回原单内容,请修改后重新提交(将作为新申请)')
    }
  } catch (e) { /* ignore */ }
  sessionStorage.removeItem('prm-reopen')
})
const needTable2 = computed(() =>
  form.sourceIdent.some(c => ['B', 'C', 'D', 'E', 'F'].includes(c)) ||
  form.relationIdent.some(c => ['G', 'H', 'I', 'J'].includes(c)))

// 关联数据资产卡片:按 名称/编码/系统 搜索平台卡片并选取(平台为源,台账兜底);存ID、带出名,不手填
import { searchAssetCards, listAssetTables } from '@/api/assetCard'
const assetOpts = ref([])
const assetSearching = ref(false)
const pickedCard = ref(null)
async function searchAssets(kw) {
  if (!kw) { assetOpts.value = []; return }
  assetSearching.value = true
  try {
    assetOpts.value = (await searchAssetCards(kw, 10)) || []
  } finally { assetSearching.value = false }
}
function onAssetPicked(id) {
  const hit = assetOpts.value.find(a => a.assetId === id)
  pickedCard.value = hit || null
  if (hit) form.assetName = hit.assetName || hit.assetId
  if (id) onAutofill(true)
}

// 一键填充示例(测试/演示):对齐 test/确权申请 手册 AST-001 全套数据
function fillDemo() {
  Object.assign(form, {
    assetId: 'AST-001', assetName: '客户用电信息表',
    rightTypes: ['数据资源持有权', '数据加工使用权'],
    rightHolder: '广东电网有限责任公司', respDept: '数字化部',
    systemOwner: '张工', contactInfo: '020-88886666',
    registerType: '初始确权', applyMode: '常规', regulated: '管制业务',
    sourceIdent: ['A'], relationIdent: ['G', 'H'],
    sourceSubject: '用电客户', sourceLimit: '涉个人信息字段对外提供须脱敏并经客户授权',
    relationSubject: '国家能源局南方监管局;用电客户', equityRisk: '未经授权对外提供个人信息存在合规风险',
    privacyInfo: '用电客户个人信息,依据用户入网协议第X条已取得对外提供授权,范围限定于结算与征信场景',
    purpose: '营销域购售电数据确权(示例)'
  })
  // autofill 取质量评分 + 自动带库表清单(AST-001 → 客户用电信息表);桩会回写申报主体为网公司,完成后恢复示例主体(分省申报口径)
  onAutofill(true).then(() => {
    form.rightHolder = '广东电网有限责任公司'
    form.respDept = '数字化部'
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
  await formRef.value.validate()
  // 申请要素逐维必填(与后端 validateRegistration 同源,前移到填报,杜绝"材料全过却提交被拒")
  if (!form.sourceIdent.length) { ElMessage.warning('请至少选择一种数据来源方式(A–F)'); return }
  if (form.sourceIdent.some(c => ['B', 'C', 'D', 'E', 'F'].includes(c)) && !form.sourceSubject) {
    ElMessage.warning('数据来源涉公开采集/受让/委托/交易等(B–F),须填写来源主体名称'); return
  }
  if (form.relationIdent.includes('H') && !form.privacyInfo) {
    ElMessage.warning('涉及用户个人/家庭隐私(H),须填写隐私关联主体说明'); return
  }
  if (form.relationIdent.includes('J') && !form.relationSubject) {
    ElMessage.warning('存在其他数据权益约束协议(J),须填写关联主体说明'); return
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
      involvesThirdParty: needTable2.value,
      reConfirm: form.registerType === '确权变更',
      thirdPartyInfo: needTable2.value ? `来源主体:${form.sourceSubject}; 限制:${form.sourceLimit}; 关联主体:${form.relationSubject}; 风险:${form.equityRisk}` : ''
    }
    applyId.value = await saveConfirmDraft(payload)
    // 表级清单全量同步(后端按 applyId 删后插);剥离前端态字段(sourceChannel/existTable),避免后端未知属性
    if (tableItems.value.length) {
      const items = tableItems.value.map(({ sourceChannel, existTable, ...rest }) => rest)
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
  checklist.value = materialRules.value.filter(hit).map((r, i) => ({
    code: r.triggerCode || (r.triggerType === 'TABLE2' ? '表2' : '核心'),
    name: r.materialName,
    m: r.triggerLabel || r.evidenceType || '材料',
    required: r.required, detail: r.detail,
    id: 'ck' + i, done: false
  }))
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
  Object.assign(form, { assetId: '', assetName: '', systemName: '', rightTypes: [], rightHolder: '', respDept: '', systemOwner: '', contactInfo: '', assetSecretLevel: '', region: '', registerType: '初始确权', applyMode: '常规', purpose: '', thirdPartyInfo: '', sourceSubject: '', sourceLimit: '', relationSubject: '', equityRisk: '', privacyInfo: '', sourceIdent: [], relationIdent: [] })
  tableItems.value = []; tableItemText.value = ''
}
</script>

<style scoped>
.wz-steps { max-width: 900px; margin: 8px auto 20px; }
.wz-body { min-height: 320px; }
.wz-foot { margin-top: 18px; display: flex; gap: 12px; justify-content: center; }
.form-tip { font-size: 12px; color: #909399; line-height: 1.6; }
.approve-chain { max-width: 880px; margin: 8px auto 0; }
</style>
