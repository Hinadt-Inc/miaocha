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
* **Logstash进程管理**: 远程部署、配置和监控Logstash实例。
* **机器管理**: 管理服务器连接，支持SSH认证。
* **模块权限控制**: 精细化的模块级别权限管理。

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

### API接口

主要API接口包括：

* `/api/logstash/processes`: 管理Logstash进程
* `/api/logstash/processes/{id}/config`: 管理进程配置
* `/api/logstash/processes/{id}/machines/{machineId}/start`: 启动特定机器上的进程
* `/api/logstash/processes/{id}/machines/{machineId}/stop`: 停止特定机器上的进程
* `/api/logstash/processes/{id}/machines/{machineId}/initialize`: 初始化特定机器上的进程环境

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

## `log-manage-server` 详细介绍

### 文件结构

`com.hina.log` 为 `log-manage-server` 模块的根包路径。

根包目录下有 Spring Boot 启动类 `LogManageSystemApplication.java`。Spring Boot 默认扫描启动类所在文件目录及子文件目录。

`log-manage-server` 主要遵循分层架构模式，主要包含以下包结构：

*   **`LogManageSystemApplication.java`**: Spring Boot 应用启动类。

*   **`controller`**: API 接口层 (Endpoint)
    *   负责接收来自客户端（如 `log-manage-ui` 或其他微服务）的HTTP请求。
    *   对请求参数进行校验和转换。
    *   调用 `service` 层的业务逻辑。
    *   返回HTTP响应给客户端。
    *   包含的主要控制器:
        * `AuthController`: 用户认证相关接口
        * `DatasourceController`: 数据源管理接口
        * `LogSearchController`: 日志搜索接口
        * `LogstashProcessController`: Logstash进程管理接口
        * `MachineController`: 服务器机器管理接口
        * `ModulePermissionController`: 模块权限管理接口
        * `SqlEditorQueryController`: SQL编辑器查询接口
        * `UserController`: 用户管理接口

*   **`service`**: 业务逻辑层 (Application)
    *   包含核心业务逻辑的实现。
    *   处理具体的业务流程，组合调用 `mapper` 或其他外部服务。
    *   通常会在这里进行事务管理。
    *   `impl` 子包通常存放接口的实现类。
    *   主要服务:
        * `DatasourceService`: 数据源管理服务
        * `LogSearchService`: 日志搜索服务
        * `LogstashProcessService`: Logstash进程管理服务
        * `MachineService`: 机器管理服务
        * `ModulePermissionService`: 模块权限管理服务
        * `SqlQueryService`: SQL查询服务
        * `UserService`: 用户管理服务

*   **`dto` (Data Transfer Object)**: 数据传输对象
    *   用于在不同层之间传递数据，特别是 API 接口层和业务逻辑层之间。
    *   定义了请求和响应的数据结构。

*   **`entity`**: 领域实体 / 数据模型 (Domain)
    *   表示核心业务对象，通常与数据库表结构对应。
    *   包含对象的属性和基本行为。
    *   主要实体:
        * `Datasource`: 数据源配置信息
        * `LogstashMachine`: Logstash机器配置
        * `LogstashProcess`: Logstash进程信息
        * `LogstashTask`: Logstash任务配置
        * `Machine`: 服务器机器信息
        * `SqlQueryHistory`: SQL查询历史记录
        * `User`: 用户信息
        * `UserModulePermission`: 用户模块权限配置

*   **`mapper` / `dao`**: 数据访问层 (Domain/Infrastructure)
    *   负责与数据库进行交互，执行CRUD（创建、读取、更新、删除）操作。
    *   通常使用 MyBatis 或 JPA 等持久化框架。
    *   将数据库记录映射到 `entity` 对象。

*   **`config`**: 配置模块
    *   包含应用的配置类，如数据库连接、消息队列、缓存、安全配置等。
    *   使用 Java 配置类（`@Configuration`）或加载 `application.yml`/`properties` 文件。
    *   例如：`SecurityConfig`, `MyBatisConfig`, `RedisConfig`。

*   **`security`**: 安全模块
    *   处理认证（Authentication）和授权（Authorization）。
    *   可能包含 Spring Security 的配置、Token 处理、用户权限管理等。

*   **`filter`**: 过滤器
    *   用于在HTTP请求到达 Controller 之前或响应返回客户端之前进行预处理或后处理。
    *   常见的用途包括：日志记录、请求参数修改、身份验证、CORS 处理等。

*   **`util`**: 工具类 (Common)
    *   存放通用的辅助方法和工具类。
    *   例如：日期处理、字符串操作、加密解密、HTTP客户端等。

*   **`exception`**: 异常处理 (Common)
    *   定义自定义异常类。
    *   全局异常处理器（`@ControllerAdvice`），用于统一处理应用中发生的异常并返回标准化的错误响应。

*   **`converter` / `assembler`**: 对象转换器 (Common/Application)
    *   负责不同对象之间的转换，如 `DTO` 与 `Entity` 之间的相互转换。
    *   确保各层之间的解耦。

*   **`annotation`**: 自定义注解 (Common/Config)
    *   定义项目中使用的自定义注解，例如用于日志记录、权限控制等AOP场景。

*   **`logstash` / `ssh`**: (Domain/Infrastructure - 特定功能)
    *   `logstash`: 包含与 Logstash 集成相关的代码，如配置管理、状态控制和命令执行等。
    *   `ssh`: 包含通过 SSH 协议与其他服务器交互的功能，例如远程执行命令或文件传输。

### 模块职责划分说明

*   **`controller` (Endpoint)**: 作为服务的入口，直接面向客户端。它们将HTTP请求转换为内部方法调用，并将业务逻辑层的处理结果转换为HTTP响应。不同类型的客户端（web, mobile, internal APIs）可能需要不同的 Controller 设计和安全考虑。
*   **`service` (Application)**: 封装了核心的业务规则和流程。这一层编排对 `mapper` 的调用，处理事务，并执行所有特定于应用程序的逻辑。
*   **`dto`**: 用于定义清晰的数据契约，在各层之间传递信息，避免直接暴露 `entity` 对象，特别是对于外部API。
*   **`entity` (Domain)**: 代表了业务领域中的核心概念和数据结构。
*   **`mapper`/`dao` (Domain/Infrastructure)**: 抽象了数据持久化逻辑，使得 `service` 层不需要关心具体的数据库实现技术。
*   **`config`**: 集中管理应用的所有配置，包括第三方组件的集成和应用行为的调整。推荐使用基于注解的配置以提高效率和可维护性。
*   **`security`**: 专注于应用的安全方面，包括用户身份验证和访问控制。
*   **`filter`**: 提供了一种在请求处理管道中插入自定义逻辑的机制，用于横切关注点，如日志记录、安全检查等。
*   **`util`, `exception`, `converter`, `annotation` (Common)**: 这些包提供了项目中广泛使用的通用功能、异常处理机制、对象转换逻辑和自定义注解，以促进代码重用和保持代码整洁。

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