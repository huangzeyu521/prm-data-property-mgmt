-- =============================================================================
-- 组织机构主数据(只读镜像表)SYS_ORGANIZATION —— 达梦 DM8
-- =============================================================================
-- 背景:PRM 的部门/归口下拉、Dashboard 部门筛选、制卡/发证省地市编码回填,
--       统一消费平台组织主数据 DAMS.SYS_ORGANIZATION 的 PRM 子集。
--       本表结构供 PRM 自有库承载该镜像;数据由平台/4A 同步写入,PRM 只读不写。
-- 列名:沿用平台主数据原生列名(ID / BIZ_ORG_* / PARENT_ID / CITY_CODE …),
--       不加 CEC_ 前缀(非 PRM 自产业务数据,便于与平台同步映射)。
-- 幂等:达梦不支持 CREATE TABLE IF NOT EXISTS;若对象已存在报错则忽略本段继续。
-- 映射:resolve() 上溯 PARENT_ID 取省(BIZ_ORG_CODE)/ 地市(CITY_CODE||BIZ_ORG_CODE)。
-- =============================================================================

CREATE TABLE SYS_ORGANIZATION (
  ID            VARCHAR(60)  NOT NULL,
  BIZ_ORG_ID    VARCHAR(32),
  BIZ_ORG_NAME  VARCHAR(200),
  BIZ_ORG_CODE  VARCHAR(60),
  SHORT_NAME    VARCHAR(200),
  PARENT_ID     VARCHAR(60),
  ORG_LEVEL     VARCHAR(32),
  ORG_TYPE      VARCHAR(32),
  CITY_CODE     VARCHAR(32),
  SORT_NO       VARCHAR(30),
  BASE_ORG_CODE VARCHAR(120),
  CONSTRAINT PK_SYS_ORGANIZATION PRIMARY KEY (ID)
);

CREATE INDEX IDX_SYS_ORG_PARENT ON SYS_ORGANIZATION (PARENT_ID);

COMMENT ON TABLE  SYS_ORGANIZATION              IS '组织机构主数据(只读镜像,平台/4A 同步,PRM 不写)';
COMMENT ON COLUMN SYS_ORGANIZATION.ID            IS '组织主键(平台 SYS_ORGANIZATION.ID)';
COMMENT ON COLUMN SYS_ORGANIZATION.BIZ_ORG_ID    IS '业务组织ID';
COMMENT ON COLUMN SYS_ORGANIZATION.BIZ_ORG_NAME  IS '组织名称';
COMMENT ON COLUMN SYS_ORGANIZATION.BIZ_ORG_CODE  IS '组织编码(省级取此作 province_code)';
COMMENT ON COLUMN SYS_ORGANIZATION.SHORT_NAME    IS '组织简称';
COMMENT ON COLUMN SYS_ORGANIZATION.PARENT_ID     IS '上级组织ID(组织树 / 归口上溯)';
COMMENT ON COLUMN SYS_ORGANIZATION.ORG_LEVEL     IS '组织层级(网级/省级/地市)';
COMMENT ON COLUMN SYS_ORGANIZATION.ORG_TYPE      IS '组织类型';
COMMENT ON COLUMN SYS_ORGANIZATION.CITY_CODE     IS '地市编码(地市级取此作 bureau_code)';
COMMENT ON COLUMN SYS_ORGANIZATION.SORT_NO       IS '排序号';
COMMENT ON COLUMN SYS_ORGANIZATION.BASE_ORG_CODE IS '基准组织编码';
