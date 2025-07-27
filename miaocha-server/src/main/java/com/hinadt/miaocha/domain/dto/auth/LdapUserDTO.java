package com.hinadt.miaocha.domain.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** LDAP用户信息DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LdapUserDTO {

    /** 用户名 (sAMAccountName) */
    private String username;

    /** 显示名称 (displayName) */
    private String displayName;

    /** 邮箱 (mail) */
    private String email;

    /** 姓 (sn) */
    private String surname;

    /** 名 (givenName) */
    private String givenName;

    /** 部门 (department) */
    private String department;

    /** 职位 (title) */
    private String title;

    /** 电话 (telephoneNumber) */
    private String phone;

    /** DN (Distinguished Name) */
    private String dn;
}
