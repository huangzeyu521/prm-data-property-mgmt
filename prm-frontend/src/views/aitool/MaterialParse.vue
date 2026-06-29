<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-table-card">
      <div style="margin-bottom:12px">
        <el-button type="primary" @click="dlg = true">上传材料</el-button>
        <el-button @click="onAggregate">归集视图</el-button>
        <el-button @click="onAccuracy">准确度评测</el-button>
        <el-button @click="onRecords">解析记录档</el-button>
        <el-button @click="onTemplates">资料模板库</el-button>
        <el-button @click="onConfigs">解析配置</el-button>
        <el-button @click="onOps">运行支撑</el-button>
        <span class="prm-table-note" style="margin-left:12px">支持 Excel/Word/PDF/扫描件/图片;OCR+版面分析(印章/表格/标题/页类型)、多粒度切片(页/段/单元格)、按数据表归集、重复检测、要素抽取与表单比对。</span>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="fileName" label="文件" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.fileName }}
            <el-tag v-if="row.ocrUsed === 1" size="small" type="warning" effect="plain" style="margin-left:4px">OCR</el-tag>
            <el-tag v-if="row.duplicateOf" size="small" type="danger" effect="plain" style="margin-left:4px">重复</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="类型" width="72" align="center" />
        <el-table-column prop="category" label="资料类型" width="100" align="center">
          <template #default="{ row }"><el-tag v-if="row.category" size="small" effect="plain">{{ dataTypeLabel(row.category) }}</el-tag><span v-else>—</span></template>
        </el-table-column>
        <el-table-column prop="sizeKb" label="大小" width="92" align="right"><template #default="{ row }">{{ fmtSize(row.sizeKb) }}</template></el-table-column>
        <el-table-column prop="batchNo" label="批次" width="120" show-overflow-tooltip />
        <el-table-column prop="fileHash" label="文档哈希(SM3)" width="150">
          <template #default="{ row }"><code class="hash">{{ row.fileHash ? row.fileHash.slice(0,12)+'…' : '-' }}</code></template>
        </el-table-column>
        <el-table-column prop="parseStatus" label="解析状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="stTag(row.parseStatus)">{{ row.parseStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column label="解析进度" width="180" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0"
              :status="row.parseStatus==='成功'?'success':(row.parseStatus==='失败'?'exception':undefined)" />
            <div v-if="row.parseStatus==='解析中'" style="font-size:11px;color:var(--prm-color-text-weak);margin-top:2px">{{ stageText(row.progress) }}</div>
            <el-tooltip v-else-if="row.parseStatus==='失败' && row.failReason" :content="row.failReason" placement="top">
              <div style="font-size:11px;color:var(--prm-color-danger);margin-top:2px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ row.failReason }}</div>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.parseStatus==='解析中'" @click="onParse(row)">解析</el-button>
            <el-button link type="primary" @click="onBatchParse(row)">本批解析</el-button>
            <el-button link type="success" :disabled="row.parseStatus!=='成功'" @click="onView(row)">查看结果</el-button>
            <el-button link type="primary" :disabled="row.parseStatus!=='成功'" @click="onClean(row)">清洗标准化</el-button>
            <el-button link type="success" :disabled="row.parseStatus!=='成功'" @click="onProfile(row)">确权画像</el-button>
            <el-button link type="primary" :disabled="row.parseStatus!=='成功'" @click="onExport(row)">导出Excel</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>

    <el-dialog v-model="dlg" title="上传确权证明材料(真实文件 · 可批量)" width="600px" align-center @closed="resetUpload">
      <el-form label-width="150px">
        <el-form-item label="关联确权申请">
          <el-input v-model="uploadApplyId" placeholder="确权申请ID(用于解析后自动比对,可空)" clearable />
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload ref="uploadRef" drag multiple :auto-upload="false" :limit="MAX_BATCH"
            accept=".pdf,.doc,.docx,.xls,.xlsx,.jpg,.jpeg,.png" :on-exceed="onExceed" :on-change="onFileChange" :on-remove="onFileChange">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此,或<em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 Excel(.xls/.xlsx) / Word(.doc/.docx) / PDF / 扫描件 / JPG / PNG;单文件 1KB–50MB;单次最多 {{ MAX_BATCH }} 个。扫描件/图片解析时自动 OCR。</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="uploading" :disabled="!fileList.length" @click="onUpload">
          上传{{ fileList.length ? `(${fileList.length})` : '' }}
        </el-button>
        <el-button @click="dlg=false">取消</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDlg" title="解析结果 · 要素抽取 / 印章 / 术语 / 表单比对" width="720px" align-center>
      <el-alert v-if="parse" :type="parse.reviewStatus==='自动通过'?'success':'warning'" :closable="false" style="margin-bottom:10px"
        :title="`置信度 ${(parse.confidence*100).toFixed(0)}% · ${parse.reviewStatus || (parse.confidence>=0.95?'自动通过':'需人工复核')}`"
        :description="parse.reviewStatus==='自动通过' ? '抽取置信度达标(≥95%),可自动采用。' : '抽取置信度低于 95%,建议人工复核要素后再采用。'" show-icon />
      <el-descriptions v-if="parse" title="确权要素抽取" :column="2" border size="small">
        <el-descriptions-item label="权利主体">{{ parse.rightSubject }}</el-descriptions-item>
        <el-descriptions-item label="权利客体">{{ parse.rightObject }}</el-descriptions-item>
        <el-descriptions-item label="权利类型">{{ parse.rightType }}</el-descriptions-item>
        <el-descriptions-item label="权利期限">{{ parse.rightTerm }}</el-descriptions-item>
        <el-descriptions-item label="授权范围">{{ parse.authScope }}</el-descriptions-item>
        <el-descriptions-item label="数据来源">{{ parse.dataSource }}</el-descriptions-item>
        <el-descriptions-item label="敏感类型">{{ parse.sensitiveType }}</el-descriptions-item>
        <el-descriptions-item label="印章识别(CV×OCR交叉校验)" :span="2">
          <el-tag :type="sealTag(parse.sealValid)">{{ parse.sealValid }}</el-tag> {{ parse.sealDesc }}
        </el-descriptions-item>
        <el-descriptions-item label="材料可信度">
          <el-tag :type="trustTag(parse.trustLevel)">{{ parse.trustLevel || '—' }}</el-tag>
          <span v-if="parse.trustScore != null" style="margin-left:6px;color:var(--prm-color-text-weak)">{{ parse.trustScore }}/100</span>
        </el-descriptions-item>
        <el-descriptions-item label="复核标记">
          <el-tag :type="parse.reviewStatus==='自动通过'?'success':'warning'">{{ parse.reviewStatus || '—' }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <div style="margin-top:14px;font-weight:600">要素来源定位 · 抽取值在材料中的所在位置（数据来源等来源字段标注定位）</div>
      <el-table :data="terms" border size="small">
        <el-table-column prop="field" label="字段" width="110" />
        <el-table-column prop="value" label="抽取值" width="140" show-overflow-tooltip />
        <el-table-column prop="sourceLocation" label="来源（材料中所在位置）" show-overflow-tooltip />
        <el-table-column label="规范" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.standard?'success':'warning'">{{ row.standard?'标准':'建议修正' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="人工确认" width="120" align="center">
          <template #default="{ row }">
            <el-button v-if="!row.standard && row.standardTerm && !row.standardTerm.includes('待人工确认')"
              link type="primary" @click="onAdoptTerm(row)">采用标准术语</el-button>
            <span v-else style="color:var(--prm-color-text-disabled)">—</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="layout" style="margin-top:14px;font-weight:600">版面分析 · OCR(印章 / 表格 / 标题 / 页类型 / 分栏)</div>
      <el-descriptions v-if="layout" :column="2" border size="small">
        <el-descriptions-item label="页类型">{{ layout.pageType || '—' }}</el-descriptions-item>
        <el-descriptions-item label="分栏数">{{ layout.columnCount ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ layout.source === 'ocr' ? 'OCR 识别' : '文本层抽取' }}</el-descriptions-item>
        <el-descriptions-item label="表格数">{{ (layout.tables && layout.tables.length) || 0 }}</el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ (layout.titles && layout.titles.join(' / ')) || '—' }}</el-descriptions-item>
        <el-descriptions-item label="印章区域" :span="2">
          <template v-if="layout.seals && layout.seals.length">
            <el-tag v-for="(s,i) in layout.seals" :key="i" size="small" style="margin-right:6px">{{ s.type }}<span v-if="s.location"> · {{ s.location }}</span></el-tag>
          </template>
          <span v-else>未检出印章区域</span>
        </el-descriptions-item>
      </el-descriptions>

      <div style="margin-top:14px;font-weight:600">
        多粒度解析片段 · 统一文档对象切片(页 / 段 / 单元格)
        <el-radio-group v-model="segGran" size="small" style="margin-left:10px" @change="loadSegments">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="PAGE">页</el-radio-button>
          <el-radio-button label="PARAGRAPH">段</el-radio-button>
          <el-radio-button label="CELL">单元格</el-radio-button>
          <el-radio-button label="TITLE">标题</el-radio-button>
        </el-radio-group>
        <span class="prm-table-note" style="margin-left:8px">共 {{ segments.length }} 片</span>
      </div>
      <el-table :data="segments.slice(0, 200)" border size="small" max-height="240">
        <el-table-column prop="granularity" label="粒度" width="100" align="center">
          <template #default="{ row }">{{ granLabel(row.granularity) }}</template>
        </el-table-column>
        <el-table-column label="定位" width="150">
          <template #default="{ row }">
            <span v-if="row.pageNo">第{{ row.pageNo }}页</span>
            <span v-if="row.sheetName">{{ row.sheetName }}</span>
            <span v-if="row.rowIdx">[{{ row.rowIdx }},{{ row.colIdx }}]</span>
            <span v-if="!row.pageNo && !row.sheetName">#{{ row.segIndex }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
      </el-table>

      <div style="margin-top:14px;font-weight:600">与确权申请表单比对 · 自动标注（点击标注定位原始材料对应位置）</div>
      <el-table :data="compares" border size="small">
        <el-table-column prop="field" label="字段" width="110" />
        <el-table-column prop="materialValue" label="材料解析值" />
        <el-table-column prop="formValue" label="表单填写值" />
        <el-table-column label="差异" width="84" align="center">
          <template #default="{ row }"><el-tag :type="diffTag(row.diffType)">{{ row.diffType }}</el-tag></template>
        </el-table-column>
        <el-table-column label="定位" width="120" align="center">
          <template #default="{ row }">
            <el-button v-if="row.sourceOffset != null && row.sourceOffset >= 0" link type="primary" @click="onLocate(row)">定位原文</el-button>
            <span v-else style="color:var(--prm-color-text-disabled)">原文未定位</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- #6 点击标注定位原始材料正文高亮视图 -->
    <el-dialog v-model="locateDlg" :title="`原始材料定位 · ${locateRow?.field || ''} = ${locateRow?.materialValue || ''}`" width="680px" align-center>
      <div v-if="locateParts" class="locate-doc">
        <span>{{ locateParts.before }}</span><mark ref="markRef" class="locate-mark">{{ locateParts.match }}</mark><span>{{ locateParts.after }}</span>
      </div>
      <el-empty v-else :image-size="50" description="原始材料为图片/扫描件,像素坐标定位需 OCR 版面分析(外部待接)" />
    </el-dialog>

    <!-- #4 归集视图:按同一数据表归集关联各类材料 -->
    <el-dialog v-model="aggDlg" title="材料归集 · 按数据表关联(元数据 / 制度 / 授权 / 合同 / 来源 等)" width="760px" align-center>
      <el-empty v-if="!aggGroups.length" description="暂无可归集材料" />
      <div v-for="g in aggGroups" :key="g.dataTableRef" class="agg-group">
        <div class="agg-title">📄 {{ g.dataTableRef }} <span class="prm-table-note">({{ g.materials.length }} 份)</span></div>
        <el-table :data="g.materials" border size="small">
          <el-table-column prop="fileName" label="文件" show-overflow-tooltip />
          <el-table-column prop="category" label="资料类型" width="110" align="center">
            <template #default="{ row }"><el-tag size="small" effect="plain">{{ dataTypeLabel(row.category) || '其他' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="parseStatus" label="状态" width="90" align="center">
            <template #default="{ row }"><el-tag :type="stTag(row.parseStatus)" size="small">{{ row.parseStatus }}</el-tag></template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 3.3 开放对接与运行支撑 -->
    <el-dialog v-model="opsDlg" title="开放对接与运行支撑(工具适配层 / 批量任务 / 统一日志 / 模型配置)" width="880px" align-center>
      <el-tabs v-model="opsTab">
        <el-tab-pane label="工具能力 & 模型配置" name="caps">
          <div style="font-weight:600;margin-bottom:6px">统一工具适配层能力清单(MCP/ACP/CLI/REST 可接入)</div>
          <el-table :data="opsCaps" border size="small">
            <el-table-column prop="name" label="能力" width="110" />
            <el-table-column prop="tool" label="工具键" width="120" />
            <el-table-column prop="desc" label="说明" show-overflow-tooltip />
            <el-table-column prop="model" label="模型" width="140" />
          </el-table>
          <div style="font-weight:600;margin:10px 0 6px">模型/平台配置(OpenAI 兼容 · 内网可配)</div>
          <el-descriptions v-if="opsModel" :column="2" border size="small">
            <el-descriptions-item label="provider">{{ opsModel.provider }}</el-descriptions-item>
            <el-descriptions-item label="OpenAI兼容">{{ opsModel.openaiCompatible ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="baseUrl" :span="2">{{ opsModel.baseUrl }}</el-descriptions-item>
            <el-descriptions-item label="model">{{ opsModel.model }}</el-descriptions-item>
            <el-descriptions-item label="embedModel">{{ opsModel.embedModel }}</el-descriptions-item>
            <el-descriptions-item label="内网切换" :span="2">{{ opsModel.intranetConfigurable }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="批量任务" name="task">
          <el-table :data="opsTasks" border size="small" max-height="360">
            <el-table-column prop="taskName" label="任务" min-width="160" show-overflow-tooltip />
            <el-table-column prop="taskType" label="类型" width="120" />
            <el-table-column label="进度" width="120" align="center"><template #default="{ row }">{{ row.done }}/{{ row.total }}(失败{{ row.failed }})</template></el-table-column>
            <el-table-column prop="status" label="状态" width="90" align="center" />
            <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click="onTaskRun(row)">运行/续跑</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="统一运行日志" name="log">
          <div style="margin-bottom:6px" class="prm-table-note" v-if="opsLogStats">总数 {{ opsLogStats.total }} · 类型 {{ statStr(opsLogStats.byType) }}</div>
          <el-table :data="opsLogs" border size="small" max-height="360">
            <el-table-column prop="logTime" label="时间" width="170" />
            <el-table-column prop="logType" label="类型" width="90" />
            <el-table-column prop="source" label="来源" width="130" show-overflow-tooltip />
            <el-table-column prop="action" label="动作" show-overflow-tooltip />
            <el-table-column prop="model" label="模型" width="120" />
            <el-table-column prop="result" label="结果" width="70" align="center" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 1.4#1 解析记录档 -->
    <el-dialog v-model="recDlg" title="解析记录档 · 解析时间/文档/字段/值/置信度/操作人" width="860px" align-center>
      <div style="margin-bottom:10px">
        <el-input v-model="recQuery.fileName" placeholder="文档名称" clearable style="width:180px" @keyup.enter="loadRecords" />
        <el-input v-model="recQuery.operator" placeholder="操作人" clearable style="width:140px;margin-left:8px" @keyup.enter="loadRecords" />
        <el-button type="primary" size="small" style="margin-left:8px" @click="loadRecords">查询</el-button>
        <el-button size="small" @click="openUrl(recExportUrl())">导出CSV</el-button>
      </div>
      <el-table :data="records" border size="small" max-height="400">
        <el-table-column prop="parseTime" label="解析时间" width="170" />
        <el-table-column prop="fileName" label="文档名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="field" label="提取字段" width="100" />
        <el-table-column prop="fieldValue" label="提取值" min-width="160" show-overflow-tooltip />
        <el-table-column prop="confidence" label="置信度" width="80" align="center">
          <template #default="{ row }">{{ row.confidence != null ? (row.confidence*100).toFixed(0)+'%' : '—' }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="100">
          <template #default="{ row }">{{ row.operatorName || row.operatorId || '—' }}</template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:10px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="recTotal" :current-page="recQuery.current" :page-size="recQuery.size" @current-change="p=>{recQuery.current=p;loadRecords()}" @size-change="s=>{recQuery.size=s;recQuery.current=1;loadRecords()}" />
    </el-dialog>

    <!-- 1.4#2 批量解析进度 -->
    <el-dialog v-model="batchDlg" :title="`批量解析进度 · 批次 ${curBatch}`" width="640px" align-center>
      <div v-if="batchStat" style="margin-bottom:10px">
        <el-tag type="info">共 {{ batchStat.total }}</el-tag>
        <el-tag type="success" style="margin-left:6px">完成 {{ batchStat.done }}</el-tag>
        <el-tag type="warning" style="margin-left:6px">进行 {{ batchStat.running }}</el-tag>
        <el-tag style="margin-left:6px">待解析 {{ batchStat.pending }}</el-tag>
        <el-tag type="danger" style="margin-left:6px">失败 {{ batchStat.failed }}</el-tag>
      </div>
      <el-table :data="batchStat ? batchStat.items : []" border size="small" max-height="320">
        <el-table-column prop="fileName" label="文档" min-width="160" show-overflow-tooltip />
        <el-table-column prop="parseStatus" label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="stTag(row.parseStatus)">{{ row.parseStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column label="进度" width="160">
          <template #default="{ row }"><el-progress :percentage="row.progress||0" /></template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 1.4#3 资料模板库 -->
    <el-dialog v-model="tplDlg" title="资料模板库 · 确权书/授权函/权属证明(在线编辑/版本/下载)" width="860px" align-center>
      <div style="margin-bottom:10px">
        <el-button type="primary" size="small" @click="onTplNew">新建模板</el-button>
      </div>
      <el-table :data="templates" border size="small" max-height="380">
        <el-table-column prop="templateType" label="类型" width="100" />
        <el-table-column prop="templateName" label="名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" width="70" align="center" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="onTplEdit(row)">在线编辑</el-button>
            <el-button link type="success" @click="onTplNewVersion(row)">新版本</el-button>
            <el-button link @click="openUrl(tplDownloadUrl(row.templateId))">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
    <el-dialog v-model="tplEditDlg" :title="tplForm.templateId ? '在线编辑模板' : '新建模板'" width="600px" align-center>
      <el-form :model="tplForm" label-width="80px">
        <el-form-item label="类型">
          <el-select v-model="tplForm.templateType" style="width:100%">
            <el-option label="确权书" value="确权书" /><el-option label="授权函" value="授权函" />
            <el-option label="权属证明" value="权属证明" /><el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="tplForm.templateName" /></el-form-item>
        <el-form-item label="正文"><el-input v-model="tplForm.content" type="textarea" :rows="8" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tplEditDlg=false">取消</el-button>
        <el-button type="primary" @click="onTplSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 1.4#4 解析元数据配置 -->
    <el-dialog v-model="cfgDlg" title="解析元数据配置 · 字段映射/提取逻辑/置信度阈值(管理员)" width="820px" align-center>
      <div style="margin-bottom:10px">
        <el-button type="primary" size="small" @click="onCfgNew">新增配置</el-button>
      </div>
      <el-table :data="configs" border size="small" max-height="380">
        <el-table-column prop="scene" label="场景" width="140" />
        <el-table-column prop="confidenceThreshold" label="置信度阈值" width="110" align="center" />
        <el-table-column prop="fieldMappingJson" label="字段映射规则" show-overflow-tooltip />
        <el-table-column prop="enabled" label="启用" width="70" align="center">
          <template #default="{ row }">{{ row.enabled===1?'是':'否' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button link type="primary" @click="onCfgEdit(row)">编辑</el-button>
            <el-button link type="danger" :disabled="row.scene==='default'" @click="onCfgDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
    <el-dialog v-model="cfgEditDlg" :title="cfgForm.configId ? '编辑解析配置' : '新增解析配置'" width="560px" align-center>
      <el-form :model="cfgForm" label-width="110px">
        <el-form-item label="场景"><el-input v-model="cfgForm.scene" :disabled="cfgForm.scene==='default'" /></el-form-item>
        <el-form-item label="置信度阈值"><el-input-number v-model="cfgForm.confidenceThreshold" :min="0" :max="1" :step="0.01" /></el-form-item>
        <el-form-item label="字段映射JSON"><el-input v-model="cfgForm.fieldMappingJson" type="textarea" :rows="3" placeholder='{"原始字段名":"模板字段键"}' /></el-form-item>
        <el-form-item label="提取逻辑JSON"><el-input v-model="cfgForm.extractLogicJson" type="textarea" maxlength="500" show-word-limit :rows="2" placeholder='{"enableModel":true}' /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="cfgForm.enabledBool" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cfgEditDlg=false">取消</el-button>
        <el-button type="primary" @click="onCfgSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 1.3 确权要素识别与特征抽取:确权画像 -->
    <el-dialog v-model="profileDlg" title="确权画像 · 要素识别与特征抽取(来源/主体/特征/约束)" width="820px" align-center>
      <div style="margin-bottom:10px">
        <el-switch v-model="profileUseModel" active-text="规则+模型" inactive-text="仅规则" />
        <el-button type="primary" size="small" :loading="profiling" style="margin-left:12px" @click="runProfile">重新抽取</el-button>
      </div>
      <template v-if="profile">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="数据来源方式">
            {{ profile.profile.sourceMethod }} <el-tag size="small" effect="plain">{{ profile.profile.sourceMethodBy }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="层级">{{ profile.profile.level }}</el-descriptions-item>
          <el-descriptions-item label="数据特征" :span="2">
            <el-tag v-for="f in featureList" :key="f" size="small" type="warning" style="margin-right:6px">{{ f }}</el-tag>
            <span v-if="!featureList.length">—</span>
          </el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:12px;font-weight:600">关键主体(来源/授权/使用/加工/共享对象)</div>
        <el-table :data="profile.subjects" border size="small">
          <el-table-column prop="subjectRole" label="角色" width="120" />
          <el-table-column prop="subjectName" label="主体名称" show-overflow-tooltip />
          <el-table-column prop="method" label="方式" width="80" align="center">
            <template #default="{ row }"><el-tag size="small" :type="row.method==='模型'?'warning':'info'">{{ row.method }}</el-tag></template>
          </el-table-column>
        </el-table>
        <div style="margin-top:12px;font-weight:600">约束信息(授权范围/使用边界/共享限制/保留期限/脱敏要求)</div>
        <el-table :data="profile.constraints" border size="small">
          <el-table-column prop="constraintType" label="约束类型" width="120" />
          <el-table-column prop="constraintValue" label="约束内容" show-overflow-tooltip />
          <el-table-column prop="method" label="方式" width="80" align="center">
            <template #default="{ row }"><el-tag size="small" :type="row.method==='模型'?'warning':'info'">{{ row.method }}</el-tag></template>
          </el-table-column>
        </el-table>
        <div style="margin-top:12px;font-weight:600">下游输入 · 分类分级 / 法律校验 / 授权判断</div>
        <el-descriptions v-if="downstream" :column="1" border size="small">
          <el-descriptions-item label="分类分级建议">{{ downstream.classificationGrade }}</el-descriptions-item>
          <el-descriptions-item label="法律校验要点">{{ (downstream.legalCheckPoints||[]).join('；') }}</el-descriptions-item>
          <el-descriptions-item label="授权判断依据">来源方式：{{ downstream.authBasis?.sourceMethod }} · 授权范围：{{ downstream.authBasis?.authScope || '—' }} · 共享限制：{{ downstream.authBasis?.shareLimit || '—' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- 1.2 数据清洗与标准化 -->
    <el-dialog v-model="cleanDlg" title="数据清洗与标准化 · 审核底表 / 待补正 / 清洗日志" width="860px" align-center>
      <div style="margin-bottom:10px">
        <el-switch v-model="cleanUseModel" active-text="规则+模型混合" inactive-text="仅规则" />
        <el-button type="primary" size="small" :loading="cleaning" style="margin-left:12px" @click="runClean">运行清洗</el-button>
        <el-upload :show-file-list="false" :multiple="true" :http-request="onTplUpload"
          accept=".xlsx,.xls,.docx,.csv,.txt" style="display:inline-block;margin-left:12px">
          <el-button size="small" type="success" :loading="tplUploading">上传结构化模板</el-button>
        </el-upload>
        <el-button size="small" :disabled="!tplCompareRows.length" style="margin-left:8px"
          @click="onDownloadTplCompare">下载对比结果</el-button>
        <span v-if="cleanStats" class="prm-table-note" style="margin-left:12px">
          共 {{ cleanStats.fields }} 字段 · 正常 {{ cleanStats.ok }} · 缺失 {{ cleanStats.missing }} · 冲突 {{ cleanStats.conflict }} · 异常 {{ cleanStats.abnormal }} · 重复 {{ cleanStats.duplicate }}
        </span>
      </div>
      <el-tabs v-model="cleanTab">
        <el-tab-pane label="统一审核底表" name="base">
          <el-table :data="cleanBase" border size="small" max-height="380">
            <el-table-column prop="rowNo" label="行" width="50" align="center" />
            <el-table-column prop="fieldLabel" label="字段" width="110" />
            <el-table-column prop="rawValue" label="原始值" show-overflow-tooltip />
            <el-table-column prop="cleanValue" label="清洗后" show-overflow-tooltip />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }"><el-tag size="small" :type="cleanStTag(row.status)">{{ row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="issue" label="问题" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`待补正清单 (${cleanPending.length})`" name="pending">
          <el-table :data="cleanPending" border size="small" max-height="380">
            <el-table-column prop="rowNo" label="行" width="50" align="center" />
            <el-table-column prop="fieldLabel" label="字段" width="110" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }"><el-tag size="small" :type="cleanStTag(row.status)">{{ row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="issue" label="问题" show-overflow-tooltip />
            <el-table-column prop="suggestion" label="待补正建议" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="清洗日志" name="log">
          <el-table :data="cleanLogs" border size="small" max-height="380">
            <el-table-column prop="field" label="字段" width="110" />
            <el-table-column prop="originalValue" label="原始值" show-overflow-tooltip />
            <el-table-column prop="rule" label="清洗规则" show-overflow-tooltip />
            <el-table-column prop="cleanedValue" label="转换结果" show-overflow-tooltip />
            <el-table-column label="方式" width="90" align="center">
              <template #default="{ row }"><el-tag size="small" :type="row.method.includes('模型')?'warning':'info'">{{ row.method }}</el-tag></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`对比日志 (${tplCompareRows.length})`" name="tpl">
          <div class="prm-table-note" style="margin-bottom:6px">
            上传结构化模板后,模板字段与材料抽取内容自动关联审核;支持多模板累积,可下载对比结果。
          </div>
          <el-empty v-if="!tplCompareRows.length" :image-size="48" description="尚无对比记录,请点击上方“上传结构化模板”" />
          <el-table v-else :data="tplCompareRows" border size="small" max-height="380">
            <el-table-column prop="templateName" label="模板" width="150" show-overflow-tooltip />
            <el-table-column prop="tplField" label="模板字段" width="110" show-overflow-tooltip />
            <el-table-column prop="tplValue" label="模板值" show-overflow-tooltip />
            <el-table-column prop="materialValue" label="材料抽取值" show-overflow-tooltip />
            <el-table-column label="一致性" width="84" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.consistency==='一致'?'success':(row.consistency==='不一致'?'danger':'info')">{{ row.consistency }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sourceLocation" label="材料中所在位置" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- #7 准确度评测 -->
    <el-dialog v-model="accDlg" title="材料智能解析 · 准确度评测(标注样本逐字段比对)" width="560px" align-center>
      <el-result v-if="acc" :icon="acc.pass ? 'success' : 'warning'"
        :title="`整体准确率 ${(acc.overall*100).toFixed(1)}%`"
        :sub-title="`阈值 ${(acc.threshold*100).toFixed(0)}% · ${acc.pass ? '达标' : '未达标'} · 样本 ${acc.sampleCount} 份 / 字段 ${acc.correctFields}/${acc.totalFields} 命中`" />
      <el-table v-if="acc" :data="acc.perField" border size="small">
        <el-table-column prop="field" label="字段" />
        <el-table-column label="命中/总数" width="120" align="center"><template #default="{ row }">{{ row.correct }}/{{ row.total }}</template></el-table-column>
        <el-table-column label="准确率" width="100" align="center"><template #default="{ row }">{{ (row.rate*100).toFixed(0) }}%</template></el-table-column>
      </el-table>
      <div v-if="acc" class="prm-table-note" style="margin-top:8px">{{ acc.note }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { pageAitMaterial, uploadAitMaterialFile, uploadAitMaterialBatch, parseAitMaterial, getAitParse, aitTermCheck, confirmAitTerm, aitCompares, aitProgress, aitParseExportUrl, aitSegments, aitAggregate, aitAccuracy, aitClean, aitCleanPending, aitCleanLog, aitExtractElements, aitProfile,
  aitRecordPage, aitRecordExportUrl, aitBatchParse, aitBatchProgress, aitTemplatePage, aitTemplateCreate, aitTemplateUpdate, aitTemplateNewVersion, aitTemplateDownloadUrl, aitParseConfigList, aitParseConfigSave, aitParseConfigDelete,
  aitOpsCapabilities, aitOpsModelConfig, aitOpsTaskPage, aitOpsTaskRun, aitOpsRunLogPage, aitOpsRunLogStats,
  aitTplCompareUpload, aitTplCompareLog, aitTplCompareExportUrl } from '@/api/aitool'
import { materialParsePhaseText } from '@/lib/aiPhases'

const MAX_BATCH = 50
const MIN_BYTES = 1024
const MAX_BYTES = 50 * 1024 * 1024
const ALLOWED_EXT = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'jpg', 'jpeg', 'png']

const q = reactive({ current: 1, size: 10 })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const dlg = ref(false)
const uploadRef = ref(); const fileList = ref([]); const uploadApplyId = ref(''); const uploadAssetId = ref(''); const uploading = ref(false)
const viewDlg = ref(false); const parse = ref(null); const terms = ref([]); const compares = ref([]); const curMaterialId = ref('')
const layout = ref(null); const segments = ref([]); const segGran = ref('')
const aggDlg = ref(false); const aggGroups = ref([])
const accDlg = ref(false); const acc = ref(null)
const cleanDlg = ref(false); const cleaning = ref(false); const cleanUseModel = ref(true); const cleanTab = ref('base')
const cleanBase = ref([]); const cleanPending = ref([]); const cleanLogs = ref([]); const cleanStats = ref(null)
const tplCompareRows = ref([]); const tplUploading = ref(false)
const profileDlg = ref(false); const profiling = ref(false); const profileUseModel = ref(true); const profile = ref(null)
// 1.4 材料解析与管理
const recDlg = ref(false); const records = ref([]); const recTotal = ref(0); const recQuery = reactive({ current: 1, size: 10, fileName: '', operator: '' })
const batchDlg = ref(false); const curBatch = ref(''); const batchStat = ref(null); let batchTimer = null
const tplDlg = ref(false); const templates = ref([]); const tplEditDlg = ref(false); const tplForm = reactive({ templateId: '', templateType: '确权书', templateName: '', content: '' }); let tplVersionMode = false
const cfgDlg = ref(false); const configs = ref([]); const cfgEditDlg = ref(false); const cfgForm = reactive({ configId: '', scene: '', confidenceThreshold: 0.95, fieldMappingJson: '', extractLogicJson: '', enabledBool: true })
function openUrl(u) { window.open(u, '_blank') }
function statStr(m) { return m ? Object.entries(m).map(([k, v]) => `${k}:${v}`).join(' ') : '' }
// 3.3 运行支撑
const opsDlg = ref(false); const opsTab = ref('caps')
const opsCaps = ref([]); const opsModel = ref(null); const opsTasks = ref([]); const opsLogs = ref([]); const opsLogStats = ref(null)
async function onOps() {
  opsDlg.value = true
  opsCaps.value = await aitOpsCapabilities() || []
  opsModel.value = await aitOpsModelConfig()
  opsTasks.value = (await aitOpsTaskPage({ current: 1, size: 50 })).records || []
  opsLogStats.value = await aitOpsRunLogStats()
  opsLogs.value = (await aitOpsRunLogPage({ current: 1, size: 100 })).records || []
}
async function onTaskRun(row) {
  await aitOpsTaskRun(row.taskId)
  ElMessage.success('任务已运行/续跑')
  opsTasks.value = (await aitOpsTaskPage({ current: 1, size: 50 })).records || []
}
const featureList = computed(() => { const f = profile.value?.profile?.dataFeatures; return f ? f.split('、').filter(Boolean) : [] })
const downstream = computed(() => { try { return JSON.parse(profile.value?.profile?.elementsJson || '{}').downstream || null } catch { return null } })
const locateDlg = ref(false); const locateRow = ref(null); const locateContent = ref(''); const markRef = ref()
const locateParts = computed(() => {
  const c = locateContent.value, row = locateRow.value
  if (!row || row.sourceOffset == null || row.sourceOffset < 0 || !c) return null
  const off = row.sourceOffset, len = (row.materialValue || '').trim().length
  return { before: c.slice(0, off), match: c.slice(off, off + len), after: c.slice(off + len) }
})

function stTag(s) { return { 成功: 'success', 失败: 'danger', 解析中: 'warning', 待解析: 'info' }[s] || 'info' }
function diffTag(d) { return { 一致: 'success', 不一致: 'danger', 缺失: 'warning', 表单未含此项: 'info' }[d] || 'info' }
function sealTag(s) { return { 有效: 'success', 可疑: 'warning', 未检出: 'info' }[s] || 'info' }
function trustTag(t) { return { 可信: 'success', 存疑: 'warning', 不可信: 'danger' }[t] || 'info' }
function fmtSize(kb) { if (!kb) return '-'; return kb >= 1024 ? (kb / 1024).toFixed(1) + ' MB' : kb + ' KB' }
// 资料类型(CEC_DATA_TYPE)编码→中文展示;兼容历史中文存量(非编码值原样显示)
const DATA_TYPE_LABEL = { '01': '元数据', '02': '制度附件', '03': '授权材料', '04': '合同材料', '05': '来源说明', '06': '确权证明', '07': '其他' }
function dataTypeLabel(v) { return v == null ? v : (DATA_TYPE_LABEL[v] || v) }
function stageText(p) {
  return materialParsePhaseText(p || 0)
}

async function load() {
  loading.value = true
  try { const r = await pageAitMaterial({ ...q }); rows.value = r.records || []; total.value = r.total || 0 }
  finally { loading.value = false }
}

function onFileChange(_file, files) { fileList.value = files }
function onExceed() { ElMessage.warning(`单次最多上传 ${MAX_BATCH} 个文件`) }
function resetUpload() { fileList.value = []; uploadApplyId.value = ''; uploadRef.value?.clearFiles?.() }

// 客户端前置校验:格式 + 大小(后端仍强校验,双保险)
function precheck(raw) {
  const ext = (raw.name.split('.').pop() || '').toLowerCase()
  if (!ALLOWED_EXT.includes(ext)) return `${raw.name}:不支持的格式 .${ext}(仅 Excel/Word/PDF/JPG/PNG)`
  if (raw.size < MIN_BYTES) return `${raw.name}:文件过小(${raw.size}B),不低于 1KB`
  if (raw.size > MAX_BYTES) return `${raw.name}:文件过大(${(raw.size / 1024 / 1024).toFixed(0)}MB),不超过 50MB`
  return null
}

async function onUpload() {
  const raws = fileList.value.map(f => f.raw).filter(Boolean)
  if (!raws.length) { ElMessage.warning('请先选择文件'); return }
  if (raws.length > MAX_BATCH) { ElMessage.warning(`单次最多 ${MAX_BATCH} 个`); return }
  for (const r of raws) { const err = precheck(r); if (err) { ElMessage.error(err); return } }
  uploading.value = true
  try {
    const fd = new FormData()
    if (uploadApplyId.value) fd.append('applyId', uploadApplyId.value)
    if (uploadAssetId.value) fd.append('assetId', uploadAssetId.value)
    if (raws.length === 1) {
      fd.append('file', raws[0])
      await uploadAitMaterialFile(fd)
    } else {
      raws.forEach(r => fd.append('files', r))
      await uploadAitMaterialBatch(fd)
    }
    ElMessage.success(`已上传 ${raws.length} 个文件(待解析)`)
    dlg.value = false
    load()
  } catch (e) {
    ElMessage.error('上传失败:' + (e?.response?.data?.message || e?.message || '请检查格式与大小'))
  } finally { uploading.value = false }
}
async function onParse(row) {
  await parseAitMaterial(row.materialId)   // 异步触发
  ElMessage.info('已开始解析,进度实时更新中…')
  pollProgress(row.materialId)
}
// #2 轮询解析进度,实时刷新对应行的状态与进度条
function pollProgress(materialId) {
  const timer = setInterval(async () => {
    try {
      const m = await aitProgress(materialId)
      const row = rows.value.find(x => x.materialId === materialId)
      if (row) { row.progress = m.progress; row.parseStatus = m.parseStatus; row.failReason = m.failReason }
      if (m.parseStatus === '成功' || m.parseStatus === '失败') {
        clearInterval(timer)
        ElMessage[m.parseStatus === '成功' ? 'success' : 'error'](
          m.parseStatus === '成功' ? '解析完成' : ('解析失败:' + (m.failReason || '')))
      }
    } catch (e) { clearInterval(timer) }
  }, 350)
}
function onExport(row) {
  window.open(aitParseExportUrl(row.materialId), '_blank')
}
function granLabel(g) { return { PAGE: '页', PARAGRAPH: '段', CELL: '单元格', TABLE: '表格', TITLE: '标题' }[g] || g }

async function onView(row) {
  curMaterialId.value = row.materialId
  parse.value = await getAitParse(row.materialId)
  terms.value = await aitTermCheck(row.materialId)
  compares.value = await aitCompares(row.materialId)
  // 版面分析(从材料 layoutJson 解析)
  layout.value = null
  try { const m = await aitProgress(row.materialId); layout.value = m.layoutJson ? JSON.parse(m.layoutJson) : null } catch { layout.value = null }
  segGran.value = ''
  await loadSegments()
  viewDlg.value = true
}
async function loadSegments() {
  try { segments.value = await aitSegments(curMaterialId.value, segGran.value) || [] }
  catch { segments.value = [] }
}
async function onAggregate() {
  aggGroups.value = await aitAggregate({}) || []
  aggDlg.value = true
}
async function onAccuracy() {
  acc.value = await aitAccuracy()
  accDlg.value = true
}
async function onProfile(row) {
  curMaterialId.value = row.materialId
  profile.value = null
  profileDlg.value = true
  await runProfile()
}
async function runProfile() {
  profiling.value = true
  try {
    profile.value = await aitExtractElements(curMaterialId.value, profileUseModel.value)
    ElMessage.success('确权画像已生成')
  } catch (e) {
    try { profile.value = await aitProfile(curMaterialId.value) } catch { /* ignore */ }
    if (!profile.value) ElMessage.error('画像生成失败:' + (e?.message || ''))
  } finally {
    profiling.value = false
  }
}
// 1.4#1 解析记录档
function recExportUrl() { return aitRecordExportUrl({ fileName: recQuery.fileName, operator: recQuery.operator }) }
async function loadRecords() {
  const r = await aitRecordPage({ current: recQuery.current, size: recQuery.size, fileName: recQuery.fileName, operator: recQuery.operator })
  records.value = r.records || []; recTotal.value = r.total || 0
}
async function onRecords() { recQuery.current = 1; recDlg.value = true; await loadRecords() }
// 1.4#2 批量解析
async function onBatchParse(row) {
  curBatch.value = row.batchNo
  const n = await aitBatchParse(row.batchNo)
  ElMessage.success(`已派发本批 ${n} 个材料解析`)
  batchDlg.value = true
  await pollBatch()
}
async function pollBatch() {
  if (batchTimer) clearInterval(batchTimer)
  const tick = async () => {
    try {
      batchStat.value = await aitBatchProgress(curBatch.value)
      if (batchStat.value && batchStat.value.running === 0 && batchStat.value.pending === 0) { clearInterval(batchTimer); batchTimer = null; load() }
    } catch { clearInterval(batchTimer); batchTimer = null }
  }
  await tick(); batchTimer = setInterval(tick, 500)
}
// 1.4#3 资料模板库
async function onTemplates() { tplDlg.value = true; templates.value = (await aitTemplatePage({ current: 1, size: 100, onlyLatest: true })).records || [] }
function onTplNew() { Object.assign(tplForm, { templateId: '', templateType: '确权书', templateName: '', content: '' }); tplVersionMode = false; tplEditDlg.value = true }
function onTplEdit(row) { Object.assign(tplForm, { templateId: row.templateId, templateType: row.templateType, templateName: row.templateName, content: row.content || '' }); tplVersionMode = false; tplEditDlg.value = true }
function onTplNewVersion(row) { Object.assign(tplForm, { templateId: '', templateType: row.templateType, templateName: row.templateName, content: row.content || '' }); tplVersionMode = true; tplEditDlg.value = true }
async function onTplSave() {
  if (tplVersionMode) await aitTemplateNewVersion({ ...tplForm })
  else if (tplForm.templateId) await aitTemplateUpdate({ ...tplForm })
  else await aitTemplateCreate({ ...tplForm })
  ElMessage.success('已保存'); tplEditDlg.value = false; onTemplates()
}
function tplDownloadUrl(id) { return aitTemplateDownloadUrl(id) }
// 1.4#4 解析元数据配置
async function onConfigs() { cfgDlg.value = true; configs.value = await aitParseConfigList() || [] }
function onCfgNew() { Object.assign(cfgForm, { configId: '', scene: '', confidenceThreshold: 0.95, fieldMappingJson: '{}', extractLogicJson: '{}', enabledBool: true }); cfgEditDlg.value = true }
function onCfgEdit(row) { Object.assign(cfgForm, { configId: row.configId, scene: row.scene, confidenceThreshold: row.confidenceThreshold, fieldMappingJson: row.fieldMappingJson || '', extractLogicJson: row.extractLogicJson || '', enabledBool: row.enabled === 1 }); cfgEditDlg.value = true }
async function onCfgSave() {
  await aitParseConfigSave({ configId: cfgForm.configId || undefined, scene: cfgForm.scene, confidenceThreshold: cfgForm.confidenceThreshold, fieldMappingJson: cfgForm.fieldMappingJson, extractLogicJson: cfgForm.extractLogicJson, enabled: cfgForm.enabledBool ? 1 : 0 })
  ElMessage.success('已保存'); cfgEditDlg.value = false; onConfigs()
}
function onCfgDelete(row) {
  ElMessageBox.confirm(`确认删除配置「${row.scene}」?`, '提示', { type: 'warning' })
    .then(() => aitParseConfigDelete(row.configId)).then(() => { ElMessage.success('已删除'); onConfigs() }).catch(() => {})
}
function cleanStTag(s) { return { 正常: 'success', 缺失: 'warning', 冲突: 'danger', 异常: 'danger', 重复: 'info' }[s] || 'info' }
async function onClean(row) {
  curMaterialId.value = row.materialId
  cleanBase.value = []; cleanPending.value = []; cleanLogs.value = []; cleanStats.value = null
  tplCompareRows.value = []
  cleanTab.value = 'base'
  cleanDlg.value = true
  await runClean()
  try { tplCompareRows.value = await aitTplCompareLog(curMaterialId.value) || [] } catch (e) { /* 忽略 */ }
}
// 1.1.1.1#4 上传结构化模板 → 与材料抽取内容自动关联审核 → 对比日志(支持多模板累积)
async function onTplUpload({ file }) {
  tplUploading.value = true
  try {
    const fd = new FormData(); fd.append('files', file)
    await aitTplCompareUpload(curMaterialId.value, fd)
    tplCompareRows.value = await aitTplCompareLog(curMaterialId.value) || []
    cleanTab.value = 'tpl'
    ElMessage.success(`模板「${file.name}」已对比,共 ${tplCompareRows.value.length} 条`)
  } catch (e) {
    ElMessage.error('模板对比失败:' + (e?.message || ''))
  } finally {
    tplUploading.value = false
  }
}
function onDownloadTplCompare() {
  window.open(aitTplCompareExportUrl(curMaterialId.value), '_blank')
}
async function runClean() {
  cleaning.value = true
  try {
    const r = await aitClean(curMaterialId.value, { useModel: cleanUseModel.value })
    cleanBase.value = r.auditBase || []
    cleanStats.value = r.stats || null
    cleanPending.value = await aitCleanPending(curMaterialId.value) || []
    cleanLogs.value = await aitCleanLog(curMaterialId.value) || []
    ElMessage.success('清洗完成')
  } catch (e) {
    ElMessage.error('清洗失败:' + (e?.message || ''))
  } finally {
    cleaning.value = false
  }
}
// #4 人工确认修改:采用标准术语写回,刷新要素+术语+比对
async function onAdoptTerm(row) {
  await confirmAitTerm(curMaterialId.value, row.field, row.standardTerm)
  ElMessage.success(`已采用标准术语:${row.field} → ${row.standardTerm}`)
  parse.value = await getAitParse(curMaterialId.value)
  terms.value = await aitTermCheck(curMaterialId.value)
  compares.value = await aitCompares(curMaterialId.value)
}
// #6 点击标注 → 取原始材料正文,在对应位置高亮定位
async function onLocate(row) {
  const m = await aitProgress(curMaterialId.value)
  locateContent.value = m?.content || ''
  locateRow.value = row
  locateDlg.value = true
  await nextTick()
  markRef.value?.scrollIntoView?.({ block: 'center', behavior: 'smooth' })
}
// 被业务流程调用时(?applyId=)预填申请上下文并直接打开上传对话框
const route = useRoute()
onMounted(() => {
  load()
  if (route.query.assetId) {
    uploadAssetId.value = String(route.query.assetId)
  }
  if (route.query.applyId) {
    uploadApplyId.value = String(route.query.applyId)
    dlg.value = true
  }
})
</script>

<style scoped>
.hash { font-family: ui-monospace, Consolas, monospace; font-size: 12px; color: #1e87f0; }
.locate-doc { max-height: 360px; overflow: auto; white-space: pre-wrap; word-break: break-all; line-height: 1.9; font-size: 13px; padding: 12px 14px; background: var(--prm-color-bg); border: 1px solid var(--prm-color-border); border-radius: 6px; color: var(--prm-color-text); }
.locate-mark { background: #ffe08a; color: #ad6800; font-weight: 700; padding: 1px 3px; border-radius: 3px; box-shadow: 0 0 0 2px #ffd666; }
</style>
