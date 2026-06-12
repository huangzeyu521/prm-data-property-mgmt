-- ============================================================================
-- 智能确权辅助工具 演示种子数据(可选):一份已解析材料 + 两条对立权属主张 + 一条主体冲突
-- 让 材料解析/冲突识别/决策支持 三页开箱即有内容;生产环境跳过本脚本。
-- ============================================================================
USE prm;

INSERT INTO IM_AIT_MATERIAL
  (CEC_MATERIAL_ID, CEC_BATCH_NO, CEC_APPLY_ID, CEC_FILE_NAME, CEC_FILE_TYPE, CEC_FILE_HASH,
   CEC_SIZE_KB, CEC_PARSE_STATUS, CEC_PROGRESS, CEC_CONTENT, CEC_CREATE_TIME, CEC_DEL_FLAG)
VALUES
  ('AITM-SEED-0001', 'BATCH-SEED01', NULL, 'AST-001-确权证明-盖好.pdf', 'PDF',
   'seedhash00000000000000000000000000000000000000000000000000000001', 156, '成功', 100,
   '兹证明客户用电信息表由广东电网有限责任公司自行生产,权利类型为数据资源持有权,有效期3年,范围全字段,已盖章。',
   NOW(), 0);

INSERT INTO IM_AIT_PARSE_RESULT
  (CEC_PARSE_ID, CEC_MATERIAL_ID, CEC_RIGHT_SUBJECT, CEC_RIGHT_OBJECT, CEC_RIGHT_TYPE, CEC_RIGHT_TERM,
   CEC_AUTH_SCOPE, CEC_DATA_SOURCE, CEC_SENSITIVE_TYPE, CEC_SEAL_VALID, CEC_SEAL_DESC,
   CEC_CONFIDENCE, CEC_REVIEW_STATUS, CEC_TRUST_SCORE, CEC_TRUST_LEVEL, CEC_CREATE_TIME, CEC_DEL_FLAG)
VALUES
  ('AITP-SEED-0001', 'AITM-SEED-0001', '广东电网有限责任公司', '客户用电信息表', '数据持有权', '3年',
   '全字段', '自行生产', '个人信息', '有效', '正文含盖章表述,CV×OCR交叉校验通过',
   0.95, '自动通过', 88, '可信', NOW(), 0);

INSERT INTO IM_AIT_KG_CLAIM
  (CEC_CLAIM_ID, CEC_ASSET_ID, CEC_SUBJECT, CEC_RIGHT_TYPE, CEC_AUTH_SCOPE, CEC_VALID_DATE,
   CEC_EXCLUSIVE, CEC_SOURCE_TYPE, CEC_CREATE_TIME, CEC_DEL_FLAG)
VALUES
  ('AITC-SEED-0001', 'AST-001', '深圳供电局', '所有权', '全字段',
   DATE_ADD(NOW(), INTERVAL 1 YEAR), 1, '历史确权', NOW(), 0),
  ('AITC-SEED-0002', 'AST-001', '广东电网有限责任公司', '数据持有权', '全字段',
   DATE_ADD(NOW(), INTERVAL 2 YEAR), 1, '当前申请', NOW(), 0);

INSERT INTO IM_AIT_CONFLICT
  (CEC_CONFLICT_ID, CEC_ASSET_ID, CEC_CONFLICT_TYPE, CEC_CONFLICT_SOURCE, CEC_CONFLICT_DESC,
   CEC_IMPACT_SCOPE, CEC_RISK_LEVEL, CEC_SUGGESTION, CEC_STATUS, CEC_CREATE_TIME, CEC_DEL_FLAG)
VALUES
  ('AITX-SEED-0001', 'AST-001', '主体冲突', '历史确权',
   '客体 AST-001 被多主体声明持有类权利:深圳供电局(所有权,全字段,排他) 与 广东电网有限责任公司(数据持有权,全字段,排他)',
   '深圳供电局;广东电网有限责任公司', '高',
   '①组织争议主体补充权属证明;②依三权分置协商划分(持有权归唯一主体);③划分结论存证后再确权',
   '待处置', NOW(), 0);
