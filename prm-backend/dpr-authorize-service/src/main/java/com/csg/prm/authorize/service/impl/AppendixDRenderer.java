package com.csg.prm.authorize.service.impl;

import com.csg.prm.authorize.dto.AgreementElementsVO;
import com.csg.prm.authorize.entity.AuthAgreement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 《南方电网数据授权运营协议》(附录D)渲染器。
 *
 * 法律条款逐字对齐《南方电网数据授权运营协议.docx》正式范本(单一真源,条款文本在本类集中维护,
 * 由 AppendixDRendererTest 锁定关键条款防漂移);可变要素两层数据驱动:
 * ① 来源要素(AgreementElementsVO:乙方/权益/场景/附件1清单)取自申请单/批量清单;
 * ② 协商要素(AuthAgreement 要素落定:止日/地理范围/安全三行/收益分配/违约金/争议方式/送达/份数)
 *   由「要素落定」环节填空——落定前渲染为下划线占位(草案),落定后据实填充(正式稿快照)。
 */
@Component
public class AppendixDRenderer {

    private static final String U = "_______________"; // 待填下划线占位

    /** 渲染附录D协议为完整 HTML(可作 application/msword 下载)。ag=协商要素来源(要素落定)。 */
    public String render(AgreementElementsVO e, AuthAgreement ag) {
        StringBuilder h = new StringBuilder(8192);
        h.append("<html xmlns:w=\"urn:schemas-microsoft-com:office:word\"><head><meta charset=\"utf-8\">")
         .append("<style>")
         .append("body{font-family:'SimSun','宋体',serif;font-size:14px;line-height:1.8;margin:32px}")
         .append("h1{text-align:center;font-size:20px}h2{font-size:15px;margin-top:18px}")
         .append("table{border-collapse:collapse;width:100%;margin:8px 0}td,th{border:1px solid #000;padding:5px 8px;font-size:13px}")
         .append(".sub{margin-left:1em}.note{color:#555;font-size:12px}.sign{margin-top:28px}")
         .append("</style></head><body>");

        h.append("<h1>南方电网数据授权运营协议</h1>");
        h.append("<p>协议编号:").append(esc(nv(e.agreementNo()))).append("</p>");
        h.append("<p>甲&nbsp;&nbsp;&nbsp;&nbsp;方:").append(U).append("(授权方)<br>")
         .append("地&nbsp;&nbsp;&nbsp;&nbsp;址:").append(U).append("<br>")
         .append("法定代表人(负责人):").append(U).append("</p>");
        h.append("<p>乙&nbsp;&nbsp;&nbsp;&nbsp;方:").append(esc(nv(e.granteeOrg()))).append("(被授权方)<br>")
         .append("地&nbsp;&nbsp;&nbsp;&nbsp;址:").append(U).append("<br>")
         .append("法定代表人(负责人):").append(U).append("</p>");

        h.append("<p>为深入贯彻落实国家“数据二十条”精神,有效激发电力数据要素价值,促进电力数据大规模社会化应用,")
         .append("本协议旨在明确甲方、乙方在数据运营过程中的权利和义务,确保各方能够在遵循法律法规的前提下,共同推动数据的有效利用和创新发展。")
         .append("协议中规定甲方许可乙方使用在甲方范围内的数据,面向社会需求单位(政府部门、外部企业、社会组织等),")
         .append("进行电力大数据加工和经营服务。</p>");
        h.append("<p>授权协议具体内容如下:</p>");

        // 一、授权数据目的、范围及内容
        h.append("<h2>一、授权数据目的、范围及内容</h2>");
        h.append("<p class='sub'>授权目的:本协议目的是在不违反国家法律法规,遵循南方电网数据管理相关制度,根据当前数据产权情况,甲方向乙方授权数据使用权、经营权。</p>");
        h.append("<p class='sub'>授权范围:本次许可使用的数据范围限于南方电网大数据中心内数据。</p>");
        h.append("<p class='sub'>具体授权内容如下:</p>");
        boolean isBatch = e.items() != null && !e.items().isEmpty();
        h.append("<p class='sub'>1.甲方向乙方授权的数据:(详见附件1.数据授权清单)。授权方式:")
         .append(esc(nv(e.authMode()))).append(";权益类型:").append(esc(nv(e.rightType()))).append("。</p>");
        String scenarioText = (e.scenario() != null && !e.scenario().isBlank()) ? esc(e.scenario())
                : (isBatch ? "各授权项详见附件1《数据授权清单》「应用场景」列" : U);
        h.append("<p class='sub'>2.授权目的和场景:").append(scenarioText).append("。</p>");

        // 二、授权时空范围(协商要素:止日/地理范围由要素落定填充,未落定按草案占位)
        h.append("<h2>二、授权时空范围</h2>");
        String until = ag != null && ag.getValidUntil() != null ? ag.getValidUntil().toLocalDate().toString()
                : (e.validDate() != null && !e.validDate().isBlank() ? e.validDate() : null);
        String period = until != null
                ? "授权有效期:自协议签订之日起至 " + esc(until)
                    + "(一般为3年,最长不超过5年)。经甲方书面同意,过期可办理续期,具体项目数据使用期限不得超过本协议有效期。"
                : "授权有效期:自协议签订之日起 " + U + " 年(一般为3年,最长不超过5年)。经甲方书面同意,过期可办理续期,具体项目数据使用期限不得超过本协议有效期。";
        String geo;
        if (ag != null && ag.getGeoScope() != null && !ag.getGeoScope().isBlank()) {
            geo = "数据使用空间范围:" + esc(ag.getGeoScope()) + "。";
        } else {
            geo = Boolean.TRUE.equals(e.crossRegion())
                    ? "数据使用空间范围:跨区域使用(具体区域详见附件1.数据授权清单)。"
                    : "数据使用空间范围:广东省行政区域内。";
        }
        h.append("<p class='note'>表1 数据授权时空范围表</p><table>")
         .append("<tr><th width='120'>维度</th><th>具体要求</th></tr>")
         .append("<tr><td>时间范围</td><td>").append(period).append("</td></tr>")
         .append("<tr><td>地理范围</td><td>").append(geo).append("</td></tr></table>");

        // 三、授权数据要求
        h.append("<h2>三、授权数据要求</h2>");
        h.append("<p class='sub'>(一)针对具体项目使用的数据,乙方应按照《中国南方电网有限责任公司数据确权授权管理业务指导书》的相关规定提出数据加工经营需求,经甲方审核通过后获取相关数据。乙方对数据使用用途发生变化时,应重新向甲方提出数据需求申请。</p>");
        h.append("<p class='sub'>(二)数据安全要求</p>");
        // 表2 三行(协商要素:要素落定逐行填空;一事一议申请单 securityReq 作数据加密行兜底)
        String secEncrypt = pick(ag != null ? ag.getSecurityEncrypt() : null,
                (e.securityReq() != null && !e.securityReq().isBlank()) ? e.securityReq() : null);
        String secAccess = pick(ag != null ? ag.getSecurityAccess() : null, null);
        String secAudit = pick(ag != null ? ag.getSecurityAudit() : null, null);
        h.append("<p class='note'>表2 数据安全要求表</p><table>")
         .append("<tr><th width='120'>要求项</th><th>具体要求</th></tr>")
         .append("<tr><td>数据加密</td><td>").append(secEncrypt).append("</td></tr>")
         .append("<tr><td>访问控制</td><td>").append(secAccess).append("</td></tr>")
         .append("<tr><td>操作审计</td><td>").append(secAudit).append("</td></tr></table>");
        h.append("<p class='sub'>被授权方开展数据运营管理过程中,须指定数据安全责任人,定期参加安全培训;发现数据泄露应立即启动应急熔断机制,并于2小时内向甲方报告。</p>");
        h.append("<p class='sub'>(三)数据原始权益说明:见南方电网数据资产管理平台数据权益卡片信息。</p>");
        h.append("<p class='sub'>(四)数据授权运营的外部许可条件要求(已据申请要素勾选,其余由签署方据实核定):</p>");
        h.append(licenseChecklist(e));
        h.append("<p class='sub'>(五)数据限制说明</p>");
        h.append("<p class='note'>表3 数据限制说明表</p><table>")
         .append("<tr><th width='130'>限制条目</th><th>要求</th></tr>")
         .append("<tr><td>被授权方禁止行为</td><td>超出授权目的、时空范围、安全要求使用或经营数据;不满足第三条第四款情况下使用和经营数据;不符合公司数据安全、信息安全管理要求。</td></tr>")
         .append("<tr><td>例外条款</td><td>政府监管要求需调用数据的,须经公司书面批准。</td></tr></table>");

        // 四、甲方权利义务
        h.append("<h2>四、甲方的主要权利义务</h2>");
        h.append("<p class='sub'>(一)甲方应当依据乙方数据申请需求,履行审批程序,并协助向乙方提供其所需的数据,审批不通过的,甲方有权拒绝向乙方提供数据且不承担任何责任。</p>");
        h.append("<p class='sub'>(二)甲方对数据的真实性、完整性负责,乙方应对甲方提供的数据进行甄别,乙方有权提出数据问题,甲方应当配合整改。</p>");
        h.append("<p class='sub'>(三)甲方有权对乙方数据使用、经营行为进行监督管理,乙方通过取得数据经营权对外提供数据产品或服务的,需在甲方处进行备案。</p>");

        // 五、乙方权利义务
        h.append("<h2>五、乙方的主要权利义务</h2>");
        String[] yi = {
            "(一)乙方应当在本次许可使用协议范围内行使权利,不得超过业务合理需求过度采集或使用数据。原则上乙方不得对甲方源数据进行修改、补充或删除等操作,应事先征得甲方的同意。",
            "(二)除甲方书面同意,乙方禁止对第三方开放、转售或共享原始数据,用于非授权地理区域或业务领域、逆向工程破解匿名化信息数据运营工作及行为。",
            "(三)乙方对数据使用经营不得用于本协议约定以外的其它目的。",
            "(四)乙方应当按照法律法规、国家政策、南方电网及甲方相关制度要求履行保密义务,确保数据的完整性和安全性,防止数据泄露、篡改或丢失,严格控制数据的传播和阅知范围。乙方在本协议项下的保密义务长期有效。",
            "(五)乙方需自行进行产品或服务的安全合规审核,组织相关专家、机构开展安全合规评审工作。加工使用数据、对外提供数据产品中引发任何第三方投诉或法律纠纷的,由乙方自行承担一切法律责任并处理纠纷。",
            "(六)若乙方在使用数据过程中发现数据存在纰漏或错误的,应当及时反馈给甲方。",
            "(七)乙方应按照南方电网网络安全、信息安全、数据安全等制度要求,做好个人信息、电力用户信息保护,防止过度采集、非授权访问、非法使用和信息泄露。"
        };
        for (String s : yi) h.append("<p class='sub'>").append(s).append("</p>");

        // 六、数据收益分配(协商要素:协议级补充约定优先(批量清单级),次取申请单要素,再落范本缺省条款)
        h.append("<h2>六、数据收益分配</h2>");
        String benefitSrc = ag != null && ag.getBenefitAllocation() != null && !ag.getBenefitAllocation().isBlank()
                ? ag.getBenefitAllocation()
                : (e.benefitAllocation() != null && !e.benefitAllocation().isBlank() ? e.benefitAllocation() : null);
        String benefit = benefitSrc != null ? esc(benefitSrc)
                : "甲乙双方应按照双方投入和友好协商对数据使用费/数据产品服务费/数据产品收入进行收益分配。具体分配方法和分配原则可参照《公司数据流通管理细则》中有关数据价值分配的相关要求执行。";
        h.append("<p class='sub'>").append(benefit).append("</p>");

        // 七、协议的变更、解除
        h.append("<h2>七、协议的变更、解除</h2>");
        h.append("<p class='sub'>(一)协议的变更与解除,由双方协商一致并签署书面协议。</p>");
        h.append("<p class='sub'>(二)发生下列情况之一者,根据有关法律规定可变更或解除本协议:1.由于不可抗力或意外事件,使本协议无法履行或部分无法履行;2.乙方违反国家法律法规、数据主管部门规定及南方电网公司相关制度规定的,其使用权随即无条件终止;3.若乙方违反南方电网公司相关制度,甲方可随时终止相关项目的数据共享开放,并无条件解除本协议;4.若因有关法律、法规、政策或上级主管部门要求需要变更、解除许可使用协议的,乙方应予以配合。</p>");
        h.append("<p class='sub'>不可抗力是指合同双方在签署本合同时不能预见、不能避免并不能克服的客观情况(地震、台风、水灾、火灾,以及政府行为、战争、瘟疫等)。受影响一方应在不可抗力发生后10天内提供有效证明并采取合理措施减少损失;影响合同履行超过 ")
         .append(U).append(" 天的,双方均有权解除合同。</p>");
        h.append("<p class='sub'>(三)一方要求变更或解除本协议时,应及时书面通知另一方。(四)变更或解除本协议致使对方遭受损失的,应由责任方负责赔偿。</p>");

        // 八、保密义务
        h.append("<h2>八、保密义务</h2>");
        h.append("<p class='sub'>乙方履行本协议须签署《保密承诺函》,保密义务按《保密承诺函》约定执行。</p>");

        // 九、违约责任(协商要素:违约金金额)
        h.append("<h2>九、违约责任</h2>");
        String penalty = ag != null && ag.getPenaltyAmount() != null && !ag.getPenaltyAmount().isBlank()
                ? "人民币 " + esc(ag.getPenaltyAmount()) + " 万元" : "人民币 " + U + " 万元";
        h.append("<p class='sub'>本合同生效后,甲乙双方均应全面履行合同义务。任何一方违约,均应当承担违约责任,并赔偿对方由此受到的损失。乙方违反本协议的任何规定,应当及时纠正违约行为,向甲方支付")
         .append(penalty).append("的违约金,违约金不足以弥补甲方损失的,还应赔偿甲方因此造成的损失。同时,甲方有权收回数据及相关资料并无条件解除本协议,乙方不得保留数据的任何拷贝形式。</p>");

        // 十、争议解决(协商要素:争议方式/送达信息)
        h.append("<h2>十、争议解决</h2>");
        if (ag != null && ag.getDisputeMethod() != null && !ag.getDisputeMethod().isBlank()) {
            h.append("<p class='sub'>(一)许可使用履行过程中若发生争议,双方应通过友好协商的方式解决。不能协商解决的,按以下方式解决:")
             .append(esc(ag.getDisputeMethod())).append(",裁决/判决为终局。</p>");
        } else {
            h.append("<p class='sub'>(一)许可使用履行过程中若发生争议,双方应通过友好协商的方式解决。不能协商解决的,按以下第 ")
             .append(U).append(" 种方式解决:1.向甲方所在地人民法院起诉;2.提交 ").append(U)
             .append(" 仲裁委员会(仲裁地点为 ").append(U).append(")仲裁,裁决为终局。</p>");
        }
        if (ag != null && ag.getServiceDelivery() != null && !ag.getServiceDelivery().isBlank()) {
            h.append("<p class='sub'>(二)双方确认司法机关可通过以下方式送达诉讼法律文书,送达时间以最先送达的为准,适用于各个司法阶段:")
             .append(esc(ag.getServiceDelivery())).append("。</p>");
        } else {
            h.append("<p class='sub'>(二)双方确认司法机关可通过手机短信(").append(U).append(")、电子邮件(")
             .append(U).append(")、邮寄(地址:").append(U).append(",收件人:").append(U)
             .append(")任一方式送达诉讼法律文书,送达时间以最先送达的为准,适用于各个司法阶段。</p>");
        }
        h.append("<p class='sub'>(三)因一方违反本合同导致诉讼、仲裁的,违约方应承担守约方为此发生的律师费、公证费、鉴定费、保全保险费、调查取证费等费用。</p>");

        // 十一、其他(协商要素:正本份数,双方各半)
        h.append("<h2>十一、其他</h2>");
        h.append("<p class='sub'>(一)本协议经双方法定代表人或授权代表签字、单位盖章后生效。</p>");
        int copies = ag != null && ag.getCopiesCount() != null && ag.getCopiesCount() >= 2 ? ag.getCopiesCount() : 4;
        h.append("<p class='sub'>(二)本协议正本一式").append(cn(copies)).append("份,甲乙双方各")
         .append(cn(copies / 2)).append("份。</p>");
        h.append("<p class='sub'>(三)本协议未尽事宜,由甲乙双方另行协议解决。</p>");

        // 签署页
        h.append("<div class='sign'><p>【以下无正文,为本协议(协议编号:").append(esc(nv(e.agreementNo()))).append(")签署页】</p>");
        h.append("<table><tr><td width='50%'>甲方(盖章):<br><br>法定代表人(负责人)或授权代表(签名):").append(U)
         .append("<br><br>签订日期:&nbsp;&nbsp;&nbsp;年&nbsp;&nbsp;月&nbsp;&nbsp;日</td>")
         .append("<td>乙方(盖章):<br><br>法定代表人(负责人)或授权代表(签名):").append(U)
         .append("<br><br>签订日期:&nbsp;&nbsp;&nbsp;年&nbsp;&nbsp;月&nbsp;&nbsp;日</td></tr></table></div>");

        // 附件1 数据授权清单
        h.append("<h2>附件1:数据授权清单</h2>");
        h.append(attachment1(e));

        h.append("<p class='note'>注:本协议草案由数据产权管理系统按附录D《南方电网数据授权运营协议》自动生成。可变要素据授权申请/清单填充,法律条款依附录D范本;下划线项为协商/线下签署项,请据实填写后用印生效。</p>");
        h.append("</body></html>");
        return h.toString();
    }

