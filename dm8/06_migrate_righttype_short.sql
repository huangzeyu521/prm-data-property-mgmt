-- =====================================================================
-- 权益类型术语统一:长式 → 短式(持有权 / 使用权 / 经营权)存量数据迁移(达梦 DM8)。
-- 与代码/UI/种子保持一致(数据资源持有权·数据加工使用权·数据产品经营权 → 持有权·使用权·经营权)。
-- 幂等:基于 REPLACE,可重复执行;支持顿号拼接多值列。
-- 适用:已存在长式数据的运行库(全新库由 03_data_dm.sql 直接落短式,无需执行本脚本)。
-- 两种持有权变体(数据资源持有权/数据持有权)均收敛为"持有权"。
-- =====================================================================

-- CEC_RIGHT_TYPE 列(11 张表)
UPDATE IM_PROPERTY_ARCHIVE     SET CEC_RIGHT_TYPE = REPLACE(REPLACE(REPLACE(REPLACE(CEC_RIGHT_TYPE,'数据资源持有权','持有权'),'数据持有权','持有权'),'数据加工使用权','使用权'),'数据产品经营权','经营权') WHERE CEC_RIGHT_TYPE LIKE '%数据%';
UPDATE IM_CONFIRM_APPLY        SET CEC_RIGHT_TYPE = REPLACE(REPLACE(REPLACE(REPLACE(CEC_RIGHT_TYPE,'数据资源持有权','持有权'),'数据持有权','持有权'),'数据加工使用权','使用权'),'数据产品经营权','经营权') WHERE CEC_RIGHT_TYPE LIKE '%数据%';
UPDATE IM_EQUITY_CARD_INFO     SET CEC_RIGHT_TYPE = REPLACE(REPLACE(REPLACE(REPLACE(CEC_RIGHT_TYPE,'数据资源持有权','持有权'),'数据持有权','持有权'),'数据加工使用权','使用权'),'数据产品经营权','经营权') WHERE CEC_RIGHT_TYPE LIKE '%数据%';
UPDATE IM_EQUITY_CERT_TEMPLATE SET CEC_RIGHT_TYPE = REPLACE(REPLACE(REPLACE(REPLACE(CEC_RIGHT_TYPE,'数据资源持有权','持有权'),'数据持有权','持有权'),'数据加工使用权','使用权'),'数据产品经营权','经营权') WHERE CEC_RIGHT_TYPE LIKE '%数据%';
UPDATE IM_AIT_PARSE_RESULT     SET CEC_RIGHT_TYPE = REPLACE(REPLACE(REPLACE(REPLACE(CEC_RIGHT_TYPE,'数据资源持有权','持有权'),'数据持有权','持有权'),'数据加工使用权','使用权'),'数据产品经营权','经营权') WHERE CEC_RIGHT_TYPE LIKE '%数据%';
UPDATE IM_AIT_KG_CLAIM         SET CEC_RIGHT_TYPE = REPLACE(REPLACE(REPLACE(REPLACE(CEC_RIGHT_TYPE,'数据资源持有权','持有权'),'数据持有权','持有权'),'数据加工使用权','使用权'),'数据产品经营权','经营权') WHERE CEC_RIGHT_TYPE LIKE '%数据%';
UPDATE IM_AIT_AUDIT_RESULT     SET CEC_RIGHT_TYPE = REPLACE(REPLACE(REPLACE(REPLACE(CEC_RIGHT_TYPE,'数据资源持有权','持有权'),'数据持有权','持有权'),'数据加工使用权','使用权'),'数据产品经营权','经营权') WHERE CEC_RIGHT_TYPE LIKE '%数据%';
UPDATE IM_AUTH_APPLY           SET CEC_RIGHT_TYPE = REPLACE(REPLACE(REPLACE(REPLACE(CEC_RIGHT_TYPE,'数据资源持有权','持有权'),'数据持有权','持有权'),'数据加工使用权','使用权'),'数据产品经营权','经营权') WHERE CEC_RIGHT_TYPE LIKE '%数据%';
UPDATE IM_AUTH_CERT            SET CEC_RIGHT_TYPE = REPLACE(REPLACE(REPLACE(REPLACE(CEC_RIGHT_TYPE,'数据资源持有权','持有权'),'数据持有权','持有权'),'数据加工使用权','使用权'),'数据产品经营权','经营权') WHERE CEC_RIGHT_TYPE LIKE '%数据%';
UPDATE IM_AUTH_CERT_TEMPLATE   SET CEC_RIGHT_TYPE = REPLACE(REPLACE(REPLACE(REPLACE(CEC_RIGHT_TYPE,'数据资源持有权','持有权'),'数据持有权','持有权'),'数据加工使用权','使用权'),'数据产品经营权','经营权') WHERE CEC_RIGHT_TYPE LIKE '%数据%';
UPDATE IM_AUTH_FILING          SET CEC_RIGHT_TYPE = REPLACE(REPLACE(REPLACE(REPLACE(CEC_RIGHT_TYPE,'数据资源持有权','持有权'),'数据持有权','持有权'),'数据加工使用权','使用权'),'数据产品经营权','经营权') WHERE CEC_RIGHT_TYPE LIKE '%数据%';

-- CEC_AGREEMENT_TYPE 列(协议类型:经营权协议存档检索)
UPDATE IM_AUTH_AGREEMENT       SET CEC_AGREEMENT_TYPE = REPLACE(REPLACE(REPLACE(REPLACE(CEC_AGREEMENT_TYPE,'数据资源持有权','持有权'),'数据持有权','持有权'),'数据加工使用权','使用权'),'数据产品经营权','经营权') WHERE CEC_AGREEMENT_TYPE LIKE '%数据%';

COMMIT;
