-- =====================================================================
-- 数据产权管理模块 · 达梦 DM8 测试数据
-- 由 MySQL 种子转换: NOW()→SYSDATE, DATE_ADD→INTERVAL, DATE_SUB→INTERVAL
-- 执行前先跑 02_schema_dm.sql 建表
-- 执行:  disql PRM_DPR/PRM_DPR2024!@localhost:5236 @03_data_dm.sql
-- =====================================================================

-- ============ F-01 产权信息管理 ============

-- 数据资产(8)
INSERT INTO IM_DPM_DATA_ASSET_INFO (CEC_ASSET_ID,CEC_ASSET_NAME,CEC_ASSET_TYPE,CEC_SOURCE_OF_ASSETS,CEC_ASSET_STATUS,CEC_ASSET_OWNER,CEC_SUBSIDIARY_NAME,CEC_SYSTEM_NAME,CEC_SCHEMA_NAME,CEC_SECURITY_LEVEL,CEC_RESP_DEPT,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AST-001','客户用电信息表','结构化','营销系统','在用','广东电网','广东电网','营销2.0','MKT','L3','数字化部',SYSDATE,0),
('AST-002','台区负荷数据','结构化','计量系统','在用','广东电网','广东电网','计量自动化','METER','L2','数字化部',SYSDATE,0),
('AST-003','设备资产台账','结构化','PMS','在用','深圳供电局','深圳供电局','PMS2.0','PMS','L1','设备部',SYSDATE,0),
('AST-004','停电事件记录','结构化','调度系统','在用','深圳供电局','深圳供电局','OMS','OMS','L2','调度中心',SYSDATE,0),
('AST-005','电费账单明细','结构化','营销系统','在用','广西电网','广西电网','营销2.0','MKT','L3','财务部',SYSDATE,0),
('AST-006','充电桩运营数据','结构化','车联网平台','在用','广东电网','广东电网','车联网','EVCS','L2','综能公司',SYSDATE,0),
('AST-007','气象环境数据','结构化','外部接入','在用','云南电网','云南电网','气象平台','WEATHER','L1','调度中心',SYSDATE,0),
('AST-008','线损分析数据','结构化','计量系统','在用','贵州电网','贵州电网','线损系统','LOSS','L2','运维部',SYSDATE,0);

-- 产权档案(8)
INSERT INTO IM_PROPERTY_ARCHIVE (CEC_ARCHIVE_ID,CEC_ASSET_ID,CEC_ASSET_NAME,CEC_RIGHT_TYPE,CEC_RIGHT_SUBJECT,CEC_RIGHT_OBJECT,CEC_ACQUIRE_MODE,CEC_USE_SCOPE,CEC_RESP_DEPT,CEC_VALID_DATE,CEC_CONFIRM_STATUS,CEC_AUTH_STATUS,CEC_EQUITY_CARD_ID,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('ARC-001','AST-001','客户用电信息表','持有权','广东电网','客户用电信息','确权认定','全网','数字化部',SYSDATE + INTERVAL '3' YEAR,'已确权','已授权','EC-PRA-0001',SYSDATE,0),
('ARC-002','AST-002','台区负荷数据','使用权','广东电网','台区负荷','确权认定','省内','数字化部',SYSDATE + INTERVAL '3' YEAR,'已确权','未授权','EC-PRA-0002',SYSDATE,0),
('ARC-003','AST-003','设备资产台账','持有权','深圳供电局','设备台账','确权认定','本单位','设备部',SYSDATE + INTERVAL '2' YEAR,'已确权','未授权','EC-PRA-0003',SYSDATE - INTERVAL '1' MONTH,0),
('ARC-004','AST-004','停电事件记录','使用权','深圳供电局','停电记录','确权认定','省内','调度中心',SYSDATE + 20,'已确权','已授权','EC-PRA-0004',SYSDATE - INTERVAL '2' MONTH,0),
('ARC-005','AST-005','电费账单明细','持有权','广西电网','电费明细','确权认定','本单位','财务部',SYSDATE + INTERVAL '3' YEAR,'待确权','未授权',NULL,SYSDATE - INTERVAL '3' MONTH,0),
('ARC-006','AST-006','充电桩运营数据','经营权','广东电网','充电运营','公司授权','对外开放','综能公司',SYSDATE + INTERVAL '2' YEAR,'已确权','已授权','EC-PRA-0006',SYSDATE - INTERVAL '12' MONTH,0),
('ARC-007','AST-007','气象环境数据','使用权','云南电网','气象数据','交易采购','省内','调度中心',SYSDATE + 10,'待确权','未授权',NULL,SYSDATE - INTERVAL '13' MONTH,0),
('ARC-008','AST-008','线损分析数据','持有权','贵州电网','线损数据','确权认定','本单位','运维部',SYSDATE + INTERVAL '3' YEAR,'已确权','未授权','EC-PRA-0008',SYSDATE - INTERVAL '14' MONTH,0);

-- 产权变更记录(5)
INSERT INTO IM_PROPERTY_CHANGE_RECORD (CEC_CHANGE_ID,CEC_ASSET_ID,CEC_CHANGE_TYPE,CEC_FIELD_NAME,CEC_BEFORE_VALUE,CEC_AFTER_VALUE,CEC_CHANGE_REASON,CEC_SOURCE_FLOW,CEC_SOURCE_TICKET,CEC_OPERATOR_ID,CEC_CHAIN_HASH,CEC_CHANGE_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CHG-001','AST-001','确权','confirmStatus','待确权','已确权','确权终审通过','确权流程','QQ-0001','admin','a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2',SYSDATE,SYSDATE,0),
('CHG-002','AST-001','授权','authStatus','未授权','已授权','授权协议生效','授权流程','SQ-0001','admin','b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3',SYSDATE,SYSDATE,0),
('CHG-003','AST-004','授权','authStatus','未授权','已授权','专项授权生效','授权流程','SQ-0004','admin','c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4',SYSDATE,SYSDATE,0),
('CHG-004','AST-006','确权','rightType','持有权','经营权','经营权确权认定','确权流程','QQ-0006','admin','d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5',SYSDATE,SYSDATE,0),
('CHG-005','AST-002','权益变更','validDate','2027','2028','季度重确权延期','重确权','QQ-0002','admin',NULL,SYSDATE,SYSDATE,0);

-- 监测规则(7,含1条联动熔断 + 申请/审核2条 + 1条草稿可删)
INSERT INTO IM_MONITOR_RULE (CEC_RULE_ID,CEC_RULE_NAME,CEC_RULE_CATEGORY,CEC_MONITOR_TARGET,CEC_THRESHOLD,CEC_PRIORITY,CEC_NOTIFY_TARGET,CEC_NOTIFY_CHANNEL,CEC_RULE_VERSION,CEC_EFFECT_STATUS,CEC_CIRCUIT_BREAK,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('RULE-001','授权到期提醒规则','到期提醒','授权证书','30','重要','合规管控小组','站内信,邮件','v1','生效中',0,SYSDATE,0),
('RULE-002','越权调用熔断规则','调用异常','授权履约','5','紧急','数字化部','站内信,短信,eLink','v1','生效中',1,SYSDATE,0),
('RULE-003','权属变动监测规则','权属变动','权益卡片','1','重要','合规管控小组','站内信','v1','生效中',0,SYSDATE,0),
('RULE-004','材料缺失合规规则','合规','确权材料','0','普通','数字化部','站内信','v1','停用',0,SYSDATE,0),
('RULE-005','确权申请审核积压规则','申请审核','确权申请','7','重要','合规管控小组','站内信,邮件','v1','生效中',0,SYSDATE,0),
('RULE-006','授权申请审核积压规则','申请审核','授权申请','7','重要','数字化部','站内信,邮件','v1','生效中',0,SYSDATE,0),
('RULE-007','调用频次异常监测规则(草稿)','调用异常','授权履约','100','普通','数字化部','站内信','v1','草稿',0,SYSDATE,0);

-- 风险预警(6)
INSERT INTO IM_ALERT_RECORD (CEC_ALERT_ID,CEC_RULE_ID,CEC_SOURCE,CEC_ASSET_ID,CEC_ALERT_LEVEL,CEC_TRIGGER_COND,CEC_ABNORMAL_DESC,CEC_ALERT_TIME,CEC_DISPOSE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AL-001','RULE-001','状态监控','AST-004','重要','授权到期<30天','停电事件记录授权将于20天后到期',SYSDATE,'待处理',SYSDATE,0),
('AL-002','RULE-002','授权履约监测','AST-006','紧急','越权调用','检测到充电桩数据跨域越权访问',SYSDATE,'处理中',SYSDATE,0),
('AL-003','RULE-003','状态监控','AST-002','普通','权属变动','台区负荷数据来源系统变更',SYSDATE,'已关闭',SYSDATE,0),
('AL-004','RULE-001','状态监控','AST-007','紧急','已过期','气象数据确权将于10天内到期',SYSDATE,'待处理',SYSDATE,0),
('AL-005','RULE-004','合规检查','AST-005','重要','材料缺失','电费账单确权缺采购协议',SYSDATE,'处理中',SYSDATE,0),
('AL-006','RULE-002','授权履约监测','AST-001','普通','调用频次异常','客户用电信息调用频次偏高',SYSDATE,'已关闭',SYSDATE,0);

-- 风险预警定向通知(按命中规则的通知对象+通知方式推送责任人,3 未读)
INSERT INTO IM_ALERT_NOTIFICATION (CEC_NOTIFY_ID,CEC_ALERT_ID,CEC_ASSET_ID,CEC_RECIPIENT,CEC_CHANNEL,CEC_TITLE,CEC_CONTENT,CEC_ALERT_LEVEL,CEC_READ_STATUS,CEC_PUSH_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('NT-001','AL-001','AST-004','合规管控小组','站内信,邮件','[重要]授权到期<30天','停电事件记录授权将于20天后到期','重要','未读',SYSDATE,SYSDATE,0),
('NT-002','AL-002','AST-006','数字化部','站内信,短信,eLink','[紧急]越权调用','检测到充电桩数据跨域越权访问','紧急','未读',SYSDATE,SYSDATE,0),
('NT-004','AL-004','AST-007','合规管控小组','站内信,邮件','[紧急]已过期','气象数据确权将于10天内到期','紧急','未读',SYSDATE,SYSDATE,0);

-- 合规检查结果(4)
INSERT INTO IM_COMPLIANCE_RESULT (CEC_CHECK_ID,CEC_ASSET_ID,CEC_RULE_ID,CEC_CHECK_RESULT,CEC_PROBLEM_DESC,CEC_SUGGESTION,CEC_CHECK_TIME,CEC_DISPOSE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CHK-001','AST-005','RULE-004','不通过','缺少采购协议材料','补传采购协议后重新校验',SYSDATE,'待处理',SYSDATE,0),
('CHK-002','AST-001','RULE-004','通过','材料完整','无',SYSDATE,'已关闭',SYSDATE,0),
('CHK-003','AST-004','RULE-001','预警','授权即将到期','启动续签流程',SYSDATE,'处理中',SYSDATE,0),
('CHK-004','AST-006','RULE-002','不通过','经营权授权疑似超出对外开放目录','复核对外开放目录范围',SYSDATE,'待处理',SYSDATE,0);

-- ============ F-02 数据确权管理 ============

-- 确权指引(4,含"操作指引"v1历史版本)
INSERT INTO IM_CONFIRM_GUIDANCE (CEC_GUIDANCE_ID,CEC_TITLE,CEC_GUIDANCE_TYPE,CEC_VERSION,CEC_PUBLISHER,CEC_PUBLISH_DATE,CEC_FILE_URL,CEC_IS_LATEST,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('GD-000','数据确权操作指引','操作说明','v1','数字化部',SYSDATE - INTERVAL '3' MONTH,'/files/确权操作指引_v1.pdf',0,SYSDATE - INTERVAL '3' MONTH,0),
('GD-001','数据确权操作指引','操作说明','v2','数字化部',SYSDATE,'/files/确权操作指引.pdf',1,SYSDATE,0),
('GD-002','数据确权授权业务指导书(附录F)','政策文件','v1','公司总部',SYSDATE,'/files/附录F.pdf',1,SYSDATE,0),
('GD-003','确权材料样例与模板','材料样例','v1','合规管控小组',SYSDATE,'/files/确权材料样例.zip',1,SYSDATE,0);

-- 确权申请(6,覆盖各状态)
INSERT INTO IM_CONFIRM_APPLY (CEC_APPLY_ID,CEC_APPLY_NO,CEC_ASSET_ID,CEC_ASSET_NAME,CEC_RIGHT_TYPE,CEC_PURPOSE,CEC_RIGHT_HOLDER,CEC_RESP_DEPT,CEC_STATUS,CEC_CURRENT_NODE,CEC_INVOLVES_THIRD_PARTY,CEC_THIRD_PARTY_INFO,CEC_SOURCE_IDENT,CEC_RELATION_IDENT,CEC_RE_CONFIRM,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CA-001','QQ-0001','AST-001','客户用电信息表','持有权','营销分析','广东电网','数字化部','已完成',80,1,'涉用户隐私,已取得授权','A','H',0,SYSDATE,0),
('CA-002','QQ-0002','AST-002','台区负荷数据','使用权','负荷预测','广东电网','数字化部','经理终审中',70,0,NULL,'A',NULL,0,SYSDATE,0),
('CA-003','QQ-0003','AST-003','设备资产台账','持有权','资产管理','深圳供电局','设备部','主管复核中',60,0,NULL,'A',NULL,0,SYSDATE,0),
('CA-004','QQ-0004','AST-005','电费账单明细','持有权','财务核算','广西电网','财务部','合规审核中',50,0,NULL,'E',NULL,0,SYSDATE,0),
('CA-005','QQ-0005','AST-007','气象环境数据','使用权','调度辅助','云南电网','调度中心','已驳回',NULL,0,'元数据质量评分 60 低于 80,自动驳回',NULL,NULL,0,SYSDATE,0),
('CA-006','QQ-0006','AST-006','充电桩运营数据','经营权','对外经营','广东电网','综能公司','草稿',NULL,1,'涉合作方商密,待补充许可','E','I',1,SYSDATE,0);

-- 确权汇总表(表3/表4)
INSERT INTO IM_CONFIRM_SUMMARY (CEC_SUMMARY_ID,CEC_APPLY_ID,CEC_SUMMARY_TYPE,CEC_CONTENT,CEC_GENERATOR_ID,CEC_GENERATE_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('SM-001','CA-001','表3 数据确权信息汇总表','客户用电信息表确权汇总:持有权归广东电网','合规管控小组',SYSDATE,SYSDATE,0),
('SM-002','CA-001','表4 数据权益内部管理汇总表','内部管理:责任部门数字化部,有效期3年','合规管控小组',SYSDATE,SYSDATE,0);

-- 确权材料(4)
INSERT INTO IM_CONFIRM_MATERIAL (CEC_MATERIAL_ID,CEC_APPLY_ID,CEC_MATERIAL_NAME,CEC_MATERIAL_TYPE,CEC_FILE_URL,CEC_OWNER,CEC_UPLOAD_TIME,CEC_CHECK_RESULT,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('MT-001','CA-001','确权证明.pdf','确权证明','/files/mt001.pdf','广东电网',SYSDATE,'通过',SYSDATE,0),
('MT-002','CA-001','用户授权函.pdf','授权函','/files/mt002.pdf','广东电网',SYSDATE,'通过',SYSDATE,0),
('MT-003','CA-004','采购协议.pdf','采购协议','/files/mt003.pdf','广西电网',SYSDATE,'待校验',SYSDATE,0),
('MT-004','CA-003','权属说明.docx','权属说明','/files/mt004.docx','深圳供电局',SYSDATE,'不通过',SYSDATE,0);

-- 权益卡片(5)
INSERT INTO IM_EQUITY_CARD_INFO (CEC_CARD_ID,CEC_CARD_NO,CEC_APPLY_ID,CEC_ASSET_ID,CEC_ASSET_NAME,CEC_RIGHT_TYPE,CEC_RIGHT_OWNER,CEC_RIGHT_SOURCE,CEC_VALID_DATE,CEC_CARD_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CARD-001','EC-PRA-0001','CA-001','AST-001','客户用电信息表','持有权','广东电网','确权认定',SYSDATE + INTERVAL '3' YEAR,'正常',SYSDATE,0),
('CARD-002','EC-PRA-0002','CA-002','AST-002','台区负荷数据','使用权','广东电网','确权认定',SYSDATE + INTERVAL '3' YEAR,'正常',SYSDATE,0),
('CARD-003','EC-PRA-0003','CA-003','AST-003','设备资产台账','持有权','深圳供电局','确权认定',SYSDATE + INTERVAL '2' YEAR,'冻结',SYSDATE,0),
('CARD-004','EC-PRA-0006','CA-006','AST-006','充电桩运营数据','经营权','广东电网','公司授权',SYSDATE + INTERVAL '2' YEAR,'正常',SYSDATE,0),
('CARD-005','EC-PRA-0008','CA-001','AST-008','线损分析数据','持有权','贵州电网','确权认定',SYSDATE + INTERVAL '3' YEAR,'失效',SYSDATE,0);

-- 权益卡片变更历史(4)
INSERT INTO IM_EQUITY_CARD_LOG (CEC_LOG_ID,CEC_CARD_ID,CEC_ACTION,CEC_FROM_STATUS,CEC_TO_STATUS,CEC_REASON,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CL-001','CARD-001','生成',NULL,'正常','确权终审通过自动制卡',SYSDATE,0),
('CL-002','CARD-003','生成',NULL,'正常','确权终审通过自动制卡',SYSDATE,0),
('CL-003','CARD-003','冻结','正常','冻结','权属争议冻结',SYSDATE,0),
('CL-004','CARD-005','注销','正常','失效','确权撤销',SYSDATE,0);

-- 权益证书模板(3)
INSERT INTO IM_EQUITY_CERT_TEMPLATE (CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_TEMPLATE_VERSION,CEC_RIGHT_TYPE,CEC_TEMPLATE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('TPL-001','持有权证书模板','v1','持有权','生效中',SYSDATE,0),
('TPL-002','使用权证书模板','v1','使用权','生效中',SYSDATE,0),
('TPL-003','经营权证书模板','v1','经营权','停用',SYSDATE,0);

-- 权益证书(3)
INSERT INTO IM_EQUITY_CERT (CEC_CERT_ID,CEC_CERT_NO,CEC_CARD_ID,CEC_ISSUE_UNIT,CEC_ISSUE_TIME,CEC_CERT_STATUS,CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CERT-001','QZ-PRA-0001','CARD-001','中国南方电网有限责任公司',SYSDATE,'生效','TPL-001','持有权证书模板',SYSDATE,0),
('CERT-002','QZ-PRA-0002','CARD-002','中国南方电网有限责任公司',SYSDATE,'生效','TPL-002','使用权证书模板',SYSDATE,0),
('CERT-003','QZ-PRA-0006','CARD-004','中国南方电网有限责任公司',SYSDATE,'已注销','TPL-003','经营权证书模板',SYSDATE,0);

-- ---- 智能确权辅助工具(补充测试数据) ----
INSERT INTO IM_AIT_MATERIAL (CEC_MATERIAL_ID,CEC_BATCH_NO,CEC_APPLY_ID,CEC_FILE_NAME,CEC_FILE_TYPE,CEC_FILE_HASH,CEC_SIZE_KB,CEC_PARSE_STATUS,CEC_PROGRESS,CEC_STORAGE_PATH,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AITM-001','BATCH-9A1F','CA-001','确权证明扫描件.pdf','pdf','3f9c2a8e1b7d4506',820,'已解析',100,'/ait/store/aitm001.pdf',SYSDATE,0),
('AITM-002','BATCH-9A1F','CA-001','用户授权函.pdf','pdf','7b1e5d2c9a0f3348',410,'已解析',100,'/ait/store/aitm002.pdf',SYSDATE,0),
('AITM-003','BATCH-7C3D','CA-006','合作方商密许可.docx','docx','c2d4f6a8b0e13579',256,'解析中',60,'/ait/store/aitm003.docx',SYSDATE,0);

INSERT INTO IM_AIT_PARSE_RESULT (CEC_PARSE_ID,CEC_MATERIAL_ID,CEC_RIGHT_SUBJECT,CEC_RIGHT_OBJECT,CEC_RIGHT_TYPE,CEC_RIGHT_TERM,CEC_AUTH_SCOPE,CEC_DATA_SOURCE,CEC_SENSITIVE_TYPE,CEC_SEAL_VALID,CEC_CONFIDENCE,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AITP-001','AITM-001','广东电网','客户用电信息','持有权','3年','全网','自行生产','个人隐私','有效',0.93,SYSDATE,0),
('AITP-002','AITM-002','广东电网','客户用电信息','使用权','2年','约定字段','公共授权','个人隐私','有效',0.88,SYSDATE,0);

INSERT INTO IM_AIT_COMPARE (CEC_COMPARE_ID,CEC_PARSE_ID,CEC_APPLY_ID,CEC_FIELD,CEC_MATERIAL_VALUE,CEC_FORM_VALUE,CEC_DIFF_TYPE,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AITC-001','AITP-001','CA-001','产权类型','持有权','持有权','一致',SYSDATE,0),
('AITC-002','AITP-001','CA-001','授权范围','全网','全字段','差异',SYSDATE,0);

INSERT INTO IM_AIT_KG_CLAIM (CEC_CLAIM_ID,CEC_ASSET_ID,CEC_SUBJECT,CEC_RIGHT_TYPE,CEC_AUTH_SCOPE,CEC_VALID_DATE,CEC_EXCLUSIVE,CEC_SOURCE_TYPE,CEC_REMARK,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AITK-001','AST-001','广东电网','持有权','全网',SYSDATE + INTERVAL '3' YEAR,1,'自行生产','基于事实性描述',SYSDATE,0),
('AITK-002','AST-006','广东电网','经营权','对外开放',SYSDATE + INTERVAL '2' YEAR,0,'公司授权','涉合作方商密',SYSDATE,0),
('AITK-003','AST-006','某充电运营合作方','使用权','合作范围',SYSDATE + INTERVAL '2' YEAR,0,'合作约定','与经营权主张存在重叠',SYSDATE,0);

INSERT INTO IM_AIT_CONFLICT (CEC_CONFLICT_ID,CEC_ASSET_ID,CEC_CONFLICT_TYPE,CEC_CONFLICT_SOURCE,CEC_CONFLICT_DESC,CEC_IMPACT_SCOPE,CEC_RISK_LEVEL,CEC_SUGGESTION,CEC_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AITF-001','AST-006','权属重叠','知识图谱比对','经营权与合作方加工使用权范围重叠','对外经营授权','高','取得第三方许可并限定用途','待消解',SYSDATE,0),
('AITF-002','AST-001','范围越界','材料-表单比对','材料授权范围窄于表单填报范围','确权认定','中','按材料口径收窄授权范围','已消解',SYSDATE,0);

INSERT INTO IM_AIT_DECISION (CEC_DECISION_ID,CEC_APPLY_ID,CEC_ASSET_ID,CEC_PREDICTION,CEC_SCORE,CEC_STRENGTH,CEC_WEAKNESS,CEC_SPLIT_PLAN,CEC_REASON,CEC_EVIDENCE_CHAIN,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AITD-001','CA-001','AST-001','建议通过',0.91,'材料齐全,权属清晰,签章有效','涉个人隐私需脱敏','持有权归广东电网,加工使用权可对内授权','要素完整且与表单一致,冲突已消解','EVID-0006',SYSDATE,0),
('AITD-002','CA-006','AST-006','建议补充材料',0.62,'经营权主张明确','合作方商密许可缺失,存在权属冲突','经营权确权前需取得第三方许可','存在未消解的权属重叠冲突',NULL,SYSDATE,0);

-- ============ 公共:区块链存证(补充测试数据) ============
INSERT INTO IM_DPR_EVIDENCE (CEC_EVIDENCE_ID,CEC_BIZ_TYPE,CEC_BIZ_ID,CEC_SUMMARY,CEC_SM3_HASH,CEC_CHAIN_TX_HASH,CEC_BLOCK_HEIGHT,CEC_ANCHOR_STATUS,CEC_EVIDENCE_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('EVID-0001','确权制卡','CARD-001','确权终审通过制卡:客户用电信息表','a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2','0x9f8e7d6c5b4a3928',100231,'已上链',SYSDATE,SYSDATE,0),
('EVID-0002','确权制卡','CARD-003','确权终审通过制卡:设备资产台账','b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3','0x8e7d6c5b4a392817',100234,'已上链',SYSDATE,SYSDATE,0),
('EVID-0003','授权发证','ACERT-001','授权发证:广州供电局-使用权','c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4','0x7d6c5b4a39281706',100240,'已上链',SYSDATE,SYSDATE,0),
('EVID-0004','协议存档','AG-001','授权运营协议双签归档:XY-0001','d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5','0x6c5b4a3928170695',100245,'已上链',SYSDATE,SYSDATE,0),
('EVID-0005','熔断处置','ACERT-003','监测联动熔断暂停授权:越权调用','e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6','0x5b4a392817069584',100251,'已上链',SYSDATE,SYSDATE,0),
('EVID-0006','变更存证','CHG-001','产权变更:确权状态 待确权→已确权','f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1','0x4a39281706958473',100255,'已上链',SYSDATE,SYSDATE,0);

-- ============ F-03 数据授权管理 ============

-- 授权域目录项:指引/场景/表单模板/协议模板(8)
INSERT INTO IM_AUTH_CATALOG_ITEM (CEC_ITEM_ID,CEC_CATEGORY,CEC_NAME,CEC_ITEM_TYPE,CEC_VERSION,CEC_STATUS,CEC_PUBLISHER,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('IT-001','GUIDANCE','数据授权操作指引','操作说明','v1','生效中','数字化部',SYSDATE,0),
('IT-002','GUIDANCE','数据授权申请单空表模板','申请模板','v1','生效中','数字化部',SYSDATE,0),
('IT-003','SCENARIO','电力金融征信','应用场景','v1','生效中','合规管控小组',SYSDATE,0),
('IT-004','SCENARIO','精准营销','应用场景','v1','生效中','营销部',SYSDATE,0),
('IT-005','SCENARIO','风险防控','应用场景','v1','停用','风控部',SYSDATE,0),
('IT-006','FORM_TEMPLATE','一事一议授权申请表','表单模板','v1','生效中','数字化部',SYSDATE,0),
('IT-007','AGREEMENT_TEMPLATE','南方电网数据授权运营协议','协议模板','v2','生效中','法规部',SYSDATE,0),
('IT-008','AGREEMENT_TEMPLATE','保密承诺函','协议模板','v1','生效中','法规部',SYSDATE,0);

-- 授权申请(6,覆盖模式与状态)
INSERT INTO IM_AUTH_APPLY (CEC_APPLY_ID,CEC_APPLY_NO,CEC_AUTH_MODE,CEC_ASSET_ID,CEC_ASSET_NAME,CEC_EQUITY_CARD_ID,CEC_GRANTEE_ORG,CEC_RIGHT_TYPE,CEC_SCENARIO,CEC_SCOPE,CEC_VALID_DATE,CEC_STATUS,CEC_NEED_CONFIDENTIALITY,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AA-001','SQ-0001','一事一议','AST-001','客户用电信息表','EC-PRA-0001','广州供电局','使用权','电力金融征信','全字段',SYSDATE + INTERVAL '2' YEAR,'已生效',1,SYSDATE,0),
('AA-002','SQ-0002','一事一议','AST-002','台区负荷数据','EC-PRA-0002','深圳供电局','使用权','负荷预测','约定字段',SYSDATE + INTERVAL '2' YEAR,'合规审核中',0,SYSDATE,0),
('AA-003','SQ-0003','一事一议','AST-003','设备资产台账','EC-PRA-0003','南网科研院','使用权','设备健康分析','全字段',SYSDATE + INTERVAL '2' YEAR,'经理审核中',0,SYSDATE,0),
('AA-004','SQ-0004','批量','AST-006','充电桩运营数据','EC-PRA-0006','广东电网综能公司','经营权','对外经营','全字段',SYSDATE + INTERVAL '2' YEAR,'领导小组审批中',1,SYSDATE,0),
('AA-005','SQ-0005','批量','AST-008','线损分析数据','EC-PRA-0008','贵州电网','使用权','线损治理','约定字段',SYSDATE + INTERVAL '2' YEAR,'主管审核中',0,SYSDATE,0),
('AA-006','SQ-0006','一事一议','AST-004','停电事件记录','EC-PRA-0004','广西电网','使用权','停电分析','全字段',SYSDATE + INTERVAL '2' YEAR,'已驳回',0,SYSDATE,0);

-- 批量授权清单 表6(3)
INSERT INTO IM_BATCH_AUTH_LIST (CEC_BATCH_LIST_ID,CEC_LIST_NO,CEC_LIST_YEAR,CEC_LIST_STATUS,CEC_ITEM_COUNT,CEC_REMARK,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('BL-001','PLQD-2026-001','2026','批准',12,'2026年度批量授权清单',SYSDATE,0),
('BL-002','PLQD-2026-002','2026','申报稿',8,'综能板块批量授权',SYSDATE,0),
('BL-003','PLQD-2026-003','2026','草案',5,'科研院数据使用批量授权',SYSDATE,0);

-- 授权合规校验(3)
INSERT INTO IM_AUTH_COMPLIANCE (CEC_CHECK_ID,CEC_APPLY_ID,CEC_RISK_LEVEL,CEC_CHECK_RESULT,CEC_PROBLEM_DESC,CEC_CHECK_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AC-001','AA-001','低','通过','材料完整,权限逻辑一致',SYSDATE,SYSDATE,0),
('AC-002','AA-004','高','预警','经营权对外授权须复核对外开放目录',SYSDATE,SYSDATE,0),
('AC-003','AA-006','中','不通过','授权范围超出确权边界',SYSDATE,SYSDATE,0);

-- 授权运营协议(3)
INSERT INTO IM_AUTH_AGREEMENT (CEC_AGREEMENT_ID,CEC_AGREEMENT_NO,CEC_APPLY_ID,CEC_TEMPLATE_ID,CEC_GRANTEE_ORG,CEC_FILE_URL,CEC_SEAL_STATUS,CEC_GRANTOR_SIGNED,CEC_GRANTEE_SIGNED,CEC_REVIEW_STATUS,CEC_ARCHIVE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AG-001','XY-0001','AA-001','IT-007','广州供电局','/files/ag001.ofd','已双签',1,1,'通过','已归档',SYSDATE,0),
('AG-002','XY-0004','AA-004','IT-007','广东电网综能公司','/files/ag004.ofd','待对方签章',1,0,'待审核','未归档',SYSDATE,0),
('AG-003','XY-0003','AA-003','IT-007','南网科研院','/files/ag003.ofd','待双方签章',0,0,'待审核','未归档',SYSDATE,0);

-- 授权权益证书(4,含1暂停)
INSERT INTO IM_AUTH_CERT (CEC_CERT_ID,CEC_CERT_NO,CEC_APPLY_ID,CEC_ASSET_ID,CEC_GRANTEE_ORG,CEC_RIGHT_TYPE,CEC_SCOPE,CEC_VALID_DATE,CEC_CERT_STATUS,CEC_SUSPEND_REASON,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('ACERT-001','AC-PRA-0001','AA-001','AST-001','广州供电局','使用权','全字段',SYSDATE + INTERVAL '2' YEAR,'生效',NULL,SYSDATE,0),
('ACERT-002','AC-PRA-0004','AA-004','AST-006','广东电网综能公司','经营权','全字段',SYSDATE + 20,'生效',NULL,SYSDATE,0),
('ACERT-003','AC-PRA-0008','AA-005','AST-008','贵州电网','使用权','约定字段',SYSDATE + INTERVAL '1' YEAR,'已暂停','监测联动熔断:越权调用',SYSDATE,0),
('ACERT-004','AC-PRA-0003','AA-003','AST-003','南网科研院','使用权','全字段',SYSDATE + INTERVAL '2' YEAR,'生效',NULL,SYSDATE,0);

-- 违规追责(2)
INSERT INTO IM_AUTH_ACCOUNTABILITY (CEC_ACCOUNT_ID,CEC_CERT_ID,CEC_ASSET_ID,CEC_GRANTEE_ORG,CEC_VIOLATION_TYPE,CEC_SOURCE_ALERT_ID,CEC_REASON,CEC_HANDLE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('ACC-001','ACERT-003','AST-008','贵州电网','越权调用','AL-002','监测联动熔断:越权调用','待追责',SYSDATE,0),
('ACC-002','ACERT-002','AST-006','广东电网综能公司','超范围','AL-002','疑似超出对外开放目录','已追责',SYSDATE,0);

-- 授权权益证书模板(3)
INSERT INTO IM_AUTH_CERT_TEMPLATE (CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_TEMPLATE_VERSION,CEC_CERT_TYPE,CEC_RIGHT_TYPE,CEC_TEMPLATE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('ATPL-001','专项授权-使用权证书模板','v1','专项授权证书','使用权','生效中',SYSDATE,0),
('ATPL-002','专项授权-经营权证书模板','v1','专项授权证书','经营权','生效中',SYSDATE,0),
('ATPL-003','批量授权-使用权证书模板','v1','批量授权证书','使用权','生效中',SYSDATE,0);

-- ============ F-03 授权 P1 专项表(指引/申请模板/应用场景/协议模板库/备案) ============
-- 授权指引(5,含历史版本 + isLatest)
INSERT INTO IM_AUTH_GUIDANCE (CEC_GUIDANCE_ID,CEC_TITLE,CEC_GUIDANCE_TYPE,CEC_VERSION,CEC_PUBLISHER,CEC_PUBLISH_DATE,CEC_FILE_URL,CEC_IS_LATEST,CEC_CONTENT,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AG-000','数据授权申请操作指引','申请步骤','v1','数字化部',SYSDATE - INTERVAL '2' MONTH,'/files/授权操作指引_v1.pdf',0,'旧版操作指引',SYSDATE - INTERVAL '2' MONTH,0),
('AG-001','数据授权申请操作指引','申请步骤','v2','数字化部',SYSDATE,'/files/授权操作指引.pdf',1,'第一步:在授权目录选择数据;第二步:填报授权申请(被授权方/用途/期限);第三步:上传材料并提交合规审核;第四步:签订协议并领取权益证书。',SYSDATE,0),
('AG-002','数据对外授权管理政策文件','政策文件','v1','公司总部',SYSDATE,'/files/对外授权政策.pdf',1,'依"三权分置",取得经营权的单位对外提供数据须签协议并向数字化部备案。',SYSDATE,0),
('AG-003','数据授权申请流程图','流程图','v1','数字化部',SYSDATE,'/files/授权流程图.png',1,'目录选数据 → 填报申请 → 合规审核 → 签订协议 → 发放证书 → 对外备案',SYSDATE,0),
('AG-004','数据授权常见问答(FAQ)','常见问答','v1','数字化部',SYSDATE,'/files/授权FAQ.pdf',1,'Q:授权与确权区别?A:先确权后授权,授权范围不得超过确权边界。Q:授权期限?A:按协议约定,到期需重新申请。',SYSDATE,0);

-- 授权申请表单模板(3,字段配置 JSON)
INSERT INTO IM_AUTH_APPLY_TEMPLATE (CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_AUTH_TYPE,CEC_FIELDS_JSON,CEC_FLOW_DESC,CEC_TEMPLATE_VERSION,CEC_TEMPLATE_STATUS,CEC_REMARK,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AAT-001','独占授权申请表单模板','独占','[{"name":"grantee","label":"被授权方","type":"文本","required":true,"rule":"非空"},{"name":"scope","label":"授权范围","type":"多行文本","required":true,"rule":"不得超出确权边界"},{"name":"term","label":"独占期限","type":"日期","required":true,"rule":"不超过确权有效期"},{"name":"exclusive","label":"排他性声明","type":"多行文本","required":false,"rule":""}]','填报申请 → 合规审核 → 主管审批 → 签订独占协议 → 发证','v1','生效中','独占授权:同一数据同期仅授一方',SYSDATE,0),
('AAT-002','共享授权申请表单模板','共享','[{"name":"grantee","label":"被授权方","type":"文本","required":true,"rule":"非空"},{"name":"scope","label":"共享范围","type":"多行文本","required":true,"rule":"不得超出确权边界"},{"name":"term","label":"共享期限","type":"日期","required":true,"rule":"不超过确权有效期"},{"name":"resharable","label":"再共享限制","type":"下拉","required":true,"rule":"允许/禁止"}]','填报申请 → 合规审核 → 签订共享协议 → 发证','v1','生效中','共享授权:可多方共享使用',SYSDATE,0),
('AAT-003','委托授权申请表单模板','委托','[{"name":"consignor","label":"委托方","type":"文本","required":true,"rule":"非空"},{"name":"trustee","label":"受托方","type":"文本","required":true,"rule":"非空"},{"name":"matters","label":"委托事项","type":"多行文本","required":true,"rule":"明确处理目的"},{"name":"term","label":"委托期限","type":"日期","required":true,"rule":""},{"name":"dataReq","label":"数据处理要求","type":"多行文本","required":true,"rule":"符合安全合规"}]','填报申请 → 合规审核 → 签订委托处理协议 → 发证','v1','生效中','委托处理:受托方按约定处理数据',SYSDATE,0);

-- 应用场景(5)
INSERT INTO IM_AUTH_SCENARIO (CEC_SCENARIO_ID,CEC_SCENARIO_NAME,CEC_CATEGORY,CEC_DESCRIPTION,CEC_REASON_TEMPLATE,CEC_SCENARIO_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('SC-001','内部经营分析','内部分析','本单位内部经营/运营数据统计与分析,不对外提供','用于本单位内部经营分析,数据不出域、不对外提供,使用范围限于授权部门。','生效中',SYSDATE,0),
('SC-002','对外数据服务','对外服务','向取得经营权的外部单位提供数据产品/服务','为对外提供数据产品/服务,被授权方仅在约定范围内使用,不得再授权或超范围使用。','生效中',SYSDATE,0),
('SC-003','联合建模','联合建模','与合作方在隐私计算/可信环境下联合建模','用于与合作方在隐私计算环境联合建模,原始数据不出域,仅交换模型/结果。','生效中',SYSDATE,0),
('SC-004','监管报送','监管报送','按行政监管要求向监管机构报送数据','应行政监管要求报送数据,使用范围限于监管报送目的,符合相关法规。','生效中',SYSDATE,0),
('SC-005','科研合作','对外服务','向高校/科研机构提供脱敏数据用于科研','用于科研合作,提供脱敏数据,仅限约定科研课题使用,到期销毁。','停用',SYSDATE,0);

-- 协议模板库(5)
INSERT INTO IM_AUTH_AGREEMENT_TEMPLATE (CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_AUTH_TYPE,CEC_PURPOSE,CEC_TEMPLATE_CONTENT,CEC_TEMPLATE_VERSION,CEC_TEMPLATE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AGT-001','运营授权协议(附录D)','运营','对外服务','甲乙双方就经营权授权达成协议:授权范围、期限、再授权限制、违约责任、数据安全义务等;对外提供数据须备案。','v1','生效中',SYSDATE,0),
('AGT-002','数据使用授权协议','独占','内部分析','被授权方在约定范围内使用数据,不得超范围、不得对外提供,到期销毁或归还。','v1','生效中',SYSDATE,0),
('AGT-003','数据共享协议','共享','联合建模','多方共享数据用于联合建模,原始数据不出域,仅交换模型/结果,各方承担同等安全义务。','v1','生效中',SYSDATE,0),
('AGT-004','数据委托处理协议','委托','对外服务','委托方委托受托方处理数据,明确处理目的/方式/期限,受托方不得留存或另作他用。','v1','生效中',SYSDATE,0),
('AGT-005','旧版运营授权协议','运营','对外服务','早期模板,已停用。','v1','停用',SYSDATE,0);

-- 对外经营权授权备案(3,附录G)
INSERT INTO IM_AUTH_FILING (CEC_FILING_ID,CEC_FILING_NO,CEC_AGREEMENT_ID,CEC_APPLY_ID,CEC_FILING_ORG,CEC_GRANTEE_ORG,CEC_RIGHT_TYPE,CEC_FILING_STATUS,CEC_FILING_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('BA-001','BA-20260601001','AG-001','AA-001','广东电网','广州供电局','使用权','已备案',SYSDATE,SYSDATE,0),
('BA-002','BA-20260601002','AG-002','AA-004','广东电网综能公司','广东电网综能公司','经营权','待备案',NULL,SYSDATE,0),
('BA-003','BA-20260601003',NULL,'AA-005','贵州电网','贵州电网','使用权','待备案',NULL,SYSDATE,0);

-- =====================================================================
-- 完。34 张表均含测试数据。
-- =====================================================================
