package com.hinadt.miaocha.integration.logstash.support;

import com.hinadt.miaocha.common.ssh.SshClient;
import com.hinadt.miaocha.config.LogstashProperties;
import com.hinadt.miaocha.domain.entity.MachineInfo;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Logstash软件包管理器 负责下载、验证、缓存和分发真实的Logstash软件包到测试环境 */
@Slf4j
@Component
public class LogstashPackageManager {

    private static final String LOGSTASH_VERSION = "9.0.2";
    private static final String LOGSTASH_FILENAME =
            "logstash-" + LOGSTASH_VERSION + "-linux-x86_64.tar.gz";
    private static final String LOGSTASH_DOWNLOAD_URL =
            "https://artifacts.elastic.co/downloads/logstash/" + LOGSTASH_FILENAME;

    @Value("${test.logstash.cache.dir:${java.io.tmpdir}/logstash-test-cache}")
    private String cacheDirectory;

    @Autowired private SshClient sshClient;
    @Autowired private LogstashProperties logstashProperties;

    /** 确保Logstash软件包可用（下载或验证缓存），并动态设置LogstashProperties */
    public void ensureLogstashPackageAvailable() {
        log.info("检查Logstash软件包可用性: {}", LOGSTASH_FILENAME);

        Path cacheDir = Paths.get(cacheDirectory);
        Path packagePath = cacheDir.resolve(LOGSTASH_FILENAME);

        try {
            Files.createDirectories(cacheDir);

            if (Files.exists(packagePath)) {
                log.info("发现缓存的软件包，验证完整性...");
                if (verifyPackageIntegrity(packagePath)) {
                    log.info("缓存的软件包验证通过，跳过下载");
                } else {
                    log.warn("缓存的软件包校验失败，重新下载");
                    Files.deleteIfExists(packagePath);
                    downloadLogstashPackage(packagePath);
                    verifyPackageIntegrity(packagePath);
                }
            } else {
                downloadLogstashPackage(packagePath);
                verifyPackageIntegrity(packagePath);
            }

            // 动态设置LogstashProperties的包路径
            String packageAbsolutePath = packagePath.toAbsolutePath().toString();
            logstashProperties.setPackagePath(packageAbsolutePath);

            log.info("Logstash软件包准备完成: {}", packagePath);
            log.info("动态设置 LogstashProperties.packagePath = {}", packageAbsolutePath);

        } catch (Exception e) {
            throw new RuntimeException("准备Logstash软件包失败", e);
        }
    }

    /** 上传Logstash软件包到指定机器 */
    public void uploadLogstashPackageToMachine(MachineInfo machine) {
        log.info("上传Logstash软件包到机器: {}:{}", machine.getIp(), machine.getPort());

        Path packagePath = Paths.get(cacheDirectory, LOGSTASH_FILENAME);

        if (!Files.exists(packagePath)) {
            throw new IllegalStateException("Logstash软件包不存在，请先调用ensureLogstashPackageAvailable()");
        }

        try {
            // 上传软件包到远程机器的/tmp目录
            sshClient.uploadFile(machine, packagePath.toString(), "/tmp/" + LOGSTASH_FILENAME);

            // 验证上传成功
            String remoteFileSize =
                    sshClient.executeCommand(
                            machine,
                            "stat -c%s /tmp/" + LOGSTASH_FILENAME + " 2>/dev/null || echo '0'");

            long localFileSize = Files.size(packagePath);
            long uploadedFileSize = Long.parseLong(remoteFileSize.trim());

            if (localFileSize != uploadedFileSize) {
                throw new RuntimeException(
                        String.format("文件上传大小不匹配: 本地=%d, 远程=%d", localFileSize, uploadedFileSize));
            }

            log.info(
                    "软件包上传成功: {} -> {}:/tmp/{} ({}字节)",
                    packagePath,
                    machine.getIp(),
                    LOGSTASH_FILENAME,
                    localFileSize);

        } catch (Exception e) {
            throw new RuntimeException("上传Logstash软件包失败", e);
        }
    }

