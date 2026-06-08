package com.csg.prm.ledger.service;

import com.csg.prm.ledger.dto.TodoCenterVO;

/**
 * 统一待办中心服务:聚合确权审批 / 授权审批 / 风险处置待办。
 */
public interface TodoCenterService {

    TodoCenterVO todos();
}
