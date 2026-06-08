package com.csg.prm.common.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * SM4 国密分组密码已知答案测试(GB/T 32907-2016 标准向量)+ 加解密往返。
 */
class Sm4UtilTest {

    @Test
    void kat_single_block() {
        // 标准向量:key = plaintext = 0123456789abcdeffedcba9876543210
        byte[] key = Sm4Util.hex("0123456789abcdeffedcba9876543210");
        byte[] in = Sm4Util.hex("0123456789abcdeffedcba9876543210");
        byte[] out = Sm4Util.cryptBlock16(in, key, true);
        assertEquals("681edf34d206965e86b3e94f536e4246", Sm4Util.toHex(out));
        // 解密复原
        assertEquals("0123456789abcdeffedcba9876543210", Sm4Util.toHex(Sm4Util.cryptBlock16(out, key, false)));
    }

    @Test
    void encrypt_decrypt_roundtrip_with_chinese() {
        String key = "0123456789abcdeffedcba9876543210";
        String plain = "中国南方电网-数据权益保密承诺函-甲方签章";
        String cipher = Sm4Util.encryptHex(plain, key);
        assertNotEquals(plain, cipher);
        assertEquals(plain, Sm4Util.decryptHex(cipher, key));
    }

    @Test
    void password_key_is_derived_via_sm3() {
        // 非 32 位十六进制口令经 SM3 派生 16 字节密钥,仍可正确往返
        String key = "运维口令-2026";
        String plain = "敏感字段";
        assertEquals(plain, Sm4Util.decryptHex(Sm4Util.encryptHex(plain, key), key));
    }
}
