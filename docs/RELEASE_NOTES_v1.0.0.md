# Release Notes - Version 1.0.0

**发布日期**: 2025年5月23日

## 🎉 海纳 Doris日志管理系统 1.0.0 正式版

这是海纳 Doris日志管理系统的第一个正式版本，提供了完整的日志管理解决方案。

## ✨ 核心功能

### 用户认证与权限管理

- 基于JWT的安全认证机制
- 细粒度的模块权限控制
- 用户会话管理

### 数据源管理

- 支持多种数据源配置和连接
- 数据源连接测试和状态监控
- 动态数据源切换

### 日志搜索与分析

- **高级日志搜索**: 支持多条件组合查询、时间范围、关键词过滤
- **时间分布分析**: 可视化日志时间分布统计
- **字段分布统计**: TOP N值分布分析
- **表结构查询**: 动态获取日志表字段信息

### SQL编辑器

- **交互式SQL编辑器**: 支持语法高亮、代码提示
- **查询结果展示**: 分页显示查询结果
- **数据导出**: 支持Excel格式导出

### Logstash进程管理

- **远程部署**: 自动化Logstash软件包部署
- **配置管理**: 主配置文件、JVM选项、logstash.yml配置
- **进程控制**: 启动、停止、监控Logstash进程
- **多机器部署**: 支持在多台服务器上部署同一进程
- **状态机管理**: 8种状态的完整生命周期管理
- **重新初始化**: 支持失败实例的重新初始化

### 机器管理

- SSH连接管理和认证
- 服务器状态监控
- 远程命令执行

## 🏗️ 技术架构

### 后端技术栈

- **Java 17** + **Spring Boot 3.4.4**
- **Spring Security** + **JWT认证**
- **MyBatis** + **MySQL 8.x**
- **Flyway数据库迁移**
- **SpringDoc OpenAPI 3.0**

### 前端技术栈

- **React 18** + **TypeScript**
- **Ant Design 5** UI组件库
- **Vite** 构建工具

### 设计模式

- **领域驱动设计(DDD)**: 清晰的分层架构
- **状态模式**: Logstash进程状态管理
- **命令模式**: SSH命令执行
- **策略模式**: 多种数据源处理

## 🚀 新特性

### 高级日志配置

- **多格式日志输出**: 支持普通格式和JSON格式
- **环境差异化配置**:
  - 开发/测试环境: 控制台 + 普通格式 + JSON格式
  - 生产环境: 仅JSON格式，优化性能
- **异步日志**: 高性能异步输出
- **智能轮转**: 按时间和大小自动轮转

### 状态机实现

- **8种状态管理**: 从初始化到运行的完整状态流转
- **状态处理器**: 每种状态的专门处理逻辑
- **异步操作**: 支持非阻塞状态转换

### 安全加强

- **加密存储**: 敏感信息使用BouncyCastle加密
- **SSH密钥管理**: 支持密钥对认证
- **会话管理**: 安全的用户会话控制

## 📦 发布包

### 1. 核心应用包

- **log-manage-server-1.0.0-exec.jar**: 可执行的Spring Boot应用包
- **log-manage-server-1.0.0.jar**: 标准JAR包

### 2. 发布分发包

- **log-manage-assembly-1.0.0-distribution.zip**: ZIP格式发布包
- **log-manage-assembly-1.0.0-distribution.tar.gz**: TAR.GZ格式发布包

## 🛠️ 部署说明

### 系统要求

- **Java**: OpenJDK 17 或 Oracle JDK 17+
- **数据库**: MySQL 8.0+
- **内存**: 推荐 2GB+
- **磁盘**: 推荐 10GB+ (用于日志存储)

### 快速启动

```bash
# 解压发布包
tar -xzf log-manage-assembly-1.0.0-distribution.tar.gz

# 配置数据库连接
vi config/application-prod.yml

# 启动应用
java -jar -Dspring.profiles.active=prod log-manage-server-1.0.0-exec.jar
```

### Docker部署

```bash
# 构建和启动
./build-start-docker.sh

# 清理镜像
./clean-docker-images.sh
```

## 📋 配置示例

### 生产环境配置

```yaml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://localhost:3306/log_manage_system
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:your_password}

log-config:
  log-home: "/var/log/log-manage-system"
  max-history: 30
  total-size-cap: "1GB"
  max-file-size: "100MB"
```

## 🔧 API接口

主要REST API端点：

- **认证管理**: `/api/auth/**`
- **用户管理**: `/api/users/**`
- **数据源管理**: `/api/datasources/**`
- **日志搜索**: `/api/logs/**`
- **SQL编辑器**: `/api/sql-editor/**`
- **Logstash管理**: `/api/logstash/processes/**`
- **机器管理**: `/api/machines/**`

**Swagger文档**: `http://your-server:port/swagger-ui/index.html`

## 🐛 已知问题

1. **限制**: 当前版本不支持集群模式部署
2. **性能**: 大量并发查询时可能需要调整数据库连接池配置
3. **兼容性**: 仅测试了MySQL 8.x版本

## 🔮 后续版本计划

### v1.1.0 (计划)

- 集群模式支持
- 更多数据源类型支持
- 性能监控仪表板
- 国际化支持

### v1.2.0 (计划)

- 微服务架构重构
- 云原生部署支持
- 实时日志监控

## 📞 技术支持

- **文档**: [项目README](../README.md)
- **配置说明**: [日志配置文档](LOGGING_CONFIGURATION.md)
- **部署指南**: [部署检查清单](DEPLOYMENT_CHECKLIST_v1.0.0.md)
- **问题反馈**: 请通过项目仓库提交Issue

## 🙏 致谢

感谢所有参与项目开发和测试的团队成员。

---

**版本**: 1.0.0  
**构建时间**: 2025-05-23  
**构建环境**: OpenJDK 17, Maven 3.9+
