package com.csg.prm.common.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 国密加解密/签验服务测试(直接构造,免 Spring 上下文)。
 */
class GmCryptoServiceTest {

    private final GmCryptoService gm = new GmCryptoService(
            "0123456789abcdeffedcba9876543210",
            "3945208f7b2144b13f36e38ac6d39f95889393692860b51a42fb81ef4df7c5b8");

    @Test
    void sm4_encrypt_decrypt_roundtrip() {
        String plain = "第三方权益:某征信机构;权益转移已约定";
        String cipher = gm.sm4Encrypt(plain);
        assertNotEquals(plain, cipher);
        assertEquals(plain, gm.sm4Decrypt(cipher));
    }

    @Test
    void sm2_sign_and_verify() {
        String data = "确权制卡 EC-PRA-001";
        String sig = gm.sm2Sign(data);
        assertTrue(gm.sm2Verify(data, sig));
        assertFalse(gm.sm2Verify(data + "x", sig));
        assertEquals(128, gm.getSm2PublicKey().length());
    }

    @Test
    void sm3_fingerprint_length() {
        assertEquals(64, gm.sm3("abc").length());
    }
}
