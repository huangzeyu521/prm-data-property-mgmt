-- 演示数据(仅 dev profile 加载;test profile 不加载,避免污染集成测试断言)
-- 数据资产(8)
INSERT INTO IM_DPM_DATA_ASSET_INFO (CEC_ASSET_ID,CEC_ASSET_NAME,CEC_ASSET_TYPE,CEC_SOURCE_OF_ASSETS,CEC_ASSET_STATUS,CEC_ASSET_OWNER,CEC_SUBSIDIARY_NAME,CEC_SYSTEM_NAME,CEC_SCHEMA_NAME,CEC_SECURITY_LEVEL,CEC_RESP_DEPT,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AST-001','客户用电信息表','结构化','营销系统','在用','广东电网','广东电网','营销2.0','MKT','L3','数字化部',CURRENT_TIMESTAMP,0),
('AST-002','台区负荷数据','结构化','计量系统','在用','广东电网','广东电网','计量自动化','METER','L2','数字化部',CURRENT_TIMESTAMP,0),
('AST-003','设备资产台账','结构化','PMS','在用','深圳供电局','深圳供电局','PMS2.0','PMS','L1','设备部',CURRENT_TIMESTAMP,0),
('AST-004','停电事件记录','结构化','调度系统','在用','深圳供电局','深圳供电局','OMS','OMS','L2','调度中心',CURRENT_TIMESTAMP,0),
('AST-005','电费账单明细','结构化','营销系统','在用','广西电网','广西电网','营销2.0','MKT','L3','财务部',CURRENT_TIMESTAMP,0),
('AST-006','充电桩运营数据','结构化','车联网平台','在用','广东电网','广东电网','车联网','EVCS','L2','综能公司',CURRENT_TIMESTAMP,0),
('AST-007','气象环境数据','结构化','外部接入','在用','云南电网','云南电网','气象平台','WEATHER','L1','调度中心',CURRENT_TIMESTAMP,0),
('AST-008','线损分析数据','结构化','计量系统','在用','贵州电网','贵州电网','线损系统','LOSS','L2','运维部',CURRENT_TIMESTAMP,0);

-- 产权档案(8,部分已确权/已授权)
INSERT INTO IM_PROPERTY_ARCHIVE (CEC_ARCHIVE_ID,CEC_ASSET_ID,CEC_ASSET_NAME,CEC_RIGHT_TYPE,CEC_RIGHT_SUBJECT,CEC_RIGHT_OBJECT,CEC_ACQUIRE_MODE,CEC_USE_SCOPE,CEC_RESP_DEPT,CEC_VALID_DATE,CEC_CONFIRM_STATUS,CEC_AUTH_STATUS,CEC_EQUITY_CARD_ID,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('ARC-001','AST-001','客户用电信息表','持有权','广东电网','客户用电信息','确权认定','全网','数字化部',DATEADD('YEAR',3,CURRENT_TIMESTAMP),'已确权','已授权','EC-PRA-0001',CURRENT_TIMESTAMP,0),
('ARC-002','AST-002','台区负荷数据','经营权','广东电网','台区负荷','确权认定','对外开放','数字化部',DATEADD('YEAR',3,CURRENT_TIMESTAMP),'已确权','未授权','EC-PRA-0002',CURRENT_TIMESTAMP,0),
('ARC-003','AST-003','设备资产台账','持有权','深圳供电局','设备台账','确权认定','本单位','设备部',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'已确权','未授权','EC-PRA-0003',DATEADD('MONTH',-1,CURRENT_TIMESTAMP),0),
('ARC-004','AST-004','停电事件记录','使用权','深圳供电局','停电记录','确权认定','省内','调度中心',DATEADD('DAY',-5,CURRENT_TIMESTAMP),'已确权','已授权','EC-PRA-0004',DATEADD('MONTH',-2,CURRENT_TIMESTAMP),0),
('ARC-005','AST-005','电费账单明细','持有权','广西电网','电费明细','确权认定','本单位','财务部',DATEADD('YEAR',3,CURRENT_TIMESTAMP),'待确权','未授权',NULL,DATEADD('MONTH',-3,CURRENT_TIMESTAMP),0),
('ARC-006','AST-006','充电桩运营数据','经营权','广东电网','充电运营','公司授权','对外开放','综能公司',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'已确权','已授权','EC-PRA-0006',DATEADD('MONTH',-12,CURRENT_TIMESTAMP),0),
('ARC-007','AST-007','气象环境数据','使用权','云南电网','气象数据','交易采购','省内','调度中心',DATEADD('DAY',10,CURRENT_TIMESTAMP),'待确权','未授权',NULL,DATEADD('MONTH',-13,CURRENT_TIMESTAMP),0),
('ARC-008','AST-008','线损分析数据','持有权','贵州电网','线损数据','确权认定','本单位','运维部',DATEADD('YEAR',3,CURRENT_TIMESTAMP),'已确权','未授权','EC-PRA-0008',DATEADD('MONTH',-14,CURRENT_TIMESTAMP),0);

-- 产权变更记录(5)
INSERT INTO IM_PROPERTY_CHANGE_RECORD (CEC_CHANGE_ID,CEC_ASSET_ID,CEC_CHANGE_TYPE,CEC_FIELD_NAME,CEC_BEFORE_VALUE,CEC_AFTER_VALUE,CEC_CHANGE_REASON,CEC_SOURCE_FLOW,CEC_SOURCE_TICKET,CEC_OPERATOR_ID,CEC_CHAIN_HASH,CEC_CHANGE_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CHG-001','AST-001','确权','confirmStatus','待确权','已确权','确权终审通过','确权流程','QQ-0001','admin','a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),
('CHG-002','AST-001','授权','authStatus','未授权','已授权','授权协议生效','授权流程','SQ-0001','admin','b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),
('CHG-003','AST-004','授权','authStatus','未授权','已授权','专项授权生效','授权流程','SQ-0004','admin','c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),
('CHG-004','AST-006','确权','rightType','持有权','经营权','经营权确权认定','确权流程','QQ-0006','admin','d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),
('CHG-005','AST-002','权益变更','validDate','2027','2028','季度重确权延期','重确权','QQ-0002','admin',NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0);

-- 监测规则(7,含1条联动熔断 + 申请/审核阶段2条 + 1条草稿可删)
INSERT INTO IM_MONITOR_RULE (CEC_RULE_ID,CEC_RULE_NAME,CEC_RULE_CATEGORY,CEC_MONITOR_TARGET,CEC_THRESHOLD,CEC_PRIORITY,CEC_NOTIFY_TARGET,CEC_NOTIFY_CHANNEL,CEC_RULE_VERSION,CEC_EFFECT_STATUS,CEC_CIRCUIT_BREAK,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('RULE-001','授权到期提醒规则','到期提醒','授权证书','30','重要','合规管控小组','站内信,邮件','v1','生效中',0,CURRENT_TIMESTAMP,0),
('RULE-002','越权调用熔断规则','调用异常','授权履约','5','紧急','数字化部','站内信,短信,eLink','v1','生效中',1,CURRENT_TIMESTAMP,0),
('RULE-003','权属变动监测规则','权属变动','权益卡片','1','重要','合规管控小组','站内信','v1','生效中',0,CURRENT_TIMESTAMP,0),
('RULE-004','材料缺失合规规则','合规','确权材料','0','普通','数字化部','站内信','v1','停用',0,CURRENT_TIMESTAMP,0),
('RULE-005','确权申请审核积压规则','申请审核','确权申请','7','重要','合规管控小组','站内信,邮件','v1','生效中',0,CURRENT_TIMESTAMP,0),
('RULE-006','授权申请审核积压规则','申请审核','授权申请','7','重要','数字化部','站内信,邮件','v1','生效中',0,CURRENT_TIMESTAMP,0),
('RULE-007','调用频次异常监测规则(草稿)','调用异常','授权履约','100','普通','数字化部','站内信','v1','草稿',0,CURRENT_TIMESTAMP,0);

-- 风险预警(6)
INSERT INTO IM_ALERT_RECORD (CEC_ALERT_ID,CEC_RULE_ID,CEC_SOURCE,CEC_ASSET_ID,CEC_ALERT_LEVEL,CEC_TRIGGER_COND,CEC_ABNORMAL_DESC,CEC_ALERT_TIME,CEC_DISPOSE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('AL-001','RULE-001','状态监控','AST-004','重要','授权到期<30天','停电事件记录授权将于20天后到期',CURRENT_TIMESTAMP,'待处理',CURRENT_TIMESTAMP,0),
('AL-002','RULE-002','授权履约监测','AST-006','紧急','越权调用','检测到充电桩数据跨域越权访问',CURRENT_TIMESTAMP,'处理中',CURRENT_TIMESTAMP,0),
('AL-003','RULE-003','状态监控','AST-002','普通','权属变动','台区负荷数据来源系统变更',CURRENT_TIMESTAMP,'已关闭',CURRENT_TIMESTAMP,0),
('AL-004','RULE-001','状态监控','AST-007','紧急','已过期','气象数据确权将于10天内到期',CURRENT_TIMESTAMP,'待处理',CURRENT_TIMESTAMP,0),
('AL-005','RULE-004','合规检查','AST-005','重要','材料缺失','电费账单确权缺采购协议',CURRENT_TIMESTAMP,'处理中',CURRENT_TIMESTAMP,0),
('AL-006','RULE-002','授权履约监测','AST-001','普通','调用频次异常','客户用电信息调用频次偏高',CURRENT_TIMESTAMP,'已关闭',CURRENT_TIMESTAMP,0);

-- 风险预警定向通知(按命中规则的通知对象+通知方式推送责任人,3 未读)
INSERT INTO IM_ALERT_NOTIFICATION (CEC_NOTIFY_ID,CEC_ALERT_ID,CEC_ASSET_ID,CEC_RECIPIENT,CEC_CHANNEL,CEC_TITLE,CEC_CONTENT,CEC_ALERT_LEVEL,CEC_READ_STATUS,CEC_PUSH_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('NT-001','AL-001','AST-004','合规管控小组','站内信,邮件','[重要]授权到期<30天','停电事件记录授权将于20天后到期','重要','未读',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),
('NT-002','AL-002','AST-006','数字化部','站内信,短信,eLink','[紧急]越权调用','检测到充电桩数据跨域越权访问','紧急','未读',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),
('NT-004','AL-004','AST-007','合规管控小组','站内信,邮件','[紧急]已过期','气象数据确权将于10天内到期','紧急','未读',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0);

-- 合规检查结果(4)
INSERT INTO IM_COMPLIANCE_RESULT (CEC_CHECK_ID,CEC_ASSET_ID,CEC_RULE_ID,CEC_CHECK_RESULT,CEC_PROBLEM_DESC,CEC_SUGGESTION,CEC_CHECK_TIME,CEC_DISPOSE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CHK-001','AST-005','RULE-004','不通过','缺少采购协议材料','补传采购协议后重新校验',CURRENT_TIMESTAMP,'待处理',CURRENT_TIMESTAMP,0),
('CHK-002','AST-001','RULE-004','通过','材料完整','无',CURRENT_TIMESTAMP,'已关闭',CURRENT_TIMESTAMP,0),
('CHK-003','AST-004','RULE-001','预警','授权即将到期','启动续签流程',CURRENT_TIMESTAMP,'处理中',CURRENT_TIMESTAMP,0),
('CHK-004','AST-006','RULE-002','不通过','经营权授权疑似超出对外开放目录','复核对外开放目录范围',CURRENT_TIMESTAMP,'待处理',CURRENT_TIMESTAMP,0);

-- 数据权益风险(5,含极高)
INSERT INTO IM_DPR_RISK (CEC_RISK_ID,CEC_RISK_NO,CEC_ASSET_ID,CEC_RIGHT_TYPES,CEC_RISK_TYPE,CEC_RISK_CONTENT,CEC_RISK_LEVEL,CEC_STRATEGY,CEC_ASSESSOR,CEC_HANDLE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('FX-001','FX-20260601001','AST-001','持有权','涉及个人/家庭隐私','客户用电信息涉及个人隐私,授权范围笼统','极高','取得数据主体授权,最小化使用并脱敏','数据产权合规管控小组','待处置',CURRENT_TIMESTAMP,0),
('FX-002','FX-20260601002','AST-005','持有权','交易采购数据','电费账单二次转售条款缺失','高','与提供方补充采购合作条款','数据产权合规管控小组','处置中',CURRENT_TIMESTAMP,0),
('FX-003','FX-20260601003','AST-007','使用权','公开采集数据','气象数据可能涉及版权限制','低','排查来源取得许可','数据产权合规管控小组','已处置',CURRENT_TIMESTAMP,0),
('FX-004','FX-20260601004','AST-002','使用权','公共数据授权','授权范围模糊存在被追责风险','中','修订补充授权范围','数据产权合规管控小组','待处置',CURRENT_TIMESTAMP,0),
('FX-005','FX-20260601005','AST-006','经营权','涉及第三方商业机密','充电运营数据涉合作方商密','高','取得第三方许可并限定用途','数据产权合规管控小组','已规避',CURRENT_TIMESTAMP,0);
