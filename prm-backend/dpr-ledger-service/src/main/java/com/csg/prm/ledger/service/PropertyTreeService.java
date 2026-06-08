package com.csg.prm.ledger.service;

import com.csg.prm.ledger.dto.PropertyTreeNode;

import java.util.List;

/**
 * 产权树服务:按"子公司—系统—模式—数据集"构建层级,并叠加确权状态。
 */
public interface PropertyTreeService {

    List<PropertyTreeNode> buildTree();
}
