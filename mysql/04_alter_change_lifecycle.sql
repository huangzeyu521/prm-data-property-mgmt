-- =============================================================================
-- 增量迁移(MySQL):确权变更生命周期 + 人工预审AI快照 新增列
-- 适用对象:已上线、IM_CONFIRM_APPLY / IM_EQUITY_CARD_INFO 已存在数据的生产库。
-- 全新部署请直接用 01_schema.sql(已含这些列),无需执行本脚本。
--
-- 背景:01_schema.sql 的 CREATE TABLE IF NOT EXISTS 仅对新建库生效,不改动现网已存在的表;
--       故存量生产库须由 DBA 执行本 ALTER 脚本补列。
-- 幂等性:MySQL 8 的 ADD COLUMN 不支持 IF NOT EXISTS,本脚本仅可执行一次;
--        若列已存在会报 1060(Duplicate column name),可忽略该列对应语句继续。
-- 对应基线:prm-backend/dpr-confirm-service/src/main/resources/db/h2/schema.sql
-- =============================================================================

-- 1) 确权申请单:人工预审AI快照 + 变更触发类型
ALTER TABLE IM_CONFIRM_APPLY
  ADD COLUMN CEC_AI_SNAPSHOT   TEXT        COMMENT '提交前AI校验结果快照(JSON),供人工预审完整复核·可追溯' AFTER CEC_CONTACT,
  ADD COLUMN CEC_CHANGE_TRIGGER VARCHAR(50) COMMENT '变更触发类型(数据新增/数据来源变更/管理要求变更/权益到期/其他,附录F权益变更四类)' AFTER CEC_REGISTER_TYPE;

-- 2) 权益卡片:版本号 + 前序被取代卡片号(确权变更版本链)
ALTER TABLE IM_EQUITY_CARD_INFO
  ADD COLUMN CEC_VERSION       INT DEFAULT 1 COMMENT '卡片版本号(确权变更每取代一次+1,初始确权为1)' AFTER CEC_CONSOLIDATED_UNIT,
  ADD COLUMN CEC_SUPERSEDED_NO VARCHAR(64)  COMMENT '前序被取代卡片号(确权变更时指向上一版正常卡,形成版本链;初始确权为空)' AFTER CEC_VERSION;

-- 3) 确权申请材料:材料来源(平台同步/用户上传)——"先从平台元数据同步已上传材料、再补全"
ALTER TABLE IM_CONFIRM_MATERIAL
  ADD COLUMN CEC_SOURCE VARCHAR(20) COMMENT '材料来源(平台同步/用户上传)' AFTER CEC_FILE_DATA;

-- 4) 存量回填:已有权益卡片版本号默认置 1
UPDATE IM_EQUITY_CARD_INFO SET CEC_VERSION = 1 WHERE CEC_VERSION IS NULL;

-- 5) 大模型校验机制完善(南网"全流程留痕追溯"):新增 AI 操作留痕表(逐次留痕 + SM3 防篡改 + 回放)。
CREATE TABLE IF NOT EXISTS IM_DPR_AI_RUNLOG (
  CEC_LOG_ID        VARCHAR(64)  NOT NULL COMMENT '留痕ID',
  CEC_APPLY_ID      VARCHAR(64)  NOT NULL COMMENT '申请ID',
  CEC_CAPABILITY    VARCHAR(40)  COMMENT '能力类型(智能解析/决策研判/冲突识别/材料校验)',
  CEC_MODEL         VARCHAR(64)  COMMENT '模型标识',
  CEC_INPUT_SUMMARY VARCHAR(2000) COMMENT '输入摘要',
  CEC_OUTPUT        LONGTEXT     COMMENT '模型输出(原样留痕)',
  CEC_DURATION_MS   BIGINT       COMMENT '耗时(ms)',
  CEC_SM3_HASH      VARCHAR(64)  COMMENT '输出SM3指纹(防篡改)',
  CEC_TRIGGER_USER  VARCHAR(64)  COMMENT '触发人',
  CEC_EVIDENCE_ID   VARCHAR(64)  COMMENT '关键节点上链存证ID',
  CEC_PROVINCE_CODE VARCHAR(32),
  CEC_BUREAU_CODE   VARCHAR(32),
  CEC_CREATOR_ID    VARCHAR(64),
  CEC_CREATE_TIME   DATETIME,
  CEC_UPDATER_ID    VARCHAR(64),
  CEC_UPDATE_TIME   DATETIME,
  CEC_DEL_FLAG      INT NOT NULL DEFAULT 0,
  PRIMARY KEY (CEC_LOG_ID),
  KEY IDX_AIRUNLOG_APPLY (CEC_APPLY_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大模型校验操作留痕';
