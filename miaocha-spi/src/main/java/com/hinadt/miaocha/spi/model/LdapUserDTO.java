package com.hinadt.miaocha.spi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** LDAP用户信息模型 SPI提供方需要返回此模型，包含从LDAP系统获取的用户基本信息 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LdapUserDTO {

    /** 用户唯一标识符 通常是LDAP中的用户DN或其他唯一标识 */
    private String uid;

    /** 用户邮箱 必填字段，用于在系统中标识用户 */
    private String email;

    /** 用户昵称/显示名称 可选字段，如果为空，系统将使用邮箱前缀作为昵称 */
    private String nickname;

    /** 用户真实姓名 可选字段 */
    private String realName;

    /** 用户部门 可选字段 */
    private String department;

    /** 用户职位 可选字段 */
    private String position;

    /** LDAP用户DN LDAP系统中用户的Distinguished Name */
    private String dn;

    /** 用户在LDAP中的组织单位 可选字段 */
    private String organizationalUnit;
}
