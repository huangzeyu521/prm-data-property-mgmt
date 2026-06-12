# 智能确权辅助工具 · MySQL 库表脚本

工具为**独立模块**：六张 `IM_AIT_*` 表自包含，与确权主流程仅通过 `CEC_APPLY_ID` **弱关联**（可空、无外键、不依赖主库任何表），可随工具独立建库部署。

## 文件

| 文件 | 内容 | 必需 |
|---|---|---|
| `01_aitool_schema.sql` | 建库(prm/utf8mb4) + 六表 DDL | ✅ |
| `02_aitool_seed.sql` | 演示种子（1 材料+解析、2 对立主张、1 主体冲突，三页开箱有内容） | 可选，生产跳过 |

## 表清单

| 表 | 用途 |
|---|---|
| `IM_AIT_MATERIAL` | 材料：文件元数据+正文、SM3 哈希、解析状态/进度/失败原因分类 |
| `IM_AIT_PARSE_RESULT` | 解析结果：5 要素抽取、印章交叉校验、复核标记、材料可信度 |
| `IM_AIT_COMPARE` | 表单比对：材料值 vs 表单值、差异类型、原文定位(offset/snippet) |
| `IM_AIT_KG_CLAIM` | 权属主张（知识图谱）：主体/类型/范围/排他/来源四类 |
| `IM_AIT_CONFLICT` | 权属冲突：主体/范围/时效/历史四类、风险等级、处置建议 |
| `IM_AIT_DECISION` | 决策建议：4 因子加权、规则+AI 预测对照、RAG 建议/引用、权益分割双方案、SM3 证据链 |

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
