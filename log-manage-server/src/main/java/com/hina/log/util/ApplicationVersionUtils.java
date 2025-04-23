package com.hina.log.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 应用版本工具类
 * 负责在应用启动时将版本信息写入version.txt文件，供脚本使用
 */
@Component
public class ApplicationVersionUtils {

    @Value("${spring.application.version:1.0}")
    private String applicationVersion;

    /**
     * 应用启动时将版本信息写入文件
     */
    @EventListener(ApplicationStartedEvent.class)
    public void writeVersionFile() {
        try {
            // 获取应用运行目录
            String baseDir = System.getProperty("user.dir");
            File versionFile = Paths.get(baseDir, "version.txt").toFile();

            // 写入版本信息
            try (FileWriter writer = new FileWriter(versionFile)) {
                writer.write(applicationVersion);
            }
        } catch (IOException e) {
            // 写入失败不影响应用启动
            System.err.println("Failed to write version file: " + e.getMessage());
        }
    }

    /**
     * 获取应用版本
     */
    public String getApplicationVersion() {
        return applicationVersion;
    }
}