package com.csg.prm.ledger.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.ledger.dto.LedgerOverviewVO;
import com.csg.prm.ledger.dto.PropertyChangeRecordQuery;
import com.csg.prm.ledger.dto.PropertyTreeNode;
import com.csg.prm.ledger.entity.DataAssetInfo;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.entity.PropertyChangeRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 台账子项增量集成测试:数据资产同步、产权树、总体概览、变更记录自动留痕。
 * 使用唯一ID,避免与共享内存库中的其他用例数据相互干扰。
 */
@SpringBootTest
@ActiveProfiles("test")
class LedgerFeaturesTest {

    @Autowired
    private DataAssetInfoService assetService;
    @Autowired
    private PropertyTreeService treeService;
    @Autowired
    private LedgerOverviewService overviewService;
    @Autowired
    private PropertyArchiveService archiveService;
    @Autowired
    private PropertyChangeRecordService changeRecordService;

    private DataAssetInfo asset(String id, String name, String sub, String sys, String schema) {
        DataAssetInfo a = new DataAssetInfo();
        a.setAssetId(id);
        a.setAssetName(name);
        a.setSubsidiaryName(sub);
        a.setSystemName(sys);
        a.setSchemaName(schema);
        a.setAssetType("结构化表");
        return a;
    }

    @Test
    void sync_then_buildTree_should_form_4_level_hierarchy() {
        assetService.saveOrUpdateAsset(asset("T-001", "测试表A", "广东电网-TREE", "营销系统", "MKT"));
        assetService.saveOrUpdateAsset(asset("T-002", "测试表B", "广东电网-TREE", "营销系统", "MKT"));

        List<PropertyTreeNode> tree = treeService.buildTree();
        PropertyTreeNode sub = tree.stream()
                .filter(n -> "广东电网-TREE".equals(n.getLabel()))
                .findFirst().orElse(null);
        assertNotNull(sub, "应存在子公司节点");
        assertEquals(PropertyTreeNode.TYPE_SUBSIDIARY, sub.getType());

        PropertyTreeNode sys = sub.getChildren().get(0);
        assertEquals("营销系统", sys.getLabel());
        PropertyTreeNode schema = sys.getChildren().get(0);
        assertEquals("MKT", schema.getLabel());
        assertEquals(2, schema.getChildren().size(), "MKT 模式下应有2个数据集");
        assertEquals(PropertyTreeNode.TYPE_DATASET, schema.getChildren().get(0).getType());
        assertEquals("未确权", schema.getChildren().get(0).getConfirmStatus(), "无档案时默认未确权");
    }

    @Test
    void overview_should_compute_metrics() {
        assetService.saveOrUpdateAsset(asset("OV-001", "概览表", "概览子公司", "财务系统", "FIN"));
        PropertyArchive archive = new PropertyArchive();
        archive.setAssetId("OV-001");
        archive.setAssetName("概览表");
        archive.setRightType("持有权");
        archive.setConfirmStatus("已确权");
        archiveService.create(archive);

        LedgerOverviewVO vo = overviewService.overview();
        assertTrue(vo.getTotalAssets() >= 1);
        assertTrue(vo.getConfirmedAssets() >= 1, "应统计到已确权资产");
        assertTrue(vo.getConfirmRate() > 0 && vo.getConfirmRate() <= 100);
        assertTrue(vo.getRightTypeDistribution().containsKey("持有权"));
        assertTrue(vo.getSubsidiaryDistribution().containsKey("概览子公司"));
    }

    @Test
    void archive_update_should_auto_record_change() {
        PropertyArchive archive = new PropertyArchive();
        archive.setAssetId("CR-001");
        archive.setAssetName("变更留痕表");
        archive.setConfirmStatus("未确权");
        String id = archiveService.create(archive);

        PropertyArchive toUpdate = archiveService.getById(id);
        toUpdate.setConfirmStatus("已确权");
        archiveService.update(toUpdate);

        PropertyChangeRecordQuery q = new PropertyChangeRecordQuery();
        q.setAssetId("CR-001");
        PageResult<PropertyChangeRecord> page = changeRecordService.page(q);
        assertTrue(page.getTotal() >= 1, "档案确权状态变更应自动留痕");
        PropertyChangeRecord rec = page.getRecords().get(0);
        assertEquals("确权状态", rec.getFieldName());
        assertEquals("未确权", rec.getBeforeValue());
        assertEquals("已确权", rec.getAfterValue());
    }
}