    /** 外部许可8条件:据涉第三方来源/涉敏感类型自动勾选,其余留待签署方核定。 */
    private String licenseChecklist(AgreementElementsVO e) {
        boolean third = e.thirdPartySource() != null && !e.thirdPartySource().isBlank();
        String sens = e.sensitiveType() == null ? "" : e.sensitiveType();
        boolean privacy = sens.contains("隐私") || sens.contains("个人");
        boolean secret = sens.contains("商密") || sens.contains("商业秘密") || sens.contains("秘密");
        StringBuilder b = new StringBuilder("<table><tr><th width='40'>勾选</th><th>外部许可条件</th></tr>");
        b.append(row(false, "涉及第三方共同生产数据,应取得第三方许可。"));
        b.append(row(false, "涉及公共数据授权,应取得相应公共数据管理部门授权。"));
        b.append(row(false, "公开采集数据:应确保被采集主体许可或其权益不受侵害。"));
        b.append(row(third, "交易采购数据:应确保数据采购来源方许可,或在未约定情况下不侵犯其权益。"));
        b.append(row(false, "涉及行政监管要求(关键基础设施、影响国家安全等),需取得监管部门同意的,应当取得监管部门同意。"));
        b.append(row(privacy, "涉及用户个人/家庭隐私的,应当确保服务过程中获取用户许可。"));
        b.append(row(secret, "涉及第三方商业机密的,应当确保服务过程中获取第三方许可。"));
        b.append(row(false, "其他协议约束情况下条件满足。"));
        return b.append("</table>").toString();
    }

