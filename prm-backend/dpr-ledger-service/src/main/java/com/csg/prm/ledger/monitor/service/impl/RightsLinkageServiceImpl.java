package com.csg.prm.ledger.monitor.service.impl;

import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.ledger.monitor.dto.LinkageResult;
import com.csg.prm.ledger.monitor.entity.AlertRecord;
import com.csg.prm.ledger.monitor.entity.MonitorRule;
import com.csg.prm.ledger.monitor.gateway.AuthSuspendGateway;
import com.csg.prm.ledger.monitor.gateway.ReConfirmGateway;
import com.csg.prm.ledger.monitor.mapper.MonitorRuleMapper;
import com.csg.prm.ledger.monitor.service.AlertRecordService;
import com.csg.prm.ledger.monitor.service.RightsLinkageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RightsLinkageServiceImpl implements RightsLinkageService {

    private static final String SOURCE = "授权履约监测";
    private static final String SOURCE_RECONFIRM = "权属变动监测";

    private final AlertRecordService alertService;
    private final MonitorRuleMapper ruleMapper;
    private final AuthSuspendGateway authSuspendGateway;
    private final ReConfirmGateway reConfirmGateway;

    public RightsLinkageServiceImpl(AlertRecordService alertService, MonitorRuleMapper ruleMapper,
                                    AuthSuspendGateway authSuspendGateway, ReConfirmGateway reConfirmGateway) {
        this.alertService = alertService;
        this.ruleMapper = ruleMapper;
        this.authSuspendGateway = authSuspendGateway;
        this.reConfirmGateway = reConfirmGateway;
    }

    @Override
    @Transactional
    public LinkageResult onViolation(String assetId, String ruleId, String violationType, String desc) {
        if (!StringUtils.hasText(assetId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "资产ID不能为空");
        }
        String vt = StringUtils.hasText(violationType) ? violationType : "违规使用";
        // 1) 违规即生成紧急预警(留痕)
        String alertId = alertService.raise(ruleId, SOURCE, assetId, AlertRecord.LEVEL_URGENT, vt, desc);

        // 2) 是否联动熔断:命中规则按规则配置;无规则时默认执行(显式上报即视为需处置)
        boolean circuitBreak = true;
        if (StringUtils.hasText(ruleId)) {
            MonitorRule rule = ruleMapper.selectById(ruleId);
            if (rule != null) {
                circuitBreak = Boolean.TRUE.equals(rule.getCircuitBreak());
            }
        }

        int suspended = 0;
        if (circuitBreak) {
            suspended = authSuspendGateway.suspendByAsset(assetId, "监测联动熔断:" + vt, alertId, vt);
        }
        return new LinkageResult(alertId, circuitBreak, suspended);
    }

    @Override
    @Transactional
    public LinkageResult triggerReConfirm(String assetId, String assetName, String rightType,
                                          String triggerType, String desc) {
        if (!StringUtils.hasText(assetId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "资产ID不能为空");
        }
        String tt = StringUtils.hasText(triggerType) ? triggerType : "来源变更";
        // 1) 权属变动生成重要级预警(留痕)
        String alertId = alertService.raise(null, SOURCE_RECONFIRM, assetId, AlertRecord.LEVEL_IMPORTANT, tt, desc);
        // 2) 联动派生重确权工单
        String reason = "监测识别" + tt + (StringUtils.hasText(desc) ? ":" + desc : "") + ",派生重确权";
        String reConfirmId = reConfirmGateway.trigger(assetId, assetName, rightType, reason, alertId);

        LinkageResult result = new LinkageResult(alertId, false, 0);
        result.setReConfirmId(reConfirmId);
        return result;
    }
}
