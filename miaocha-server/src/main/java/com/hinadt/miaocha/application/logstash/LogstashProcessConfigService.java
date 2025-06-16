package com.hinadt.miaocha.application.logstash;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Logstash进程配置服务 处理配置相关的业务逻辑和状态检查 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogstashProcessConfigService {

    private final LogstashMachineMapper logstashMachineMapper;

    /**
     * 验证配置操作的前置条件 确保目标实例不在禁止配置操作的状态
     *
     * @param processId 进程ID
     * @param instanceIds 指定的实例ID列表，为null则检查所有实例
     * @param operationType 操作类型描述
     */
    public void validateConfigOperationConditions(
            Long processId, List<Long> instanceIds, String operationType) {
        if (processId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        log.debug("验证进程[{}]的{}条件", processId, operationType);

        List<LogstashMachine> targetInstances = getTargetInstances(processId, instanceIds);
        validateInstancesNotInForbiddenStates(targetInstances, operationType);

        log.debug("进程[{}]的{}条件验证通过", processId, operationType);
    }

    /**
     * 验证配置更新的前置条件 确保目标实例不在运行状态
     *
     * @param processId 进程ID
     * @param instanceIds 指定的实例ID列表，为null则检查所有实例
     */
    public void validateConfigUpdateConditions(Long processId, List<Long> instanceIds) {
        validateConfigOperationConditions(processId, instanceIds, "配置更新");
    }

    /**
     * 验证配置刷新的前置条件 确保目标实例不在运行状态
     *
     * @param processId 进程ID
     * @param instanceIds 指定的实例ID列表，为null则检查所有实例
     */
    public void validateConfigRefreshConditions(Long processId, List<Long> instanceIds) {
        validateConfigOperationConditions(processId, instanceIds, "配置刷新");
    }

    /** 获取目标实例列表 */
    private List<LogstashMachine> getTargetInstances(Long processId, List<Long> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            // 获取进程的所有实例
            return logstashMachineMapper.selectByLogstashProcessId(processId);
        } else {
            // 获取指定的实例并验证它们属于该进程
            return instanceIds.stream()
                    .map(
                            instanceId -> {
                                LogstashMachine instance =
                                        logstashMachineMapper.selectById(instanceId);
                                if (instance == null) {
                                    throw new BusinessException(
                                            ErrorCode.RESOURCE_NOT_FOUND,
                                            "指定的LogstashMachine实例不存在: ID=" + instanceId);
                                }
                                if (!instance.getLogstashProcessId().equals(processId)) {
                                    throw new BusinessException(
                                            ErrorCode.VALIDATION_ERROR,
                                            "LogstashMachine实例["
                                                    + instanceId
                                                    + "]不属于进程["
                                                    + processId
                                                    + "]");
                                }
                                return instance;
                            })
                    .toList();
        }
    }

    /** 验证实例不在禁止配置操作的状态 */
    private void validateInstancesNotInForbiddenStates(
            List<LogstashMachine> instances, String operationType) {
        // 禁止配置操作的状态
        Set<LogstashMachineState> forbiddenStates =
                Set.of(
                        LogstashMachineState.RUNNING,
                        LogstashMachineState.STARTING,
                        LogstashMachineState.STOPPING);

        for (LogstashMachine instance : instances) {
            LogstashMachineState state = LogstashMachineState.valueOf(instance.getState());

            if (forbiddenStates.contains(state)) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        String.format(
                                "实例[%s]当前状态[%s]不允许%s，请先停止实例",
                                instance.getId(), state.getDescription(), operationType));
            }

            if (state == LogstashMachineState.INITIALIZE_FAILED) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        String.format(
                                "实例[%s]当前状态[%s]不允许%s，请重新初始化",
                                instance.getId(), state.getDescription(), operationType));
            }
        }
    }
}
