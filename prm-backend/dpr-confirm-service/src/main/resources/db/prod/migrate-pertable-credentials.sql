-- ============================================================================
-- P0–P3 逐表凭证迁移(35 号文 表2:B–J 凭证按数据表逐张承载)· 生产迁移脚本
-- 适用:达梦 DM8 / MySQL(prod 表结构由 DBA 管理,sql.init.mode=never,不随包自动执行)
-- 服务:dpr-confirm-service · 表:IM_CONFIRM_TABLE_ITEM / IM_CONFIRM_MATERIAL / IM_CONFIRM_MATERIAL_RULE
--
-- 变更内容:
--   ① IM_CONFIRM_TABLE_ITEM 增 10 列:逐表凭证附件名(CEC_*_ATT ×5)+ 逐表上传件引用(CEC_*_MID ×5)
--      —— 逐表凭证单一真源(step1 表2 / step2 逐表区 / step3 校验 / 审核侧 四处同源)
--   ② 清理历史"系统级 B–J"IM_CONFIRM_MATERIAL 记录:P3 后 B–F(非A)来源凭证 与 G–J 关联资料
--      不再落系统级材料表(按规则表 SOURCE≠A / RELATION 的材料名匹配);表1/表2/权属凭证/A 说明保留。
--
-- 执行须知:
--   * 执行前备份 IM_CONFIRM_MATERIAL(至少导出将删记录);
--   * 先跑【预检】确认删除范围与预期一致,再执行【清理】;
--   * 历史已归档/审结申请如需留痕审计,可将 DELETE 换为软删(UPDATE CEC_DEL_FLAG=1,若启用逻辑删)。
-- ============================================================================

-- ---------------------------------------------------------------------------
-- ① DDL:IM_CONFIRM_TABLE_ITEM 增列(DM8 与 MySQL 语法一致)
-- ---------------------------------------------------------------------------
ALTER TABLE IM_CONFIRM_TABLE_ITEM ADD CEC_SRC_ATT VARCHAR(300);  -- 来源凭证附件(来源 B–F)
ALTER TABLE IM_CONFIRM_TABLE_ITEM ADD CEC_G_ATT   VARCHAR(300);  -- G 行政监管关联资料附件
ALTER TABLE IM_CONFIRM_TABLE_ITEM ADD CEC_H_ATT   VARCHAR(300);  -- H 个人/家庭隐私关联资料附件
ALTER TABLE IM_CONFIRM_TABLE_ITEM ADD CEC_I_ATT   VARCHAR(300);  -- I 第三方商业机密关联资料附件
ALTER TABLE IM_CONFIRM_TABLE_ITEM ADD CEC_J_ATT   VARCHAR(300);  -- J 其他第三方机构协议关联资料附件
ALTER TABLE IM_CONFIRM_TABLE_ITEM ADD CEC_SRC_MID VARCHAR(64);   -- 来源凭证上传件 materialId(字节存 IM_CONFIRM_MATERIAL)
ALTER TABLE IM_CONFIRM_TABLE_ITEM ADD CEC_G_MID   VARCHAR(64);
ALTER TABLE IM_CONFIRM_TABLE_ITEM ADD CEC_H_MID   VARCHAR(64);
ALTER TABLE IM_CONFIRM_TABLE_ITEM ADD CEC_I_MID   VARCHAR(64);
ALTER TABLE IM_CONFIRM_TABLE_ITEM ADD CEC_J_MID   VARCHAR(64);

-- ---------------------------------------------------------------------------
-- ② 预检:将被清理的历史系统级 B–J 材料(先看数量与明细,确认后再删)
--    口径 = 确权场景规则中 (SOURCE 且 触发码≠A) 或 RELATION 的应交材料名
-- ---------------------------------------------------------------------------
SELECT COUNT(*) AS will_delete_count
FROM IM_CONFIRM_MATERIAL m
WHERE m.CEC_MATERIAL_NAME IN (
    SELECT r.CEC_MATERIAL_NAME FROM IM_CONFIRM_MATERIAL_RULE r
    WHERE r.CEC_SCENE = '确权'
      AND ( (r.CEC_TRIGGER_TYPE = 'SOURCE' AND r.CEC_TRIGGER_CODE <> 'A')
            OR r.CEC_TRIGGER_TYPE = 'RELATION' )
);

SELECT m.CEC_MATERIAL_ID, m.CEC_APPLY_ID, m.CEC_MATERIAL_NAME, m.CEC_SOURCE
FROM IM_CONFIRM_MATERIAL m
WHERE m.CEC_MATERIAL_NAME IN (
    SELECT r.CEC_MATERIAL_NAME FROM IM_CONFIRM_MATERIAL_RULE r
    WHERE r.CEC_SCENE = '确权'
      AND ( (r.CEC_TRIGGER_TYPE = 'SOURCE' AND r.CEC_TRIGGER_CODE <> 'A')
            OR r.CEC_TRIGGER_TYPE = 'RELATION' )
);

-- ---------------------------------------------------------------------------
-- ③ 清理:删除历史系统级 B–J 材料(逐表凭证已由 IM_CONFIRM_TABLE_ITEM 新列承载)
-- ---------------------------------------------------------------------------
DELETE FROM IM_CONFIRM_MATERIAL
WHERE CEC_MATERIAL_NAME IN (
    SELECT r.CEC_MATERIAL_NAME FROM IM_CONFIRM_MATERIAL_RULE r
    WHERE r.CEC_SCENE = '确权'
      AND ( (r.CEC_TRIGGER_TYPE = 'SOURCE' AND r.CEC_TRIGGER_CODE <> 'A')
            OR r.CEC_TRIGGER_TYPE = 'RELATION' )
);

-- ---------------------------------------------------------------------------
-- ④ 验证:应为 0;新列存在(DM8 用 USER_TAB_COLUMNS,MySQL 用 information_schema)
-- ---------------------------------------------------------------------------
SELECT COUNT(*) AS remain_bj_count
FROM IM_CONFIRM_MATERIAL m
WHERE m.CEC_MATERIAL_NAME IN (
    SELECT r.CEC_MATERIAL_NAME FROM IM_CONFIRM_MATERIAL_RULE r
    WHERE r.CEC_SCENE = '确权'
      AND ( (r.CEC_TRIGGER_TYPE = 'SOURCE' AND r.CEC_TRIGGER_CODE <> 'A')
            OR r.CEC_TRIGGER_TYPE = 'RELATION' )
);
-- DM8:   SELECT COLUMN_NAME FROM USER_TAB_COLUMNS WHERE TABLE_NAME='IM_CONFIRM_TABLE_ITEM' AND COLUMN_NAME LIKE 'CEC_%_ATT';
-- MySQL: SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_NAME='IM_CONFIRM_TABLE_ITEM' AND COLUMN_NAME LIKE 'CEC\_%\_ATT';
