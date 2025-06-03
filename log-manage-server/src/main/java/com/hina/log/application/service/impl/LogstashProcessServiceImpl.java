package com.hina.log.application.service.impl;

import com.hina.log.application.logstash.LogstashConfigSyncService;
import com.hina.log.application.logstash.LogstashMachineConnectionValidator;
import com.hina.log.application.logstash.LogstashProcessDeployService;
import com.hina.log.application.logstash.command.LogstashCommandFactory;
import com.hina.log.application.logstash.enums.LogstashMachineState;
import com.hina.log.application.logstash.parser.LogstashConfigParser;
import com.hina.log.application.logstash.task.TaskService;
import com.hina.log.application.service.LogstashProcessService;
import com.hina.log.application.service.TableValidationService;
import com.hina.log.application.service.sql.JdbcQueryExecutor;
import com.hina.log.common.exception.BusinessException;
import com.hina.log.common.exception.ErrorCode;
import com.hina.log.domain.converter.LogstashMachineConverter;
import com.hina.log.domain.converter.LogstashProcessConverter;
import com.hina.log.domain.dto.logstash.LogstashMachineDetailDTO;
import com.hina.log.domain.dto.logstash.LogstashProcessConfigUpdateRequestDTO;
import com.hina.log.domain.dto.logstash.LogstashProcessCreateDTO;
import com.hina.log.domain.dto.logstash.LogstashProcessResponseDTO;
import com.hina.log.domain.dto.logstash.LogstashProcessScaleRequestDTO;
import com.hina.log.domain.dto.logstash.LogstashProcessUpdateDTO;
import com.hina.log.domain.entity.DatasourceInfo;
import com.hina.log.domain.entity.LogstashMachine;
import com.hina.log.domain.entity.LogstashProcess;
import com.hina.log.domain.entity.MachineInfo;
import com.hina.log.domain.mapper.DatasourceMapper;
import com.hina.log.domain.mapper.LogstashMachineMapper;
import com.hina.log.domain.mapper.LogstashProcessMapper;
import com.hina.log.domain.mapper.MachineMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Logstash进程管理服务实现类 */
@Service
public class LogstashProcessServiceImpl implements LogstashProcessService {
    // 常量
    private static final Logger logger = LoggerFactory.getLogger(LogstashProcessServiceImpl.class);

    // 实例变量
    private final LogstashProcessMapper logstashProcessMapper;
    private final LogstashMachineMapper logstashMachineMapper;
    private final MachineMapper machineMapper;
    private final DatasourceMapper datasourceMapper;
    private final LogstashProcessDeployService logstashDeployService;
    private final LogstashProcessConverter logstashProcessConverter;
    private final LogstashMachineConverter logstashMachineConverter;
    private final LogstashConfigParser logstashConfigParser;
    private final TableValidationService tableValidationService;
    private final JdbcQueryExecutor jdbcQueryExecutor;
    private final TaskService taskService;
    private final LogstashConfigSyncService configSyncService;
    private final LogstashMachineConnectionValidator connectionValidator;
    private final LogstashCommandFactory commandFactory;

    // 构造函数
    public LogstashProcessServiceImpl(
            LogstashProcessMapper logstashProcessMapper,
            LogstashMachineMapper logstashMachineMapper,
            MachineMapper machineMapper,
            DatasourceMapper datasourceMapper,
            @Qualifier("logstashDeployServiceImpl") LogstashProcessDeployService logstashDeployService,
            LogstashProcessConverter logstashProcessConverter,
            LogstashMachineConverter logstashMachineConverter,
            LogstashConfigParser logstashConfigParser,
            TableValidationService tableValidationService,
            JdbcQueryExecutor jdbcQueryExecutor,
            TaskService taskService,
            LogstashConfigSyncService configSyncService,
            LogstashMachineConnectionValidator connectionValidator,
            LogstashCommandFactory commandFactory) {
        this.logstashProcessMapper = logstashProcessMapper;
        this.logstashMachineMapper = logstashMachineMapper;
        this.machineMapper = machineMapper;
        this.datasourceMapper = datasourceMapper;
        this.logstashDeployService = logstashDeployService;
        this.logstashProcessConverter = logstashProcessConverter;
        this.logstashMachineConverter = logstashMachineConverter;
        this.logstashConfigParser = logstashConfigParser;
        this.tableValidationService = tableValidationService;
        this.jdbcQueryExecutor = jdbcQueryExecutor;
        this.taskService = taskService;
        this.configSyncService = configSyncService;
        this.connectionValidator = connectionValidator;
        this.commandFactory = commandFactory;
    }

