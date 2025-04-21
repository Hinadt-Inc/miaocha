package com.hina.log.service.logstash;

import com.hina.log.entity.Machine;
import com.hina.log.exception.LogstashException;
import com.hina.log.exception.SshException;
import com.hina.log.exception.SshDependencyException;
import com.hina.log.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logstash部署服务接口
 * 负责Logstash的部署、启动、停止等操作
 */
public interface LogstashDeploymentService {

    /**
     * 上传Logstash安装包到目标机器
     * 
     * @param machine    目标机器
     * @param localPath  本地文件路径
     * @param remotePath 远程文件路径
     * @throws SshException 上传失败时抛出异常
     */
    void uploadLogstashPackage(Machine machine, String localPath, String remotePath) throws SshException;

    /**
     * 执行SSH命令
     * 
     * @param machine 目标机器
     * @param command 要执行的命令
     * @return 命令执行结果
     * @throws SshException 执行失败时抛出异常
     */
    String executeCommand(Machine machine, String command) throws SshException;

    /**
     * 部署并启动Logstash
     * 
     * @param machine       目标机器
     * @param configContent 配置文件内容
     * @param deployDir     部署目录
     * @param packagePath   安装包路径
     * @return 进程ID
     * @throws SshException 执行失败时抛出异常
     */
    String deployAndStartLogstash(Machine machine, String configContent, String deployDir, String packagePath)
            throws SshException;

    /**
     * 检查进程是否运行
     * 
     * @param machine 目标机器
     * @param pid     进程ID
     * @return 是否运行
     * @throws SshException 执行失败时抛出异常
     */
    boolean isProcessRunning(Machine machine, String pid) throws SshException;

    /**
     * 检查Logstash是否运行
     * 
     * @param machine 目标机器
     * @return 是否运行
     * @throws SshException 执行失败时抛出异常
     */
    boolean isLogstashRunning(Machine machine) throws SshException;

    /**
     * 获取Logstash进程ID
     * 
     * @param machine 目标机器
     * @return 进程ID
     * @throws SshException 执行失败时抛出异常
     */
    String getLogstashProcessId(Machine machine) throws SshException;

    /**
     * 停止Logstash
     * 
     * @param machine 目标机器
     * @param pid     进程ID，可为null
     * @return 是否成功停止
     * @throws SshException 执行失败时抛出异常
     */
    boolean stopLogstash(Machine machine, String pid) throws SshException;
}