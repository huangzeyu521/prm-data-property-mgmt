package com.csg.prm.common.evidence;

/**
 * 区块链上链回执:交易哈希与区块高度。
 */
public class ChainReceipt {

    private final String txHash;
    private final long blockHeight;

    public ChainReceipt(String txHash, long blockHeight) {
        this.txHash = txHash;
        this.blockHeight = blockHeight;
    }

    public String getTxHash() {
        return txHash;
    }

    public long getBlockHeight() {
        return blockHeight;
    }
}
