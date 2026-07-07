package com.csg.prm.authorize;

import com.csg.prm.authorize.dto.AgreementElementsVO;
import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.service.impl.AppendixDRenderer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 附录D 渲染一致性锁定测试:法律条款须逐字对齐《南方电网数据授权运营协议.docx》正式范本。
 * 模板换版时先改范本再改渲染器——本测试红了才允许动条款文本,防条款漂移。
 */
class AppendixDRendererTest {

    private final AppendixDRenderer renderer = new AppendixDRenderer();

    private String render(AuthAgreement ag) {
        AuthAgreement base = ag != null ? ag : new AuthAgreement();
        base.setAgreementNo("XY-TEST");
        base.setGranteeOrg("广州供电局");
        return renderer.render(AgreementElementsVO.of(base, null), base);
    }

    @Test
    void legal_clauses_match_official_template() {
        String html = render(null);
        // 序言(范本口径:无「有偿/无偿」措辞)
        assertTrue(html.contains("协议中规定甲方许可乙方使用在甲方范围内的数据"), "序言须对齐范本");
        assertFalse(html.contains("有偿/无偿"), "范本已无有偿/无偿措辞");
        // 表1 时间范围含「项目期限不得超过协议有效期」约束
        assertTrue(html.contains("具体项目数据使用期限不得超过本协议有效期"), "表1 范本原文");
        // 第八章 保密义务(承诺函必签条款)
        assertTrue(html.contains("乙方履行本协议须签署《保密承诺函》"), "第八章范本原文");
        // 第七章 不可抗力定义
        assertTrue(html.contains("不能预见、不能避免并不能克服的客观情况"), "不可抗力定义范本原文");
        // 外部许可8条件全量呈现
        assertTrue(html.contains("涉及第三方共同生产数据,应取得第三方许可"));
        assertTrue(html.contains("涉及用户个人/家庭隐私的,应当确保服务过程中获取用户许可"));
        assertTrue(html.contains("其他协议约束情况下条件满足"));
        // 生效条款
        assertTrue(html.contains("本协议经双方法定代表人或授权代表签字、单位盖章后生效"));
        // 附件2 备案义务(第四章(三))
        assertTrue(html.contains("需在甲方处进行备案"));
    }

    @Test
    void draft_shows_placeholders_final_fills_negotiated_values() {
        // 草案:协商项渲染为下划线占位
        String draft = render(null);
        assertTrue(draft.contains("人民币 _______________ 万元"), "违约金草案占位");
        assertTrue(draft.contains("本协议正本一式四份,甲乙双方各两份"), "份数缺省四份");
        // 落定:全部据实填充
        AuthAgreement ag = new AuthAgreement();
        ag.setValidUntil(LocalDateTime.of(2029, 6, 30, 0, 0));
        ag.setGeoScope("南方电网经营区域(粤桂滇黔琼)");
        ag.setSecurityEncrypt("传输通道国密加密");
        ag.setSecurityAccess("最小授权,按需开通");
        ag.setSecurityAudit("操作全留痕");
        ag.setBenefitAllocation("按调用次数计费,收益 7:3 分成");
        ag.setPenaltyAmount("50");
        ag.setDisputeMethod("提交广州仲裁委员会(仲裁地点为广州)仲裁");
        ag.setServiceDelivery("邮箱:legal@csg.cn;地址:广州市天河区,收件人:法务部");
        ag.setCopiesCount(6);
        String fin = render(ag);
        assertTrue(fin.contains("2029-06-30"), "止日填充");
        assertTrue(fin.contains("南方电网经营区域(粤桂滇黔琼)"), "地理范围填充");
        assertTrue(fin.contains("传输通道国密加密"), "表2 数据加密行");
        assertTrue(fin.contains("最小授权,按需开通"), "表2 访问控制行");
        assertTrue(fin.contains("操作全留痕"), "表2 操作审计行");
        assertTrue(fin.contains("收益 7:3 分成"), "第六章收益分配补充");
        assertTrue(fin.contains("人民币 50 万元"), "违约金填充");
        assertTrue(fin.contains("广州仲裁委员会"), "争议方式填充");
        assertTrue(fin.contains("legal@csg.cn"), "送达信息填充");
        assertTrue(fin.contains("本协议正本一式六份,甲乙双方各三份"), "份数按落定值");
        assertFalse(fin.contains("人民币 _______________ 万元"), "落定后不得残留违约金占位");
    }
}
