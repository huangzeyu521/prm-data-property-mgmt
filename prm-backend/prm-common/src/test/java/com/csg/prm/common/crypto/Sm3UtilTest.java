package com.csg.prm.common.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * SM3 国密杂凑算法已知答案测试(GB/T 32905-2016 标准向量)。
 */
class Sm3UtilTest {

    @Test
    void kat_abc() {
        // 标准向量:SM3("abc")
        assertEquals("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0",
                Sm3Util.hashHex("abc"));
    }

    @Test
    void kat_512bit_message() {
        // 标准向量:64 字节 "abcd"*16
        String input = "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd";
        assertEquals("debe9ff92275b8a138604889c18e5a4d6fdb70e5387e5765293dcba39c0c5732",
                Sm3Util.hashHex(input));
    }

    @Test
    void empty_and_tamper_sensitivity() {
        // 空串有确定摘要;单字符改动导致摘要完全不同(防篡改基础)
        assertEquals(64, Sm3Util.hashHex("").length());
        assertNotEquals(Sm3Util.hashHex("权益卡片A|DA-001"), Sm3Util.hashHex("权益卡片A|DA-002"));
    }
}
