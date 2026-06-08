package com.csg.prm.ledger.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.ledger.dto.PropertyArchiveQuery;
import com.csg.prm.ledger.entity.PropertyArchive;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 产权档案服务端到端集成测试(H2)。覆盖 创建→查询→分页→修改→删除→关联约束。
 */
@SpringBootTest
@ActiveProfiles("test")
class PropertyArchiveServiceTest {

    @Autowired
    private PropertyArchiveService service;

    private PropertyArchive newArchive(String assetName) {
        PropertyArchive a = new PropertyArchive();
        a.setAssetId("DA-IM-CARD-0000001");
        a.setAssetName(assetName);
        a.setRightType("数据资源持有权");
        a.setRightSubject("广东电网有限责任公司");
        a.setRespDept("数字化管理部门");
        return a;
    }

    @Test
    void create_then_get_should_persist_and_autofill() {
        String id = service.create(newArchive("客户用电信息表"));
        assertNotNull(id, "应返回生成的档案ID");

        PropertyArchive saved = service.getById(id);
        assertEquals("客户用电信息表", saved.getAssetName());
        assertEquals("未确权", saved.getConfirmStatus(), "未传确权状态时应默认未确权");
        assertNotNull(saved.getCreateTime(), "创建时间应被自动填充");
        assertEquals(0, saved.getDelFlag(), "逻辑删除位应自动置0");
    }

    @Test
    void create_should_reject_blank_required_fields() {
        PropertyArchive bad = new PropertyArchive();
        bad.setAssetName("缺少资产ID");
        assertThrows(BizException.class, () -> service.create(bad));
    }

    @Test
    void page_should_filter_by_condition() {
        service.create(newArchive("设备台账表"));
        service.create(newArchive("营销客户档案表"));

        PropertyArchiveQuery q = new PropertyArchiveQuery();
        q.setAssetName("设备");
        PageResult<PropertyArchive> page = service.page(q);
        assertTrue(page.getTotal() >= 1, "应能按资产名称模糊命中");
        assertTrue(page.getRecords().stream().allMatch(r -> r.getAssetName().contains("设备")));
    }

    @Test
    void update_should_change_fields() {
        String id = service.create(newArchive("待更新表"));
        PropertyArchive upd = service.getById(id);
        upd.setConfirmStatus("已确权");
        upd.setRightType("数据加工使用权");
        service.update(upd);

        PropertyArchive after = service.getById(id);
        assertEquals("已确权", after.getConfirmStatus());
        assertEquals("数据加工使用权", after.getRightType());
        assertNotNull(after.getUpdateTime());
    }

    @Test
    void delete_should_logically_remove() {
        String id = service.create(newArchive("待删除表"));
        service.delete(id);
        assertThrows(BizException.class, () -> service.getById(id), "逻辑删除后应查询不到");
    }

    @Test
    void delete_should_be_blocked_when_linked_to_equity_card() {
        PropertyArchive a = newArchive("已关联权益卡片表");
        a.setEquityCardId("EC-PRA-2026-00211");
        String id = service.create(a);
        BizException ex = assertThrows(BizException.class, () -> service.delete(id));
        assertTrue(ex.getMessage().contains("解除关联"), "应触发关联约束拦截");
    }
}
