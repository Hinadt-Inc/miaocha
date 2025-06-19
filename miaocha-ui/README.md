# 秒查前端 (MiaoQue Frontend)

⚡ **秒查（MiaoQue）企业级日志管理平台前端**，基于 React 18 + TypeScript + Vite 构建的现代化 Web 应用。

<div align="center">

[![React](https://img.shields.io/badge/React-18+-61DAFB?logo=react)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.7+-3178C6?logo=typescript)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-6.2+-646CFF?logo=vite)](https://vitejs.dev/)
[![Ant Design](https://img.shields.io/badge/Ant%20Design-5.25+-1677FF?logo=antdesign)](https://ant.design/)
[![Node.js](https://img.shields.io/badge/Node.js-22.11+-339933?logo=node.js)](https://nodejs.org/)

</div>

---

## 🚀 项目概述

miaocha-ui 是秒查日志管理系统的前端模块，提供了完整的企业级日志查询、分析、管理界面。采用现代化的前端技术栈，支持高性能的数据可视化和实时交互。

### ✨ 核心特性

- 🎯 **高性能查询界面** - 支持复杂条件的日志搜索和实时分析
- 📊 **数据可视化** - 基于 ECharts 的丰富图表和统计展示
- 🛠️ **SQL 编辑器** - 集成 Monaco Editor 的专业 SQL 开发环境
- 🔐 **权限管理** - 基于角色的细粒度权限控制系统
- ⚙️ **系统管理** - Logstash 进程管理、数据源配置、用户管理等
- 📱 **响应式设计** - 支持多设备和不同屏幕尺寸
- 🌗 **主题切换** - 支持明暗主题切换
- 🚀 **性能优化** - 代码分割、懒加载、虚拟列表等优化策略

---

## 🛠️ 技术栈

### 核心框架

- **React 18** - 现代化的用户界面库，支持并发特性
- **TypeScript 5.7** - 类型安全的 JavaScript 超集
- **Vite 6.2** - 极速的前端构建工具

### UI 组件库

- **Ant Design 5.25** - 企业级 UI 设计语言和组件库
- **@ant-design/pro-components** - 中后台解决方案组件
- **@ant-design/icons** - 丰富的图标资源

### 状态管理

- **Redux Toolkit** - 现代化的 Redux 状态管理
- **React Query** - 强大的数据获取和缓存库

### 开发工具

- **Monaco Editor** - VS Code 同款的代码编辑器
- **ECharts** - 强大的数据可视化库
- **ESLint + Prettier** - 代码质量和格式化工具
- **Husky + lint-staged** - Git 钩子和代码检查

### 构建优化

- **代码分割** - 自动按路由和功能模块分割
- **Tree Shaking** - 自动移除未使用的代码
- **资源压缩** - 自动压缩 JS、CSS、图片等资源

---

## 📁 项目结构

```
miaocha-ui/
├── frontend/                   # 前端源码目录
│   ├── public/                 # 静态资源
│   │   ├── logo.png           # 应用图标
│   │   └── monaco-editor/     # Monaco Editor 资源
│   ├── src/
│   │   ├── api/               # API 接口层
│   │   │   ├── auth.ts        # 认证相关
│   │   │   ├── datasource.ts  # 数据源管理
│   │   │   ├── logstash.ts    # Logstash 管理
│   │   │   ├── logs.ts        # 日志查询
│   │   │   ├── sql.ts         # SQL 执行
│   │   │   └── user.ts        # 用户管理
│   │   ├── components/        # 公共组件
│   │   │   ├── AuthCheck/     # 权限检查组件
│   │   │   ├── Loading/       # 加载组件
│   │   │   ├── Profile/       # 用户档案组件
│   │   │   └── common/        # 通用组件
│   │   ├── hooks/             # 自定义 Hooks
│   │   │   ├── useAppHooks.ts # 应用级 Hooks
│   │   │   ├── useFilters.ts  # 过滤器 Hooks
│   │   │   └── useLoading.ts  # 加载状态 Hooks
│   │   ├── layouts/           # 布局组件
│   │   │   └── MainLayout/    # 主布局
│   │   ├── pages/             # 页面组件
│   │   │   ├── Home/          # 首页 - 日志查询分析
│   │   │   ├── SQLEditor/     # SQL 编辑器
│   │   │   ├── Login/         # 登录页
│   │   │   └── system/        # 系统管理页面
│   │   │       ├── DataSourceManagementPage.tsx
│   │   │       ├── LogstashManagementPage.tsx
│   │   │       ├── UserManagementPage.tsx
│   │   │       └── ...
│   │   ├── routes/            # 路由配置
│   │   ├── store/             # Redux 状态管理
│   │   ├── types/             # TypeScript 类型定义
│   │   ├── utils/             # 工具函数
│   │   └── styles/            # 全局样式
│   ├── package.json           # 依赖配置
│   ├── vite.config.ts         # Vite 配置
│   ├── tsconfig.json          # TypeScript 配置
│   └── eslint.config.js       # ESLint 配置
└── pom.xml                    # Maven 配置
```

---

## 🌟 功能模块

### 🔍 数据发现 (首页)

- **高性能日志搜索** - 支持关键词、正则表达式、复杂条件查询
- **实时数据分析** - 时间分布图表、字段统计分析
- **字段探索** - 动态字段发现和值分布分析
- **查询优化** - 智能查询建议和性能提示

### 💻 SQL 编辑器

- **Monaco Editor 集成** - VS Code 级别的编辑体验
- **SQL 智能提示** - 语法高亮、自动补全、错误检查
- **查询执行引擎** - 支持多数据源查询和结果缓存
- **历史记录** - 查询历史管理和快速复用
- **结果导出** - 支持 CSV、Excel 等格式导出

### ⚙️ 系统管理

- **用户管理** - 用户创建、编辑、权限分配
- **数据源管理** - 多种数据源连接配置和测试
- **Logstash 管理** - 进程生命周期管理和监控
- **服务器管理** - 集群节点管理和状态监控
- **模块管理** - 功能模块配置和权限控制

---

## 🚀 快速开始

### 环境要求

- **Node.js** >= 22.11.0
- **Yarn** >= 1.22.22 (推荐) 或 npm >= 8.0.0
- **Java** >= 17 (用于 Maven 构建)

### 本地开发

```bash
# 进入前端目录
cd miaocha-ui/frontend

# 安装依赖
yarn install

# 启动开发服务器
yarn dev

# 代码格式化
yarn format

# 代码检查
yarn lint
```

访问 http://localhost:5173 查看应用。

### 生产构建

```bash
# 构建前端资源
yarn build

# 或使用 Maven 构建整个模块
cd miaocha-ui
mvn clean package
```

---

## 🔧 配置说明

### 代理配置

开发环境下，API 请求会自动代理到后端服务器。修改 `vite.config.ts` 中的 proxy 配置：

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://your-backend-server:port',
      changeOrigin: true,
    },
  },
}
```

### 构建配置

- **输出目录**: `frontend/dist/`
- **静态资源目录**: `frontend/dist/assets/`
- **代码分割策略**: 按功能模块和第三方库分割
- **资源优化**: 自动压缩、Tree Shaking、资源内联

### Maven 集成

前端构建完全集成到 Maven 生命周期中：

- **自动安装** Node.js 和 Yarn
- **依赖安装** 和缓存管理
- **生产构建** 和资源打包
- **JAR 集成** 静态资源打包到 `META-INF/resources/`

---

## 📊 性能优化

### 构建优化

- ⚡ **代码分割** - 路由级别和组件级别的懒加载
- 🗜️ **资源压缩** - Gzip 压缩和资源内联优化
- 🌳 **Tree Shaking** - 自动移除未使用的代码
- 📦 **Chunk 优化** - 智能的代码块分割策略

### 运行时优化

- 🔄 **虚拟列表** - 大数据量列表性能优化
- 💾 **查询缓存** - React Query 实现的智能缓存
- 🎯 **懒加载** - 路由和组件的按需加载
- 📱 **响应式优化** - 移动端适配和性能优化

---

## 🔐 权限控制

前端实现了基于角色的权限控制系统：

- **路由权限** - 根据用户角色动态显示菜单和页面
- **组件权限** - 组件级别的权限控制
- **API 权限** - 请求拦截和权限验证
- **数据权限** - 基于用户权限的数据过滤

### 支持的角色

- `USER` - 普通用户，可查询日志和使用 SQL 编辑器
- `ADMIN` - 管理员，可管理用户、数据源等
- `SUPER_ADMIN` - 超级管理员，拥有所有权限

---

## 🐛 故障排除

### 常见问题

1. **启动失败**

   ```bash
   # 清理依赖和缓存
   yarn clean
   yarn install
   ```
2. **API 请求 404**
   - 检查后端服务是否正常运行
   - 确认 `vite.config.ts` 中的代理配置
   - 验证 API 接口路径是否正确
3. **构建失败**

   ```bash
   # 检查 TypeScript 类型错误
   yarn build

   # 跳过类型检查构建
   yarn build:no-tsc
   ```
4. **Monaco Editor 加载问题**
   - 确保 `public/monaco-editor/` 目录完整
   - 检查 Worker 文件的路径配置

---

## 🤝 开发规范

### 代码风格

- 使用 **TypeScript** 进行类型安全开发
- 遵循 **ESLint** 和 **Prettier** 规范
- 组件采用 **函数式组件** + **Hooks** 模式
- 使用 **CSS Modules** 或 **Less** 进行样式管理

### 提交规范

- 提交前自动执行代码检查和格式化
- 遵循 Conventional Commits 规范
- 必须通过所有 ESLint 和 TypeScript 检查

### 组件开发

- 组件命名采用 **PascalCase**
- 自定义 Hooks 以 `use` 开头
- 导出的接口和类型以 `I` 开头
- 保持组件的单一职责原则

---

## 📚 相关文档

- [秒查系统架构文档](../README.md)
- [后端 API 文档](../miaocha-server/README.md)
- [部署指南](../docs/DEPLOYMENT.md)
- [开发规范](../docs/CODE_STYLE.md)

---

## 📞 技术支持

如有问题或建议，请通过以下方式联系：

- 📧 **技术支持**: tech-support@miaocha.com
- 🐛 **Bug 报告**: 在项目 Issues 中提交
- 💡 **功能建议**: 在项目 Discussions 中讨论

---

<div align="center">

**⚡ 秒查前端 - 让日志查询更高效，让数据分析更简单**

Made with ❤️ by MiaoQue Team

</div>

