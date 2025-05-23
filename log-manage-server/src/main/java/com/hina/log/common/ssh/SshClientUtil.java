package com.hina.log.common.ssh;

import com.hina.log.common.exception.SshException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyPair;

/**
 * SSH工具类，提供加载SSH私钥等工具方法
 */
public class SshClientUtil {

    private static final Logger logger = LoggerFactory.getLogger(SshClientUtil.class);

    /**
     * 从私钥字符串加载KeyPair
     * 
     * @param privateKeyData 私钥内容
     * @return KeyPair对象
     * @throws SshException 如果私钥加载失败
     */
    public static KeyPair loadPrivateKey(String privateKeyData) throws SshException {
        try {
            // 使用BouncyCastle库解析PEM格式的私钥
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            PEMParser pemParser = new PEMParser(new StringReader(privateKeyData));
            Object object = pemParser.readObject();
            pemParser.close();

            if (object == null) {
                throw new SshException("无法识别的私钥格式");
            }

            KeyPair keyPair;

            if (object instanceof PEMKeyPair) {
                // 标准PEM私钥
                keyPair = converter.getKeyPair((PEMKeyPair) object);
            } else if (object instanceof KeyPair) {
                // 已经是Java KeyPair
                keyPair = (KeyPair) object;
            } else {
                throw new SshException("不支持的私钥类型: " + object.getClass().getName());
            }

            if (keyPair == null) {
                throw new SshException("无法加载SSH私钥，不支持的格式或私钥无效");
            }

            logger.debug("成功加载私钥，类型: {}", keyPair.getPrivate().getAlgorithm());

            return keyPair;
        } catch (IOException e) {
            throw new SshException("SSH私钥加载失败: " + e.getMessage(), e);
        }
    }
}