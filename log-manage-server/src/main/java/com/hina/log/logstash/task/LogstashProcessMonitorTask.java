package com.hina.log.logstash.task;

import com.hina.log.entity.LogstashMachine;
import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.exception.SshException;
import com.hina.log.logstash.enums.LogstashProcessState;
import com.hina.log.mapper.LogstashMachineMapper;
import com.hina.log.mapper.LogstashProcessMapper;
import com.hina.log.mapper.MachineMapper;
import com.hina.log.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Logstash进程监控任务
 * 定期检查所有运行中的Logstash进程状态，发现异常停止的进程时更新状态
 * 只检查已运行至少5分钟的进程，以避免刚启动的进程被误判为异常
 */
@Component
public class LogstashProcessMonitorTask {
    private static final Logger logger = LoggerFactory.getLogger(LogstashProcessMonitorTask.class);

    // 进程需要运行的最小时间（分钟）才会被监控
    private static final long MIN_PROCESS_RUNTIME_MINUTES = 5;

    private final LogstashMachineMapper logstashMachineMapper;
    private final LogstashProcessMapper logstashProcessMapper;
    private final MachineMapper machineMapper;
    private final SshClient sshClient;

    public LogstashProcessMonitorTask(
            LogstashMachineMapper logstashMachineMapper,
            LogstashProcessMapper logstashProcessMapper,
            MachineMapper machineMapper,
            SshClient sshClient) {
        this.logstashMachineMapper = logstashMachineMapper;
        this.logstashProcessMapper = logstashProcessMapper;
        this.machineMapper = machineMapper;
        this.sshClient = sshClient;
    }

