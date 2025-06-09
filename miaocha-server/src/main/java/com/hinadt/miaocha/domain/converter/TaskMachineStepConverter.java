package com.hinadt.miaocha.domain.converter;

import com.hinadt.miaocha.domain.dto.logstash.TaskDetailDTO.MachineStepDTO;
import com.hinadt.miaocha.domain.entity.LogstashTaskMachineStep;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.MachineMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 任务机器步骤转换器 负责将基于机器ID的数据转换为基于机器名称的数据 */
@Component
public class TaskMachineStepConverter {

    @Autowired private TaskDetailConverter taskDetailConverter;

    @Autowired private MachineMapper machineMapper;

    /**
     * 转换机器步骤映射（使用机器名称作为键）
     *
     * @param machineStepsMap 基于机器ID的步骤映射
     * @return 基于机器名称的步骤映射
     */
    public Map<String, List<MachineStepDTO>> convertToNameBasedMap(
            Map<Long, List<LogstashTaskMachineStep>> machineStepsMap) {
        if (machineStepsMap == null || machineStepsMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // 获取所有涉及的机器ID
        Set<Long> machineIds = machineStepsMap.keySet();

        // 批量查询机器信息
        List<MachineInfo> machineInfos = machineMapper.selectByIds(new ArrayList<>(machineIds));
        Map<Long, MachineInfo> machineMap =
                machineInfos.stream()
                        .collect(Collectors.toMap(MachineInfo::getId, machine -> machine));

        // 转换为基于名称的映射
        Map<String, List<MachineStepDTO>> nameBasedMap = new HashMap<>();

        for (Map.Entry<Long, List<LogstashTaskMachineStep>> entry : machineStepsMap.entrySet()) {
            Long machineId = entry.getKey();
            List<LogstashTaskMachineStep> steps = entry.getValue();

            // 获取机器名称，如果找不到则使用ID作为键
            String machineKey = getMachineKey(machineMap, machineId);

            // 转换步骤列表
            List<MachineStepDTO> stepDtos =
                    steps.stream()
                            .map(taskDetailConverter::convertToStepDTO)
                            .collect(Collectors.toList());

            nameBasedMap.put(machineKey, stepDtos);
        }

        return nameBasedMap;
    }

    /**
     * 转换已经转换为DTO的步骤映射（使用机器名称作为键）
     *
     * @param machineStepDtoMap 基于机器ID的DTO步骤映射
     * @return 基于机器名称的DTO步骤映射
     */
    public Map<String, List<MachineStepDTO>> convertDtoMapToNameBased(
            Map<Long, List<MachineStepDTO>> machineStepDtoMap) {
        if (machineStepDtoMap == null || machineStepDtoMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // 获取所有涉及的机器ID
        Set<Long> machineIds = machineStepDtoMap.keySet();

        // 批量查询机器信息
        List<MachineInfo> machineInfos = machineMapper.selectByIds(new ArrayList<>(machineIds));
        Map<Long, MachineInfo> machineMap =
                machineInfos.stream()
                        .collect(Collectors.toMap(MachineInfo::getId, machine -> machine));

        // 转换为基于名称的映射
        Map<String, List<MachineStepDTO>> nameBasedMap = new HashMap<>();

        for (Map.Entry<Long, List<MachineStepDTO>> entry : machineStepDtoMap.entrySet()) {
            Long machineId = entry.getKey();
            List<MachineStepDTO> steps = entry.getValue();

            // 获取机器名称，如果找不到则使用ID作为键
            String machineKey = getMachineKey(machineMap, machineId);

            nameBasedMap.put(machineKey, steps);
        }

        return nameBasedMap;
    }

    /** 获取机器的键值（优先使用名称，如果不存在则使用ID字符串） */
    private String getMachineKey(Map<Long, MachineInfo> machineMap, Long machineId) {
        MachineInfo machineInfo = machineMap.get(machineId);
        if (machineInfo != null
                && machineInfo.getName() != null
                && !machineInfo.getName().isEmpty()) {
            return machineInfo.getName();
        }
        // 如果找不到机器或名称为空，则使用ID作为键
        return "Machine-" + machineId;
    }
}
