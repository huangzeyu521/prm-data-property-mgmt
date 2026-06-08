package com.csg.prm.common.crypto;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 国密 SM4 分组密码算法(GB/T 32907-2016)纯 Java 实现。ECB 模式 + PKCS#7 填充。
 * 用于敏感字段加密存储(信创环境合规要求使用国密算法)。
 */
public final class Sm4Util {

    private static final int BLOCK = 16;

    private static final byte[] SBOX = hex(
            "d690e9fecce13db716b614c228fb2c05"
          + "2b679a762abe04c3aa441326498606 99".replace(" ", "")
          + "9c4250f491ef987a33540b43edcfac62"
          + "e4b31ca9c908e89580df94fa758f3fa6"
          + "4707a7fcf37317ba83593c19e6854fa8"
          + "686b81b27164da8bf8eb0f4b70569d35"
          + "1e240e5e6358d1a225227c3b0121788 7".replace(" ", "")
          + "d400465 79fd327524c3602e7a0c4c89e".replace(" ", "")
          + "eabf8ad240c738b5a3f7f2cef96115a1"
          + "e0ae5da49b341a55ad933230f58cb1e3"
          + "1df6e22e8266ca60c02923ab0d534e6f"
          + "d5db3745defd8e2f03ff6a726d6c5b51"
          + "8d1baf92bbddbc7f11d95c411f105ad8"
          + "0ac13188a5cd7bbd2d74d012b8e5b4b0"
          + "89699 74a0c96777e65b9f109c56ec684".replace(" ", "")
          + "18f07dec3adc4d2079ee5f3ed7cb3948");

    private static final int[] FK = {0xa3b1bac6, 0x56aa3350, 0x677d9197, 0xb27022dc};

    private static final int[] CK = {
            0x00070e15, 0x1c232a31, 0x383f464d, 0x545b6269, 0x70777e85, 0x8c939aa1, 0xa8afb6bd, 0xc4cbd2d9,
            0xe0e7eef5, 0xfc030a11, 0x181f262d, 0x343b4249, 0x50575e65, 0x6c737a81, 0x888f969d, 0xa4abb2b9,
            0xc0c7ced5, 0xdce3eaf1, 0xf8ff060d, 0x141b2229, 0x30373e45, 0x4c535a61, 0x686f767d, 0x848b9299,
            0xa0a7aeb5, 0xbcc3cad1, 0xd8dfe6ed, 0xf4fb0209, 0x10171e25, 0x2c333a41, 0x484f565d, 0x646b7279
    };

    private Sm4Util() {
    }

    /** 明文(UTF-8)-> SM4-ECB 密文十六进制 */
    public static String encryptHex(String plain, String keyHex) {
        return toHex(encryptEcb(plain.getBytes(StandardCharsets.UTF_8), normalizeKey(keyHex)));
    }

    /** SM4-ECB 密文十六进制 -> 明文(UTF-8) */
    public static String decryptHex(String cipherHex, String keyHex) {
        return new String(decryptEcb(hex(cipherHex), normalizeKey(keyHex)), StandardCharsets.UTF_8);
    }

    public static byte[] encryptEcb(byte[] data, byte[] key) {
        int[] rk = expandKey(key, true);
        byte[] padded = pkcs7Pad(data);
        byte[] out = new byte[padded.length];
        for (int i = 0; i < padded.length; i += BLOCK) {
            cryptBlock(padded, i, out, i, rk);
        }
        return out;
    }

