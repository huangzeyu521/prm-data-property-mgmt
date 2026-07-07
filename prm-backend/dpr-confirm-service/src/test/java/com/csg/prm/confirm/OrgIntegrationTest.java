package com.csg.prm.confirm;

import com.csg.prm.common.evidence.ChainEvidence;
import com.csg.prm.common.evidence.ChainEvidenceService;
import com.csg.prm.common.org.Jurisdiction;
import com.csg.prm.common.org.OrgNode;
import com.csg.prm.common.org.OrgService;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.service.EquityCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组织主数据只读层 + 省地市码回填:
 * 1) OrgService 解析归口网级(地市→bureau+province;省级→province;网级→空);
 * 2) 组织树/分级清单可用;
 * 3) 确权制卡按申报组织自动回填卡片与存证的 province_code/bureau_code(补此前 null)。
 */
@SpringBootTest
@ActiveProfiles("test")
class OrgIntegrationTest {

    @Autowired private OrgService orgService;
    @Autowired private EquityCardService cardService;
    @Autowired private ChainEvidenceService chainEvidenceService;

    @Test
    void resolve_bureau_walksUp_to_province() {
        Jurisdiction j = orgService.resolve("广州供电局");
        assertEquals("GD", j.provinceCode(), "广州供电局应上溯到广东省网");
        assertEquals("4401", j.bureauCode(), "广州局 bureau=城市行政码");
        assertEquals("广东电网有限责任公司", j.provinceName());
    }

    @Test
    void resolve_province_hasNoBureau() {
        Jurisdiction j = orgService.resolve("广东电网有限责任公司");
        assertEquals("GD", j.provinceCode());
        assertNull(j.bureauCode(), "省级单位无地市归属");
    }

    @Test
    void resolve_topGrid_and_unknown_areEmpty() {
        assertTrue(orgService.resolve("中国南方电网有限责任公司").isEmpty(), "网级总部无省/地市归属");
        assertTrue(orgService.resolve("查无此组织XYZ").isEmpty());
        assertTrue(orgService.resolve(null).isEmpty());
    }

    @Test
    void tree_and_levelList_available() {
        List<OrgNode> tree = orgService.tree();
        assertFalse(tree.isEmpty(), "组织树非空");
        assertEquals("中国南方电网有限责任公司", tree.get(0).getName(), "根=网级总部");
        assertFalse(tree.get(0).getChildren().isEmpty(), "网级下挂省网");
        assertTrue(orgService.listByLevel("地市").size() >= 8, "应含多个地市供电局");
    }

    @Test
    void cardIssuance_backfills_province_bureau_on_card_and_evidence() {
        String assetId = "ORG-CFM-" + System.nanoTime();
        ConfirmApply a = new ConfirmApply();
        a.setAssetId(assetId);
        a.setAssetName("回填测试-" + assetId);
        a.setRightType("持有权");
        a.setRightHolder("广州供电局"); // 申报组织=地市局

        String cardId = cardService.generateFromApply(a);
        EquityCard card = cardService.getById(cardId);
        assertEquals("GD", card.getProvinceCode(), "卡片省码应回填");
        assertEquals("4401", card.getBureauCode(), "卡片地市码应回填");

        List<ChainEvidence> evs = chainEvidenceService.listByBiz(cardId);
        assertFalse(evs.isEmpty(), "制卡应有上链存证");
        ChainEvidence ev = evs.get(0);
        assertEquals("GD", ev.getProvinceCode(), "存证省码应回填(非 null)");
        assertEquals("4401", ev.getBureauCode(), "存证地市码应回填(非 null)");
    }
}
