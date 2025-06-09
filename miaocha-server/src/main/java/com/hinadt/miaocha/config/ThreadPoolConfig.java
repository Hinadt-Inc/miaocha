package com.hinadt.miaocha.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 线程池统一配置类 包含所有应用使用的线程池配置： 1. logstashTaskExecutor - 用于异步执行Logstash相关任务 2. logQueryExecutor -
 * 用于并行执行日志查询和分析
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    /** Logstash任务执行器 用于异步执行部署、启动、停止Logstash等长时间运行的任务 */
    @Bean(name = "logstashTaskExecutor")
    public Executor logstashTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(5);
        // 最大线程数
        executor.setMaxPoolSize(10);
        // 队列容量
        executor.setQueueCapacity(25);
        // 线程名前缀
        executor.setThreadNamePrefix("logstash-task-");
        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 初始化
        executor.initialize();
        return executor;
    }

    /** 日志查询线程池 用于并行执行日志分析和检索SQL查询 */
    @Bean("logQueryExecutor")
    public Executor logQueryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(8);
        // 最大线程数
        executor.setMaxPoolSize(16);
        // 队列容量
        executor.setQueueCapacity(100);
        // 线程名前缀
        executor.setThreadNamePrefix("log-query-");
        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间
        executor.setAwaitTerminationSeconds(60);
        // 初始化线程池
        executor.initialize();
        return executor;
    }
}
