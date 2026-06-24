package com.csg.prm.common.evidence;

import com.csg.prm.common.api.PageResult;

import java.util.List;

/**
 * 区块链存证服务(国密 SM3 指纹 + 上链回执)。
 * 各业务服务在关键节点(确权制卡/授权发证/协议存档/熔断处置/风险闭环)调用 {@link #anchor} 留痕。
 */
public interface ChainEvidenceService {

    /**
     * 计算业务数据 SM3 指纹并锚定上链,落库存证记录。
     * @param bizType 业务节点类型
     * @param bizId   业务主键
     * @param summary 人可读摘要
     * @param payload 参与指纹计算的业务数据(顺序拼接的关键字段)
     * @return 存证记录ID
     */
    String anchor(String bizType, String bizId, String summary, String payload);

    /**
     * 同 {@link #anchor(String, String, String, String)},但显式带归口网级(省/地市码),
     * 用于确权制卡/授权发证等按"申报组织"回填存证的 province_code/bureau_code(否则随用户上下文,可能为空)。
     * 省/地市码为空时回退到公共字段自动填充(用户上下文)。
     */
    String anchor(String bizType, String bizId, String summary, String payload,
                  String provinceCode, String bureauCode);

    /**
     * 校验:用当前业务数据重算 SM3,与存证记录指纹比对(防篡改验真)。
     * @return true 表示数据与上链指纹一致
     */
    boolean verify(String evidenceId, String payload);

    ChainEvidence getById(String evidenceId);

    List<ChainEvidence> listByBiz(String bizId);

    PageResult<ChainEvidence> page(long current, long size, String bizType, String bizId);
}
