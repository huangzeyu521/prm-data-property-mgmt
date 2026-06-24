-- =============================================================================
-- 组织机构主数据(只读镜像表)SYS_ORGANIZATION —— MySQL
-- =============================================================================
-- 背景:PRM 的部门/归口下拉、Dashboard 部门筛选、制卡/发证省地市编码回填,
--       统一消费平台组织主数据 DAMS.SYS_ORGANIZATION 的 PRM 子集。
--       本表结构供 PRM 自有库承载该镜像;数据由平台/4A 同步写入,PRM 只读不写。
-- 列名:沿用平台主数据原生列名(ID / BIZ_ORG_* / PARENT_ID / CITY_CODE …),
--       不加 CEC_ 前缀(非 PRM 自产业务数据,便于与平台同步映射)。
-- 幂等:CREATE TABLE IF NOT EXISTS,可重复执行。
-- 映射:resolve() 上溯 PARENT_ID 取省(BIZ_ORG_CODE)/ 地市(CITY_CODE||BIZ_ORG_CODE)。
-- =============================================================================

CREATE TABLE IF NOT EXISTS SYS_ORGANIZATION (
  ID            VARCHAR(60)  NOT NULL COMMENT '组织主键(平台 SYS_ORGANIZATION.ID)',
  BIZ_ORG_ID    VARCHAR(32)           COMMENT '业务组织ID',
  BIZ_ORG_NAME  VARCHAR(200)          COMMENT '组织名称',
  BIZ_ORG_CODE  VARCHAR(60)           COMMENT '组织编码(省级取此作 province_code)',
  SHORT_NAME    VARCHAR(200)          COMMENT '组织简称',
  PARENT_ID     VARCHAR(60)           COMMENT '上级组织ID(组织树 / 归口上溯)',
  ORG_LEVEL     VARCHAR(32)           COMMENT '组织层级(网级/省级/地市)',
  ORG_TYPE      VARCHAR(32)           COMMENT '组织类型',
  CITY_CODE     VARCHAR(32)           COMMENT '地市编码(地市级取此作 bureau_code)',
  SORT_NO       VARCHAR(30)           COMMENT '排序号',
  BASE_ORG_CODE VARCHAR(120)          COMMENT '基准组织编码',
  PRIMARY KEY (ID),
  KEY IDX_SYS_ORG_PARENT (PARENT_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织机构主数据(只读镜像,平台/4A 同步,PRM 不写)';
