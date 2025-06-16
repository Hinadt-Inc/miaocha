package com.hinadt.miaocha.domain.converter;

import com.hinadt.miaocha.domain.dto.logstash.TaskDetailDTO;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashTaskMachineStep;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.MachineMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 任务实例步骤转换器 负责将基于LogstashMachine实例ID的数据转换为基于实例名称的数据 */
@Component
public class TaskMachineStepConverter {

    @Autowired private TaskDetailConverter taskDetailConverter;

    @Autowired private LogstashMachineMapper logstashMachineMapper;

    @Autowired private MachineMapper machineMapper;

    /**
     * 转换实例步骤映射（使用实例名称作为键）
     *
     * @param instanceStepsMap 基于LogstashMachine实例ID的步骤映射
     * @return 基于实例名称的步骤映射
     */
    public Map<String, List<TaskDetailDTO.InstanceStepDTO>> convertToNameBasedMap(
            Map<Long, List<LogstashTaskMachineStep>> instanceStepsMap) {
        if (instanceStepsMap == null || instanceStepsMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // 获取所有涉及的LogstashMachine实例ID
        Set<Long> logstashMachineIds = instanceStepsMap.keySet();

        // 批量查询LogstashMachine实例信息
        List<LogstashMachine> logstashMachines =
                logstashMachineMapper.selectByIds(new ArrayList<>(logstashMachineIds));
        Map<Long, LogstashMachine> logstashMachineMap =
                logstashMachines.stream()
                        .collect(Collectors.toMap(LogstashMachine::getId, instance -> instance));

        // 获取所有涉及的机器ID，用于查询机器名称
        Set<Long> machineIds =
                logstashMachines.stream()
                        .map(LogstashMachine::getMachineId)
                        .collect(Collectors.toSet());

        List<MachineInfo> machineInfos = machineMapper.selectByIds(new ArrayList<>(machineIds));
        Map<Long, MachineInfo> machineMap =
                machineInfos.stream()
                        .collect(Collectors.toMap(MachineInfo::getId, machine -> machine));

        // 转换为基于实例名称的映射
        Map<String, List<TaskDetailDTO.InstanceStepDTO>> nameBasedMap = new HashMap<>();

        for (Map.Entry<Long, List<LogstashTaskMachineStep>> entry : instanceStepsMap.entrySet()) {
            Long logstashMachineId = entry.getKey();
            List<LogstashTaskMachineStep> steps = entry.getValue();

            // 获取实例名称作为键
            String instanceKey = getInstanceKey(logstashMachineMap, machineMap, logstashMachineId);

            // 转换步骤列表
            List<TaskDetailDTO.InstanceStepDTO> stepDtos =
                    steps.stream()
                            .map(taskDetailConverter::convertToStepDTO)
                            .collect(Collectors.toList());

            nameBasedMap.put(instanceKey, stepDtos);
        }

        return nameBasedMap;
    }

    /** 获取实例的键值（格式：机器名称-实例ID） */
    private String getInstanceKey(
            Map<Long, LogstashMachine> logstashMachineMap,
            Map<Long, MachineInfo> machineMap,
            Long logstashMachineId) {
        LogstashMachine logstashMachine = logstashMachineMap.get(logstashMachineId);
        if (logstashMachine == null) {
            return "实例-" + logstashMachineId;
        }

        MachineInfo machineInfo = machineMap.get(logstashMachine.getMachineId());
        String machineName =
                (machineInfo != null && machineInfo.getName() != null)
                        ? machineInfo.getName()
                        : "Machine-" + logstashMachine.getMachineId();

        return machineName + "-实例-" + logstashMachineId;
    }
}
