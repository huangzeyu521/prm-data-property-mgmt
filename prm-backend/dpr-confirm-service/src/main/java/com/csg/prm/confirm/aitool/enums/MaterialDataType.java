package com.csg.prm.confirm.aitool.enums;

import org.springframework.util.StringUtils;

/**
 * 资料类型(联调清单 行25 {@code DATA_TYPE} / 母平台 PDM 列 {@code CEC_DATA_TYPE})的规范取值登记表。
 *
 * <p><b>规范化最终值</b>:对外/入库一律存<b>编码</b> {@code 01–07},不再存中文标签。
 * 由智能确权材料归类({@code classifyCategory})产出,写入 {@code AitMaterial.category}
 * (物理列 {@code IM_AIT_MATERIAL.CEC_CATEGORY},即母平台 {@code CEC_DATA_TYPE})。
 *
 * <pre>
 *   01 元数据   02 制度附件  03 授权材料  04 合同材料
 *   05 来源说明 06 确权证明  07 其他
 * </pre>
 *
 * <p>大模型/规则分类天然输出中文类别词,统一经 {@link #codeOf(String)} 收敛为编码后落库;
 * 前端展示时再经编码→中文映射还原可读标签(兼容历史中文存量)。
 */
public enum MaterialDataType {

    METADATA("01", "元数据"),
    POLICY_ATTACHMENT("02", "制度附件"),
    AUTHORIZATION("03", "授权材料"),
    CONTRACT("04", "合同材料"),
    SOURCE_DESC("05", "来源说明"),
    CONFIRM_PROOF("06", "确权证明"),
    OTHER("07", "其他");

    /** 规范编码(入库值),与联调清单 行25 一致。 */
    private final String code;

    /** 中文标签(展示用,亦为分类匹配关键词)。 */
    private final String label;

    MaterialDataType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 把分类结果(中文类别词,或已是编码的存量)收敛为规范编码 {@code 01–07}。
     * 无法判定时归 {@code 07 其他};入参为空亦归 {@code 07}。
     */
    public static String codeOf(String text) {
        if (!StringUtils.hasText(text)) {
            return OTHER.code;
        }
        String t = text.trim();
        for (MaterialDataType d : values()) {
            // 已是编码 → 原样;含中文标签 → 取对应编码(容忍模型多余修饰)
            if (d.code.equals(t) || t.contains(d.label)) {
                return d.code;
            }
        }
        return OTHER.code;
    }

    /**
     * 编码→中文标签(展示用)。非法/历史中文值原样返回,保证前端不丢可读性。
     */
    public static String labelOf(String code) {
        if (!StringUtils.hasText(code)) {
            return OTHER.label;
        }
        for (MaterialDataType d : values()) {
            if (d.code.equals(code)) {
                return d.label;
            }
        }
        return code;
    }
}
