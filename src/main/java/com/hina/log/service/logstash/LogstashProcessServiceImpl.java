package com.hina.log.service.logstash;

import com.hina.log.config.LogstashProperties;
import com.hina.log.dto.TaskDetailDTO;
import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.entity.LogstashMachine;
import com.hina.log.enums.LogstashProcessState;
import com.hina.log.enums.LogstashProcessStep;
import com.hina.log.enums.StepStatus;
import com.hina.log.enums.TaskOperationType;
import com.hina.log.exception.LogstashException;
import com.hina.log.exception.SshDependencyException;
import com.hina.log.exception.SshException;
import com.hina.log.exception.SshOperationException;
import com.hina.log.mapper.LogstashProcessMapper;
import com.hina.log.mapper.LogstashMachineMapper;
import com.hina.log.service.task.TaskService;
import com.hina.log.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Logstash进程服务实现类 - 负责Logstash进程的部署、启动和停止
 */
@Service("logstashDeployServiceImpl")
public class LogstashProcessServiceImpl implements LogstashProcessService {
    private static final Logger logger = LoggerFactory.getLogger(LogstashProcessServiceImpl.class);

    private final LogstashProcessMapper logstashProcessMapper;
    private final LogstashProperties logstashProperties;
    private final TaskService taskService;
    private final SshClient sshClient;
    private final LogstashMachineMapper logstashMachineMapper;

    public LogstashProcessServiceImpl(
            LogstashProcessMapper logstashProcessMapper,
            LogstashProperties logstashProperties,
            TaskService taskService,
            SshClient sshClient,
            LogstashMachineMapper logstashMachineMapper) {
        this.logstashProcessMapper = logstashProcessMapper;
        this.logstashProperties = logstashProperties;
        this.taskService = taskService;
        this.sshClient = sshClient;
        this.logstashMachineMapper = logstashMachineMapper;
    }

