package com.csg.prm.confirm.integration.dto;

import com.csg.prm.confirm.entity.ConfirmTableItem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 确权变更·完整基线:在身份级 {@link ChangeBaseline} 之上,带出上一版"真实确权结论"作变更底版。
 * <p>反查该资产/系统最近一笔「已完成(制卡)」确权,载入:
 * <ul>
 *   <li>{@link #tableItems} —— 上一版表3逐表行(IM_CONFIRM_TABLE_ITEM),作可编辑底版,在其上做差异编辑;</li>
 *   <li>{@link #cards} —— 上一版权益卡片(IM_EQUITY_CARD_INFO 正常态),作表4/确权边界(scope/有效期)底版;</li>
 *   <li>{@link #summaries} —— 上一版表3/表4认定意见文本(IM_CONFIRM_SUMMARY),作认定来源留痕。</li>
 * </ul>
 * 当无上一版确权(首次/桩)时 {@link #fromRealConfirm}=false,{@link #base} 退回目录合成桩、明细为空。
 */
public record ChangeBaselineFull(
        ChangeBaseline base,                 // 身份级基线(系统/权属主体/层级/部门/三权/管制/来源/关联/确权时间/版本)
        boolean fromRealConfirm,             // true=反查到真实上一版确权;false=无上一版,base 为合成桩
        String priorApplyId,                 // 被取代的上一版申请ID(供写入 baselineRef、版本链留痕)
        List<ConfirmTableItem> tableItems,   // 上一版表3逐表行(真实底版)
        List<CardView> cards,                // 上一版权益卡片(表4/边界底版)
        List<SummaryView> summaries          // 上一版表3/表4认定意见(来源留痕)
) {
    /** 权益卡片视图:仅暴露变更底版所需边界字段。 */
    public record CardView(String cardNo, String rightType, String scope,
                           LocalDateTime validDate, String cardStatus, String consolidatedUnit) {
    }

    /** 确权汇总认定意见视图(表3/表4)。 */
    public record SummaryView(String summaryType, String content, String generatorId) {
    }
}
