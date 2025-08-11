package com.hinadt.miaocha.integration.logstash.support;

import com.hinadt.miaocha.application.logstash.LogstashProcessDeployService;
import com.hinadt.miaocha.application.logstash.path.LogstashPathUtils;
import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.infrastructure.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.infrastructure.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.infrastructure.mapper.MachineMapper;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Logstash配置验证器
 *
 * <p>提供配置验证功能：
 *
 * <ul>
 *   <li>数据库配置验证
 *   <li>远程文件配置验证（支持异步重试）
 *   <li>配置同步任务验证
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogstashConfigurationVerifier {

    private final LogstashProcessMapper processMapper;
    private final LogstashMachineMapper machineMapper;
    private final MachineMapper machineInfoMapper;
    private final LogstashProcessDeployService deployService;
    private final SshClient sshClient;

    // ==================== 数据库配置验证 ====================

    /** 验证进程配置更新 */
    public boolean verifyProcessUpdate(
            Long processId, String expectedConfig, String expectedJvm, String expectedYml) {
        LogstashProcess process = processMapper.selectById(processId);
        if (process == null) {
            log.error("进程不存在: {}", processId);
            return false;
        }

        boolean valid =
                validateConfigContent("进程主配置", expectedConfig, process.getConfigContent())
                        && validateConfigContent("进程JVM配置", expectedJvm, process.getJvmOptions())
                        && validateConfigContent("进程系统配置", expectedYml, process.getLogstashYml());

        if (valid) {
            log.info("✅ 进程 {} 配置验证通过", processId);
        }
        return valid;
    }

    /** 验证所有实例与进程配置同步 */
    public boolean verifyInstancesSync(Long processId) {
        LogstashProcess process = processMapper.selectById(processId);
        if (process == null) {
            log.error("进程不存在: {}", processId);
            return false;
        }

        List<LogstashMachine> instances = machineMapper.selectByLogstashProcessId(processId);
        if (instances.isEmpty()) {
            log.error("进程 {} 没有关联实例", processId);
            return false;
        }

        boolean allValid =
                instances.stream()
                        .allMatch(
                                instance ->
                                        verifyInstanceDatabase(
                                                instance,
                                                process.getConfigContent(),
                                                process.getJvmOptions(),
                                                process.getLogstashYml()));

        if (allValid) {
            log.info("✅ 进程 {} 所有实例配置同步验证通过", processId);
        }
        return allValid;
    }

    /** 验证单个实例数据库配置 */
    public boolean verifyInstanceDatabase(
            LogstashMachine instance,
            String expectedConfig,
            String expectedJvm,
            String expectedYml) {
        boolean valid =
                validateConfigContent("实例主配置", expectedConfig, instance.getConfigContent())
                        && validateConfigContent("实例JVM配置", expectedJvm, instance.getJvmOptions())
                        && validateConfigContent("实例系统配置", expectedYml, instance.getLogstashYml());

        if (valid) {
            log.debug("✅ 实例 {} 数据库配置验证通过", instance.getId());
        }
        return valid;
    }

    // ==================== 远程文件配置验证 ====================

    /** 验证实例远程配置文件（默认重试策略） */
    public boolean verifyInstanceRemoteFiles(
            LogstashMachine instance,
            String expectedConfig,
            String expectedJvm,
            String expectedYml) {
        return verifyInstanceRemoteFiles(instance, expectedConfig, expectedJvm, expectedYml, 10, 3);
    }

    /** 验证实例远程配置文件（自定义重试策略） */
    public boolean verifyInstanceRemoteFiles(
            LogstashMachine instance,
            String expectedConfig,
            String expectedJvm,
            String expectedYml,
            int maxRetries,
            int intervalSeconds) {
        MachineInfo machineInfo = machineInfoMapper.selectById(instance.getMachineId());
        if (machineInfo == null) {
            log.error("找不到机器信息: {}", instance.getMachineId());
            return false;
        }

        String deployPath = deployService.getInstanceDeployPath(instance);

        return retryUntilSuccess(
                () -> {
                    boolean valid = true;

                    // 验证主配置文件
                    if (expectedConfig != null) {
                        String configPath =
                                LogstashPathUtils.buildConfigFilePath(deployPath, instance.getId());
                        valid &= verifyRemoteFile(machineInfo, configPath, expectedConfig, "主配置");
                    }

                    // 验证JVM配置文件
                    if (expectedJvm != null) {
                        String jvmPath = LogstashPathUtils.buildJvmOptionsPath(deployPath);
                        valid &= verifyRemoteFile(machineInfo, jvmPath, expectedJvm, "JVM配置");
                    }

                    // 验证系统配置文件
                    if (expectedYml != null) {
                        String ymlPath = LogstashPathUtils.buildLogstashYmlPath(deployPath);
                        valid &= verifyRemoteFile(machineInfo, ymlPath, expectedYml, "系统配置");
                    }

                    return valid;
                },
                maxRetries,
                intervalSeconds,
                "实例 " + instance.getId() + " 远程配置文件验证");
    }

    // ==================== 配置同步任务验证 ====================

    /** 验证配置回填任务完成 */
    public boolean verifyConfigSyncTask(Long processId, int maxRetries, int intervalSeconds) {
        boolean syncCompleted =
                retryUntilSuccess(
                        () -> {
                            LogstashProcess process = processMapper.selectById(processId);
                            if (process == null) {
                                return false;
                            }

                            // 检查JVM和系统配置是否已回填
                            boolean hasJvm = StringUtils.hasText(process.getJvmOptions());
                            boolean hasYml = StringUtils.hasText(process.getLogstashYml());

                            if (hasJvm && hasYml) {
                                log.info(
                                        "配置回填完成 - JVM配置长度: {}, 系统配置长度: {}",
                                        process.getJvmOptions().length(),
                                        process.getLogstashYml().length());
                                return true;
                            }
                            return false;
                        },
                        maxRetries,
                        intervalSeconds,
                        "配置回填任务");

        if (!syncCompleted) {
            return false;
        }

        // 验证实例级配置同步
        List<LogstashMachine> instances = machineMapper.selectByLogstashProcessId(processId);
        boolean allInstancesSync =
                instances.stream()
                        .allMatch(
                                instance ->
                                        StringUtils.hasText(instance.getJvmOptions())
                                                && StringUtils.hasText(instance.getLogstashYml()));

        if (!allInstancesSync) {
            log.error("部分实例配置同步失败");
            return false;
        }

        log.info("✅ 配置回填任务验证通过");
        return true;
    }

    /** 验证完整配置更新（数据库 + 远程文件） */
    public boolean verifyCompleteUpdate(
            Long processId, String expectedConfig, String expectedJvm, String expectedYml) {
        // 1. 验证数据库更新
        boolean dbValid =
                retryUntilSuccess(
                        () ->
                                verifyProcessUpdate(
                                                processId, expectedConfig, expectedJvm, expectedYml)
                                        && verifyInstancesSync(processId),
                        5,
                        3,
                        "数据库配置更新");

        if (!dbValid) {
            log.error("数据库配置更新验证失败");
            return false;
        }

        // 2. 验证远程文件更新（异步，需要更长等待时间）
        List<LogstashMachine> instances = machineMapper.selectByLogstashProcessId(processId);
        boolean allRemoteValid =
                instances.stream()
                        .allMatch(
                                instance ->
                                        verifyInstanceRemoteFiles(
                                                instance,
                                                expectedConfig,
                                                expectedJvm,
                                                expectedYml,
                                                20,
                                                3));

        if (allRemoteValid) {
            log.info("✅ 完整配置更新验证通过");
        }
        return allRemoteValid;
    }

    // ==================== 测试辅助方法 ====================

    /** 模拟配置不一致场景 */
    public boolean simulateConfigMismatch(LogstashMachine instance, String mismatchContent) {
        MachineInfo machineInfo = machineInfoMapper.selectById(instance.getMachineId());
        if (machineInfo == null) {
            log.error("找不到机器信息: {}", instance.getMachineId());
            return false;
        }

        try {
            String deployPath = deployService.getInstanceDeployPath(instance);
            String configPath = LogstashPathUtils.buildConfigFilePath(deployPath, instance.getId());

            // 写入不一致的配置内容
            String tempFile = "/tmp/mismatch-config-" + System.currentTimeMillis() + ".conf";
            String createCmd =
                    String.format("cat > %s << 'EOF'\n%s\nEOF", tempFile, mismatchContent);
            sshClient.executeCommand(machineInfo, createCmd);

            String moveCmd = String.format("mv %s %s", tempFile, configPath);
            sshClient.executeCommand(machineInfo, moveCmd);

            log.info("✅ 成功模拟实例 {} 配置不一致", instance.getId());
            return true;
        } catch (Exception e) {
            log.error("模拟配置不一致失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /** 验证配置刷新效果 */
    public boolean verifyConfigRefresh(LogstashMachine instance, String expectedConfig) {
        return verifyInstanceRemoteFiles(instance, expectedConfig, null, null, 12, 2);
    }

    // ==================== 私有辅助方法 ====================

    /** 验证配置内容匹配（兼容换行符差异） */
    private boolean validateConfigContent(String configType, String expected, String actual) {
        if (expected == null) {
            return true; // 不需要验证
        }

        if (actual == null) {
            log.error("{} 不匹配 - 期望非空，实际为空", configType);
            return false;
        }

        // 规范化内容，去除首尾空白字符
        String normalizedExpected = expected.trim();
        String normalizedActual = actual.trim();

        if (!normalizedExpected.equals(normalizedActual)) {
            log.error(
                    "{} 不匹配 - 期望长度: {}, 实际长度: {}",
                    configType,
                    normalizedExpected.length(),
                    normalizedActual.length());

            // 详细比较前几行，帮助调试
            String[] expectedLines = normalizedExpected.split("\n", 5);
            String[] actualLines = normalizedActual.split("\n", 5);

            log.debug("{} 详细对比:", configType);
            log.debug("期望前几行: {}", String.join(" | ", expectedLines));
            log.debug("实际前几行: {}", String.join(" | ", actualLines));

            return false;
        }
        return true;
    }

    /** 验证远程文件内容（兼容换行符差异） */
    private boolean verifyRemoteFile(
            MachineInfo machineInfo, String filePath, String expectedContent, String fileType) {
        try {
            // 检查文件存在
            String checkCmd =
                    String.format("[ -f %s ] && echo 'exists' || echo 'not_exists'", filePath);
            String checkResult = sshClient.executeCommand(machineInfo, checkCmd);

            if (!"exists".equals(checkResult.trim())) {
                log.debug("{} 文件不存在: {}", fileType, filePath);
                return false;
            }

            // 读取并比较内容
            String catCmd = String.format("cat %s", filePath);
            String actualContent = sshClient.executeCommand(machineInfo, catCmd);

            // 规范化内容，去除首尾空白字符
            String normalizedExpected = expectedContent.trim();
            String normalizedActual = actualContent.trim();

            if (!normalizedExpected.equals(normalizedActual)) {
                log.debug("{} 文件内容不匹配: {}", fileType, filePath);
                log.debug(
                        "期望内容长度: {}, 实际内容长度: {}",
                        normalizedExpected.length(),
                        normalizedActual.length());

                // 只在DEBUG模式下显示前100个字符，避免日志过长
                if (log.isDebugEnabled()) {
                    String expectedPreview =
                            normalizedExpected.length() > 100
                                    ? normalizedExpected.substring(0, 100) + "..."
                                    : normalizedExpected;
                    String actualPreview =
                            normalizedActual.length() > 100
                                    ? normalizedActual.substring(0, 100) + "..."
                                    : normalizedActual;
                    log.debug("期望内容: [{}]", expectedPreview);
                    log.debug("实际内容: [{}]", actualPreview);
                }
                return false;
            }

            return true;
        } catch (Exception e) {
            log.debug("验证 {} 文件失败: {}", fileType, e.getMessage());
            return false;
        }
    }

    /** 通用重试逻辑 */
    private boolean retryUntilSuccess(
            Supplier<Boolean> operation,
            int maxRetries,
            int intervalSeconds,
            String operationName) {
        for (int retry = 1; retry <= maxRetries; retry++) {
            try {
                if (operation.get()) {
                    if (retry > 1) {
                        log.info("✅ {} 验证通过 (第{}次尝试)", operationName, retry);
                    }
                    return true;
                }

                if (retry < maxRetries) {
                    log.debug(
                            "🔄 {} 验证失败 (第{}/{}次)，{}秒后重试...",
                            operationName,
                            retry,
                            maxRetries,
                            intervalSeconds);
                    Thread.sleep(intervalSeconds * 1000L);
                } else {
                    log.error("❌ {} 验证最终失败，已重试{}次", operationName, maxRetries);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("{} 验证被中断", operationName);
                return false;
            } catch (Exception e) {
                log.error("{} 验证异常: {}", operationName, e.getMessage());
                return false;
            }
        }
        return false;
    }
}
