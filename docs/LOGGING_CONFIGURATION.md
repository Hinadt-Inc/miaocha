# 日志配置说明

## 概述

本项目实现了多格式日志输出配置，支持普通格式和JSON格式日志，并根据不同环境提供不同的日志输出策略。

## 环境配置策略

### 开发环境 (dev)
- **控制台输出**: 彩色格式，便于开发调试
- **普通格式文件**: `log-manage-system.log`
- **JSON格式文件**: `log-manage-system-json.log` 和 `log-manage-system-json-error.log`
- **日志级别**: 
  - 根日志级别: INFO
  - 应用日志级别: DEBUG (`com.hina.log`)

### 测试环境 (test)
- **控制台输出**: 彩色格式
- **普通格式文件**: `log-manage-system.log`
- **JSON格式文件**: `log-manage-system-json.log` 和 `log-manage-system-json-error.log`
- **日志级别**: INFO

### 生产环境 (prod)
- **仅JSON格式文件**: `log-manage-system-json.log` 和 `log-manage-system-json-error.log`
- **无控制台输出**
- **无普通格式文件**
- **日志级别**: 
  - 根日志级别: INFO
  - 应用日志级别: INFO (`com.hina.log`)
  - 框架日志级别: WARN (`org.springframework`, `org.hibernate`, `org.apache`)

## 日志文件说明

### 普通格式日志文件
- **文件名**: `log-manage-system.log`
- **格式**: `时间 [线程] 级别 类名 - 消息`
- **示例**: 
  ```
  2024-01-15 10:30:45.123 [main] INFO  com.hina.log.service.UserService - 用户登录成功: admin
  ```

### JSON格式日志文件

#### INFO级别日志 (`log-manage-system-json.log`)
- **内容**: 所有级别的日志，但排除ERROR级别
- **格式**: JSON格式，包含以下字段：
  ```json
  {
    "service": "log-manage-system",
    "time": "2024-01-15 10:30:45.123",
    "level": "INFO",
    "logger": "com.hina.log.service.UserService",
    "line": "45",
    "thread": "main",
    "method": "login",
    "msg": "用户登录成功: admin",
    "stacktrace": ""
  }
  ```

#### ERROR级别日志 (`log-manage-system-json-error.log`)
- **内容**: 只包含ERROR级别的日志
- **格式**: JSON格式，包含完整的堆栈信息
- **示例**:
  ```json
  {
    "service": "log-manage-system",
    "time": "2024-01-15 10:30:45.123",
    "level": "ERROR",
    "logger": "com.hina.log.service.UserService",
    "line": "67",
    "thread": "main",
    "method": "authenticate",
    "msg": "用户认证失败",
    "stacktrace": "java.lang.RuntimeException: 密码错误\\n\\tat com.hina.log..."
  }
  ```

## 日志轮转配置

### 文件轮转策略
- **时间维度**: 按天轮转
- **大小维度**: 单文件最大100MB
- **保留策略**: 默认保留30天
- **总大小限制**: 默认1GB
- **命名格式**: `{文件名}.{日期}.{序号}.log`

### 示例文件列表
```
logs/
├── log-manage-system.log                    # 当前普通格式日志
├── log-manage-system-json.log               # 当前JSON格式INFO日志
├── log-manage-system-json-error.log         # 当前JSON格式ERROR日志
└── archive/
    ├── log-manage-system.2024-01-14.0.log
    ├── log-manage-system.2024-01-14.1.log
    ├── log-manage-system-json.2024-01-14.0.log
    └── log-manage-system-json-error.2024-01-14.0.log
```

## 配置参数

可以通过以下配置参数调整日志行为：

```yaml
log-config:
  log-home: "./logs"              # 日志文件目录
  max-history: 30                 # 保留天数
  total-size-cap: "1GB"          # 总大小限制
  clean-history-on-start: true    # 启动时清理历史文件
  max-file-size: "100MB"         # 单文件最大大小
  queue-size: 1024               # 异步队列大小
  discarding-threshold: 0        # 丢弃阈值
```

## 异步日志配置

所有文件输出都采用异步方式，提高应用性能：

- **队列大小**: 1024
- **丢弃阈值**: 0 (不丢弃日志)
- **异步appender**: 
  - `ASYNC_FILE`: 普通格式异步输出
  - `ASYNC_JSON_INFO`: JSON格式INFO异步输出
  - `ASYNC_JSON_ERROR`: JSON格式ERROR异步输出

## 使用建议

### 开发阶段
- 使用控制台输出进行实时调试
- 查看普通格式文件了解业务流程
- 使用JSON格式文件进行日志分析工具集成

### 测试阶段
- 重点关注JSON格式日志
- 验证日志格式是否满足日志分析系统要求
- 测试日志轮转和性能

### 生产环境
- 仅使用JSON格式日志
- 集成到ELK或其他日志分析系统
- 监控日志文件大小和磁盘使用情况

## 日志分析集成

JSON格式日志特别适合与以下工具集成：

1. **Elasticsearch + Logstash + Kibana (ELK)**
2. **Fluentd**
3. **Prometheus + Grafana**
4. **Splunk**

## 故障排查

### 常见问题

1. **日志文件不生成**
   - 检查日志目录权限
   - 确认配置文件路径正确

2. **JSON格式错误**
   - 检查logstash-logback-encoder依赖
   - 验证JSON模板格式

3. **性能问题**
   - 调整异步队列大小
   - 优化日志输出频率

### 配置验证

启动应用时可以通过以下日志确认配置正确：
```
[main] INFO  org.springframework.boot.context.logging.ClasspathLoggingApplicationListener - Application started with classpath: ...
```

## 扩展配置

### 添加自定义Appender

可以通过继承现有配置添加自定义的日志输出：

```xml
<appender name="CUSTOM_APPENDER" class="...">
    <!-- 自定义配置 -->
</appender>

<springProfile name="custom">
    <root level="INFO">
        <appender-ref ref="CUSTOM_APPENDER" />
    </root>
</springProfile>
```

### MDC支持

JSON格式支持MDC (Mapped Diagnostic Context)，可以添加自定义字段：

```java
MDC.put("userId", "12345");
MDC.put("requestId", "req-789");
logger.info("处理用户请求");
MDC.clear();
```

这将在JSON日志中自动包含这些字段。 