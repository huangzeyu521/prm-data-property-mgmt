package com.csg.prm.confirm.aitool.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitCompare;
import com.csg.prm.confirm.aitool.entity.AitDocSegment;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseResult;

import java.util.List;

/**
 * 智能确权辅助工具-材料智能解析服务(M1 / SW-001~004)。
 */
public interface AitMaterialService {

    /** 术语库匹配建议项;sourceLocation=该值在材料正文中的定位(1.3#1 来源字段:标注所在位置) */
    record TermSuggestion(String field, String value, String standardTerm, boolean standard, String sourceLocation) {
    }

    /** #4 材料归集分组:同一数据表标识下的一组材料(按类别关联) */
    record MaterialGroup(String dataTableRef, List<AitMaterial> materials) {
    }

    /** 1.4#2 批量解析进度明细项 */
    record BatchItem(String materialId, String fileName, String parseStatus, int progress, String failReason) {
    }

    /** 1.4#2 批量解析聚合进度 */
    record BatchProgress(int total, int done, int failed, int running, int pending, List<BatchItem> items) {
    }

    /** 上传材料(文档哈希去重 + 批次),返回材料ID */
    String upload(AitMaterial material);

    /**
     * 真实文件上传(#1):校验格式/大小 -> 存储二进制 -> 抽取正文 -> 建材料,返回材料ID。
     * 格式仅限 PDF/Word/JPG/PNG,单文件 100KB–500MB。
     */
    String uploadBinary(String fileName, byte[] data, String applyId, String assetId, String batchNo);

    /** 读取已上传文件原件字节 */
    byte[] loadFile(String materialId);

    /** 触发解析(同步):OCR/NLP 抽取要素 -> 印章识别 -> 与确权申请表单自动比对 */
    void parse(String materialId);

    /** 异步提交解析:分阶段更新进度(10/55/100),供前端实时进度条轮询(#2) */
    void submitParse(String materialId);

    /** 取材料(含解析状态与进度),供进度轮询 */
    AitMaterial getMaterial(String materialId);

    /** 导出解析结果(要素+印章+表单比对差异)为 Excel(#8) */
    byte[] exportParseExcel(String materialId);

    AitParseResult getParse(String materialId);

    /** 术语库匹配:对解析要素做标准术语校验与建议(多要素) */
    List<TermSuggestion> termCheck(String materialId);

    /** 人工确认修改:把某要素采用为标准术语,写回解析结果 */
    void confirmTerm(String materialId, String field, String standardTerm);

    /** 解析结果与确权申请表单的比对差异 */
    List<AitCompare> compares(String materialId);

    PageResult<AitMaterial> page(PageQuery query, String batchNo, String parseStatus, String applyId);

    /** #5 取材料的多粒度解析片段(granularity 可空=全部:PAGE/PARAGRAPH/CELL/TABLE/TITLE) */
    List<AitDocSegment> segments(String materialId, String granularity);

    /** #4 按数据表标识归集材料(applyId 或 dataTableRef 任一过滤,均空=全部) */
    List<MaterialGroup> aggregate(String applyId, String dataTableRef);

    /** 1.4#2 批量解析:按批次号排队解析其下全部未成功材料,返回派发数。 */
    int batchParse(String batchNo);

    /** 1.4#2 批量解析聚合进度。 */
    BatchProgress batchProgress(String batchNo);
}
