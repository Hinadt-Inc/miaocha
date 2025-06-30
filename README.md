<div align="center">
<img src="docs/images/logo.png" alt="秒查 Logo" width="120" height="120" />

# 🔍 秒查 - 企业级日志管理平台

## 🚀 基于 Doris 的高性能日志检索系统，提供毫秒级查询和智能分析能力

[![Version](https://img.shields.io/badge/Version-2.0.0--SNAPSHOT-blue.svg)](https://github.com/your-org/miaocha)
[![Java Version](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

</div>

---

## 💡 项目概述

**秒查（Miaocha）**是一个基于 **Spring Boot 3.x** 和 **React 19** 的企业级日志管理平台。系统提供完整的日志采集、存储、查询、分析功能，以及强大的 **Logstash 进程管理**能力，支持**分布式部署**和**动态扩缩容**，让您能够秒级查询海量日志数据。

### ⭐ 为什么选择秒查？

- ⚡ **毫秒级查询**: 基于 Apache Doris 的高性能日志检索引擎
- 🎯 **智能分析**: 支持复杂查询条件和多维度数据分析
- 🚀 **动态扩容**: Logstash 进程支持秒级水平扩缩容
- 🛡️ **企业级**: 完善的权限管理和多租户支持
- 🔧 **零运维**: 内置自动化运维和故障恢复机制

---

## 🎯 核心功能

### 🔍 日志检索分析
- **多维度检索**: 支持关键词、时间范围、字段条件的复杂组合查询
- **实时统计分析**: 时间分布柱状图、字段分布统计
- **高性能查询**: 毫秒级响应，支持海量数据检索
- **智能可视化**: 直观的图表展示和数据探索

### 📊 模块化日志管理
- **业务线隔离**: 按业务模块区分不同应用的日志数据
- **一站式流程**: 从建表配置到日志采集的完整管理
- **动态配置**: 支持查询配置热更新和字段映射
- **权限控制**: 模块级别的细粒度权限管理

### ⚙️ Logstash 管理功能
- **进程生命周期**: 部署、启动、停止、监控的完整管理
- **智能扩缩容**: 支持动态添加/移除服务器节点
- **状态监控**: 实时进程状态跟踪和健康检查
- **配置管理**: 统一的配置文件管理和版本控制

### 💻 SQL编辑器功能
- **Monaco编辑器**: VS Code级别的SQL编辑体验
- **智能提示**: 语法高亮、自动补全、错误检查
- **多数据源**: 支持连接不同类型的数据库
- **结果导出**: 支持CSV、Excel等格式的数据导出

### 🔐 系统管理功能
- **用户管理**: 用户创建、编辑、密码管理
- **数据源管理**: 多种数据源连接配置和测试
- **服务器管理**: SSH连接的服务器节点管理
- **权限管理**: 基于模块的细粒度权限控制

---

## 🛠️ 技术栈

<div align="center">
<table>
<tr>
<td width="50%" align="center">

### 🔧 后端技术栈

[![Java](https://img.shields.io/badge/Java-17+-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen?logo=spring)](https://spring.io/)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6+-green?logo=springsecurity)](https://spring.io/projects/spring-security)
[![MyBatis](https://img.shields.io/badge/MyBatis-3.0.4-red?logo=mybatis)](https://mybatis.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.4.0-blue?logo=mysql)](https://www.mysql.com/)
[![Apache Doris](https://img.shields.io/badge/Apache%20Doris-支持-purple)](https://doris.apache.org/)

</td>
<td width="50%" align="center">

### 🎨 前端技术栈

[![React](https://img.shields.io/badge/React-19.1.0-blue?logo=react)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.7.2-blue?logo=typescript)](https://www.typescriptlang.org/)
[![Ant Design](https://img.shields.io/badge/Ant%20Design-5.25.1-blue?logo=antdesign)](https://ant.design/)
[![Vite](https://img.shields.io/badge/Vite-6.2.0-purple?logo=vite)](https://vitejs.dev/)
[![Monaco Editor](https://img.shields.io/badge/Monaco%20Editor-0.52.2-blue)](https://microsoft.github.io/monaco-editor/)
[![ECharts](https://img.shields.io/badge/ECharts-5.6.0-red)](https://echarts.apache.org/)

</td>
</tr>
</table>
</div>

### 🏗️ 项目结构

```
miaocha/
├── miaocha-server/        # Spring Boot 后端服务
│   ├── src/main/java/com/hinadt/miaocha/
│   │   ├── endpoint/      # API接口层
│   │   ├── application/   # 应用服务层
│   │   ├── domain/        # 领域层
│   │   ├── common/        # 通用组件
│   │   └── config/        # 配置管理
│   └── src/main/resources/
├── miaocha-ui/            # React 前端界面
│   └── frontend/
│       ├── src/
│       │   ├── pages/     # 页面组件
│       │   ├── api/       # API接口
│       │   ├── components/ # 公共组件
│       │   └── utils/     # 工具函数
│       └── package.json
├── miaocha-assembly/      # 打包模块
├── docs/                  # 项目文档
└── pom.xml               # Maven 根配置
```

---

## 🚀 快速开始

### 🏃‍♂️ 本地启动

```bash
# 📥 1. 克隆项目
git clone <repository-url>
cd miaocha

# 🔧 2. 配置数据库连接  
# 编辑 miaocha-server/src/main/resources/application-dev.yml

# 🏗️ 3. 构建项目
mvn clean package

# 🚀 4. 启动后端服务
cd miaocha-server
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 🎨 5. 启动前端服务（新终端）
cd miaocha-ui/frontend
npm install
npm run dev
```

### 🐳 Docker 部署

```bash
# 🏗️ 构建并启动
./build-start-docker.sh

# 🧹 清理镜像
./clean-docker-images.sh
```

---

## 📚 API 文档

🔗 **Swagger UI文档**: `http://<serverIp>:<port>/swagger-ui/index.html#/`

### 🔌 主要API模块

- 🔍 **日志检索**: `/api/logs` - 日志查询和分析
- 💻 **SQL编辑器**: `/api/sql` - 自定义SQL查询
- 👤 **用户管理**: `/api/users` - 用户和认证
- 🗄️ **数据源管理**: `/api/datasources` - 数据源配置
- ⚙️ **Logstash管理**: `/api/logstash` - 进程管理
- 🖥️ **服务器管理**: `/api/machines` - 机器连接
- 📊 **模块管理**: `/api/modules` - 日志模块配置
- 🔐 **权限管理**: `/api/permissions` - 访问控制

---

<div align="center">

### 🌟 如果这个项目对您有帮助，请给我们一个 Star ⭐

[![GitHub stars](https://img.shields.io/github/stars/your-org/miaocha?style=social)](https://github.com/your-org/miaocha)

<sub>🎨 Built with ❤️ | 📜 Licensed under Apache 2.0</sub>

</div>

