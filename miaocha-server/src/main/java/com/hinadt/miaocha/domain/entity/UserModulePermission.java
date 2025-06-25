package com.hinadt.miaocha.domain.entity;

import com.hinadt.miaocha.common.annotation.UserAuditable;
import java.time.LocalDateTime;
import lombok.Data;

/** 用户模块权限实体类 */
@Data
public class UserModulePermission implements UserAuditable {

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

    /** 创建人邮箱 */
    private String createUser;

    /** 修改人邮箱 */
    private String updateUser;
}
