package com.csg.prm.common.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轻量 JWT(HMAC-SHA256)自实现:零外部依赖(仅 JDK Mac + Base64URL),适配信创离线构建。
 * 载荷:sub(userId)/name/role/prov/exp。生产由 4A 在网关签发同构身份头,此处用于内建登录/演示。
 */
public final class JwtUtil {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final String HEADER = B64.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    /** 密钥:优先环境变量/系统属性 PRM_JWT_SECRET,否则用内置默认(生产须覆盖) */
    private static String secret() {
        String s = System.getenv("PRM_JWT_SECRET");
        if (s == null || s.isEmpty()) s = System.getProperty("prm.auth.jwt-secret");
        return (s == null || s.isEmpty()) ? "prm-dpr-default-secret-change-in-prod-2026" : s;
    }

    private JwtUtil() {
    }

    /** 签发 token,ttlSeconds 后过期 */
    public static String issue(String userId, String name, String role, String province, long ttlSeconds) {
        long exp = System.currentTimeMillis() / 1000 + ttlSeconds;
        String payloadJson = "{"
                + "\"sub\":\"" + esc(userId) + "\","
                + "\"name\":\"" + esc(name) + "\","
                + "\"role\":\"" + esc(role) + "\","
                + "\"prov\":\"" + esc(province == null ? "" : province) + "\","
                + "\"exp\":" + exp + "}";
        String payload = B64.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = HEADER + "." + payload;
        return signingInput + "." + sign(signingInput);
    }

    /** 校验签名与过期,通过则返回载荷字段,否则返回 null */
    public static Map<String, String> verify(String token) {
        if (token == null) return null;
        String t = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
        String[] parts = t.split("\\.");
        if (parts.length != 3) return null;
        String signingInput = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(signingInput), parts[2])) return null;
        String payloadJson = new String(B64D.decode(parts[1]), StandardCharsets.UTF_8);
        Map<String, String> claims = parseFlat(payloadJson);
        String exp = claims.get("exp");
        if (exp != null) {
            try {
                if (Long.parseLong(exp) < System.currentTimeMillis() / 1000) return null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return claims;
    }

    private static String sign(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return B64.encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签名失败", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }

    /** 极简扁平 JSON 解析(仅用于自签载荷,键值无嵌套) */
    private static Map<String, String> parseFlat(String json) {
        Map<String, String> m = new LinkedHashMap<>();
        String body = json.trim();
        if (body.startsWith("{")) body = body.substring(1);
        if (body.endsWith("}")) body = body.substring(0, body.length() - 1);
        for (String seg : splitTop(body)) {
            int c = seg.indexOf(':');
            if (c < 0) continue;
            String k = unq(seg.substring(0, c).trim());
            String v = unq(seg.substring(c + 1).trim());
            m.put(k, v);
        }
        return m;
    }

    private static java.util.List<String> splitTop(String s) {
        java.util.List<String> out = new java.util.ArrayList<>();
        boolean inStr = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"') inStr = !inStr;
            if (ch == ',' && !inStr) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private static String unq(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
