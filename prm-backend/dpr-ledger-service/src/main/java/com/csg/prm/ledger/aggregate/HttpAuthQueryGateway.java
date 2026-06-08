package com.csg.prm.ledger.aggregate;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 授权域查询 HTTP 实现:经授权服务 /api/dpr/auth/apply/page 读取授权申请,映射为中性记录。
 * 仅当 prm.aggregate.enabled=true 时启用并 @Primary 覆盖本地桩。
 */
@Component
@Primary
@ConditionalOnProperty(name = "prm.aggregate.enabled", havingValue = "true")
public class HttpAuthQueryGateway extends AbstractHttpQueryGateway implements AuthQueryGateway {

    /** 终态(非待办):草稿 / 已生效 / 已驳回;其余均视为审批链待办 */
    private static final Set<String> TERMINAL = Set.of("草稿", "已生效", "已驳回");
    private static final String LINK_PREFIX = "/authorize/apply/";

    public HttpAuthQueryGateway(
            @Value("${prm.aggregate.auth-url:http://localhost:9103}") String authUrl) {
        super(authUrl, "/api/dpr/auth/apply/page");
    }

    private DomainRecord map(JsonNode n) {
        DomainRecord r = new DomainRecord();
        r.setDomain("授权");
        r.setId(text(n, "applyId"));
        r.setNo(text(n, "applyNo"));
        r.setAssetId(text(n, "assetId"));
        r.setAssetName(text(n, "assetName"));
        r.setRightType(text(n, "rightType"));
        r.setParty(text(n, "granteeOrg"));
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
        return fetch(n -> {
            String status = text(n, "status");
            return status != null && !TERMINAL.contains(status) ? map(n) : null;
        });
    }
}
