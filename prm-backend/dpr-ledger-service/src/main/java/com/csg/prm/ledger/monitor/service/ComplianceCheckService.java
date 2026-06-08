package com.csg.prm.ledger.monitor.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.ledger.monitor.dto.ComplianceReportVO;
import com.csg.prm.ledger.monitor.dto.ComplianceResultQuery;
import com.csg.prm.ledger.monitor.entity.ComplianceResult;

/**
 * 合规性检查服务:多维巡检数据资产,识别 有效期/权限范围/申请材料/协议内容 等合规问题,
 * 生成检查结果与检查报告,并联动预警。
 */
public interface ComplianceCheckService {

    /**
     * 执行到期合规巡检:对已确权且有效期在 days 天内(含已过期)的档案生成警告并触发预警。
     * @return 命中的问题数量
     */
    int runExpiryCheck(int days);

    /**
     * 执行多维合规检查(有效期/权限范围/申请材料/协议内容),生成检查结果+预警(去重),并产出检查报告。
     * @return 检查报告
     */
    ComplianceReportVO runComplianceCheck();

    /** 按报告ID重新聚合检查结果,返回检查报告(供"查看报告")。 */
    ComplianceReportVO report(String reportId);

    PageResult<ComplianceResult> page(ComplianceResultQuery query);
}
