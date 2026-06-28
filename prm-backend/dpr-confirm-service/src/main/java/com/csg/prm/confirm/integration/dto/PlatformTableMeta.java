package com.csg.prm.confirm.integration.dto;

/**
 * 平台元数据库表(供确权"选卡片→自动带库表清单"用)。
 * 字段来源(平台接入后映射):
 * - instanceName/schemaName/tableCode/tableName/tableComment/secretLevel ← TW_DATA_CARD(数据资产卡片,一卡≈一库表)
 * - sourceType(A-F)/sourceSubject ← AU_TABLE_META_DATA(产权元数据表信息,已采集则带出,未采集为 null 待补全)
 * - gFlag/hFlag/iFlag/jFlag ← AU_TABLE_META_DATA.IS_CHECK/IS_PRIVACY/IS_BUS_SECRET/IS_EQUITY(G/H/I/J 信息关联识别)
 * - existTable ← TW_DATA_CARD.IS_EXIST_TABLE(不存在库表的不允许确权)
 * - *Attachment ← AU_TABLE_META_DATA.SOURCE_NAME/CHECK_NAME/PRIVACY_NAME/BUS_SECRET_NAME/EQUITY_NAME
 *   (平台元数据已上传材料的附件名;供确权"先从平台同步已上传材料、再补全"——命中则免上传)
 */
public record PlatformTableMeta(
        String instanceName,   // 实例/TNS
        String schemaName,     // 模式/schema
        String tableCode,      // 表代码
        String tableName,      // 表名称
        String tableComment,   // 表注释
        String secretLevel,    // 密级
        String sourceType,     // 数据来源判定(A-F),平台元数据已采集则带出
        String sourceSubject,  // 来源主体名称
        boolean gFlag,         // G 涉行政监管(IS_CHECK)
        boolean hFlag,         // H 涉个人/家庭隐私(IS_PRIVACY)
        boolean iFlag,         // I 涉第三方商业机密(IS_BUS_SECRET)
        boolean jFlag,         // J 其他数据权益约束协议(IS_EQUITY)
        boolean existTable,    // 是否存在库表(false=不可确权)
        // 以下为平台已上传材料附件名(AU_TABLE_META_DATA),未采集为 null
        String sourceAttachment,    // SOURCE_NAME 来源/权属证明材料附件(对应 A–F + 证明材料)
        String checkAttachment,     // CHECK_NAME  行政监管要求附件(对应 G)
        String privacyAttachment,   // PRIVACY_NAME 个人/家庭隐私授权附件(对应 H)
        String busSecretAttachment, // BUS_SECRET_NAME 第三方商业机密附件(对应 I)
        String equityAttachment,    // EQUITY_NAME 其他数据权益约束协议附件(对应 J)
        // 以下为逐表"表2"明细(AU_TABLE_META_DATA),平台已采集则带出供申报人核实修正
        String sourceDesc,          // SOURCE_DESC 来源说明/来源权益限制摘要
        String gSubject,            // CHECK_DESC 行政监管(G)信息识别关联主体说明
        String hSubject,            // PRIVACY_DESC 个人/家庭隐私(H)信息识别关联主体说明
        String iSubject,            // BUS_SECRET_DESC 第三方商业机密(I)信息识别关联主体说明
        String jSubject,            // EQUITY_DESC 其他协议(J)信息识别关联主体说明
        String authTime,            // AUTH_TIME 确权时间(已确权才有)
        // 系统责任信息(TW_DATA_CARD,表1 系统负责人/联系方式;系统内各卡片一致)
        String mgtUser,             // MGT_USER 卡片责任人(系统负责人)
        String mgtUserPhone,        // MGT_USER_PHONE 责任人电话(联系方式)
        // 申报主体(TW_DATA_CARD.MGT_UNIT/MGT_MNG_DEPT + SYS_ORGANIZATION.ORG_TYPE 单位层级)
        String mgtUnit,             // MGT_UNIT 管理单位(表1 公司主体/申报权属主体)
        String mgtDept,             // MGT_MNG_DEPT 管理部门(责任部门)
        String subjectLevel,        // SYS_ORGANIZATION.ORG_TYPE 单位层级:公司总部/分省公司/专业子公司
        // 逐表对外授权状态(dpr-authorize 反查;确权变更据此精确判断影响哪条授权)
        boolean authorized,         // 该库表是否已对外授权
        String authId,              // 授权号
        String authScope,           // 授权范围摘要
        String authStatus           // 授权状态(有效/即将到期/已暂停 等)
) {
}
