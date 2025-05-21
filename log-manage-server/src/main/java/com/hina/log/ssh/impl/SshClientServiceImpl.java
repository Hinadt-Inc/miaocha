package com.hina.log.ssh.impl;

import com.hina.log.entity.Machine;
import com.hina.log.ssh.SshClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * SSH客户端服务实现类
 * 使用JSch库在远程机器上执行命令
 */
@Service
public class SshClientServiceImpl implements SshClientService {
    private static final Logger logger = LoggerFactory.getLogger(SshClientServiceImpl.class);
    
    private static final int DEFAULT_SSH_PORT = 22;
    private static final int DEFAULT_SESSION_TIMEOUT = 30000;
    private static final int DEFAULT_CHANNEL_TIMEOUT = 10000;
    
    @Override
    public String executeCommand(Machine machine, String command) throws Exception {
        Session session = null;
        ChannelExec channel = null;
        
        try {
            logger.debug("在机器 [{}:{}] 上执行命令: {}", machine.getIp(), DEFAULT_SSH_PORT, command);
            
            JSch jsch = new JSch();
            
            // 创建SSH会话
            session = jsch.getSession(machine.getUsername(), machine.getIp(), DEFAULT_SSH_PORT);
            session.setPassword(machine.getPassword());
            
            // 配置SSH连接
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(DEFAULT_SESSION_TIMEOUT);
            
            // 创建执行通道
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            
            // 获取命令输出流
            StringBuilder responseBuilder = new StringBuilder();
            InputStream inputStream = channel.getInputStream();
            channel.connect(DEFAULT_CHANNEL_TIMEOUT);
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line).append("\n");
                }
            }
            
            // 检查执行状态
            if (channel.getExitStatus() != 0) {
                logger.warn("命令在机器 [{}] 上执行失败，退出码: {}", machine.getIp(), channel.getExitStatus());
            }
            
            String response = responseBuilder.toString().trim();
            logger.debug("命令执行结果: {}", response);
            
            return response;
            
        } finally {
            // 关闭通道和会话
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
} 