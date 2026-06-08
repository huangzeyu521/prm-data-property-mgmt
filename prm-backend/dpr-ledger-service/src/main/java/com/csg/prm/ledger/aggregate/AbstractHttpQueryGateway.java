package com.csg.prm.ledger.aggregate;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域只读查询网关 HTTP 基类:POST 目标域的分页接口(返回 R<PageResult<T>>),
 * 取 data.records 数组并交由子类映射为 {@link DomainRecord}。
 * 读取失败仅告警并返回空列表,绝不抛出——保证资产360/待办中心永远可用(降级为仅台账本地数据)。
 */
abstract class AbstractHttpQueryGateway {

    private static final Logger log = LoggerFactory.getLogger(AbstractHttpQueryGateway.class);
    /** 过取上限:试点规模下一次拉满后在内存按资产/待办过滤,避免改动各域查询接口 */
    private static final int FETCH_SIZE = 500;

    protected final RestClient client;
    private final String pagePath;

    protected AbstractHttpQueryGateway(String baseUrl, String pagePath) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.pagePath = pagePath;
    }

    /** 拉取一页记录并逐条映射;映射器返回 null 表示该条不纳入结果(如待办过滤) */
    protected List<DomainRecord> fetch(java.util.function.Function<JsonNode, DomainRecord> mapper) {
        List<DomainRecord> result = new ArrayList<>();
        try {
            JsonNode body = client.post().uri(pagePath)
                    .header("Content-Type", "application/json")
                    .header("X-User-Id", "system")
                    .body("{\"current\":1,\"size\":" + FETCH_SIZE + "}")
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode records = body == null ? null : body.path("data").path("records");
            if (records != null && records.isArray()) {
                for (JsonNode n : records) {
                    DomainRecord r = mapper.apply(n);
                    if (r != null) {
                        result.add(r);
                    }
                }
            }
        } catch (RuntimeException e) {
            log.warn("[聚合查询] 调用 {} 失败(降级为空): {}", pagePath, e.getMessage());
        }
        return result;
    }

    protected static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    protected static Integer intVal(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asInt();
    }
}
