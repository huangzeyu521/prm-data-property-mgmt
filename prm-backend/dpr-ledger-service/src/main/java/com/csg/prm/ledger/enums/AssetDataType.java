package com.csg.prm.ledger.enums;

/**
 * 数据资产形态类型 —— {@code DataAssetInfo.assetType}(物理列 CEC_ASSET_TYPE)的取值登记表。
 *
 * <p><b>为什么存在这个枚举(预留口子,非功能实现)</b><br>
 * 母平台「数据资产管理平台 V3.6」同时纳管结构化数据(库表)与非结构化数据(由独立的
 * 「非结构化数据服务平台」承载)。但本模块当前确权链路是<b>库表范式</b>:确权粒度到数据表、
 * A–J 要素从库表元数据带出、{@code existTable=false} 即不可确权(见
 * {@code dpr-confirm-service} 的 {@code PlatformTableMeta} / {@code AssetTableMetaService})。
 *
 * <p>依据《附录F:中国南方电网有限责任公司数据确权授权业务指导书(征求意见稿)》:该指导书
 * <b>全文未区分结构化/非结构化</b>,适用范围用"数据"大口径(不排斥非结构化),但其填报工具
 * 收敛到"数据表 + 元数据唯一ID",<b>未给出非结构化资产的确权填报要素</b>。因此本期按 YAGNI
 * 只<b>登记类型枚举</b>,不实现非结构化确权。
 *
 * <p><b>本枚举刻意不被任何业务分支消费</b>——{@code assetType} 仍以 {@code String} 原样存取,
 * 仅在此集中登记合法取值与"是否已支持确权"。待附录F(或单独的非结构化数据指引)发布非结构化
 * 填报要素后,再按既定方案:引入 {@code ConfirmTarget} 抽象替代 {@code ConfirmTableItem} 的
 * 库表硬绑定,届时以本枚举为分叉轴。在此之前,请勿据此新增分支逻辑。
 */
public enum AssetDataType {

    /** 结构化数据(库表)。当前确权链路<b>仅</b>支持此形态,与附录F 表级填报范式一致。 */
    STRUCTURED("结构化", true),

    /**
     * 非结构化数据(文档/影像/音视频/对象等,源自「非结构化数据服务平台」)。
     * <b>预留位:本期不支持确权</b>——无库表/字段元数据,且附录F 暂无对应填报要素。
     * 待制度补齐后按 {@code ConfirmTarget} 抽象方案落地。
     */
    UNSTRUCTURED("非结构化", false);

    /** 与 CEC_ASSET_TYPE 列存量取值一致的中文标签(见 data.sql,现网全为"结构化")。 */
    private final String label;

    /** 当前确权链路是否已支持该形态。{@code false} 表示仅登记、暂不可确权。 */
    private final boolean confirmSupported;

    AssetDataType(String label, boolean confirmSupported) {
        this.label = label;
        this.confirmSupported = confirmSupported;
    }

    public String getLabel() {
        return label;
    }

    public boolean isConfirmSupported() {
        return confirmSupported;
    }

    /**
     * 按中文标签解析,未知/空值返回 {@code null}(由调用方决定兜底,本枚举不做隐式默认)。
     */
    public static AssetDataType fromLabel(String label) {
        if (label == null) {
            return null;
        }
        String trimmed = label.trim();
        for (AssetDataType type : values()) {
            if (type.label.equals(trimmed)) {
                return type;
            }
        }
        return null;
    }
}
