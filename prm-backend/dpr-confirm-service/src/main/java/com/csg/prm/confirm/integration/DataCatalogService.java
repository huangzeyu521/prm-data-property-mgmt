package com.csg.prm.confirm.integration;

import com.csg.prm.confirm.integration.dto.CatalogNode;
import com.csg.prm.confirm.integration.dto.PlatformTableMeta;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 数据目录(确权申请左侧范围树)服务。
 * <p>层级对齐平台 TW_DATA_CARD:业务域(DOMAIN_2024) → 系统(MGT_SYS_NAME) → 一级功能模块(FUN_LEVEL1) → 库表(卡片)。
 * 平台未接入期间用 stub 合成"一个系统下多功能模块、每模块多库表"的真实结构(替代旧"一卡假合成多表");
 * 接入后改 PlatformCardClient 实现(按 DOMAIN_2024/MGT_SYS_NAME/FUN_LEVEL 懒加载 + AU_TABLE_META_DATA 带确权要素),本服务不变。
 */
@Service
public class DataCatalogService {

    // src:数据来源类型完整标签(A 自行生产/B 公开采集/C 公共授权/D 共同生产/E 交易采购/F 其他),供"来源变更"演示
    private record Tbl(String code, String name, String secret, String src, boolean g, boolean h, boolean i, boolean j) {
    }

    private record Mod(String name, String code, List<Tbl> tables) {
    }

    private record Sys(String name, String code, List<Mod> modules) {
    }

    private record Dom(String name, List<Sys> systems) {
    }

    private static final String A = "A 自行生产数据", B = "B 公开采集数据", C = "C 公共授权数据",
            D = "D 共同生产数据", E = "E 交易采购数据", F = "F 其他来源数据";

