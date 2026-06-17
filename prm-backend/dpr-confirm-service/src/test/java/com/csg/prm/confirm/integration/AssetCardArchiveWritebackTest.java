package com.csg.prm.confirm.integration;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.integration.dto.AssetArchiveRowVO;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 档案只读查询(PRM 兜底)+ 写回 stub 验证。三条钉死结论:只读不新增 / 平台清单优先 / 写回单向。
 */
@SpringBootTest
@ActiveProfiles("test")
class AssetCardArchiveWritebackTest {

    @Autowired private AssetCardArchiveService archive;
    @Autowired private AssetCardWritebackService writeback;
    @Autowired private ConfirmApplyMapper applyMapper;
    @Autowired private EquityCardMapper cardMapper;

    private String seedDone(String tag) {
        String asset = "ARCH-" + tag + "-" + System.nanoTime();
        ConfirmApply a = new ConfirmApply();
        a.setApplyNo("AP-" + System.nanoTime());
        a.setAssetId(asset);
        a.setAssetName("档案测试-" + tag);
        a.setStatus(ConfirmApply.STATUS_DONE);
        a.setRegisterType("初始确权");
        a.setRightType("持有权");
        a.setRightHolder("鼎和保险");
        a.setRespDept("数字化部");
        a.setSourceIdentification("A自行生产数据");
        a.setSourceRef("CHAIN-" + tag);
        a.setCurrentNode(ConfirmApply.NODE_DONE);
        applyMapper.insert(a);
        EquityCard c = new EquityCard();
        c.setCardNo("EQ-" + System.nanoTime());
        c.setAssetId(asset);
        c.setRightType("持有权");
        c.setCardStatus(EquityCard.STATUS_NORMAL);
        cardMapper.insert(c);
        return asset;
    }

    /** 档案查询:已确权资产出现在列表,行含状态/权益数;只读、无新增。 */
    @Test
    void archive_lists_confirmed_assets() {
        String asset = seedDone("LIST");
        PageResult<AssetArchiveRowVO> page = archive.page(new PageQuery(), null, null);
        assertTrue(page.getTotal() >= 1);
        AssetArchiveRowVO row = page.getRecords().stream()
                .filter(r -> asset.equals(r.assetId())).findFirst().orElseThrow();
        assertEquals(AssetCardIntegrationService.STATE_DONE, row.state());
        assertEquals("持有权", row.rightType());
        assertTrue(row.equityCount() >= 1, "应统计权益条目数");
    }

    /** 关键词 + 状态筛选生效。 */
    @Test
    void archive_filters_by_keyword_and_state() {
        String asset = seedDone("KW");
        PageQuery q = new PageQuery();
        q.setSize(100);
        // 关键词命中资产ID
        assertTrue(archive.page(q, asset, null).getRecords().stream().anyMatch(r -> asset.equals(r.assetId())));
        // 状态筛"已确权"应包含;筛"待确权"不应包含该已确权资产
        assertTrue(archive.page(q, asset, AssetCardIntegrationService.STATE_DONE).getRecords().stream()
                .anyMatch(r -> asset.equals(r.assetId())));
        assertFalse(archive.page(q, asset, AssetCardIntegrationService.STATE_NONE).getRecords().stream()
                .anyMatch(r -> asset.equals(r.assetId())));
    }

    /** 写回:已确权资产构造载荷(stub 未真正外发,accepted=false 但 note 说明已构造载荷)。 */
    @Test
    void writeback_done_asset_builds_payload() {
        String asset = seedDone("WB");
        AssetCardWritebackService.WritebackResult r = writeback.writeback(asset);
        assertFalse(r.accepted(), "stub 未真正写回");
        assertTrue(r.note().contains("载荷"), "应已构造写回载荷:" + r.note());
    }

    /** 写回:未确权资产不写回。 */
    @Test
    void writeback_unconfirmed_asset_skipped() {
        AssetCardWritebackService.WritebackResult r = writeback.writeback("ARCH-NONE-" + System.nanoTime());
        assertFalse(r.accepted());
        assertTrue(r.note().contains("尚未确权"), "应提示尚未确权:" + r.note());
    }
}
