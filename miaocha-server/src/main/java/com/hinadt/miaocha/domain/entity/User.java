package com.hinadt.miaocha.domain.entity;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

/** 用户实体类 */
@Data
@FieldNameConstants
public class User {
    private Long id;
    private String nickname;
    private String email;
    private String uid;
    private String password;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
