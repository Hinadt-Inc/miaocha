package com.hina.log.logstash;

import com.hina.log.entity.LogstashMachine;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.logstash.enums.LogstashMachineState;
import com.hina.log.mapper.LogstashMachineMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Logstash进程配置服务
 * 处理配置相关的业务逻辑和状态检查
 */
@Service
public class LogstashProcessConfigService {

    private static final Logger logger = LoggerFactory.getLogger(LogstashProcessConfigService.class);
    private final LogstashMachineMapper logstashMachineMapper;

    public LogstashProcessConfigService(LogstashMachineMapper logstashMachineMapper) {
        this.logstashMachineMapper = logstashMachineMapper;
    }

    /**
     * 验证更新配置的前置条件
     * 确保目标进程不在运行状态
     *
     * @param processId 进程ID
     * @param machineId 机器ID (如果为null则检查所有机器)
     */
    public void validateConfigUpdateConditions(Long processId, Long machineId) {
        if (processId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        if (machineId == null) {
            // 全局更新：检查所有机器上的进程状态
            List<LogstashMachine> runningMachines = 
                logstashMachineMapper.selectByLogstashProcessIdAndState(processId, LogstashMachineState.RUNNING.name());
            
            if (!runningMachines.isEmpty()) {
                logger.error("无法更新配置：存在正在运行的Logstash进程实例");
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, 
                    "无法更新配置：在更新配置前，所有Logstash进程实例必须处于非运行状态");
            }
        } else {
            // 单机更新：检查当前机器上的进程状态
            LogstashMachine logstashMachine = 
                logstashMachineMapper.selectByLogstashProcessIdAndMachineId(processId, machineId);
            
            if (logstashMachine != null && 
                LogstashMachineState.RUNNING.name().equals(logstashMachine.getState())) {
                logger.error("无法更新配置：机器 [{}] 上的Logstash进程 [{}] 正在运行中", machineId, processId);
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, 
                    "无法更新配置：在更新配置前，Logstash进程必须处于非运行状态");
            }
        }
    }

    /**
     * 验证刷新配置的前置条件
     * 确保目标进程不在运行状态
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
                logstashMachineMapper.selectByLogstashProcessIdAndState(processId, LogstashMachineState.RUNNING.name());
            
            if (!runningMachines.isEmpty()) {
                logger.error("无法刷新配置：存在正在运行的Logstash进程实例");
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, 
                    "无法刷新配置：在刷新配置前，所有Logstash进程实例必须处于非运行状态");
            }
        } else {
            // 单机刷新：检查当前机器上的进程状态
            LogstashMachine logstashMachine = 
                logstashMachineMapper.selectByLogstashProcessIdAndMachineId(processId, machineId);
            
            if (logstashMachine != null && 
                LogstashMachineState.RUNNING.name().equals(logstashMachine.getState())) {
                logger.error("无法刷新配置：机器 [{}] 上的Logstash进程 [{}] 正在运行中", machineId, processId);
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, 
                    "无法刷新配置：在刷新配置前，Logstash进程必须处于非运行状态");
            }
        }
    }
} 