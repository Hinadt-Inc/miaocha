package com.hina.log.logstash.state;

import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.logstash.command.LogstashCommand;
import com.hina.log.logstash.command.LogstashCommandFactory;
import com.hina.log.logstash.enums.LogstashMachineState;
import com.hina.log.logstash.enums.LogstashMachineStep;
import com.hina.log.logstash.enums.StepStatus;
import com.hina.log.logstash.task.TaskService;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 初始化失败状态处理器
 * 允许重新初始化进程
 */
@Component
public class InitializeFailedStateHandler extends AbstractLogstashMachineStateHandler {

    public InitializeFailedStateHandler(TaskService taskService, LogstashCommandFactory commandFactory) {
        super(taskService, commandFactory);
    }

    @Override
    public LogstashMachineState getState() {
        return LogstashMachineState.INITIALIZE_FAILED;
    }

    @Override
    public CompletableFuture<Boolean> handleInitialize(LogstashProcess process, Machine machine, String taskId) {
        Long processId = process.getId();
        Long machineId = machine.getId();
        
        logger.info("重新初始化机器 [{}] 上的Logstash进程 [{}]", machineId, processId);
        
        // 重置所有初始化步骤的状态
        taskService.resetStepStatuses(taskId, StepStatus.PENDING);
        
        CompletableFuture<Boolean> result = CompletableFuture.completedFuture(true);
        
        // 先删除进程目录（不计入步骤状态）
        result = result.thenCompose(success -> {
            if (!success) return CompletableFuture.completedFuture(false);
            
            logger.info("删除机器 [{}] 上的Logstash进程 [{}] 目录", machineId, processId);
            LogstashCommand deleteCommand = commandFactory.deleteProcessDirectoryCommand(processId);
            
            return deleteCommand.execute(machine)
                    .exceptionally(ex -> {
                        // 即使删除失败也继续执行（可能目录不存在）
                        logger.warn("删除进程目录失败，将继续执行初始化: {}", ex.getMessage());
                        return true;
                    });
        });
        
        // 1. 创建远程目录
        result = result.thenCompose(success -> {
            if (!success) return CompletableFuture.completedFuture(false);
            
            taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.CREATE_REMOTE_DIR.getId(), StepStatus.RUNNING);
            LogstashCommand createDirCommand = commandFactory.createDirectoryCommand(processId);
            
            return createDirCommand.execute(machine)
                    .thenApply(createDirSuccess -> {
                        StepStatus status = createDirSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        String errorMessage = createDirSuccess ? null : "创建远程目录失败";
                        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.CREATE_REMOTE_DIR.getId(), status, errorMessage);
                        return createDirSuccess;
                    })
                    .exceptionally(ex -> {
                        String errorMessage = ex.getMessage();
                        logger.error("创建远程目录时发生异常: {}", errorMessage, ex);
                        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.CREATE_REMOTE_DIR.getId(), StepStatus.FAILED, errorMessage);
                        return false;
                    });
        });
        
        // 2. 上传Logstash安装包
        result = result.thenCompose(success -> {
            if (!success) return CompletableFuture.completedFuture(false);
            
            taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.UPLOAD_PACKAGE.getId(), StepStatus.RUNNING);
            LogstashCommand uploadCommand = commandFactory.uploadPackageCommand(processId);
            
            return uploadCommand.execute(machine)
                    .thenApply(uploadSuccess -> {
                        StepStatus status = uploadSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        String errorMessage = uploadSuccess ? null : "上传Logstash安装包失败";
                        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.UPLOAD_PACKAGE.getId(), status, errorMessage);
                        return uploadSuccess;
                    })
                    .exceptionally(ex -> {
                        String errorMessage = ex.getMessage();
                        logger.error("上传安装包时发生异常: {}", errorMessage, ex);
                        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.UPLOAD_PACKAGE.getId(), StepStatus.FAILED, errorMessage);
                        return false;
                    });
        });
        
        // 3. 解压Logstash安装包
        result = result.thenCompose(success -> {
            if (!success) return CompletableFuture.completedFuture(false);
            
            taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.EXTRACT_PACKAGE.getId(), StepStatus.RUNNING);
            LogstashCommand extractCommand = commandFactory.extractPackageCommand(processId);
            
            return extractCommand.execute(machine)
                    .thenApply(extractSuccess -> {
                        StepStatus status = extractSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        String errorMessage = extractSuccess ? null : "解压Logstash安装包失败";
                        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.EXTRACT_PACKAGE.getId(), status, errorMessage);
                        return extractSuccess;
                    })
                    .exceptionally(ex -> {
                        String errorMessage = ex.getMessage();
                        logger.error("解压安装包时发生异常: {}", errorMessage, ex);
                        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.EXTRACT_PACKAGE.getId(), StepStatus.FAILED, errorMessage);
                        return false;
                    });
        });
        
        // 4. 创建配置文件
        result = result.thenCompose(success -> {
            if (!success) return CompletableFuture.completedFuture(false);
            
            taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.CREATE_CONFIG.getId(), StepStatus.RUNNING);
            LogstashCommand configCommand = commandFactory.createConfigCommand(process);
            
            return configCommand.execute(machine)
                    .thenApply(configSuccess -> {
                        StepStatus status = configSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        String errorMessage = configSuccess ? null : "创建配置文件失败";
                        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.CREATE_CONFIG.getId(), status, errorMessage);
                        return configSuccess;
                    })
                    .exceptionally(ex -> {
                        String errorMessage = ex.getMessage();
                        logger.error("创建配置文件时发生异常: {}", errorMessage, ex);
                        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.CREATE_CONFIG.getId(), StepStatus.FAILED, errorMessage);
                        return false;
                    });
        });
        
        // 5. 修改系统配置
        result = result.thenCompose(success -> {
            if (!success) return CompletableFuture.completedFuture(false);
            
            taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.MODIFY_CONFIG.getId(), StepStatus.RUNNING);
            LogstashCommand modifyConfigCommand = commandFactory.modifySystemConfigCommand(processId);
            
            return modifyConfigCommand.execute(machine)
                    .thenApply(modifyConfigSuccess -> {
                        StepStatus status = modifyConfigSuccess ? StepStatus.COMPLETED : StepStatus.FAILED;
                        String errorMessage = modifyConfigSuccess ? null : "修改系统配置失败";
                        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.MODIFY_CONFIG.getId(), status, errorMessage);
                        return modifyConfigSuccess;
                    })
                    .exceptionally(ex -> {
                        String errorMessage = ex.getMessage();
                        logger.error("修改系统配置时发生异常: {}", errorMessage, ex);
                        taskService.updateStepStatus(taskId, machineId, LogstashMachineStep.MODIFY_CONFIG.getId(), StepStatus.FAILED, errorMessage);
                        return false;
                    });
        });
        
        return result;
    }

    @Override
    public boolean canInitialize() {
        return true; // 允许重新初始化
    }

    @Override
    public LogstashMachineState getNextState(OperationType operationType, boolean success) {
        if (operationType == OperationType.INITIALIZE) {
            return success ? LogstashMachineState.NOT_STARTED : LogstashMachineState.INITIALIZE_FAILED;
        }
        return getState(); // 默认保持当前状态
    }
} 