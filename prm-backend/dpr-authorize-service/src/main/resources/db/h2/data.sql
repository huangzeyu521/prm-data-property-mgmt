-- 演示数据(仅 dev profile 加载;test profile 不加载)
-- 授权域目录项:指引/场景/表单模板/协议模板(8)
INSERT INTO IM_AUTH_CATALOG_ITEM (CEC_ITEM_ID,CEC_CATEGORY,CEC_NAME,CEC_ITEM_TYPE,CEC_VERSION,CEC_STATUS,CEC_PUBLISHER,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('IT-001','GUIDANCE','数据授权操作指引','操作说明','v1','生效中','数字化部',CURRENT_TIMESTAMP,0),
('IT-002','GUIDANCE','数据授权申请单空表模板','申请模板','v1','生效中','数字化部',CURRENT_TIMESTAMP,0),
('IT-003','SCENARIO','电力金融征信','应用场景','v1','生效中','合规管控小组',CURRENT_TIMESTAMP,0),
('IT-004','SCENARIO','精准营销','应用场景','v1','生效中','营销部',CURRENT_TIMESTAMP,0),
('IT-005','SCENARIO','风险防控','应用场景','v1','停用','风控部',CURRENT_TIMESTAMP,0),
('IT-006','FORM_TEMPLATE','一事一议授权申请表','表单模板','v1','生效中','数字化部',CURRENT_TIMESTAMP,0),
('IT-007','AGREEMENT_TEMPLATE','南方电网数据授权运营协议','协议模板','v2','生效中','法规部',CURRENT_TIMESTAMP,0),
('IT-008','AGREEMENT_TEMPLATE','保密承诺函','协议模板','v1','生效中','法规部',CURRENT_TIMESTAMP,0);

-- 授权申请(6,覆盖模式与状态)
INSERT INTO IM_AUTH_APPLY (CEC_APPLY_ID,CEC_APPLY_NO,CEC_AUTH_MODE,CEC_ASSET_ID,CEC_ASSET_NAME,CEC_EQUITY_CARD_ID,CEC_GRANTEE_ORG,CEC_RIGHT_TYPE,CEC_SCENARIO,CEC_SCOPE,CEC_VALID_DATE,CEC_STATUS,CEC_NEED_CONFIDENTIALITY,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AA-001','SQ-0001','一事一议','AST-001','客户用电信息表','EC-PRA-0001','广州供电局','数据加工使用权','电力金融征信','全字段',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'已生效',TRUE,CURRENT_TIMESTAMP,0),
('AA-002','SQ-0002','一事一议','AST-002','台区负荷数据','EC-PRA-0002','深圳供电局','数据加工使用权','负荷预测','约定字段',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'合规审核中',FALSE,CURRENT_TIMESTAMP,0),
('AA-003','SQ-0003','一事一议','AST-003','设备资产台账','EC-PRA-0003','南网科研院','数据加工使用权','设备健康分析','全字段',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'经理审核中',FALSE,CURRENT_TIMESTAMP,0),
('AA-004','SQ-0004','批量','AST-006','充电桩运营数据','EC-PRA-0006','广东电网综能公司','数据产品经营权','对外经营','全字段',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'领导小组审批中',TRUE,CURRENT_TIMESTAMP,0),
('AA-005','SQ-0005','批量','AST-008','线损分析数据','EC-PRA-0008','贵州电网','数据加工使用权','线损治理','约定字段',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'主管审核中',FALSE,CURRENT_TIMESTAMP,0),
('AA-006','SQ-0006','一事一议','AST-004','停电事件记录','EC-PRA-0004','广西电网','数据加工使用权','停电分析','全字段',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'已驳回',FALSE,CURRENT_TIMESTAMP,0);

-- 批量授权清单 表6(3)
INSERT INTO IM_BATCH_AUTH_LIST (CEC_BATCH_LIST_ID,CEC_LIST_NO,CEC_LIST_YEAR,CEC_LIST_STATUS,CEC_ITEM_COUNT,CEC_REMARK,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('BL-001','PLQD-2026-001','2026','批准',12,'2026年度批量授权清单',CURRENT_TIMESTAMP,0),
('BL-002','PLQD-2026-002','2026','申报稿',8,'综能板块批量授权',CURRENT_TIMESTAMP,0),
('BL-003','PLQD-2026-003','2026','草案',5,'科研院数据使用批量授权',CURRENT_TIMESTAMP,0);

-- 授权合规校验(3)
INSERT INTO IM_AUTH_COMPLIANCE (CEC_CHECK_ID,CEC_APPLY_ID,CEC_RISK_LEVEL,CEC_CHECK_RESULT,CEC_PROBLEM_DESC,CEC_CHECK_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AC-001','AA-001','低','通过','材料完整,权限逻辑一致',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),
('AC-002','AA-004','高','预警','经营权对外授权须复核对外开放目录',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),
('AC-003','AA-006','中','不通过','授权范围超出确权边界',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0);

-- 授权运营协议(3,双签/审核/归档不同阶段)
INSERT INTO IM_AUTH_AGREEMENT (CEC_AGREEMENT_ID,CEC_AGREEMENT_NO,CEC_APPLY_ID,CEC_TEMPLATE_ID,CEC_GRANTEE_ORG,CEC_FILE_URL,CEC_SEAL_STATUS,CEC_GRANTOR_SIGNED,CEC_GRANTEE_SIGNED,CEC_REVIEW_STATUS,CEC_ARCHIVE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AG-001','XY-0001','AA-001','IT-007','广州供电局','/files/ag001.ofd','已双签',TRUE,TRUE,'通过','已归档',CURRENT_TIMESTAMP,0),
('AG-002','XY-0004','AA-004','IT-007','广东电网综能公司','/files/ag004.ofd','待对方签章',TRUE,FALSE,'待审核','未归档',CURRENT_TIMESTAMP,0),
('AG-003','XY-0003','AA-003','IT-007','南网科研院','/files/ag003.ofd','待双方签章',FALSE,FALSE,'待审核','未归档',CURRENT_TIMESTAMP,0);

-- 对外经营授权备案(3)
INSERT INTO IM_AUTH_FILING (CEC_FILING_ID,CEC_FILING_NO,CEC_AGREEMENT_ID,CEC_APPLY_ID,CEC_FILING_ORG,CEC_GRANTEE_ORG,CEC_RIGHT_TYPE,CEC_FILING_STATUS,CEC_FILING_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('BA-001','BA-20260601001','AG-001','AA-001','广东电网','广州供电局','数据加工使用权','已备案',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),
('BA-002','BA-20260601002','AG-002','AA-004','广东电网综能公司','广东电网综能公司','数据产品经营权','待备案',NULL,CURRENT_TIMESTAMP,0),
('BA-003','BA-20260601003',NULL,'AA-005','贵州电网','贵州电网','数据加工使用权','待备案',NULL,CURRENT_TIMESTAMP,0);

-- 授权权益证书(4,含1暂停)
INSERT INTO IM_AUTH_CERT (CEC_CERT_ID,CEC_CERT_NO,CEC_APPLY_ID,CEC_ASSET_ID,CEC_GRANTEE_ORG,CEC_RIGHT_TYPE,CEC_SCOPE,CEC_VALID_DATE,CEC_CERT_STATUS,CEC_SUSPEND_REASON,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('ACERT-001','AC-PRA-0001','AA-001','AST-001','广州供电局','数据加工使用权','全字段',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'生效',NULL,CURRENT_TIMESTAMP,0),
('ACERT-002','AC-PRA-0004','AA-004','AST-006','广东电网综能公司','数据产品经营权','全字段',DATEADD('DAY',20,CURRENT_TIMESTAMP),'生效',NULL,CURRENT_TIMESTAMP,0),
('ACERT-003','AC-PRA-0008','AA-005','AST-008','贵州电网','数据加工使用权','约定字段',DATEADD('YEAR',1,CURRENT_TIMESTAMP),'已暂停','监测联动熔断:越权调用',CURRENT_TIMESTAMP,0),
('ACERT-004','AC-PRA-0003','AA-003','AST-003','南网科研院','数据加工使用权','全字段',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'生效',NULL,CURRENT_TIMESTAMP,0);

-- 违规追责(2)
INSERT INTO IM_AUTH_ACCOUNTABILITY (CEC_ACCOUNT_ID,CEC_CERT_ID,CEC_ASSET_ID,CEC_GRANTEE_ORG,CEC_VIOLATION_TYPE,CEC_SOURCE_ALERT_ID,CEC_REASON,CEC_HANDLE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('ACC-001','ACERT-003','AST-008','贵州电网','越权调用','AL-002','监测联动熔断:越权调用','待追责',CURRENT_TIMESTAMP,0),
('ACC-002','ACERT-002','AST-006','广东电网综能公司','超范围','AL-002','疑似超出对外开放目录','已追责',CURRENT_TIMESTAMP,0);

-- 授权权益证书模板(3)
INSERT INTO IM_AUTH_CERT_TEMPLATE (CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_TEMPLATE_VERSION,CEC_CERT_TYPE,CEC_RIGHT_TYPE,CEC_TEMPLATE_CONTENT,CEC_TEMPLATE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('ATPL-001','专项授权-数据加工使用权证书模板','v1','专项授权证书','数据加工使用权','兹证明被授权方 {被授权方} 经专项授权,对 {所属系统} / {数据表}(模式 {模式名称})享有 {权益类型}。授权范围:{授权范围};使用场景及目的:{使用场景及目的};授权期限至 {授权期限}。本证书依审核通过的授权协议与确权信息标准化生成,授权范围不超确权边界。证书编号:{证书编号}。','生效中',CURRENT_TIMESTAMP,0),
('ATPL-002','专项授权-数据产品经营权证书模板','v1','专项授权证书','数据产品经营权','兹证明被授权方 {被授权方} 经专项授权,对 {所属系统} / {数据表}(模式 {模式名称})享有 {权益类型}。授权范围:{授权范围};使用场景及目的:{使用场景及目的};授权期限至 {授权期限}。经营权对外提供数据产品/服务须在公司备案(附录G),授权范围仅限对外开放目录。证书编号:{证书编号}。','生效中',CURRENT_TIMESTAMP,0),
('ATPL-003','批量授权-数据加工使用权证书模板','v1','批量授权证书','数据加工使用权','兹证明被授权方 {被授权方} 经批量授权,对 {所属系统} / {数据表} 享有 {权益类型}(本批授权明细以《数据授权清单》为准)。使用场景及目的:{使用场景及目的};授权期限至 {授权期限}。证书编号:{证书编号}。','生效中',CURRENT_TIMESTAMP,0);

-- 授权指引材料(可研 3.2.2.1.1.3.1.1)
INSERT INTO IM_AUTH_GUIDANCE (CEC_GUIDANCE_ID,CEC_TITLE,CEC_GUIDANCE_TYPE,CEC_VERSION,CEC_PUBLISHER,CEC_PUBLISH_DATE,CEC_FILE_URL,CEC_IS_LATEST,CEC_CONTENT,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AG-000','数据授权申请操作指引','申请步骤','v1','数字化部',DATEADD('MONTH',-2,CURRENT_TIMESTAMP),'/files/授权操作指引_v1.pdf',FALSE,'旧版操作指引',DATEADD('MONTH',-2,CURRENT_TIMESTAMP),0),
('AG-001','数据授权申请操作指引','申请步骤','v2','数字化部',CURRENT_TIMESTAMP,'/files/授权操作指引.pdf',TRUE,'第一步:在授权目录选择数据;第二步:填报授权申请(被授权方/用途/期限);第三步:上传材料并提交合规审核;第四步:签订协议并领取权益证书。',CURRENT_TIMESTAMP,0),
('AG-002','数据对外授权管理政策文件','政策文件','v1','公司总部',CURRENT_TIMESTAMP,'/files/对外授权政策.pdf',TRUE,'依"三权分置",取得数据产品经营权的单位对外提供数据须签协议并向数字化部备案。',CURRENT_TIMESTAMP,0),
('AG-003','数据授权申请流程图','流程图','v1','数字化部',CURRENT_TIMESTAMP,'/files/授权流程图.png',TRUE,'目录选数据 → 填报申请 → 合规审核 → 签订协议 → 发放证书 → 对外备案',CURRENT_TIMESTAMP,0),
('AG-004','数据授权常见问答(FAQ)','常见问答','v1','数字化部',CURRENT_TIMESTAMP,'/files/授权FAQ.pdf',TRUE,'Q:授权与确权区别?A:先确权后授权,授权范围不得超过确权边界。Q:授权期限?A:按协议约定,到期需重新申请。',CURRENT_TIMESTAMP,0);

-- 授权申请表单模板(可研 3.2.2.1.1.3.1.2:独占/共享/委托)
INSERT INTO IM_AUTH_APPLY_TEMPLATE (CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_AUTH_TYPE,CEC_FIELDS_JSON,CEC_FLOW_DESC,CEC_TEMPLATE_VERSION,CEC_TEMPLATE_STATUS,CEC_REMARK,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AAT-001','一事一议(专项)授权申请单模板(表5)','一事一议','[{"name":"granteeOrg","label":"申请主体(被授权方)","type":"文本","required":true,"rule":"从组织树选取"},{"name":"sysName","label":"所属系统","type":"文本","required":true,"rule":"资产卡片带出"},{"name":"schemaName","label":"模式名称","type":"文本","required":false,"rule":"表5"},{"name":"assetName","label":"数据表","type":"文本","required":true,"rule":"库表名"},{"name":"equityCardId","label":"生效权益卡片","type":"文本","required":true,"rule":"先确后授"},{"name":"rightType","label":"授权权益类型","type":"下拉","required":true,"rule":"使用权/经营权;经营权须在对外开放目录"},{"name":"scenario","label":"使用场景及目的","type":"多行文本","required":true,"rule":"附录D §3.4.4"},{"name":"scope","label":"授权范围","type":"多行文本","required":true,"rule":"不得超出确权边界"},{"name":"validTerm","label":"授权时效","type":"下拉","required":true,"rule":"默认两年,不超确权有效期"},{"name":"benefitAllocation","label":"利益分配约定","type":"多行文本","required":false,"rule":"附录D §3.4.4"},{"name":"securityReq","label":"安全保障要求","type":"多行文本","required":false,"rule":"附录D §3.4.4"}]','填报申请 → 合规审核 → 业务审核 → 主管审核 → 经理审核 → 副总审批 → 已生效(签《运营授权协议》·发证)','v1','生效中','专项一事一议:仅限本次特定场景使用',CURRENT_TIMESTAMP,0),
('AAT-002','批量授权清单模板(表6)','批量','[{"name":"listYear","label":"授权年度","type":"文本","required":true,"rule":"按年度"},{"name":"granteeOrg","label":"申请主体(被授权方)","type":"文本","required":true,"rule":"批量共享"},{"name":"rightType","label":"默认权益类型","type":"下拉","required":true,"rule":"资源池过滤键"},{"name":"itemSysName","label":"逐项·所属系统","type":"文本","required":true,"rule":"确权目录带出"},{"name":"itemSchemaName","label":"逐项·模式名称","type":"文本","required":false,"rule":"表6"},{"name":"itemAssetName","label":"逐项·数据表","type":"文本","required":true,"rule":"库表名"},{"name":"itemRightType","label":"逐项·权益类型","type":"下拉","required":true,"rule":""},{"name":"itemEquityCardId","label":"逐项·生效卡片","type":"文本","required":true,"rule":"先确后授"},{"name":"itemThirdParty","label":"逐项·涉第三方","type":"文本","required":false,"rule":"确权带出"},{"name":"itemSensitive","label":"逐项·涉隐私商密","type":"文本","required":false,"rule":"确权带出"},{"name":"itemCrossRegion","label":"逐项·是否跨系统域","type":"下拉","required":false,"rule":"清单系统并集判定"}]','需求归集 → 合规审核 → 主管审核 → 经理审核 → 副总审批 → 领导小组审批 → 已生效(一清单一《运营授权协议》·发证)','v1','生效中','批量按年度;一清单一协议',CURRENT_TIMESTAMP,0);

-- 授权应用场景配置(可研 3.2.2.1.1.3.1.3)
INSERT INTO IM_AUTH_SCENARIO (CEC_SCENARIO_ID,CEC_SCENARIO_NAME,CEC_CATEGORY,CEC_RIGHT_TYPE,CEC_DESCRIPTION,CEC_REASON_TEMPLATE,CEC_SCENARIO_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('SC-001','内部经营分析','内部分析','数据加工使用权','本单位内部经营/运营数据统计与分析,不对外提供','用于本单位内部经营分析,数据不出域、不对外提供,使用范围限于授权部门。','生效中',CURRENT_TIMESTAMP,0),
('SC-002','对外数据服务','对外服务','数据产品经营权','向取得经营权的外部单位提供数据产品/服务','为对外提供数据产品/服务,被授权方仅在约定范围内使用,不得再授权或超范围使用。','生效中',CURRENT_TIMESTAMP,0),
('SC-003','联合建模','联合建模','数据加工使用权','与合作方在隐私计算/可信环境下联合建模','用于与合作方在隐私计算环境联合建模,原始数据不出域,仅交换模型/结果。','生效中',CURRENT_TIMESTAMP,0),
('SC-004','监管报送','监管报送','通用','按行政监管要求向监管机构报送数据','应行政监管要求报送数据,使用范围限于监管报送目的,符合相关法规。','生效中',CURRENT_TIMESTAMP,0),
('SC-005','科研合作','对外服务','数据产品经营权','向高校/科研机构提供脱敏数据用于科研','用于科研合作,提供脱敏数据,仅限约定科研课题使用,到期销毁。','停用',CURRENT_TIMESTAMP,0);

-- 授权协议模板库(可研 3.2.2.1.1.3.3.1)
INSERT INTO IM_AUTH_AGREEMENT_TEMPLATE (CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_AUTH_TYPE,CEC_PURPOSE,CEC_TEMPLATE_CONTENT,CEC_TEMPLATE_VERSION,CEC_TEMPLATE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AGT-001','专项数据授权运营协议(附录D·一事一议)','一事一议','对外服务','《南方电网数据授权运营协议》(附录D)。授权方(甲方)/被授权方(乙方);授权方式:一事一议;授权权益类型。一、数据范围:授权数据表(系统/模式/库表)及字段范围,不得超出确权边界。二、使用场景及目的:仅限约定场景使用,不得超范围、不得再授权。三、授权期限:默认两年,不超确权有效期,到期销毁。四、利益分配:双方约定(免费内部共享/按次计费/收益分成)。五、安全保障:加密传输、最小授权访问控制、操作留痕审计、数据脱敏。六、合规与备案:经营权对外提供须备案(附录G),范围仅限对外开放目录。七、违约责任/争议解决。','v1','生效中',CURRENT_TIMESTAMP,0),
('AGT-002','数据批量授权运营协议(附录D·批量)','批量','对外服务','《南方电网数据授权运营协议》(附录D)。授权方(甲方)/被授权方(乙方);授权方式:批量(一清单一协议)。一、数据范围:以本协议附件《数据授权清单》逐表列明为准。二、使用场景及目的:各授权项按清单约定场景使用,不得超范围、不得再授权。三、授权期限:默认两年,不超确权有效期,到期销毁。四、利益分配:本清单整体约定(免费内部共享/按次计费/收益分成)。五、安全保障:加密传输、最小授权访问控制、操作留痕审计、数据脱敏。六、合规与备案:经营权对外提供须备案(附录G),范围仅限对外开放目录。七、违约责任/争议解决。','v1','生效中',CURRENT_TIMESTAMP,0);
