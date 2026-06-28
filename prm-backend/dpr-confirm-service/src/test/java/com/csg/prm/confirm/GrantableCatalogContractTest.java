package com.csg.prm.confirm;

import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.integration.DataCatalogService;
import com.csg.prm.confirm.integration.dto.CatalogTreeNode;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 可授资源池「确权目录」契约锁(防回归)。
 *
 * 批量授权 picker(GrantableCatalogTree)以「确权目录全树」(DataCatalogService.fullTree)为层级源,
 * 按 (assetId=SYS:系统名, tableCode) 与库表级生效权益卡片配对,展示"可授数据表"。
 *
 * 历史回归:权益卡片下沉库表级后 assetId 变为「SYS:系统名」,而旧 picker 以台账树「库表真实 assetId」配对,
 * 两套 assetId 约定不一致 → 求交永空 → picker「暂无数据」。
 *
 * 本测试锁定跨模块键约定一致:遵循真实种子约定(assetId=SYS:系统, tableCode=库表代码)的库表级生效卡片,
 * 必须能在确权目录全树中按 (sysName, tableCode) 命中一个「已确权」叶子;而错误的库表代码必须命中失败。
 * 任一侧键约定漂移(叶名字段、sysName 形态、tableCode 前缀、CONFIRMED 集合)都会让本测试变红,
 * 而非线上静默空目录。
 *
 * 测试 profile 仅建表(schema.sql)、不灌种子(data.sql),故卡片在测试内自行插入,确保确定性、不依赖灌库。
 */
@SpringBootTest
@ActiveProfiles("test")
class GrantableCatalogContractTest {

    private static final String SYS_PREFIX = "SYS:";
    // 真实种子锚点:营销管理系统 / 用户用电信息表(已确权 + 对外开放),seed CARD-002 即此库表的使用权卡。
    private static final String ANCHOR_SYS = "营销管理系统";
    private static final String ANCHOR_TABLE = "MKT_BILL_CONS";

    @Autowired
    private EquityCardMapper cardMapper;

    private final DataCatalogService catalog = new DataCatalogService();

    /** 全树叶键(sysName|tableCode)→ 是否已确权。 */
    private Map<String, Boolean> leafConfirmed() {
        Map<String, Boolean> leaves = new HashMap<>();
        collect(catalog.fullTree(), leaves);
        return leaves;
    }

    private void collect(List<CatalogTreeNode> nodes, Map<String, Boolean> out) {
        for (CatalogTreeNode n : nodes) {
            if ("card".equals(n.type()) && n.tableCode() != null) {
                out.put(n.sysName() + "|" + n.tableCode(), n.confirmed());
            } else {
                collect(n.children(), out);
            }
        }
    }

    /** picker 配对键复刻:卡片 assetId(SYS:系统)→ sysName,与叶键 (sysName|tableCode) 求交。 */
    private boolean pickerJoins(EquityCard card, Map<String, Boolean> leaves) {
        if (card.getAssetId() == null || !card.getAssetId().startsWith(SYS_PREFIX) || card.getTableCode() == null) {
            return false;
        }
        String sysName = card.getAssetId().substring(SYS_PREFIX.length());
        return leaves.containsKey(sysName + "|" + card.getTableCode());
    }

    private EquityCard liveTableCard(String id, String sysName, String tableCode, String rightType) {
        EquityCard c = new EquityCard();
        c.setCardId(id);
        c.setCardNo(id);
        c.setAssetId(SYS_PREFIX + sysName);   // 库表级卡片 assetId = SYS:系统名(与确权事实/开放目录同键)
        c.setAssetName("用户用电信息表");
        c.setTableCode(tableCode);
        c.setSchemaName("BILLING");
        c.setRightType(rightType);
        c.setCardStatus("正常");
        return c;
    }

    @Test
    @DisplayName("遵循种子约定的库表级生效卡片,在确权目录全树命中『已确权』叶子(键=SYS:系统,tableCode)")
    void cardFollowingSeedConventionJoinsConfirmedLeaf() {
        Map<String, Boolean> leaves = leafConfirmed();
        // 前置:确权目录确实把锚点库表暴露为已确权叶子(否则授权无从谈起)
        assertTrue(Boolean.TRUE.equals(leaves.get(ANCHOR_SYS + "|" + ANCHOR_TABLE)),
                "确权目录全树应含已确权叶子 " + ANCHOR_SYS + "/" + ANCHOR_TABLE + "(先确后授锚点)");

        EquityCard card = liveTableCard("CTEST-USE-1", ANCHOR_SYS, ANCHOR_TABLE, "数据加工使用权");
        cardMapper.insert(card);

        assertTrue(pickerJoins(card, leaves),
                "遵循 SYS:系统 + tableCode 约定的生效卡片必须能与确权目录叶子配对(否则即『暂无数据』回归)");
        assertTrue(Boolean.TRUE.equals(leaves.get(ANCHOR_SYS + "|" + card.getTableCode())),
                "配对到的叶子必须是已确权(先确后授)");
    }

    @Test
    @DisplayName("库表代码不在确权目录中的卡片,picker 配对失败(不会误授未确权库表)")
    void cardWithUnknownTableDoesNotJoin() {
        Map<String, Boolean> leaves = leafConfirmed();
        EquityCard bogus = liveTableCard("CTEST-BOGUS-1", ANCHOR_SYS, "NOT_A_REAL_TABLE", "数据加工使用权");
        cardMapper.insert(bogus);
        assertFalse(pickerJoins(bogus, leaves),
                "库表代码不在确权目录的卡片不应被 picker 命中(防止误授未确权库表)");
    }
}
