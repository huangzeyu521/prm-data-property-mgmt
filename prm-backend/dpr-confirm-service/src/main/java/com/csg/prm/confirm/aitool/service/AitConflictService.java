package com.csg.prm.confirm.aitool.service;

import com.csg.prm.confirm.aitool.entity.AitConflict;
import com.csg.prm.confirm.aitool.entity.AitKgClaim;

import java.util.List;
import java.util.Map;

/**
 * 智能确权辅助工具-权属冲突识别服务(M2 / SW-005~006)。
 * 知识图谱主张登记 + 主体/范围/时效/历史冲突检测 + 冲突分析报告。
 */
public interface AitConflictService {

    /** 登记一条权属主张(构建/更新知识图谱) */
    String addClaim(AitKgClaim claim);

    /** 对当前申请主张做四类冲突检测,落库并返回检出的冲突 */
    List<AitConflict> detect(AitKgClaim current);

    List<AitKgClaim> claims(String assetId);

    List<AitConflict> conflicts(String assetId);

    /** 多维筛选冲突(按 资产/冲突类型/风险等级/起止时间)(#17) */
    List<AitConflict> conflicts(String assetId, String conflictType, String riskLevel,
                                String startTime, String endTime);

    void resolve(String conflictId, String feedback);

    /** 冲突分析报告(按类型聚合 + 明细 + 风险分布) */
    Map<String, Object> report(String assetId);

    /** 多维筛选冲突分析报告(#17) */
    Map<String, Object> report(String assetId, String conflictType, String riskLevel,
                               String startTime, String endTime);

    /** 导出冲突分析报告为 Word(.docx)(#17) */
    byte[] exportReportWord(String assetId, String conflictType, String riskLevel,
                            String startTime, String endTime);
}
