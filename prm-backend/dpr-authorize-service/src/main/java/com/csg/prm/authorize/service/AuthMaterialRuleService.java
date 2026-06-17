package com.csg.prm.authorize.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthMaterialRule;
import com.csg.prm.authorize.mapper.AuthMaterialRuleMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 授权应交材料清单——可配置规则的单一真源(对齐确权侧方案)。
 * 启动幂等种入 批量/一事一议 两场景规则;按 场景×触发条件(涉第三方/涉隐私商密)生成应交清单。
 * 材料变更=改这张表的数据,前后端同读此源,不动代码、不重部署。
 */
@Service
@Order(50)
public class AuthMaterialRuleService implements ApplicationRunner {

    public static final String SCENE_BATCH = "批量";
    public static final String SCENE_SPECIAL = "一事一议";

    /** 规则表为空/异常时的兜底,保证应交清单永不为空。 */
    private static final List<String> FALLBACK = List.of("《表5 数据授权申请单》");

    private final AuthMaterialRuleMapper mapper;

    public AuthMaterialRuleService(AuthMaterialRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed(SCENE_BATCH, "所有批量授权申请均需提供");
        seed(SCENE_SPECIAL, "发起一事一议授权申请必须提供");
    }

    /** 启用规则(按 sortNo),供前端取清单与后端校验共用——单一真源。 */
    public List<AuthMaterialRule> listEnabled(String scene) {
        return mapper.selectList(new LambdaQueryWrapper<AuthMaterialRule>()
                .eq(AuthMaterialRule::getScene, scene)
                .eq(AuthMaterialRule::getEnabled, true)
                .orderByAsc(AuthMaterialRule::getSortNo));
    }

    /** 按申请的第三方来源/敏感类型,生成应交材料名清单(后端校验/前端渲染共用)。 */
    public List<String> requiredNames(AuthApply apply) {
        String scene = AuthApply.MODE_BATCH.equals(apply.getAuthMode()) ? SCENE_BATCH : SCENE_SPECIAL;
        boolean thirdParty = StringUtils.hasText(apply.getThirdPartySource());
        boolean sensitive = StringUtils.hasText(apply.getSensitiveType());
        List<String> req = new ArrayList<>();
        for (AuthMaterialRule r : listEnabled(scene)) {
            if (hit(r, thirdParty, sensitive) && !req.contains(r.getMaterialName())) {
                req.add(r.getMaterialName());
            }
        }
        return req.isEmpty() ? new ArrayList<>(FALLBACK) : req;
    }

    private boolean hit(AuthMaterialRule r, boolean thirdParty, boolean sensitive) {
        return switch (r.getTriggerType()) {
            case AuthMaterialRule.T_ALWAYS -> true;
            case AuthMaterialRule.T_THIRD_PARTY -> thirdParty;
            case AuthMaterialRule.T_SENSITIVE -> sensitive;
            default -> false;
        };
    }

    /** 幂等种入:仅当该场景下无规则时,写入当前(联调材料清单 Excel)的授权应交材料规则。 */
    private void seed(String scene, String table5Cond) {
        Long cnt = mapper.selectCount(new LambdaQueryWrapper<AuthMaterialRule>()
                .eq(AuthMaterialRule::getScene, scene));
        if (cnt != null && cnt > 0) {
            return;
        }
        insert(scene, AuthMaterialRule.T_ALWAYS, "《表5 数据授权申请单》", "必填", "表单",
                "填写申请主体、所属系统、模式及数据表名称、申请权益类型(持有权/使用权/经营权)、使用场景及目的摘要、权益时效(默认两年)、是否跨区域跨域等。" + table5Cond + "。", 1);
        insert(scene, AuthMaterialRule.T_THIRD_PARTY, "第三方许可凭证或说明", "视情况", "凭证",
                "申请数据涉及第三方来源方式时必须提供:第三方关于数据授权的许可文件或详细情况说明。", 2);
        insert(scene, AuthMaterialRule.T_SENSITIVE, "信息授权协议", "视情况", "协议",
                "申请数据涉及个人隐私或商业秘密时必须提供:相应的信息授权协议附件(如个人隐私授权协议范本),以证明合规获取授权。", 3);
    }

    private void insert(String scene, String type, String name, String required,
                        String evidence, String detail, int sort) {
        AuthMaterialRule r = new AuthMaterialRule();
        r.setScene(scene);
        r.setTriggerType(type);
        r.setMaterialName(name);
        r.setRequired(required);
        r.setEvidenceType(evidence);
        r.setDetail(detail);
        r.setSortNo(sort);
        r.setEnabled(true);
        mapper.insert(r);
    }
}
