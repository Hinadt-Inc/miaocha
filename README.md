# 海纳 Doris日志管理系统

## 项目概述

海纳 Doris日志管理系统是一个用于集中管理和分析应用日志的平台。它提供了日志的采集、存储、查询、分析和可视化功能，帮助开发和运维团队快速定位问题、监控应用状态和优化系统性能。

## 主要模块

* **`log-manage-server`**: 后端核心服务，负责日志处理、存储和API接口。
* **`log-manage-ui`**: 前端用户界面，提供日志查询、可视化和系统管理功能。
* **`log-manage-assembly`**: 项目打包和部署模块。

## 系统功能

* **用户认证与权限管理**: 基于JWT的认证机制，用户登录和权限控制。
* **数据源管理**: 添加、配置和管理多种数据源连接。
* **日志搜索与分析**: 高级日志搜索功能，支持多种查询条件和过滤。
* **SQL编辑器**: 支持直接编写和执行SQL查询，查看和导出结果。
* **Logstash进程管理**: 远程部署、配置和监控Logstash实例，支持重新初始化失败的实例。
* **机器管理**: 管理服务器连接，支持SSH认证。
* **模块权限控制**: 精细化的模块级别权限管理。
* **日志配置管理**: 配置和监控日志系统，支持多格式日志输出。

## 技术栈

### 后端技术
* **语言与框架**: Java 17, Spring Boot, Spring Security
* **API文档**: SpringDoc/OpenAPI 3.0
* **ORM**: MyBatis
* **数据库**: MySQL
* **认证**: JWT (JSON Web Token)
* **数据库迁移**: Flyway
* **SSH**: Apache SSHD, JSch
* **导出功能**: Apache POI
* **加密**: Bouncy Castle
* **日志系统**: Logback + Logstash JSON Encoder，支持多格式日志输出

### 前端技术
* **框架**: React 18
* **UI组件库**: Ant Design 5
* **HTTP客户端**: Axios
* **构建工具**: Vite

## 日志搜索与分析功能

日志搜索与分析模块是系统的核心功能之一，提供了强大的日志检索和可视化能力。

### 核心功能

1. **日志明细查询**
   * 支持多条件组合查询，包括时间范围、关键词、字段值等
   * 分页查询结果展示
   * 自定义字段排序
   * 支持SQL条件表达式

2. **时间分布分析**
   * 根据指定时间间隔统计日志数量分布
   * 生成时间柱状图数据
   * 支持按分钟、小时、天等粒度聚合

3. **字段分布统计**
   * 分析指定字段的TOP N值分布
   * 支持同时分析多个字段的分布情况
   * 利用Doris TOPN函数高效计算

4. **表结构信息查询**
   * 获取日志表字段详细信息
   * 支持显示字段名称、数据类型、字段描述等
   * 用于动态构建查询界面

### API接口

* `/api/logs/search/details`: 执行日志明细查询
* `/api/logs/search/histogram`: 执行日志时间分布查询
* `/api/logs/search/field-distributions`: 执行字段分布统计
* `/api/logs/columns`: 获取日志表字段信息

### 数据模型

日志搜索功能支持以下主要数据模型：
* `LogSearchDTO`: 封装日志搜索请求参数
* `LogDetailResultDTO`: 包含日志明细查询结果
* `LogHistogramResultDTO`: 包含时间分布统计结果
* `LogFieldDistributionResultDTO`: 包含字段分布统计结果
* `SchemaInfoDTO.ColumnInfoDTO`: 表字段结构信息

## Logstash进程管理

Logstash进程管理模块提供了对Logstash实例的完整生命周期管理，包括部署、配置、启动、监控和停止。

### 状态机实现

系统使用状态模式实现了Logstash进程的状态管理，确保进程在不同状态下的操作逻辑清晰可控。

#### Logstash状态定义

Logstash进程在系统中有以下8种状态：

