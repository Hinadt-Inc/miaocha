package com.hinadt.miaocha.application.logstash;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Logstash进程配置服务 处理配置相关的业务逻辑和状态检查 */
@Service
public class LogstashProcessConfigService {

    private static final Logger logger =
            LoggerFactory.getLogger(LogstashProcessConfigService.class);
    private final LogstashMachineMapper logstashMachineMapper;

    public LogstashProcessConfigService(LogstashMachineMapper logstashMachineMapper) {
        this.logstashMachineMapper = logstashMachineMapper;
    }

    /**
     * 验证更新配置的前置条件 确保目标进程不在运行状态
     *
     * @param processId 进程ID
     * @param machineId 机器ID (如果为null则检查所有机器)
     */
    public void validateConfigUpdateConditions(Long processId, Long machineId) {
        logger.debug("验证进程 [{}] 在机器 [{}] 上的配置更新条件", processId, machineId);

        List<LogstashMachine> machines;
        if (machineId != null) {
            // 检查单个机器
            LogstashMachine machine =
                    logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                            processId, machineId);
            if (machine == null) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        String.format("找不到进程 [%s] 在机器 [%s] 上的记录", processId, machineId));
            }
            machines = List.of(machine);
        } else {
            // 检查所有关联机器
            machines = logstashMachineMapper.selectByLogstashProcessId(processId);
        }

        for (LogstashMachine machine : machines) {
            LogstashMachineState state = LogstashMachineState.valueOf(machine.getState());

            // 检查状态是否允许更新配置
            if (state == LogstashMachineState.RUNNING
                    || state == LogstashMachineState.STARTING
                    || state == LogstashMachineState.STOPPING) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        String.format("当前状态 [%s] 不允许更新配置，请先停止进程", state.getDescription()));
            }

            if (state == LogstashMachineState.INITIALIZE_FAILED) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        String.format("当前状态 [%s] 不允许更新配置，请重新初始化", state.getDescription()));
            }
        }

        logger.debug("进程 [{}] 配置更新条件验证通过", processId);
    }

    /**
     * 验证刷新配置的前置条件 确保目标进程不在运行状态
     *
     * @param processId 进程ID
     * @param machineId 机器ID (如果为null则检查所有机器)
     */
    public void validateConfigRefreshConditions(Long processId, Long machineId) {
        if (processId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        if (machineId == null) {
            // 全局刷新：检查所有机器上的进程状态
            List<LogstashMachine> runningMachines =
                    logstashMachineMapper.selectByLogstashProcessIdAndState(
                            processId, LogstashMachineState.RUNNING.name());

            if (!runningMachines.isEmpty()) {
                logger.error("无法刷新配置：存在正在运行的Logstash进程实例");
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR, "无法刷新配置：在刷新配置前，所有Logstash进程实例必须处于非运行状态");
            }
        } else {
            // 单机刷新：检查当前机器上的进程状态
            LogstashMachine logstashMachine =
                    logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                            processId, machineId);

            if (logstashMachine != null
                    && LogstashMachineState.RUNNING.name().equals(logstashMachine.getState())) {
                logger.error("无法刷新配置：机器 [{}] 上的Logstash进程 [{}] 正在运行中", machineId, processId);
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR, "无法刷新配置：在刷新配置前，Logstash进程必须处于非运行状态");
            }
        }
    }
}
