package com.hinadt.miaocha.application.logstash;

import com.hinadt.miaocha.application.service.MachineService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.MachineConnectionTestResultDTO;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Logstash机器连接验证服务 在对单台机器执行操作前验证机器连接 */
@Service
public class LogstashMachineConnectionValidator {

    private static final Logger logger =
            LoggerFactory.getLogger(LogstashMachineConnectionValidator.class);

    private final MachineService machineService;

    public LogstashMachineConnectionValidator(MachineService machineService) {
        this.machineService = machineService;
    }

    /**
     * 验证单台机器的连接 如果连接失败，抛出业务异常，不进入任务队列和状态机流程
     *
     * @param machineInfo 要验证的机器
     * @throws BusinessException 如果机器连接失败
     */
    public void validateSingleMachineConnection(MachineInfo machineInfo) {
        if (machineInfo == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "机器信息不能为空");
        }

        Long machineId = machineInfo.getId();
        logger.debug("开始验证机器 [{}] ({}) 的连接", machineId, machineInfo.getIp());

        try {
            MachineConnectionTestResultDTO result = machineService.testConnection(machineId);
            if (!result.isSuccess()) {
                String errorMessage =
                        String.format(
                                "无法连接到机器 [%s] (%s:%d)，错误原因: %s",
                                machineInfo.getName(),
                                machineInfo.getIp(),
                                machineInfo.getPort(),
                                result.getErrorMessage());
                logger.error(errorMessage);
                throw new BusinessException(ErrorCode.MACHINE_CONNECTION_FAILED, errorMessage);
            }

            logger.debug("机器 [{}] ({}) 连接验证成功", machineId, machineInfo.getIp());

        } catch (BusinessException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            "验证机器 [%s] (%s:%d) 连接时发生异常: %s",
                            machineInfo.getName(),
                            machineInfo.getIp(),
                            machineInfo.getPort(),
                            e.getMessage());
            logger.error(errorMessage, e);
            throw new BusinessException(ErrorCode.MACHINE_CONNECTION_FAILED, errorMessage);
        }
    }

    /**
     * 验证机器连接（通过机器ID）
     *
     * @param machineId 机器ID
     * @throws BusinessException 如果机器连接失败或机器不存在
     */
    public void validateMachineConnectionById(Long machineId) {
        if (machineId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "机器ID不能为空");
        }

        try {
            MachineConnectionTestResultDTO result = machineService.testConnection(machineId);
            if (!result.isSuccess()) {
                String errorMessage =
                        String.format(
                                "无法连接到机器 [ID:%d]，错误原因: %s", machineId, result.getErrorMessage());
                logger.error(errorMessage);
                throw new BusinessException(ErrorCode.MACHINE_CONNECTION_FAILED, errorMessage);
            }

            logger.debug("机器 [ID:{}] 连接验证成功", machineId);

        } catch (BusinessException e) {
            // 重新抛出业务异常（包括机器不存在等情况）
            throw e;
        } catch (Exception e) {
            String errorMessage =
                    String.format("验证机器 [ID:%d] 连接时发生异常: %s", machineId, e.getMessage());
            logger.error(errorMessage, e);
            throw new BusinessException(ErrorCode.MACHINE_CONNECTION_FAILED, errorMessage);
        }
    }
}
