-- =====================================================================
-- Seata AT 模式回滚日志表(仅 prod 分布式事务需要)
-- 达梦 DM8 版本
-- prod profile 三个服务均 seata.enabled=true(默认 AT 模式 + 数据源代理),
-- 按 Seata 规范:每个被代理的业务库都需建 undo_log。
-- dev/test(H2/禁用Seata)不需要本表。
-- 适配 Seata 1.5+ / 2.x。
-- 由 MySQL 版本转换: LONGBLOB→BLOB, DATETIME(6)→TIMESTAMP,
--   ENGINE/CHARSET→删除, UNIQUE KEY→CREATE UNIQUE INDEX
-- 执行:  disql PRM_DPR/PRM_DPR2024!@localhost:5236 @06_seata_undo_log_dm.sql
-- =====================================================================

CREATE TABLE undo_log (
  branch_id     BIGINT       NOT NULL,
  xid           VARCHAR(128) NOT NULL,
  context       VARCHAR(128) NOT NULL,
  rollback_info BLOB         NOT NULL,
  log_status    INT          NOT NULL,
  log_created   TIMESTAMP    NOT NULL,
  log_modified  TIMESTAMP    NOT NULL
);
COMMENT ON TABLE undo_log IS 'Seata AT 模式回滚日志';
COMMENT ON COLUMN undo_log.branch_id IS '分支事务ID';
COMMENT ON COLUMN undo_log.xid IS '全局事务ID';
COMMENT ON COLUMN undo_log.context IS 'undo_log 上下文(序列化方式等)';
COMMENT ON COLUMN undo_log.rollback_info IS '回滚信息';
COMMENT ON COLUMN undo_log.log_status IS '0=正常,1=防御';
COMMENT ON COLUMN undo_log.log_created IS '创建时间';
COMMENT ON COLUMN undo_log.log_modified IS '修改时间';

CREATE UNIQUE INDEX ux_undo_log ON undo_log (xid, branch_id);

-- 注:若各微服务连接【独立】DM8 库,则每个库都要建 undo_log。
-- 本模块 3 个服务共用 PRM_DPR 时,建一次即可。
