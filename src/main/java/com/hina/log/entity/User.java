package com.hina.log.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
public class User {
    private Long id;
    private String nickname;
    private String email;
    private String uid;
    private Boolean isAdmin;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}