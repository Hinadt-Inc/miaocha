package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.MachineService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.converter.MachineConverter;
import com.hinadt.miaocha.domain.dto.MachineCreateDTO;
import com.hinadt.miaocha.domain.dto.MachineDTO;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.domain.mapper.MachineMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 机器管理服务实现类 */
@Service
@RequiredArgsConstructor
public class MachineServiceImpl implements MachineService {

    private final MachineMapper machineMapper;
    private final SshClient sshClient;
    private final MachineConverter machineConverter;
    private final LogstashMachineMapper logstashMachineMapper;

    @Override
    @Transactional
    public MachineDTO createMachine(MachineCreateDTO dto) {
        // 检查机器名称是否已存在
        if (machineMapper.selectByName(dto.getName()) != null) {
            throw new BusinessException(ErrorCode.MACHINE_NAME_EXISTS);
        }

        // 创建机器记录
        MachineInfo machineInfo = machineConverter.toEntity(dto);
        machineMapper.insert(machineInfo);

        return machineConverter.toDto(machineInfo);
    }

    @Override
    @Transactional
    public MachineDTO updateMachine(Long id, MachineCreateDTO dto) {
        // 检查机器是否存在
        MachineInfo machineInfo = machineMapper.selectById(id);
        if (machineInfo == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND);
        }

        // 检查机器名称是否已被其他机器使用
        MachineInfo existingMachineInfo = machineMapper.selectByName(dto.getName());
        if (existingMachineInfo != null && !existingMachineInfo.getId().equals(id)) {
            throw new BusinessException(ErrorCode.MACHINE_NAME_EXISTS);
        }

        // 更新机器记录
        machineInfo = machineConverter.updateEntity(machineInfo, dto);
        machineInfo.setId(id);
        machineMapper.update(machineInfo);

        return machineConverter.toDto(machineInfo);
    }

    @Override
    @Transactional
    public void deleteMachine(Long id) {
        // 检查机器是否存在
        if (machineMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND);
        }

        // 检查机器是否被Logstash实例引用
        if (logstashMachineMapper.countByMachineId(id) > 0) {
            throw new BusinessException(ErrorCode.MACHINE_IN_USE);
        }

        // 删除机器记录
        machineMapper.deleteById(id);
    }

    @Override
    public MachineDTO getMachine(Long id) {
        MachineInfo machineInfo = machineMapper.selectById(id);
        if (machineInfo == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND);
        }
        return machineConverter.toDto(machineInfo);
    }

    @Override
    public List<MachineDTO> getAllMachines() {
        return machineMapper.selectAll().stream()
                .map(machineConverter::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean testConnection(Long id) {
        // 检查机器是否存在
        MachineInfo machineInfo = machineMapper.selectById(id);
        if (machineInfo == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND);
        }

        // 测试连接
        return sshClient.testConnection(machineInfo);
    }

    @Override
    public boolean testConnectionWithParams(MachineCreateDTO dto) {
        // 创建临时的机器信息对象用于测试连接
        MachineInfo machineInfo = machineConverter.toEntity(dto);

        // 测试连接
        return sshClient.testConnection(machineInfo);
    }
}
