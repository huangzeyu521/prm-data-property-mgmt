-- =====================================================================
-- 达梦 DM8 初始化:表空间 + 用户 + 字符集
-- 数据产权管理模块 (IM-DAM-DPR)
-- 此脚本在容器首次初始化时自动执行
-- 执行:  cd dm8 && disql sysdba/Sysdba001@localhost:5236 @01_create_tablespace.sql
-- =====================================================================

-- 设置会话字符集(确保 UTF8)
ALTER SESSION SET NLS_CHARACTERSET = 'UTF8';

-- 创建表空间(根据实际磁盘调整数据文件路径与大小)
CREATE TABLESPACE PRM_DPR_TBS
  DATAFILE 'PRM_DPR.dbf'
  SIZE 2048
  AUTOEXTEND ON
  NEXT 512
  MAXSIZE 32768
  CACHE = NORMAL;

-- 创建用户(生产环境请替换为强密码)
CREATE USER PRM_DPR
  IDENTIFIED BY "PRM_DPR2024!"
  DEFAULT TABLESPACE PRM_DPR_TBS
  DEFAULT INDEX TABLESPACE PRM_DPR_TBS;

-- 授予必要权限
GRANT RESOURCE, DBA TO PRM_DPR;
GRANT CREATE TABLE, CREATE VIEW, CREATE PROCEDURE, CREATE SEQUENCE, CREATE TRIGGER TO PRM_DPR;

-- 后续依次执行建表与数据脚本:
-- disql PRM_DPR/PRM_DPR2024!@localhost:5236 @02_schema_dm.sql
-- disql PRM_DPR/PRM_DPR2024!@localhost:5236 @03_data_dm.sql
-- (可选) 智能工具独立库表:
-- disql PRM_DPR/PRM_DPR2024!@localhost:5236 @04_schema_aitool_dm.sql
-- disql PRM_DPR/PRM_DPR2024!@localhost:5236 @05_seed_aitool_dm.sql
-- (prod 可选) Seata AT 模式 undo_log:
-- disql PRM_DPR/PRM_DPR2024!@localhost:5236 @06_seata_undo_log_dm.sql

-- 一键执行全部:
-- disql PRM_DPR/PRM_DPR2024!@localhost:5236 \`02_schema_dm.sql; \`03_data_dm.sql

