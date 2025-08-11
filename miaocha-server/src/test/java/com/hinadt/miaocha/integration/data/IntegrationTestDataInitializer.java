package com.hinadt.miaocha.integration.data;

import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.domain.entity.ModuleInfo;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.infrastructure.mapper.DatasourceMapper;
import com.hinadt.miaocha.infrastructure.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.infrastructure.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.infrastructure.mapper.MachineMapper;
import com.hinadt.miaocha.infrastructure.mapper.ModuleInfoMapper;
import com.hinadt.miaocha.infrastructure.mapper.UserMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 集成测试数据初始化器 在Spring Boot启动并完成Flyway迁移后，插入测试所需的基础数据 使用Mapper层进行数据操作，符合项目架构设计 */
@Slf4j
@Component
public class IntegrationTestDataInitializer {

    @Autowired private UserMapper userMapper;

    @Autowired private MachineMapper machineMapper;

    @Autowired private DatasourceMapper datasourceMapper;

    @Autowired private ModuleInfoMapper moduleInfoMapper;

    @Autowired private LogstashProcessMapper logstashProcessMapper;

    @Autowired private LogstashMachineMapper logstashMachineMapper;

    /** 初始化集成测试所需的基础数据 此方法应在Flyway迁移完成后调用 */
    @Transactional
    public void initializeTestData() {
        log.info("开始初始化集成测试数据...");

        try {
            // 1. 创建测试用户
            User testUser = createTestUser();
            log.debug("创建测试用户: {}", testUser.getNickname());

            // 2. 创建测试机器
            MachineInfo testMachine = createTestMachine();
            log.debug("创建测试机器: {}", testMachine.getName());

            // 3. 创建测试数据源
            DatasourceInfo testDatasource = createTestDatasource();
            log.debug("创建测试数据源: {}", testDatasource.getName());

            // 4. 创建测试模块
            ModuleInfo testModule = createTestModule(testDatasource);
            log.debug("创建测试模块: {}", testModule.getName());

            // 5. 创建测试 Logstash 进程定义
            LogstashProcess testProcess = createTestLogstashProcess(testModule);
            log.debug("创建测试 Logstash 进程: {}", testProcess.getName());

            // 6. 创建测试 Logstash 实例记录
            LogstashMachine testLogstashMachine =
                    createTestLogstashMachine(testProcess, testMachine);
            log.debug("创建测试 Logstash 实例: ID={}", testLogstashMachine.getId());

            log.info("集成测试数据初始化完成");

        } catch (Exception e) {
            log.error("初始化集成测试数据失败", e);
            throw new RuntimeException("集成测试数据初始化失败", e);
        }
    }

    private User createTestUser() {
        // 检查是否已存在，避免重复插入
        User existingUser = userMapper.selectByUid("test_admin_uid");
        if (existingUser != null) {
            return existingUser;
        }

        User user = new User();
        user.setNickname("test_admin");
        user.setEmail("test@example.com");
        user.setUid("test_admin_uid");
        user.setRole("ADMIN");
        user.setStatus(1); // 启用状态

        userMapper.insert(user);
        return user;
    }

    private MachineInfo createTestMachine() {
        // 检查是否已存在，避免重复插入
        MachineInfo existingMachine = machineMapper.selectByName("test-ssh-machine");
        if (existingMachine != null) {
            return existingMachine;
        }

        MachineInfo machine = new MachineInfo();
        machine.setName("test-ssh-machine");
        machine.setIp("ssh-server");
        machine.setPort(2222);
        machine.setUsername("testuser");
        machine.setPassword("testpassword");
        machine.setCreateUser("test_admin");
        machine.setUpdateUser("test_admin");

        machineMapper.insert(machine);
        return machine;
    }

    private DatasourceInfo createTestDatasource() {
        // 检查是否已存在，避免重复插入
        DatasourceInfo existingDatasource = datasourceMapper.selectByName("test-datasource");
        if (existingDatasource != null) {
            return existingDatasource;
        }

        DatasourceInfo datasource = new DatasourceInfo();
        datasource.setName("test-datasource");
        datasource.setType("MYSQL");
        datasource.setJdbcUrl("jdbc:mysql://mysql-db:3306/miaocha_integration_test");
        datasource.setUsername("test_user");
        datasource.setPassword("test_password");
        datasource.setCreateUser("test_admin");
        datasource.setUpdateUser("test_admin");

        datasourceMapper.insert(datasource);
        return datasource;
    }

    private ModuleInfo createTestModule(DatasourceInfo datasource) {
        // 检查是否已存在，避免重复插入
        ModuleInfo existingModule = moduleInfoMapper.selectByName("test-module");
        if (existingModule != null) {
            return existingModule;
        }

        ModuleInfo module = new ModuleInfo();
        module.setName("test-module");
        module.setDatasourceId(datasource.getId());
        module.setTableName("test_table");
        module.setDorisSql("SELECT * FROM test_table");
        module.setCreateUser("test_admin");
        module.setUpdateUser("test_admin");
        module.setStatus(1);

        moduleInfoMapper.insert(module);
        return module;
    }

