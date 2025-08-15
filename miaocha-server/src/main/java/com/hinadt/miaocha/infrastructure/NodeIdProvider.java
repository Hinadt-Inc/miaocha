package com.hinadt.miaocha.infrastructure;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NodeIdProvider {

    @Value("${node.id:}")
    private String configuredNodeId; // 手动覆盖（优先级最高）

    @Value("${server.port:0}")
    private int serverPort; // 同机多实例时用于区分

    @Getter private String nodeId;

    @PostConstruct
    public void init() {
        if (configuredNodeId != null && !configuredNodeId.isBlank()) {
            this.nodeId = configuredNodeId.trim();
            log.info("[NodeIdProvider] Using configured node.id = {}", this.nodeId);
        } else {
            this.nodeId = generateShortNodeId();
            log.info("[NodeIdProvider] Generated node.id = {}", this.nodeId);
        }
    }

    /**
     * 短ID格式：h3 p2 i2 - n4 -> 总长 12 h3: hostname 的 hash 十六进制后3位（区分主机） p2: server.port 的
     * base36（2位，区分同机不同端口实例） i2: 进程 PID 的 base36（2位，区分同机同端口不同进程） n4: NanoID 随机 4 位（避免极端碰撞）
     */
    private String generateShortNodeId() {
        String h3 = hostHash3();
        String p2 = base36Pad(serverPort, 2);
        String i2 = base36Pad(getPidSafe(), 2);
        String n4 =
                NanoIdUtils.randomNanoId(
                        NanoIdUtils.DEFAULT_NUMBER_GENERATOR, NanoIdUtils.DEFAULT_ALPHABET, 4);
        return h3 + p2 + i2 + "-" + n4;
    }

    private static String hostHash3() {
        String hex = "unk";
        try {
            String host = InetAddress.getLocalHost().getHostName();
            String h = Integer.toHexString(host.hashCode());
            hex =
                    (h.length() >= 3)
                            ? h.substring(h.length() - 3)
                            : ("000" + h).substring(h.length());
        } catch (UnknownHostException ignored) {
        }
        return hex;
    }

    private static int getPidSafe() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName(); // "pid@hostname"
            int idx = name.indexOf('@');
            return idx > 0 ? Integer.parseInt(name.substring(0, idx)) : name.hashCode();
        } catch (Throwable t) {
            return (int) (System.nanoTime() & 0xFFFF);
        }
    }

    private static String base36Pad(int value, int width) {
        if (value < 0) value = -value;
        String s = Integer.toString(value % 1296, 36); // 36^2 = 1296 -> 两位上限
        s = s.toLowerCase();
        return ("00" + s).substring(("00" + s).length() - width);
    }
}
