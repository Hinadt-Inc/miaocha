# 秒查LDAP认证提供者

这个模块实现了秒查系统的LDAP认证功能，通过SPI插件的方式提供LDAP用户认证服务。

## 功能特性

- **LDAP用户认证**: 支持通过LDAP服务器验证用户身份
- **灵活配置**: 支持多种LDAP服务器（Active Directory, OpenLDAP等）
- **SPI架构**: 通过SPI机制实现，易于扩展和替换

## 使用方法

### 1. 配置文件方式

在 `application.properties` 中添加 LDAP 配置：

```properties
miaocha.ldap.enabled=true
miaocha.ldap.url=ldap://your-ldap-server:389
miaocha.ldap.base-dn=dc=example,dc=com
miaocha.ldap.user-dn=ou=users
miaocha.ldap.manager-dn=cn=admin,dc=example,dc=com
miaocha.ldap.manager-password=admin_password
miaocha.ldap.user-search-filter=(uid={0})
miaocha.ldap.user-object-class=inetOrgPerson
miaocha.ldap.email-attribute=mail
miaocha.ldap.nickname-attribute=cn
miaocha.ldap.real-name-attribute=displayName
miaocha.ldap.department-attribute=department
miaocha.ldap.position-attribute=title
miaocha.ldap.organizational-unit-attribute=ou
```

### 2. 登录方式

使用 LDAP 认证时，需要在登录请求中指定 `providerId`：

```json
{
  "loginIdentifier": "username",
  "password": "password",
  "providerId": "ldap"
}
```

**重要说明**：
- LDAP 认证仅用于身份验证，不会自动创建或同步用户信息
- 用户必须先在系统中存在（由管理员创建）才能使用 LDAP 登录
- 如果需要用户数据同步，请使用外部工具进行用户管理

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
```properties
miaocha.ldap.enabled=true
miaocha.ldap.url=ldap://ad.example.com:389
miaocha.ldap.base-dn=dc=example,dc=com
miaocha.ldap.user-dn=cn=Users
miaocha.ldap.user-search-filter=(sAMAccountName={0})
miaocha.ldap.user-object-class=user
miaocha.ldap.email-attribute=userPrincipalName
```

### OpenLDAP
```properties
miaocha.ldap.enabled=true
miaocha.ldap.url=ldap://openldap.example.com:389
miaocha.ldap.base-dn=dc=example,dc=com
miaocha.ldap.user-dn=ou=people
miaocha.ldap.user-search-filter=(uid={0})
miaocha.ldap.user-object-class=inetOrgPerson
```

## 开发说明

### SPI接口

该模块实现了以下SPI接口：
- `LdapAuthProvider`: LDAP用户认证提供者

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