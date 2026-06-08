package com.csg.prm.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 上下文装配验证:确认整套(Web + MyBatis-Plus + SCA 自动配置)在离线 profile 下可正常启动。
 */
@SpringBootTest
@ActiveProfiles("test")
class LedgerApplicationTests {

    @Test
    void contextLoads() {
    }
}
