package com.hinadt.miaocha.integration.logstash.support;

import com.hinadt.miaocha.TestContainersFactory;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.infrastructure.mapper.MachineMapper;
import com.hinadt.miaocha.integration.TestMachineFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.GenericContainer;

/**
 * Logstash容器环境管理器
 *
 * <p>职责： 1. 专门管理SSH容器的生命周期 2. 提供容器状态查询和健康检查 3. 可选地提供容器对应的MachineInfo实体 4. 与业务实体解耦，专注容器管理
 *
 * <p>设计原则： - 容器优先：主要职责是管理实际的SSH容器 - 可选转换：可以选择性地提供MachineInfo，但不强制 - 状态透明：提供容器运行状态的查询接口
 */
@Slf4j
@Component
public class LogstashMachineTestEnvironment {

    @Autowired private TestMachineFactory machineFactory;
    @Autowired private MachineMapper machineMapper;

    private final List<GenericContainer<?>> sshContainers = new ArrayList<>();

    /**
     * 启动指定数量的SSH容器
     *
     * @param count 容器数量
     * @return 启动的容器列表
     */
    public List<GenericContainer<?>> startSshContainers(int count) {
        log.info("启动 {} 个SSH容器用于Logstash测试", count);

        List<GenericContainer<?>> containers =
                TestContainersFactory.SshMachineContainerManager.createAndStartSshMachines(count);

        sshContainers.addAll(containers);

        log.info("成功启动 {} 个SSH容器", containers.size());
        return new ArrayList<>(containers);
    }

    /**
     * 获取机器信息列表
     *
     * @return 机器信息列表
     */
    public List<MachineInfo> machines() {
        List<MachineInfo> machines = new ArrayList<>();

        for (int i = 0; i < sshContainers.size(); i++) {
            GenericContainer<?> container = sshContainers.get(i);

            if (container.isRunning()) {
                TestContainersFactory.SshMachineContainerManager.SshConnectionInfo connectionInfo =
                        TestContainersFactory.SshMachineContainerManager.getConnectionInfo(
                                container);

                MachineInfo machine =
                        machineFactory.forContainer(
                                connectionInfo.host(),
                                connectionInfo.port(),
                                connectionInfo.username(),
                                connectionInfo.password(),
                                i + 1);

                machines.add(machine);
            }
        }

        return machines;
    }

    /**
     * 获取指定索引的机器信息
     *
     * @param index 容器索引（从0开始）
     * @return 机器信息
     * @throws IllegalArgumentException 如果索引无效或容器未运行
     */
    public MachineInfo machine(int index) {
        if (index < 0 || index >= sshContainers.size()) {
            throw new IllegalArgumentException(
                    String.format("无效的容器索引: %d，有效范围: 0-%d", index, sshContainers.size() - 1));
        }

        GenericContainer<?> container = sshContainers.get(index);
        if (!container.isRunning()) {
            throw new IllegalStateException("容器 " + index + " 未运行");
        }

        TestContainersFactory.SshMachineContainerManager.SshConnectionInfo connectionInfo =
                TestContainersFactory.SshMachineContainerManager.getConnectionInfo(container);

        return machineFactory.forContainer(
                connectionInfo.host(),
                connectionInfo.port(),
                connectionInfo.username(),
                connectionInfo.password(),
                index + 1);
    }

    /**
     * 检查所有容器是否正在运行
     *
     * @return 是否所有容器都在运行
     */
    public boolean allRunning() {
        return sshContainers.stream().allMatch(GenericContainer::isRunning);
    }

    /**
     * 获取容器数量
     *
     * @return 容器数量
     */
    public int containerCount() {
        return sshContainers.size();
    }

    /**
     * 获取运行中的容器数量
     *
     * @return 运行中的容器数量
     */
    public int runningCount() {
        return (int)
                sshContainers.stream().mapToLong(container -> container.isRunning() ? 1 : 0).sum();
    }

    /**
     * 获取容器运行状态
     *
     * @return 容器状态信息
     */
    public ContainerStatus status() {
        int total = sshContainers.size();
        int running = runningCount();

        return new ContainerStatus(total, running, total == running);
    }

    /**
     * 将机器信息持久化到数据库 这样业务代码就能通过machineMapper.selectById()查询到对应的机器记录
     *
     * @param machines 要持久化的机器信息列表
     */
    public void persistMachinesToDatabase(List<MachineInfo> machines) {
        log.info("将 {} 个测试机器信息保存到数据库", machines.size());

        for (MachineInfo machine : machines) {
            try {
                // 检查是否已存在（避免重复插入）
                MachineInfo existing = machineMapper.selectById(machine.getId());
                if (existing == null) {
                    machineMapper.insert(machine);
                    log.debug(
                            "保存测试机器到数据库: {} - {}:{}",
                            machine.getName(),
                            machine.getIp(),
                            machine.getPort());
                } else {
                    log.debug("测试机器已存在，跳过插入: {}", machine.getName());
                }
            } catch (Exception e) {
                log.error("保存测试机器[{}]到数据库失败: {}", machine.getName(), e.getMessage(), e);
                throw new RuntimeException("保存测试机器到数据库失败", e);
            }
        }

        log.info("测试机器信息保存完成");
    }

    /**
     * 清理容器对应的机器记录
     *
     * @param machines 要清理的机器记录列表
     */
    public void cleanupContainerMachines(List<MachineInfo> machines) {
        if (machines == null || machines.isEmpty()) {
            return;
        }

        log.info("清理 {} 个容器机器记录", machines.size());

        for (MachineInfo machine : machines) {
            try {
                machineMapper.deleteById(machine.getId());
                log.debug(
                        "删除容器机器记录: {} - {}:{}",
                        machine.getName(),
                        machine.getIp(),
                        machine.getPort());
            } catch (Exception e) {
                log.warn("删除容器机器记录[{}]失败: {}", machine.getName(), e.getMessage());
            }
        }

        log.info("容器机器记录清理完成");
    }

    /** 清理所有容器 */
    public void cleanup() {
        log.info("清理SSH容器环境");

        TestContainersFactory.SshMachineContainerManager.cleanupAllContainers();
        sshContainers.clear();

        log.info("SSH容器环境清理完成");
    }

    /** 容器状态记录 */
    public record ContainerStatus(int totalContainers, int runningContainers, boolean allRunning) {}
}
