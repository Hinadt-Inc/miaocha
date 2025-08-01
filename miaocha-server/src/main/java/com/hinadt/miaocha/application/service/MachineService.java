package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.MachineConnectionTestResultDTO;
import com.hinadt.miaocha.domain.dto.MachineCreateDTO;
import com.hinadt.miaocha.domain.dto.MachineDTO;
import java.util.List;

/** 机器管理服务接口 */
public interface MachineService {

    /**
     * 创建机器
     *
     * @param dto 机器创建DTO
     * @return 创建的机器信息
     */
    MachineDTO createMachine(MachineCreateDTO dto);

    /**
     * 更新机器
     *
     * @param id 机器ID
     * @param dto 机器更新DTO
     * @return 更新后的机器信息
     */
    MachineDTO updateMachine(Long id, MachineCreateDTO dto);

    /**
     * 删除机器
     *
     * @param id 机器ID
     */
    void deleteMachine(Long id);

    /**
     * 获取机器信息
     *
     * @param id 机器ID
     * @return 机器信息
     */
    MachineDTO getMachine(Long id);

    /**
     * 获取所有机器
     *
     * @return 机器列表
     */
    List<MachineDTO> getAllMachines();

    /**
     * 测试机器连接
     *
     * @param id 机器ID
     * @return 连接测试结果
     */
    MachineConnectionTestResultDTO testConnection(Long id);

    /**
     * 测试机器连接 (使用参数)
     *
     * @param dto 机器连接参数
     * @return 连接测试结果
     */
    MachineConnectionTestResultDTO testConnectionWithParams(MachineCreateDTO dto);
}
