# 部署检查清单 - Version 1.0.0

## 📋 生产环境部署前检查

### ✅ 环境要求验证

- [ ] **Java版本**: OpenJDK 17 或 Oracle JDK 17+ 已安装
- [ ] **数据库**: MySQL 8.0+ 已部署并运行
- [ ] **内存**: 服务器可用内存 ≥ 2GB
- [ ] **磁盘空间**: 可用磁盘空间 ≥ 10GB
- [ ] **网络**: 确保相关端口(8080, 3306)可访问

### ✅ 发布包验证

- [ ] **主执行包**: `log-manage-server-1.0.0-exec.jar` (71MB)
- [ ] **分发包**: `log-manage-assembly-1.0.0-distribution.tar.gz` (62MB)
- [ ] **版本标签**: Git tag `v1.0.0` 已创建
- [ ] **发布说明**: `RELEASE_NOTES_v1.0.0.md` 已准备

### ✅ 数据库准备

- [ ] **数据库创建**: 创建 `log_manage_system` 数据库
- [ ] **用户权限**: 确保数据库用户有足够权限
- [ ] **字符集**: 数据库字符集设置为 `utf8mb4`
- [ ] **时区**: 数据库时区设置为 `Asia/Shanghai`

### ✅ 配置文件准备

- [ ] **生产配置**: 准备 `application-prod.yml`
- [ ] **数据库连接**: 配置正确的数据库连接信息
- [ ] **日志路径**: 设置生产环境日志目录
- [ ] **安全配置**: 配置JWT密钥和会话超时

## 🚀 部署步骤

### 1. 环境准备

```bash
# 创建应用目录
sudo mkdir -p /opt/log-manage-system
sudo mkdir -p /var/log/log-manage-system
sudo mkdir -p /etc/log-manage-system

# 设置权限
sudo chown -R appuser:appuser /opt/log-manage-system
sudo chown -R appuser:appuser /var/log/log-manage-system
```

### 2. 部署应用

```bash
# 解压发布包
tar -xzf log-manage-assembly-1.0.0-distribution.tar.gz -C /opt/log-manage-system

# 复制配置文件
cp application-prod.yml /etc/log-manage-system/
```

### 3. 数据库初始化

```bash
# 连接数据库并创建数据库
mysql -u root -p
CREATE DATABASE log_manage_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'logmanage'@'%' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON log_manage_system.* TO 'logmanage'@'%';
FLUSH PRIVILEGES;
```

### 4. 启动应用

```bash
# 生产环境启动
cd /opt/log-manage-system
java -jar \
  -Dspring.profiles.active=prod \
  -Dspring.config.location=file:/etc/log-manage-system/ \
  -Xms1024m -Xmx2048m \
  log-manage-server-1.0.0-exec.jar
```

## 🔧 生产环境配置示例

### application-prod.yml

```yaml
server:
  port: 8080

spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://localhost:3306/log_manage_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: logmanage
    password: ${DB_PASSWORD:your_secure_password}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000

  security:
    jwt:
      secret: ${JWT_SECRET:your-256-bit-secret-key}
      expiration: 86400000  # 24小时

log-config:
  log-home: "/var/log/log-manage-system"
  max-history: 30
  total-size-cap: "1GB"
  max-file-size: "100MB"
  queue-size: 1024
  discarding-threshold: 0

logging:
  level:
    com.hina.log: INFO
    org.springframework: WARN
    org.hibernate: WARN
```

## 🔍 部署后验证

### ✅ 服务状态检查

- [ ] **应用启动**: 服务成功启动，无错误日志
- [ ] **端口监听**: 确认端口8080正在监听
- [ ] **数据库连接**: 数据库连接池正常工作
- [ ] **日志生成**: JSON格式日志文件正常生成

### ✅ 功能验证

- [ ] **健康检查**: 访问 `http://server:8080/actuator/health`
- [ ] **API文档**: 访问 `http://server:8080/swagger-ui/index.html`
- [ ] **用户登录**: 验证用户认证功能正常
- [ ] **基础功能**: 测试主要功能模块

### ✅ 性能验证

- [ ] **内存使用**: 应用内存使用在预期范围内
- [ ] **响应时间**: API响应时间正常
- [ ] **日志轮转**: 日志文件轮转正常工作
- [ ] **数据库性能**: 数据库查询性能正常

## 🚨 故障排查

### 常见问题

1. **启动失败**
   - 检查Java版本和环境变量
   - 查看日志文件中的错误信息
   - 确认配置文件路径和内容
2. **数据库连接失败**
   - 验证数据库服务状态
   - 检查连接字符串和认证信息
   - 确认网络连通性
3. **权限问题**
   - 检查文件系统权限
   - 确认数据库用户权限
   - 验证应用运行用户权限

### 日志文件位置

```
/var/log/log-manage-system/
├── log-manage-system-json.log         # INFO级别JSON日志
├── log-manage-system-json-error.log   # ERROR级别JSON日志
└── archive/                           # 历史日志文件
```

## 📞 应急联系

- **技术支持**: 开发团队
- **运维支持**: 运维团队
- **文档参考**: [发布说明](RELEASE_NOTES_v1.0.0.md)

## ✅ 部署完成确认

部署工程师签名: ________________  
部署日期: ________________  
验收负责人签名: ________________  
验收日期: ________________

---

**注意**: 此检查清单适用于 1.0.0 版本，后续版本请参考对应的部署文档。
