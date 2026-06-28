package com.csg.prm.confirm.integration.dto;

import java.util.List;

/**
 * 数据资产目录全树节点(非懒加载,供「数据资产确权目录管理」整树渲染 + 计数/筛选)。
 * 层级对齐平台:业务域(domain)→ 系统(system)→ 一级功能模块(module)→ 库表卡片(card)。
 *
 * @param id        节点唯一标识(card 叶子=表代码)
 * @param label     显示名
 * @param type      domain | system | module | card
 * @param sysName   card 叶子所属系统(供卡片明细查询);非叶为空
 * @param tableCode card 叶子表代码;非叶为空
 * @param confirmed card 叶子是否已确权
 * @param authorized card 叶子是否已对外授权
 * @param children  子节点(card 叶子为空列表)
 */
public record CatalogTreeNode(
        String id,
        String label,
        String type,
        String sysName,
        String tableCode,
        boolean confirmed,
        boolean authorized,
        List<CatalogTreeNode> children
) {
}
