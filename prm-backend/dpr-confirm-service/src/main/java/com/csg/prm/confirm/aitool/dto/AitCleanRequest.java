package com.csg.prm.confirm.aitool.dto;

import java.util.List;
import java.util.Map;

/**
 * 数据清洗请求:rows 为待清洗记录(每行 原始字段名→原始值);useModel 控制是否启用模型语义归一(#3 混合清洗)。
 * rows 为空时由服务端从材料解析结果自动派生一行。
 */
public class AitCleanRequest {

    private List<Map<String, String>> rows;
    private boolean useModel = true;

    public List<Map<String, String>> getRows() { return rows; }
    public void setRows(List<Map<String, String>> rows) { this.rows = rows; }
    public boolean isUseModel() { return useModel; }
    public void setUseModel(boolean useModel) { this.useModel = useModel; }
}
