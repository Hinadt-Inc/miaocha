package com.hinadt.miaocha.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Logstash 配置属性 */
@Data
@Configuration
@ConfigurationProperties(prefix = "logstash")
public class LogstashProperties {

    /** Logstash 压缩包路径 */
    private String packagePath;

    /** Logstash 远程部署目录 */
    private String deployDir;

    /**
     * 获取部署基础目录 兼容性方法，实际返回的是deployDir
     *
     * @return 部署基础目录
     */
    public String getDeployBaseDir() {
        return deployDir;
    }
}
