<div align="center">
<img src="docs/images/logo.png" alt="秒查 Logo" width="120" height="120" />

# 🔍 秒查 - 企业级日志管理平台
  
## 🚀 基于 Apache Doris 的高性能日志检索系统

[![Version](https://img.shields.io/badge/Version-2.0.0--SNAPSHOT-blue.svg)](https://github.com/Hinadt-Inc/miaocha)
[![Java Version](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

</div>

---

## 📖 关于秒查

**秒查（Miaocha）** 是一个开源的企业级日志管理平台，基于 **Spring Boot 3.x**、**React 19** 和 **Apache Doris**
，提供毫秒级日志查询、智能分析和动态扩缩容能力。支持分布式部署和强大的 Logstash 进程管理，满足企业海量日志处理需求。

### ⭐ 核心优势

- ⚡ **毫秒级查询**: 高性能日志检索引擎
- 🎯 **智能分析**: 支持复杂查询和多维度分析
- 🚀 **动态扩容**: Logstash 秒级水平扩展
- 🛡️ **企业级安全**: 细粒度权限和多租户支持
- 🔧 **零运维**: 自动化运维与故障恢复

---

## 🎯 核心功能

- **日志检索**: 支持关键词、时间范围、字段组合查询，毫秒级响应
- **模块化管理**: 业务线隔离，动态配置查询字段和时间字段
- **Logstash 管理**: 进程部署、监控、动态扩缩容
- **SQL 编辑器**: 智能提示、语法高亮、结果导出
- **系统管理**: 用户、权限、数据源、服务器管理

---

## 📸 功能展示

### 日志主界面搜索

<img src="docs/images/logsearch.png" width="600"  alt=""/>

### Logstash 进程管理

<img src="docs/images/logstashmanage.png" width="600"  alt=""/>

### SQL 编辑器查询

<img src="docs/images/sqlQuery.png" width="600"  alt=""/>

---

## 🚀 快速开始

### 本地运行

```bash
# 1. 克隆项目
git clone https://github.com/Hinadt-Inc/miaocha
cd miaocha

# 2. 配置数据库
# 编辑 miaocha-server/src/main/resources/application-dev.yml

# 3. 构建项目
mvn clean package

# 4. 启动后端
cd miaocha-server
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 5. 启动前端
cd miaocha-ui/frontend
npm install
npm run dev
```

### Docker 部署

```bash
# 构建并启动
./scripts/build-start-docker.sh

# 清理镜像
./scripts/clean-docker-images.sh
```

---

## 📝 文档资源

| 类型   | 文档名称 | 描述        | 链接                             |
|------|------|-----------|--------------------------------|
| 开发手册 | 开发指南 | 环境搭建、代码规范 | [链接](docs/developer-guide.md)  |
| 开发手册 | 部署指南 | 部署步骤、配置说明 | [链接](docs/deployment-guide.md) |
| 产品手册 | 用户指南 | 功能使用、配置说明 | [链接](docs/user-guide.md)       |
| 产品手册 | FAQ  | 常见问题解答    | [链接](docs/faq.md)              |

**注意**: 以上文档为占位符，请在 `docs/` 目录下补充实际内容。

---

## 🔔 最新更新

### 版本 2.0.0

- **模块查询配置**: 支持多种字段检索方式，灵活指定时间字段，提升查询准确性
- **性能优化**: 更快日志检索速度
- **稳定性提升**: 修复已知问题

---

## 🌟 社区

欢迎访问我们的 [GitHub 仓库](https://github.com/Hinadt-Inc/miaocha) 并给项目一个 Star ⭐ 支持！

---

<sub>🎨 Built with ❤️ | 📜 Licensed under Apache 2.0</sub>
