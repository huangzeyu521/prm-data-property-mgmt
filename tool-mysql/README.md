# 智能确权辅助工具 · MySQL 库表脚本

工具为**独立模块**：全部 `IM_AIT_*` 表自包含，与确权主流程仅通过 `CEC_APPLY_ID` **弱关联**（可空、无外键、不依赖主库任何表），可随工具独立建库部署。

> 达梦(DM8)版本见 `../tool-dm8/`。本目录已同步最新改动(22 张表,覆盖可研 1.1.1.1~3.3 九大组功能点)。

## 文件

| 文件 | 内容 | 必需 |
|---|---|---|
| `01_aitool_schema.sql` | 建库(prm/utf8mb4) + 22 张表 DDL | ✅ |
| `02_aitool_seed.sql` | 演示种子（1 材料+解析、2 对立主张、1 主体冲突，三页开箱有内容） | 可选，生产跳过 |

## 表清单(22 张)

| 分组 | 表 |
|---|---|
| 材料解析(1.1.1.1) | `IM_AIT_MATERIAL`、`IM_AIT_DOC_SEGMENT`、`IM_AIT_PARSE_RESULT`、`IM_AIT_COMPARE` |
| 数据清洗(1.2) | `IM_AIT_AUDIT_BASE`、`IM_AIT_CLEAN_LOG` |
| 要素抽取(1.3) | `IM_AIT_PROFILE`、`IM_AIT_PROFILE_SUBJECT`、`IM_AIT_CONSTRAINT` |
| 材料管理(1.4) | `IM_AIT_PARSE_RECORD`、`IM_AIT_DOC_TEMPLATE`、`IM_AIT_PARSE_CONFIG` |
| 知识库RAG(2.1) | `IM_AIT_KB_DOC`、`IM_AIT_KB_CHUNK` |
| 冲突识别与分析 | `IM_AIT_KG_CLAIM`、`IM_AIT_CONFLICT`、`IM_AIT_CONFLICT_RULE` |
| 确权决策(3.1/3.2) | `IM_AIT_DECISION`、`IM_AIT_AUDIT_RESULT`、`IM_AIT_EVIDENCE` |
| 运行支撑(3.3) | `IM_AIT_TASK`、`IM_AIT_RUN_LOG` |

## 执行

```bash
mysql -uroot -p < 01_aitool_schema.sql
mysql -uroot -p < 02_aitool_seed.sql   # 可选
```

## 服务接入 MySQL

工具部署在 `dpr-confirm-service` 内，切 MySQL 用 prod profile：

```bash
SPRING_PROFILES_ACTIVE=prod \
SPRING_DATASOURCE_URL='jdbc:mysql://<host>:3306/prm?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
SPRING_DATASOURCE_USERNAME=... SPRING_DATASOURCE_PASSWORD=... \
java -jar dpr-confirm-service-0.1.0-SNAPSHOT.jar
```

> 库名默认 `prm`（与整库脚本 `mysql/01_schema.sql` 同库可共存——本目录六表是其子集，重复执行 `CREATE TABLE IF NOT EXISTS` 幂等）。若工具独立建库，改 `01` 脚本头部库名并同步数据源 URL 即可。
>
> 结构与 H2 dev（`db/h2/schema.sql`）保持同步；后续加列请双侧同改并更新本目录。
