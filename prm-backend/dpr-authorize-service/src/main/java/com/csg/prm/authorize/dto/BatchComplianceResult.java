package com.csg.prm.authorize.dto;

import java.util.List;

/**
 * 批量授权清单合规校验(只读试跑)结果:整单是否可提交 + 逐项被拦原因。
 * 与 submit 门禁同源(applyService.submitBlockReason + 第三方凭证红线),供前端"校验通过才放行提交"闭环。
 */
public record BatchComplianceResult(boolean allPass, int total, int blockedCount, List<BlockedItem> blocked) {

    /** 被拦明细 */
    public record BlockedItem(String applyId, String assetName, String reason) {
    }
}
