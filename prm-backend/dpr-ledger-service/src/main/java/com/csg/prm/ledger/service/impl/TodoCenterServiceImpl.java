package com.csg.prm.ledger.service.impl;

import com.csg.prm.ledger.aggregate.AuthQueryGateway;
import com.csg.prm.ledger.aggregate.ConfirmQueryGateway;
import com.csg.prm.ledger.aggregate.DomainRecord;
import com.csg.prm.ledger.dto.TodoCenterVO;
import com.csg.prm.ledger.service.TodoCenterService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 统一待办中心实现。确权/授权待办经只读网关跨服务获取,统一映射为中性记录
 * 便于前端一致展示与跳转。
 */
@Service
public class TodoCenterServiceImpl implements TodoCenterService {

    private final ConfirmQueryGateway confirmGateway;
    private final AuthQueryGateway authGateway;

    public TodoCenterServiceImpl(ConfirmQueryGateway confirmGateway,
                                 AuthQueryGateway authGateway) {
        this.confirmGateway = confirmGateway;
        this.authGateway = authGateway;
    }

    @Override
    public TodoCenterVO todos() {
        List<DomainRecord> confirmTodos = confirmGateway.pending();
        List<DomainRecord> authTodos = authGateway.pending();

        TodoCenterVO vo = new TodoCenterVO();
        vo.setConfirmTodos(confirmTodos);
        vo.setAuthTodos(authTodos);
        vo.setConfirmCount(confirmTodos.size());
        vo.setAuthCount(authTodos.size());
        vo.setTotal(confirmTodos.size() + authTodos.size());
        return vo;
    }
}
