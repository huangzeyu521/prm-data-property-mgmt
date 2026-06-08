package com.csg.prm.ledger.controller;

import com.csg.prm.common.api.R;
import com.csg.prm.ledger.dto.TodoCenterVO;
import com.csg.prm.ledger.service.TodoCenterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统一待办中心:GET /api/dpr/ledger/todo —— 跨域汇聚确权/授权/风险待办。
 */
@RestController
@RequestMapping("/api/dpr/ledger/todo")
public class TodoCenterController {

    private final TodoCenterService todoCenterService;

    public TodoCenterController(TodoCenterService todoCenterService) {
        this.todoCenterService = todoCenterService;
    }

    @GetMapping
    public R<TodoCenterVO> todos() {
        return R.ok(todoCenterService.todos());
    }
}
