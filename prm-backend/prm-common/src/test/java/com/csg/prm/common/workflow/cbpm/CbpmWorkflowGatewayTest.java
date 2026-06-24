package com.csg.prm.common.workflow.cbpm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 南网 CBPM/BPS 流程组件对接层单元测试:验证请求工厂遵循规范(operateType/必填项)、
 * 响应信封兼容 success/successes、本地桩离线可用。
 */
class CbpmWorkflowGatewayTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final LocalCbpmWorkflowGateway local = new LocalCbpmWorkflowGateway();

    @Test
    void submitRequest_factories_carryCorrectOperateTypeAndRequiredFields() {
        Map<String, String> jadp = Map.of("dapDemo", "dapDemo1");

        CbpmSubmitRequest report = CbpmSubmitRequest.report("1", "wfFacade", jadp,
                List.of(WFParticipant.person("SuperAdmin", "SuperAdmin")));
        assertEquals(CbpmOperateType.REPORT, report.getOperateType());
        assertEquals("1", report.getWorkId());
        assertEquals("wfFacade", report.getWorkflowFacadeName());
        assertEquals("person", report.getNextParticipantList().get(0).getTypeCode());

        CbpmSubmitRequest dispatch = CbpmSubmitRequest.dispatch("1", "wfFacade", "5563", "通过", jadp, null);
        assertEquals(CbpmOperateType.DISPATCH, dispatch.getOperateType());
        assertEquals("5563", dispatch.getTaskId());

        CbpmSubmitRequest back = CbpmSubmitRequest.back("1", "wfFacade", "5588", "回退", jadp, null);
        assertEquals(CbpmOperateType.BACK, back.getOperateType());

        CbpmSubmitRequest terminate = CbpmSubmitRequest.terminate("1", "wfFacade", "4209", "终止", jadp);
        assertEquals(CbpmOperateType.TERMINATE, terminate.getOperateType());
        assertEquals("4209", terminate.getProcessInsId());
    }

    @Test
    void submitRequest_serializesToSpecShape() throws Exception {
        CbpmSubmitRequest r = CbpmSubmitRequest.dispatch("1", "wfFacade", "5563", "通过", Map.of("dapDemo", "dapDemo1"),
                List.of(WFParticipant.person("SuperAdmin", "SuperAdmin")));
        String json = mapper.writeValueAsString(r);
        // 规范字段名:workId / workflowFacadeName / operateType / taskId / jadpTodoIds / nextParticipantList
        assertTrue(json.contains("\"operateType\":2"), json);
        assertTrue(json.contains("\"workflowFacadeName\":\"wfFacade\""), json);
        assertTrue(json.contains("\"typeCode\":\"person\""), json);
    }

    @Test
    void result_envelope_acceptsSuccessAndSuccessesKeys() throws Exception {
        // 规范响应示例用 "success";参数表用 "successes" —— 两者都应可解析
        CbpmResult a = mapper.readValue("{\"success\":1,\"message\":\"\",\"errors\":0,\"totalCounts\":1}", CbpmResult.class);
        assertEquals(1, a.getSuccesses());
        assertTrue(a.isOk());

        CbpmResult b = mapper.readValue("{\"successes\":2,\"message\":\"\",\"errors\":0,\"totalCounts\":2}", CbpmResult.class);
        assertEquals(2, b.getSuccesses());
        assertTrue(b.isOk());
    }

    @Test
    void localStub_isOfflineUsable() {
        CbpmResult r = local.submit(CbpmSubmitRequest.report("1", "wfFacade", Map.of("d", "d1"), null));
        assertTrue(r.isOk());

        CbpmResult batch = local.batchSubmit(List.of(
                CbpmSubmitRequest.report("1", "wfFacade", Map.of("d", "d1"), null),
                CbpmSubmitRequest.report("2", "wfFacade", Map.of("d", "d1"), null)));
        assertEquals(2, batch.getSuccesses());

        assertNotNull(local.queryTodo(new CbpmTodoQuery("SuperAdmin")));
        assertTrue(local.queryTodo(new CbpmTodoQuery("SuperAdmin")).isEmpty());
        assertEquals(0L, local.countTodo(new CbpmTodoQuery("SuperAdmin")));
    }
}
