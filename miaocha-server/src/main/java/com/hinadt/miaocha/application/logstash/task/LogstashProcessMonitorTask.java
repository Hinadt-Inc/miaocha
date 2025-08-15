package com.hinadt.miaocha.application.logstash.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.application.logstash.enums.LogstashMachineState;
import com.hinadt.miaocha.application.logstash.path.LogstashPathUtils;
import com.hinadt.miaocha.common.exception.SshException;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.infrastructure.email.EmailService;
import com.hinadt.miaocha.infrastructure.email.EmailTemplateRenderer;
import com.hinadt.miaocha.infrastructure.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.infrastructure.mapper.LogstashProcessMapper;
import com.hinadt.miaocha.infrastructure.mapper.MachineMapper;
import com.hinadt.miaocha.infrastructure.ssh.SshClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Logstash process monitor task. Periodically checks all running Logstash instances and updates
 * status if a process is unexpectedly dead. For processes that have gone offline, after the scan
 * completes, aggregates and sends alert emails to configured recipients.
 */
@Slf4j
@Component
public class LogstashProcessMonitorTask {

    // Minimum running minutes before a process is considered for monitoring
    private static final long MIN_PROCESS_RUNTIME_MINUTES = 2;

    private final LogstashMachineMapper logstashMachineMapper;
    private final LogstashProcessMapper logstashProcessMapper;
    private final MachineMapper machineMapper;
    private final SshClient sshClient;
    private final EmailService emailService;
    private final EmailTemplateRenderer templateRenderer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${miaocha.alerts.mail.enabled:false}")
    private boolean mailEnabled;

    public LogstashProcessMonitorTask(
            LogstashMachineMapper logstashMachineMapper,
            LogstashProcessMapper logstashProcessMapper,
            MachineMapper machineMapper,
            SshClient sshClient,
            EmailService emailService,
            EmailTemplateRenderer templateRenderer) {
        this.logstashMachineMapper = logstashMachineMapper;
        this.logstashProcessMapper = logstashProcessMapper;
        this.machineMapper = machineMapper;
        this.sshClient = sshClient;
        this.emailService = emailService;
        this.templateRenderer = templateRenderer;
    }

    /** Periodically check running Logstash instances every configured interval. */
    @Scheduled(fixedRateString = "${logstash.monitor.interval:300000}")
    public void monitorLogstashProcesses() {
        try {
            log.debug("Start scheduled Logstash process status check...");

            // Fetch all LogstashMachine entries that have a recorded PID
            List<LogstashMachine> processesWithPid =
                    logstashMachineMapper.selectAllWithProcessPid();
            if (processesWithPid.isEmpty()) {
                log.debug("No running Logstash instances found. Skipping check.");
                return;
            }

            // Start timing
            LocalDateTime checkStartTime = LocalDateTime.now();
            log.debug("Found {} instances with PID. Checking status...", processesWithPid.size());

            int checkedCount = 0;
            int skippedCount = 0;
            // Collect dead instances for later aggregation email
            List<DeadInstance> deadInstances = new ArrayList<>();

            // Iterate and check
            for (LogstashMachine logstashMachine : processesWithPid) {
                if (shouldCheckProcess(logstashMachine)) {
                    DeadInstance dead = checkProcessStatus(logstashMachine);
                    if (dead != null) {
                        deadInstances.add(dead);
                    }
                    checkedCount++;
                } else {
                    skippedCount++;
                }
            }

            // After checks, aggregate and send alert emails if needed
            if (!deadInstances.isEmpty()) {
                dispatchOfflineAlerts(deadInstances);
            }

            // Finish timing
            long duration = Duration.between(checkStartTime, LocalDateTime.now()).toMillis();
            log.debug(
                    "Logstash status check completed. Checked: {}, skipped (new): {}, duration: {}"
                            + " ms",
                    checkedCount,
                    skippedCount,
                    duration);
        } catch (Exception e) {
            // Catch-all to ensure the scheduler keeps running
            log.error("Unexpected error during Logstash monitoring task: {}", e.getMessage(), e);
        }
    }

    /**
     * Determine whether the instance should be checked. Only check instances in RUNNING or
     * STOP_FAILED state and that have run for at least MIN_PROCESS_RUNTIME_MINUTES.
     */
    private boolean shouldCheckProcess(LogstashMachine logstashMachine) {
        Long processId = logstashMachine.getLogstashProcessId();
        String pid = logstashMachine.getProcessPid();

        // Skip if no PID
        if (!StringUtils.hasText(pid)) {
            return false;
        }

        // Fetch process for updateTime
        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            log.warn("Logstash process record not found for id={}, maybe deleted.", processId);
            return false;
        }

