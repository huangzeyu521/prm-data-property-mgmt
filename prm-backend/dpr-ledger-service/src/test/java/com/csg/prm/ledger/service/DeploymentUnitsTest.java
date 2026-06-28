package com.csg.prm.ledger.service;

import com.csg.prm.common.org.DeploymentUnits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 系统部署单位分类器(打√口径 10 桶)纯单测:验证"最具体优先"归类与边界。
 */
class DeploymentUnitsTest {

    @Test
    void classify_bureauCodeAndName_subProvincialCitiesFirst() {
        // 广州/深圳须单列,先于省网命中(否则被并入广东)
        assertEquals(DeploymentUnits.GUANGZHOU, DeploymentUnits.classify("GD", "4401", "广东电网"));
        assertEquals(DeploymentUnits.SHENZHEN, DeploymentUnits.classify(null, "4403", null));
        assertEquals(DeploymentUnits.GUANGZHOU, DeploymentUnits.classify(null, null, "广州供电局"));
        assertEquals(DeploymentUnits.SHENZHEN, DeploymentUnits.classify(null, null, "深圳供电局"));
    }

    @Test
    void classify_networkDirectUnits() {
        assertEquals(DeploymentUnits.EHV, DeploymentUnits.classify(null, null, "南方电网超高压输电公司"));
        assertEquals(DeploymentUnits.DISPATCH, DeploymentUnits.classify(null, null, "南方电网总调(双调中心)"));
        assertEquals(DeploymentUnits.DISPATCH, DeploymentUnits.classify(null, null, "调控中心"));
        assertEquals(DeploymentUnits.HQ, DeploymentUnits.classify("CSG", null, "中国南方电网有限责任公司"));
        assertEquals(DeploymentUnits.HQ, DeploymentUnits.classify(null, null, "南方电网总部"));
    }

    @Test
    void classify_fiveProvinces_byCodeOrName() {
        assertEquals(DeploymentUnits.GD, DeploymentUnits.classify("GD", null, "广东电网有限责任公司"));
        assertEquals(DeploymentUnits.GX, DeploymentUnits.classify(null, null, "广西电网"));
        assertEquals(DeploymentUnits.YN, DeploymentUnits.classify("YN", null, null));
        assertEquals(DeploymentUnits.GZ, DeploymentUnits.classify(null, null, "贵州电网"));
        assertEquals(DeploymentUnits.HI, DeploymentUnits.classify("HI", null, null));
    }

    @Test
    void classify_unresolvable_fallsToUnidentified() {
        assertEquals(DeploymentUnits.UNIDENTIFIED, DeploymentUnits.classify(null, null, ""));
        assertEquals(DeploymentUnits.UNIDENTIFIED, DeploymentUnits.classify(null, null, "某不识别单位"));
    }

    @Test
    void order_isFixedTenBuckets() {
        assertEquals(10, DeploymentUnits.ORDER.size());
        assertEquals(DeploymentUnits.HQ, DeploymentUnits.ORDER.get(0));
        assertEquals(DeploymentUnits.SHENZHEN, DeploymentUnits.ORDER.get(9));
    }
}
