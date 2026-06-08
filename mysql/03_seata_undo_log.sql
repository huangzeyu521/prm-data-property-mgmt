-- =====================================================================
-- Seata AT 模式回滚日志表(仅 prod 分布式事务需要)
-- prod profile 三个服务均 seata.enabled=true(默认 AT 模式 + 数据源代理),
-- 按 Seata 规范:每个被代理的业务库都需建 undo_log。
-- dev/test/dm(SEATA_ENABLED=false)不需要本表。
-- 适配 Seata 1.5+ / 2.x。
-- 执行:  mysql -u root -p prm_dpr < 03_seata_undo_log.sql
-- =====================================================================
USE prm_dpr;

CREATE TABLE IF NOT EXISTS undo_log (
  branch_id     BIGINT       NOT NULL COMMENT '分支事务ID',
  xid           VARCHAR(128) NOT NULL COMMENT '全局事务ID',
  context       VARCHAR(128) NOT NULL COMMENT 'undo_log 上下文(序列化方式等)',
  rollback_info LONGBLOB     NOT NULL COMMENT '回滚信息',
  log_status    INT          NOT NULL COMMENT '0=正常,1=防御',
  log_created   DATETIME(6)  NOT NULL COMMENT '创建时间',
  log_modified  DATETIME(6)  NOT NULL COMMENT '修改时间',
  UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT 模式回滚日志';

-- 注:若各微服务连接【独立】MySQL 库,则每个库都要建 undo_log。
-- 本模块 3 个服务共用 prm_dpr 时,建一次即可。
