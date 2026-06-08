package com.csg.prm.ledger.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.ledger.aggregate.AuthQueryGateway;
import com.csg.prm.ledger.aggregate.ConfirmQueryGateway;
import com.csg.prm.ledger.aggregate.DomainRecord;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.mapper.PropertyArchiveMapper;
import com.csg.prm.ledger.monitor.dto.ComplianceReportVO;
import com.csg.prm.ledger.monitor.dto.ComplianceResultQuery;
import com.csg.prm.ledger.monitor.entity.AlertRecord;
import com.csg.prm.ledger.monitor.entity.ComplianceResult;
import com.csg.prm.ledger.monitor.mapper.ComplianceResultMapper;
import com.csg.prm.ledger.monitor.service.AlertRecordService;
import com.csg.prm.ledger.monitor.service.ComplianceCheckService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ComplianceCheckServiceImpl implements ComplianceCheckService {

    private static final String STATUS_CONFIRMED = "已确权";
    private static final String STATUS_PENDING_CONFIRM = "待确权";
    private static final String STATUS_AUTHORIZED = "已授权";
    private static final String DIM_EXPIRY = "有效期";
    private static final String DIM_SCOPE = "权限范围";
    private static final String DIM_MATERIAL = "申请材料";
    private static final String DIM_AGREEMENT = "协议内容";

    private final PropertyArchiveMapper archiveMapper;
    private final ComplianceResultMapper complianceMapper;
    private final AlertRecordService alertRecordService;
    private final ConfirmQueryGateway confirmGateway;
    private final AuthQueryGateway authGateway;

    public ComplianceCheckServiceImpl(PropertyArchiveMapper archiveMapper,
                                      ComplianceResultMapper complianceMapper,
                                      AlertRecordService alertRecordService,
                                      ConfirmQueryGateway confirmGateway,
                                      AuthQueryGateway authGateway) {
        this.archiveMapper = archiveMapper;
        this.complianceMapper = complianceMapper;
        this.alertRecordService = alertRecordService;
        this.confirmGateway = confirmGateway;
        this.authGateway = authGateway;
    }

    @Override
    @Transactional
    public int runExpiryCheck(int days) {
        int threshold = days <= 0 ? 30 : days;
        LocalDateTime deadline = LocalDateTime.now().plusDays(threshold);

        LambdaQueryWrapper<PropertyArchive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PropertyArchive::getConfirmStatus, STATUS_CONFIRMED)
                .isNotNull(PropertyArchive::getValidDate)
                .le(PropertyArchive::getValidDate, deadline);
        List<PropertyArchive> expiring = archiveMapper.selectList(wrapper);

        int issues = 0;
        for (PropertyArchive a : expiring) {
            // 定时巡检去重:同资产已有未关闭的"有效期巡检"预警则跳过,避免重复刷
            if (alertRecordService.existsOpen(a.getAssetId(), "有效期巡检")) {
                continue;
            }
            boolean expired = a.getValidDate().isBefore(LocalDateTime.now());
            String result = expired ? ComplianceResult.RESULT_FAIL : ComplianceResult.RESULT_WARN;
            String desc = expired ? "权益已过期仍处于已确权状态" : "权益有效期临近(" + threshold + "天内)";

            ComplianceResult cr = new ComplianceResult();
            cr.setAssetId(a.getAssetId());
            cr.setCheckResult(result);
            cr.setProblemDesc(desc);
            cr.setSuggestion("请及时发起确权变更(续期)或办理权益注销");
            cr.setCheckTime(LocalDateTime.now());
            cr.setDisposeStatus("待处置");
            complianceMapper.insert(cr);

            String level = expired ? AlertRecord.LEVEL_URGENT : AlertRecord.LEVEL_IMPORTANT;
            alertRecordService.raise(null, "合规检查", a.getAssetId(), level, "有效期巡检", desc);
            issues++;
        }
        return issues;
    }

    @Override
    @Transactional
    public ComplianceReportVO runComplianceCheck() {
        String reportId = "RPT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        List<PropertyArchive> archives = archiveMapper.selectList(null);

        Map<String, Integer> byDim = new LinkedHashMap<>();
        byDim.put(DIM_EXPIRY, 0);
        byDim.put(DIM_SCOPE, 0);
        byDim.put(DIM_MATERIAL, 0);
        byDim.put(DIM_AGREEMENT, 0);
        int[] wf = new int[2]; // [0]=警告 [1]=不合规

        for (PropertyArchive a : archives) {
            // ① 有效期:已确权 + 有效期在 30 天内(含已过期)
            if (STATUS_CONFIRMED.equals(a.getConfirmStatus()) && a.getValidDate() != null
                    && a.getValidDate().isBefore(now.plusDays(30))) {
                boolean expired = a.getValidDate().isBefore(now);
                record(reportId, DIM_EXPIRY, a.getAssetId(),
                        expired ? ComplianceResult.RESULT_FAIL : ComplianceResult.RESULT_WARN,
                        expired ? "确权权益已过期仍处于已确权状态" : "确权权益有效期临近(30天内)",
                        "请发起确权变更(续期)或办理权益注销",
                        expired ? AlertRecord.LEVEL_URGENT : AlertRecord.LEVEL_IMPORTANT, byDim, wf);
            }
            // ② 权限范围:对外开放/经营权 但未取得对外授权 -> 越权
            boolean openScope = "对外开放".equals(a.getUseScope()) || "数据产品经营权".equals(a.getRightType());
            if (openScope && !STATUS_AUTHORIZED.equals(a.getAuthStatus())) {
                record(reportId, DIM_SCOPE, a.getAssetId(), ComplianceResult.RESULT_FAIL,
                        "对外经营/开放使用但未取得对外授权,超出权限范围(越权风险)",
                        "核查对外开放目录并补办授权,或收紧使用范围",
                        AlertRecord.LEVEL_URGENT, byDim, wf);
            }
            // ③ 申请材料:待确权 -> 申请材料/确权流程未完成
            if (STATUS_PENDING_CONFIRM.equals(a.getConfirmStatus())) {
                record(reportId, DIM_MATERIAL, a.getAssetId(), ComplianceResult.RESULT_WARN,
                        "资产确权未完成(待确权),申请材料/确权流程待补齐",
                        "补齐确权申请材料并推进确权流程",
                        AlertRecord.LEVEL_IMPORTANT, byDim, wf);
            }
            // ④ 协议内容:已授权 但有效期已过 -> 授权协议到期未续
            if (STATUS_AUTHORIZED.equals(a.getAuthStatus()) && a.getValidDate() != null
                    && a.getValidDate().isBefore(now)) {
                record(reportId, DIM_AGREEMENT, a.getAssetId(), ComplianceResult.RESULT_FAIL,
                        "授权协议有效期已过仍处于已授权状态,协议到期未续签",
                        "启动授权续签或终止授权并解除协议",
                        AlertRecord.LEVEL_URGENT, byDim, wf);
            }
        }

        int inflightConfirm = safeSize(confirmGateway.pending());
        int inflightAuth = safeSize(authGateway.pending());
        int hit = wf[0] + wf[1];

        ComplianceReportVO vo = new ComplianceReportVO();
        vo.setReportId(reportId);
        vo.setReportTime(now);
        vo.setTotalArchives(archives.size());
        vo.setHitCount(hit);
        vo.setWarnCount(wf[0]);
        vo.setFailCount(wf[1]);
        vo.setByDimension(byDim);
        vo.setInflightConfirm(inflightConfirm);
        vo.setInflightAuth(inflightAuth);
        vo.setSummary("本次合规检查覆盖 " + archives.size() + " 个产权档案,新发现合规问题 " + hit
                + " 项(警告 " + wf[0] + "/不合规 " + wf[1] + "):有效期 " + byDim.get(DIM_EXPIRY)
                + "、权限范围 " + byDim.get(DIM_SCOPE) + "、申请材料 " + byDim.get(DIM_MATERIAL)
                + "、协议内容 " + byDim.get(DIM_AGREEMENT) + ";在途确权申请 " + inflightConfirm
                + "、授权申请 " + inflightAuth + "。命中项已生成检查结果与告警,详见整改建议。");
        return vo;
    }

    /** 记录一条命中:去重(同资产+同维度未关闭预警则跳过)后写检查结果并联动预警,并累计维度/结果计数。 */
    private void record(String reportId, String dim, String assetId, String result, String desc,
                        String suggestion, String level, Map<String, Integer> byDim, int[] wf) {
        if (!StringUtils.hasText(assetId) || alertRecordService.existsOpen(assetId, dim)) {
            return;
        }
        String full = "【" + dim + "】" + desc;
        ComplianceResult cr = new ComplianceResult();
        cr.setAssetId(assetId);
        cr.setCheckDim(dim);
        cr.setCheckResult(result);
        cr.setProblemDesc(full);
        cr.setSuggestion(suggestion);
        cr.setReportUrl(reportId);
        cr.setCheckTime(LocalDateTime.now());
        cr.setDisposeStatus("待处置");
        complianceMapper.insert(cr);
        alertRecordService.raise(null, "合规检查", assetId, level, dim, full);
        byDim.merge(dim, 1, Integer::sum);
        if (ComplianceResult.RESULT_FAIL.equals(result)) {
            wf[1]++;
        } else {
            wf[0]++;
        }
    }

    private int safeSize(List<DomainRecord> list) {
        return list == null ? 0 : list.size();
    }

    @Override
    public ComplianceReportVO report(String reportId) {
        LambdaQueryWrapper<ComplianceResult> w = new LambdaQueryWrapper<>();
        w.eq(ComplianceResult::getReportUrl, reportId).orderByDesc(ComplianceResult::getCheckTime);
        List<ComplianceResult> rows = complianceMapper.selectList(w);

        Map<String, Integer> byDim = new LinkedHashMap<>();
        byDim.put(DIM_EXPIRY, 0);
        byDim.put(DIM_SCOPE, 0);
        byDim.put(DIM_MATERIAL, 0);
        byDim.put(DIM_AGREEMENT, 0);
        int warn = 0;
        int fail = 0;
        for (ComplianceResult r : rows) {
            if (r.getCheckDim() != null) {
                byDim.merge(r.getCheckDim(), 1, Integer::sum);
            }
            if (ComplianceResult.RESULT_FAIL.equals(r.getCheckResult())) {
                fail++;
            } else {
                warn++;
            }
        }
        ComplianceReportVO vo = new ComplianceReportVO();
        vo.setReportId(reportId);
        vo.setReportTime(rows.isEmpty() ? LocalDateTime.now() : rows.get(0).getCheckTime());
        vo.setHitCount(rows.size());
        vo.setWarnCount(warn);
        vo.setFailCount(fail);
        vo.setByDimension(byDim);
        vo.setSummary("报告 " + reportId + ":合规问题 " + rows.size() + " 项(警告 " + warn + "/不合规 " + fail
                + "):有效期 " + byDim.get(DIM_EXPIRY) + "、权限范围 " + byDim.get(DIM_SCOPE)
                + "、申请材料 " + byDim.get(DIM_MATERIAL) + "、协议内容 " + byDim.get(DIM_AGREEMENT) + "。");
        return vo;
    }

    @Override
    public PageResult<ComplianceResult> page(ComplianceResultQuery query) {
        LambdaQueryWrapper<ComplianceResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getCheckResult()), ComplianceResult::getCheckResult, query.getCheckResult())
                .eq(StringUtils.hasText(query.getAssetId()), ComplianceResult::getAssetId, query.getAssetId())
                .orderByDesc(ComplianceResult::getCheckTime);
        IPage<ComplianceResult> page = complianceMapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }
}
