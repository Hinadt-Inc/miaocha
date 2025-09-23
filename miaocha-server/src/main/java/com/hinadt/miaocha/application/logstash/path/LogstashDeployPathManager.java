package com.hinadt.miaocha.application.logstash.path;

import com.hinadt.miaocha.config.LogstashProperties;
import com.hinadt.miaocha.domain.entity.LogstashMachine;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import com.hinadt.miaocha.infrastructure.mapper.LogstashMachineMapper;
import com.hinadt.miaocha.infrastructure.mapper.MachineMapper;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Logstash部署路径管理器 负责处理Logstash实例的部署路径生成和规范化 */
@Component
@RequiredArgsConstructor
public class LogstashDeployPathManager {

    private static final Logger logger = LoggerFactory.getLogger(LogstashDeployPathManager.class);

    private final LogstashProperties logstashProperties;
    private final LogstashMachineMapper logstashMachineMapper;
    private final MachineMapper machineMapper;

    /**
     * 获取LogstashMachine实例的部署路径 优先使用数据库中存储的路径，否则生成默认路径
     *
     * @param logstashMachineId LogstashMachine实例ID
     * @return 部署路径
     */
    public String getInstanceDeployPath(Long logstashMachineId) {
        if (logstashMachineId == null) {
            throw new IllegalArgumentException("LogstashMachine ID不能为null");
        }

        try {
            // 从数据库获取实例信息
            LogstashMachine logstashMachine = logstashMachineMapper.selectById(logstashMachineId);
            if (logstashMachine != null && StringUtils.hasText(logstashMachine.getDeployPath())) {
                // 数据库中存储的是完整的部署路径，直接使用
                return logstashMachine.getDeployPath();
            }

            // 数据库中没有路径信息，生成默认路径
            if (logstashMachine != null) {
                MachineInfo machineInfo = machineMapper.selectById(logstashMachine.getMachineId());
                if (machineInfo != null) {
                    return generateDefaultInstancePath(machineInfo);
                }
            }
        } catch (Exception e) {
            logger.warn(
                    "无法从数据库获取部署路径，logstashMachineId: {}, 错误: {}",
                    logstashMachineId,
                    e.getMessage());
        }

        throw new IllegalStateException("无法获取LogstashMachine实例的部署路径，ID: " + logstashMachineId);
    }

    /**
     * 获取LogstashMachine实例的部署路径 优先使用数据库中存储的路径，否则生成默认路径
     *
     * @param logstashMachine LogstashMachine实例
     * @return 部署路径
     */
    public String getInstanceDeployPath(LogstashMachine logstashMachine) {
        if (logstashMachine == null) {
            throw new IllegalArgumentException("LogstashMachine不能为null");
        }

        // 如果实例已有部署路径，直接返回
        if (StringUtils.hasText(logstashMachine.getDeployPath())) {
            return logstashMachine.getDeployPath();
        }

        // 否则生成默认路径
        MachineInfo machineInfo = machineMapper.selectById(logstashMachine.getMachineId());
        if (machineInfo == null) {
            throw new IllegalStateException("找不到机器信息，机器ID: " + logstashMachine.getMachineId());
        }

        return generateDefaultInstancePath(machineInfo);
    }

    /**
     * 生成默认的LogstashMachine实例部署路径 使用UUID确保绝对唯一性
     *
     * @param machineInfo 机器信息
     * @return 默认部署路径
     */
    public String generateDefaultInstancePath(MachineInfo machineInfo) {
        if (machineInfo == null) {
            throw new IllegalArgumentException("参数不能为null");
        }

        String actualDeployDir = normalizeDeployBaseDir(machineInfo);
        // 使用UUID确保绝对唯一性
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        return String.format("%s/logstash-%s", actualDeployDir, uuid);
    }

    /**
     * 规范化实例部署路径 如果传入的是相对路径（不以/开头且不包含绝对前缀），则将其视为相对于目标用户 home 目录。
     *
     * @param rawDeployPath 用户自定义部署路径
     * @param machineInfo 目标机器信息
     * @return 规范化后的绝对路径
     */
    public String normalizeInstanceDeployPath(String rawDeployPath, MachineInfo machineInfo) {
        if (!StringUtils.hasText(rawDeployPath)) {
            throw new IllegalArgumentException("自定义部署路径不能为空");
        }
        if (machineInfo == null) {
            throw new IllegalArgumentException("MachineInfo不能为null");
        }

        String trimmedPath = rawDeployPath.trim();

        // 处理以 ~ 开头的路径，视为用户家目录
        if (trimmedPath.startsWith("~")) {
            String subPath = trimmedPath.substring(1);
            if (subPath.startsWith("/")) {
                subPath = subPath.substring(1);
            }
            Path homeDir = Path.of(resolveHomeDirectory(machineInfo));
            Path normalized = subPath.isEmpty() ? homeDir : homeDir.resolve(subPath).normalize();
            return normalized.toString();
        }

        // 已经是绝对路径
        if (trimmedPath.startsWith("/")) {
            return Path.of(trimmedPath).normalize().toString();
        }

        // 相对路径，拼接到用户家目录下
        Path homeDir = Path.of(resolveHomeDirectory(machineInfo));
        return homeDir.resolve(trimmedPath).normalize().toString();
    }

    private String resolveHomeDirectory(MachineInfo machineInfo) {
        String username = machineInfo.getUsername();
        if (!StringUtils.hasText(username)) {
            username = "root";
        }
        return "root".equals(username) ? "/root" : String.format("/home/%s", username);
    }

    /**
     * 规范化部署目录路径 如果deployBaseDir不是绝对路径（不以/开头），则将其转换为用户家目录下的路径
     *
     * @param machineInfo 机器信息，用于获取用户名
     * @return 规范化后的绝对路径
     */
    public String normalizeDeployBaseDir(MachineInfo machineInfo) {
        if (machineInfo == null) {
            throw new IllegalArgumentException("MachineInfo不能为null");
        }

        String deployBaseDir = logstashProperties.getDeployBaseDir();
        if (deployBaseDir.startsWith("/")) {
            // 已经是绝对路径，直接返回
            return deployBaseDir;
        } else {
            // 相对路径，转换为用户家目录下的路径
            String username = machineInfo.getUsername();
            return String.format("/home/%s/%s", username, deployBaseDir);
        }
    }

    /**
     * 获取部署基础目录
     *
     * @return 部署基础目录
     */
    public String getDeployBaseDir() {
        return logstashProperties.getDeployBaseDir();
    }
}