* `INITIALIZING`: 初始化中 - 创建过程中的初始状态
* `NOT_STARTED`: 未启动 - 初始化完成但未启动
* `STARTING`: 正在启动 - 启动进程中
* `RUNNING`: 运行中 - 进程正常运行
* `STOPPING`: 正在停止 - 停止进程中
* `START_FAILED`: 启动失败 - 启动过程中遇到错误
* `STOP_FAILED`: 停止失败 - 停止过程中遇到错误
* `INITIALIZE_FAILED`: 初始化失败 - 可以重新初始化

#### 状态转换流程

1. **初始化流程**:
   * 初始状态: `INITIALIZING`
   * 成功: `INITIALIZING` → `NOT_STARTED`
   * 失败: `INITIALIZING` → `INITIALIZE_FAILED`

2. **启动流程**:
   * 初始状态: `NOT_STARTED`
   * 启动中: `NOT_STARTED` → `STARTING`
   * 成功: `STARTING` → `RUNNING`
   * 失败: `STARTING` → `START_FAILED`

3. **停止流程**:
   * 初始状态: `RUNNING`
   * 停止中: `RUNNING` → `STOPPING`
   * 成功: `STOPPING` → `NOT_STARTED`
   * 失败: `STOPPING` → `STOP_FAILED`

4. **恢复流程**:
   * 从失败状态重试: `START_FAILED` → `STARTING` 或 `INITIALIZE_FAILED` → `INITIALIZING`

#### 状态处理器

系统为每个状态实现了专门的处理器类，负责该状态下的操作逻辑：

* `InitializingStateHandler`: 处理初始化状态下的操作
* `NotStartedStateHandler`: 处理未启动状态下的操作
* `StartingStateHandler`: 处理启动中状态下的操作
* `RunningStateHandler`: 处理运行中状态下的操作
* `StoppingStateHandler`: 处理停止中状态下的操作
* `StartFailedStateHandler`: 处理启动失败状态下的操作
* `StopFailedStateHandler`: 处理停止失败状态下的操作
* `InitializeFailedStateHandler`: 处理初始化失败状态下的操作

### 上下文管理

系统使用`LogstashMachineContext`类作为状态机上下文，实现：

* 保存当前状态和状态处理器
* 管理状态转换
* 委托操作给当前状态处理器
* 提供异步操作接口

### 主要功能

1. **环境部署与初始化**
   * 远程部署Logstash软件包
   * 初始化配置环境
   * 上传和管理配置文件
   * **支持重新初始化失败的机器实例**

2. **配置管理**
   * 支持管理主配置文件
   * 支持自定义JVM选项
   * 支持修改logstash.yml配置
   * 配置同步和版本控制

3. **进程控制**
   * 启动Logstash进程
   * 停止Logstash进程
   * 监控进程状态
   * 自动恢复失败进程

4. **多机器部署**
   * 支持在多台机器上部署同一进程
   * 独立管理每台机器上的进程状态
   * 批量操作多台机器

### 重新初始化功能

当Logstash机器实例初始化失败时，系统提供了重新初始化功能：

#### 功能特性
* **状态检查**: 严格限制只有 `INITIALIZE_FAILED` 状态的机器才能重新初始化
* **两种操作模式**:
  - 单机器重新初始化：针对特定失败的机器进行重新初始化
  - 批量重新初始化：一次性重新初始化所有失败的机器
* **完整的初始化流程**: 删除原有目录、重新创建环境、上传配置等
* **事务保证**: 确保操作的原子性和数据一致性

#### API接口
* `POST /api/logstash/processes/{id}/reinitialize`: 重新初始化所有失败的机器
* `POST /api/logstash/processes/{id}/machines/{machineId}/reinitialize`: 重新初始化指定机器

### API接口

主要API接口包括：

* `/api/logstash/processes`: 管理Logstash进程
* `/api/logstash/processes/{id}/config`: 管理进程配置
* `/api/logstash/processes/{id}/start`: 全局启动进程
* `/api/logstash/processes/{id}/stop`: 全局停止进程
* `/api/logstash/processes/{id}/machines/{machineId}/start`: 启动特定机器上的进程
* `/api/logstash/processes/{id}/machines/{machineId}/stop`: 停止特定机器上的进程
* `/api/logstash/processes/{id}/reinitialize`: 重新初始化所有失败的机器
* `/api/logstash/processes/{id}/machines/{machineId}/reinitialize`: 重新初始化特定机器

