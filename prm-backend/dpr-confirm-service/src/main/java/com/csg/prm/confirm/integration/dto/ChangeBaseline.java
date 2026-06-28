package com.csg.prm.confirm.integration.dto;

/**
 * 确权变更基线:某系统"现有确权结论"快照,供「确权变更申请」做变更前→变更后 diff 与预填。
 * 接入平台后由确权结果(表3/表4/权益卡片)反查;当前由 DataCatalogService 据已确权库表合成。
 */
public record ChangeBaseline(
        String sysName,        // 系统名称
        String rightHolder,    // 权属主体(公司主体)
        String subjectLevel,   // 主体层级(总部/分省/专业子公司)
        String respDept,       // 责任部门
        String rightType,      // 现有权属类型(三权,「、」分隔)
        String regulated,      // 管制属性
        String sourceIdent,    // 数据来源权益识别 A–F(现状并集)
        String relationIdent,  // 信息关联权益识别 G–J(现状并集)
        String authTime,       // 确权时间(基线版本时间)
        int version            // 当前确权版本号(变更后 +1)
) {
}
