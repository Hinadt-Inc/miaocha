package com.hinadt.miaocha.domain.entity;

import com.hinadt.miaocha.common.annotation.UserAuditable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SystemCacheConfig implements UserAuditable {

    private Long id;

    private String cacheGroup;

    private String cacheKey;

    private String content;

    private LocalDateTime createTime;

    private String createUser;

    @Override
    public String getUpdateUser() {
        return "";
    }

    @Override
    public void setUpdateUser(String updateUser) {}
}
