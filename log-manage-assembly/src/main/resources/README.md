# 日志管理系统

## 系统目录结构

```
.
├── bin/                   # 启动脚本目录
│   ├── start.sh           # 启动脚本 (Linux/Mac)
│   ├── stop.sh            # 停止脚本 (Linux/Mac)
│   ├── restart.sh         # 重启脚本 (Linux/Mac)
│   ├── status.sh          # 状态检查脚本 (Linux/Mac)
│   ├── start.bat          # 启动脚本 (Windows)
│   ├── stop.bat           # 停止脚本 (Windows)
│   ├── restart.bat        # 重启脚本 (Windows)
│   ├── status.bat         # 状态检查脚本 (Windows)
│   └── lib/               # 脚本库目录
│       └── functions.sh   # 公共函数库
├── config/                # 配置文件目录
│   ├── application.yml         # 默认应用配置
│   ├── application-dev.yml     # 开发环境配置
│   ├── application-prod.yml    # 生产环境配置
│   ├── logback-dev.xml         # 开发环境日志配置
│   └── logback-prod.xml        # 生产环境日志配置
├── lib/                   # 依赖库目录
├── logs/                  # 日志输出目录 (运行时创建)
├── static/                # 前端静态资源目录
└── log-manage-server.jar  # 主程序 JAR 包
```

## 系统要求

- JDK 17 或更高版本
- MySQL 8.0 或更高版本

## 前端和后端集成

系统采用了前后端一体化设计，提供了统一的访问入口：

- **根路径** (`/`): 由前端应用接管，处理所有页面路由
- **API路径** (`/api/**`): 由后端服务处理，提供数据接口

这种设计的好处是：

1. 单一入口，避免了跨域问题
2. 简化部署流程，无需分别部署前端和后端
3. 前端路由刷新不会丢失（例如直接访问`/users/1`仍然能加载正确页面）

## 快速开始

1. 配置数据库连接

编辑 `config/application-{env}.yml` 文件，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/log_manage_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```

2. 启动应用

**Linux/Mac**:
```bash
# 默认使用开发环境 (dev)
./bin/start.sh

# 指定环境
./bin/start.sh --env prod

# 启用调试模式
./bin/start.sh --debug

# 查看所有选项
./bin/start.sh --help
```

**Windows**:
```
# 默认使用开发环境 (dev)
bin\start.bat

# 指定环境
bin\start.bat --env prod

# 启用调试模式
bin\start.bat --debug

# 查看所有选项
bin\start.bat --help
```

3. 停止应用

**Linux/Mac**:
```bash
./bin/stop.sh

# 强制停止
./bin/stop.sh --force
```

**Windows**:
```
bin\stop.bat

# 强制停止
bin\stop.bat --force
```

4. 检查应用状态

**Linux/Mac**:
```bash
./bin/status.sh
```

**Windows**:
```
bin\status.bat
```

5. 重启应用

**Linux/Mac**:
```bash
./bin/restart.sh [选项]
```

**Windows**:
```
bin\restart.bat [选项]
```

## 多环境配置

系统支持多环境配置，主要包括：

- **dev**: 开发环境 (默认)
  - 更详细的日志输出
  - 更多的调试信息
  - 更小的内存消耗

- **prod**: 生产环境
  - 优化的日志级别
  - 更大的内存分配
  - 异步日志输出

您可以通过在启动脚本中添加 `--env` 参数来指定环境：

```bash
./bin/start.sh --env prod
```

## 版本管理

系统支持自动版本显示功能，无需在发布新版本时手动修改脚本中的版本号。版本号来源于以下几个方面：

1. **Maven版本**：从项目的`pom.xml`文件中读取，这是版本号的主要来源
2. **Spring Boot配置**：应用启动时会读取`spring.application.version`配置
3. **版本文件**：在应用目录下的`version.txt`文件

在应用包装和运行过程中，版本信息会自动同步：

- 打包时，Maven版本会写入`version.txt`文件
- 应用启动时，会显示在启动标题中
- 所有管理脚本会自动读取`version.txt`显示一致的版本

如需修改版本，只需在主`pom.xml`文件中更新版本号即可。

## 服务器配置说明

可通过修改 `config/application-{env}.yml` 文件来自定义应用配置，主要配置项包括：

- 服务器端口
- 数据库连接
- 日志级别
- JWT 安全配置
- SSH 连接配置

## 日志配置

日志配置在 `config/logback-{env}.xml` 文件中，默认会在 `logs` 目录下生成：

- `log-manage-system.log`: 所有日志
- `log-manage-system-error.log`: 仅错误日志

## 问题排查

如果遇到启动问题，请检查：

1. JDK 版本是否正确（`java -version`）
2. 数据库连接信息是否正确
3. 检查 `logs` 目录下的日志文件，特别是 `startup.log`
4. 使用状态检查脚本 `status.sh` 或 `status.bat` 查看应用状态

## 联系支持

如有问题，请联系: support@hina.com 