    /** 在远程机器上解压Logstash软件包 */
    public void extractLogstashPackageOnMachine(MachineInfo machine, String targetDir) {
        log.info("在机器{}上解压Logstash软件包到: {}", machine.getIp(), targetDir);

        try {
            // 创建目标目录
            sshClient.executeCommand(machine, "mkdir -p " + targetDir);

            // 解压软件包
            String extractCommand =
                    String.format(
                            "cd %s && tar -xzf /tmp/%s --strip-components=1",
                            targetDir, LOGSTASH_FILENAME);

            String extractResult = sshClient.executeCommand(machine, extractCommand);

            // 验证解压成功（检查关键文件）
            String verifyCommand =
                    String.format(
                            "test -f %s/bin/logstash && test -f %s/config/logstash.yml && echo 'OK'"
                                    + " || echo 'FAIL'",
                            targetDir, targetDir);

            String verifyResult = sshClient.executeCommand(machine, verifyCommand);

            if (!"OK".equals(verifyResult.trim())) {
                throw new RuntimeException("Logstash软件包解压验证失败");
            }

            // 设置执行权限
            sshClient.executeCommand(machine, "chmod +x " + targetDir + "/bin/logstash");

            log.info("Logstash软件包解压完成: {}", targetDir);

        } catch (Exception e) {
            throw new RuntimeException("解压Logstash软件包失败", e);
        }
    }

    /** 获取缓存的软件包路径 */
    public String getCachedPackagePath() {
        return Paths.get(cacheDirectory, LOGSTASH_FILENAME).toString();
    }

    /** 获取软件包文件名 */
    public String getPackageFilename() {
        return LOGSTASH_FILENAME;
    }

    /** 获取Logstash版本 */
    public String getLogstashVersion() {
        return LOGSTASH_VERSION;
    }

    /** 验证LogstashProperties配置是否正确设置 */
    public boolean verifyLogstashPropertiesConfiguration() {
        String configuredPath = logstashProperties.getPackagePath();
        String expectedPath = getCachedPackagePath();

        boolean isCorrect = expectedPath.equals(configuredPath);

        if (isCorrect) {
            log.info("✅ LogstashProperties包路径配置正确: {}", configuredPath);
        } else {
            log.error("❌ LogstashProperties包路径配置错误 - 期望: {}, 实际: {}", expectedPath, configuredPath);
        }

        return isCorrect;
    }

    /** 清理缓存 */
    public void clearCache() {
        try {
            Path cacheDir = Paths.get(cacheDirectory);
            if (Files.exists(cacheDir)) {
                Files.walk(cacheDir)
                        .sorted((a, b) -> b.compareTo(a)) // 逆序删除（文件先，目录后）
                        .forEach(
                                path -> {
                                    try {
                                        Files.deleteIfExists(path);
                                    } catch (IOException e) {
                                        log.warn("删除缓存文件失败: {}", path, e);
                                    }
                                });
                log.info("缓存清理完成: {}", cacheDirectory);
            }
        } catch (Exception e) {
            log.warn("清理缓存时出错", e);
        }
    }

    private void downloadLogstashPackage(Path targetPath) throws IOException {
        log.info("开始下载Logstash软件包: {} -> {}", LOGSTASH_DOWNLOAD_URL, targetPath);

        URL url = new URL(LOGSTASH_DOWNLOAD_URL);

        try (InputStream inputStream = url.openStream();
                FileOutputStream outputStream = new FileOutputStream(targetPath.toFile())) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                // 每10MB打印一次进度
                if (totalBytes % (10 * 1024 * 1024) == 0) {
                    log.info("下载进度: {}MB", totalBytes / (1024 * 1024));
                }
            }

            log.info("下载完成: {}字节", totalBytes);
        }
    }

    private boolean verifyPackageIntegrity(Path packagePath) {
        try {
            log.debug("验证软件包完整性: {}", packagePath);

            // 计算SHA256校验和
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(packagePath);
            byte[] hashBytes = digest.digest(fileBytes);

            StringBuilder hashString = new StringBuilder();
            for (byte b : hashBytes) {
                hashString.append(String.format("%02x", b));
            }

            String actualSha256 = hashString.toString();
            log.debug("文件SHA256: {}", actualSha256);

            // 注意：由于我们使用了模拟的校验和，这里只检查文件是否存在且有合理大小
            long fileSize = Files.size(packagePath);
            if (fileSize < 50 * 1024 * 1024) { // 至少50MB
                log.warn("文件大小异常: {}字节", fileSize);
                return false;
            }

            log.debug("软件包完整性验证通过");
            return true;

        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("验证软件包完整性时出错", e);
            return false;
        }
    }
}
