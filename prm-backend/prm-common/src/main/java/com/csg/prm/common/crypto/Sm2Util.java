package com.csg.prm.common.crypto;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * 国密 SM2 椭圆曲线数字签名算法(GB/T 32918,推荐曲线 sm2p256v1)纯 Java 实现。
 * 提供密钥对生成、签名与验签(用于区块链存证国密签名留痕、关键操作签章)。
 * 公钥编码:64 字节十六进制(X||Y);私钥:32 字节十六进制;签名:64 字节十六进制(R||S)。
 */
public final class Sm2Util {

    private static final BigInteger P = h("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFF");
    private static final BigInteger A = h("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFC");
    private static final BigInteger B = h("28E9FA9E9D9F5E344D5A9E4BCF6509A7F39789F515AB8F92DDBCBD414D940E93");
    private static final BigInteger N = h("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFF7203DF6B21C6052B53BBF40939D54123");
    private static final BigInteger GX = h("32C4AE2C1F1981195F9904466A39C9948FE30BBFF2660BE1715A4589334C74C7");
    private static final BigInteger GY = h("BC3736A2F4F6779C59BDCEE36B692153D0A9877CC62A474002DF32E52139F0A0");
    private static final ECPoint G = new ECPoint(GX, GY);

    private static final byte[] DEFAULT_ID = "1234567812345678".getBytes(StandardCharsets.UTF_8);
    private static final SecureRandom RNG = new SecureRandom();

    private Sm2Util() {
    }

    /** 生成密钥对:[0]=私钥hex(64), [1]=公钥hex(128, X||Y) */
    public static String[] generateKeyPair() {
        BigInteger d;
        do {
            d = new BigInteger(256, RNG).mod(N.subtract(BigInteger.ONE)).add(BigInteger.ONE);
        } while (d.signum() == 0);
        ECPoint pub = multiply(G, d);
        return new String[]{to32Hex(d), to32Hex(pub.x) + to32Hex(pub.y)};
    }

    /** 用私钥对消息签名,返回 R||S 十六进制(128) */
    public static String sign(String privHex, String message) {
        BigInteger d = h(privHex);
        BigInteger e = new BigInteger(1, eHash(publicOf(d), message.getBytes(StandardCharsets.UTF_8)));
        BigInteger r;
        BigInteger s;
        while (true) {
            BigInteger k = new BigInteger(256, RNG).mod(N.subtract(BigInteger.ONE)).add(BigInteger.ONE);
            ECPoint p1 = multiply(G, k);
            r = e.add(p1.x).mod(N);
            if (r.signum() == 0 || r.add(k).equals(N)) {
                continue;
            }
            BigInteger dInc = d.add(BigInteger.ONE).modInverse(N);
            s = dInc.multiply(k.subtract(r.multiply(d))).mod(N);
            if (s.signum() != 0) {
                break;
            }
        }
        return to32Hex(r) + to32Hex(s);
    }

