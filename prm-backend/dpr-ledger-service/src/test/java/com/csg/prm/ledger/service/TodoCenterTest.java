package com.csg.prm.ledger.service;

import com.csg.prm.ledger.aggregate.AuthQueryGateway;
import com.csg.prm.ledger.aggregate.ConfirmQueryGateway;
import com.csg.prm.ledger.aggregate.DomainRecord;
import com.csg.prm.ledger.dto.TodoCenterVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 统一待办中心聚合测试(eLink 待办的 Web 实现):跨域确权/授权待办计数自洽。
 * 用 @Primary 桩网关注入跨域待办记录。
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TodoCenterTest.StubConfig.class)
class TodoCenterTest {

    static class StubConfirm implements ConfirmQueryGateway {
        final List<DomainRecord> pending = new ArrayList<>();
        public List<DomainRecord> findByAsset(String assetId) { return new ArrayList<>(); }
        public List<DomainRecord> pending() { return pending; }
    }

    static class StubAuth implements AuthQueryGateway {
        final List<DomainRecord> pending = new ArrayList<>();
        public List<DomainRecord> findByAsset(String assetId) { return new ArrayList<>(); }
        public List<DomainRecord> pending() { return pending; }
    }

    @TestConfiguration
    static class StubConfig {
        @Bean @Primary StubConfirm stubConfirm() { return new StubConfirm(); }
        @Bean @Primary StubAuth stubAuth() { return new StubAuth(); }
    }

    @Autowired private TodoCenterService todoCenterService;
    @Autowired private StubConfirm stubConfirm;
    @Autowired private StubAuth stubAuth;

    private static DomainRecord rec(String domain, String status) {
        DomainRecord r = new DomainRecord();
        r.setDomain(domain);
        r.setStatus(status);
        return r;
    }

    @Test
    void todo_center_aggregates_counts_across_domains() {
        stubConfirm.pending.add(rec("确权", "合规审核中"));
        stubConfirm.pending.add(rec("确权", "经理终审中"));
        stubAuth.pending.add(rec("授权", "业务审核中"));

        TodoCenterVO vo = todoCenterService.todos();

        assertEquals(2, vo.getConfirmCount(), "确权待办数");
        assertEquals(1, vo.getAuthCount(), "授权待办数");
        assertEquals(vo.getConfirmCount() + vo.getAuthCount(), vo.getTotal(),
                "合计应等于各域之和");
    }
}
