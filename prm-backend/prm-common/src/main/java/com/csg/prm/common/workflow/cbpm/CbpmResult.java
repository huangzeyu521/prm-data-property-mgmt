package com.csg.prm.common.workflow.cbpm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * CBPM/BPS 操作响应信封,对齐规范:{@code {successes, message, errors, totalCounts}}。
 * 规范示例中成功数键为 {@code success},此处用 {@link JsonAlias} 兼容 success/successes 两种写法。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CbpmResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功个数 */
    @JsonAlias({"success", "successes"})
    private int successes;
    /** 接口返回信息 */
    private String message;
    /** 错误个数 */
    @JsonAlias({"error", "errors"})
    private int errors;
    /** 操作总数 */
    private int totalCounts;

    public CbpmResult() {
    }

    public CbpmResult(int successes, String message, int errors, int totalCounts) {
        this.successes = successes;
        this.message = message;
        this.errors = errors;
        this.totalCounts = totalCounts;
    }

    /** 构造一条成功结果。 */
    public static CbpmResult ok(int count) {
        return new CbpmResult(count, "", 0, count);
    }

    /** 全部成功且无错误。 */
    public boolean isOk() {
        return errors == 0 && successes > 0;
    }

    public int getSuccesses() {
        return successes;
    }

    public void setSuccesses(int successes) {
        this.successes = successes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getTotalCounts() {
        return totalCounts;
    }

    public void setTotalCounts(int totalCounts) {
        this.totalCounts = totalCounts;
    }
}