## API接口文档

访问Swagger UI文档: `http://<serverIp>:<port>/swagger-ui/index.html#/`

## 项目构建与运行

### 构建项目
```bash
mvn clean package
```

### 使用Docker构建和启动
```bash
./build-start-docker.sh
```

### 清理Docker镜像
```bash
./clean-docker-images.sh
```

## 环境信息

### 测试环境
* **后端API地址**: `http://10.254.133.210:32088/`

## 配置信息

系统支持多环境配置，包括:
* `application.yml`: 基础配置
* `application-dev.yml`: 开发环境配置
* `application-test.yml`: 测试环境配置
* `application-prod.yml`: 生产环境配置

主要配置项包括:
* 数据库连接信息
* JWT认证配置
* Logstash配置
* SQL查询导出配置

## 日志配置

项目采用了灵活的多格式日志配置方案，支持根据环境自动切换日志输出策略：

### 环境策略

* **开发环境 (dev)**: 控制台彩色输出 + 普通格式文件 + JSON格式文件
* **测试环境 (test)**: 控制台输出 + 普通格式文件 + JSON格式文件
* **生产环境 (prod)**: 仅JSON格式文件输出

### 日志文件类型

1. **普通格式日志**: `log-manage-system.log`
   - 适合开发调试和人工阅读
   - 标准文本格式，包含时间、线程、级别、类名和消息

2. **JSON格式日志**:
   - `log-manage-system-json.log`: 包含INFO及以下级别
   - `log-manage-system-json-error.log`: 仅包含ERROR级别
   - 适合日志分析工具和监控系统集成

### 日志轮转

* 按日期和大小轮转 (单文件最大100MB)
* 默认保留30天历史日志
* 总大小限制1GB
* 异步输出提高性能

详细配置说明请参考：[日志配置文档](docs/LOGGING_CONFIGURATION.md)

## `log-manage-server` 详细介绍

### 项目架构

`log-manage-server` 采用分层架构设计，遵循领域驱动设计(DDD)原则，主要分为以下几层：

#### 目录结构

```
log-manage-server/src/main/java/com/hina/log/
├── LogManageSystemApplication.java     # Spring Boot 启动类
├── endpoint/                          # API接口层 (Interface Layer)
├── application/                       # 应用服务层 (Application Layer)
│   ├── service/                      # 应用服务
│   ├── logstash/                     # Logstash相关应用服务
│   ├── security/                     # 安全相关配置
│   └── filter/                       # 过滤器
├── domain/                           # 领域层 (Domain Layer)
│   ├── entity/                       # 领域实体
│   ├── dto/                          # 数据传输对象
│   ├── mapper/                       # 数据访问接口
│   └── converter/                    # 对象转换器
├── common/                           # 通用模块 (Infrastructure Layer)
│   ├── util/                         # 工具类
│   ├── exception/                    # 异常处理
│   ├── ssh/                          # SSH工具
│   └── annotation/                   # 自定义注解
└── config/                           # 配置模块
```

### 分层架构说明

#### 1. **Endpoint层** (Interface Layer)
负责接收来自客户端的HTTP请求，进行参数校验和格式转换。

**主要组件:**
* `AuthEndpoint`: 用户认证相关接口
* `DatasourceEndpoint`: 数据源管理接口
* `LogSearchEndpoint`: 日志搜索接口
* `LogstashProcessEndpoint`: Logstash进程管理接口
* `MachineEndpoint`: 服务器机器管理接口
* `ModulePermissionEndpoint`: 模块权限管理接口
* `SqlEditorQueryEndpoint`: SQL编辑器查询接口
* `UserEndpoint`: 用户管理接口

#### 2. **Application层** (Application Layer)
包含应用服务和业务流程编排逻辑。