    // 演示目录:业务域 → 系统 → 一级功能模块 → 库表。覆盖南网主要业务系统;营销系统刻意覆盖 4 类变更场景:
    //   来源变更(MKT_EXP_APPLY=D / MKT_TRADE_SETTLE=E)、管理要求变更(BILL_INVOICE=G / BILL_CONS=H / TRADE_SETTLE=I)、
    //   权益到期(TRADE_SETTLE 授权即将到期)、数据新增(TRADE_DECL/EXP_CUST 未确权的新表)。
    private static final List<Dom> CATALOG = List.of(
            new Dom("市场营销域", List.of(
                    new Sys("营销管理系统", "MKT", List.of(
                            new Mod("市场交易", "TRADE", List.of(
                                    new Tbl("MKT_TRADE_DECL", "市场交易申报表", "普通商密", A, false, false, false, false),
                                    new Tbl("MKT_TRADE_SETTLE", "市场交易结算表", "普通商密", E, false, false, true, false))),
                            new Mod("业扩报装", "EXPAND", List.of(
                                    new Tbl("MKT_EXP_APPLY", "业扩申请表", "不涉密", D, false, false, false, false),
                                    new Tbl("MKT_EXP_CUST", "客户信息表", "敏感信息", A, false, true, false, false))),
                            new Mod("计费账务", "BILLING", List.of(
                                    new Tbl("MKT_BILL_INVOICE", "电费账单表", "普通商密", A, true, false, false, false),
                                    new Tbl("MKT_BILL_CONS", "用户用电信息表", "敏感信息", A, false, true, false, false))))),
                    new Sys("计量自动化系统", "AMR", List.of(
                            new Mod("采集运维", "COLLECT", List.of(
                                    new Tbl("AMR_COL_TASK", "采集任务表", "不涉密", A, false, false, false, false))),
                            new Mod("计量点管理", "POINT", List.of(
                                    new Tbl("AMR_PT_METER", "计量点表", "不涉密", A, false, false, false, false))),
                            new Mod("用电信息采集", "LOAD", List.of(
                                    new Tbl("AMR_LOAD_CURVE", "负荷曲线表", "普通商密", A, false, false, false, false),
                                    new Tbl("AMR_EVENT_LOG", "计量异常事件表", "普通商密", A, true, false, false, false))))),
                    new Sys("客户服务系统", "CS", List.of(
                            new Mod("服务工单", "ORDER", List.of(
                                    new Tbl("CS_WORK_ORDER", "客服工单表", "敏感信息", A, false, true, false, false),
                                    new Tbl("CS_COMPLAINT", "投诉记录表", "敏感信息", A, false, true, false, false))),
                            new Mod("服务知识库", "KB", List.of(
                                    new Tbl("CS_KB_FAQ", "服务知识表", "不涉密", B, false, false, false, false),
                                    new Tbl("CS_KB_POLICY", "政策法规表", "不涉密", C, false, false, false, false),
                                    new Tbl("CS_KB_EXT", "外部参考资料表", "不涉密", F, false, false, false, false))))))),
            new Dom("办公域", List.of(
                    new Sys("车辆管理系统", "VEH", List.of(
                            new Mod("车辆定位监控", "GPS", List.of(
                                    new Tbl("VEH_GPS_TRACK", "车辆轨迹表", "普通商密", A, true, false, false, false))),
                            new Mod("车辆调度", "DISPATCH", List.of(
                                    new Tbl("VEH_DISP_ORDER", "调度工单表", "不涉密", A, false, false, false, false))))),
                    new Sys("协同办公系统", "OA", List.of(
                            new Mod("公文流转", "DOC", List.of(
                                    new Tbl("OA_DOC_FLOW", "公文流转表", "普通商密", A, true, false, false, false))),
                            new Mod("会议管理", "MEETING", List.of(
                                    new Tbl("OA_MEETING", "会议纪要表", "普通商密", A, false, false, false, false))),
                            new Mod("督办管理", "SUPERVISE", List.of(
                                    new Tbl("OA_TASK", "督办任务表", "不涉密", A, false, false, false, false))))),
                    new Sys("人力资源系统", "HR", List.of(
                            new Mod("员工管理", "STAFF", List.of(
                                    new Tbl("HR_EMPLOYEE", "员工信息表", "敏感信息", A, false, true, false, false),
                                    new Tbl("HR_SALARY", "薪酬发放表", "敏感信息", A, false, true, false, false))),
                            new Mod("培训管理", "TRAIN", List.of(
                                    new Tbl("HR_TRAIN", "培训记录表", "不涉密", A, false, false, false, false))))))),
            new Dom("输配电域", List.of(
                    new Sys("生产管理系统", "PMS", List.of(
                            new Mod("设备台账", "ASSET", List.of(
                                    new Tbl("PMS_DEV_LEDGER", "设备台账表", "不涉密", A, false, false, false, false))),
                            new Mod("检修管理", "MAINT", List.of(
                                    new Tbl("PMS_MNT_PLAN", "检修计划表", "不涉密", A, false, false, false, false))),
                            new Mod("缺陷管理", "DEFECT", List.of(
                                    new Tbl("PMS_DEFECT", "设备缺陷表", "普通商密", A, false, false, false, false),
                                    new Tbl("PMS_TEST", "试验报告表", "普通商密", A, false, false, false, false))))),
                    new Sys("调度自动化系统", "DMS", List.of(
                            new Mod("实时运行", "SCADA", List.of(
                                    new Tbl("DMS_SCADA", "实时遥测表", "普通商密", A, false, false, false, false),
                                    new Tbl("DMS_LOAD", "负荷预测表", "普通商密", A, false, false, false, false))),
                            new Mod("调度日志", "LOG", List.of(
                                    new Tbl("DMS_LOG", "调度操作记录表", "普通商密", A, true, false, false, false))))),
                    new Sys("配电自动化系统", "DAS", List.of(
                            new Mod("配网监测", "FEEDER", List.of(
                                    new Tbl("DAS_FEEDER", "馈线监测表", "普通商密", A, false, false, false, false))),
                            new Mod("故障处理", "FAULT", List.of(
                                    new Tbl("DAS_FAULT", "故障录波表", "普通商密", A, false, false, false, false))))))),
            new Dom("基建工程域", List.of(
                    new Sys("工程建设系统", "PROJ", List.of(
                            new Mod("项目计划", "PLAN", List.of(
                                    new Tbl("PROJ_PLAN", "工程计划表", "不涉密", A, false, false, false, false))),
                            new Mod("物资采购", "PROCURE", List.of(
                                    new Tbl("PROJ_MATERIAL", "物资采购表", "普通商密", E, false, false, true, false),
                                    new Tbl("PROJ_CONTRACT", "施工合同表", "普通商密", E, false, false, true, false))),
                            new Mod("进度跟踪", "PROGRESS", List.of(
                                    new Tbl("PROJ_PROGRESS", "进度跟踪表", "不涉密", A, false, false, false, false))))))),
            new Dom("安全监管域", List.of(
                    new Sys("安全生产系统", "SAFE", List.of(
                            new Mod("隐患排查", "HAZARD", List.of(
                                    new Tbl("SAFE_HAZARD", "隐患排查表", "普通商密", A, false, false, false, false))),
                            new Mod("作业管理", "WORK", List.of(
                                    new Tbl("SAFE_WORK", "作业票表", "不涉密", A, false, false, false, false))),
                            new Mod("应急管理", "EMERGENCY", List.of(
                                    new Tbl("SAFE_EMERGENCY", "应急预案表", "不涉密", A, false, false, false, false))))))));

