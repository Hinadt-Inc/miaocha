package com.hina.log.application.service.impl;

import com.hina.log.domain.converter.MachineConverter;
import com.hina.log.domain.dto.MachineCreateDTO;
import com.hina.log.domain.dto.MachineDTO;
import com.hina.log.domain.entity.Machine;
import com.hina.log.common.exception.BusinessException;
import com.hina.log.common.exception.ErrorCode;
import com.hina.log.domain.mapper.MachineMapper;
import com.hina.log.application.service.MachineService;
import com.hina.log.common.ssh.SshClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机器管理服务实现类
 */
@Service
@RequiredArgsConstructor
public class MachineServiceImpl implements MachineService {

    private final MachineMapper machineMapper;
    private final SshClient sshClient;
    private final MachineConverter machineConverter;

    @Override
    @Transactional
    public MachineDTO createMachine(MachineCreateDTO dto) {
        // 检查机器名称是否已存在
        if (machineMapper.selectByName(dto.getName()) != null) {
            throw new BusinessException(ErrorCode.MACHINE_NAME_EXISTS);
        }

        // 创建机器记录
        Machine machine = machineConverter.toEntity(dto);
        machineMapper.insert(machine);

        return machineConverter.toDto(machine);
    }

    @Override
    @Transactional
    public MachineDTO updateMachine(Long id, MachineCreateDTO dto) {
        // 检查机器是否存在
        Machine machine = machineMapper.selectById(id);
        if (machine == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND);
        }

        // 检查机器名称是否已被其他机器使用
        Machine existingMachine = machineMapper.selectByName(dto.getName());
        if (existingMachine != null && !existingMachine.getId().equals(id)) {
            throw new BusinessException(ErrorCode.MACHINE_NAME_EXISTS);
        }

        // 更新机器记录
        machine = machineConverter.updateEntity(machine, dto);
        machine.setId(id);
        machineMapper.update(machine);

        return machineConverter.toDto(machine);
    }

    @Override
    @Transactional
    public void deleteMachine(Long id) {
        // 检查机器是否存在
        if (machineMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND);
        }

        // 删除机器记录
        machineMapper.deleteById(id);
    }

    @Override
    public MachineDTO getMachine(Long id) {
        // 检查机器是否存在
        Machine machine = machineMapper.selectById(id);
        if (machine == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND);
        }

        return machineConverter.toDto(machine);
    }

    @Override
    public List<MachineDTO> getAllMachines() {
        List<Machine> machines = machineMapper.selectAll();
        return machines.stream()
                .map(machineConverter::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean testConnection(Long id) {
        // 检查机器是否存在
        Machine machine = machineMapper.selectById(id);
        if (machine == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND);
        }

        // 测试连接
        return sshClient.testConnection(machine);
    }
}