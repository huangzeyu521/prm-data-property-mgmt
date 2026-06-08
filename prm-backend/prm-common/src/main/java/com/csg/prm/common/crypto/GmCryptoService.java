package com.csg.prm.common.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 国密加解密/签验服务(SM4 对称加密 + SM2 签名)。
 * 管理服务级密钥(支持配置注入,缺省内置开发密钥),供敏感字段加密与关键操作签章使用。
 * 生产环境密钥应由密码机/KMS 托管。
 */
@Service
public class GmCryptoService {

    /** SM4 16 字节密钥(32 位十六进制);非十六进制口令将经 SM3 派生 */
    private final String sm4Key;
    /** SM2 私钥(32 字节十六进制) */
    private final String sm2Priv;
    /** SM2 公钥(64 字节十六进制 X||Y),由私钥派生 */
    private final String sm2Pub;

    public GmCryptoService(
            @Value("${prm.gm.sm4-key:0123456789abcdeffedcba9876543210}") String sm4Key,
            @Value("${prm.gm.sm2-priv:3945208f7b2144b13f36e38ac6d39f95889393692860b51a42fb81ef4df7c5b8}") String sm2Priv) {
        this.sm4Key = sm4Key;
        this.sm2Priv = sm2Priv;
        this.sm2Pub = Sm2Util.publicKeyOf(sm2Priv);
    }

    /** SM4-ECB 加密(明文 -> 密文十六进制) */
    public String sm4Encrypt(String plain) {
        return Sm4Util.encryptHex(plain, sm4Key);
    }

    /** SM4-ECB 解密(密文十六进制 -> 明文) */
    public String sm4Decrypt(String cipherHex) {
        return Sm4Util.decryptHex(cipherHex, sm4Key);
    }

    /** SM3 数据指纹 */
    public String sm3(String data) {
        return Sm3Util.hashHex(data);
    }

    /** SM2 签名(返回 R||S 十六进制) */
    public String sm2Sign(String data) {
        return Sm2Util.sign(sm2Priv, data);
    }

    /** SM2 验签(使用服务公钥) */
    public boolean sm2Verify(String data, String signHex) {
        return Sm2Util.verify(sm2Pub, data, signHex);
    }

    public String getSm2PublicKey() {
        return sm2Pub;
    }
}