    // 演示:已确权的库表代码(实际接入后查 ConfirmApply 已完成 / 权益卡片生效来判定)。
    // 营销系统刻意多张已确权(覆盖来源/管理/到期变更);TRADE_DECL/EXP_CUST 留未确权供"数据新增"演示。
    private static final Set<String> CONFIRMED = Set.of(
            "MKT_BILL_CONS", "MKT_BILL_INVOICE", "MKT_TRADE_SETTLE", "MKT_EXP_APPLY",
            "AMR_PT_METER", "AMR_LOAD_CURVE", "CS_WORK_ORDER", "OA_DOC_FLOW", "HR_TRAIN");

    // 演示:已对外授权的库表(⊆ 已确权;实际接入后由 dpr-authorize 按表反查有效授权)。
    private static final Set<String> AUTHORIZED_TABLES = Set.of("MKT_BILL_CONS", "MKT_TRADE_SETTLE", "OA_DOC_FLOW");

    // 演示:授权状态多样化(供"权益到期续止"变更演示)。
    private static String authStatusOf(String code) {
        return switch (code) {
            case "MKT_TRADE_SETTLE" -> "即将到期(剩 30 天,2026-07-25)";
            default -> "有效(2025-04-20 起)";
        };
    }

    // status: null=全部;unconfirmed=仅未确权(初始确权);confirmed=仅已确权(确权变更)
    private boolean visible(String code, String status) {
        if (!StringUtils.hasText(status)) {
            return true;
        }
        boolean confirmed = CONFIRMED.contains(code);
        return "confirmed".equals(status) ? confirmed : !confirmed;
    }

    /** 懒加载子节点:type 为父节点类型(null/root=根),id 为父节点 id;status 过滤已/未确权叶子。 */
    public List<CatalogNode> tree(String type, String id, String status) {
        List<CatalogNode> out = new ArrayList<>();
        if (!StringUtils.hasText(type) || "root".equalsIgnoreCase(type)) {
            for (Dom d : CATALOG) {
                int c = countDomain(d, status);
                if (c > 0) {
                    out.add(new CatalogNode(d.name(), d.name(), "domain", c, false));
                }
            }
            return out;
        }
        if ("domain".equals(type)) {
            CATALOG.stream().filter(d -> d.name().equals(id)).findFirst().ifPresent(d ->
                    d.systems().forEach(s -> {
                        int c = countSys(s, status);
                        if (c > 0) {
                            // 系统节点带确权进度(已确权 N / 总 M),供前端「已确权 N/M」徽标
                            int total = (int) s.modules().stream().flatMap(m -> m.tables().stream()).count();
                            int confirmedCnt = (int) s.modules().stream().flatMap(m -> m.tables().stream())
                                    .filter(t -> CONFIRMED.contains(t.code())).count();
                            out.add(CatalogNode.system(s.name(), c, confirmedCnt, total));
                        }
                    }));
            return out;
        }
        if ("system".equals(type)) {
            findSys(id).ifPresent(s -> s.modules().forEach(m -> {
                int c = (int) m.tables().stream().filter(t -> visible(t.code(), status)).count();
                if (c > 0) {
                    out.add(new CatalogNode(s.name() + "/" + m.name(), m.name(), "module", c, false));
                }
            }));
            return out;
        }
        if ("module".equals(type)) {
            String[] parts = id.split("/", 2); // id = 系统名/模块名
            if (parts.length == 2) {
                findSys(parts[0]).ifPresent(s -> s.modules().stream().filter(m -> m.name().equals(parts[1])).findFirst()
                        .ifPresent(m -> m.tables().stream().filter(t -> visible(t.code(), status)).forEach(t ->
                                out.add(CatalogNode.table(t.code(), t.name(), AUTHORIZED_TABLES.contains(t.code()), CONFIRMED.contains(t.code()))))));
            }
            return out;
        }
        return out;
    }

