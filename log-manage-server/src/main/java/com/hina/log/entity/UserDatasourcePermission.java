package com.hina.log.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户数据源权限实体类
 */
@Data
public class UserDatasourcePermission {
    private Long id;
    private Long userId;
    private Long datasourceId;
    private String tableName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}