package com.csg.prm.confirm.system;

import com.csg.prm.common.query.PageQuery;

/** 操作日志查询条件:按操作人/动作/结果/时间范围过滤。 */
public class SysOpLogQuery extends PageQuery {

    private String userName;
    private String action;
    private String result;
    private String createTimeStart;
    private String createTimeEnd;

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getCreateTimeStart() { return createTimeStart; }
    public void setCreateTimeStart(String createTimeStart) { this.createTimeStart = createTimeStart; }
    public String getCreateTimeEnd() { return createTimeEnd; }
    public void setCreateTimeEnd(String createTimeEnd) { this.createTimeEnd = createTimeEnd; }
}
