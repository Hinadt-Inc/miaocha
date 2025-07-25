package com.hinadt.miaocha.spi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** OAuth认证用户信息模型 SPI提供方需要返回此模型，包含用户的基本信息 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserInfo {

    /** 用户唯一标识符 由SPI提供方决定，通常是用户在认证系统中的唯一ID */
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
}