    public static byte[] decryptEcb(byte[] data, byte[] key) {
        if (data.length == 0 || data.length % BLOCK != 0) {
            throw new IllegalArgumentException("SM4 密文长度非法");
        }
        int[] rk = expandKey(key, false);
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i += BLOCK) {
            cryptBlock(data, i, out, i, rk);
        }
        return pkcs7Unpad(out);
    }

    /** 单分组加/解密(16 字节),供 KAT 校验 */
    public static byte[] cryptBlock16(byte[] in16, byte[] key, boolean encrypt) {
        int[] rk = expandKey(key, encrypt);
        byte[] out = new byte[BLOCK];
        cryptBlock(in16, 0, out, 0, rk);
        return out;
    }

    private static void cryptBlock(byte[] in, int inOff, byte[] out, int outOff, int[] rk) {
        int[] x = new int[4];
        for (int i = 0; i < 4; i++) {
            x[i] = beInt(in, inOff + i * 4);
        }
        int b0 = x[0], b1 = x[1], b2 = x[2], b3 = x[3];
        for (int i = 0; i < 32; i++) {
            int tmp = b0 ^ t(b1 ^ b2 ^ b3 ^ rk[i]);
            b0 = b1;
            b1 = b2;
            b2 = b3;
            b3 = tmp;
        }
        // 反序输出 X35,X34,X33,X32
        putInt(out, outOff, b3);
        putInt(out, outOff + 4, b2);
        putInt(out, outOff + 8, b1);
        putInt(out, outOff + 12, b0);
    }

    private static int[] expandKey(byte[] key, boolean encrypt) {
        if (key.length != BLOCK) {
            throw new IllegalArgumentException("SM4 密钥须为 16 字节");
        }
        int[] k = new int[4];
        for (int i = 0; i < 4; i++) {
            k[i] = beInt(key, i * 4) ^ FK[i];
        }
        int[] rk = new int[32];
        int k0 = k[0], k1 = k[1], k2 = k[2], k3 = k[3];
        for (int i = 0; i < 32; i++) {
            int tmp = k0 ^ tPrime(k1 ^ k2 ^ k3 ^ CK[i]);
            rk[i] = tmp;
            k0 = k1;
            k1 = k2;
            k2 = k3;
            k3 = tmp;
        }
        if (!encrypt) {
            for (int i = 0; i < 16; i++) {
                int t = rk[i];
                rk[i] = rk[31 - i];
                rk[31 - i] = t;
            }
        }
        return rk;
    }

    private static int t(int x) {
        int b = tau(x);
        return b ^ rotl(b, 2) ^ rotl(b, 10) ^ rotl(b, 18) ^ rotl(b, 24);
    }

    private static int tPrime(int x) {
        int b = tau(x);
        return b ^ rotl(b, 13) ^ rotl(b, 23);
    }

    private static int tau(int x) {
        return ((SBOX[(x >>> 24) & 0xff] & 0xff) << 24)
                | ((SBOX[(x >>> 16) & 0xff] & 0xff) << 16)
                | ((SBOX[(x >>> 8) & 0xff] & 0xff) << 8)
                | (SBOX[x & 0xff] & 0xff);
    }

    private static int rotl(int x, int n) {
        return (x << n) | (x >>> (32 - n));
    }

    private static int beInt(byte[] b, int off) {
        return ((b[off] & 0xff) << 24) | ((b[off + 1] & 0xff) << 16)
                | ((b[off + 2] & 0xff) << 8) | (b[off + 3] & 0xff);
    }

    private static void putInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >>> 24);
        b[off + 1] = (byte) (v >>> 16);
        b[off + 2] = (byte) (v >>> 8);
        b[off + 3] = (byte) v;
    }

    private static byte[] pkcs7Pad(byte[] data) {
        int pad = BLOCK - (data.length % BLOCK);
        byte[] out = Arrays.copyOf(data, data.length + pad);
        for (int i = data.length; i < out.length; i++) {
            out[i] = (byte) pad;
        }
        return out;
    }

    private static byte[] pkcs7Unpad(byte[] data) {
        int pad = data[data.length - 1] & 0xff;
        if (pad < 1 || pad > BLOCK) {
            throw new IllegalArgumentException("SM4 填充非法");
        }
        return Arrays.copyOf(data, data.length - pad);
    }

    /** 密钥规范化:接受 32 位十六进制(16 字节);其他字符串以 SM3 派生 16 字节 */
    private static byte[] normalizeKey(String keyHex) {
        if (keyHex != null && keyHex.matches("[0-9a-fA-F]{32}")) {
            return hex(keyHex);
        }
        // 由任意口令经 SM3 派生稳定 16 字节密钥
        byte[] h = hex(Sm3Util.hashHex(keyHex == null ? "" : keyHex));
        return Arrays.copyOf(h, BLOCK);
    }

    static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) {
            sb.append(Character.forDigit((x >> 4) & 0xf, 16));
            sb.append(Character.forDigit(x & 0xf, 16));
        }
        return sb.toString();
    }

    static byte[] hex(String s) {
        int n = s.length() / 2;
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) {
            b[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return b;
    }
}
