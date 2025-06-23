package com.hinadt.miaocha.application.logstash.path;

/** Logstash路径工具类 统一管理Logstash实例的各种路径构建逻辑，避免路径构建逻辑散落在各处 */
public class LogstashPathUtils {

    /**
     * 构建日志文件路径
     *
     * @param deployPath 部署路径
     * @param logstashMachineId Logstash实例ID
     * @return 日志文件完整路径
     */
    public static String buildLogFilePath(String deployPath, Long logstashMachineId) {
        return deployPath + "/logs/logstash-" + logstashMachineId + ".log";
    }

    /**
     * 构建PID文件路径
     *
     * @param deployPath 部署路径
     * @param logstashMachineId Logstash实例ID
     * @return PID文件完整路径
     */
    public static String buildPidFilePath(String deployPath, Long logstashMachineId) {
        return deployPath + "/logs/logstash-" + logstashMachineId + ".pid";
    }

    /**
     * 构建配置文件路径
     *
     * @param deployPath 部署路径
     * @param logstashMachineId Logstash实例ID
     * @return 配置文件完整路径
     */
    public static String buildConfigFilePath(String deployPath, Long logstashMachineId) {
        return deployPath + "/config/logstash-" + logstashMachineId + ".conf";
    }

    /**
     * 构建日志目录路径
     *
     * @param deployPath 部署路径
     * @return 日志目录完整路径
     */
    public static String buildLogDirPath(String deployPath) {
        return deployPath + "/logs";
    }

    /**
     * 构建配置目录路径
     *
     * @param deployPath 部署路径
     * @return 配置目录完整路径
     */
    public static String buildConfigDirPath(String deployPath) {
        return deployPath + "/config";
    }

    /**
     * 构建Logstash二进制文件路径
     *
     * @param deployPath 部署路径
     * @return Logstash二进制文件完整路径
     */
    public static String buildLogstashBinPath(String deployPath) {
        return deployPath + "/bin/logstash";
    }

    /**
     * 构建JVM配置文件路径
     *
     * @param deployPath 部署路径
     * @return JVM配置文件完整路径
     */
    public static String buildJvmOptionsPath(String deployPath) {
        return deployPath + "/config/jvm.options";
    }

    /**
     * 构建Logstash系统配置文件路径
     *
     * @param deployPath 部署路径
     * @return Logstash系统配置文件完整路径
     */
    public static String buildLogstashYmlPath(String deployPath) {
        return deployPath + "/config/logstash.yml";
    }
}
