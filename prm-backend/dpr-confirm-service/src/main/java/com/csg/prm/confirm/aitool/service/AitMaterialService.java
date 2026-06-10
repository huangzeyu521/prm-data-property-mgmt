package com.csg.prm.confirm.aitool.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitCompare;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseResult;

import java.util.List;

/**
 * 智能确权辅助工具-材料智能解析服务(M1 / SW-001~004)。
 */
public interface AitMaterialService {

    /** 术语库匹配建议项 */
    record TermSuggestion(String field, String value, String standardTerm, boolean standard) {
    }

    /** 上传材料(文档哈希去重 + 批次),返回材料ID */
    String upload(AitMaterial material);

    /**
     * 真实文件上传(#1):校验格式/大小 -> 存储二进制 -> 抽取正文 -> 建材料,返回材料ID。
     * 格式仅限 PDF/Word/JPG/PNG,单文件 100KB–500MB。
     */
    String uploadBinary(String fileName, byte[] data, String applyId, String batchNo);

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
}
