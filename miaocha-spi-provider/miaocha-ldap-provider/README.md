# 秒查LDAP认证提供者

这个模块实现了秒查系统的LDAP认证功能，通过SPI插件的方式提供LDAP用户认证和同步服务。

## 功能特性

- **LDAP用户认证**: 支持通过LDAP服务器验证用户身份
- **LDAP用户同步**: 支持从LDAP服务器同步用户信息
- **灵活配置**: 支持多种LDAP服务器（Active Directory, OpenLDAP等）
- **SPI架构**: 通过SPI机制实现，易于扩展和替换

## 使用方法

### 1. 启用LDAP功能

在系统配置中设置以下系统属性：

```bash
-Dmiaocha.ldap.enabled=true
-Dmiaocha.ldap.url=ldap://your-ldap-server:389
-Dmiaocha.ldap.base-dn=dc=example,dc=com
-Dmiaocha.ldap.user-dn=ou=users
-Dmiaocha.ldap.manager-dn=cn=admin,dc=example,dc=com
-Dmiaocha.ldap.manager-password=admin_password
```

### 2. 配置文件方式

将 `application-ldap-example.yml` 的内容添加到您的应用配置文件中，并根据您的LDAP环境进行调整。

### 3. 登录方式

启用LDAP后，用户可以使用以下方式登录：
- 使用LDAP用户名和密码
- 使用邮箱地址和LDAP密码

系统会首先尝试LDAP认证，如果LDAP认证失败或服务不可用，则回退到系统内置的用户认证。

## 配置参数说明

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `miaocha.ldap.enabled` | 是否启用LDAP | false |
| `miaocha.ldap.url` | LDAP服务器URL | ldap://localhost:389 |
| `miaocha.ldap.base-dn` | 基础DN | dc=example,dc=com |
| `miaocha.ldap.user-dn` | 用户搜索DN | ou=users |
| `miaocha.ldap.manager-dn` | 管理员DN | cn=admin,dc=example,dc=com |
| `miaocha.ldap.manager-password` | 管理员密码 | admin |
| `miaocha.ldap.user-search-filter` | 用户搜索过滤器 | (uid={0}) |
| `miaocha.ldap.user-object-class` | 用户对象类 | inetOrgPerson |

## 支持的LDAP服务器

### Active Directory
```yaml
miaocha:
  ldap:
    enabled: true
    url: ldap://ad.example.com:389
    base-dn: dc=example,dc=com
    user-dn: cn=Users
    user-search-filter: "(sAMAccountName={0})"
    user-object-class: user
    email-attribute: userPrincipalName
```

### OpenLDAP
```yaml
miaocha:
  ldap:
    enabled: true
    url: ldap://openldap.example.com:389
    base-dn: dc=example,dc=com
    user-dn: ou=people
    user-search-filter: "(uid={0})"
    user-object-class: inetOrgPerson
```

## 开发说明

### SPI接口

该模块实现了以下SPI接口：
- `LdapAuthenticationService`: LDAP用户认证服务
- `LdapUserSyncService`: LDAP用户同步服务

### 扩展开发

如果需要自定义LDAP实现，可以：
1. 实现对应的SPI接口
2. 在 `META-INF/services/` 目录下注册实现类
3. 打包成独立的jar文件

## 故障排除

### 常见问题

1. **连接超时**
   - 检查LDAP服务器地址和端口
   - 确认网络连通性
   - 调整连接超时参数

2. **认证失败**
   - 验证管理员DN和密码
   - 检查用户搜索过滤器
   - 确认用户在LDAP中存在

3. **用户信息不完整**
   - 检查属性映射配置
   - 确认LDAP中用户具有相应属性

### 日志调试

启用调试日志：
```yaml
logging:
  level:
    com.hinadt.miaocha.ldap: DEBUG
```

## 安全建议

1. 使用LDAPS（SSL/TLS）连接
2. 为LDAP连接创建专用的只读账户
3. 定期更新LDAP连接密码
4. 限制LDAP搜索范围 