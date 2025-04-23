# 日志管理系统 (Log Management System)

一个功能全面的日志管理系统，支持多数据源连接、日志采集、检索、分析和可视化。该系统使用Spring Boot 3.4和Java 17开发后端，并提供现代化的前端界面。

## 项目结构

```
log-manage-system/
├── log-manage-assembly/     # 项目打包和分发模块
│   └── ...
├── log-manage-server/       # 后端服务模块
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/hina/log/
│   │       │       ├── annotation/     # 自定义注解
│   │       │       ├── config/         # 应用配置
│   │       │       ├── controller/     # API控制器
│   │       │       ├── converter/      # 数据转换器
│   │       │       ├── dto/            # 数据传输对象
│   │       │       ├── entity/         # 数据实体
│   │       │       ├── exception/      # 异常处理
│   │       │       ├── filter/         # 过滤器
│   │       │       ├── logstash/       # Logstash集成
│   │       │       ├── mapper/         # MyBatis数据访问
│   │       │       ├── security/       # 安全配置
│   │       │       ├── service/        # 业务服务
│   │       │       ├── ssh/            # SSH连接工具
│   │       │       ├── util/           # 工具类
│   │       │       └── LogManageSystemApplication.java 
│   │       └── resources/              # 应用资源文件
│   └── pom.xml
├── log-manage-ui/           # 前端界面模块
│   ├── src/
│   └── pom.xml
├── Dockerfile               # Docker部署文件
├── build-start-docker.sh    # Docker构建和启动脚本
├── clean-docker-images.sh   # Docker清理脚本
├── pom.xml                  # 主项目POM文件
└── .gitignore               # Git忽略文件
```

## 技术栈

### 后端
- Java 17
- Spring Boot 3.4
- Spring Security
- MyBatis 3.0
- MySQL 8
- JWT认证
- Swagger/OpenAPI 3
- Apache SSHD (SSH客户端)
- Logstash集成
- Maven

### 前端
- 现代化JavaScript/TypeScript框架
- 响应式设计
- 图表和可视化组件

## 功能模块

### 用户认证与授权
- 基于JWT的认证机制
- 角色权限管理 (SUPER_ADMIN, ADMIN, USER)
- 刷新令牌机制

### 数据源管理
- 支持多种数据库连接
- 数据源连接测试
- 用户数据源权限管理

### 日志检索分析
- 高级日志搜索功能
- 基于时间段、关键字等条件的查询
- 表结构和字段信息获取

### SQL编辑器
- SQL查询执行
- 查询结果展示和导出

### Logstash进程管理
- 远程部署和管理Logstash实例
- 进程状态监控和控制

### 机器管理
- 服务器连接和管理
- SSH密钥认证支持

## API接口文档

系统使用Swagger/OpenAPI 3.0提供完整的API文档。以下是主要API模块的概述：

### 认证模块 (/api/auth)
- `POST /api/auth/login` - 用户登录，返回JWT令牌
- `POST /api/auth/refresh` - 刷新JWT令牌

### 用户管理 (/api/users)
- 用户增删改查
- 密码管理
- 权限设置

### 数据源管理 (/api/datasources)
- 创建、更新、删除数据源
- 测试数据源连接
- 获取数据源信息和列表

### 日志检索 (/api/logs)
- 执行日志搜索查询
- 获取日志表字段信息

### SQL编辑器 (/api/sql)
- 执行SQL查询
- 查询历史管理

### Logstash管理 (/api/logstash/processes)
- 部署和启动Logstash实例
- 监控和控制进程状态

### 机器管理 (/api/machines)
- 服务器连接管理
- 密钥管理

## 部署方式

### Docker部署
系统提供了完整的Docker支持，可以使用以下脚本快速部署：

```bash
# 构建并启动Docker容器
./build-start-docker.sh

# 清理Docker镜像
./clean-docker-images.sh
```

### 安全配置
系统实现了全面的安全机制：
- API访问控制
- CORS配置
- JWT令牌验证
- 角色权限管理

## 系统要求
- Java 17或更高版本
- MySQL 8或更高版本
- Docker (用于容器化部署)

## 开发环境设置
1. 克隆仓库
2. 配置数据库连接
3. 构建项目：`mvn clean install`
4. 运行后端：`java -jar log-manage-assembly/target/log-manage-system.jar`
5. 前端开发模式：参考前端目录下的README

## 贡献
欢迎提交Pull Request或Issue来改进项目。

## 许可证
项目采用私有许可证，详情请见[海纳科技许可协议](https://www.hina.com/licenses)。 