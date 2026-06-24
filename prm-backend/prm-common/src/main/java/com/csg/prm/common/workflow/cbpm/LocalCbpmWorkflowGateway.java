package com.csg.prm.common.workflow.cbpm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * CBPM/BPS 流程组件网关本地桩(默认):仅记录操作意图,不跨内网调用流程引擎。
 * 用于离线/单测(PRM 不依赖南网内网 BPS 引擎);生产/联机由 {@link HttpCbpmWorkflowGateway} @Primary 覆盖。
 */
@Component
public class LocalCbpmWorkflowGateway implements CbpmWorkflowGateway {

    private static final Logger log = LoggerFactory.getLogger(LocalCbpmWorkflowGateway.class);

    @Override
    public CbpmResult submit(CbpmSubmitRequest request) {
        log.info("[CBPM-本地桩] 操作 operateType={} workId={} taskId={} 门面={} (未启用真实引擎)",
                request.getOperateType(), request.getWorkId(), request.getTaskId(),
                request.getWorkflowFacadeName());
        return CbpmResult.ok(1);
    }

    @Override
    public CbpmResult batchSubmit(List<CbpmSubmitRequest> requests) {
        int n = requests == null ? 0 : requests.size();
        log.info("[CBPM-本地桩] 批量操作 共 {} 条 (未启用真实引擎)", n);
        return CbpmResult.ok(n);
    }

    @Override
    public List<CbpmWorkItem> queryTodo(CbpmTodoQuery query) {
        log.debug("[CBPM-本地桩] 查询待办 transActor={} processId={} (返回空)",
                query == null ? null : query.getTransActor(),
                query == null ? null : query.getProcessId());
        return Collections.emptyList();
    }

    @Override
    public long countTodo(CbpmTodoQuery query) {
        return 0L;
    }

    @Override
    public List<CbpmWorkItem> queryDone(CbpmTodoQuery query) {
        return Collections.emptyList();
    }
}
