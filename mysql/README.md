# 数据产权管理模块 · MySQL 库表

数据资产管理平台 V3.6 · 数据产权管理（IM-DAM-DPR）对接 MySQL 的建库脚本与测试数据。
脚本由项目现用 H2（MySQL 兼容模式）schema 转换而来，已在 **MySQL 8.0** 实测通过。

## 文件

| 文件 | 内容 |
|---|---|
| `01_schema.sql` | 建库 + 29 张业务表 DDL（utf8mb4、InnoDB、注释、索引） |
| `02_data.sql` | 测试数据（29 张表均有数据，含存证/智能工具表补充数据） |
| `03_seata_undo_log.sql` | **(prod 可选)** Seata AT 模式 `undo_log` 表，仅分布式事务需要 |

> 库名：`prm_dpr`　字符集：`utf8mb4`

## 一键导入

```bash
# 方式一：本机 mysql 客户端
mysql -u root -p < 01_schema.sql
mysql -u root -p prm_dpr < 02_data.sql

# 方式二：Docker（无需本机装 MySQL）
docker run -d --name prm-mysql -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 \
  -v D:/MyProject/PRM/mysql:/sql mysql:8 --character-set-server=utf8mb4
docker exec prm-mysql sh -c 'mysql -uroot -proot < /sql/01_schema.sql'
docker exec prm-mysql sh -c 'mysql -uroot -proot < /sql/02_data.sql'
```

## 后端对接 MySQL（从 H2/达梦切换）

模块用 `prm.db-type` 切换分页方言、零改代码。对接 MySQL 三步：

1. **加 JDBC 驱动**（各 `dpr-*-service/pom.xml`，prod 用达梦时则用达梦驱动）：
   ```xml
   <dependency>
     <groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId>
   </dependency>
   ```
2. **新增 `application-mysql.yml`**（每个服务，端口各自 9101/9102/9103）：
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/prm_dpr?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
       username: root
       password: root
       driver-class-name: com.mysql.cj.jdbc.Driver
   prm:
     db-type: MYSQL          # MyBatis-Plus 分页方言走 MySQL
   ```
3. **以 mysql profile 启动**：`--spring.profiles.active=mysql`（或 `SPRING_PROFILES_ACTIVE=mysql`）。

> 逻辑删除：`CEC_DEL_FLAG`（0=正常,1=已删），MyBatis-Plus 全局过滤已配置，查询自动带 `CEC_DEL_FLAG=0`。

## 库表清单（29 张）

**公共**：`IM_DPR_EVIDENCE`（区块链存证）

**F-01 产权信息管理**（6）：`IM_DPM_DATA_ASSET_INFO` 数据资产、`IM_PROPERTY_ARCHIVE` 产权档案、`IM_PROPERTY_CHANGE_RECORD` 变更记录、`IM_MONITOR_RULE` 监测规则、`IM_ALERT_RECORD` 风险预警、`IM_COMPLIANCE_RESULT` 合规检查

**F-02 数据确权管理**（8 + 智能工具 6）：`IM_CONFIRM_APPLY` 确权申请、`IM_CONFIRM_GUIDANCE` 确权指引、`IM_CONFIRM_MATERIAL` 申请材料、`IM_CONFIRM_SUMMARY` 汇总表(表3/4)、`IM_EQUITY_CARD_INFO` 权益卡片、`IM_EQUITY_CARD_LOG` 卡片变更、`IM_EQUITY_CERT` 权益证书、`IM_EQUITY_CERT_TEMPLATE` 证书模板；智能工具：`IM_AIT_MATERIAL`/`IM_AIT_PARSE_RESULT`/`IM_AIT_COMPARE`/`IM_AIT_KG_CLAIM`/`IM_AIT_CONFLICT`/`IM_AIT_DECISION`

**F-03 数据授权管理**（8）：`IM_AUTH_APPLY` 授权申请、`IM_BATCH_AUTH_LIST` 批量清单(表6)、`IM_AUTH_CATALOG_ITEM` 目录项(指引/场景/模板)、`IM_AUTH_COMPLIANCE` 合规校验、`IM_AUTH_AGREEMENT` 运营协议、`IM_AUTH_CERT` 授权证书、`IM_AUTH_CERT_TEMPLATE` 证书模板、`IM_AUTH_ACCOUNTABILITY` 违规追责

## 完整性审计（这些表够吗）

对全代码做了表名审计（结论：业务表完整）：

- 纯 **MyBatis-Plus**，无 mapper XML、无 `@Select/@Insert` 原生 SQL —— 代码引用的所有 `IM_` 表 = 这 29 张（+ 3 张已删功能表）。无遗漏、无跨表 JOIN 隐含表。
- **基础设施表**（按需，非业务表）：
  - `undo_log`（Seata）：prod profile 三服务 `seata.enabled=true`（默认 AT 模式），规范上每个业务库需建 → 见 `03_seata_undo_log.sql`。dev/test 不需要。当前未用 `@GlobalTransactional`，暂不会真正写入，但 prod 开了 Seata 代理建议先建好。
  - Camunda `ACT_*`（~49 张）：**仅 `-Pcamunda` 启用时**需要，且 Camunda 启动器自动建表（schema-update），**无需手工 DDL**；默认构建零 Camunda。
- 主键策略：业务表主键为 `VARCHAR(64)`（雪花/UUID 应用层生成，`IdType.ASSIGN_UUID` 等），**无数据库序列/自增**，故无 ID 生成器表。

## 说明

- **已排除 3 张停用功能表**（对应前端/后端已删功能）：`IM_SENSITIVE_VAULT`（国密保险箱）、`IM_DPR_RISK`（数据权益风险表/附录F）、`IM_AUTH_FILING`（对外经营授权备案/附录G）。如需保留，可从项目 H2 schema 补回。
- 字段前缀 `CEC_` 沿用平台规范；审计字段：`CEC_CREATOR_ID/CEC_CREATE_TIME/CEC_UPDATER_ID/CEC_UPDATE_TIME`；多租隔离：`CEC_PROVINCE_CODE/CEC_BUREAU_CODE`（ABAC 数据范围）。
- 测试数据贯通业务主线：8 资产 → 6 确权申请（覆盖各状态）→ 5 权益卡片（含冻结/失效）→ 6 授权申请（一事一议/批量、均引用权益卡片即"先确后授"）→ 4 授权证书（含 1 张熔断暂停）→ 2 违规追责 → 6 条区块链存证。
- H2→MySQL 转换：`CLOB→TEXT`、`TIMESTAMP→DATETIME`、`BOOLEAN→TINYINT(1)`、`DATEADD()→DATE_ADD()`、`CURRENT_TIMESTAMP→NOW()`。
- 实测：MySQL 8.0 导入零报错，29 表全建、全有数据，utf8mb4 中文正确（`客户用电信息表`=7字符/21字节）。
