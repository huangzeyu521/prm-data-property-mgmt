package com.csg.prm.confirm.integration.dto;

import java.util.List;

/**
 * 确权变更·授权影响:该系统/资产是否存在有效对外授权,变更可能影响在用授权关系。
 * 按"动态跟踪原则":来源权益收紧或权益到期时,联动评估对应授权的暂停/续签/终止。
 * 接入后由授权服务(dpr-authorize)按 assetId/系统反查有效授权;当前为确权变更联动桩。
 */
public record AuthImpact(String sysName, boolean hasActive, List<Item> items) {
    public record Item(String tableCode, String tableName, String authId, String scope,
                       String authStatus, String suggestion) {
    }
}
