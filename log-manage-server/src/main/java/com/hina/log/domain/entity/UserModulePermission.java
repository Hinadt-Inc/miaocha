package com.hina.log.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;

/** 用户模块权限实体类 */
@Data
public class UserModulePermission {

    /** 权限ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 数据源ID */
    private Long datasourceId;

    /** 模块名称 */
    private String module;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
