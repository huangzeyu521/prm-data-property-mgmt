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
('AA-005','SQ-0005','批量','AST-008','线损分析数据','EC-PRA-0008','贵州电网','数据加工使用权','线损治理','约定字段',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'数字化部认定中',FALSE,CURRENT_TIMESTAMP,0),
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
INSERT INTO IM_AUTH_CERT_TEMPLATE (CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_TEMPLATE_VERSION,CEC_CERT_TYPE,CEC_RIGHT_TYPE,CEC_TEMPLATE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('ATPL-001','专项授权-数据加工使用权证书模板','v1','专项授权证书','数据加工使用权','生效中',CURRENT_TIMESTAMP,0),
('ATPL-002','专项授权-数据产品经营权证书模板','v1','专项授权证书','数据产品经营权','生效中',CURRENT_TIMESTAMP,0),
('ATPL-003','批量授权-数据加工使用权证书模板','v1','批量授权证书','数据加工使用权','生效中',CURRENT_TIMESTAMP,0);

-- 授权指引材料(可研 3.2.2.1.1.3.1.1)
INSERT INTO IM_AUTH_GUIDANCE (CEC_GUIDANCE_ID,CEC_TITLE,CEC_GUIDANCE_TYPE,CEC_VERSION,CEC_PUBLISHER,CEC_PUBLISH_DATE,CEC_FILE_URL,CEC_IS_LATEST,CEC_CONTENT,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AG-000','数据授权申请操作指引','申请步骤','v1','数字化部',DATEADD('MONTH',-2,CURRENT_TIMESTAMP),'/files/授权操作指引_v1.pdf',FALSE,'旧版操作指引',DATEADD('MONTH',-2,CURRENT_TIMESTAMP),0),
('AG-001','数据授权申请操作指引','申请步骤','v2','数字化部',CURRENT_TIMESTAMP,'/files/授权操作指引.pdf',TRUE,'第一步:在授权目录选择数据;第二步:填报授权申请(被授权方/用途/期限);第三步:上传材料并提交合规审核;第四步:签订协议并领取权益证书。',CURRENT_TIMESTAMP,0),
('AG-002','数据对外授权管理政策文件','政策文件','v1','公司总部',CURRENT_TIMESTAMP,'/files/对外授权政策.pdf',TRUE,'依"三权分置",取得数据产品经营权的单位对外提供数据须签协议并向数字化部备案。',CURRENT_TIMESTAMP,0),
('AG-003','数据授权申请流程图','流程图','v1','数字化部',CURRENT_TIMESTAMP,'/files/授权流程图.png',TRUE,'目录选数据 → 填报申请 → 合规审核 → 签订协议 → 发放证书 → 对外备案',CURRENT_TIMESTAMP,0),
('AG-004','数据授权常见问答(FAQ)','常见问答','v1','数字化部',CURRENT_TIMESTAMP,'/files/授权FAQ.pdf',TRUE,'Q:授权与确权区别?A:先确权后授权,授权范围不得超过确权边界。Q:授权期限?A:按协议约定,到期需重新申请。',CURRENT_TIMESTAMP,0);

-- 授权申请表单模板(可研 3.2.2.1.1.3.1.2:独占/共享/委托)
INSERT INTO IM_AUTH_APPLY_TEMPLATE (CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_AUTH_TYPE,CEC_FIELDS_JSON,CEC_FLOW_DESC,CEC_TEMPLATE_VERSION,CEC_TEMPLATE_STATUS,CEC_REMARK,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AAT-001','独占授权申请表单模板','独占','[{"name":"grantee","label":"被授权方","type":"文本","required":true,"rule":"非空"},{"name":"scope","label":"授权范围","type":"多行文本","required":true,"rule":"不得超出确权边界"},{"name":"term","label":"独占期限","type":"日期","required":true,"rule":"不超过确权有效期"},{"name":"exclusive","label":"排他性声明","type":"多行文本","required":false,"rule":""}]','填报申请 → 合规审核 → 主管审批 → 签订独占协议 → 发证','v1','生效中','独占授权:同一数据同期仅授一方',CURRENT_TIMESTAMP,0),
('AAT-002','共享授权申请表单模板','共享','[{"name":"grantee","label":"被授权方","type":"文本","required":true,"rule":"非空"},{"name":"scope","label":"共享范围","type":"多行文本","required":true,"rule":"不得超出确权边界"},{"name":"term","label":"共享期限","type":"日期","required":true,"rule":"不超过确权有效期"},{"name":"resharable","label":"再共享限制","type":"下拉","required":true,"rule":"允许/禁止"}]','填报申请 → 合规审核 → 签订共享协议 → 发证','v1','生效中','共享授权:可多方共享使用',CURRENT_TIMESTAMP,0),
('AAT-003','委托授权申请表单模板','委托','[{"name":"consignor","label":"委托方","type":"文本","required":true,"rule":"非空"},{"name":"trustee","label":"受托方","type":"文本","required":true,"rule":"非空"},{"name":"matters","label":"委托事项","type":"多行文本","required":true,"rule":"明确处理目的"},{"name":"term","label":"委托期限","type":"日期","required":true,"rule":""},{"name":"dataReq","label":"数据处理要求","type":"多行文本","required":true,"rule":"符合安全合规"}]','填报申请 → 合规审核 → 签订委托处理协议 → 发证','v1','生效中','委托处理:受托方按约定处理数据',CURRENT_TIMESTAMP,0);
