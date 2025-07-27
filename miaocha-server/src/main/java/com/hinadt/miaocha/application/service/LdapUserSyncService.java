package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.auth.LdapUserDTO;
import java.util.List;

/** LDAP用户同步服务接口 */
public interface LdapUserSyncService {

    /**
     * 获取LDAP中所有用户（分页）
     *
     * @return LDAP用户列表
     */
    List<LdapUserDTO> getAllLdapUsers();

    /**
     * 同步LDAP用户到本地数据库
     *
     * @return 同步的用户数量
     */
    int syncUsersToLocal();

    /**
     * 手动触发用户同步
     *
     * @return 同步结果信息
     */
    String manualSync();
}
