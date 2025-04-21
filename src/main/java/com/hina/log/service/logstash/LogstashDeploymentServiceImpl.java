package com.hina.log.service.logstash;

import com.hina.log.entity.Machine;
import com.hina.log.exception.LogstashException;
import com.hina.log.exception.SshException;
import com.hina.log.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

/**
 * Logstash部署服务实现类
 * 负责Logstash的部署、启动、停止等操作
 */
@Service
public class LogstashDeploymentServiceImpl implements LogstashDeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(LogstashDeploymentServiceImpl.class);

    private final SshClient sshClient;

    public LogstashDeploymentServiceImpl(SshClient sshClient) {
        this.sshClient = sshClient;
    }

    @Override
    public void uploadLogstashPackage(Machine machine, String localPath, String remotePath) throws SshException {
        logger.info("Uploading Logstash package from {} to {}:{}", localPath, machine.getIp(), remotePath);
        try {
            sshClient.uploadFile(machine, localPath, remotePath);
            logger.info("Successfully uploaded Logstash package to {}:{}", machine.getIp(), remotePath);
        } catch (SshException e) {
            logger.error("Failed to upload Logstash package to {}:{}: {}", machine.getIp(), remotePath, e.getMessage());
            throw new SshException("Failed to upload Logstash package: " + e.getMessage(), e);
        }
    }

    @Override
    public String executeCommand(Machine machine, String command) throws SshException {
        logger.debug("Executing command on {}: {}", machine.getIp(), command);
        try {
            String result = sshClient.executeCommand(machine, command);
            logger.debug("Command execution result: {}", result);
            return result;
        } catch (SshException e) {
            logger.error("Failed to execute command on {}: {}", machine.getIp(), e.getMessage());
            throw new SshException("Failed to execute command: " + e.getMessage(), e);
        }
    }

    @Override
    public String deployAndStartLogstash(Machine machine, String configContent, String deployDir, String packagePath)
            throws SshException {
        logger.info("Deploying and starting Logstash on {}", machine.getIp());

        try {
            // 1. Create deployment directory if it doesn't exist
            executeCommand(machine, "mkdir -p " + deployDir);

            // 2. Upload Logstash package if it's a local path
            if (!packagePath.startsWith("http://") && !packagePath.startsWith("https://")) {
                String fileName = Paths.get(packagePath).getFileName().toString();
                String remoteTempPath = "/tmp/" + fileName;
                uploadLogstashPackage(machine, packagePath, remoteTempPath);
                packagePath = remoteTempPath;
            }

            // 3. Download package if it's a URL (on remote machine)
            if (packagePath.startsWith("http://") || packagePath.startsWith("https://")) {
                String fileName = packagePath.substring(packagePath.lastIndexOf('/') + 1);
                String downloadCommand = "cd " + deployDir + " && wget -q " + packagePath;
                executeCommand(machine, downloadCommand);
                packagePath = Paths.get(deployDir, fileName).toString();
            } else if (!packagePath.startsWith("/")) {
                // If not a URL and not an absolute path, assume it's now in /tmp
                String fileName = Paths.get(packagePath).getFileName().toString();
                executeCommand(machine, "cp " + packagePath + " " + deployDir);
                packagePath = Paths.get(deployDir, fileName).toString();
            }

            // 4. Extract Logstash package
            String extractDir = Paths.get(deployDir, "logstash").toString();
            executeCommand(machine, "mkdir -p " + extractDir);

            if (packagePath.endsWith(".tar.gz") || packagePath.endsWith(".tgz")) {
                executeCommand(machine, "tar -xzf " + packagePath + " -C " + extractDir + " --strip-components=1");
            } else if (packagePath.endsWith(".zip")) {
                executeCommand(machine, "unzip -o " + packagePath + " -d " + extractDir + " && mv " + extractDir +
                        "/logstash-*/* " + extractDir + " && rm -rf " + extractDir + "/logstash-*");
            } else {
                throw new LogstashException("Unsupported package format. Supported formats: .tar.gz, .tgz, .zip");
            }

            // 5. Create configuration file
            String configDir = Paths.get(extractDir, "config").toString();
            String configFile = Paths.get(configDir, "logstash.conf").toString();
            executeCommand(machine, "mkdir -p " + configDir);
            executeCommand(machine, "echo '" + configContent.replace("'", "'\\''") + "' > " + configFile);

            // 6. Start Logstash
            String logstashBin = Paths.get(extractDir, "bin/logstash").toString();
            executeCommand(machine, "chmod +x " + logstashBin);

            // Create startup script
            String startupScript = Paths.get(extractDir, "start.sh").toString();
            String startupCommand = "#!/bin/bash\ncd " + extractDir + "\n"
                    + "nohup bin/logstash -f config/logstash.conf > " + extractDir + "/logs/logstash.log 2>&1 & \n"
                    + "echo $! > " + extractDir + "/logstash.pid\n";

            executeCommand(machine, "mkdir -p " + extractDir + "/logs");
            executeCommand(machine, "echo '" + startupCommand.replace("'", "'\\''") + "' > " + startupScript);
            executeCommand(machine, "chmod +x " + startupScript);

            // Execute startup script
            executeCommand(machine, startupScript);

            // Get PID
            String pid = executeCommand(machine, "cat " + extractDir + "/logstash.pid").trim();
            logger.info("Logstash started on {} with PID {}", machine.getIp(), pid);

            // Verify process is running
            if (!isProcessRunning(machine, pid)) {
                throw new LogstashException("Logstash process started but not running");
            }

            return pid;
        } catch (SshException e) {
            logger.error("Failed to deploy and start Logstash on {}: {}", machine.getIp(), e.getMessage());
            throw new SshException("Failed to deploy and start Logstash: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during Logstash deployment on {}: {}", machine.getIp(), e.getMessage());
            throw new LogstashException("Unexpected error during Logstash deployment: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isProcessRunning(Machine machine, String pid) throws SshException {
        if (pid == null || pid.trim().isEmpty()) {
            return false;
        }

        try {
            String result = executeCommand(machine, "ps -p " + pid + " -o pid=");
            return result != null && !result.trim().isEmpty();
        } catch (SshException e) {
            // If command fails, process is likely not running
            return false;
        }
    }

    @Override
    public boolean isLogstashRunning(Machine machine) throws SshException {
        try {
            String pid = getLogstashProcessId(machine);
            return pid != null && !pid.trim().isEmpty() && isProcessRunning(machine, pid);
        } catch (SshException e) {
            return false;
        }
    }

    @Override
    public String getLogstashProcessId(Machine machine) throws SshException {
        try {
            // Try to find PID using pgrep
            String result = executeCommand(machine, "pgrep -f 'logstash -f'");
            if (result != null && !result.trim().isEmpty()) {
                // Return first PID if multiple lines
                return result.trim().split("\\s+")[0];
            }

            // Alternative approach using ps and grep
            result = executeCommand(machine,
                    "ps aux | grep '[l]ogstash -f' | awk '{print $2}' | head -1");
            return result != null ? result.trim() : "";
        } catch (SshException e) {
            logger.error("Failed to get Logstash process ID on {}: {}", machine.getIp(), e.getMessage());
            throw new SshException("Failed to get Logstash process ID: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean stopLogstash(Machine machine, String pid) throws SshException {
        logger.info("Stopping Logstash on {} with PID {}", machine.getIp(), pid);

        try {
            if (pid != null && !pid.trim().isEmpty()) {
                // Try to stop by PID
                executeCommand(machine, "kill " + pid);

                // Check if process is still running after a short delay
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (isProcessRunning(machine, pid)) {
                    // Force kill if still running
                    executeCommand(machine, "kill -9 " + pid);
                }

                return !isProcessRunning(machine, pid);
            } else {
                // Kill all Logstash processes if no PID specified
                executeCommand(machine, "pkill -f 'logstash -f' || true");

                // Force kill any remaining processes
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                executeCommand(machine, "pkill -9 -f 'logstash -f' || true");

                return !isLogstashRunning(machine);
            }
        } catch (SshException e) {
            logger.error("Failed to stop Logstash on {}: {}", machine.getIp(), e.getMessage());
            throw new SshException("Failed to stop Logstash: " + e.getMessage(), e);
        }
    }
}