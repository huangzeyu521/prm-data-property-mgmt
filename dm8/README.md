# 达梦 DM8 部署/迁移 SQL 执行手册

数据产权管理模块(IM-DAM-DPR)· 数据资产管理平台 V3.6 · 中国南方电网

> 信创生产库为达梦 DM8。应用 `dm` profile 下 `sql.init.mode=never`,**不自动建表**,
> 库表结构与增量列由 DBA 按本手册执行。基线以 H2 schema 为准
> (`dpr-confirm-service` / `dpr-authorize-service` 的 `src/main/resources/db/h2/schema.sql`),
> 达梦脚本由 MySQL 方言转换:`DATETIME→TIMESTAMP`、`TEXT/LONGTEXT→CLOB`、`TINYINT(1)→SMALLINT`、`NOW()→SYSDATE`。

## 一、全新部署(空库)——按序执行

| 顺序 | 文件 | 说明 | 是否必需 |
|---|---|---|---|
| 1 | `01_create_tablespace.sql` | 表空间 + 用户 + 字符集初始化 | 必需 |
| 2 | `02_schema_dm.sql` | 全量库表 DDL | 必需 |
| 3 | `09_sync_h2_baseline_dm.sql` | **补齐 02 相对 H2 基线漂移的列/表(见下「基线漂移」)** | **必需** |
| 4 | `03_data_dm.sql` | 演示/测试数据 | 生产可跳过 |
| 5 | `05_seed_aitool_dm.sql` | 智能确权辅助工具演示种子 | 可选,生产跳过 |
| 6 | `06_seata_undo_log_dm.sql` | Seata AT 回滚日志表 | 仅 prod 分布式事务需要 |

> ⚠️ 全新部署**必须**在 `02` 之后执行 `09`。历史上 `02_schema_dm.sql` 未随实体/H2 演进同步,
> 缺 `IM_CONFIRM_APPLY.CEC_CHANGE_DIFF`、`IM_BATCH_AUTH_LIST.CEC_GEO_SCOPE` 等列及
> `IM_CONFIRM_RECHECK_TASK`/`IM_DPR_RISK`/`IM_SENSITIVE_VAULT`/`SYS_ORGANIZATION` 等表,
> 仅跑 `02` 会导致 `ConfirmApplyMapper/BatchAuthListMapper.selectList` 报「Invalid column name」。
> 全新部署**不要执行** `04_alter_*`(其列已在 `02` 中);`09` 与 `04` 互不重叠。

## 二、存量生产库升级(已上线、表里已有数据)——只执行增量

| 顺序 | 文件 | 说明 |
|---|---|---|
| 1 | `04_alter_change_lifecycle_dm.sql` | 补齐近期新增列/表(见下) |
| 2 | `07_normalize_material_datatype_dm.sql` | 资料类型 `CEC_CATEGORY`(CEC_DATA_TYPE)中文→编码 01–07,**幂等可重跑** |
| 3 | `08_sys_organization_dm.sql` | 组织主数据只读镜像表 `SYS_ORGANIZATION`(部门/归口下拉、Dashboard 筛选、制卡/发证省地市编码回填消费;数据由平台/4A 同步,PRM 不写)。达梦不支持 `IF NOT EXISTS`,对象已存在则忽略本段继续 |
| 4 | `09_sync_h2_baseline_dm.sql` | **补齐 H2 基线漂移的列/表(见「基线漂移」)。存量库同样必需;列/表已存在则报错忽略该条继续** |

### 基线漂移(09 补齐的列/表)

`09_sync_h2_baseline_dm.sql` 以 H2 schema 为准,补齐 `02`/`04` 未同步的历史漂移:
- 补列:`IM_CONFIRM_APPLY`(CEC_CHANGE_DIFF/CEC_CHANGE_SUMMARY/CEC_CHANGE_VERSION/CEC_BASELINE_REF/CEC_SUBJECT_LEVEL)、
  `IM_BATCH_AUTH_LIST`(CEC_GEO_SCOPE)、`IM_AUTH_AGREEMENT`/`IM_AUTH_APPLY`/`IM_AUTH_FILING`/`IM_AUTH_SCENARIO`/
  `IM_CONFIRM_TABLE_ITEM`/`IM_EQUITY_CARD_INFO` 等多列。
- 补表:`IM_CONFIRM_RECHECK_TASK`(重确权工单)、`IM_DPR_RISK`(数据权益风险)、`IM_SENSITIVE_VAULT`(国密保险箱)、
  `SYS_ORGANIZATION`(组织镜像,若已跑 `08` 则该建表语句报"对象已存在",忽略即可)。

`04_alter_change_lifecycle_dm.sql` 覆盖的增量项:
1. 确权变更生命周期:`IM_CONFIRM_APPLY.CEC_CHANGE_TRIGGER`、`IM_EQUITY_CARD_INFO.CEC_VERSION` / `CEC_SUPERSEDED_NO`(+ 存量版本号回填)
2. 人工预审 AI 快照:`IM_CONFIRM_APPLY.CEC_AI_SNAPSHOT`
3. 平台同步材料来源:`IM_CONFIRM_MATERIAL.CEC_SOURCE`
4. 大模型校验留痕:新增表 `IM_DPR_AI_RUNLOG`(含跨域字段 `CEC_BIZ_TYPE`)+ 索引
5. 授权侧 AI 校验快照:`IM_AUTH_APPLY.CEC_AI_SNAPSHOT`

**幂等性**:达梦不支持 `ADD COLUMN/CREATE TABLE IF NOT EXISTS`,`04_alter_*` **仅可执行一次**;
若某列/表已存在会报"列名重复/对象已存在",**忽略该条对应语句继续**即可。
`07_normalize_material_datatype_dm.sql` 为纯数据回填(中文→编码),**幂等可重复执行**。

## 三、注意事项

- 暴露过的 `DASHSCOPE_API_KEY` 须在阿里云百炼控制台轮换(与库无关,部署清单备注)。
- MySQL 侧对应迁移见 `mysql/04_alter_change_lifecycle.sql`(若该环境也部署 MySQL)。
- 升级后建议抽查:`IM_DPR_AI_RUNLOG`、`IM_AUTH_APPLY.CEC_AI_SNAPSHOT`、`IM_CONFIRM_MATERIAL.CEC_SOURCE` 是否已存在。
