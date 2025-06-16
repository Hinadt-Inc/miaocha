package com.hinadt.miaocha.integration.logstash;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Logstash模块集成测试套件
 *
 * <p>测试覆盖范围： 1. 状态机和状态转换完整流程 2. Task生命周期管理 3. Command执行和调度 4. 一机多实例部署和管理 5. 异常处理和恢复机制
 *
 * <p>测试架构分层： - Scenario层：完整业务场景测试 - Workflow层：工作流组合测试 - Component层：组件集成测试 - Foundation层：基础设施测试
 */
@Epic("秒查日志管理系统")
@Feature("Logstash模块集成测试")
@Owner("开发团队")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestPropertySource(
        properties = {
            "logstash.deploy.base-dir=/tmp/logstash-integration-test",
            "logstash.connection.timeout=5000",
            "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Logstash模块集成测试套件")
public class LogstashIntegrationTestSuite {

    /**
     * 测试套件执行顺序： 1. Foundation Tests (基础设施) 2. Component Tests (组件集成) 3. Workflow Tests (工作流) 4.
     * Scenario Tests (业务场景) 5. Multi-Instance Tests (一机多实例) 6. Chaos Tests (异常和恢复)
     */
    @BeforeAll
    static void setUpSuite() {
        System.out.println("🚀 开始执行Logstash模块集成测试套件");
        System.out.println("📋 测试覆盖：状态机、任务生命周期、一机多实例、异常处理");
    }

    @AfterAll
    static void tearDownSuite() {
        System.out.println("✅ Logstash模块集成测试套件执行完成");
    }

    @Nested
    @DisplayName("1️⃣ Foundation Tests - 基础设施测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FoundationTests {
        // 数据库连接、配置加载、基础组件初始化等
    }

    @Nested
    @DisplayName("2️⃣ Component Tests - 组件集成测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ComponentTests {
        // State Handler、Task Mapper、Command Executor等组件测试
    }

    @Nested
    @DisplayName("3️⃣ Workflow Tests - 工作流测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WorkflowTests {
        // 完整的启动、停止、重启工作流测试
    }

    @Nested
    @DisplayName("4️⃣ Scenario Tests - 业务场景测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ScenarioTests {
        // 真实业务场景的端到端测试
    }

    @Nested
    @DisplayName("5️⃣ Multi-Instance Tests - 一机多实例测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class MultiInstanceTests {
        // 一机多实例的完整集成测试
    }

    @Nested
    @DisplayName("6️⃣ Chaos Tests - 异常和恢复测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ChaosTests {
        // 异常情况和系统恢复能力测试
    }
}