    /** 按系统(可再按一级功能模块筛)列出库表卡片;modules 为空=全部模块;status 过滤已/未确权。 */
    public List<PlatformTableMeta> cardsBySystem(String sysName, List<String> modules, String status) {
        List<PlatformTableMeta> out = new ArrayList<>();
        findSys(sysName).ifPresent(s -> {
            for (Mod m : s.modules()) {
                if (modules != null && !modules.isEmpty() && !modules.contains(m.name())) {
                    continue;
                }
                for (Tbl t : m.tables()) {
                    if (!visible(t.code(), status)) {
                        continue;
                    }
                    // 模拟 AU_TABLE_META_DATA 逐表预填(平台已采集):来源主体/来源说明 + 各关联主体说明;
                    // 已确权表带确权时间(供"确权变更"基线)。申报人据此核实修正,无须从零填。
                    boolean confirmed = CONFIRMED.contains(t.code());
                    char sc = t.src().charAt(0);
                    String srcSubject = sc == 'A' ? "中国南方电网有限责任公司"
                            : (sc == 'D' ? "南网及共同生产方" : (sc == 'E' ? "XXX 科技有限公司(交易采购)" : "外部来源方"));
                    out.add(new PlatformTableMeta(
                            s.code() + "_DB01", m.code(), t.code(), t.name(), t.name(),
                            t.secret(), t.src(), srcSubject,
                            t.g(), t.h(), t.i(), t.j(), true,
                            // 平台已上传材料附件名(AU_TABLE_META_DATA.*_NAME;正本在"上传材料"步同步)
                            // 逐来源字母各给专属说明文件,避免 B/C/D/F 张冠李戴(对齐 35 号文 表1 各来源应交说明)
                            switch (sc) {
                                case 'A' -> "数据来源与系统建设投入说明.pdf";
                                case 'B' -> "公共采集情况说明.pdf";
                                case 'C' -> "公共数据授权说明.pdf";
                                case 'D' -> "共同生产情况说明.pdf";
                                case 'E' -> "交易采购情况说明.pdf";
                                case 'F' -> "其他来源情况说明.pdf";
                                default -> "数据来源情况说明.pdf";
                            },
                            t.g() ? "行政监管要求说明.pdf" : null,
                            t.h() ? "用户入网协议(个人信息授权).pdf" : null,
                            t.i() ? "第三方保密协议.pdf" : null,
                            t.j() ? "其他第三方机构协议.pdf" : null,
                            sc == 'A' ? "自行生产数据,由" + s.name() + "建设投入形成"
                                    : (sc == 'E' ? "交易采购数据,依采购协议约定使用范围" : "共同生产数据,依共享协议约定权益"),
                            t.g() ? "电力行政监管要求(电价/供电服务监管)" : null,
                            t.h() ? "用户个人/家庭用电信息,依据用户入网协议授权,限电力服务场景" : null,
                            t.i() ? "涉交易对手商业机密,依保密协议约定使用" : null,
                            t.j() ? "其他第三方机构协议约束" : null,
                            confirmed ? "2025-04-20" : null,
                            // 系统责任信息(TW_DATA_CARD.MGT_USER/MGT_USER_PHONE,系统内一致)
                            sysOwner(s.code()), sysPhone(s.code()),
                            // 申报主体(MGT_UNIT/MGT_MNG_DEPT + SYS_ORGANIZATION.ORG_TYPE 单位层级)
                            sysUnit(s.code()), sysDept(s.code()), sysLevel(s.code()),
                            // 逐表对外授权状态(dpr-authorize 反查)
                            AUTHORIZED_TABLES.contains(t.code()),
                            AUTHORIZED_TABLES.contains(t.code()) ? "AUTH-" + t.code() : null,
                            AUTHORIZED_TABLES.contains(t.code()) ? t.name() + "(脱敏)对外开放目录" : null,
                            AUTHORIZED_TABLES.contains(t.code()) ? authStatusOf(t.code()) : null));
                }
            }
        });
        return out;
    }

