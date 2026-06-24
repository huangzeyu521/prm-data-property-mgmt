package com.csg.prm.common.workflow.cbpm;

import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * CBPM/BPS 流程组件网关 HTTP 实现:经 RestClient 调用南网平台流程引擎。
 * 仅当 {@code prm.cbpm.enabled=true} 时启用并 @Primary 覆盖本地桩。
 * <p>严格遵循《DAP 集成接口说明文档 1.1》:
 * <ul>
 *   <li>操作:POST {@code /api/cbpm/bps/submit}(数组体)、{@code /api/cbpm/bps/batchDataSubmit}</li>
 *   <li>查询:POST {@code /api/wf/queryTodoVOListByPage} 等</li>
 *   <li>请求头:X-EOS-UserId/UserName、X-BPS-ClientId/TenantId/TenantToken、X-JADP-ctp-t-code</li>
 * </ul>
 */
@Component
@Primary
@ConditionalOnProperty(name = "prm.cbpm.enabled", havingValue = "true")
public class HttpCbpmWorkflowGateway implements CbpmWorkflowGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpCbpmWorkflowGateway.class);

    private final RestClient client;
    private final String clientId;
    private final String tenantId;
    private final String tenantToken;
    private final String jadpTCode;

    public HttpCbpmWorkflowGateway(
            @Value("${prm.cbpm.base-url:http://localhost:9898}") String baseUrl,
            @Value("${prm.cbpm.client-id:default}") String clientId,
            @Value("${prm.cbpm.tenant-id:BPS_DEFAULT_TENANT}") String tenantId,
            @Value("${prm.cbpm.tenant-token:}") String tenantToken,
            @Value("${prm.cbpm.jadp-t-code:}") String jadpTCode) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.clientId = clientId;
        this.tenantId = tenantId;
        this.tenantToken = tenantToken;
        this.jadpTCode = jadpTCode;
        log.info("[CBPM] 已启用 HTTP 流程引擎对接,地址={} 租户={}", baseUrl, tenantId);
    }

    @Override
    public CbpmResult submit(CbpmSubmitRequest request) {
        // 提交接口按数组上送 [ {...} ]
        return post("/api/cbpm/bps/submit", List.of(request), CbpmResult.class, CbpmResult.ok(0));
    }

    @Override
    public CbpmResult batchSubmit(List<CbpmSubmitRequest> requests) {
        return post("/api/cbpm/bps/batchDataSubmit", requests, CbpmResult.class, CbpmResult.ok(0));
    }

    @Override
    public List<CbpmWorkItem> queryTodo(CbpmTodoQuery query) {
        PageResp resp = post("/api/wf/queryTodoVOListByPage", query, PageResp.class, null);
        return resp == null || resp.list == null ? Collections.emptyList() : resp.list;
    }

    @Override
    public long countTodo(CbpmTodoQuery query) {
        Long n = post("/api/wf/queryTodoVOCount", query, Long.class, 0L);
        return n == null ? 0L : n;
    }

    @Override
    public List<CbpmWorkItem> queryDone(CbpmTodoQuery query) {
        PageResp resp = post("/api/wf/queryDoneVOListByPage", query, PageResp.class, null);
        return resp == null || resp.list == null ? Collections.emptyList() : resp.list;
    }

    /** 统一 POST:注入规范请求头,失败仅告警并返回兜底值(不阻断主流程)。 */
    private <T> T post(String uri, Object body, Class<T> type, T fallback) {
        try {
            return client.post().uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers())
                    .body(body)
                    .retrieve()
                    .body(type);
        } catch (RuntimeException e) {
            log.warn("[CBPM] 调用 {} 失败(不阻断主流程): {}", uri, e.getMessage());
            return fallback;
        }
    }

    /** 注入 X-EOS-*(当前操作人)+ X-BPS-*(租户/客户端/令牌)+ X-JADP-* 规范头。 */
    private Consumer<org.springframework.http.HttpHeaders> headers() {
        UserContext ctx = UserContextHolder.get();
        String userId = ctx != null && StringUtils.hasText(ctx.getUserId()) ? ctx.getUserId() : "system";
        String userName = ctx != null && StringUtils.hasText(ctx.getUserName()) ? ctx.getUserName() : userId;
        return h -> {
            h.set("X-EOS-UserId", userId);
            h.set("X-EOS_UserName", userName);
            h.set("X-BPS-ClientId", clientId);
            h.set("X-BPS-TenantId", tenantId);
            if (StringUtils.hasText(tenantToken)) {
                h.set("X-BPS-TenantToken", tenantToken);
            }
            if (StringUtils.hasText(jadpTCode)) {
                h.set("X-JADP-ctp-t-code", jadpTCode);
            }
        };
    }

    /** 分页查询响应包装:{@code {list, count, pageNo}}。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PageResp {
        public List<CbpmWorkItem> list;
        public int count;
        public int pageNo;
    }
}
