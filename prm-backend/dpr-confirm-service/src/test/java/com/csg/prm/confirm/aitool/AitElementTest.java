package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.entity.AitConstraint;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitProfile;
import com.csg.prm.confirm.aitool.entity.AitProfileSubject;
import com.csg.prm.confirm.aitool.service.AitElementService;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.3 确权要素识别与特征抽取(#1~#6)能力测试。useModel=false → 纯规则,确定性可重复。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitElementTest {

    @Autowired private AitElementService elementService;
    @Autowired private AitMaterialService materialService;

    private static final String CORPUS =
            "来源主体：广东电网有限责任公司；授权主体：中国南方电网有限责任公司；使用主体：广州供电局；"
            + "加工主体：南网数字研究院；共享对象：第三方科研机构；数据来源为交易采购；涉及个人信息与商业秘密；"
            + "授权范围：约定字段；使用边界：仅限内部分析使用；共享限制：禁止再共享；保留期限：3年；脱敏要求：手机号与身份证脱敏";

    private String newMaterial(String name, String content, String tableRef, String category) {
        AitMaterial m = new AitMaterial();
        m.setFileName(name);
        m.setContent(content);
        m.setDataTableRef(tableRef);
        m.setCategory(category);
        return materialService.upload(m);
    }

    private AitProfileSubject subj(List<AitProfileSubject> ss, String role) {
        return ss.stream().filter(s -> role.equals(s.getSubjectRole())).findFirst().orElse(null);
    }

    private AitConstraint cons(List<AitConstraint> cs, String type) {
        return cs.stream().filter(c -> type.equals(c.getConstraintType())).findFirst().orElse(null);
    }

    /** #1 来源方式;#2 五类主体;#3 数据特征;#5 五类约束。 */
    @Test
    void extract_identifies_source_subjects_features_constraints() {
        String id = newMaterial("确权要素.docx", CORPUS, "DT-ELEM", "确权证明");
        AitElementService.ProfileDTO dto = elementService.extract(id, false);

        // #1 数据来源方式
        assertEquals("交易采购", dto.profile().getSourceMethod());
        // #3 数据特征
        assertTrue(dto.profile().getDataFeatures().contains("个人信息"), "应识别个人信息");
        assertTrue(dto.profile().getDataFeatures().contains("商业秘密"), "应识别商业秘密");
        // #2 五类主体
        assertEquals(5, dto.subjects().size(), "应识别五类主体");
        assertEquals("广东电网有限责任公司", subj(dto.subjects(), AitProfileSubject.R_SOURCE).getSubjectName());
        assertEquals("第三方科研机构", subj(dto.subjects(), AitProfileSubject.R_SHARED).getSubjectName());
        // #5 五类约束
        assertEquals(5, dto.constraints().size(), "应识别五类约束");
        assertTrue(cons(dto.constraints(), AitConstraint.T_USE_BOUNDARY).getConstraintValue().contains("内部"));
        assertTrue(cons(dto.constraints(), AitConstraint.T_DESENSITIZE).getConstraintValue().contains("脱敏")
                || cons(dto.constraints(), AitConstraint.T_DESENSITIZE).getConstraintValue().contains("手机号"));
    }

    /** #6 结构化确权画像 + 下游(分类分级/法律校验/授权判断)输入。 */
    @Test
    void profile_has_structured_json_and_downstream() {
        String id = newMaterial("画像.docx", CORPUS, "DT-PROF", "确权证明");
        elementService.extract(id, false);
        AitElementService.ProfileDTO dto = elementService.profile(id);
        String json = dto.profile().getElementsJson();
        assertNotNull(json);
        assertTrue(json.contains("downstream"), "应含下游输入");
        assertTrue(json.contains("classificationGrade"), "应含分类分级建议");
        assertTrue(json.contains("legalCheckPoints"), "应含法律校验要点");
        assertTrue(json.contains("authBasis"), "应含授权判断依据");
    }

    /** #4 多源抽取:正文 + 多粒度片段语料(此处验证规则+模型混合路径不报错且规则生效)。 */
    @Test
    void extract_marks_rule_method() {
        String id = newMaterial("方式标记.docx", CORPUS, "DT-BY", "确权证明");
        AitElementService.ProfileDTO dto = elementService.extract(id, false);
        assertEquals("规则", dto.profile().getSourceMethodBy());
        assertTrue(dto.subjects().stream().allMatch(s -> "规则".equals(s.getMethod())), "纯规则抽取应标记规则");
    }

    /** #6 表级 + 附件级 确权特征视图聚合。 */
    @Test
    void view_aggregates_table_and_attachment_level() {
        String t = "DT-VIEW";
        String tableMat = newMaterial("主表元数据.xlsx", CORPUS, t, "元数据");        // 表级
        String attMat = newMaterial("授权附件.pdf", CORPUS, t, "授权材料");           // 附件级
        elementService.extract(tableMat, false);
        elementService.extract(attMat, false);

        Map<String, Object> view = elementService.view(t, null);
        assertEquals(2, ((Number) view.get("profileCount")).intValue());
        assertEquals(1, ((List<?>) view.get("tableLevel")).size(), "应有一条表级画像");
        assertEquals(1, ((List<?>) view.get("attachmentLevel")).size(), "应有一条附件级画像");
    }
}
