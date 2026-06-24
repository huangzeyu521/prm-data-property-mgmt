package com.csg.prm.common.org;

/**
 * 归口网级:由组织上溯派生的省/地市归属,用于存证与卡片的 province_code/bureau_code 回填及 ABAC 隔离。
 * 全字段为空表示无法解析(组织缺失或为网级总部)。
 */
public record Jurisdiction(String provinceCode, String provinceName,
                           String bureauCode, String bureauName) {

    public static final Jurisdiction EMPTY = new Jurisdiction(null, null, null, null);

    public boolean isEmpty() {
        return provinceCode == null && bureauCode == null;
    }
}
