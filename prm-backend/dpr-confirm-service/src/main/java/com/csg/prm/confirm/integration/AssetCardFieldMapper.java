package com.csg.prm.confirm.integration;

import com.csg.prm.confirm.integration.dto.AssetEquityVO;
import com.csg.prm.confirm.integration.dto.AssetPropertyVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产卡片集成适配层(Anti-Corruption Layer)。
 * 产权模块对外暴露稳定的语义契约({@link AssetPropertyVO});与数据资产管理平台真实表/列的差异隔离在此层。
 *
 * 写回目标 = 平台产权元数据表 AU_TABLE_META_DATA(表级产权信息),真实列已对接:
 *  SOURCE_JUDGE(来源判定) / SOURCE_MAIN_NAME(来源主体) / SOURCE_DESC(来源说明) /
 *  IS_CHECK·CHECK_DESC(行政监管G) / IS_PRIVACY·PRIVACY_DESC(个人隐私H) /
 *  IS_BUS_SECRET·BUS_SECRET_DESC(第三方商密I) / IS_EQUITY·EQUITY_DESC(其他权益协议J) /
 *  AUTH_TIME(确权时间)。布尔列为 NUMBER(1,0) → 输出 0/1。
 *  CARD_ID = TW_DATA_CARD.ID(资产卡片主键);平台据卡片解析 SYS_ID/TABLE_ID 定位行。
 */
@Component
public class AssetCardFieldMapper {

    /** 语义维度 → AU_TABLE_META_DATA 列名(数据字典,便于追溯;布尔/四维由方法派生)。 */
    private static final Map<String, String> FIELD = new LinkedHashMap<>();

    static {
        FIELD.put("sourceMethod", "SOURCE_JUDGE");
        FIELD.put("sourceSubject", "SOURCE_MAIN_NAME");
        FIELD.put("sourceLimit", "SOURCE_DESC");
        FIELD.put("regulated", "IS_CHECK/CHECK_DESC");
        FIELD.put("privacy", "IS_PRIVACY/PRIVACY_DESC");
        FIELD.put("tradeSecret", "IS_BUS_SECRET/BUS_SECRET_DESC");
        FIELD.put("agreement", "IS_EQUITY/EQUITY_DESC");
        FIELD.put("confirmTime", "AUTH_TIME");
    }

    public String platformField(String semanticKey) {
        return FIELD.getOrDefault(semanticKey, semanticKey);
    }

    /**
     * 把产权契约转为平台产权元数据表 AU_TABLE_META_DATA 的列结构,供平台直接 UPSERT。
     * G–J 四维"是否"由 ConfirmApply.relationIdentification(含 G/H/I/J 标记)+ regulated/涉第三方派生。
     */
    public Map<String, Object> toPlatformProperty(AssetPropertyVO v) {
        Map<String, Object> o = new LinkedHashMap<>();
        // 关联键:平台据卡片ID解析 SYS_ID/TABLE_ID 定位 AU_TABLE_META_DATA 行
        o.put("CARD_ID", v.assetId());
        // 来源
        o.put("SOURCE_JUDGE", v.sourceMethod());
        o.put("SOURCE_MAIN_NAME", v.sourceSubject());
        o.put("SOURCE_DESC", v.sourceLimit());
        // G 行政监管
        o.put("IS_CHECK", bool01(v.involvesRegulation()));
        o.put("CHECK_DESC", v.regulated());
        // H 用户个人/家庭隐私
        o.put("IS_PRIVACY", bool01(v.involvesPrivacy()));
        o.put("PRIVACY_DESC", v.privacyInfo());
        // I 第三方商业机密
        o.put("IS_BUS_SECRET", bool01(v.involvesTradeSecret()));
        o.put("BUS_SECRET_DESC", v.thirdPartyInfo());
        // J 其他数据权益约束协议
        o.put("IS_EQUITY", bool01(v.involvesThirdPartyAgreement()));
        o.put("EQUITY_DESC", v.relationSubject());
        // 确权时间
        o.put("AUTH_TIME", v.confirmTime() == null ? null : v.confirmTime().toString());
        return o;
    }

    /**
     * 权益条目写回载荷。平台「权益基本信息」表字典尚未提供,暂以语义键直出(TODO:拿到平台权益表后对齐)。
     */
    public List<Map<String, Object>> toPlatformEquity(List<AssetEquityVO> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (list == null) {
            return out;
        }
        for (AssetEquityVO e : list) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("cardNo", e.cardNo());
            o.put("rightType", e.rightType());
            o.put("rightOwner", e.rightOwner());
            o.put("rightSource", e.rightSource());
            o.put("scope", e.scope());
            o.put("cardStatus", e.cardStatus());
            out.add(o);
        }
        return out;
    }

    /** 布尔 → NUMBER(1,0) 的 0/1;null(占位态/未填)按 0 处理。 */
    private static Integer bool01(Boolean b) {
        return Boolean.TRUE.equals(b) ? 1 : 0;
    }
}
