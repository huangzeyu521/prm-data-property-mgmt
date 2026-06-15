# 智能确权辅助工具 · 达梦(DM8)库表脚本

工具为**独立模块**：全部 `IM_AIT_*` 表自包含，与确权主流程仅通过 `CEC_APPLY_ID` **弱关联**（可空、无外键、不依赖主库任何表），可随工具独立建库部署。

> 本目录是 `tool-mysql/`（MySQL 版)的**达梦(DM8)转换**，并已**同步最新改动**（材料智能解析 / 数据清洗 / 要素抽取与画像 / 材料管理 / 知识库 RAG / 冲突识别与分析 / 确权 Agent / 审核台账 / 运行支撑 全部新增表与扩列）。与 `dm8/02_schema_dm.sql` 的 aitool 段同源。

## 文件

| 文件 | 内容 | 必需 |
|---|---|---|
| `01_aitool_schema_dm.sql` | 22 张 `IM_AIT_*` 表 DDL（达梦语法） | ✅ |
| `02_aitool_seed_dm.sql` | 演示种子（1 材料+解析、2 对立主张、1 主体冲突，三页开箱有内容） | 可选，生产跳过 |

## 表清单（22 张）

| 分组 | 表 |
|---|---|
| 材料解析(1.1.1.1) | `IM_AIT_MATERIAL`(+分类/数据表/版面/查重/页数/OCR列)、`IM_AIT_DOC_SEGMENT`(多粒度切片)、`IM_AIT_PARSE_RESULT`、`IM_AIT_COMPARE` |
| 数据清洗(1.2) | `IM_AIT_AUDIT_BASE`(统一审核底表)、`IM_AIT_CLEAN_LOG`(清洗日志) |
| 要素抽取(1.3) | `IM_AIT_PROFILE`(确权画像)、`IM_AIT_PROFILE_SUBJECT`(五类主体)、`IM_AIT_CONSTRAINT`(五类约束) |
| 材料管理(1.4) | `IM_AIT_PARSE_RECORD`(解析记录档)、`IM_AIT_DOC_TEMPLATE`(资料模板库)、`IM_AIT_PARSE_CONFIG`(解析配置) |
| 知识库RAG(2.1) | `IM_AIT_KB_DOC`、`IM_AIT_KB_CHUNK` |
| 冲突识别与分析 | `IM_AIT_KG_CLAIM`、`IM_AIT_CONFLICT`(+追溯/法律风险列)、`IM_AIT_CONFLICT_RULE`(规则配置) |
| 确权决策(3.1/3.2) | `IM_AIT_DECISION`、`IM_AIT_AUDIT_RESULT`(Agent审核)、`IM_AIT_EVIDENCE`(证据链) |
| 运行支撑(3.3) | `IM_AIT_TASK`(批量任务)、`IM_AIT_RUN_LOG`(统一运行日志) |

## 执行

```bash
# 表空间/用户(整库共用,首次执行)见 ../dm8/01_create_tablespace.sql
disql PRM_DPR/PRM_DPR2024!@localhost:5236 @01_aitool_schema_dm.sql
disql PRM_DPR/PRM_DPR2024!@localhost:5236 @02_aitool_seed_dm.sql   # 可选
```

## 服务接入达梦

工具部署在 `dpr-confirm-service` 内，切达梦用 `dm` profile(`-Pdm` 构建带达梦驱动):

```bash
SPRING_PROFILES_ACTIVE=dm \
DM_URL='jdbc:dm://<host>:5236' DM_USER=PRM_DPR DM_PASSWORD='PRM_DPR2024!' \
PRM_AI_PROVIDER=qwen DASHSCOPE_API_KEY=... \
java -jar dpr-confirm-service-0.1.0-SNAPSHOT.jar
```

> 达梦语法转换要点(由 MySQL 转):去 `ENGINE/CHARSET/AUTO_INCREMENT`、去内联 `COMMENT`(改 `COMMENT ON COLUMN`)、`TINYINT(1)→INT`、`DATETIME→TIMESTAMP`、`TEXT→CLOB`、去反引号。
> 结构与 `dm8/02_schema_dm.sql`、`mysql/01_schema.sql`、H2 dev(`db/h2/schema.sql`)保持同步;后续加列请四侧同改。