    private LogstashProcess createTestLogstashProcess(ModuleInfo module) {
        // 检查是否已存在，避免重复插入
        LogstashProcess existingProcess =
                logstashProcessMapper.selectByName("test-logstash-process");
        if (existingProcess != null) {
            return existingProcess;
        }

        String configContent =
                "input {\n"
                        + "  beats {\n"
                        + "    port => 5044\n"
                        + "  }\n"
                        + "}\n\n"
                        + "filter {\n"
                        + "  # 测试过滤器配置\n"
                        + "  mutate {\n"
                        + "    add_field => { \"test_field\" => \"integration_test\" }\n"
                        + "  }\n"
                        + "}\n\n"
                        + "output {\n"
                        + "  stdout {\n"
                        + "    codec => rubydebug\n"
                        + "  }\n"
                        + "}";

        LogstashProcess process = new LogstashProcess();
        process.setName("test-logstash-process");
        process.setModuleId(module.getId());
        process.setConfigContent(configContent);
        process.setJvmOptions("-Xms512m -Xmx1g");
        process.setLogstashYml("path.data: /tmp/logstash-data\nlog.level: info");
        process.setCreateUser("test_admin");
        process.setUpdateUser("test_admin");

        logstashProcessMapper.insert(process);
        return process;
    }

    private LogstashMachine createTestLogstashMachine(
            LogstashProcess process, MachineInfo machine) {
        // 检查是否已存在，避免重复插入（根据进程ID和机器ID组合）
        String deployPath = "/opt/logstash-test/instance-" + process.getId();
        LogstashMachine existingMachine =
                logstashMachineMapper.selectByMachineAndPath(machine.getId(), deployPath);
        if (existingMachine != null) {
            return existingMachine;
        }

        LogstashMachine logstashMachine = new LogstashMachine();
        logstashMachine.setLogstashProcessId(process.getId());
        logstashMachine.setMachineId(machine.getId());
        logstashMachine.setState(LogstashMachineState.NOT_STARTED.name());
        logstashMachine.setConfigContent(process.getConfigContent());
        logstashMachine.setJvmOptions(process.getJvmOptions());
        logstashMachine.setLogstashYml(process.getLogstashYml());
        logstashMachine.setDeployPath(deployPath);
        logstashMachine.setCreateUser("test_admin");
        logstashMachine.setUpdateUser("test_admin");

        logstashMachineMapper.insert(logstashMachine);
        return logstashMachine;
    }

    /** 清理测试数据 */
    @Transactional
    public void cleanupTestData() {
        log.info("开始清理集成测试数据...");

        try {
            // 按照外键依赖关系的反序删除

            // 1. 删除 Logstash 实例记录
            cleanupLogstashMachines();

            // 2. 删除 Logstash 进程
            cleanupLogstashProcesses();

            // 3. 删除模块信息
            cleanupModules();

            // 4. 删除数据源
            cleanupDatasources();

            // 5. 删除机器信息
            cleanupMachines();

            // 6. 删除测试用户
            cleanupUsers();

            log.info("集成测试数据清理完成");

        } catch (Exception e) {
            log.error("清理集成测试数据失败", e);
            // 清理失败不抛异常，避免影响测试
        }
    }

    private void cleanupLogstashMachines() {
        try {
            LogstashProcess process = logstashProcessMapper.selectByName("test-logstash-process");
            if (process != null) {
                List<LogstashMachine> machines =
                        logstashMachineMapper.selectByLogstashProcessId(process.getId());
                for (LogstashMachine machine : machines) {
                    logstashMachineMapper.deleteById(machine.getId());
                    log.debug("删除测试 Logstash 实例: ID={}", machine.getId());
                }
            }
        } catch (Exception e) {
            log.warn("清理 LogstashMachine 数据时出错: {}", e.getMessage());
        }
    }

    private void cleanupLogstashProcesses() {
        try {
            LogstashProcess process = logstashProcessMapper.selectByName("test-logstash-process");
            if (process != null) {
                logstashProcessMapper.deleteById(process.getId());
                log.debug("删除测试 Logstash 进程: {}", process.getName());
            }
        } catch (Exception e) {
            log.warn("清理 LogstashProcess 数据时出错: {}", e.getMessage());
        }
    }

    private void cleanupModules() {
        try {
            ModuleInfo module = moduleInfoMapper.selectByName("test-module");
            if (module != null) {
                moduleInfoMapper.deleteById(module.getId());
                log.debug("删除测试模块: {}", module.getName());
            }
        } catch (Exception e) {
            log.warn("清理 ModuleInfo 数据时出错: {}", e.getMessage());
        }
    }

    private void cleanupDatasources() {
        try {
            DatasourceInfo datasource = datasourceMapper.selectByName("test-datasource");
            if (datasource != null) {
                datasourceMapper.deleteById(datasource.getId());
                log.debug("删除测试数据源: {}", datasource.getName());
            }
        } catch (Exception e) {
            log.warn("清理 DatasourceInfo 数据时出错: {}", e.getMessage());
        }
    }

    private void cleanupMachines() {
        try {
            MachineInfo machine = machineMapper.selectByName("test-ssh-machine");
            if (machine != null) {
                machineMapper.deleteById(machine.getId());
                log.debug("删除测试机器: {}", machine.getName());
            }
        } catch (Exception e) {
            log.warn("清理 MachineInfo 数据时出错: {}", e.getMessage());
        }
    }

    private void cleanupUsers() {
        try {
            User user = userMapper.selectByUid("test_admin_uid");
            if (user != null) {
                userMapper.deleteById(user.getId());
                log.debug("删除测试用户: {}", user.getNickname());
            }
        } catch (Exception e) {
            log.warn("清理 User 数据时出错: {}", e.getMessage());
        }
    }
}
