package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.enums.MaterialDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 资料类型(CEC_DATA_TYPE / 联调清单 行25)规范取值:中文类别词 ↔ 编码 01–07 互转。
 */
class MaterialDataTypeTest {

    /** 中文标签收敛为规范编码;模型多余修饰仍按 contains 命中。 */
    @Test
    void code_of_chinese_label() {
        assertEquals("01", MaterialDataType.codeOf("元数据"));
        assertEquals("02", MaterialDataType.codeOf("制度附件"));
        assertEquals("03", MaterialDataType.codeOf("这份是授权材料"));
        assertEquals("04", MaterialDataType.codeOf("合同材料"));
        assertEquals("05", MaterialDataType.codeOf("来源说明"));
        assertEquals("06", MaterialDataType.codeOf("确权证明"));
        assertEquals("07", MaterialDataType.codeOf("其他"));
    }

    /** 已是编码 → 幂等;空/无法判定 → 07 其他。 */
    @Test
    void code_of_is_idempotent_and_defaults_to_other() {
        assertEquals("03", MaterialDataType.codeOf("03"), "编码入参幂等");
        assertEquals("07", MaterialDataType.codeOf(null), "空 → 07");
        assertEquals("07", MaterialDataType.codeOf("  "), "空白 → 07");
        assertEquals("07", MaterialDataType.codeOf("无法识别的怪值XYZ"), "未知 → 07");
    }

    /** 编码→中文展示;历史中文/非法值原样返回,保证前端不丢可读性。 */
    @Test
    void label_of_code_for_display() {
        assertEquals("元数据", MaterialDataType.labelOf("01"));
        assertEquals("确权证明", MaterialDataType.labelOf("06"));
        assertEquals("其他", MaterialDataType.labelOf("07"));
        assertEquals("授权材料", MaterialDataType.labelOf("授权材料"), "历史中文存量原样返回");
    }
}
