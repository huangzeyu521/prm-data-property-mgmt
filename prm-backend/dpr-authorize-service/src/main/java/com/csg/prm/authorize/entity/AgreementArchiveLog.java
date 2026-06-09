package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 授权协议存档审计日志(可研 3.2.2.1.1.3.3.4:归档/查看/下载 等访问留痕,可追溯)。
 * 对应物理表 IM_AUTH_ARCHIVE_LOG。
 */
@TableName("IM_AUTH_ARCHIVE_LOG")
public class AgreementArchiveLog extends BaseEntity {

    @TableId(value = "CEC_LOG_ID", type = IdType.ASSIGN_UUID)
    private String logId;

    @TableField("CEC_AGREEMENT_ID")
    private String agreementId;

    @TableField("CEC_AGREEMENT_NO")
    private String agreementNo;

    /** 操作:归档/查看/下载 */
    @TableField("CEC_ACTION")
    private String action;

    @TableField("CEC_OPERATOR")
    private String operator;

    @TableField("CEC_OPERATE_TIME")
    private LocalDateTime operateTime;

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getAgreementId() { return agreementId; }
    public void setAgreementId(String agreementId) { this.agreementId = agreementId; }
    public String getAgreementNo() { return agreementNo; }
    public void setAgreementNo(String agreementNo) { this.agreementNo = agreementNo; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getOperateTime() { return operateTime; }
    public void setOperateTime(LocalDateTime operateTime) { this.operateTime = operateTime; }
}
