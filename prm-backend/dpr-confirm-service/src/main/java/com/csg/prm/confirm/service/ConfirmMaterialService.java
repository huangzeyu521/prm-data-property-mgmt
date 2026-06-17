package com.csg.prm.confirm.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.confirm.dto.MaterialCheckReport;
import com.csg.prm.confirm.entity.ConfirmMaterial;

import java.util.List;

/**
 * 确权申请材料服务(F-02 材料上传与补充 / 材料校验)。
 */
public interface ConfirmMaterialService {
    String upload(ConfirmMaterial m);

    /** 真实文件上传:格式验证(PDF/Word/JPG/PNG + 大小)后 Base64 入库,返回 materialId。 */
    String uploadFile(ConfirmMaterial meta, String fileName, byte[] data);

    /** 下载/预览原件二进制。 */
    byte[] download(String materialId);

    void delete(String materialId);
    List<ConfirmMaterial> listByApply(String applyId);
    /** 校验:通过/不通过 + 异常说明 */
    void check(String materialId, boolean pass, String abnormalDesc);

    /** 规则化材料校验:按应交清单(来源/关联标识)比对,自动识别缺失项+不合规项,逐材料写校验结果,返回报告。 */
    MaterialCheckReport runCheck(String applyId);

    /** 材料 AI 校验(qwen3-max 逐份校验完整性/合规性/一致性,stub 回退),返回严格 JSON 字符串 */
    String aiCheck(String applyId);

    /** 抽取某份材料正文(供确权内生 AI 能力复用);无法抽取返回空串 */
    String materialText(String materialId);

    /** 推送审核(后端门禁):材料校验全通过且无缺失才提交审核,否则拒并返回缺失/不合规清单。 */
    void pushReview(String applyId);

    /** 导出材料校验结果(CSV,Excel 可直接打开)。 */
    byte[] exportCheck(String applyId);

    PageResult<ConfirmMaterial> page(long current, long size, String applyId, String checkResult);
}
