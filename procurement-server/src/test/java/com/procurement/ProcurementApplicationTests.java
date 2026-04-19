package com.procurement;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("需要数据库连接 — 仅在集成测试环境中运行")
class ProcurementApplicationTests {

    @Test
    void contextLoads() {
    }
}
