package com.hina.log;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 日志管理系统主应用类
 *
 * @author hina
 */
@SpringBootApplication
@MapperScan("com.hina.log.domain.mapper")
@EnableScheduling
public class LogManageSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogManageSystemApplication.class, args);
    }
}
