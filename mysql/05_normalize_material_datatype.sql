-- =============================================================================
-- 资料类型(CEC_DATA_TYPE / 联调清单 行25)规范化:中文 → 编码 01–07
-- =============================================================================
-- 背景:智能确权材料归类历史上把中文类别词(元数据/制度附件/…)直接写入
--       IM_AIT_MATERIAL.CEC_CATEGORY(母平台 PDM 对应列 CEC_DATA_TYPE)。
--       规范化最终值要求该列存编码 01–07;代码已改为落编码,本脚本回填存量行。
-- 幂等:仅命中中文存量行,已是编码的行不受影响,可重复执行。
-- 映射:01 元数据 / 02 制度附件 / 03 授权材料 / 04 合同材料 / 05 来源说明 / 06 确权证明 / 07 其他
-- =============================================================================

UPDATE IM_AIT_MATERIAL SET CEC_CATEGORY = '01' WHERE CEC_CATEGORY = '元数据';
UPDATE IM_AIT_MATERIAL SET CEC_CATEGORY = '02' WHERE CEC_CATEGORY = '制度附件';
UPDATE IM_AIT_MATERIAL SET CEC_CATEGORY = '03' WHERE CEC_CATEGORY = '授权材料';
UPDATE IM_AIT_MATERIAL SET CEC_CATEGORY = '04' WHERE CEC_CATEGORY = '合同材料';
UPDATE IM_AIT_MATERIAL SET CEC_CATEGORY = '05' WHERE CEC_CATEGORY = '来源说明';
UPDATE IM_AIT_MATERIAL SET CEC_CATEGORY = '06' WHERE CEC_CATEGORY = '确权证明';
UPDATE IM_AIT_MATERIAL SET CEC_CATEGORY = '07' WHERE CEC_CATEGORY = '其他';

ALTER TABLE IM_AIT_MATERIAL MODIFY COLUMN CEC_CATEGORY VARCHAR(50)
  COMMENT '资料类型编码(CEC_DATA_TYPE):01元数据/02制度附件/03授权材料/04合同材料/05来源说明/06确权证明/07其他';
