package com.csg.prm.ledger.dto;

import com.csg.prm.ledger.aggregate.DomainRecord;

import java.io.Serializable;
import java.util.List;

/**
 * 统一待办中心:跨域汇聚待办(确权审批 / 授权审批),
 * 让用户"一处看全待办、一键直达办理",对齐工作指引"一次都不跑/主动协同"。
 */
public class TodoCenterVO implements Serializable {

    private List<DomainRecord> confirmTodos;
    private List<DomainRecord> authTodos;

    private int confirmCount;
    private int authCount;
    private int total;

    public List<DomainRecord> getConfirmTodos() {
        return confirmTodos;
    }

    public void setConfirmTodos(List<DomainRecord> confirmTodos) {
        this.confirmTodos = confirmTodos;
    }

    public List<DomainRecord> getAuthTodos() {
        return authTodos;
    }

    public void setAuthTodos(List<DomainRecord> authTodos) {
        this.authTodos = authTodos;
    }

    public int getConfirmCount() {
        return confirmCount;
    }

    public void setConfirmCount(int confirmCount) {
        this.confirmCount = confirmCount;
    }

    public int getAuthCount() {
        return authCount;
    }

    public void setAuthCount(int authCount) {
        this.authCount = authCount;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
