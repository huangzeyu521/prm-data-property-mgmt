package com.csg.prm.common.evidence;

/**
 * 区块链上链网关。将数据指纹(SM3)锚定到联盟链并返回回执。
 * 生产环境由 Feign/SDK 实现(如长安链/FISCO BCOS)+ @Primary 覆盖;
 * 本地/测试用 {@link LocalBlockchainGateway} 桩实现(确定性回执,便于校验)。
 */
public interface BlockchainGateway {

    /** 将 SM3 指纹锚定上链,返回交易哈希与区块高度 */
    ChainReceipt anchor(String sm3Hash);
}