    /** 系统名 → 业务域名 映射(供授权侧"所属业务域"按系统逐表带出);系统隶属的业务域来自目录结构。 */
    public java.util.Map<String, String> systemDomainMap() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        for (Dom d : CATALOG) {
            for (Sys s : d.systems()) {
                map.putIfAbsent(s.name(), d.name());
            }
        }
        return map;
    }

    /** 单系统所属业务域;未知返回空串。 */
    public String domainOfSystem(String sysName) {
        if (!StringUtils.hasText(sysName)) {
            return "";
        }
        for (Dom d : CATALOG) {
            for (Sys s : d.systems()) {
                if (s.name().equals(sysName)) {
                    return d.name();
                }
            }
        }
        return "";
    }

    /**
     * 数据资产目录全树(非懒加载):业务域 → 系统 → 一级功能模块 → 库表卡片。
     * 供「数据资产确权目录管理」整树渲染 + 计数/筛选;card 叶子带 sysName/tableCode/已确权/已授权。
     */
    public List<com.csg.prm.confirm.integration.dto.CatalogTreeNode> fullTree() {
        List<com.csg.prm.confirm.integration.dto.CatalogTreeNode> domains = new ArrayList<>();
        for (Dom d : CATALOG) {
            List<com.csg.prm.confirm.integration.dto.CatalogTreeNode> sysNodes = new ArrayList<>();
            for (Sys s : d.systems()) {
                List<com.csg.prm.confirm.integration.dto.CatalogTreeNode> modNodes = new ArrayList<>();
                for (Mod m : s.modules()) {
                    List<com.csg.prm.confirm.integration.dto.CatalogTreeNode> cardNodes = new ArrayList<>();
                    for (Tbl t : m.tables()) {
                        cardNodes.add(new com.csg.prm.confirm.integration.dto.CatalogTreeNode(
                                "card:" + s.name() + ":" + t.code(), t.name(), "card",
                                s.name(), t.code(), CONFIRMED.contains(t.code()),
                                AUTHORIZED_TABLES.contains(t.code()), List.of()));
                    }
                    modNodes.add(new com.csg.prm.confirm.integration.dto.CatalogTreeNode(
                            "mod:" + s.name() + ":" + m.name(), m.name(), "module", null, null, false, false, cardNodes));
                }
                sysNodes.add(new com.csg.prm.confirm.integration.dto.CatalogTreeNode(
                        "sys:" + s.name(), s.name(), "system", s.name(), null, false, false, modNodes));
            }
            domains.add(new com.csg.prm.confirm.integration.dto.CatalogTreeNode(
                    "dom:" + d.name(), d.name(), "domain", null, null, false, false, sysNodes));
        }
        return domains;
    }

    /** 确权变更基线:据系统"已确权"库表合成现有确权结论(来源/关联取已确权表并集;其余为申报主体桩)。 */
    public com.csg.prm.confirm.integration.dto.ChangeBaseline baselineOf(String sysName) {
        java.util.Optional<Sys> so = findSys(sysName);
        if (so.isEmpty()) {
            return null;
        }
        Sys s = so.get();
        java.util.Set<Character> src = new java.util.TreeSet<>();
        java.util.Set<Character> rel = new java.util.TreeSet<>();
        boolean anyConfirmed = false;
        for (Mod m : s.modules()) {
            for (Tbl t : m.tables()) {
                if (!CONFIRMED.contains(t.code())) {
                    continue;
                }
                anyConfirmed = true;
                src.add(t.src().charAt(0)); // 已确权表来源类型并集(覆盖 A/D/E…)
                if (t.g()) rel.add('G');
                if (t.h()) rel.add('H');
                if (t.i()) rel.add('I');
                if (t.j()) rel.add('J');
            }
        }
        if (!anyConfirmed) {
            return null; // 无已确权库表 → 无基线(不应进入确权变更)
        }
        String srcStr = src.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("A");
        String relStr = rel.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        return new com.csg.prm.confirm.integration.dto.ChangeBaseline(
                sysName, sysUnit(s.code()), sysLevel(s.code()), sysDept(s.code()),
                "持有权、使用权、经营权", // 现有三权(基线)
                sysLevel(s.code()).contains("总部") ? "非管制" : "非管制",
                srcStr, relStr, "2025-04-20", 1);
    }

    /** 确权变更·授权影响(逐表精确):只对"本次选中/被改的库表"中已授权者,返回受影响授权 + 按触发动因给处置建议。 */
    public com.csg.prm.confirm.integration.dto.AuthImpact authImpactOf(String sysName, List<String> tableCodes, String trigger) {
        java.util.List<com.csg.prm.confirm.integration.dto.AuthImpact.Item> items = new ArrayList<>();
        if (tableCodes != null) {
            for (Tbl t : tablesOfSystem(sysName)) {
                if (!tableCodes.contains(t.code()) || !AUTHORIZED_TABLES.contains(t.code())) {
                    continue;
                }
                String status = authStatusOf(t.code());
                String suggest = status.contains("到期") ? "授权即将到期 → 评估续签或终止" : suggestByTrigger(trigger);
                items.add(new com.csg.prm.confirm.integration.dto.AuthImpact.Item(
                        t.code(), t.name(), "AUTH-" + t.code(), t.name() + "(脱敏)对外开放目录",
                        status, suggest));
            }
        }
        return new com.csg.prm.confirm.integration.dto.AuthImpact(sysName, !items.isEmpty(), items);
    }

    /** 按变更触发动因给授权处置建议(动态跟踪原则)。 */
    private String suggestByTrigger(String trigger) {
        String t = trigger == null ? "" : trigger;
        if (t.contains("到期")) return "权益到期 → 对应授权应终止";
        if (t.contains("来源")) return "来源权益变动 → 评估续签或收窄授权范围";
        if (t.contains("管理") || t.contains("监管")) return "管理要求变更 → 授权暂停复核";
        if (t.contains("新增")) return "数据新增 → 评估是否纳入授权范围";
        return "评估对在用授权的影响(暂停/续签/终止)";
    }

    private List<Tbl> tablesOfSystem(String sysName) {
        return findSys(sysName).map(s -> s.modules().stream().flatMap(m -> m.tables().stream()).toList())
                .orElse(java.util.List.of());
    }

    private java.util.Optional<Sys> findSys(String sysName) {
        return CATALOG.stream().flatMap(d -> d.systems().stream()).filter(s -> s.name().equals(sysName)).findFirst();
    }

    private int countSys(Sys s, String status) {
        return (int) s.modules().stream().flatMap(m -> m.tables().stream()).filter(t -> visible(t.code(), status)).count();
    }

    private int countDomain(Dom d, String status) {
        return d.systems().stream().mapToInt(s -> countSys(s, status)).sum();
    }

    // 系统责任信息桩(模拟 TW_DATA_CARD.MGT_USER / MGT_USER_PHONE,接入后由平台带出)
    private String sysOwner(String sysCode) {
        return switch (sysCode) {
            case "MKT" -> "李明";
            case "AMR" -> "王强";
            case "VEH" -> "赵敏";
            case "OA" -> "孙浩";
            case "PMS" -> "周勇";
            case "CS" -> "陈静";
            case "HR" -> "刘洋";
            case "DMS" -> "郑刚";
            case "DAS" -> "冯磊";
            case "PROJ" -> "许伟";
            case "SAFE" -> "何军";
            default -> "系统负责人";
        };
    }

    private String sysPhone(String sysCode) {
        return switch (sysCode) {
            case "MKT" -> "020-31001001";
            case "AMR" -> "020-31002002";
            case "VEH" -> "020-31003003";
            case "OA" -> "020-31004004";
            case "PMS" -> "020-31005005";
            case "CS" -> "020-31006006";
            case "HR" -> "020-31007007";
            case "DMS" -> "020-31008008";
            case "DAS" -> "020-31009009";
            case "PROJ" -> "020-31010010";
            case "SAFE" -> "020-31011011";
            default -> "020-00000000";
        };
    }

    // 申报主体桩(模拟 TW_DATA_CARD.MGT_UNIT 管理单位 = SYS_ORGANIZATION.BIZ_ORG_NAME 组织名)
    private String sysUnit(String sysCode) {
        return switch (sysCode) {
            case "MKT", "AMR", "CS", "DMS", "DAS" -> "广东电网有限责任公司";
            case "VEH", "OA", "HR", "SAFE" -> "中国南方电网有限责任公司";
            case "PMS" -> "广西电网有限责任公司";
            case "PROJ" -> "南方电网建设公司";
            default -> "中国南方电网有限责任公司";
        };
    }

    // 管理部门桩(MGT_MNG_DEPT = SYS_ORGANIZATION.BIZ_ORG_DESC 组织部门)
    private String sysDept(String sysCode) {
        return switch (sysCode) {
            case "MKT" -> "市场营销部";
            case "AMR" -> "计量中心";
            case "VEH", "OA" -> "办公室(后勤部)";
            case "PMS" -> "生产技术部";
            case "CS" -> "客户服务中心";
            case "HR" -> "人力资源部";
            case "DMS" -> "系统运行部";
            case "DAS" -> "配网管理部";
            case "PROJ" -> "基建部";
            case "SAFE" -> "安全监管部";
            default -> "数字化部";
        };
    }

    // 单位层级桩(SYS_ORGANIZATION.ORG_TYPE 单位层级:公司总部/分省公司/专业子公司)
    private String sysLevel(String sysCode) {
        return switch (sysCode) {
            case "VEH", "OA", "HR", "SAFE" -> "公司总部";
            default -> "分省公司";
        };
    }
}
