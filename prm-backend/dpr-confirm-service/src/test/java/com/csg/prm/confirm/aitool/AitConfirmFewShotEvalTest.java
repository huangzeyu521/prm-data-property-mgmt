package com.csg.prm.confirm.aitool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 确权要素抽取 few-shot/评测集完整性 + 脱敏守卫校验(纯单测,不依赖 Spring)。
 * 数据来源:南网 MDAU 产权补录工单脱敏后生成(scripts/gen-confirm-fewshot.py)。
 */
class AitConfirmFewShotEvalTest {

    private static final String RES = "/aitool/eval/confirm-fewshot.json";
    private static final ObjectMapper OM = new ObjectMapper();
    private static final Set<String> SECRECY = Set.of("不涉密", "核心商密", "普通商密", "工作秘密", "敏感信息");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)\\d{11}(?!\\d)");
    private static final Pattern IPV4 = Pattern.compile("\\b\\d{1,3}(?:\\.\\d{1,3}){3}\\b");

    private String raw() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(RES)) {
            assertNotNull(in, "评测集资源应存在:" + RES);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private JsonNode root() throws Exception {
        return OM.readTree(raw());
    }

    /** 规模与一致性:caseCount 与实际条数一致,覆盖足够多。 */
    @Test
    void dataset_size_consistent() throws Exception {
        JsonNode r = root();
        JsonNode cases = r.get("cases");
        assertTrue(cases.isArray() && cases.size() >= 100, "评测集应有充足样本,实际=" + cases.size());
        assertEquals(cases.size(), r.get("caseCount").asInt(), "caseCount 应与实际条数一致");
        assertTrue(r.get("sourceWorkOrders").asInt() >= 10, "应覆盖多个工单来源");
    }

    /** 每个 case 结构完整:输入字段 + 标签字段齐全且类型正确。 */
    @Test
    void every_case_well_formed() throws Exception {
        for (JsonNode c : root().get("cases")) {
            assertTrue(hasText(c, "id"), "case 应有 id");
            assertTrue(hasText(c, "source"), "case 应有 source");
            assertTrue(hasText(c.path("system"), "name"), "case 应有 system.name");
            assertTrue(hasText(c.path("input"), "tableNameCn"), "case 应有 input.tableNameCn");

            JsonNode e = c.get("expected");
            assertNotNull(e, "case 应有 expected");
            for (String b : new String[]{"involvesRegulation", "involvesPrivacy",
                    "involvesTradeSecret", "involvesThirdPartyAgreement"}) {
                assertTrue(e.has(b) && e.get(b).isBoolean(), "expected." + b + " 应为布尔");
            }
            JsonNode sec = e.get("secrecy");
            if (sec != null && !sec.isNull()) {
                assertTrue(SECRECY.contains(sec.asText()), "密级应在枚举内,实际=" + sec.asText());
            }
            JsonNode st = e.get("sourceType");
            if (st != null && !st.isNull()) {
                assertTrue(st.asText().matches("^[A-F].*"), "来源方式应为 A-F 大类,实际=" + st.asText());
            }
        }
    }

    /** 脱敏守卫:全文不得残留 11 位手机号 / IPv4。 */
    @Test
    void desensitized_no_pii() throws Exception {
        String raw = raw();
        assertFalse(PHONE.matcher(raw).find(), "评测集不应残留手机号");
        assertFalse(IPV4.matcher(raw).find(), "评测集不应残留 IP 地址");
    }

    /** 标签有区分度:布尔标签应同时存在正反样本,可用于评测。 */
    @Test
    void labels_have_variance() throws Exception {
        boolean regTrue = false;
        boolean regFalse = false;
        for (JsonNode c : root().get("cases")) {
            if (c.path("expected").path("involvesRegulation").asBoolean()) {
                regTrue = true;
            } else {
                regFalse = true;
            }
        }
        assertTrue(regTrue && regFalse, "行政监管标签应同时含正反样本");
    }

    private static boolean hasText(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v != null && !v.isNull() && !v.asText().isBlank();
    }
}