**主要组件:**
* **`service/`**: 核心应用服务
  - `DatasourceService`: 数据源管理服务
  - `LogSearchService`: 日志搜索服务
  - `LogstashProcessService`: Logstash进程管理服务
  - `MachineService`: 机器管理服务
  - `ModulePermissionService`: 模块权限管理服务
  - `SqlQueryService`: SQL查询服务
  - `UserService`: 用户管理服务

* **`logstash/`**: Logstash专门的应用服务
  - `LogstashProcessDeployService`: Logstash部署服务
  - `LogstashConfigSyncService`: 配置同步服务
  - `state/`: 状态管理相关类
  - `command/`: 命令模式实现
  - `task/`: 任务管理

* **`security/`**: 安全相关配置
* **`filter/`**: HTTP请求过滤器

#### 3. **Domain层** (Domain Layer)
包含核心业务实体和业务逻辑。

**主要组件:**
* **`entity/`**: 领域实体
  - `Datasource`: 数据源配置信息
  - `LogstashMachine`: Logstash机器配置
  - `LogstashProcess`: Logstash进程信息
  - `Machine`: 服务器机器信息
  - `User`: 用户信息
  - `UserModulePermission`: 用户模块权限配置

* **`dto/`**: 数据传输对象
  - 用于不同层之间的数据传输
  - 包含请求和响应的数据结构

* **`mapper/`**: 数据访问接口
  - MyBatis映射器接口
  - 负责与数据库交互

* **`converter/`**: 对象转换器
  - 负责DTO与Entity之间的转换
  - 确保各层之间的解耦

#### 4. **Common层** (Infrastructure Layer)
提供基础设施和通用功能支持。

**主要组件:**
* **`util/`**: 工具类
  - 通用的辅助方法和工具类
  - 例如：日期处理、字符串操作、加密解密等

* **`exception/`**: 异常处理
  - 自定义异常类
  - 全局异常处理器

* **`ssh/`**: SSH工具
  - SSH连接和命令执行
  - 远程文件操作

* **`annotation/`**: 自定义注解
  - 用于AOP、权限控制等

#### 5. **Config层**
应用配置管理。

**主要组件:**
* 数据库配置
* 安全配置
* MyBatis配置
* 其他第三方组件配置

### 核心设计模式

1. **状态模式**: 用于Logstash机器状态管理
2. **命令模式**: 用于SSH命令执行
3. **策略模式**: 用于不同类型的数据源处理
4. **观察者模式**: 用于任务状态监控
5. **工厂模式**: 用于对象创建

### 技术特性

* **分层架构**: 清晰的职责分离
* **领域驱动**: 以业务为核心的设计
* **状态管理**: 完善的状态机实现
* **异步处理**: 支持异步任务执行
* **事务管理**: 确保数据一致性
* **安全控制**: 完善的认证授权机制

## 前端 (`log-manage-ui`) 介绍

前端使用React框架开发，基于Ant Design组件库构建用户界面，提供以下功能:

* 用户登录和权限管理界面
* 数据源配置和管理
* 日志搜索与分析界面
* SQL编辑器
* Logstash进程管理
* 机器连接管理

## 数据库结构

系统使用Flyway进行数据库版本控制和迁移管理，数据库脚本位于 `src/main/resources/db/migration` 目录。

## 如何开发和调试

1. 克隆项目代码库
2. 配置MySQL数据库
3. 修改 `application-dev.yml` 中的数据库连接信息
4. 启动后端服务: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
5. 启动前端开发服务器:
   ```bash
   cd log-manage-ui/src/main/frontend
   npm install
   npm start
   ```

## 如何贡献

1. Fork项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送分支 (`git push origin feature/amazing-feature`)
5. 打开Pull Request

## License

[TODO: 添加项目许可证信息]

## 📞 技术支持

- **文档**: [项目README](README.md)
- **配置说明**: [日志配置文档](docs/LOGGING_CONFIGURATION.md)
- **发布说明**: [1.0.0版本发布说明](docs/RELEASE_NOTES_v1.0.0.md)
- **部署指南**: [部署检查清单](docs/DEPLOYMENT_CHECKLIST_v1.0.0.md)
- **问题反馈**: 请通过项目仓库提交Issue 