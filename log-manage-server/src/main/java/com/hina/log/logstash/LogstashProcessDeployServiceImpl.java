package com.hina.log.logstash;

import com.hina.log.dto.TaskDetailDTO;
import com.hina.log.entity.LogstashProcess;
import com.hina.log.entity.Machine;
import com.hina.log.logstash.command.LogstashCommand;
import com.hina.log.logstash.command.LogstashCommandFactory;
import com.hina.log.logstash.enums.LogstashProcessState;
import com.hina.log.logstash.enums.LogstashProcessStep;
import com.hina.log.logstash.enums.TaskOperationType;
import com.hina.log.logstash.state.LogstashProcessStateManager;
import com.hina.log.logstash.task.TaskService;
import com.hina.log.mapper.LogstashProcessMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Logstash进程服务实现类 - 负责Logstash进程的部署、启动和停止
 */
@Service("logstashDeployServiceImpl")
public class LogstashProcessDeployServiceImpl implements LogstashProcessDeployService {
    private static final Logger logger = LoggerFactory.getLogger(LogstashProcessDeployServiceImpl.class);

    private final LogstashProcessMapper logstashProcessMapper;
    private final TaskService taskService;
    private final LogstashProcessStateManager stateManager;
    private final LogstashCommandFactory commandFactory;

    public LogstashProcessDeployServiceImpl(
            LogstashProcessMapper logstashProcessMapper,
            TaskService taskService,
            LogstashProcessStateManager stateManager,
            LogstashCommandFactory commandFactory) {
        this.logstashProcessMapper = logstashProcessMapper;
        this.taskService = taskService;
        this.stateManager = stateManager;
        this.commandFactory = commandFactory;
    }

    @Override
    public void initializeProcessAsync(LogstashProcess process, List<Machine> machines) {
        if (process == null || machines == null || machines.isEmpty()) {
            return;
        }

        Long processId = process.getId();

        // 更新进程状态为初始化中
        logstashProcessMapper.updateState(processId, LogstashProcessState.INITIALIZING.name());

        // 创建任务
        String taskName = "初始化Logstash进程[" + process.getName() + "]";
        String taskDescription = "初始化Logstash进程环境";

        // 获取初始化步骤ID列表 - 不包括启动相关的步骤
        List<String> stepIds = Arrays.stream(LogstashProcessStep.values())
                .filter(step -> step != LogstashProcessStep.STOP_PROCESS &&
                        step != LogstashProcessStep.START_PROCESS &&
                        step != LogstashProcessStep.VERIFY_PROCESS)
                .map(LogstashProcessStep::getId)
                .collect(Collectors.toList());

        // 创建任务
        String taskId = taskService.createTask(
                processId,
                taskName,
                taskDescription,
                TaskOperationType.INITIALIZE,
                machines,
                stepIds);

        // 执行初始化操作
        taskService.executeAsync(taskId, () -> {
            // 执行初始化操作，传递taskId以便在每步更新状态
            stateManager.initialize(process, machines, taskId)
                    .thenAccept(initSuccess -> {
                        logger.info("Logstash进程环境初始化{}", initSuccess ? "成功" : "失败");
                    });
        }, null);

    }

    @Override
    public void startProcessAsync(LogstashProcess process, List<Machine> machines) {
        if (process == null || machines == null || machines.isEmpty()) {
            return;
        }

        Long processId = process.getId();

        // 更新进程状态为启动中
        logstashProcessMapper.updateState(processId, LogstashProcessState.STARTING.name());

        // 创建任务
        String taskName = "启动Logstash进程[" + process.getName() + "]";
        String taskDescription = "启动Logstash进程";

        // 获取启动步骤ID列表 - 只包括启动相关步骤
        List<String> stepIds = List.of(
                LogstashProcessStep.START_PROCESS.getId(),
                LogstashProcessStep.VERIFY_PROCESS.getId());

        // 创建任务
        String taskId = taskService.createTask(
                processId,
                taskName,
                taskDescription,
                TaskOperationType.START,
                machines,
                stepIds);

        // 执行启动操作
        taskService.executeAsync(taskId, () -> {
            // 执行启动操作，传递taskId以便在每步更新状态
            stateManager.start(process, machines, taskId)
                    .thenAccept(startSuccess -> {
                        logger.info("Logstash进程启动{}", startSuccess ? "成功" : "失败");

                    });
        }, null);
    }

    @Override
    public void stopProcessAsync(Long processId, List<Machine> machines) {
        if (processId == null || machines == null || machines.isEmpty()) {
            return;
        }

        // 查询进程信息
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            logger.error("找不到指定的Logstash进程: {}", processId);
            return;
        }

        // 创建任务
        String taskName = "停止Logstash进程 [" + processId + "]";
        String taskDescription = "停止目标机器上的Logstash进程";

        // 创建任务
        String taskId = taskService.createTask(
                processId,
                taskName,
                taskDescription,
                TaskOperationType.STOP,
                machines,
                List.of(LogstashProcessStep.STOP_PROCESS.getId()));

        // 执行任务
        taskService.executeAsync(taskId, () -> {
            stateManager.stop(process, machines, taskId)
                    .thenAccept(success -> {
                        logger.info("Logstash进程 [{}] 停止{}", processId, success ? "成功" : "失败");
                    });
        }, null);

    }

    @Override
    public CompletableFuture<Boolean> deleteProcessDirectory(Long processId, List<Machine> machines) {
        if (processId == null || machines == null || machines.isEmpty()) {
            logger.warn("无法删除进程目录: 进程ID或机器列表为空");
            return CompletableFuture.completedFuture(false);
        }

        logger.info("开始删除Logstash进程目录，进程ID: {}, 机器数量: {}", processId, machines.size());

        // 创建删除进程目录的命令
        LogstashCommand deleteCommand = commandFactory.deleteProcessDirectoryCommand(processId);

        // 收集所有机器上的删除操作的Future
        List<CompletableFuture<Boolean>> futures = machines.stream()
                .map(deleteCommand::execute)
                .toList();

        // 等待所有机器上的操作完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    // 检查所有机器是否都成功删除
                    boolean allSuccess = futures.stream()
                            .allMatch(future -> {
                                try {
                                    return future.get();
                                } catch (Exception e) {
                                    logger.error("获取删除命令执行结果时发生异常", e);
                                    return false;
                                }
                            });

                    logger.info("Logstash进程目录删除{}，进程ID: {}", allSuccess ? "成功" : "失败", processId);
                    return allSuccess;
                });
    }

    @Override
    public Optional<TaskDetailDTO> getLatestProcessTaskDetail(Long processId) {
        return taskService.getLatestProcessTaskDetail(processId);
    }

    @Override
    public List<String> getAllProcessTaskIds(Long processId) {
        return taskService.getAllProcessTaskIds(processId);
    }

    @Override
    @Transactional
    public void deleteTask(String taskId) {
        taskService.deleteTask(taskId);
    }

    @Override
    @Transactional
    public void deleteTaskSteps(String taskId) {
        taskService.deleteTaskSteps(taskId);
    }
}