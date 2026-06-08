package com.csg.prm.confirm.dto;

import java.util.ArrayList;
import java.util.List;

/** 批量操作结果:总数/成功/失败 + 失败明细。 */
public class BatchResult {

    private int total;
    private int success;
    private int failed;
    private final List<Failure> failures = new ArrayList<>();

    public void ok() {
        success++;
        total++;
    }

    public void fail(String id, String reason) {
        failed++;
        total++;
        failures.add(new Failure(id, reason));
    }

    public int getTotal() {
        return total;
    }

    public int getSuccess() {
        return success;
    }

    public int getFailed() {
        return failed;
    }

    public List<Failure> getFailures() {
        return failures;
    }

    public static class Failure {
        private final String id;
        private final String reason;

        public Failure(String id, String reason) {
            this.id = id;
            this.reason = reason;
        }

        public String getId() {
            return id;
        }

        public String getReason() {
            return reason;
        }
    }
}