    /**
     * 定时检查所有运行中的Logstash进程状态
     * 每10分钟执行一次
     */
    @Scheduled(fixedRateString = "${logstash.monitor.interval:600000}")
    public void monitorLogstashProcesses() {
        try {
            logger.info("开始定时检查Logstash进程状态...");

            // 获取所有有PID记录的Logstash进程关联
            List<LogstashMachine> processesWithPid = logstashMachineMapper.selectAllWithProcessPid();
            if (processesWithPid.isEmpty()) {
                logger.info("没有找到正在运行的Logstash进程，跳过检查");
                return;
            }

            // 记录检查开始时间
            LocalDateTime checkStartTime = LocalDateTime.now();
            logger.info("找到{}个有PID记录的Logstash进程，开始检查状态", processesWithPid.size());

            int checkedCount = 0;
            int skippedCount = 0;

            // 遍历所有进程关联记录进行检查
            for (LogstashMachine logstashMachine : processesWithPid) {
                if (shouldCheckProcess(logstashMachine)) {
                    checkProcessStatus(logstashMachine);
                    checkedCount++;
                } else {
                    skippedCount++;
                }
            }

            // 记录检查完成时间
            long duration = Duration.between(checkStartTime, LocalDateTime.now()).toMillis();
            logger.info("Logstash进程状态检查完成，共检查{}个进程，跳过{}个新启动进程，耗时{}毫秒",
                    checkedCount, skippedCount, duration);
        } catch (Exception e) {
            // 捕获所有异常，确保定时任务不会因为异常而停止
            logger.error("Logstash进程监控任务执行异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 判断是否应该检查该进程
     * 只检查状态为RUNNING且已运行至少5分钟的进程
     * 
     * @param logstashMachine Logstash进程与机器的关联记录
     * @return 是否应该检查
     */
    private boolean shouldCheckProcess(LogstashMachine logstashMachine) {
        Long processId = logstashMachine.getLogstashProcessId();
        String pid = logstashMachine.getProcessPid();

        // 跳过没有PID的记录
        if (!StringUtils.hasText(pid)) {
            return false;
        }

        // 获取进程信息
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            logger.warn("找不到ID为{}的Logstash进程记录，可能已被删除", processId);
            return false;
        }

        // 只检查运行中的进程
        if (!LogstashProcessState.RUNNING.name().equals(process.getState())) {
            logger.debug("Logstash进程[{}]状态为{}，不是运行中状态，跳过检查",
                    processId, process.getState());
            return false;
        }

        // 检查进程最后更新时间，确保至少运行了MIN_PROCESS_RUNTIME_MINUTES分钟
        LocalDateTime updateTime = process.getUpdateTime();
        if (updateTime == null) {
            logger.warn("Logstash进程[{}]没有更新时间记录，无法判断运行时长，跳过检查", processId);
            return false;
        }

        LocalDateTime minRunTime = LocalDateTime.now().minusMinutes(MIN_PROCESS_RUNTIME_MINUTES);
        if (updateTime.isAfter(minRunTime)) {
            // 进程更新时间少于MIN_PROCESS_RUNTIME_MINUTES分钟，跳过检查
            long runningMinutes = Duration.between(updateTime, LocalDateTime.now()).toMinutes();
            logger.debug("Logstash进程[{}]运行时间不足{}分钟(当前{}分钟)，跳过检查",
                    processId, MIN_PROCESS_RUNTIME_MINUTES, runningMinutes);
            return false;
        }

        return true;
    }

    /**
     * 检查单个Logstash进程的状态
     * 
     * @param logstashMachine Logstash进程与机器的关联记录
     */
    private void checkProcessStatus(LogstashMachine logstashMachine) {
        Long processId = logstashMachine.getLogstashProcessId();
        Long machineId = logstashMachine.getMachineId();
        String pid = logstashMachine.getProcessPid();

        // 获取机器信息
        Machine machine = machineMapper.selectById(machineId);
        if (machine == null) {
            logger.warn("找不到ID为{}的机器记录，无法检查Logstash进程[{}]状态",
                    machineId, processId);
            return;
        }

        try {
            // 通过SSH检查进程是否存在
            boolean processExists = checkProcessExistsBySsh(machine, pid);
            if (!processExists) {
                handleDeadProcess(processId, machine, pid);
            } else {
                logger.debug("Logstash进程[{}]在机器[{}]上正常运行中，PID={}",
                        processId, machine.getIp(), pid);
            }
        } catch (Exception e) {
            logger.error("检查Logstash进程[{}]在机器[{}]上的状态时发生错误: {}",
                    processId, machine.getIp(), e.getMessage(), e);
        }
    }

    /**
     * 通过SSH检查进程是否存在
     * 
     * @param machine 目标机器
     * @param pid     进程PID
     * @return 进程是否存在
     */
    private boolean checkProcessExistsBySsh(Machine machine, String pid) {
        try {
            // 使用ps命令检查进程是否存在
            String command = String.format("ps -p %s -o pid= || echo \"Process not found\"", pid);
            String result = sshClient.executeCommand(machine, command);

            // 如果结果包含"Process not found"，则进程不存在
            boolean exists = !result.contains("Process not found") && StringUtils.hasText(result.trim());
            return exists;
        } catch (SshException e) {
            logger.error("SSH执行检查进程命令失败: {}", e.getMessage());
            // 如果SSH执行失败，为安全起见，假设进程仍在运行
            return true;
        }
    }

    /**
     * 处理已经死亡的进程
     * 
     * @param processId Logstash进程ID
     * @param machine   目标机器
     * @param pid       进程PID
     */
    private void handleDeadProcess(Long processId, Machine machine, String pid) {
        logger.warn("检测到Logstash进程[{}]在机器[{}]上异常终止，PID={}",
                processId, machine.getIp(), pid);

        try {
            // 更新进程状态为未启动
            int updateResult = logstashProcessMapper.updateState(processId, LogstashProcessState.NOT_STARTED.name());
            if (updateResult > 0) {
                logger.info("已将Logstash进程[{}]状态更新为未启动", processId);
            } else {
                logger.warn("更新Logstash进程[{}]状态失败，可能已被删除", processId);
            }

            // 清空PID记录
            int pidUpdateResult = logstashMachineMapper.updateProcessPid(processId, machine.getId(), null);
            if (pidUpdateResult > 0) {
                logger.info("已清除Logstash进程[{}]在机器[{}]上的PID记录", processId, machine.getIp());
            } else {
                logger.warn("清除Logstash进程[{}]在机器[{}]上的PID记录失败，可能已被删除",
                        processId, machine.getIp());
            }
        } catch (Exception e) {
            logger.error("处理死亡进程[{}]时发生错误: {}", processId, e.getMessage(), e);
        }
    }
}