package com.csg.prm.common.crypto;

import java.nio.charset.StandardCharsets;

/**
 * 国密 SM3 密码杂凑算法(GB/T 32905-2016)纯 Java 实现。
 * 用于区块链存证的数据指纹(防篡改);信创环境合规要求使用国密算法。
 */
public final class Sm3Util {

    private static final int[] IV = {
            0x7380166f, 0x4914b2b9, 0x172442d7, 0xda8a0600,
            0xa96f30bc, 0x163138aa, 0xe38dee4d, 0xb0fb0e4e
    };

    private Sm3Util() {
    }

    /** 对 UTF-8 字符串求 SM3,返回 64 位小写十六进制摘要 */
    public static String hashHex(String input) {
        return hashHex(input == null ? new byte[0] : input.getBytes(StandardCharsets.UTF_8));
    }

    /** 对字节数组求 SM3,返回 64 位小写十六进制摘要 */
    public static String hashHex(byte[] message) {
        byte[] digest = hash(message);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16));
            sb.append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }

    /** 返回 32 字节 SM3 摘要(供 SM2 等算法拼接使用) */
    public static byte[] hash(byte[] message) {
        byte[] padded = pad(message);
        int[] v = IV.clone();
        int blocks = padded.length / 64;
        int[] w = new int[68];
        int[] w1 = new int[64];
        for (int i = 0; i < blocks; i++) {
            compress(v, padded, i * 64, w, w1);
        }
        byte[] out = new byte[32];
        for (int i = 0; i < 8; i++) {
            out[i * 4] = (byte) (v[i] >>> 24);
            out[i * 4 + 1] = (byte) (v[i] >>> 16);
            out[i * 4 + 2] = (byte) (v[i] >>> 8);
            out[i * 4 + 3] = (byte) v[i];
        }
        return out;
    }

    private static byte[] pad(byte[] msg) {
        long bitLen = (long) msg.length * 8;
        int k = 56 - (int) ((msg.length + 1) % 64);
        if (k < 0) {
            k += 64;
        }
        byte[] padded = new byte[msg.length + 1 + k + 8];
        System.arraycopy(msg, 0, padded, 0, msg.length);
        padded[msg.length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) {
            padded[padded.length - 1 - i] = (byte) (bitLen >>> (8 * i));
        }
        return padded;
    }

    private static void compress(int[] v, byte[] data, int off, int[] w, int[] w1) {
        for (int j = 0; j < 16; j++) {
            w[j] = ((data[off + j * 4] & 0xff) << 24)
                    | ((data[off + j * 4 + 1] & 0xff) << 16)
                    | ((data[off + j * 4 + 2] & 0xff) << 8)
                    | (data[off + j * 4 + 3] & 0xff);
        }
        for (int j = 16; j < 68; j++) {
            w[j] = p1(w[j - 16] ^ w[j - 9] ^ rotl(w[j - 3], 15)) ^ rotl(w[j - 13], 7) ^ w[j - 6];
        }
        for (int j = 0; j < 64; j++) {
            w1[j] = w[j] ^ w[j + 4];
        }

        int a = v[0], b = v[1], c = v[2], d = v[3], e = v[4], f = v[5], g = v[6], h = v[7];
        for (int j = 0; j < 64; j++) {
            int t = j < 16 ? 0x79cc4519 : 0x7a879d8a;
            int ss1 = rotl(rotl(a, 12) + e + rotl(t, j), 7);
            int ss2 = ss1 ^ rotl(a, 12);
            int tt1 = ff(a, b, c, j) + d + ss2 + w1[j];
            int tt2 = gg(e, f, g, j) + h + ss1 + w[j];
            d = c;
            c = rotl(b, 9);
            b = a;
            a = tt1;
            h = g;
            g = rotl(f, 19);
            f = e;
            e = p0(tt2);
        }
        v[0] ^= a;
        v[1] ^= b;
        v[2] ^= c;
        v[3] ^= d;
        v[4] ^= e;
        v[5] ^= f;
        v[6] ^= g;
        v[7] ^= h;
    }

    private static int ff(int x, int y, int z, int j) {
        return j < 16 ? (x ^ y ^ z) : ((x & y) | (x & z) | (y & z));
    }

    private static int gg(int x, int y, int z, int j) {
        return j < 16 ? (x ^ y ^ z) : ((x & y) | (~x & z));
    }

    private static int p0(int x) {
        return x ^ rotl(x, 9) ^ rotl(x, 17);
    }

    private static int p1(int x) {
        return x ^ rotl(x, 15) ^ rotl(x, 23);
    }

    private static int rotl(int x, int n) {
        n &= 31;
        return (x << n) | (x >>> (32 - n));
    }
}