    private String row(boolean checked, String text) {
        return "<tr><td style='text-align:center'>" + (checked ? "☑" : "□") + "</td><td>" + text + "</td></tr>";
    }

    /** 附件1《数据授权清单》:批量取清单各项,专项合成单行。 */
    private String attachment1(AgreementElementsVO e) {
        List<AgreementElementsVO.Item> items = e.items();
        if (items == null || items.isEmpty()) {
            // 专项:用顶层要素合成一行
            items = new ArrayList<>();
            if (e.dataTable() != null || e.scenario() != null) {
                items.add(new AgreementElementsVO.Item(e.sysName(), e.dataTable(), e.schemaName(),
                        e.rightType(), e.scenario(), e.validDate()));
            }
        }
        StringBuilder b = new StringBuilder("<table>")
                .append("<tr><th>申请单位</th><th>系统名称</th><th>模式名称</th><th>表名称</th><th>权益名称</th><th>应用场景</th><th>授权时效</th></tr>");
        if (items.isEmpty()) {
            b.append("<tr><td colspan='7' style='text-align:center'>(无清单明细)</td></tr>");
        } else {
            for (AgreementElementsVO.Item it : items) {
                b.append("<tr>")
                 .append("<td>").append(dash(e.granteeOrg())).append("</td>")
                 .append("<td>").append(dash(it.sysName())).append("</td>")
                 .append("<td>").append(dash(it.schemaName())).append("</td>")
                 .append("<td>").append(dash(it.dataTable())).append("</td>")
                 .append("<td>").append(dash(it.rightType())).append("</td>")
                 .append("<td>").append(dash(it.scenario())).append("</td>")
                 .append("<td>").append(esc(it.validDate() != null && !it.validDate().isBlank() ? it.validDate() : "默认两年")).append("</td>")
                 .append("</tr>");
            }
        }
        b.append("</table>");
        b.append("<p class='note'>注:面向同一使用/经营用途的多张数据表或面向多个用途的多张数据表,可多选填写。权益名称为持有权、使用权、经营权之一。</p>");
        return b.toString();
    }

    /** 协商项取值:已落定用落定值(转义),否则次选值,再否则下划线占位(草案)。 */
    private static String pick(String negotiated, String fallback) {
        if (negotiated != null && !negotiated.isBlank()) return esc(negotiated);
        if (fallback != null && !fallback.isBlank()) return esc(fallback);
        return U;
    }

    /** 份数中文数字(协议行文习惯:一式四份/各两份)。 */
    private static String cn(int n) {
        String[] c = {"零", "一", "两", "三", "四", "五", "六", "七", "八", "九", "十"};
        return n >= 0 && n <= 10 ? c[n] : String.valueOf(n);
    }

    private static String nv(String s) { return s == null ? "" : s; }
    private static String blankIf(String s) { return (s == null || s.isBlank()) ? U : s; }
    /** 清单单元格:空值显示「—」,避免空白格(并 HTML 转义)。 */
    private static String dash(String s) { return (s == null || s.isBlank()) ? "—" : esc(s); }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
