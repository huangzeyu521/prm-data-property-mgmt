# 数据产权管理模块(数据资产管理平台 V3.6)

中国南方电网·云化数据中心 —— 数据流通管理下的二级应用模块 `数据产权管理`(界面前缀 `IM-DAM-DPR`)。
围绕国家"数据二十条"**三权分置**(持有权 / 加工使用权 / 产品经营权),实现 **确权—授权—权益监测—综合分析** 全流程线上闭环。两条铁律:**先确后授、有权才用** 与 **只填一张表**。

## 功能域与实现状态

| 功能项 | 子项 / 能力 | 后端服务 | 前端页面 |
|---|---|---|---|
| **F-01 产权信息管理** | 产权档案(一数一档)、数据资产同步、产权树、总体概览、变更记录(自动留痕) / 权益状态监控、风险预警(处置闭环)、合规检查(到期巡检)、监测规则(版本/禁删) | `dpr-ledger-service` | 产权总体概览、产权树、产权档案管理、风险预警、监测规则配置 |
| **F-02 数据确权管理** | 确权申请状态机、多级审批、终审自动生成权益卡片、卡片冻结熔断 | `dpr-confirm-service` | 确权申请与审核、权益卡片管理 |
| **F-03 数据授权管理** | 一事一议/批量授权、先确后授校验、审批通过自动签发授权证书、证书撤销 | `dpr-authorize-service` | 授权申请与审核、授权权益证书 |
| **F-04 综合分析管理** | 确权看板、授权看板(规模/通过率/分布) | confirm + authorize | 确权看板、授权看板 |

## 技术栈(严格信创)

- **后端**:JDK 17 · Spring Boot 3.2 · Spring Cloud 2023 · **Spring Cloud Alibaba(Nacos/Gateway/Sentinel/Seata)** · MyBatis-Plus
- **数据库**:生产 达梦 DM8;开发/测试 H2(MySQL 兼容模式)。方言经 `prm.db-type` 切换,代码零改动
- **前端**:Vue 3 · Vite · Element Plus · Pinia · Vue Router · Axios · ECharts(遵循数研院 UI 规范与"界面高压线")
- **信创目标**:银河麒麟 V10 / 鲲鹏 / 国密 SM2-3-4 / 4A SSO / 区块链存证

## 工程结构

```
prm-backend/            Maven 多模块(6 模块)
├─ prm-common           统一响应/异常/分页/用户上下文(ABAC)/BaseEntity/MP自动填充/共享配置
├─ prm-gateway          Spring Cloud Gateway + Sentinel(路由 ledger/confirm/auth)
├─ dpr-ledger-service   F-01(端口 9101)
├─ dpr-confirm-service  F-02(端口 9102)
└─ dpr-authorize-service F-03(端口 9103)
prm-frontend/           Vue3 前端(11 页面)
deploy/、.m2/settings.xml(阿里云镜像)
docs/                   需求综述、F-01 技术方案
```

## 构建与运行

### 后端(本机无需装 JDK,用 Docker-Maven)

```powershell
# 编译并运行全部单测(23 个)
docker run --rm -v D:/MyProject/PRM/prm-backend:/app -v D:/MyProject/PRM/.m2:/root/.m2 `
  -w /app maven:3.9-eclipse-temurin-17 mvn -B test

# 本地启动单个服务(dev profile:H2 + 关闭 Nacos/Seata)
docker run --rm -p 9101:9101 -v D:/MyProject/PRM/prm-backend:/app -v D:/MyProject/PRM/.m2:/root/.m2 `
  -w /app maven:3.9-eclipse-temurin-17 mvn -pl dpr-ledger-service -am spring-boot:run
```

> 生产(信创):切 `prod` profile —— 达梦 DM8 + Nacos + Sentinel + Seata,`prm.db-type=DM`。

### 前端

```powershell
cd prm-frontend
npm install
npm run dev      # http://localhost:5173,/api 按业务域代理到 9101/9102/9103
npm run build    # 生产构建
```

## 测试

- 后端 **23** 个集成单测(H2 全离线),覆盖各业务状态机、自动留痕、先确后授/冻结熔断、看板统计。
- 前端 `npm run build` 通过(11 页面 chunk)。

## 关键业务约束(已落地)

先确后授(授权引用有效权益卡片) · 授权不得超确权边界 · 三权分置分别确权发证 · 防篡改自动留痕 · 生效规则/预警记录/权益卡片禁物理删/冻结熔断 · RBAC+ABAC 数据范围隔离 · 公共字段自动填充 · 逻辑删除全局过滤。

## 后续增强(TODO)

接入大瓦特 AI(OCR/权属冲突/RAG 决策)· 权益/授权证书 PDF 模板渲染 · Feign 跨服务回写产权档案确权状态 · 区块链存证 · 国密落地 · Camunda 替换简化状态机 · Drools 规则引擎 · 等保三级与同城灾备。
