-- =============================================================================
-- 增量迁移(达梦DM8):确权变更生命周期 + 人工预审AI快照 新增列
-- 适用对象:已上线、IM_CONFIRM_APPLY / IM_EQUITY_CARD_INFO 已存在数据的生产库。
-- 全新部署请直接用 02_schema_dm.sql(已含这些列),无需执行本脚本。
--
-- 背景:02_schema_dm.sql 的 CREATE TABLE 仅对新建库生效,不会改动现网已存在的表;
--       故存量生产库须由 DBA 执行本 ALTER 脚本补列。
-- 幂等性:达梦不支持 ADD COLUMN IF NOT EXISTS,本脚本仅可执行一次;
--        若列已存在会报"列名重复",可忽略该列对应语句继续。
-- 对应基线:prm-backend/dpr-confirm-service/src/main/resources/db/h2/schema.sql
-- =============================================================================

-- 1) 确权申请单:人工预审AI快照 + 变更触发类型
ALTER TABLE IM_CONFIRM_APPLY ADD CEC_AI_SNAPSHOT CLOB;
ALTER TABLE IM_CONFIRM_APPLY ADD CEC_CHANGE_TRIGGER VARCHAR(50);
COMMENT ON COLUMN IM_CONFIRM_APPLY.CEC_AI_SNAPSHOT IS '提交前AI校验结果快照(JSON),供人工预审完整复核·可追溯';
COMMENT ON COLUMN IM_CONFIRM_APPLY.CEC_CHANGE_TRIGGER IS '变更触发类型(数据新增/数据来源变更/管理要求变更/权益到期/其他,附录F权益变更四类)';

-- 2) 权益卡片:版本号 + 前序被取代卡片号(确权变更版本链)
ALTER TABLE IM_EQUITY_CARD_INFO ADD CEC_VERSION INT DEFAULT 1;
ALTER TABLE IM_EQUITY_CARD_INFO ADD CEC_SUPERSEDED_NO VARCHAR(64);
COMMENT ON COLUMN IM_EQUITY_CARD_INFO.CEC_VERSION IS '卡片版本号(确权变更每取代一次+1,初始确权为1)';
COMMENT ON COLUMN IM_EQUITY_CARD_INFO.CEC_SUPERSEDED_NO IS '前序被取代卡片号(确权变更时指向上一版正常卡,形成版本链;初始确权为空)';

-- 3) 确权申请材料:材料来源(平台同步/用户上传)——"先从平台元数据同步已上传材料、再补全"
ALTER TABLE IM_CONFIRM_MATERIAL ADD CEC_SOURCE VARCHAR(20);
COMMENT ON COLUMN IM_CONFIRM_MATERIAL.CEC_SOURCE IS '材料来源(平台同步/用户上传)';

-- 4) 存量回填:已有权益卡片版本号默认置 1(新增列 DEFAULT 仅对新行生效,存量行须显式回填)
UPDATE IM_EQUITY_CARD_INFO SET CEC_VERSION = 1 WHERE CEC_VERSION IS NULL;
