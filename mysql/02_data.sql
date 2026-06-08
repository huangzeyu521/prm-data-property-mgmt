-- =====================================================================
-- 数据产权管理模块 · MySQL 测试数据
-- 由 H2 dev 种子转换 + 补充(存证/智能工具表原无种子,本文件补齐)
-- H2→MySQL: DATEADD→DATE_ADD, CURRENT_TIMESTAMP→NOW(), TRUE/FALSE→1/0
-- 执行:  mysql -u root -p prm_dpr < 02_data.sql   (先跑 01_schema.sql)
-- =====================================================================
USE prm_dpr;
SET NAMES utf8mb4;

-- ============ F-01 产权信息管理 ============

-- 数据资产(8)
INSERT INTO IM_DPM_DATA_ASSET_INFO (CEC_ASSET_ID,CEC_ASSET_NAME,CEC_ASSET_TYPE,CEC_SOURCE_OF_ASSETS,CEC_ASSET_STATUS,CEC_ASSET_OWNER,CEC_SUBSIDIARY_NAME,CEC_SYSTEM_NAME,CEC_SCHEMA_NAME,CEC_SECURITY_LEVEL,CEC_RESP_DEPT,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AST-001','客户用电信息表','结构化','营销系统','在用','广东电网','广东电网','营销2.0','MKT','L3','数字化部',NOW(),0),
('AST-002','台区负荷数据','结构化','计量系统','在用','广东电网','广东电网','计量自动化','METER','L2','数字化部',NOW(),0),
('AST-003','设备资产台账','结构化','PMS','在用','深圳供电局','深圳供电局','PMS2.0','PMS','L1','设备部',NOW(),0),
('AST-004','停电事件记录','结构化','调度系统','在用','深圳供电局','深圳供电局','OMS','OMS','L2','调度中心',NOW(),0),
('AST-005','电费账单明细','结构化','营销系统','在用','广西电网','广西电网','营销2.0','MKT','L3','财务部',NOW(),0),
('AST-006','充电桩运营数据','结构化','车联网平台','在用','广东电网','广东电网','车联网','EVCS','L2','综能公司',NOW(),0),
('AST-007','气象环境数据','结构化','外部接入','在用','云南电网','云南电网','气象平台','WEATHER','L1','调度中心',NOW(),0),
('AST-008','线损分析数据','结构化','计量系统','在用','贵州电网','贵州电网','线损系统','LOSS','L2','运维部',NOW(),0);

-- 产权档案(8)
INSERT INTO IM_PROPERTY_ARCHIVE (CEC_ARCHIVE_ID,CEC_ASSET_ID,CEC_ASSET_NAME,CEC_RIGHT_TYPE,CEC_RIGHT_SUBJECT,CEC_RIGHT_OBJECT,CEC_ACQUIRE_MODE,CEC_USE_SCOPE,CEC_RESP_DEPT,CEC_VALID_DATE,CEC_CONFIRM_STATUS,CEC_AUTH_STATUS,CEC_EQUITY_CARD_ID,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('ARC-001','AST-001','客户用电信息表','数据持有权','广东电网','客户用电信息','确权认定','全网','数字化部',DATE_ADD(NOW(),INTERVAL 3 YEAR),'已确权','已授权','EC-PRA-0001',NOW(),0),
('ARC-002','AST-002','台区负荷数据','数据加工使用权','广东电网','台区负荷','确权认定','省内','数字化部',DATE_ADD(NOW(),INTERVAL 3 YEAR),'已确权','未授权','EC-PRA-0002',NOW(),0),
('ARC-003','AST-003','设备资产台账','数据持有权','深圳供电局','设备台账','确权认定','本单位','设备部',DATE_ADD(NOW(),INTERVAL 2 YEAR),'已确权','未授权','EC-PRA-0003',DATE_SUB(NOW(),INTERVAL 1 MONTH),0),
('ARC-004','AST-004','停电事件记录','数据加工使用权','深圳供电局','停电记录','确权认定','省内','调度中心',DATE_ADD(NOW(),INTERVAL 20 DAY),'已确权','已授权','EC-PRA-0004',DATE_SUB(NOW(),INTERVAL 2 MONTH),0),
('ARC-005','AST-005','电费账单明细','数据持有权','广西电网','电费明细','确权认定','本单位','财务部',DATE_ADD(NOW(),INTERVAL 3 YEAR),'待确权','未授权',NULL,DATE_SUB(NOW(),INTERVAL 3 MONTH),0),
('ARC-006','AST-006','充电桩运营数据','数据产品经营权','广东电网','充电运营','公司授权','对外开放','综能公司',DATE_ADD(NOW(),INTERVAL 2 YEAR),'已确权','已授权','EC-PRA-0006',DATE_SUB(NOW(),INTERVAL 12 MONTH),0),
('ARC-007','AST-007','气象环境数据','数据加工使用权','云南电网','气象数据','交易采购','省内','调度中心',DATE_ADD(NOW(),INTERVAL 10 DAY),'待确权','未授权',NULL,DATE_SUB(NOW(),INTERVAL 13 MONTH),0),
('ARC-008','AST-008','线损分析数据','数据持有权','贵州电网','线损数据','确权认定','本单位','运维部',DATE_ADD(NOW(),INTERVAL 3 YEAR),'已确权','未授权','EC-PRA-0008',DATE_SUB(NOW(),INTERVAL 14 MONTH),0);

-- 产权变更记录(5)
INSERT INTO IM_PROPERTY_CHANGE_RECORD (CEC_CHANGE_ID,CEC_ASSET_ID,CEC_CHANGE_TYPE,CEC_FIELD_NAME,CEC_BEFORE_VALUE,CEC_AFTER_VALUE,CEC_CHANGE_REASON,CEC_SOURCE_FLOW,CEC_SOURCE_TICKET,CEC_OPERATOR_ID,CEC_CHAIN_HASH,CEC_CHANGE_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CHG-001','AST-001','确权','confirmStatus','待确权','已确权','确权终审通过','确权流程','QQ-0001','admin','a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2',NOW(),NOW(),0),
('CHG-002','AST-001','授权','authStatus','未授权','已授权','授权协议生效','授权流程','SQ-0001','admin','b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3',NOW(),NOW(),0),
('CHG-003','AST-004','授权','authStatus','未授权','已授权','专项授权生效','授权流程','SQ-0004','admin','c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4',NOW(),NOW(),0),
('CHG-004','AST-006','确权','rightType','数据持有权','数据产品经营权','经营权确权认定','确权流程','QQ-0006','admin','d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5',NOW(),NOW(),0),
('CHG-005','AST-002','权益变更','validDate','2027','2028','季度重确权延期','重确权','QQ-0002','admin',NULL,NOW(),NOW(),0);

-- 监测规则(7,含1条联动熔断 + 申请/审核2条 + 1条草稿可删)
INSERT INTO IM_MONITOR_RULE (CEC_RULE_ID,CEC_RULE_NAME,CEC_RULE_CATEGORY,CEC_MONITOR_TARGET,CEC_THRESHOLD,CEC_PRIORITY,CEC_NOTIFY_TARGET,CEC_NOTIFY_CHANNEL,CEC_RULE_VERSION,CEC_EFFECT_STATUS,CEC_CIRCUIT_BREAK,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('RULE-001','授权到期提醒规则','到期提醒','授权证书','30','重要','合规管控小组','站内信,邮件','v1','生效中',0,NOW(),0),
('RULE-002','越权调用熔断规则','调用异常','授权履约','5','紧急','数字化部','站内信,短信,eLink','v1','生效中',1,NOW(),0),
('RULE-003','权属变动监测规则','权属变动','权益卡片','1','重要','合规管控小组','站内信','v1','生效中',0,NOW(),0),
('RULE-004','材料缺失合规规则','合规','确权材料','0','普通','数字化部','站内信','v1','停用',0,NOW(),0),
('RULE-005','确权申请审核积压规则','申请审核','确权申请','7','重要','合规管控小组','站内信,邮件','v1','生效中',0,NOW(),0),
('RULE-006','授权申请审核积压规则','申请审核','授权申请','7','重要','数字化部','站内信,邮件','v1','生效中',0,NOW(),0),
('RULE-007','调用频次异常监测规则(草稿)','调用异常','授权履约','100','普通','数字化部','站内信','v1','草稿',0,NOW(),0);

-- 风险预警(6)
INSERT INTO IM_ALERT_RECORD (CEC_ALERT_ID,CEC_RULE_ID,CEC_SOURCE,CEC_ASSET_ID,CEC_ALERT_LEVEL,CEC_TRIGGER_COND,CEC_ABNORMAL_DESC,CEC_ALERT_TIME,CEC_DISPOSE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AL-001','RULE-001','状态监控','AST-004','重要','授权到期<30天','停电事件记录授权将于20天后到期',NOW(),'待处理',NOW(),0),
('AL-002','RULE-002','授权履约监测','AST-006','紧急','越权调用','检测到充电桩数据跨域越权访问',NOW(),'处理中',NOW(),0),
('AL-003','RULE-003','状态监控','AST-002','普通','权属变动','台区负荷数据来源系统变更',NOW(),'已关闭',NOW(),0),
('AL-004','RULE-001','状态监控','AST-007','紧急','已过期','气象数据确权将于10天内到期',NOW(),'待处理',NOW(),0),
('AL-005','RULE-004','合规检查','AST-005','重要','材料缺失','电费账单确权缺采购协议',NOW(),'处理中',NOW(),0),
('AL-006','RULE-002','授权履约监测','AST-001','普通','调用频次异常','客户用电信息调用频次偏高',NOW(),'已关闭',NOW(),0);

-- 风险预警定向通知(按命中规则的通知对象+通知方式推送责任人,3 未读)
INSERT INTO IM_ALERT_NOTIFICATION (CEC_NOTIFY_ID,CEC_ALERT_ID,CEC_ASSET_ID,CEC_RECIPIENT,CEC_CHANNEL,CEC_TITLE,CEC_CONTENT,CEC_ALERT_LEVEL,CEC_READ_STATUS,CEC_PUSH_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('NT-001','AL-001','AST-004','合规管控小组','站内信,邮件','[重要]授权到期<30天','停电事件记录授权将于20天后到期','重要','未读',NOW(),NOW(),0),
('NT-002','AL-002','AST-006','数字化部','站内信,短信,eLink','[紧急]越权调用','检测到充电桩数据跨域越权访问','紧急','未读',NOW(),NOW(),0),
('NT-004','AL-004','AST-007','合规管控小组','站内信,邮件','[紧急]已过期','气象数据确权将于10天内到期','紧急','未读',NOW(),NOW(),0);

-- 合规检查结果(4)
INSERT INTO IM_COMPLIANCE_RESULT (CEC_CHECK_ID,CEC_ASSET_ID,CEC_RULE_ID,CEC_CHECK_RESULT,CEC_PROBLEM_DESC,CEC_SUGGESTION,CEC_CHECK_TIME,CEC_DISPOSE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CHK-001','AST-005','RULE-004','不通过','缺少采购协议材料','补传采购协议后重新校验',NOW(),'待处理',NOW(),0),
('CHK-002','AST-001','RULE-004','通过','材料完整','无',NOW(),'已关闭',NOW(),0),
('CHK-003','AST-004','RULE-001','预警','授权即将到期','启动续签流程',NOW(),'处理中',NOW(),0),
('CHK-004','AST-006','RULE-002','不通过','经营权授权疑似超出对外开放目录','复核对外开放目录范围',NOW(),'待处理',NOW(),0);

-- ============ F-02 数据确权管理 ============

-- 确权指引(4,含"操作指引"v1历史版本)
INSERT INTO IM_CONFIRM_GUIDANCE (CEC_GUIDANCE_ID,CEC_TITLE,CEC_GUIDANCE_TYPE,CEC_VERSION,CEC_PUBLISHER,CEC_PUBLISH_DATE,CEC_FILE_URL,CEC_IS_LATEST,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('GD-000','数据确权操作指引','操作说明','v1','数字化部',DATE_SUB(NOW(),INTERVAL 3 MONTH),'/files/确权操作指引_v1.pdf',0,DATE_SUB(NOW(),INTERVAL 3 MONTH),0),
('GD-001','数据确权操作指引','操作说明','v2','数字化部',NOW(),'/files/确权操作指引.pdf',1,NOW(),0),
('GD-002','数据确权授权业务指导书(附录F)','政策文件','v1','公司总部',NOW(),'/files/附录F.pdf',1,NOW(),0),
('GD-003','确权材料样例与模板','材料样例','v1','合规管控小组',NOW(),'/files/确权材料样例.zip',1,NOW(),0);

-- 确权申请(6,覆盖各状态)
INSERT INTO IM_CONFIRM_APPLY (CEC_APPLY_ID,CEC_APPLY_NO,CEC_ASSET_ID,CEC_ASSET_NAME,CEC_RIGHT_TYPE,CEC_PURPOSE,CEC_RIGHT_HOLDER,CEC_RESP_DEPT,CEC_STATUS,CEC_CURRENT_NODE,CEC_INVOLVES_THIRD_PARTY,CEC_THIRD_PARTY_INFO,CEC_SOURCE_IDENT,CEC_RELATION_IDENT,CEC_RE_CONFIRM,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CA-001','QQ-0001','AST-001','客户用电信息表','数据持有权','营销分析','广东电网','数字化部','已完成',80,1,'涉用户隐私,已取得授权','A','H',0,NOW(),0),
('CA-002','QQ-0002','AST-002','台区负荷数据','数据加工使用权','负荷预测','广东电网','数字化部','经理终审中',70,0,NULL,'A',NULL,0,NOW(),0),
('CA-003','QQ-0003','AST-003','设备资产台账','数据持有权','资产管理','深圳供电局','设备部','主管复核中',60,0,NULL,'A',NULL,0,NOW(),0),
('CA-004','QQ-0004','AST-005','电费账单明细','数据持有权','财务核算','广西电网','财务部','合规审核中',50,0,NULL,'E',NULL,0,NOW(),0),
('CA-005','QQ-0005','AST-007','气象环境数据','数据加工使用权','调度辅助','云南电网','调度中心','已驳回',NULL,0,'元数据质量评分 60 低于 80,自动驳回',NULL,NULL,0,NOW(),0),
('CA-006','QQ-0006','AST-006','充电桩运营数据','数据产品经营权','对外经营','广东电网','综能公司','草稿',NULL,1,'涉合作方商密,待补充许可','E','I',1,NOW(),0);

-- 确权汇总表(表3/表4)
INSERT INTO IM_CONFIRM_SUMMARY (CEC_SUMMARY_ID,CEC_APPLY_ID,CEC_SUMMARY_TYPE,CEC_CONTENT,CEC_GENERATOR_ID,CEC_GENERATE_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('SM-001','CA-001','表3 数据确权信息汇总表','客户用电信息表确权汇总:持有权归广东电网','合规管控小组',NOW(),NOW(),0),
('SM-002','CA-001','表4 数据权益内部管理汇总表','内部管理:责任部门数字化部,有效期3年','合规管控小组',NOW(),NOW(),0);

-- 确权材料(4)
INSERT INTO IM_CONFIRM_MATERIAL (CEC_MATERIAL_ID,CEC_APPLY_ID,CEC_MATERIAL_NAME,CEC_MATERIAL_TYPE,CEC_FILE_URL,CEC_OWNER,CEC_UPLOAD_TIME,CEC_CHECK_RESULT,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('MT-001','CA-001','确权证明.pdf','确权证明','/files/mt001.pdf','广东电网',NOW(),'通过',NOW(),0),
('MT-002','CA-001','用户授权函.pdf','授权函','/files/mt002.pdf','广东电网',NOW(),'通过',NOW(),0),
('MT-003','CA-004','采购协议.pdf','采购协议','/files/mt003.pdf','广西电网',NOW(),'待校验',NOW(),0),
('MT-004','CA-003','权属说明.docx','权属说明','/files/mt004.docx','深圳供电局',NOW(),'不通过',NOW(),0);

-- 权益卡片(5)
INSERT INTO IM_EQUITY_CARD_INFO (CEC_CARD_ID,CEC_CARD_NO,CEC_APPLY_ID,CEC_ASSET_ID,CEC_ASSET_NAME,CEC_RIGHT_TYPE,CEC_RIGHT_OWNER,CEC_RIGHT_SOURCE,CEC_VALID_DATE,CEC_CARD_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CARD-001','EC-PRA-0001','CA-001','AST-001','客户用电信息表','数据持有权','广东电网','确权认定',DATE_ADD(NOW(),INTERVAL 3 YEAR),'正常',NOW(),0),
('CARD-002','EC-PRA-0002','CA-002','AST-002','台区负荷数据','数据加工使用权','广东电网','确权认定',DATE_ADD(NOW(),INTERVAL 3 YEAR),'正常',NOW(),0),
('CARD-003','EC-PRA-0003','CA-003','AST-003','设备资产台账','数据持有权','深圳供电局','确权认定',DATE_ADD(NOW(),INTERVAL 2 YEAR),'冻结',NOW(),0),
('CARD-004','EC-PRA-0006','CA-006','AST-006','充电桩运营数据','数据产品经营权','广东电网','公司授权',DATE_ADD(NOW(),INTERVAL 2 YEAR),'正常',NOW(),0),
('CARD-005','EC-PRA-0008','CA-001','AST-008','线损分析数据','数据持有权','贵州电网','确权认定',DATE_ADD(NOW(),INTERVAL 3 YEAR),'失效',NOW(),0);

-- 权益卡片变更历史(4)
INSERT INTO IM_EQUITY_CARD_LOG (CEC_LOG_ID,CEC_CARD_ID,CEC_ACTION,CEC_FROM_STATUS,CEC_TO_STATUS,CEC_REASON,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CL-001','CARD-001','生成',NULL,'正常','确权终审通过自动制卡',NOW(),0),
('CL-002','CARD-003','生成',NULL,'正常','确权终审通过自动制卡',NOW(),0),
('CL-003','CARD-003','冻结','正常','冻结','权属争议冻结',NOW(),0),
('CL-004','CARD-005','注销','正常','失效','确权撤销',NOW(),0);

-- 权益证书模板(3)
INSERT INTO IM_EQUITY_CERT_TEMPLATE (CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_TEMPLATE_VERSION,CEC_RIGHT_TYPE,CEC_TEMPLATE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('TPL-001','数据持有权证书模板','v1','数据资源持有权','生效中',NOW(),0),
('TPL-002','数据加工使用权证书模板','v1','数据加工使用权','生效中',NOW(),0),
('TPL-003','数据产品经营权证书模板','v1','数据产品经营权','停用',NOW(),0);

-- 权益证书(3)
INSERT INTO IM_EQUITY_CERT (CEC_CERT_ID,CEC_CERT_NO,CEC_CARD_ID,CEC_ISSUE_UNIT,CEC_ISSUE_TIME,CEC_CERT_STATUS,CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CERT-001','QZ-PRA-0001','CARD-001','中国南方电网有限责任公司',NOW(),'生效','TPL-001','数据持有权证书模板',NOW(),0),
('CERT-002','QZ-PRA-0002','CARD-002','中国南方电网有限责任公司',NOW(),'生效','TPL-002','数据加工使用权证书模板',NOW(),0),
('CERT-003','QZ-PRA-0006','CARD-004','中国南方电网有限责任公司',NOW(),'已注销','TPL-003','数据产品经营权证书模板',NOW(),0);

-- ---- 智能确权辅助工具(补充测试数据) ----
INSERT INTO IM_AIT_MATERIAL (CEC_MATERIAL_ID,CEC_BATCH_NO,CEC_APPLY_ID,CEC_FILE_NAME,CEC_FILE_TYPE,CEC_FILE_HASH,CEC_SIZE_KB,CEC_PARSE_STATUS,CEC_PROGRESS,CEC_STORAGE_PATH,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AITM-001','BATCH-9A1F','CA-001','确权证明扫描件.pdf','pdf','3f9c2a8e1b7d4506',820,'已解析',100,'/ait/store/aitm001.pdf',NOW(),0),
('AITM-002','BATCH-9A1F','CA-001','用户授权函.pdf','pdf','7b1e5d2c9a0f3348',410,'已解析',100,'/ait/store/aitm002.pdf',NOW(),0),
('AITM-003','BATCH-7C3D','CA-006','合作方商密许可.docx','docx','c2d4f6a8b0e13579',256,'解析中',60,'/ait/store/aitm003.docx',NOW(),0);

INSERT INTO IM_AIT_PARSE_RESULT (CEC_PARSE_ID,CEC_MATERIAL_ID,CEC_RIGHT_SUBJECT,CEC_RIGHT_OBJECT,CEC_RIGHT_TYPE,CEC_RIGHT_TERM,CEC_AUTH_SCOPE,CEC_DATA_SOURCE,CEC_SENSITIVE_TYPE,CEC_SEAL_VALID,CEC_CONFIDENCE,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AITP-001','AITM-001','广东电网','客户用电信息','数据持有权','3年','全网','自行生产','个人隐私','有效',0.93,NOW(),0),
('AITP-002','AITM-002','广东电网','客户用电信息','数据加工使用权','2年','约定字段','公共授权','个人隐私','有效',0.88,NOW(),0);

INSERT INTO IM_AIT_COMPARE (CEC_COMPARE_ID,CEC_PARSE_ID,CEC_APPLY_ID,CEC_FIELD,CEC_MATERIAL_VALUE,CEC_FORM_VALUE,CEC_DIFF_TYPE,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AITC-001','AITP-001','CA-001','产权类型','数据持有权','数据持有权','一致',NOW(),0),
('AITC-002','AITP-001','CA-001','授权范围','全网','全字段','差异',NOW(),0);

INSERT INTO IM_AIT_KG_CLAIM (CEC_CLAIM_ID,CEC_ASSET_ID,CEC_SUBJECT,CEC_RIGHT_TYPE,CEC_AUTH_SCOPE,CEC_VALID_DATE,CEC_EXCLUSIVE,CEC_SOURCE_TYPE,CEC_REMARK,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AITK-001','AST-001','广东电网','数据持有权','全网',DATE_ADD(NOW(),INTERVAL 3 YEAR),1,'自行生产','基于事实性描述',NOW(),0),
('AITK-002','AST-006','广东电网','数据产品经营权','对外开放',DATE_ADD(NOW(),INTERVAL 2 YEAR),0,'公司授权','涉合作方商密',NOW(),0),
('AITK-003','AST-006','某充电运营合作方','数据加工使用权','合作范围',DATE_ADD(NOW(),INTERVAL 2 YEAR),0,'合作约定','与经营权主张存在重叠',NOW(),0);

INSERT INTO IM_AIT_CONFLICT (CEC_CONFLICT_ID,CEC_ASSET_ID,CEC_CONFLICT_TYPE,CEC_CONFLICT_SOURCE,CEC_CONFLICT_DESC,CEC_IMPACT_SCOPE,CEC_RISK_LEVEL,CEC_SUGGESTION,CEC_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AITF-001','AST-006','权属重叠','知识图谱比对','经营权与合作方加工使用权范围重叠','对外经营授权','高','取得第三方许可并限定用途','待消解',NOW(),0),
('AITF-002','AST-001','范围越界','材料-表单比对','材料授权范围窄于表单填报范围','确权认定','中','按材料口径收窄授权范围','已消解',NOW(),0);

INSERT INTO IM_AIT_DECISION (CEC_DECISION_ID,CEC_APPLY_ID,CEC_ASSET_ID,CEC_PREDICTION,CEC_SCORE,CEC_STRENGTH,CEC_WEAKNESS,CEC_SPLIT_PLAN,CEC_REASON,CEC_EVIDENCE_CHAIN,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AITD-001','CA-001','AST-001','建议通过',0.91,'材料齐全,权属清晰,签章有效','涉个人隐私需脱敏','持有权归广东电网,加工使用权可对内授权','要素完整且与表单一致,冲突已消解','EVID-0006',NOW(),0),
('AITD-002','CA-006','AST-006','建议补充材料',0.62,'经营权主张明确','合作方商密许可缺失,存在权属冲突','经营权确权前需取得第三方许可','存在未消解的权属重叠冲突',NULL,NOW(),0);

-- ============ 公共:区块链存证(补充测试数据) ============
INSERT INTO IM_DPR_EVIDENCE (CEC_EVIDENCE_ID,CEC_BIZ_TYPE,CEC_BIZ_ID,CEC_SUMMARY,CEC_SM3_HASH,CEC_CHAIN_TX_HASH,CEC_BLOCK_HEIGHT,CEC_ANCHOR_STATUS,CEC_EVIDENCE_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('EVID-0001','确权制卡','CARD-001','确权终审通过制卡:客户用电信息表','a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2','0x9f8e7d6c5b4a3928',100231,'已上链',NOW(),NOW(),0),
('EVID-0002','确权制卡','CARD-003','确权终审通过制卡:设备资产台账','b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3','0x8e7d6c5b4a392817',100234,'已上链',NOW(),NOW(),0),
('EVID-0003','授权发证','ACERT-001','授权发证:广州供电局-数据加工使用权','c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4','0x7d6c5b4a39281706',100240,'已上链',NOW(),NOW(),0),
('EVID-0004','协议存档','AG-001','授权运营协议双签归档:XY-0001','d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5','0x6c5b4a3928170695',100245,'已上链',NOW(),NOW(),0),
('EVID-0005','熔断处置','ACERT-003','监测联动熔断暂停授权:越权调用','e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6','0x5b4a392817069584',100251,'已上链',NOW(),NOW(),0),
('EVID-0006','变更存证','CHG-001','产权变更:确权状态 待确权→已确权','f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1','0x4a39281706958473',100255,'已上链',NOW(),NOW(),0);

-- ============ F-03 数据授权管理 ============

-- 授权域目录项:指引/场景/表单模板/协议模板(8)
INSERT INTO IM_AUTH_CATALOG_ITEM (CEC_ITEM_ID,CEC_CATEGORY,CEC_NAME,CEC_ITEM_TYPE,CEC_VERSION,CEC_STATUS,CEC_PUBLISHER,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('IT-001','GUIDANCE','数据授权操作指引','操作说明','v1','生效中','数字化部',NOW(),0),
('IT-002','GUIDANCE','数据授权申请单空表模板','申请模板','v1','生效中','数字化部',NOW(),0),
('IT-003','SCENARIO','电力金融征信','应用场景','v1','生效中','合规管控小组',NOW(),0),
('IT-004','SCENARIO','精准营销','应用场景','v1','生效中','营销部',NOW(),0),
('IT-005','SCENARIO','风险防控','应用场景','v1','停用','风控部',NOW(),0),
('IT-006','FORM_TEMPLATE','一事一议授权申请表','表单模板','v1','生效中','数字化部',NOW(),0),
('IT-007','AGREEMENT_TEMPLATE','南方电网数据授权运营协议','协议模板','v2','生效中','法规部',NOW(),0),
('IT-008','AGREEMENT_TEMPLATE','保密承诺函','协议模板','v1','生效中','法规部',NOW(),0);

-- 授权申请(6,覆盖模式与状态)
INSERT INTO IM_AUTH_APPLY (CEC_APPLY_ID,CEC_APPLY_NO,CEC_AUTH_MODE,CEC_ASSET_ID,CEC_ASSET_NAME,CEC_EQUITY_CARD_ID,CEC_GRANTEE_ORG,CEC_RIGHT_TYPE,CEC_SCENARIO,CEC_SCOPE,CEC_VALID_DATE,CEC_STATUS,CEC_NEED_CONFIDENTIALITY,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AA-001','SQ-0001','一事一议','AST-001','客户用电信息表','EC-PRA-0001','广州供电局','数据加工使用权','电力金融征信','全字段',DATE_ADD(NOW(),INTERVAL 2 YEAR),'已生效',1,NOW(),0),
('AA-002','SQ-0002','一事一议','AST-002','台区负荷数据','EC-PRA-0002','深圳供电局','数据加工使用权','负荷预测','约定字段',DATE_ADD(NOW(),INTERVAL 2 YEAR),'合规审核中',0,NOW(),0),
('AA-003','SQ-0003','一事一议','AST-003','设备资产台账','EC-PRA-0003','南网科研院','数据加工使用权','设备健康分析','全字段',DATE_ADD(NOW(),INTERVAL 2 YEAR),'经理审核中',0,NOW(),0),
('AA-004','SQ-0004','批量','AST-006','充电桩运营数据','EC-PRA-0006','广东电网综能公司','数据产品经营权','对外经营','全字段',DATE_ADD(NOW(),INTERVAL 2 YEAR),'领导小组审批中',1,NOW(),0),
('AA-005','SQ-0005','批量','AST-008','线损分析数据','EC-PRA-0008','贵州电网','数据加工使用权','线损治理','约定字段',DATE_ADD(NOW(),INTERVAL 2 YEAR),'数字化部认定中',0,NOW(),0),
('AA-006','SQ-0006','一事一议','AST-004','停电事件记录','EC-PRA-0004','广西电网','数据加工使用权','停电分析','全字段',DATE_ADD(NOW(),INTERVAL 2 YEAR),'已驳回',0,NOW(),0);

-- 批量授权清单 表6(3)
INSERT INTO IM_BATCH_AUTH_LIST (CEC_BATCH_LIST_ID,CEC_LIST_NO,CEC_LIST_YEAR,CEC_LIST_STATUS,CEC_ITEM_COUNT,CEC_REMARK,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('BL-001','PLQD-2026-001','2026','批准',12,'2026年度批量授权清单',NOW(),0),
('BL-002','PLQD-2026-002','2026','申报稿',8,'综能板块批量授权',NOW(),0),
('BL-003','PLQD-2026-003','2026','草案',5,'科研院数据使用批量授权',NOW(),0);

-- 授权合规校验(3)
INSERT INTO IM_AUTH_COMPLIANCE (CEC_CHECK_ID,CEC_APPLY_ID,CEC_RISK_LEVEL,CEC_CHECK_RESULT,CEC_PROBLEM_DESC,CEC_CHECK_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AC-001','AA-001','低','通过','材料完整,权限逻辑一致',NOW(),NOW(),0),
('AC-002','AA-004','高','预警','经营权对外授权须复核对外开放目录',NOW(),NOW(),0),
('AC-003','AA-006','中','不通过','授权范围超出确权边界',NOW(),NOW(),0);

-- 授权运营协议(3)
INSERT INTO IM_AUTH_AGREEMENT (CEC_AGREEMENT_ID,CEC_AGREEMENT_NO,CEC_APPLY_ID,CEC_TEMPLATE_ID,CEC_GRANTEE_ORG,CEC_FILE_URL,CEC_SEAL_STATUS,CEC_GRANTOR_SIGNED,CEC_GRANTEE_SIGNED,CEC_REVIEW_STATUS,CEC_ARCHIVE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AG-001','XY-0001','AA-001','IT-007','广州供电局','/files/ag001.ofd','已双签',1,1,'通过','已归档',NOW(),0),
('AG-002','XY-0004','AA-004','IT-007','广东电网综能公司','/files/ag004.ofd','待对方签章',1,0,'待审核','未归档',NOW(),0),
('AG-003','XY-0003','AA-003','IT-007','南网科研院','/files/ag003.ofd','待双方签章',0,0,'待审核','未归档',NOW(),0);

-- 授权权益证书(4,含1暂停)
INSERT INTO IM_AUTH_CERT (CEC_CERT_ID,CEC_CERT_NO,CEC_APPLY_ID,CEC_ASSET_ID,CEC_GRANTEE_ORG,CEC_RIGHT_TYPE,CEC_SCOPE,CEC_VALID_DATE,CEC_CERT_STATUS,CEC_SUSPEND_REASON,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('ACERT-001','AC-PRA-0001','AA-001','AST-001','广州供电局','数据加工使用权','全字段',DATE_ADD(NOW(),INTERVAL 2 YEAR),'生效',NULL,NOW(),0),
('ACERT-002','AC-PRA-0004','AA-004','AST-006','广东电网综能公司','数据产品经营权','全字段',DATE_ADD(NOW(),INTERVAL 20 DAY),'生效',NULL,NOW(),0),
('ACERT-003','AC-PRA-0008','AA-005','AST-008','贵州电网','数据加工使用权','约定字段',DATE_ADD(NOW(),INTERVAL 1 YEAR),'已暂停','监测联动熔断:越权调用',NOW(),0),
('ACERT-004','AC-PRA-0003','AA-003','AST-003','南网科研院','数据加工使用权','全字段',DATE_ADD(NOW(),INTERVAL 2 YEAR),'生效',NULL,NOW(),0);

-- 违规追责(2)
INSERT INTO IM_AUTH_ACCOUNTABILITY (CEC_ACCOUNT_ID,CEC_CERT_ID,CEC_ASSET_ID,CEC_GRANTEE_ORG,CEC_VIOLATION_TYPE,CEC_SOURCE_ALERT_ID,CEC_REASON,CEC_HANDLE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('ACC-001','ACERT-003','AST-008','贵州电网','越权调用','AL-002','监测联动熔断:越权调用','待追责',NOW(),0),
('ACC-002','ACERT-002','AST-006','广东电网综能公司','超范围','AL-002','疑似超出对外开放目录','已追责',NOW(),0);

-- 授权权益证书模板(3)
INSERT INTO IM_AUTH_CERT_TEMPLATE (CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_TEMPLATE_VERSION,CEC_CERT_TYPE,CEC_RIGHT_TYPE,CEC_TEMPLATE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('ATPL-001','专项授权-数据加工使用权证书模板','v1','专项授权证书','数据加工使用权','生效中',NOW(),0),
('ATPL-002','专项授权-数据产品经营权证书模板','v1','专项授权证书','数据产品经营权','生效中',NOW(),0),
('ATPL-003','批量授权-数据加工使用权证书模板','v1','批量授权证书','数据加工使用权','生效中',NOW(),0);

-- =====================================================================
-- 完。29 张表均含测试数据。
-- =====================================================================
