package com.csg.prm.common.evidence;

import com.csg.prm.common.crypto.Sm3Util;
import org.springframework.stereotype.Component;

/**
 * 区块链上链网关本地桩。无联盟链依赖时生成确定性回执(由 SM3 指纹派生),
 * 保证可复现、可校验。生产环境另以 SDK/Feign 实现 + @Primary 覆盖真正上链。
 */
@Component
public class LocalBlockchainGateway implements BlockchainGateway {

    @Override
    public ChainReceipt anchor(String sm3Hash) {
        // 交易哈希:对指纹再次 SM3 派生(确定性),前缀 0x
        String tx = "0x" + Sm3Util.hashHex("TX|" + sm3Hash);
        // 区块高度:由指纹前 6 位十六进制派生为稳定正整数(模拟链高)
        long height = Long.parseLong(sm3Hash.substring(0, 6), 16) + 1_000_000L;
        return new ChainReceipt(tx, height);
    }
}
