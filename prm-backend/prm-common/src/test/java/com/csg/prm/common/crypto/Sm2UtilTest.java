package com.csg.prm.common.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SM2 国密签名算法功能性 KAT:签名-验签往返、防篡改、错误公钥拒绝、公钥派生确定性。
 */
class Sm2UtilTest {

    @Test
    void sign_then_verify_roundtrip() {
        String[] kp = Sm2Util.generateKeyPair();
        String priv = kp[0];
        String pub = kp[1];
        String msg = "授权证书 AC-PRA-001 发证存证";

        String sig = Sm2Util.sign(priv, msg);
        assertEquals(128, sig.length(), "签名应为 R||S 64 字节");
        assertTrue(Sm2Util.verify(pub, msg, sig), "合法签名应验签通过");
    }

    @Test
    void verify_should_fail_on_tampered_message() {
        String[] kp = Sm2Util.generateKeyPair();
        String sig = Sm2Util.sign(kp[0], "原始数据");
        assertFalse(Sm2Util.verify(kp[1], "原始数据-被篡改", sig), "篡改消息应验签失败");
    }

    @Test
    void verify_should_fail_with_wrong_public_key() {
        String[] kp1 = Sm2Util.generateKeyPair();
        String[] kp2 = Sm2Util.generateKeyPair();
        String sig = Sm2Util.sign(kp1[0], "数据");
        assertFalse(Sm2Util.verify(kp2[1], "数据", sig), "他人公钥应验签失败");
    }

    @Test
    void public_key_derivation_is_deterministic() {
        String priv = "3945208f7b2144b13f36e38ac6d39f95889393692860b51a42fb81ef4df7c5b8";
        String pub1 = Sm2Util.publicKeyOf(priv);
        String pub2 = Sm2Util.publicKeyOf(priv);
        assertEquals(pub1, pub2);
        assertEquals(128, pub1.length());
        assertTrue(Sm2Util.verify(pub1, "x", Sm2Util.sign(priv, "x")));
    }
}
