package com.csg.prm.common.evidence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 区块链存证记录(全节点留痕上链:确权制卡/授权发证/协议存档/熔断处置/风险闭环等)。
 * 以国密 SM3 计算业务数据指纹并锚定上链,实现权益可追溯、防篡改。对应物理表 IM_DPR_EVIDENCE。
 */
@TableName("IM_DPR_EVIDENCE")
public class ChainEvidence extends BaseEntity {

    public static final String STATUS_ANCHORED = "已上链";
    public static final String STATUS_PENDING = "待上链";

    @TableId(value = "CEC_EVIDENCE_ID", type = IdType.ASSIGN_UUID)
    private String evidenceId;

    /** 业务节点类型:确权制卡/授权发证/协议存档/熔断处置/风险处置闭环 */
    @TableField("CEC_BIZ_TYPE")
    private String bizType;

    /** 业务主键(卡片ID/证书ID/协议ID/风险ID...) */
    @TableField("CEC_BIZ_ID")
    private String bizId;

    /** 摘要(人可读) */
    @TableField("CEC_SUMMARY")
    private String summary;

    /** 业务数据 SM3 指纹(64 位十六进制) */
    @TableField("CEC_SM3_HASH")
    private String sm3Hash;

    /** 上链交易哈希 */
    @TableField("CEC_CHAIN_TX_HASH")
    private String chainTxHash;

    /** 区块高度 */
    @TableField("CEC_BLOCK_HEIGHT")
    private Long blockHeight;

    /** 存证状态:待上链/已上链 */
    @TableField("CEC_ANCHOR_STATUS")
    private String anchorStatus;

    /** 上链时间 */
    @TableField("CEC_EVIDENCE_TIME")
    private LocalDateTime evidenceTime;

    public String getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(String evidenceId) {
        this.evidenceId = evidenceId;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSm3Hash() {
        return sm3Hash;
    }

    public void setSm3Hash(String sm3Hash) {
        this.sm3Hash = sm3Hash;
    }

    public String getChainTxHash() {
        return chainTxHash;
    }

    public void setChainTxHash(String chainTxHash) {
        this.chainTxHash = chainTxHash;
    }

    public Long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(Long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public String getAnchorStatus() {
        return anchorStatus;
    }

    public void setAnchorStatus(String anchorStatus) {
        this.anchorStatus = anchorStatus;
    }

    public LocalDateTime getEvidenceTime() {
        return evidenceTime;
    }

    public void setEvidenceTime(LocalDateTime evidenceTime) {
        this.evidenceTime = evidenceTime;
    }
}
