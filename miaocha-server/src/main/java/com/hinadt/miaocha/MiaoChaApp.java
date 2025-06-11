package com.hinadt.miaocha;

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
@MapperScan("com.hinadt.miaocha.domain.mapper")
@EnableScheduling
public class MiaoChaApp {
    public static void main(String[] args) {
        SpringApplication.run(MiaoChaApp.class, args);
    }
}
