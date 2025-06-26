package com.hinadt.miaocha.integration.logstash;

import static org.assertj.core.api.Assertions.*;

import com.hinadt.miaocha.TestContainersFactory;
import com.hinadt.miaocha.application.logstash.command.LogstashCommandFactory;
import com.hinadt.miaocha.common.exception.SshException;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.integration.data.IntegrationMySQLTestDataInitializer;
import com.hinadt.miaocha.integration.logstash.support.LogstashMachineTestEnvironment;
import com.hinadt.miaocha.integration.logstash.support.LogstashPackageManager;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Story 1: Logstash测试环境搭建和验证
 *
 * <p>目标：建立可复用的测试基础设施 - 验证SSH容器启动和连接 - 验证MySQL容器连接 - 验证Logstash软件包下载和预置 - 验证基础网络连通性
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Logstash Story 1: Logstash测试环境搭建和验证")
@Order(1)
public class LogstashEnvironmentSetupIntegrationTest {

    @Container @ServiceConnection
    static MySQLContainer<?> mysqlContainer = TestContainersFactory.mysqlContainer();

    @Autowired private LogstashMachineTestEnvironment testEnvironment;
    @Autowired private LogstashPackageManager packageManager;
    @Autowired private SshClient sshClient;
    @Autowired private LogstashCommandFactory commandFactory;
    @Autowired private IntegrationMySQLTestDataInitializer dataInitializer;

    private List<MachineInfo> testMachines;

    @BeforeAll
    void setupTestEnvironment() {
        log.info("=== Logstash Story 1: 开始搭建测试环境 ===");
        dataInitializer.initializeTestData();
        // 创建1台SSH容器用于基础验证
        testEnvironment.startSshContainers(1);
        testMachines = testEnvironment.machines();
        assertThat(testMachines).hasSize(1);

        log.info(
                "测试环境搭建完成 - SSH容器: {}, MySQL容器: {}",
                testMachines.size(),
                mysqlContainer.isRunning());
    }

    @AfterAll
    void cleanupTestEnvironment() {
        log.info("=== Logstash Story 1: 开始清理测试环境 ===");
        dataInitializer.cleanupTestData();
        testEnvironment.cleanup();
        log.info("测试环境清理完成");
    }

    @Test
    @Order(1)
    @DisplayName("验证SSH容器连接")
    void testSshContainerConnection() throws SshException {
        log.info("🔍 测试SSH容器连接");

        MachineInfo machine = testMachines.get(0);

        // 验证SSH连接
        assertThatNoException()
                .isThrownBy(
                        () -> {
                            String result = sshClient.executeCommand(machine, "echo 'SSH连接成功'");
                            assertThat(result.trim()).isEqualTo("SSH连接成功");
                        });

        // 验证基础命令执行
        String hostname = sshClient.executeCommand(machine, "hostname");
        assertThat(hostname).isNotBlank();

        // 验证文件系统操作
        sshClient.executeCommand(machine, "mkdir -p /tmp/test");
        String lsResult = sshClient.executeCommand(machine, "ls  /tmp/");
        assertThat(lsResult).contains("test");

        log.info("✅ SSH容器连接验证通过 - {}:{}", machine.getIp(), machine.getPort());
    }

    @Test
    @Order(1)
    @DisplayName("验证MySQL容器连接")
    void testMysqlContainerConnection() throws Exception {
        log.info("🔍 测试MySQL容器连接");

        // MySQL容器应该已经启动并可连接
        assertThat(mysqlContainer.isRunning()).isTrue();
        assertThat(mysqlContainer.isCreated()).isTrue();

        // 验证数据库连接参数
        String jdbcUrl = mysqlContainer.getJdbcUrl();
        String username = mysqlContainer.getUsername();
        String password = mysqlContainer.getPassword();

        assertThat(jdbcUrl).isNotBlank();
        assertThat(username).isNotBlank();
        assertThat(password).isNotBlank();

        // 真正验证数据库连接
        assertThatNoException()
                .isThrownBy(
                        () -> {
                            try (java.sql.Connection conn =
                                    java.sql.DriverManager.getConnection(
                                            jdbcUrl, username, password)) {
                                // 验证连接有效性
                                assertThat(conn.isValid(5)).isTrue();

                                // 执行简单查询验证数据库功能
                                try (java.sql.Statement stmt = conn.createStatement();
                                        java.sql.ResultSet rs =
                                                stmt.executeQuery("SELECT 1 as test_value")) {
                                    assertThat(rs.next()).isTrue();
                                    assertThat(rs.getInt("test_value")).isEqualTo(1);
                                }

                                // 验证数据库名称
                                String actualDatabaseName = conn.getCatalog();
                                assertThat(actualDatabaseName).isNotBlank();

                                // 验证数据库版本信息
                                String databaseProductName =
                                        conn.getMetaData().getDatabaseProductName();
                                String databaseProductVersion =
                                        conn.getMetaData().getDatabaseProductVersion();
                                assertThat(databaseProductName).containsIgnoringCase("mysql");
                                assertThat(databaseProductVersion).isNotBlank();

                                log.info(
                                        "数据库产品: {}, 版本: {}, 数据库名: {}",
                                        databaseProductName,
                                        databaseProductVersion,
                                        actualDatabaseName);
                            }
                        });

        log.info("✅ MySQL容器连接验证通过 - {} (真实连接测试成功)", jdbcUrl);
    }

    @Test
    @Order(2)
    @DisplayName("验证Logstash软件包下载和预置")
    void testLogstashPackagePreparation() throws SshException {
        log.info("🔍 测试Logstash软件包准备");

        MachineInfo machine = testMachines.get(0);

        // 下载并验证Logstash包
        assertThatNoException()
                .isThrownBy(
                        () -> {
                            packageManager.ensureLogstashPackageAvailable();
                        });

        // 上传包到测试容器
        assertThatNoException()
                .isThrownBy(
                        () -> {
                            packageManager.uploadLogstashPackageToMachine(machine);
                        });

        // 验证包已上传到容器
        String lsResult = sshClient.executeCommand(machine, "ls -la /tmp/");
        assertThat(lsResult).containsPattern("logstash-.*\\.tar\\.gz");

        // 验证包完整性（检查文件大小）
        String sizeResult =
                sshClient.executeCommand(
                        machine, "ls -lh /tmp/logstash-*.tar.gz | awk '{print $5}'");
        assertThat(sizeResult.trim()).matches("\\d+[KMG]?"); // 应该有合理的文件大小

        log.info("✅ Logstash软件包准备验证通过");
    }
}