    /** 用公钥(X||Y hex)验签 */
    public static boolean verify(String pubHex, String message, String signHex) {
        try {
            if (pubHex == null || pubHex.length() != 128 || signHex == null || signHex.length() != 128) {
                return false;
            }
            ECPoint pub = new ECPoint(h(pubHex.substring(0, 64)), h(pubHex.substring(64)));
            BigInteger r = h(signHex.substring(0, 64));
            BigInteger s = h(signHex.substring(64));
            if (r.signum() <= 0 || r.compareTo(N) >= 0 || s.signum() <= 0 || s.compareTo(N) >= 0) {
                return false;
            }
            BigInteger e = new BigInteger(1, eHash(pub, message.getBytes(StandardCharsets.UTF_8)));
            BigInteger t = r.add(s).mod(N);
            if (t.signum() == 0) {
                return false;
            }
            ECPoint p1 = add(multiply(G, s), multiply(pub, t));
            if (p1.infinity) {
                return false;
            }
            return e.add(p1.x).mod(N).equals(r);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** 由私钥(hex)派生公钥 hex(X||Y) */
    public static String publicKeyOf(String privHex) {
        ECPoint pub = multiply(G, h(privHex));
        return to32Hex(pub.x) + to32Hex(pub.y);
    }

    private static ECPoint publicOf(BigInteger d) {
        return multiply(G, d);
    }

    /** e = SM3(ZA || M);ZA = SM3(ENTL||ID||a||b||Gx||Gy||Px||Py) */
    private static byte[] eHash(ECPoint pub, byte[] msg) {
        int entl = DEFAULT_ID.length * 8;
        ByteArrayOutputStream za = new ByteArrayOutputStream();
        za.write((entl >>> 8) & 0xff);
        za.write(entl & 0xff);
        za.writeBytes(DEFAULT_ID);
        za.writeBytes(to32(A));
        za.writeBytes(to32(B));
        za.writeBytes(to32(GX));
        za.writeBytes(to32(GY));
        za.writeBytes(to32(pub.x));
        za.writeBytes(to32(pub.y));
        byte[] zaDigest = Sm3Util.hash(za.toByteArray());

        ByteArrayOutputStream m = new ByteArrayOutputStream();
        m.writeBytes(zaDigest);
        m.writeBytes(msg);
        return Sm3Util.hash(m.toByteArray());
    }

    // ---- 椭圆曲线点运算(仿射坐标,mod p) ----

    private static ECPoint add(ECPoint p1, ECPoint p2) {
        if (p1.infinity) {
            return p2;
        }
        if (p2.infinity) {
            return p1;
        }
        if (p1.x.equals(p2.x)) {
            if (p1.y.equals(p2.y)) {
                return doublePoint(p1);
            }
            return ECPoint.INFINITY;
        }
        BigInteger lambda = p2.y.subtract(p1.y).multiply(p2.x.subtract(p1.x).modInverse(P)).mod(P);
        BigInteger x3 = lambda.multiply(lambda).subtract(p1.x).subtract(p2.x).mod(P);
        BigInteger y3 = lambda.multiply(p1.x.subtract(x3)).subtract(p1.y).mod(P);
        return new ECPoint(x3, y3);
    }

    private static ECPoint doublePoint(ECPoint p) {
        if (p.infinity || p.y.signum() == 0) {
            return ECPoint.INFINITY;
        }
        BigInteger three = BigInteger.valueOf(3);
        BigInteger lambda = p.x.multiply(p.x).multiply(three).add(A)
                .multiply(p.y.shiftLeft(1).modInverse(P)).mod(P);
        BigInteger x3 = lambda.multiply(lambda).subtract(p.x.shiftLeft(1)).mod(P);
        BigInteger y3 = lambda.multiply(p.x.subtract(x3)).subtract(p.y).mod(P);
        return new ECPoint(x3, y3);
    }

    private static ECPoint multiply(ECPoint p, BigInteger k) {
        ECPoint result = ECPoint.INFINITY;
        ECPoint addend = p;
        BigInteger kk = k.mod(N);
        for (int i = 0; i < kk.bitLength(); i++) {
            if (kk.testBit(i)) {
                result = add(result, addend);
            }
            addend = doublePoint(addend);
        }
        return result;
    }

    private static final class ECPoint {
        static final ECPoint INFINITY = new ECPoint();
        final BigInteger x;
        final BigInteger y;
        final boolean infinity;

        private ECPoint() {
            this.x = null;
            this.y = null;
            this.infinity = true;
        }

        ECPoint(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
            this.infinity = false;
        }
    }

    private static BigInteger h(String hex) {
        return new BigInteger(hex, 16);
    }

    private static byte[] to32(BigInteger v) {
        byte[] b = v.toByteArray();
        byte[] out = new byte[32];
        if (b.length > 32) {
            System.arraycopy(b, b.length - 32, out, 0, 32);
        } else {
            System.arraycopy(b, 0, out, 32 - b.length, b.length);
        }
        return out;
    }

    private static String to32Hex(BigInteger v) {
        return Sm4Util.toHex(to32(v));
    }
}
