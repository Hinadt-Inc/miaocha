package com.hinadt.miaocha.integration.logstash.support;

import com.hinadt.miaocha.application.logstash.path.LogstashPathUtils;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.infrastructure.ssh.SshClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Logstash进程验证器
 *
 * <p>职责： 1. 验证文件系统状态（目录、配置文件） 2. 验证进程运行状态（PID、进程存在性） 3. 验证进程停止和清理状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogstashProcessVerifier {

    private final SshClient sshClient;

    /** 验证实例目录结构是否正确 */
    public boolean verifyInstanceDirectoryStructure(
            LogstashMachine instance, List<MachineInfo> testMachines) {
        try {
            MachineInfo machine = findMachineById(testMachines, instance.getMachineId());
            String deployPath = instance.getDeployPath();

            // 验证主目录存在
            if (!directoryExists(machine, deployPath)) {
                log.error("主目录不存在: {}", deployPath);
                return false;
            }

            // 验证关键子目录存在
            String[] expectedDirs = {"bin", "config"};
            for (String dir : expectedDirs) {
                String dirPath = deployPath + "/" + dir;
                if (!directoryExists(machine, dirPath)) {
                    log.error("子目录不存在: {}", dirPath);
                    return false;
                }
            }

            // 验证配置文件存在
            String configPath = LogstashPathUtils.buildConfigFilePath(deployPath, instance.getId());
            if (!fileExists(machine, configPath)) {
                log.error("配置文件不存在: {}", configPath);
                return false;
            }

            log.info("✅ 实例目录结构验证通过，实例ID: {}", instance.getId());
            return true;
        } catch (Exception e) {
            log.error("验证实例目录结构时发生错误，实例ID: {}", instance.getId(), e);
            return false;
        }
    }

    /** 验证进程真实运行状态 */
    public boolean verifyProcessActuallyRunning(
            LogstashMachine instance, List<MachineInfo> testMachines) {
        try {
            MachineInfo machine = findMachineById(testMachines, instance.getMachineId());
            String pidPath =
                    LogstashPathUtils.buildPidFilePath(instance.getDeployPath(), instance.getId());

            // 验证PID文件存在
            if (!fileExists(machine, pidPath)) {
                log.error("PID文件不存在: {}", pidPath);
                return false;
            }

            // 读取PID并验证进程运行
            String pid = readFile(machine, pidPath);
            if (pid == null || pid.trim().isEmpty()) {
                log.error("PID文件为空: {}", pidPath);
                return false;
            }

            // 验证进程确实在运行
            if (!processRunning(machine, pid.trim())) {
                log.error("进程未运行，PID: {}", pid.trim());
                return false;
            }

            log.info("✅ 进程真实运行状态验证通过，实例ID: {}, PID: {}", instance.getId(), pid.trim());
            return true;
        } catch (Exception e) {
            log.error("验证进程运行状态时发生错误，实例ID: {}", instance.getId(), e);
            return false;
        }
    }

    /** 验证进程确实已停止 */
    public boolean verifyProcessStopped(LogstashMachine instance, List<MachineInfo> testMachines) {
        try {
            MachineInfo machine = findMachineById(testMachines, instance.getMachineId());
            String pidPath =
                    LogstashPathUtils.buildPidFilePath(instance.getDeployPath(), instance.getId());

            // 如果PID文件存在，验证进程不在运行
            if (fileExists(machine, pidPath)) {
                String pid = readFile(machine, pidPath);
                if (pid != null && !pid.trim().isEmpty()) {
                    if (processRunning(machine, pid.trim())) {
                        log.error("进程仍在运行，实例ID: {}, PID: {}", instance.getId(), pid.trim());
                        return false;
                    }
                }
            }

            log.info("✅ 进程停止状态验证通过，实例ID: {}", instance.getId());
            return true;
        } catch (Exception e) {
            log.error("验证进程停止状态时发生错误，实例ID: {}", instance.getId(), e);
            return false;
        }
    }

    /** 验证实例目录被清理 */
    public boolean verifyInstanceDirectoryCleanup(
            LogstashMachine instance, List<MachineInfo> testMachines) {
        try {
            MachineInfo machine = findMachineById(testMachines, instance.getMachineId());
            String deployPath = instance.getDeployPath();

            // 等待一段时间确保异步删除完成
            Thread.sleep(2000);

            if (directoryExists(machine, deployPath)) {
                log.error("实例目录未被清理: {}", deployPath);
                return false;
            }

            log.info("✅ 实例目录清理验证通过，实例ID: {}", instance.getId());
            return true;
        } catch (Exception e) {
            log.error("验证实例目录清理时发生错误，实例ID: {}", instance.getId(), e);
            return false;
        }
    }

    // ==================== 私有辅助方法 ====================

    private MachineInfo findMachineById(List<MachineInfo> testMachines, Long machineId) {
        return testMachines.stream()
                .filter(m -> m.getId().equals(machineId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("找不到机器ID: " + machineId));
    }

    private boolean directoryExists(MachineInfo machine, String path) {
        try {
            String command =
                    String.format("[ -d \"%s\" ] && echo \"exists\" || echo \"not_exists\"", path);
            String result = sshClient.executeCommand(machine, command);
            return "exists".equals(result.trim());
        } catch (Exception e) {
            log.warn("检查目录存在性失败: {}", path, e);
            return false;
        }
    }

    private boolean fileExists(MachineInfo machine, String path) {
        try {
            String command =
                    String.format("[ -f \"%s\" ] && echo \"exists\" || echo \"not_exists\"", path);
            String result = sshClient.executeCommand(machine, command);
            return "exists".equals(result.trim());
        } catch (Exception e) {
            log.warn("检查文件存在性失败: {}", path, e);
            return false;
        }
    }

    private String readFile(MachineInfo machine, String path) {
        try {
            String command = String.format("cat %s", path);
            return sshClient.executeCommand(machine, command).trim();
        } catch (Exception e) {
            log.warn("读取文件失败: {}", path, e);
            return null;
        }
    }

    private boolean processRunning(MachineInfo machine, String pid) {
        try {
            String command =
                    String.format(
                            "ps -p %s > /dev/null && echo \"running\" || echo \"not_running\"",
                            pid);
            String result = sshClient.executeCommand(machine, command);
            return "running".equals(result.trim());
        } catch (Exception e) {
            log.warn("检查进程运行状态失败，PID: {}", pid, e);
            return false;
        }
    }
}
