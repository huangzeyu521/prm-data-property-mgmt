package com.csg.prm.ledger.aggregate;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 确权域查询 HTTP 实现:经确权服务 /api/dpr/confirm/apply/page 读取确权申请,映射为中性记录。
 * 仅当 prm.aggregate.enabled=true 时启用并 @Primary 覆盖本地桩。
 */
@Component
@Primary
@ConditionalOnProperty(name = "prm.aggregate.enabled", havingValue = "true")
public class HttpConfirmQueryGateway extends AbstractHttpQueryGateway implements ConfirmQueryGateway {

    /** 审批中(待办)状态:人工预审中 / 合规审核中 / 主管复核中 / 经理终审中 */
    private static final Set<String> PENDING = Set.of("人工预审中", "合规审核中", "主管复核中", "经理终审中");
    private static final String LINK_PREFIX = "/confirm/apply/";

    public HttpConfirmQueryGateway(
            @Value("${prm.aggregate.confirm-url:http://localhost:9102}") String confirmUrl) {
        super(confirmUrl, "/api/dpr/confirm/apply/page");
    }

    private DomainRecord map(JsonNode n) {
        DomainRecord r = new DomainRecord();
        r.setDomain("确权");
        r.setId(text(n, "applyId"));
        r.setNo(text(n, "applyNo"));
        r.setAssetId(text(n, "assetId"));
        r.setAssetName(text(n, "assetName"));
        r.setRightType(text(n, "rightType"));
        r.setParty(text(n, "rightHolder"));
        r.setStatus(text(n, "status"));
        r.setNode(intVal(n, "currentNode"));
        r.setTime(text(n, "createTime"));
        r.setLink(LINK_PREFIX + r.getId());
        return r;
    }

    @Override
    public List<DomainRecord> findByAsset(String assetId) {
        return fetch(n -> assetId != null && assetId.equals(text(n, "assetId")) ? map(n) : null);
    }

    @Override
    public List<DomainRecord> pending() {
        return fetch(n -> PENDING.contains(text(n, "status")) ? map(n) : null);
    }
}
