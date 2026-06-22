package com.csg.prm.confirm.integration;

import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.integration.dto.AssetEquityVO;
import com.csg.prm.confirm.integration.dto.AssetPropertyVO;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资产卡片集成 MVP:by-assetId 装配「产权信息 / 权益基本信息」契约 + 边界态 + 适配层。
 */
@SpringBootTest
@ActiveProfiles("test")
class AssetCardIntegrationTest {

    @Autowired private AssetCardIntegrationService service;
    @Autowired private ConfirmApplyMapper applyMapper;
    @Autowired private EquityCardMapper cardMapper;

    private ConfirmApply apply(String assetId, String status, String registerType) {
        ConfirmApply a = new ConfirmApply();
        a.setApplyNo("AP-" + System.nanoTime());
        a.setAssetId(assetId);
        a.setAssetName("非车险理赔系统-理赔表");
        a.setStatus(status);
        a.setRegisterType(registerType);
        a.setRightType("持有权");
        a.setRightHolder("鼎和保险");
        a.setRespDept("数字化部");
        a.setSourceIdentification("A自行生产数据");
        a.setSourceSubject("鼎和保险");
        a.setRegulated("国家金融监督管理总局");
        a.setRecognitionOpinion("权属清晰,予以确权");
        a.setSourceRef("CHAIN-0xabc123");
        a.setCurrentNode(ConfirmApply.NODE_DONE);
        a.setValidDate(LocalDateTime.now().plusYears(3));
        applyMapper.insert(a);
        return a;
    }

    private void card(String assetId, String rightType) {
        EquityCard c = new EquityCard();
        c.setCardNo("EQ-" + System.nanoTime());
        c.setAssetId(assetId);
        c.setAssetName("非车险理赔系统-理赔表");
        c.setRightType(rightType);
        c.setRightOwner("鼎和保险");
        c.setRightSource("自行生产");
        c.setScope("全字段");
        c.setCardStatus(EquityCard.STATUS_NORMAL);
        cardMapper.insert(c);
    }

    /** 已确权资产:产权信息含结论,权益条目齐全。 */
    @Test
    void confirmed_asset_returns_full_contracts() {
        String asset = "ASSET-DONE-" + System.nanoTime();
        apply(asset, ConfirmApply.STATUS_DONE, "初始确权");
        card(asset, "持有权");
        card(asset, "使用权");

        AssetPropertyVO p = service.property(asset);
        assertEquals(AssetCardIntegrationService.STATE_DONE, p.state(), "应为已确权");
        assertEquals("持有权", p.rightType());
        assertEquals("鼎和保险", p.rightHolder());
        assertEquals("A自行生产数据", p.sourceMethod());
        assertEquals("CHAIN-0xabc123", p.evidenceRef(), "应带确权凭证");
        assertNotNull(p.confirmTime(), "已确权应有确权时间");

        List<AssetEquityVO> eq = service.equity(asset);
        assertEquals(2, eq.size(), "应有两条权益条目");
        assertTrue(eq.stream().anyMatch(e -> "使用权".equals(e.rightType())));
    }

    /** 未确权资产:返回"待确权"占位,卡片不空白。 */
    @Test
    void unconfirmed_asset_returns_placeholder() {
        String asset = "ASSET-NONE-" + System.nanoTime();
        AssetPropertyVO p = service.property(asset);
        assertEquals(AssetCardIntegrationService.STATE_NONE, p.state());
        assertNull(p.rightType(), "占位无产权结论");
        assertNotNull(p.message(), "应有引导发起确权的提示");
        assertTrue(service.equity(asset).isEmpty(), "无权益条目");
    }

    /** 确权在途:返回"确权中"+节点提示。 */
    @Test
    void in_progress_asset_returns_progress_state() {
        String asset = "ASSET-PROG-" + System.nanoTime();
        apply(asset, ConfirmApply.STATUS_COMPLIANCE, "初始确权");
        AssetPropertyVO p = service.property(asset);
        assertEquals(AssetCardIntegrationService.STATE_PROGRESS, p.state());
        assertNotNull(p.message());
    }

    /** 已撤回:不得误判为"确权中(节点 null)",应回落为"待确权"并提示可重新发起。 */
    @Test
    void withdrawn_asset_is_not_in_progress() {
        String asset = "ASSET-WD-" + System.nanoTime();
        apply(asset, ConfirmApply.STATUS_WITHDRAWN, "初始确权");
        AssetPropertyVO p = service.property(asset);
        assertEquals(AssetCardIntegrationService.STATE_NONE, p.state(), "已撤回应回落待确权,非确权中/已驳回");
        assertTrue(p.message() != null && p.message().contains("撤回"), "应提示已撤回可重新发起:" + p.message());
    }

    /** 确权变更:已确权且提示为变更。 */
    @Test
    void change_registration_reflected() {
        String asset = "ASSET-CHG-" + System.nanoTime();
        apply(asset, ConfirmApply.STATUS_DONE, "确权变更");
        AssetPropertyVO p = service.property(asset);
        assertEquals(AssetCardIntegrationService.STATE_DONE, p.state());
        assertTrue(p.message() != null && p.message().contains("变更"), "应标注确权变更");
    }

    /** 适配层输出平台产权元数据表 AU_TABLE_META_DATA 列结构(写回可直接 UPSERT)。 */
    @Test
    void adapter_outputs_au_metadata_columns() {
        String asset = "ASSET-ADP-" + System.nanoTime();
        apply(asset, ConfirmApply.STATUS_DONE, "初始确权");
        Map<String, Object> m = service.propertyForPlatform(asset);
        assertEquals(asset, m.get("CARD_ID"), "关联卡片ID");
        assertEquals("A自行生产数据", m.get("SOURCE_JUDGE"), "来源判定");
        assertEquals("鼎和保险", m.get("SOURCE_MAIN_NAME"), "来源主体");
        assertEquals(1, m.get("IS_CHECK"), "含行政监管→IS_CHECK=1(NUMBER(1,0))");
        assertEquals("国家金融监督管理总局", m.get("CHECK_DESC"));
        assertEquals(0, m.get("IS_PRIVACY"), "无H标记→0");
        assertNotNull(m.get("AUTH_TIME"), "已确权应有确权时间 AUTH_TIME");
    }

    /** G–J 四维由 relationIdentification 派生:全标记 + 涉第三方 → 四个 IS_* 均为 1。 */
    @Test
    void adapter_derives_ghij_dimensions() {
        String asset = "ASSET-GHIJ-" + System.nanoTime();
        ConfirmApply a = apply(asset, ConfirmApply.STATUS_DONE, "初始确权");
        a.setRelationIdentification("G,H,I,J");
        a.setInvolvesThirdParty(true);
        a.setThirdPartyInfo("涉及供应商采购信息");
        a.setPrivacyInfo("涉及用户个人信息");
        a.setRelationSubject("某供应商");
        applyMapper.updateById(a);

        Map<String, Object> m = service.propertyForPlatform(asset);
        assertEquals(1, m.get("IS_CHECK"));
        assertEquals(1, m.get("IS_PRIVACY"));
        assertEquals("涉及用户个人信息", m.get("PRIVACY_DESC"));
        assertEquals(1, m.get("IS_BUS_SECRET"));
        assertEquals("涉及供应商采购信息", m.get("BUS_SECRET_DESC"));
        assertEquals(1, m.get("IS_EQUITY"));
        assertEquals("某供应商", m.get("EQUITY_DESC"));
    }
}
