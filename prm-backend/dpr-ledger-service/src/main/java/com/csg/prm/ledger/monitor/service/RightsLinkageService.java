package com.csg.prm.ledger.monitor.service;

import com.csg.prm.ledger.monitor.dto.LinkageResult;

/**
 * 权益动态监测联动服务(附录F 3.4.5)。
 * 监测识别违规/越权 -> 生成预警 + (按规则)联动熔断暂停授权 + 追责。
 */
public interface RightsLinkageService {

    /**
     * 上报一次违规事件并执行联动处置。
     * @param assetId       涉事资产
     * @param ruleId        命中的监测规则ID(可空;为空时默认执行熔断)
     * @param violationType 违规类型(越权调用/违规使用/超范围/到期未续)
     * @param desc          异常描述
     */
    LinkageResult onViolation(String assetId, String ruleId, String violationType, String desc);

    /**
     * 监测识别数据新增/来源变更/到期 -> 生成预警 + 联动派生重确权工单(附录F 3.3.2 季度重确权)。
     * @param triggerType 触发类型(数据新增/来源变更/到期)
     * @return 派生的重确权工单ID(本地桩为 null)与预警ID
     */
    LinkageResult triggerReConfirm(String assetId, String assetName, String rightType,
                                   String triggerType, String desc);
}