    @Override
    public CompletableFuture<Boolean> deployAndStartAsync(LogstashProcess process, List<Machine> machines) {
        if (process == null || machines == null || machines.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        // 更新进程状态为启动中
        logstashProcessMapper.updateState(process.getId(), LogstashProcessState.STARTING.name());

        // 创建任务
        String taskName = "部署并启动Logstash进程[" + process.getName() + "]";
        String taskDescription = "部署并启动Logstash进程到目标机器";

        // 获取步骤ID列表（不包括停止步骤）
        List<String> stepIds = Arrays.stream(LogstashProcessStep.values())
                .filter(step -> step != LogstashProcessStep.STOP_PROCESS)
                .map(LogstashProcessStep::getId)
                .collect(Collectors.toList());

        // 创建任务
        String taskId = taskService.createTask(
                process.getId(),
                taskName,
                taskDescription,
                TaskOperationType.START,
                machines,
                stepIds);

        // 创建任务完成Future
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

        // 执行任务
        taskService.executeAsync(taskId, () -> {
            try {
                executeStartTask(taskId, process, machines);
                resultFuture.complete(true);
            } catch (Exception e) {
                logger.error("执行Logstash启动任务失败", e);
                resultFuture.complete(false);
            }
        }, null);

        return resultFuture;
    }

    @Override
    public CompletableFuture<Boolean> stopAsync(List<Machine> machines) {
        if (machines == null || machines.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        // 创建任务
        String taskName = "停止Logstash进程";
        String taskDescription = "停止目标机器上的Logstash进程";

        // 创建任务，使用0作为进程ID（表示不与特定进程关联）
        String taskId = taskService.createTask(
                0L,
                taskName,
                taskDescription,
                TaskOperationType.STOP,
                machines,
                List.of(LogstashProcessStep.STOP_PROCESS.getId()));

        // 创建任务完成Future
        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

        // 执行任务
        taskService.executeAsync(taskId, () -> {
            try {
                executeStopTask(taskId, machines);
                resultFuture.complete(true);
            } catch (Exception e) {
                logger.error("执行Logstash停止任务失败", e);
                resultFuture.complete(false);
            }
        }, null);

        return resultFuture;
    }

    @Override
    public String getProcessTaskStatus(Long processId) {
        return taskService.getLatestProcessTaskDetail(processId)
                .map(detail -> taskService.getTaskSummary(detail.getTaskId()))
                .orElse("未找到与进程关联的任务");
    }

    @Override
    public Optional<TaskDetailDTO> getLatestProcessTaskDetail(Long processId) {
        return taskService.getLatestProcessTaskDetail(processId);
    }

    @Override
    public boolean resetFailedProcessState(Long processId) {
        // 更新进程状态为未启动
        logstashProcessMapper.updateState(processId, LogstashProcessState.NOT_STARTED.name());
        return true;
    }

    /**
     * 执行启动任务
     */
    private void executeStartTask(String taskId, LogstashProcess process, List<Machine> machines) {
        // 遍历所有机器，执行部署和启动任务
        for (Machine machine : machines) {
            try {
                // 1. 创建远程目录
                executeStep(taskId, machine.getId(), LogstashProcessStep.CREATE_REMOTE_DIR, () -> {
                    try {
                        String result = sshClient.executeCommand(machine,
                                String.format("mkdir -p %s", logstashProperties.getDeployDir()));
                        if (result == null || result.isEmpty()) {
                            throw new LogstashException("创建远程目录失败");
                        }
                        return true;
                    } catch (SshDependencyException e) {
                        throw new SshOperationException("系统缺少SSH依赖: " + e.getMessage() +
                                "。请在服务器安装必要的SSH客户端工具后重试", e);
                    } catch (SshException e) {
                        throw new SshOperationException("创建远程目录失败: " + e.getMessage(), e);
                    }
                });

                // 2. 上传安装包
                executeStep(taskId, machine.getId(), LogstashProcessStep.UPLOAD_PACKAGE, () -> {
                    try {
                        sshClient.uploadFile(machine,
                                logstashProperties.getPackagePath(),
                                logstashProperties.getDeployDir());
                        return true;
                    } catch (SshException e) {
                        throw new SshOperationException("上传Logstash安装包失败: " + e.getMessage(), e);
                    }
                });

                // 3. 解压安装包
                executeStep(taskId, machine.getId(), LogstashProcessStep.EXTRACT_PACKAGE, () -> {
                    try {
                        String packageFileName = getPackageFileName(logstashProperties.getPackagePath());
                        String command = String.format("cd %s && tar -xzf %s",
                                logstashProperties.getDeployDir(), packageFileName);

                        String result = sshClient.executeCommand(machine, command);
                        if (result == null) {
                            throw new LogstashException("解压Logstash安装包失败");
                        }
                        return true;
                    } catch (SshException e) {
                        throw new SshOperationException("解压Logstash安装包失败: " + e.getMessage(), e);
                    }
                });

                // 4. 创建配置
                executeStep(taskId, machine.getId(), LogstashProcessStep.CREATE_CONFIG, () -> {
                    try {
                        String configContent = process.getConfigJson();
                        String configPath = String.format("%s/config/logstash-%s.conf",
                                logstashProperties.getDeployDir(), process.getId());

                        // 使用执行命令的方式创建文件
                        String escapedContent = configContent.replace("\"", "\\\"").replace("$", "\\$");
                        String command = String.format("mkdir -p %1$s/config && echo \"%2$s\" > %3$s",
                                logstashProperties.getDeployDir(),
                                escapedContent,
                                configPath);

                        String result = sshClient.executeCommand(machine, command);
                        if (result == null) {
                            throw new LogstashException("创建Logstash配置文件失败");
                        }
                        return true;
                    } catch (SshException e) {
                        throw new SshOperationException("创建Logstash配置文件失败: " + e.getMessage(), e);
                    }
                });

                // 5. 启动进程
                executeStep(taskId, machine.getId(), LogstashProcessStep.START_PROCESS, () -> {
                    try {
                        String command = String.format(
                                "cd %s && bin/logstash -f config/logstash-%s.conf --config.reload.automatic > logs/logstash-%s.log 2>&1 &",
                                logstashProperties.getDeployDir(), process.getId(), process.getId());

                        String result = sshClient.executeCommand(machine, command);
                        if (result == null) {
                            throw new LogstashException("启动Logstash进程失败");
                        }
                        return true;
                    } catch (SshException e) {
                        throw new SshOperationException("启动Logstash进程失败: " + e.getMessage(), e);
                    }
                });

                // 6. 验证进程
                executeStep(taskId, machine.getId(), LogstashProcessStep.VERIFY_PROCESS, () -> {
                    try {
                        String command = String.format(
                                "ps -ef | grep 'logstash-%s.conf' | grep -v grep | awk '{print $2}'",
                                process.getId());

                        String pid = sshClient.executeCommand(machine, command);
                        if (pid == null || pid.trim().isEmpty()) {
                            throw new LogstashException("未找到运行中的Logstash进程");
                        }

                        // 保存进程PID到LogstashMachine表
                        String pidValue = pid.trim().split("\\s+")[0]; // 获取第一个PID
                        logstashMachineMapper.updateProcessPid(process.getId(), machine.getId(), pidValue);

                        logger.info("Saved Logstash process PID {} for process {} on machine {}",
                                pidValue, process.getId(), machine.getId());

                        // 更新进程状态为运行中
                        logstashProcessMapper.updateState(process.getId(), LogstashProcessState.RUNNING.name());
                        return true;
                    } catch (SshException e) {
                        throw new SshOperationException("验证Logstash进程失败: " + e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                logger.error("在机器{}上执行Logstash任务失败", machine.getId(), e);
            }
        }
    }

    /**
     * 执行停止任务
     */
    private void executeStopTask(String taskId, List<Machine> machines) {
        for (Machine machine : machines) {
            try {
                // 停止进程
                executeStep(taskId, machine.getId(), LogstashProcessStep.STOP_PROCESS, () -> {
                    try {
                        // 先查找进程
                        String findCommand = "ps -ef | grep logstash | grep -v grep | awk '{print $2}'";
                        String pids = sshClient.executeCommand(machine, findCommand);

                        if (pids != null && !pids.trim().isEmpty()) {
                            // 存在进程，杀死进程
                            String[] pidArray = pids.split("\\s+");
                            List<String> pidList = new ArrayList<>();

                            for (String pid : pidArray) {
                                if (pid != null && !pid.trim().isEmpty()) {
                                    pidList.add(pid.trim());
                                }
                            }

                            if (!pidList.isEmpty()) {
                                String killCommand = "kill -9 " + String.join(" ", pidList);
                                sshClient.executeCommand(machine, killCommand);

                                // 查找关联的LogstashMachine记录，并清除进程PID
                                List<LogstashMachine> logstashMachines = logstashMachineMapper
                                        .selectByMachineId(machine.getId());

                                for (LogstashMachine logstashMachine : logstashMachines) {
                                    if (logstashMachine.getProcessPid() != null &&
                                            pidList.contains(logstashMachine.getProcessPid())) {
                                        // 清除进程PID
                                        logstashMachineMapper.updateProcessPid(
                                                logstashMachine.getLogstashProcessId(),
                                                machine.getId(),
                                                null);

                                        logger.info("Cleared Logstash process PID for process {} on machine {}",
                                                logstashMachine.getLogstashProcessId(), machine.getId());
                                    }
                                }
                            }
                        }

                        return true;
                    } catch (SshException e) {
                        throw new SshOperationException("停止Logstash进程失败: " + e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                logger.error("在机器{}上停止Logstash进程失败", machine.getId(), e);
            }
        }
    }

    /**
     * 执行步骤
     */
    private <T> T executeStep(String taskId, Long machineId, LogstashProcessStep step, ThrowingSupplier<T> action) {
        try {
            // 更新步骤状态为执行中
            taskService.updateStepStatus(taskId, machineId, step.getId(), StepStatus.RUNNING);

            // 执行操作
            T result = action.get();

            // 更新步骤状态为已完成
            taskService.updateStepStatus(taskId, machineId, step.getId(), StepStatus.COMPLETED);

            return result;
        } catch (Exception e) {
            // 更新步骤状态为失败
            taskService.updateStepStatus(taskId, machineId, step.getId(), StepStatus.FAILED);
            taskService.updateStepErrorMessage(taskId, machineId, step.getId(), e.getMessage());

            throw new RuntimeException("执行步骤失败: " + step.getName(), e);
        }
    }

    /**
     * 从路径获取文件名
     */
    private String getPackageFileName(String path) {
        int lastSlashIndex = path.lastIndexOf('/');
        return lastSlashIndex >= 0 ? path.substring(lastSlashIndex + 1) : path;
    }

    /**
     * 可抛出异常的Supplier函数式接口
     */
    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}