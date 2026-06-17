package com.csg.prm.confirm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmMaterialRule;
import com.csg.prm.confirm.mapper.ConfirmMaterialRuleMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 确权应交材料清单——可配置规则的单一真源。
 * 替代散落在前端/后端的硬编码 CODE_MATERIAL:启动种入(幂等),按场景×触发条件(A–J/涉三方)生成应交清单。
 * 材料变更=改这张表的数据,前后端同读此源,不动代码、不重部署。
 */
@Service
@Order(50)
public class ConfirmMaterialRuleService implements ApplicationRunner {

    public static final String SCENE_CONFIRM = "确权";

    /** 规则表为空/异常时的兜底,保证应交清单永不为空。 */
    private static final List<String> FALLBACK = List.of("《表1 数据确权信息清单(系统级)》");

    private static final Pattern CODE = Pattern.compile("^\\s*([A-J])");

    private final ConfirmMaterialRuleMapper mapper;

    public ConfirmMaterialRuleService(ConfirmMaterialRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedConfirmRules();
    }

    /** 启用规则(按 sortNo),供前端取清单与后端生成校验共用——单一真源。 */
    public List<ConfirmMaterialRule> listEnabled(String scene) {
        return mapper.selectList(new LambdaQueryWrapper<ConfirmMaterialRule>()
                .eq(ConfirmMaterialRule::getScene, scene)
                .eq(ConfirmMaterialRule::getEnabled, true)
                .orderByAsc(ConfirmMaterialRule::getSortNo));
    }

    /** 按申请的来源/关联识别(A–J)+涉三方标志,生成应交材料名清单(后端校验用)。 */
    public List<String> requiredNames(ConfirmApply apply) {
        List<ConfirmMaterialRule> rules = listEnabled(SCENE_CONFIRM);
        Set<String> src = codes(apply.getSourceIdentification());
        Set<String> rel = codes(apply.getRelationIdentification());
        boolean t2 = Boolean.TRUE.equals(apply.getInvolvesThirdParty());
        List<String> req = new ArrayList<>();
        for (ConfirmMaterialRule r : rules) {
            if (hit(r, src, rel, t2) && !req.contains(r.getMaterialName())) {
                req.add(r.getMaterialName());
            }
        }
        return req.isEmpty() ? new ArrayList<>(FALLBACK) : req;
    }

    private boolean hit(ConfirmMaterialRule r, Set<String> src, Set<String> rel, boolean t2) {
        return switch (r.getTriggerType()) {
            case ConfirmMaterialRule.T_ALWAYS -> true;
            case ConfirmMaterialRule.T_TABLE2 -> t2;
            case ConfirmMaterialRule.T_SOURCE -> src.contains(r.getTriggerCode());
            case ConfirmMaterialRule.T_RELATION -> rel.contains(r.getTriggerCode());
            default -> false;
        };
    }

    /** 从 "A,B" 或 "A自行生产数据,B公开采集" 抽取前导 A–J 码。 */
    private Set<String> codes(String idents) {
        Set<String> out = new LinkedHashSet<>();
        if (idents == null || idents.isBlank()) {
            return out;
        }
        for (String tok : idents.split("[,，]")) {
            Matcher m = CODE.matcher(tok);
            if (m.find()) {
                out.add(m.group(1));
            }
        }
        return out;
    }

    /** 幂等种入:仅当该场景下无规则时,写入当前(联调材料清单 Excel)的应交材料规则。 */
    private void seedConfirmRules() {
        Long cnt = mapper.selectCount(new LambdaQueryWrapper<ConfirmMaterialRule>()
                .eq(ConfirmMaterialRule::getScene, SCENE_CONFIRM));
        if (cnt != null && cnt > 0) {
            return;
        }
        int s = 0;
        // 一、核心表单(总是必交)
        insert(ConfirmMaterialRule.T_ALWAYS, null, null, "《表1 数据确权信息清单(系统级)》", "必填", "表单",
                "填写系统名称、公司主体、登记类型(初始确权/确权变更)、数据来源权益识别、信息关联权益识别及负责人信息等。", ++s);
        insert(ConfirmMaterialRule.T_ALWAYS, null, null, "数据确权证明材料(权属/来源凭证)", "必填", "凭证",
                "数据权属或来源的基础证明材料。", ++s);
        // 涉第三方时必交
        insert(ConfirmMaterialRule.T_TABLE2, null, null, "《表2 数据确权信息清单(涉及第三方权益)》", "视情况", "表单",
                "勾选涉及第三方权益(B–J)时填写:模式名称、数据表名称、来源主体名称、权益限制摘要、风险提示等。", ++s);
        // 二、证明材料——来源识别 A–F
        insert(ConfirmMaterialRule.T_SOURCE, "A", "自行生产", "数据来源设备/系统建设投入情况说明", "视情况", "说明",
                "数据来源设备、系统建设投入情况说明。", ++s);
        insert(ConfirmMaterialRule.T_SOURCE, "B", "公开采集", "公共采集情况说明(方式/方法/来源)", "视情况", "说明",
                "公共采集情况说明(必须包含采集方式、方法和采集来源)。", ++s);
        insert(ConfirmMaterialRule.T_SOURCE, "C", "公共数据授权", "公共数据授权说明", "视情况", "说明",
                "公共数据授权说明。", ++s);
        insert(ConfirmMaterialRule.T_SOURCE, "D", "共同生产", "共享/共同生产情况说明", "视情况", "说明",
                "包括共享数据、共同生产情况的说明。", ++s);
        insert(ConfirmMaterialRule.T_SOURCE, "E", "交易采购", "交易采购情况说明", "视情况", "凭证",
                "交易采购情况说明,以及相关的采购凭证附件(如:数据信息服务采购协议)。", ++s);
        insert(ConfirmMaterialRule.T_SOURCE, "F", "其他方式", "其他来源情况说明", "视情况", "说明",
                "获取来源的相关情况说明。", ++s);
        // 二、证明材料——关联识别 G–J
        insert(ConfirmMaterialRule.T_RELATION, "G", "行政监管", "行政监管要求补充说明", "视情况", "说明",
                "关于监管要求情况的相关证明或补充说明。", ++s);
        insert(ConfirmMaterialRule.T_RELATION, "H", "个人/家庭隐私", "个人/家庭隐私授权说明(如用户入网协议)", "视情况", "凭证",
                "补充说明、授权内容情况或协议附件(如:用户入网协议)。", ++s);
        insert(ConfirmMaterialRule.T_RELATION, "I", "第三方商业机密", "第三方商业机密授权说明", "视情况", "说明",
                "补充说明或第三方授权内容情况说明。", ++s);
        insert(ConfirmMaterialRule.T_RELATION, "J", "其他第三方协议", "其他第三方机构协议", "视情况", "凭证",
                "补充说明、授权内容情况或机构合作协议附件。", ++s);
    }

    private void insert(String type, String code, String label, String name,
                        String required, String evidence, String detail, int sort) {
        ConfirmMaterialRule r = new ConfirmMaterialRule();
        r.setScene(SCENE_CONFIRM);
        r.setTriggerType(type);
        r.setTriggerCode(code);
        r.setTriggerLabel(label);
        r.setMaterialName(name);
        r.setRequired(required);
        r.setEvidenceType(evidence);
        r.setDetail(detail);
        r.setSortNo(sort);
        r.setEnabled(true);
        mapper.insert(r);
    }
}
