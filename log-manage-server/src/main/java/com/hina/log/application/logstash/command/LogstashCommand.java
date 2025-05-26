package com.hina.log.application.logstash.command;

import com.hina.log.domain.entity.Machine;
import java.util.concurrent.CompletableFuture;

/** Logstash命令接口 使用命令模式封装对Logstash的各种操作 */
public interface LogstashCommand {

    /**
     * 执行命令
     *
     * @param machine 目标机器
     * @return 异步执行结果
     */
    CompletableFuture<Boolean> execute(Machine machine);

    /**
     * 获取命令描述
     *
     * @return 命令描述
     */
    String getDescription();
}
