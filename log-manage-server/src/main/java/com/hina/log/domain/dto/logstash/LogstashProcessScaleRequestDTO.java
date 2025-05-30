package com.hina.log.domain.dto.logstash;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "Logstash进程扩容/缩容请求DTO")
public class LogstashProcessScaleRequestDTO {

    @Schema(description = "要添加的机器ID列表，用于扩容操作")
    private List<Long> addMachineIds;

    @Schema(description = "要移除的机器ID列表，用于缩容操作")
    private List<Long> removeMachineIds;

    @Schema(description = "自定义部署路径，仅在扩容时有效。如果不指定，将使用默认部署路径")
    private String customDeployPath;

    @Schema(description = "是否强制缩容，即使机器处于运行状态也执行缩容（会先停止进程）", defaultValue = "false")
    private Boolean forceScale = false;

    /** 验证请求参数的有效性 */
    public void validate() {
        boolean hasAdd = addMachineIds != null && !addMachineIds.isEmpty();
        boolean hasRemove = removeMachineIds != null && !removeMachineIds.isEmpty();

        if (!hasAdd && !hasRemove) {
            throw new IllegalArgumentException("扩容或缩容操作至少需要指定一个机器ID列表");
        }

        if (hasAdd && hasRemove) {
            throw new IllegalArgumentException("不能同时执行扩容和缩容操作");
        }
    }

    /** 判断是否为扩容操作 */
    public boolean isScaleOut() {
        return addMachineIds != null && !addMachineIds.isEmpty();
    }

    /** 判断是否为缩容操作 */
    public boolean isScaleIn() {
        return removeMachineIds != null && !removeMachineIds.isEmpty();
    }
}