    // 公共方法 - 按照接口定义顺序排列
    @Override
    @Transactional
    public LogstashProcessResponseDTO createLogstashProcess(LogstashProcessCreateDTO dto) {
        // 参数校验
        if (dto == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "参数不能为空");
        }

        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程名称不能为空");
        }

        if (!StringUtils.hasText(dto.getModule())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块名称不能为空");
        }

        if (dto.getDatasourceId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "数据源ID不能为空");
        }

        if (dto.getMachineIds() == null || dto.getMachineIds().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "至少需要选择一台机器");
        }

        // 检查进程名称是否已存在
        if (logstashProcessMapper.selectByName(dto.getName()) != null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NAME_EXISTS);
        }

        // 检查模块名称是否已存在
        if (logstashProcessMapper.selectByModule(dto.getModule()) != null) {
            throw new BusinessException(ErrorCode.LOGSTASH_MODULE_EXISTS);
        }

        // 检查数据源是否存在
        if (datasourceMapper.selectById(dto.getDatasourceId()) == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND, "指定的数据源不存在");
        }

        // 如果有配置文件，验证配置文件并提取表名
        if (StringUtils.hasText(dto.getConfigContent())) {
            // 验证Logstash配置
            LogstashConfigParser.ValidationResult validationResult =
                    logstashConfigParser.validateConfig(dto.getConfigContent());
            if (!validationResult.isValid()) {
                throw new BusinessException(
                        validationResult.getErrorCode(), validationResult.getErrorMessage());
            }

            // 如果没有手动指定表名，尝试从配置中提取
            if (!StringUtils.hasText(dto.getTableName())) {
                Optional<String> tableName =
                        logstashConfigParser.extractTableName(dto.getConfigContent());
                tableName.ifPresent(dto::setTableName);
            }
        }

        // 创建进程记录
        LogstashProcess process = logstashProcessConverter.toEntity(dto);
        process.setCreateTime(LocalDateTime.now());
        process.setUpdateTime(LocalDateTime.now());
        logstashProcessMapper.insert(process);

        // 确定实际使用的部署路径
        String fullDeployPath;
        if (StringUtils.hasText(dto.getCustomDeployPath())) {
            // 用户指定了部署路径，直接使用，不拼接
            fullDeployPath = dto.getCustomDeployPath();
        } else {
            // 使用默认路径并拼接进程ID
            String baseDeployPath = logstashDeployService.getDeployBaseDir();
            fullDeployPath = String.format("%s/logstash-%d", baseDeployPath, process.getId());
        }

        // 创建进程与机器的关联关系
        for (Long machineId : dto.getMachineIds()) {
            if (machineMapper.selectById(machineId) == null) {
                throw new BusinessException(
                        ErrorCode.MACHINE_NOT_FOUND, "指定的机器不存在: ID=" + machineId);
            }

            // 使用转换器创建LogstashMachine，并复制配置，设置完整的部署路径
            LogstashMachine logstashMachine =
                    logstashMachineConverter.createFromProcess(process, machineId, fullDeployPath);
            logstashMachineMapper.insert(logstashMachine);
        }

        // 获取进程对应的机器
        List<MachineInfo> machineInfos = getMachinesForProcess(process.getId());

        // 执行进程环境初始化
        logstashDeployService.initializeProcess(process, machineInfos);

        // 检查jvmOptions和logstashYml是否为空，如果为空则异步同步配置文件
        boolean needSyncJvmOptions = !StringUtils.hasText(dto.getJvmOptions());
        boolean needSyncLogstashYml = !StringUtils.hasText(dto.getLogstashYml());

        if (needSyncJvmOptions || needSyncLogstashYml) {
            // 使用配置同步服务，异步执行配置同步
            configSyncService.syncConfigurationAsync(
                    process.getId(), needSyncJvmOptions, needSyncLogstashYml);
        }

        return getLogstashProcess(process.getId());
    }

    @Override
    @Transactional
    public void deleteLogstashProcess(Long id) {
        LogstashProcess process = getAndValidateProcess(id);

        // 获取进程对应的机器
        List<MachineInfo> machineInfos = getMachinesForProcess(id);
        if (machineInfos.isEmpty()) {
            logger.warn("进程没有关联的机器，进程ID: {}", id);
        } else {
            // 检查进程是否正在运行，运行中的进程不能删除
            List<LogstashMachine> machineRelations =
                    logstashMachineMapper.selectByLogstashProcessId(id);
            boolean anyRunning =
                    machineRelations.stream()
                            .anyMatch(
                                    m ->
                                            LogstashMachineState.RUNNING.name().equals(m.getState())
                                                    || LogstashMachineState.STARTING
                                                            .name()
                                                            .equals(m.getState()));

            if (anyRunning) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "运行中的进程不能删除，请先停止进程");
            }

            // 删除进程物理目录 - 直接调用，不依赖状态管理
            deleteProcessDirectories(id, machineInfos);
        }

        // 删除与进程相关的所有任务和步骤
        deleteProcessTasks(id);

        // 删除与进程相关的所有机器关联
        logstashMachineMapper.deleteByLogstashProcessId(id);
        logger.info("已删除进程与机器的关联关系: {}", id);

        // 删除进程记录
        logstashProcessMapper.deleteById(id);
        logger.info("已删除Logstash进程: {}", id);
    }

    @Override
    public LogstashProcessResponseDTO getLogstashProcess(Long id) {
        LogstashProcess process = getAndValidateProcess(id);
        return logstashProcessConverter.toResponseDTO(process);
    }

    @Override
    public List<LogstashProcessResponseDTO> getAllLogstashProcesses() {
        List<LogstashProcess> processes = logstashProcessMapper.selectAll();
        return processes.stream()
                .map(logstashProcessConverter::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO startLogstashProcess(Long id) {
        LogstashProcess process = getAndValidateProcess(id);

        // 验证配置
        validateProcessConfig(process);

        // 获取进程关联的可启动机器
        List<MachineInfo> machinesToStart = getStartableMachines(id);
        if (machinesToStart.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "没有处于可启动状态的机器实例");
        }

        // 启动进程
        logstashDeployService.startProcess(process, machinesToStart);

        return getLogstashProcess(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO startMachineProcess(Long id, Long machineId) {
        // 获取进程和机器信息
        LogstashProcess process = getAndValidateProcess(id);
        MachineInfo machineInfo = machineMapper.selectById(machineId);
        if (machineInfo == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND);
        }

        // 验证进程配置
        validateProcessConfig(process);

        // 启动单台机器上的进程
        logstashDeployService.startMachine(process, machineInfo);

        return getLogstashProcess(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO stopLogstashProcess(Long id) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        // 检查进程是否存在
        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        // 获取进程关联的可停止机器
        List<MachineInfo> machinesToStop = getStoppableMachines(id);
        if (machinesToStop.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "没有处于可停止状态的机器实例");
        }

        // 停止进程
        logstashDeployService.stopProcess(id, machinesToStop);

        return getLogstashProcess(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO stopMachineProcess(Long id, Long machineId) {
        // 验证进程ID
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        // 获取机器信息
        MachineInfo machineInfo = machineMapper.selectById(machineId);
        if (machineInfo == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND);
        }

        // 停止单台机器上的进程
        logstashDeployService.stopMachine(id, machineInfo);

        return getLogstashProcess(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO forceStopLogstashProcess(Long id) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        // 检查进程是否存在
        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        // 获取进程关联的所有机器（强制停止不检查状态）
        List<MachineInfo> allMachines = getMachinesForProcess(id);
        if (allMachines.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程未关联任何机器实例");
        }

        logger.warn("开始强制停止Logstash进程[{}]，涉及{}台机器", id, allMachines.size());

        // 强制停止进程
        logstashDeployService.forceStopProcess(id, allMachines);

        return getLogstashProcess(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO forceStopMachineProcess(Long id, Long machineId) {
        // 验证进程ID
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        // 获取机器信息
        MachineInfo machineInfo = machineMapper.selectById(machineId);
        if (machineInfo == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND);
        }

        logger.warn("开始强制停止Logstash进程[{}]在机器[{}]上的实例", id, machineId);

        // 强制停止单台机器上的进程
        logstashDeployService.forceStopMachine(id, machineInfo);

        return getLogstashProcess(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO updateSingleMachineConfig(
            Long id, Long machineId, String configContent, String jvmOptions, String logstashYml) {
        // 验证进程和机器是否存在，以及它们之间的关系
        ValidatedEntities entities = getAndValidateObjects(id, machineId);

        // 验证机器连接
        connectionValidator.validateSingleMachineConnection(entities.machineInfo);

        // 参数校验 - 至少有一个配置需要更新
        boolean hasUpdate =
                StringUtils.hasText(configContent)
                        || StringUtils.hasText(jvmOptions)
                        || StringUtils.hasText(logstashYml);

        if (!hasUpdate) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "至少需要指定一项配置内容进行更新");
        }

        // 验证配置内容
        if (StringUtils.hasText(configContent)) {
            LogstashConfigParser.ValidationResult validationResult =
                    logstashConfigParser.validateConfig(configContent);
            if (!validationResult.isValid()) {
                throw new BusinessException(
                        validationResult.getErrorCode(), validationResult.getErrorMessage());
            }
        }

        // 先同步更新数据库中该机器的配置记录
        configSyncService.updateConfigForSingleMachine(
                id, machineId, configContent, jvmOptions, logstashYml);

        // 更新远程配置文件
        List<MachineInfo> machineInfos = new ArrayList<>();
        machineInfos.add(entities.machineInfo);
        logstashDeployService.updateMultipleConfigs(
                id, machineInfos, configContent, jvmOptions, logstashYml);

        return getLogstashProcess(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO updateLogstashConfig(
            Long id, LogstashProcessConfigUpdateRequestDTO dto) {
        getAndValidateProcess(id);
        validateUpdateConfigDto(dto);

        // 检查所有Logstash机器的状态，如果有任何一个运行中则不允许更新
        logstashDeployService.getConfigService().validateConfigUpdateConditions(id, null);

        // 如果更新主配置文件，验证配置内容
        String configContent = null;
        if (StringUtils.hasText(dto.getConfigContent())) {
            LogstashConfigParser.ValidationResult validationResult =
                    logstashConfigParser.validateConfig(dto.getConfigContent());
            if (!validationResult.isValid()) {
                throw new BusinessException(
                        validationResult.getErrorCode(), validationResult.getErrorMessage());
            }
            configContent = dto.getConfigContent();
        }

        // 准备JVM配置
        String jvmOptions = StringUtils.hasText(dto.getJvmOptions()) ? dto.getJvmOptions() : null;

        // 准备Logstash系统配置
        String logstashYml =
                StringUtils.hasText(dto.getLogstashYml()) ? dto.getLogstashYml() : null;

        // 只更新配置相关字段，不更新模块名、进程名等其他字段
        logstashProcessMapper.updateConfigOnly(id, configContent, jvmOptions, logstashYml);

        // 同步更新所有关联LogstashMachine的配置
        configSyncService.updateConfigForAllMachines(id, configContent, jvmOptions, logstashYml);

        // 使用LogstashProcessDeployService更新配置
        // 获取所有关联机器，如果指定了machineIds则只更新这些机器
        List<MachineInfo> targetMachineInfos;
        if (dto.getMachineIds() != null && !dto.getMachineIds().isEmpty()) {
            targetMachineInfos = machineMapper.selectByIds(dto.getMachineIds());
        } else {
            // 获取所有关联机器
            List<LogstashMachine> logstashMachines =
                    logstashMachineMapper.selectByLogstashProcessId(id);
            List<Long> machineIds =
                    logstashMachines.stream()
                            .map(LogstashMachine::getMachineId)
                            .collect(Collectors.toList());
            targetMachineInfos = machineMapper.selectByIds(machineIds);
        }

        if (!targetMachineInfos.isEmpty()) {
            // 异步更新所有目标机器的配置
            logstashDeployService.updateMultipleConfigs(
                    id, targetMachineInfos, configContent, jvmOptions, logstashYml);
        }

        return logstashProcessConverter.toResponseDTO(logstashProcessMapper.selectById(id));
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO refreshLogstashConfig(
            Long id, LogstashProcessConfigUpdateRequestDTO dto) {
        LogstashProcess process = getAndValidateProcess(id);

        // 验证配置
        validateProcessConfig(process);

        // 同步更新所有关联LogstashMachine的配置
        configSyncService.updateConfigForAllMachines(
                id, process.getConfigContent(), process.getJvmOptions(), process.getLogstashYml());

        // 获取要刷新配置的机器列表
        List<Long> machineIds = dto != null ? dto.getMachineIds() : null;
        List<MachineInfo> machinesToRefreshes;

        if (machineIds != null && !machineIds.isEmpty()) {
            // 刷新指定机器的配置
            machinesToRefreshes = getTargetMachinesForConfig(id, machineIds);
        } else {
            // 刷新所有机器的配置
            machinesToRefreshes = getMachinesForProcess(id);
        }

        // 刷新配置
        logstashDeployService.refreshConfig(id, machinesToRefreshes);

        return getLogstashProcess(id);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO executeDorisSql(Long id, String sql) {
        LogstashProcess process = getAndValidateProcess(id);

        if (!StringUtils.hasText(sql)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SQL语句不能为空");
        }

        // 获取进程对应的机器状态
        List<LogstashMachine> machineRelations =
                logstashMachineMapper.selectByLogstashProcessId(id);
        if (machineRelations.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程未关联任何机器");
        }

        // 检查是否有任何机器不处于未启动状态
        boolean anyNotStopped =
                machineRelations.stream()
                        .anyMatch(
                                m ->
                                        m.getState() != null
                                                && !LogstashMachineState.NOT_STARTED
                                                        .name()
                                                        .equals(m.getState()));

        if (anyNotStopped) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "只有当所有进程实例都处于未启动状态时才能执行Doris SQL");
        }

        // 检查dorisSql字段是否已有值
        if (StringUtils.hasText(process.getDorisSql())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "该进程已经执行过Doris SQL，不能重复执行");
        }

        // 检查SQL是否包含DROP语句
        String sqlLower = sql.toLowerCase().trim();
        if (sqlLower.contains("drop ")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不允许执行DROP语句");
        }

        // 获取数据源
        DatasourceInfo datasourceInfo = datasourceMapper.selectById(process.getDatasourceId());
        if (datasourceInfo == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND, "找不到关联的数据源");
        }

        try {
            // 执行SQL
            logger.info("执行Doris SQL: {}", sql);
            jdbcQueryExecutor.executeQuery(datasourceInfo, sql);

            // 更新进程的dorisSql字段
            process.setDorisSql(sql);
            process.setUpdateTime(LocalDateTime.now());
            logstashProcessMapper.update(process);

            logger.info("Doris SQL执行成功并已保存到进程 [{}]", id);
            return getLogstashProcess(id);
        } catch (Exception e) {
            logger.error("执行Doris SQL失败: {}", e.getMessage(), e);
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR, "执行Doris SQL失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO updateLogstashProcessMetadata(
            Long id, LogstashProcessUpdateDTO dto) {
        // 验证进程是否存在
        LogstashProcess existingProcess = getAndValidateProcess(id);

        // 如果module发生变化，验证新module的唯一性
        if (!existingProcess.getModule().equals(dto.getModule())) {
            LogstashProcess processWithSameModule =
                    logstashProcessMapper.selectByModule(dto.getModule());
            if (processWithSameModule != null && !processWithSameModule.getId().equals(id)) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        String.format("模块名称 '%s' 已被其他进程使用，请选择其他名称", dto.getModule()));
            }
        }

        // 更新元信息
        int updateResult =
                logstashProcessMapper.updateMetadataOnly(id, dto.getName(), dto.getModule());
        if (updateResult == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新进程信息失败");
        }

        logger.info(
                "成功更新Logstash进程[{}]的元信息: name={}, module={}", id, dto.getName(), dto.getModule());

        return getLogstashProcess(id);
    }

    // 私有方法

    /**
     * 获取并验证进程、机器和关联关系
     *
     * @param id Logstash进程ID
     * @param machineId 机器ID
     * @return 包含验证后实体的对象
     */
    private ValidatedEntities getAndValidateObjects(Long id, Long machineId) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        if (machineId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "机器ID不能为空");
        }

        // 检查进程是否存在
        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        // 检查机器是否存在
        MachineInfo machineInfo = machineMapper.selectById(machineId);
        if (machineInfo == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND);
        }

        // 检查进程与机器的关联关系
        LogstashMachine relation =
                logstashMachineMapper.selectByLogstashProcessIdAndMachineId(id, machineId);
        if (relation == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "指定的机器未与该进程关联");
        }

        return new ValidatedEntities(process, machineInfo, relation);
    }

    /** 删除进程物理目录 */
    private void deleteProcessDirectories(Long processId, List<MachineInfo> machineInfos) {
        try {
            CompletableFuture<Boolean> deleteFuture =
                    logstashDeployService.deleteProcessDirectory(processId, machineInfos);
            boolean success = deleteFuture.get(); // 等待删除完成
            if (!success) {
                logger.warn("删除进程物理目录出现部分失败，但会继续删除数据库记录，进程ID: {}", processId);
            } else {
                logger.info("成功删除进程物理目录，进程ID: {}", processId);
            }
        } catch (Exception e) {
            logger.error("删除进程物理目录时发生错误: {}", e.getMessage(), e);
            // 继续进行数据库记录删除
        }
    }

    /** 删除进程关联的所有任务 */
    private void deleteProcessTasks(Long processId) {
        try {
            // 获取所有与进程相关的任务
            List<String> taskIds = taskService.getAllProcessTaskIds(processId);
            if (!taskIds.isEmpty()) {
                logger.info("找到{}个与进程关联的任务，进程ID: {}", taskIds.size(), processId);

                // 删除每个任务及其步骤
                for (String taskId : taskIds) {
                    logger.info("删除进程关联的任务: {}", taskId);
                    deleteProcessTask(taskId);
                }
            } else {
                logger.info("没有找到与进程关联的任务，进程ID: {}", processId);
            }
        } catch (Exception e) {
            logger.error("删除进程相关任务时发生错误: {}", e.getMessage(), e);
            // 继续删除其他数据
        }
    }

    /** 删除进程关联的任务及步骤 */
    private void deleteProcessTask(String taskId) {
        try {
            // 先删除任务步骤
            taskService.deleteTaskSteps(taskId);
            // 再删除任务
            taskService.deleteTask(taskId);
        } catch (Exception e) {
            logger.error("删除任务失败: {}", taskId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除任务数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取并验证Logstash进程
     *
     * @param id 进程ID
     * @return 验证后的进程实体
     */
    private LogstashProcess getAndValidateProcess(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        LogstashProcess process = logstashProcessMapper.selectById(id);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        return process;
    }

    /**
     * 获取可以启动的机器列表
     *
     * @param processId 进程ID
     * @return 可启动的机器列表
     */
    private List<MachineInfo> getStartableMachines(Long processId) {
        // 获取进程对应的所有机器
        List<MachineInfo> machineInfos = getMachinesForProcess(processId);
        if (machineInfos.isEmpty()) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND, "进程未关联任何机器");
        }

        // 获取进程关联的机器状态
        List<LogstashMachine> machineRelations =
                logstashMachineMapper.selectByLogstashProcessId(processId);

        // 过滤出可以启动的机器
        List<MachineInfo> machinesToStart = new ArrayList<>();
        for (LogstashMachine relation : machineRelations) {
            // 只有 NOT_STARTED 或 START_FAILED 状态的机器可以启动
            if (LogstashMachineState.NOT_STARTED.name().equals(relation.getState())
                    || LogstashMachineState.START_FAILED.name().equals(relation.getState())) {

                Optional<MachineInfo> machine =
                        machineInfos.stream()
                                .filter(m -> m.getId().equals(relation.getMachineId()))
                                .findFirst();

                machine.ifPresent(machinesToStart::add);
            }
        }

        return machinesToStart;
    }

    /**
     * 获取可以停止的机器列表
     *
     * @param processId 进程ID
     * @return 可停止的机器列表
     */
    private List<MachineInfo> getStoppableMachines(Long processId) {
        // 获取进程对应的所有机器
        List<MachineInfo> machineInfos = getMachinesForProcess(processId);
        if (machineInfos.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程未关联任何机器");
        }

        // 获取进程关联的机器状态
        List<LogstashMachine> machineRelations =
                logstashMachineMapper.selectByLogstashProcessId(processId);

        // 过滤出可以停止的机器
        List<MachineInfo> machinesToStop = new ArrayList<>();
        for (LogstashMachine relation : machineRelations) {
            // 只有 RUNNING 或 STOP_FAILED 状态的机器可以停止
            if (LogstashMachineState.RUNNING.name().equals(relation.getState())
                    || LogstashMachineState.STOP_FAILED.name().equals(relation.getState())) {

                Optional<MachineInfo> machine =
                        machineInfos.stream()
                                .filter(m -> m.getId().equals(relation.getMachineId()))
                                .findFirst();

                machine.ifPresent(machinesToStop::add);
            }
        }

        return machinesToStop;
    }

    /** 获取进程对应的所有机器 */
    private List<MachineInfo> getMachinesForProcess(Long processId) {
        List<LogstashMachine> relations =
                logstashMachineMapper.selectByLogstashProcessId(processId);
        List<MachineInfo> machineInfos = new ArrayList<>();
        for (LogstashMachine relation : relations) {
            MachineInfo machineInfo = machineMapper.selectById(relation.getMachineId());
            if (machineInfo != null) {
                machineInfos.add(machineInfo);
            }
        }
        return machineInfos;
    }

    /** 验证配置更新DTO */
    private void validateUpdateConfigDto(LogstashProcessConfigUpdateRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "配置更新请求不能为空");
        }

        // 至少有一个配置项需要更新
        boolean hasUpdate =
                StringUtils.hasText(dto.getConfigContent())
                        || StringUtils.hasText(dto.getJvmOptions())
                        || StringUtils.hasText(dto.getLogstashYml());

        if (!hasUpdate) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "至少需要指定一项配置内容进行更新");
        }
    }

    /** 获取要更新配置的目标机器列表 */
    private List<MachineInfo> getTargetMachinesForConfig(Long processId, List<Long> machineIds) {
        List<MachineInfo> targetMachineInfos;
        if (machineIds != null && !machineIds.isEmpty()) {
            // 针对指定机器更新配置
            targetMachineInfos = new ArrayList<>();
            for (Long machineId : machineIds) {
                // 验证指定的机器是否与该进程关联
                LogstashMachine relation =
                        logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                                processId, machineId);
                if (relation == null) {
                    throw new BusinessException(
                            ErrorCode.VALIDATION_ERROR, "指定的机器未与该进程关联: 机器ID=" + machineId);
                }

                MachineInfo machineInfo = machineMapper.selectById(machineId);
                if (machineInfo == null) {
                    throw new BusinessException(
                            ErrorCode.MACHINE_NOT_FOUND, "指定的机器不存在: ID=" + machineId);
                }

                targetMachineInfos.add(machineInfo);
            }
        } else {
            // 全局更新，获取所有关联的机器
            targetMachineInfos = getMachinesForProcess(processId);
        }

        if (targetMachineInfos.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "没有找到要更新配置的目标机器");
        }

        return targetMachineInfos;
    }

    /**
     * 验证进程配置是否完整
     *
     * @param process 进程实体
     */
    private void validateProcessConfig(LogstashProcess process) {
        // 检查配置是否完整
        if (!StringUtils.hasText(process.getConfigContent())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Logstash配置不能为空");
        }

        // 检查表名是否存在
        if (!StringUtils.hasText(process.getTableName())) {
            throw new BusinessException(ErrorCode.LOGSTASH_CONFIG_TABLE_MISSING, "表名不能为空，无法启动进程");
        }

        // 检查目标数据源中是否存在该表
        if (!tableValidationService.isTableExists(
                process.getDatasourceId(), process.getTableName())) {
            throw new BusinessException(
                    ErrorCode.LOGSTASH_TARGET_TABLE_NOT_FOUND,
                    String.format(
                            "目标数据源(ID: %d)中不存在表 '%s'，请先创建表后再启动进程",
                            process.getDatasourceId(), process.getTableName()));
        }
    }

    // 内部类

    /** 用于存储验证后的实体对象 */
    private static class ValidatedEntities {
        final LogstashProcess process;
        final MachineInfo machineInfo;
        final LogstashMachine relation;

        ValidatedEntities(
                LogstashProcess process, MachineInfo machineInfo, LogstashMachine relation) {
            this.process = process;
            this.machineInfo = machineInfo;
            this.relation = relation;
        }
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO reinitializeLogstashMachine(Long id, Long machineId) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }

        // 验证进程是否存在
        LogstashProcess process = getAndValidateProcess(id);

        if (machineId != null) {
            // 重新初始化指定机器
            reinitializeSingleMachine(process, machineId);
        } else {
            // 重新初始化所有初始化失败的机器
            reinitializeAllFailedMachines(process);
        }

        return getLogstashProcess(id);
    }

    /** 重新初始化单台机器 */
    private void reinitializeSingleMachine(LogstashProcess process, Long machineId) {
        // 验证机器是否存在
        MachineInfo machineInfo = machineMapper.selectById(machineId);
        if (machineInfo == null) {
            throw new BusinessException(ErrorCode.MACHINE_NOT_FOUND, "指定的机器不存在: ID=" + machineId);
        }

        // 获取机器关联关系
        LogstashMachine logstashMachine =
                logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                        process.getId(), machineId);
        if (logstashMachine == null) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "进程与机器没有关联关系，进程ID: " + process.getId() + ", 机器ID: " + machineId);
        }

        // 检查机器状态是否为初始化失败
        if (!LogstashMachineState.INITIALIZE_FAILED.name().equals(logstashMachine.getState())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    String.format(
                            "机器[%s]当前状态为[%s]，只有初始化失败的机器才能重新初始化",
                            machineId, logstashMachine.getState()));
        }

        logger.info("开始重新初始化机器[{}]上的Logstash进程[{}]", machineId, process.getId());

        // 执行重新初始化
        logstashDeployService.initializeProcess(process, List.of(machineInfo));
    }

    /** 重新初始化所有初始化失败的机器 */
    private void reinitializeAllFailedMachines(LogstashProcess process) {
        // 获取所有关联的机器关系
        List<LogstashMachine> machineRelations =
                logstashMachineMapper.selectByLogstashProcessId(process.getId());

        // 筛选出初始化失败的机器
        List<LogstashMachine> failedMachines =
                machineRelations.stream()
                        .filter(
                                relation ->
                                        LogstashMachineState.INITIALIZE_FAILED
                                                .name()
                                                .equals(relation.getState()))
                        .collect(Collectors.toList());

        if (failedMachines.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "没有初始化失败的机器需要重新初始化");
        }

        // 获取机器详细信息
        List<MachineInfo> machinesToReinitialize = new ArrayList<>();
        for (LogstashMachine failedMachine : failedMachines) {
            MachineInfo machineInfo = machineMapper.selectById(failedMachine.getMachineId());
            if (machineInfo != null) {
                machinesToReinitialize.add(machineInfo);
            } else {
                logger.warn("机器[{}]不存在，跳过重新初始化", failedMachine.getMachineId());
            }
        }

        if (machinesToReinitialize.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "没有有效的机器可以重新初始化");
        }

        logger.info("开始重新初始化{}台初始化失败的机器", machinesToReinitialize.size());

        // 执行重新初始化
        logstashDeployService.initializeProcess(process, machinesToReinitialize);
    }

    @Override
    public LogstashMachineDetailDTO getLogstashMachineDetail(Long id, Long machineId) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }
        if (machineId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "机器ID不能为空");
        }

        // 获取并验证进程、机器和关联关系
        ValidatedEntities validated = getAndValidateObjects(id, machineId);
        LogstashProcess process = validated.process;
        MachineInfo machineInfo = validated.machineInfo;
        LogstashMachine logstashMachine = validated.relation;

        // 使用转换器构建详细信息DTO（部署路径从数据库中的LogstashMachine获取）
        return logstashMachineConverter.toDetailDTO(logstashMachine, process, machineInfo);
    }

    @Override
    @Transactional
    public LogstashProcessResponseDTO scaleLogstashProcess(
            Long id, LogstashProcessScaleRequestDTO dto) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进程ID不能为空");
        }
        if (dto == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "扩容/缩容请求不能为空");
        }

        // 验证请求参数
        try {
            dto.validate();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }

        // 验证进程是否存在
        LogstashProcess process = getAndValidateProcess(id);

        if (dto.isScaleOut()) {
            // 扩容操作
            return performScaleOut(process, dto);
        } else if (dto.isScaleIn()) {
            // 缩容操作
            return performScaleIn(process, dto);
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "无效的扩容/缩容操作");
        }
    }

    /** 执行扩容操作 */
    private LogstashProcessResponseDTO performScaleOut(
            LogstashProcess process, LogstashProcessScaleRequestDTO dto) {
        List<Long> addMachineIds = dto.getAddMachineIds();
        Long processId = process.getId();

        logger.info("开始扩容Logstash进程[{}]，添加{}台机器", processId, addMachineIds.size());

        // 验证要添加的机器
        List<MachineInfo> newMachineInfos =
                validateAndGetMachinesForScaleOut(processId, addMachineIds);

        // 确定部署路径
        String deployPath;
        if (StringUtils.hasText(dto.getCustomDeployPath())) {
            // 用户指定了部署路径，直接使用，不拼接
            deployPath = dto.getCustomDeployPath();
        } else {
            // 使用默认路径并拼接进程ID
            String baseDeployPath = logstashDeployService.getDeployBaseDir();
            deployPath = String.format("%s/logstash-%d", baseDeployPath, processId);
        }

        // 创建新的LogstashMachine关联关系
        for (MachineInfo machineInfo : newMachineInfos) {
            LogstashMachine logstashMachine =
                    logstashMachineConverter.createFromProcess(
                            process, machineInfo.getId(), deployPath);
            logstashMachineMapper.insert(logstashMachine);
            logger.info("已创建进程[{}]与机器[{}]的关联关系", processId, machineInfo.getId());
        }

        // 在新机器上初始化Logstash进程环境
        logstashDeployService.initializeProcess(process, newMachineInfos);

        logger.info("Logstash进程[{}]扩容完成，成功添加{}台机器", processId, newMachineInfos.size());
        return getLogstashProcess(processId);
    }

    /** 执行缩容操作 */
    private LogstashProcessResponseDTO performScaleIn(
            LogstashProcess process, LogstashProcessScaleRequestDTO dto) {
        List<Long> removeMachineIds = dto.getRemoveMachineIds();
        Long processId = process.getId();
        Boolean forceScale = dto.getForceScale();

        logger.info(
                "开始缩容Logstash进程[{}]，移除{}台机器，强制模式：{}",
                processId,
                removeMachineIds.size(),
                forceScale);

        // 验证要移除的机器和防御性检查
        List<MachineInfo> removeMachineInfos =
                validateAndGetMachinesForScaleIn(processId, removeMachineIds, forceScale);

        // 如果有运行中的机器且非强制模式，则拒绝操作
        if (!forceScale) {
            validateMachinesNotRunningForScaleIn(processId, removeMachineIds);
        } else {
            // 强制模式：先停止运行中的机器
            stopRunningMachinesForScaleIn(processId, removeMachineInfos);
        }

        // 删除机器上的进程目录
        deleteProcessDirectoriesForMachines(processId, removeMachineInfos);

        // 删除与进程相关的任务（针对特定机器）
        deleteTasksForMachines(processId, removeMachineIds);

        // 删除LogstashMachine关联关系
        for (Long machineId : removeMachineIds) {
            LogstashMachine relation =
                    logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                            processId, machineId);
            if (relation != null) {
                logstashMachineMapper.deleteById(relation.getId());
                logger.info("已删除进程[{}]与机器[{}]的关联关系", processId, machineId);
            }
        }

        logger.info("Logstash进程[{}]缩容完成，成功移除{}台机器", processId, removeMachineIds.size());
        return getLogstashProcess(processId);
    }

    /** 验证并获取扩容的目标机器 */
    private List<MachineInfo> validateAndGetMachinesForScaleOut(
            Long processId, List<Long> addMachineIds) {
        List<MachineInfo> newMachineInfos = new ArrayList<>();

        for (Long machineId : addMachineIds) {
            // 验证机器是否存在
            MachineInfo machineInfo = machineMapper.selectById(machineId);
            if (machineInfo == null) {
                throw new BusinessException(
                        ErrorCode.MACHINE_NOT_FOUND, "指定的机器不存在: ID=" + machineId);
            }

            // 验证机器是否已经与该进程关联
            LogstashMachine existingRelation =
                    logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                            processId, machineId);
            if (existingRelation != null) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "机器[" + machineId + "]已经与进程[" + processId + "]关联，无法重复添加");
            }

            // 验证机器连接
            connectionValidator.validateSingleMachineConnection(machineInfo);

            newMachineInfos.add(machineInfo);
        }

        if (newMachineInfos.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "没有有效的机器可以添加到进程中");
        }

        return newMachineInfos;
    }

    /** 验证并获取缩容的目标机器 */
    private List<MachineInfo> validateAndGetMachinesForScaleIn(
            Long processId, List<Long> removeMachineIds, Boolean forceScale) {
        List<MachineInfo> removeMachineInfos = new ArrayList<>();

        // 防御性编程：确保缩容后至少保留一台机器
        List<LogstashMachine> allRelations =
                logstashMachineMapper.selectByLogstashProcessId(processId);
        if (allRelations.size() <= removeMachineIds.size()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "缩容后必须至少保留一台机器，当前共"
                            + allRelations.size()
                            + "台机器，不能移除"
                            + removeMachineIds.size()
                            + "台");
        }

        for (Long machineId : removeMachineIds) {
            // 验证机器是否存在
            MachineInfo machineInfo = machineMapper.selectById(machineId);
            if (machineInfo == null) {
                throw new BusinessException(
                        ErrorCode.MACHINE_NOT_FOUND, "指定的机器不存在: ID=" + machineId);
            }

            // 验证机器是否与该进程关联
            LogstashMachine relation =
                    logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                            processId, machineId);
            if (relation == null) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "机器[" + machineId + "]未与进程[" + processId + "]关联，无法移除");
            }

            removeMachineInfos.add(machineInfo);
        }

        return removeMachineInfos;
    }

    /** 验证要缩容的机器不在运行状态 */
    private void validateMachinesNotRunningForScaleIn(Long processId, List<Long> removeMachineIds) {
        for (Long machineId : removeMachineIds) {
            LogstashMachine relation =
                    logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                            processId, machineId);
            if (relation != null) {
                LogstashMachineState state = LogstashMachineState.valueOf(relation.getState());
                if (state == LogstashMachineState.RUNNING
                        || state == LogstashMachineState.STARTING) {
                    throw new BusinessException(
                            ErrorCode.VALIDATION_ERROR,
                            "机器["
                                    + machineId
                                    + "]当前状态为["
                                    + state.getDescription()
                                    + "]，不能在非强制模式下缩容运行中的机器");
                }
            }
        }
    }

    /** 停止运行中的机器（强制缩容模式） */
    private void stopRunningMachinesForScaleIn(
            Long processId, List<MachineInfo> removeMachineInfos) {
        List<MachineInfo> runningMachines = new ArrayList<>();

        for (MachineInfo machineInfo : removeMachineInfos) {
            LogstashMachine relation =
                    logstashMachineMapper.selectByLogstashProcessIdAndMachineId(
                            processId, machineInfo.getId());
            if (relation != null) {
                LogstashMachineState state = LogstashMachineState.valueOf(relation.getState());
                if (state == LogstashMachineState.RUNNING
                        || state == LogstashMachineState.STARTING) {
                    runningMachines.add(machineInfo);
                }
            }
        }

        if (!runningMachines.isEmpty()) {
            logger.info("强制缩容模式：停止{}台运行中的机器", runningMachines.size());
            logstashDeployService.stopProcess(processId, runningMachines);

            // 等待停止完成（简单的同步等待，可以优化为异步等待）
            try {
                Thread.sleep(5000); // 等待5秒让停止操作完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("等待停止操作被中断");
            }
        }
    }

    /** 删除指定机器上的进程目录 */
    private void deleteProcessDirectoriesForMachines(
            Long processId, List<MachineInfo> machineInfos) {
        // 直接调用部署服务的删除方法，复用现有逻辑
        try {
            CompletableFuture<Boolean> deleteFuture =
                    logstashDeployService.deleteProcessDirectory(processId, machineInfos);
            boolean success = deleteFuture.get(); // 等待删除完成
            if (!success) {
                logger.warn("删除部分机器上的进程目录失败，进程ID: {}，机器数量: {}", processId, machineInfos.size());
            } else {
                logger.info("成功删除{}台机器上的进程[{}]目录", machineInfos.size(), processId);
            }
        } catch (Exception e) {
            logger.error("删除指定机器上的进程目录时发生错误，进程ID: {}, 错误: {}", processId, e.getMessage(), e);
        }
    }

    /** 删除指定机器的任务记录 */
    private void deleteTasksForMachines(Long processId, List<Long> machineIds) {
        for (Long machineId : machineIds) {
            try {
                List<String> taskIds = taskService.getAllMachineTaskIds(processId, machineId);
                for (String taskId : taskIds) {
                    try {
                        // 先删除任务步骤再删除任务
                        taskService.deleteTaskSteps(taskId);
                        taskService.deleteTask(taskId);
                        logger.debug("已删除任务: {}", taskId);
                    } catch (Exception e) {
                        logger.error("删除任务[{}]失败: {}", taskId, e.getMessage(), e);
                    }
                }
                logger.info("已删除进程[{}]在机器[{}]上的{}个任务记录", processId, machineId, taskIds.size());
            } catch (Exception e) {
                logger.error(
                        "删除进程[{}]在机器[{}]上的任务记录时发生异常: {}", processId, machineId, e.getMessage(), e);
            }
        }
    }
}
