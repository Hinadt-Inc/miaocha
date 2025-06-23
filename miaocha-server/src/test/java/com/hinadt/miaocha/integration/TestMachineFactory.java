package com.hinadt.miaocha.integration;

import com.hinadt.miaocha.domain.entity.MachineInfo;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 测试机器工厂
 *
 * <p>职责： 1. 提供不同场景下的MachineInfo实体创建 2. 支持真实容器对应的机器和虚拟机器 3. 处理各种测试场景的机器配置
 *
 * <p>设计理念： - MachineInfo是业务实体，与实际容器解耦 - 提供工厂方法满足不同测试需求 - 支持异常场景（如机器不存在、连接失败等）
 */
@Slf4j
@Component
public class TestMachineFactory {

    /**
     * 为容器创建对应的机器信息
     *
     * @param containerHost 容器主机地址
     * @param containerPort 容器端口
     * @param username SSH用户名
     * @param password SSH密码
     * @param machineIndex 机器索引
     * @return 机器信息实体
     */
    public MachineInfo forContainer(
            String containerHost,
            int containerPort,
            String username,
            String password,
            int machineIndex) {
        MachineInfo machine = new MachineInfo();
        // 使用100+machineIndex的ID避免与其他测试数据冲突
        machine.setId((long) (10000 + machineIndex));
        machine.setName("test-container-" + (10000 + machineIndex));
        machine.setIp(containerHost);
        machine.setPort(containerPort);
        machine.setUsername(username);
        machine.setPassword(password);

        setTestDefaults(machine);

        log.debug("创建容器机器: {} - {}:{}", machine.getName(), machine.getIp(), machine.getPort());
        return machine;
    }

    /**
     * 创建模拟机器（用于纯业务逻辑测试）
     *
     * @param machineId 机器ID
     * @param machineName 机器名称
     * @return 机器信息实体
     */
    public MachineInfo mockMachine(Long machineId, String machineName) {
        MachineInfo machine = new MachineInfo();
        machine.setId(machineId);
        machine.setName(machineName);
        machine.setIp("192.168.1." + machineId);
        machine.setPort(22);
        machine.setUsername("testuser");
        machine.setPassword("testpass");

        setTestDefaults(machine);

        log.debug("创建模拟机器: {} - {}:{}", machine.getName(), machine.getIp(), machine.getPort());
        return machine;
    }

    /**
     * 创建不可达机器（用于测试连接失败场景）
     *
     * @param machineId 机器ID
     * @return 不可达的机器信息
     */
    public MachineInfo unreachableMachine(Long machineId) {
        MachineInfo machine = new MachineInfo();
        machine.setId(machineId);
        machine.setName("unreachable-machine-" + machineId);
        machine.setIp("192.168.255.255"); // 不可达的IP
        machine.setPort(22);
        machine.setUsername("testuser");
        machine.setPassword("wrongpass");

        setTestDefaults(machine);

        log.debug("创建不可达机器: {} - {}:{}", machine.getName(), machine.getIp(), machine.getPort());
        return machine;
    }

    /**
     * 创建错误配置机器（用于测试配置错误场景）
     *
     * @param machineId 机器ID
     * @param errorType 错误类型
     * @return 错误配置的机器信息
     */
    public MachineInfo errorMachine(Long machineId, MachineErrorType errorType) {
        MachineInfo machine = new MachineInfo();
        machine.setId(machineId);
        machine.setName("error-machine-" + machineId);

        switch (errorType) {
            case WRONG_PORT -> {
                machine.setIp("127.0.0.1");
                machine.setPort(9999); // 错误端口
                machine.setUsername("testuser");
                machine.setPassword("testpass");
            }
            case WRONG_CREDENTIALS -> {
                machine.setIp("127.0.0.1");
                machine.setPort(22);
                machine.setUsername("wronguser");
                machine.setPassword("wrongpass");
            }
            case INVALID_IP -> {
                machine.setIp("invalid.ip.address");
                machine.setPort(22);
                machine.setUsername("testuser");
                machine.setPassword("testpass");
            }
        }

        setTestDefaults(machine);

        log.debug(
                "创建错误机器: {} - {}:{} (错误类型: {})",
                machine.getName(),
                machine.getIp(),
                machine.getPort(),
                errorType);
        return machine;
    }

    /** 设置测试默认值 */
    private void setTestDefaults(MachineInfo machine) {
        LocalDateTime now = LocalDateTime.now();
        machine.setCreateTime(now);
        machine.setUpdateTime(now);
        machine.setCreateUser("test_factory");
        machine.setUpdateUser("test_factory");
    }

    /** 机器错误类型枚举 */
    public enum MachineErrorType {
        WRONG_PORT, // 错误端口
        WRONG_CREDENTIALS, // 错误凭据
        INVALID_IP // 无效IP
    }
}
