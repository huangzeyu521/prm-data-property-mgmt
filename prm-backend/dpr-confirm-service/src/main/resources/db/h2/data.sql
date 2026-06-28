-- 演示数据(仅 dev profile 加载;test profile 不加载)
-- 演示账号(5 角色,密码均 Prm@1234 的 SM3 摘要):随 schema 在 datasource 初始化期种入,
-- 早于 Web 服务接流量,杜绝"AuthService 播种被慢启动的 AI 运行器拖后、登录窗口期失败"。
-- 与 AuthService 运行器幂等共存(其 selectCount 命中即跳过)。口径须与 AuthService.seed 一致。
INSERT INTO IM_SYS_USER (CEC_USER_ID,CEC_USERNAME,CEC_PASSWORD_HASH,CEC_REAL_NAME,CEC_ROLE,CEC_PROVINCE_CODE,CEC_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('USR-APPLY','apply','9338f855bfca1551a6b7e608e391c8ddaec90b3667dd6088485ee86e6bcce7e7','梁晶晶','apply','GD','启用',CURRENT_TIMESTAMP,0),
('USR-PRECHECK','precheck','9338f855bfca1551a6b7e608e391c8ddaec90b3667dd6088485ee86e6bcce7e7','周慎之','precheck','GD','启用',CURRENT_TIMESTAMP,0),
('USR-REVIEW','review','9338f855bfca1551a6b7e608e391c8ddaec90b3667dd6088485ee86e6bcce7e7','李天天','review','GD','启用',CURRENT_TIMESTAMP,0),
('USR-MANAGER','manager','9338f855bfca1551a6b7e608e391c8ddaec90b3667dd6088485ee86e6bcce7e7','黄主管','manager','GD','启用',CURRENT_TIMESTAMP,0),
('USR-DIRECTOR','director','9338f855bfca1551a6b7e608e391c8ddaec90b3667dd6088485ee86e6bcce7e7','郑经理','director','','启用',CURRENT_TIMESTAMP,0),
('USR-ADMIN','admin','9338f855bfca1551a6b7e608e391c8ddaec90b3667dd6088485ee86e6bcce7e7','陈明亮','admin','','启用',CURRENT_TIMESTAMP,0),
('USR-VIEWER','viewer','9338f855bfca1551a6b7e608e391c8ddaec90b3667dd6088485ee86e6bcce7e7','黄文静','view','','启用',CURRENT_TIMESTAMP,0),
('USR-BUSINESS','business','9338f855bfca1551a6b7e608e391c8ddaec90b3667dd6088485ee86e6bcce7e7','刘业务','business','GD','启用',CURRENT_TIMESTAMP,0),
('USR-GM','gm','9338f855bfca1551a6b7e608e391c8ddaec90b3667dd6088485ee86e6bcce7e7','高总经理','gm','','启用',CURRENT_TIMESTAMP,0),
('USR-LEADERSHIP','leadership','9338f855bfca1551a6b7e608e391c8ddaec90b3667dd6088485ee86e6bcce7e7','领导小组','leadership','','启用',CURRENT_TIMESTAMP,0),
('USR-SUPER','super','9338f855bfca1551a6b7e608e391c8ddaec90b3667dd6088485ee86e6bcce7e7','吴海涛','all','','启用',CURRENT_TIMESTAMP,0);

-- 确权指引(4,含"操作指引"v1历史版本演示历史版本记录)
INSERT INTO IM_CONFIRM_GUIDANCE (CEC_GUIDANCE_ID,CEC_TITLE,CEC_GUIDANCE_TYPE,CEC_VERSION,CEC_PUBLISHER,CEC_PUBLISH_DATE,CEC_FILE_URL,CEC_IS_LATEST,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('GD-000','数据确权操作指引','操作说明','v1','数字化部',DATEADD('MONTH',-3,CURRENT_TIMESTAMP),'/files/确权操作指引_v1.pdf',FALSE,DATEADD('MONTH',-3,CURRENT_TIMESTAMP),0),
('GD-001','数据确权操作指引','操作说明','v2','数字化部',CURRENT_TIMESTAMP,'/files/确权操作指引.pdf',TRUE,CURRENT_TIMESTAMP,0),
('GD-002','数据确权授权业务指导书(附录F)','政策文件','v1','公司总部',CURRENT_TIMESTAMP,'/files/附录F.pdf',TRUE,CURRENT_TIMESTAMP,0),
('GD-003','确权材料样例与模板','材料样例','v1','合规管控小组',CURRENT_TIMESTAMP,'/files/确权材料样例.zip',TRUE,CURRENT_TIMESTAMP,0);

-- 确权申请(6,系统级)。确权以"系统"为单元(一份确权申请 = 一个系统),演示均为系统级
-- (CEC_ASSET_ID=SYS:系统名 → 查询页"系统名称"列直显系统名,不再出现库表名);覆盖 初始/变更 两类登记 + 全状态分布。
INSERT INTO IM_CONFIRM_APPLY (CEC_APPLY_ID,CEC_APPLY_NO,CEC_ASSET_ID,CEC_ASSET_NAME,CEC_RIGHT_TYPE,CEC_PURPOSE,CEC_RIGHT_HOLDER,CEC_RESP_DEPT,CEC_SUBJECT_LEVEL,CEC_SYSTEM_OWNER,CEC_CONTACT,CEC_STATUS,CEC_CURRENT_NODE,CEC_INVOLVES_THIRD_PARTY,CEC_THIRD_PARTY_INFO,CEC_SOURCE_IDENT,CEC_RELATION_IDENT,CEC_RE_CONFIRM,CEC_REGULATED,CEC_REGISTER_TYPE,CEC_CHANGE_TRIGGER,CEC_BASELINE_REF,CEC_CHANGE_VERSION,CEC_CHANGE_SUMMARY,CEC_REJECT_REASON,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CA-001','QQ-0001','SYS:营销管理系统','营销管理系统','数据资源持有权、数据加工使用权、数据产品经营权','系统级初始确权','广东电网有限责任公司','市场营销部','分省公司','李明','020-31001001','已完成',80,TRUE,'涉用户隐私,已取得授权','A,E','G,H,I',FALSE,'非管制','初始确权',NULL,NULL,NULL,NULL,NULL,CURRENT_TIMESTAMP,0),
('CA-002','QQ-0002','SYS:计量自动化系统','计量自动化系统','数据资源持有权、数据加工使用权','系统级初始确权','广东电网有限责任公司','计量中心','分省公司','王强','020-31002002','经理终审中',70,FALSE,NULL,'A','G',FALSE,'非管制','初始确权',NULL,NULL,NULL,NULL,NULL,CURRENT_TIMESTAMP,0),
('CA-003','QQ-0003','SYS:生产管理系统','生产管理系统','数据资源持有权','系统级初始确权','广西电网有限责任公司','生产技术部','分省公司','周勇','020-31005005','主管复核中',60,FALSE,NULL,'A',NULL,FALSE,'非管制','初始确权',NULL,NULL,NULL,NULL,NULL,DATEADD('DAY',-12,CURRENT_TIMESTAMP),0),
('CA-004','QQ-0004','SYS:协同办公系统','协同办公系统','数据资源持有权、数据加工使用权、数据产品经营权','管理要求变更重新确权','中国南方电网有限责任公司','办公室(后勤部)','公司总部','孙浩','020-31004004','合规审核中',50,TRUE,'涉用户个人隐私(本次新增)','A','G,H',TRUE,'非管制','确权变更','管理要求变更','协同办公系统#v1',2,'本次确权变更(触发:管理要求变更)共修改 1 项:信息关联(G–J)「G→G、H」(新增涉用户个人隐私 H,合规要求升级);来源 A–F 维持原值',NULL,DATEADD('DAY',-10,CURRENT_TIMESTAMP),0),
('CA-005','QQ-0005','SYS:客户服务系统','客户服务系统','数据资源持有权、数据加工使用权','系统级初始确权','广东电网有限责任公司','客户服务中心','分省公司','陈静','020-31006006','已驳回',NULL,TRUE,'涉用户个人隐私','A,B','H',FALSE,'非管制','初始确权',NULL,NULL,NULL,NULL,'元数据质量评分 60 低于 80,自动驳回',CURRENT_TIMESTAMP,0),
('CA-006','QQ-0006','SYS:人力资源系统','人力资源系统','数据产品经营权','新增库表首次确权','中国南方电网有限责任公司','人力资源部','公司总部','刘洋','020-31007007','草稿',NULL,TRUE,'涉员工隐私,待补充授权','A','H',TRUE,'非管制','确权变更','数据新增','人力资源系统#v1',1,'数据新增:新增 1 张库表首次确权登记(既有已确权库表不动,不联动授权)',NULL,CURRENT_TIMESTAMP,0);

-- 确权汇总表(表3/表4,对应已完成申请)
INSERT INTO IM_CONFIRM_SUMMARY (CEC_SUMMARY_ID,CEC_APPLY_ID,CEC_SUMMARY_TYPE,CEC_CONTENT,CEC_GENERATOR_ID,CEC_GENERATE_TIME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('SM-001','CA-001','表3 数据确权信息汇总表','客户用电信息表确权汇总:持有权归广东电网','合规管控小组',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),
('SM-002','CA-001','表4 数据权益内部管理汇总表','内部管理:责任部门数字化部,有效期3年','合规管控小组',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0);

-- 确权材料(4)
INSERT INTO IM_CONFIRM_MATERIAL (CEC_MATERIAL_ID,CEC_APPLY_ID,CEC_MATERIAL_NAME,CEC_MATERIAL_TYPE,CEC_FILE_URL,CEC_OWNER,CEC_UPLOAD_TIME,CEC_CHECK_RESULT,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('MT-001','CA-001','确权证明.pdf','确权证明','/files/mt001.pdf','广东电网',CURRENT_TIMESTAMP,'通过',CURRENT_TIMESTAMP,0),
('MT-002','CA-001','用户授权函.pdf','授权函','/files/mt002.pdf','广东电网',CURRENT_TIMESTAMP,'通过',CURRENT_TIMESTAMP,0),
('MT-003','CA-004','采购协议.pdf','采购协议','/files/mt003.pdf','广西电网',CURRENT_TIMESTAMP,'待校验',CURRENT_TIMESTAMP,0),
('MT-004','CA-003','权属说明.docx','权属说明','/files/mt004.docx','深圳供电局',CURRENT_TIMESTAMP,'不通过',CURRENT_TIMESTAMP,0);

-- 权益卡片(7,库表级·三权分置)。对齐附录F 表4:权益卡片打在每一张数据资产卡片(库表)上,粒度=(系统×库表×单一权属)。
-- CEC_ASSET_ID=SYS:系统名(系统名列),CEC_ASSET_NAME=库表名,CEC_TABLE_CODE=库表代码,CEC_SCOPE=系统/库表。
-- 演示:营销「用户用电信息表」三权分置(持有/使用/经营,经营权 60 天到期);协同办公「公文流转表」确权变更 v2 取代 v1(旧卡失效);生产「设备台账表」冻结卡。
-- 表4 权益要素逐字段:CEC_SCHEMA_NAME 模式 / CEC_RIGHTS_CONTENT 权益内容摘要 / CEC_RIGHTS_CREDENTIAL 权益凭证 / CEC_ACQUIRE_MODE 取得方式(认定) / CEC_AUTH_UNIT 授权单位(认定为空) / CEC_CONFIRM_TIME 确权时间。
INSERT INTO IM_EQUITY_CARD_INFO (CEC_CARD_ID,CEC_CARD_NO,CEC_APPLY_ID,CEC_ASSET_ID,CEC_ASSET_NAME,CEC_TABLE_CODE,CEC_SCHEMA_NAME,CEC_RIGHT_TYPE,CEC_RIGHT_OWNER,CEC_RIGHT_SOURCE,CEC_RIGHTS_CONTENT,CEC_RIGHTS_CREDENTIAL,CEC_ACQUIRE_MODE,CEC_AUTH_UNIT,CEC_CONFIRM_TIME,CEC_SCOPE,CEC_VALID_DATE,CEC_CARD_STATUS,CEC_CONSOLIDATED_UNIT,CEC_VERSION,CEC_SUPERSEDED_NO,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CARD-001','EC-PRA-0001','CA-001','SYS:营销管理系统','用户用电信息表','MKT_BILL_CONS','BILLING','数据资源持有权','广东电网有限责任公司','确权认定','对「用户用电信息表」享有数据资源持有权(系统建设投入形成,依法持有、管理、处置)','确权认定资料(确权单 QQ-0001);含第三方权益证明/授权说明','认定',NULL,DATEADD('DAY',-60,CURRENT_TIMESTAMP),'营销管理系统 / 用户用电信息表',DATEADD('YEAR',3,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0),
('CARD-002','EC-PRA-0002','CA-001','SYS:营销管理系统','用户用电信息表','MKT_BILL_CONS','BILLING','数据加工使用权','广东电网有限责任公司','确权认定','对「用户用电信息表」享有数据加工使用权(在确权约束与授权范围内加工使用)','确权认定资料(确权单 QQ-0001);含第三方权益证明/授权说明','认定',NULL,DATEADD('DAY',-60,CURRENT_TIMESTAMP),'营销管理系统 / 用户用电信息表',DATEADD('YEAR',3,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0),
('CARD-003','EC-PRA-0003','CA-001','SYS:营销管理系统','用户用电信息表','MKT_BILL_CONS','BILLING','数据产品经营权','广东电网有限责任公司','确权认定','对「用户用电信息表」享有数据产品经营权(对外经营依公司对外开放目录与授权)','确权认定资料(确权单 QQ-0001);含第三方权益证明/授权说明','认定',NULL,DATEADD('DAY',-60,CURRENT_TIMESTAMP),'营销管理系统 / 用户用电信息表',DATEADD('DAY',60,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0),
('CARD-004','EC-PRA-0004','CA-002','SYS:计量自动化系统','计量点表','AMR_PT_METER','POINT','数据资源持有权','广东电网有限责任公司','确权认定','对「计量点表」享有数据资源持有权(系统建设投入形成,依法持有、管理、处置)','确权认定资料(确权单 QQ-0002)','认定',NULL,DATEADD('DAY',-60,CURRENT_TIMESTAMP),'计量自动化系统 / 计量点表',DATEADD('YEAR',3,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0),
('CARD-005','EC-PRA-0005','CA-004','SYS:协同办公系统','公文流转表','OA_DOC_FLOW','DOC','数据资源持有权','中国南方电网有限责任公司','确权变更认定','对「公文流转表」享有数据资源持有权(系统建设投入形成,依法持有、管理、处置)','确权认定资料(确权单 QQ-0004);确权变更 v2','认定',NULL,DATEADD('DAY',-10,CURRENT_TIMESTAMP),'协同办公系统 / 公文流转表',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',2,'EC-PRA-0005-OLD',CURRENT_TIMESTAMP,0),
('CARD-006','EC-PRA-0006','CA-003','SYS:生产管理系统','设备台账表','PMS_DEV_LEDGER','ASSET','数据资源持有权','广西电网有限责任公司','确权认定','对「设备台账表」享有数据资源持有权(系统建设投入形成,依法持有、管理、处置)','确权认定资料(确权单 QQ-0003)','认定',NULL,DATEADD('DAY',-90,CURRENT_TIMESTAMP),'生产管理系统 / 设备台账表',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'冻结','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0),
('CARD-007','EC-PRA-0005-OLD','CA-004','SYS:协同办公系统','公文流转表','OA_DOC_FLOW','DOC','数据资源持有权','中国南方电网有限责任公司','确权认定','对「公文流转表」享有数据资源持有权(被 v2 取代)','确权认定资料(确权单 QQ-0004)','认定',NULL,DATEADD('YEAR',-1,CURRENT_TIMESTAMP),'协同办公系统 / 公文流转表',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'失效','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0),
-- 可授资源池扩充:跨 5 系统 8 库表的库表级生效卡片(使用权 + 经营权),令批量授权 picker 呈现更丰富的可授数据表。
-- 对应库表均已在 DataCatalogService.CONFIRMED(确权目录标记已确权);经营权卡片走对外开放目录裁剪(gateway 桩放行)。
('CARD-008','EC-PRA-0008','CA-001','SYS:营销管理系统','市场交易结算表','MKT_TRADE_SETTLE','TRADE','数据加工使用权','广东电网有限责任公司','确权认定','对「市场交易结算表」享有数据加工使用权(在确权约束与授权范围内加工使用)','确权认定资料(确权单 QQ-0008);涉第三方来源(E)/商业秘密','认定',NULL,DATEADD('DAY',-45,CURRENT_TIMESTAMP),'营销管理系统 / 市场交易结算表',DATEADD('YEAR',3,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0),
('CARD-009','EC-PRA-0009','CA-001','SYS:营销管理系统','市场交易结算表','MKT_TRADE_SETTLE','TRADE','数据产品经营权','广东电网有限责任公司','确权认定','对「市场交易结算表」享有数据产品经营权(对外经营依公司对外开放目录与授权)','确权认定资料(确权单 QQ-0008);涉第三方来源(E)/商业秘密','认定',NULL,DATEADD('DAY',-45,CURRENT_TIMESTAMP),'营销管理系统 / 市场交易结算表',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0),
('CARD-010','EC-PRA-0010','CA-001','SYS:营销管理系统','电费账单表','MKT_BILL_INVOICE','BILLING','数据加工使用权','广东电网有限责任公司','确权认定','对「电费账单表」享有数据加工使用权(在确权约束与授权范围内加工使用)','确权认定资料(确权单 QQ-0010)','认定',NULL,DATEADD('DAY',-40,CURRENT_TIMESTAMP),'营销管理系统 / 电费账单表',DATEADD('YEAR',3,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0),
('CARD-011','EC-PRA-0011','CA-001','SYS:营销管理系统','电费账单表','MKT_BILL_INVOICE','BILLING','数据产品经营权','广东电网有限责任公司','确权认定','对「电费账单表」享有数据产品经营权(对外经营依公司对外开放目录与授权)','确权认定资料(确权单 QQ-0010)','认定',NULL,DATEADD('DAY',-40,CURRENT_TIMESTAMP),'营销管理系统 / 电费账单表',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0),
('CARD-012','EC-PRA-0012','CA-002','SYS:计量自动化系统','计量点表','AMR_PT_METER','POINT','数据加工使用权','广东电网有限责任公司','确权认定','对「计量点表」享有数据加工使用权(在确权约束与授权范围内加工使用)','确权认定资料(确权单 QQ-0002)','认定',NULL,DATEADD('DAY',-55,CURRENT_TIMESTAMP),'计量自动化系统 / 计量点表',DATEADD('YEAR',3,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0),
('CARD-013','EC-PRA-0013','CA-002','SYS:计量自动化系统','负荷曲线表','AMR_LOAD_CURVE','LOAD','数据加工使用权','广东电网有限责任公司','确权认定','对「负荷曲线表」享有数据加工使用权(在确权约束与授权范围内加工使用)','确权认定资料(确权单 QQ-0013)','认定',NULL,DATEADD('DAY',-35,CURRENT_TIMESTAMP),'计量自动化系统 / 负荷曲线表',DATEADD('YEAR',3,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0),
('CARD-014','EC-PRA-0014','CA-005','SYS:客户服务系统','客服工单表','CS_WORK_ORDER','ORDER','数据加工使用权','广东电网有限责任公司','确权认定','对「客服工单表」享有数据加工使用权(在确权约束与授权范围内加工使用;涉个人隐私)','确权认定资料(确权单 QQ-0014);涉个人隐私(H)','认定',NULL,DATEADD('DAY',-30,CURRENT_TIMESTAMP),'客户服务系统 / 客服工单表',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0),
('CARD-015','EC-PRA-0015','CA-004','SYS:协同办公系统','公文流转表','OA_DOC_FLOW','DOC','数据加工使用权','中国南方电网有限责任公司','确权认定','对「公文流转表」享有数据加工使用权(在确权约束与授权范围内加工使用)','确权认定资料(确权单 QQ-0004)','认定',NULL,DATEADD('DAY',-10,CURRENT_TIMESTAMP),'协同办公系统 / 公文流转表',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',2,NULL,CURRENT_TIMESTAMP,0),
('CARD-016','EC-PRA-0016','CA-004','SYS:协同办公系统','公文流转表','OA_DOC_FLOW','DOC','数据产品经营权','中国南方电网有限责任公司','确权认定','对「公文流转表」享有数据产品经营权(对外经营依公司对外开放目录与授权)','确权认定资料(确权单 QQ-0004)','认定',NULL,DATEADD('DAY',-10,CURRENT_TIMESTAMP),'协同办公系统 / 公文流转表',DATEADD('YEAR',2,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',2,NULL,CURRENT_TIMESTAMP,0),
('CARD-017','EC-PRA-0017','CA-006','SYS:人力资源系统','培训记录表','HR_TRAIN','TRAIN','数据加工使用权','中国南方电网有限责任公司','确权认定','对「培训记录表」享有数据加工使用权(在确权约束与授权范围内加工使用)','确权认定资料(确权单 QQ-0017)','认定',NULL,DATEADD('DAY',-20,CURRENT_TIMESTAMP),'人力资源系统 / 培训记录表',DATEADD('YEAR',3,CURRENT_TIMESTAMP),'正常','中国南方电网有限责任公司',1,NULL,CURRENT_TIMESTAMP,0);

-- 权益卡片变更历史(对应卡片)
INSERT INTO IM_EQUITY_CARD_LOG (CEC_LOG_ID,CEC_CARD_ID,CEC_ACTION,CEC_FROM_STATUS,CEC_TO_STATUS,CEC_REASON,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CL-001','CARD-001','生成',NULL,'正常','确权终审通过自动制卡',CURRENT_TIMESTAMP,0),
('CL-002','CARD-006','生成',NULL,'正常','确权终审通过自动制卡',CURRENT_TIMESTAMP,0),
('CL-003','CARD-006','冻结','正常','冻结','权属争议冻结',CURRENT_TIMESTAMP,0),
('CL-004','CARD-007','注销','正常','失效','确权变更:旧版卡片转历史(被 EC-PRA-0005 v2 取代)',CURRENT_TIMESTAMP,0),
('CL-005','CARD-005','生成',NULL,'正常','确权变更终审通过,生成 v2 权益卡片',CURRENT_TIMESTAMP,0);

-- 权益证书模板(3)
INSERT INTO IM_EQUITY_CERT_TEMPLATE (CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_TEMPLATE_VERSION,CEC_RIGHT_TYPE,CEC_TEMPLATE_STATUS,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('TPL-001','数据持有权证书模板','v1','数据资源持有权','生效中',CURRENT_TIMESTAMP,0),
('TPL-002','数据加工使用权证书模板','v1','数据加工使用权','生效中',CURRENT_TIMESTAMP,0),
('TPL-003','数据产品经营权证书模板','v1','数据产品经营权','停用',CURRENT_TIMESTAMP,0);

-- 权益证书(3)
INSERT INTO IM_EQUITY_CERT (CEC_CERT_ID,CEC_CERT_NO,CEC_CARD_ID,CEC_ISSUE_UNIT,CEC_ISSUE_TIME,CEC_CERT_STATUS,CEC_TEMPLATE_ID,CEC_TEMPLATE_NAME,CEC_CREATE_TIME,CEC_DEL_FLAG) VALUES
('CERT-001','QZ-PRA-0001','CARD-001','中国南方电网有限责任公司',CURRENT_TIMESTAMP,'生效','TPL-001','数据持有权证书模板',CURRENT_TIMESTAMP,0),
('CERT-002','QZ-PRA-0002','CARD-002','中国南方电网有限责任公司',CURRENT_TIMESTAMP,'生效','TPL-002','数据加工使用权证书模板',CURRENT_TIMESTAMP,0),
('CERT-003','QZ-PRA-0006','CARD-004','中国南方电网有限责任公司',CURRENT_TIMESTAMP,'已注销','TPL-003','数据产品经营权证书模板',CURRENT_TIMESTAMP,0);
