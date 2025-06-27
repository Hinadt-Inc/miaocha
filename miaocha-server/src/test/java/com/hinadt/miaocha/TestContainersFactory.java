package com.hinadt.miaocha;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers 工厂类
 *
 * <p>提供 MySQL 数据库容器的创建和初始化操作
 */
@Slf4j
public class TestContainersFactory {

    // 共享网络，用于容器间通信
    public static final Network SHARED_NETWORK = Network.newNetwork();

    // MySQL 容器配置
    private static final String MYSQL_IMAGE = "mysql:8.0";
    private static final String TEST_DATABASE = "miaocha_integration_test";
    private static final String TEST_USERNAME = "test_user";
    private static final String TEST_PASSWORD = "test_password";

    // SSH 容器配置
    private static final String SSH_IMAGE = "wataken44/ubuntu-latest-sshd:latest";
    private static final String SSH_USERNAME = "ubuntu";
    private static final String SSH_PASSWORD = "ubuntu";
    private static final int SSH_CONTAINER_PORT = 22;

    // Doris 容器配置
    private static final String DORIS_IMAGE = "apache/doris:2.1.9-all";

    /** MySQL 测试容器 - 全局唯一 提供真实的 MySQL 环境，存储机器信息、进程配置等数据 */
    public static MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(MYSQL_IMAGE)
                .withDatabaseName(TEST_DATABASE)
                .withUsername(TEST_USERNAME)
                .withPassword(TEST_PASSWORD)
                .withNetwork(SHARED_NETWORK)
                .withNetworkAliases("mysql-db")
                .withStartupTimeout(Duration.ofMinutes(2));
    }

    /**
     * Doris 测试容器 - 提供真实的 Doris 环境，用于日志搜索集成测试
     *
     * <p>配置说明： - 暴露 9030 端口用于 MySQL 协议连接（FE 查询端口） - 暴露 8030 端口用于 HTTP 协议连接（FE Web 端口） -
     * 默认用户名：root，密码：空 - 启动超时设置为5分钟，因为 Doris 需要较长时间初始化
     */
    public static GenericContainer<?> dorisContainer() {
        return new GenericContainer<>(DORIS_IMAGE)
                .withNetwork(SHARED_NETWORK)
                .withNetworkAliases("doris-fe")
                .withExposedPorts(9030, 9060, 8030, 8040) // 9030: MySQL协议端口, 8030: HTTP端口
                .withEnv("TZ", "Asia/Shanghai") // 设置时区
                .withStartupTimeout(Duration.ofMinutes(5))
                .waitingFor(
                        Wait.forListeningPorts(9030, 9060, 8030, 8040)
                                .withStartupTimeout(Duration.ofMinutes(5)));
    }

    /** SSH 机器容器管理器 根据测试场景动态创建所需数量的机器容器 */
    public static class SshMachineContainerManager {

        private static final List<GenericContainer<?>> runningContainers = new ArrayList<>();

        /**
         * 创建指定数量的 SSH 机器容器（并行创建）
         *
         * @param machineCount 需要的机器数量
         * @return SSH 机器容器列表
         */
        public static List<GenericContainer<?>> createAndStartSshMachines(int machineCount) {
            log.info("并行创建 {} 个 SSH 机器容器用于测试", machineCount);

            // 并行创建容器的 CompletableFuture 列表
            List<CompletableFuture<GenericContainer<?>>> containerFutures =
                    IntStream.range(0, machineCount)
                            .mapToObj(
                                    i ->
                                            CompletableFuture.<GenericContainer<?>>supplyAsync(
                                                    () -> createAndStartSingleSshContainer(i + 1)))
                            .toList();

            // 等待所有容器创建完成并收集结果
            List<GenericContainer<?>> containers =
                    containerFutures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());

            // 将所有容器添加到运行容器列表中
            runningContainers.addAll(containers);

            log.info("成功并行创建并启动了 {} 个 SSH 机器容器", containers.size());
            return containers;
        }

        /**
         * 创建单个 SSH 容器
         *
         * @param machineIndex 机器索引（从1开始）
         * @return 已启动的 SSH 容器
         */
        private static GenericContainer<?> createAndStartSingleSshContainer(int machineIndex) {
            GenericContainer<?> sshContainer =
                    new GenericContainer<>(DockerImageName.parse(SSH_IMAGE))
                            .withEnv("TZ", "Asia/Shanghai")
                            .withExposedPorts(SSH_CONTAINER_PORT)
                            .withNetwork(SHARED_NETWORK)
                            .withNetworkAliases("ssh-machine-" + machineIndex)
                            .withStartupTimeout(Duration.ofMinutes(3))
                            .waitingFor(Wait.forListeningPorts(SSH_CONTAINER_PORT));

            sshContainer.start();
            try {
                sshContainer.execInContainer(
                        "/bin/bash",
                        "-c",
                        "echo '" + SSH_USERNAME + ":" + SSH_PASSWORD + "' | chpasswd");
                sshContainer.execInContainer("/bin/bash", "-c", "chmod -R 777 /opt");
            } catch (IOException | InterruptedException e) {
                log.error("SSH容器 ssh-machine-" + machineIndex + " 密码设置失败: {}", e.getMessage());
            }

            log.info(
                    "SSH 机器容器 {} 已启动: {}:{}",
                    machineIndex,
                    sshContainer.getHost(),
                    sshContainer.getMappedPort(SSH_CONTAINER_PORT));

            return sshContainer;
        }

        /** 获取 SSH 容器的连接信息 */
        public static SshConnectionInfo getConnectionInfo(GenericContainer<?> container) {
            return new SshConnectionInfo(
                    container.getHost(),
                    container.getMappedPort(SSH_CONTAINER_PORT),
                    SSH_USERNAME,
                    SSH_PASSWORD);
        }

        /** 清理所有 SSH 容器（并行清理） */
        public static void cleanupAllContainers() {
            if (runningContainers.isEmpty()) {
                log.info("没有需要清理的 SSH 机器容器");
                return;
            }

            log.info("并行清理 {} 个 SSH 机器容器", runningContainers.size());

            // 并行停止所有容器
            // 等待所有清理操作完成
            CompletableFuture.allOf(
                            runningContainers.stream()
                                    .map(
                                            container ->
                                                    CompletableFuture.runAsync(
                                                            () -> {
                                                                try {
                                                                    String containerName =
                                                                            container
                                                                                    .getContainerName();
                                                                    container.stop();
                                                                    log.debug(
                                                                            "容器已停止: {}",
                                                                            containerName);
                                                                } catch (Exception e) {
                                                                    log.warn(
                                                                            "停止容器时出错: {}",
                                                                            e.getMessage());
                                                                }
                                                            }))
                                    .toArray(CompletableFuture[]::new))
                    .join();

            runningContainers.clear();
            log.info("所有 SSH 机器容器清理完成");
        }

        /** SSH 连接信息 */
        public record SshConnectionInfo(String host, int port, String username, String password) {}
    }
}