        // Only check when instance is running or stop failed
        if (!LogstashMachineState.RUNNING.name().equals(logstashMachine.getState())
                && !LogstashMachineState.STOP_FAILED.name().equals(logstashMachine.getState())) {
            log.debug(
                    "Instance of process [{}] on machine [{}] is in state {}. Skipping.",
                    processId,
                    logstashMachine.getMachineId(),
                    logstashMachine.getState());
            return false;
        }

        // Check updateTime to ensure minimum runtime
        LocalDateTime updateTime = process.getUpdateTime();
        if (updateTime == null) {
            log.warn(
                    "Logstash process [{}] has no updateTime; cannot determine runtime. Skipping.",
                    processId);
            return false;
        }

        LocalDateTime minRunTime = LocalDateTime.now().minusMinutes(MIN_PROCESS_RUNTIME_MINUTES);
        if (updateTime.isAfter(minRunTime)) {
            // Updated within the minimum window, skip
            long runningMinutes = Duration.between(updateTime, LocalDateTime.now()).toMinutes();
            log.debug(
                    "Process [{}] runtime < {} minutes ({}m). Skipping.",
                    processId,
                    MIN_PROCESS_RUNTIME_MINUTES,
                    runningMinutes);
            return false;
        }

        return true;
    }

    /** Check a single Logstash instance. Return a DeadInstance if it is found dead. */
    private DeadInstance checkProcessStatus(LogstashMachine logstashMachine) {
        Long processId = logstashMachine.getLogstashProcessId();
        Long machineId = logstashMachine.getMachineId();
        Long logstashMachineId = logstashMachine.getId();
        String pid = logstashMachine.getProcessPid();

        // Fetch machine info
        MachineInfo machineInfo = machineMapper.selectById(machineId);
        if (machineInfo == null) {
            log.warn(
                    "Machine not found for id={}, cannot check process [{}] status.",
                    machineId,
                    processId);
            return null;
        }

        try {
            // Check process existence via SSH
            boolean processExists = checkProcessExistsBySsh(machineInfo, pid);
            if (!processExists) {
                handleDeadProcess(logstashMachineId, machineInfo, pid);
                // Build a dead instance record for later email
                return buildDeadInstance(logstashMachine, machineInfo, pid);
            } else {
                log.debug(
                        "Process instance [{}] on [{}] is running. PID={}",
                        logstashMachineId,
                        machineInfo.getIp(),
                        pid);
            }
        } catch (Exception e) {
            log.error(
                    "Error checking Process instance [{}] on [{}]: {}",
                    logstashMachineId,
                    machineInfo.getIp(),
                    e.getMessage(),
                    e);
        }
        return null;
    }

    /** Check whether a PID exists via SSH. */
    private boolean checkProcessExistsBySsh(MachineInfo machineInfo, String pid) {
        try {
            // Use ps to check PID existence
            String command = String.format("ps -p %s -o pid= || echo \"Process not found\"", pid);
            String result = sshClient.executeCommand(machineInfo, command);

            // If contains marker, the process is not found
            return !result.contains("Process not found") && StringUtils.hasText(result.trim());
        } catch (SshException e) {
            log.error("SSH check for process failed: {}", e.getMessage());
            // On SSH failure, assume still running to avoid false positives
            return true;
        }
    }

    /** Update state and clear PID for a dead instance. */
    private void handleDeadProcess(Long logstashMachineId, MachineInfo machineInfo, String pid) {
        log.warn(
                "Detected dead Logstash instance [{}] on machine [{}], PID={}",
                logstashMachineId,
                machineInfo.getIp(),
                pid);

        try {
            // Update state to NOT_STARTED
            int updateResult =
                    logstashMachineMapper.updateStateById(
                            logstashMachineId, LogstashMachineState.NOT_STARTED.name());
            if (updateResult > 0) {
                log.debug(
                        "Updated instance [{}] on machine [{}] state to NOT_STARTED",
                        logstashMachineId,
                        machineInfo.getId());
            } else {
                log.warn(
                        "Failed updating instance [{}] on machine [{}]; maybe deleted.",
                        logstashMachineId,
                        machineInfo.getId());
            }

            // Clear PID
            int pidUpdateResult =
                    logstashMachineMapper.updateProcessPidById(logstashMachineId, null);
            if (pidUpdateResult > 0) {
                log.debug(
                        "Cleared PID for instance [{}] on machine [{}]",
                        logstashMachineId,
                        machineInfo.getIp());
            } else {
                log.warn(
                        "Failed clearing PID for instance [{}] on machine [{}]",
                        logstashMachineId,
                        machineInfo.getIp());
            }
        } catch (Exception e) {
            log.error(
                    "Error while handling dead instance [{}]: {}",
                    logstashMachineId,
                    e.getMessage(),
                    e);
        }
    }

    /** Build dead instance record for alerting. */
    private DeadInstance buildDeadInstance(
            LogstashMachine logstashMachine, MachineInfo machineInfo, String pid) {
        LogstashProcess process =
                logstashProcessMapper.selectById(logstashMachine.getLogstashProcessId());
        if (process == null) {
            return null;
        }
        DeadInstance di = new DeadInstance();
        di.processId = process.getId();
        di.processName = process.getName();
        di.instanceId = logstashMachine.getId();
        di.machineIp = machineInfo.getIp();
        di.deployPath = logstashMachine.getDeployPath();
        di.pid = pid;
        di.alertRecipientsJson = process.getAlertRecipients();
        di.detectedAt = LocalDateTime.now();
        return di;
    }

    /** Aggregate by process and send alert emails with last 50 log lines for each instance. */
    private void dispatchOfflineAlerts(List<DeadInstance> deadInstances) {
        if (!mailEnabled) {
            log.debug(
                    "Mail alerts disabled. Skip email dispatch for {} dead instances.",
                    deadInstances.size());
            return;
        }

        Map<Long, List<DeadInstance>> byProcess =
                deadInstances.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(di -> di.processId));

        for (Map.Entry<Long, List<DeadInstance>> entry : byProcess.entrySet()) {
            List<DeadInstance> list = entry.getValue();
            if (CollectionUtils.isEmpty(list)) continue;

            DeadInstance first = list.get(0);
            List<String> recipients = parseRecipients(first.alertRecipientsJson);
            if (CollectionUtils.isEmpty(recipients)) {
                log.warn(
                        "No alert recipients configured for process [{}] ({}). Skipping email.",
                        first.processId,
                        first.processName);
                continue;
            }

            // Build template model and render

            String subject =
                    String.format("[Alert] Logstash instances stopped - %s", first.processName);
            Map<String, Object> model = new java.util.HashMap<>();
            model.put("processName", nullToEmpty(first.processName));
            model.put("generatedAt", LocalDateTime.now().toString());
            model.put("instances", buildInstanceModels(list));
            String html = templateRenderer.render("logstash_offline", model);
            try {
                emailService.sendHtml(recipients, subject, html);
                log.info(
                        "Sent offline alert email to {} recipients for process [{}] ({} instances)",
                        recipients.size(),
                        first.processId,
                        list.size());
            } catch (Exception ex) {
                log.error("Failed to send alert email: {}", ex.getMessage(), ex);
            }
        }
    }

    private String fetchLastLogsSafely(DeadInstance di) {
        try {
            MachineInfo machine =
                    machineMapper.selectById(
                            logstashMachineMapper.selectById(di.instanceId).getMachineId());
            if (machine == null) return "Machine not found.";
            String logPath = LogstashPathUtils.buildLogFilePath(di.deployPath);
            String cmd =
                    String.format(
                            "tail -n 50 \"%s\" 2>/dev/null || echo 'No logs found.'", logPath);
            return sshClient.executeCommand(machine, cmd);
        } catch (Exception e) {
            return "Failed to fetch logs: " + e.getMessage();
        }
    }

    private List<String> parseRecipients(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Invalid alert recipients JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> buildInstanceModels(List<DeadInstance> list) {
        List<Map<String, Object>> models = new ArrayList<>();
        for (DeadInstance di : list) {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("instanceId", di.instanceId);
            m.put("machineIp", di.machineIp);
            m.put("deployPath", nullToEmpty(di.deployPath));
            m.put("pid", nullToDash(di.pid));
            m.put("detectedAt", di.detectedAt.toString());
            m.put("lastLogs", fetchLastLogsSafely(di));
            models.add(m);
        }
        return models;
    }

    private String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Value holder for dead instance info used for alerting. */
    private static class DeadInstance {
        Long processId;
        String processName;
        Long instanceId;
        String machineIp;
        String deployPath;
        String pid;
        String alertRecipientsJson;
        LocalDateTime detectedAt;
    }
}
