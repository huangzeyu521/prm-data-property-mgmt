package com.csg.prm.authorize.dto;

import java.util.List;

/**
 * 批量授权清单合规校验(只读试跑)结果:整单是否可提交 + 逐项被拦原因 + 逐项逐维度明细。
 * 与 submit 门禁同源(applyService.submitBlockReason + 第三方凭证红线),供前端"校验通过才放行提交"闭环。
 * items:逐项逐维度结果(通过项也返回),供前端展示完整校验明细(可核对·可定位),不再是只回被拦项的黑盒。
 */
public record BatchComplianceResult(boolean allPass, int total, int blockedCount,
                                    List<BlockedItem> blocked, List<ItemCheck> items) {

    /** 被拦明细 */
    public record BlockedItem(String applyId, String assetName, String reason) {
    }

    /** 逐项校验结果:数据资产 + 各维度结果 + 结论(通过/不通过)。 */
    public record ItemCheck(String applyId, String assetName, boolean pass, List<Dim> dims) {
    }

    /** 单维度结果:维度名 + 是否通过 + 说明(— 表示不涉及)。 */
    public record Dim(String name, boolean ok, String note) {
    }
}
