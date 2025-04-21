package com.hina.log.ssh;

import com.hina.log.exception.SshException;
import com.hina.log.exception.SshDependencyException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * SSH文件传输接口
 */
public interface SshFileTransfer {

    /**
     * 上传文件到远程服务器
     *
     * @param config     SSH配置
     * @param localPath  本地文件路径
     * @param remotePath 远程文件路径
     * @throws IOException      如果IO操作失败
     * @throws TimeoutException 如果传输超时
     * @throws SshException     如果SSH操作失败
     */
    void uploadFile(SshConfig config, String localPath, String remotePath)
            throws IOException, TimeoutException, SshException;

    /**
     * 从远程服务器下载文件
     *
     * @param config     SSH配置
     * @param remotePath 远程文件路径
     * @param localPath  本地文件路径
     * @throws IOException      如果IO操作失败
     * @throws TimeoutException 如果传输超时
     * @throws SshException     如果SSH操作失败
     */
    void downloadFile(SshConfig config, String remotePath, String localPath)
            throws IOException, TimeoutException, SshException;

    /**
     * 检查文件上传是否成功
     *
     * @param config     SSH配置
     * @param localPath  本地文件路径
     * @param remotePath 远程文件路径
     * @return 上传是否成功
     */
    default boolean uploadFileCheck(SshConfig config, String localPath, String remotePath) {
        try {
            uploadFile(config, localPath, remotePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}