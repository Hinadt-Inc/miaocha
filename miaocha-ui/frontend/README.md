# 日志管理系统前端

基于React + TypeScript + Vite构建的现代化日志管理平台前端。

![项目截图](public/logo.png)

## 功能特性

- 日志查询与展示
- SQL编辑器(支持语法高亮、自动补全)
- 数据源管理
- 用户权限管理
- 日志分析可视化
- 响应式设计

## 技术栈

- React 18
- TypeScript
- Vite
- Monaco Editor
- Redux Toolkit
- Less
- Ant Design
- Vitest

## 快速开始

1. 安装依赖:

```bash
npm install
```

2. 开发模式:

```bash
npm run dev
```

3. 构建生产版本:

```bash
npm run build
```

## 项目结构

```
log-manage-web/
├── public/            # 静态资源
├── src/               # 源代码
│   ├── api/           # API接口
│   ├── assets/        # 静态资源
│   ├── components/    # 公共组件
│   ├── hooks/         # 自定义Hook
│   ├── layouts/       # 布局组件
│   ├── pages/         # 页面组件
│   ├── providers/     # Context提供者
│   ├── routes/        # 路由配置
│   ├── store/         # Redux状态管理
│   ├── styles/        # 全局样式
│   ├── types/         # 类型定义
│   └── utils/         # 工具函数
├── .eslintrc.js       # ESLint配置
├── vite.config.ts     # Vite配置
└── tsconfig.json      # TypeScript配置
```

## 开发指南

1. 确保Node.js版本 >= 16
2. 安装依赖:

```bash
npm install
```

3. 启动开发服务器:

```bash
npm run dev
```

4. 运行测试:

```bash
npm test
```

## 代码质量

项目配置了ESLint和Prettier确保代码风格一致:

```bash
npm run lint   # 检查代码风格
npm run format # 格式化代码
```

## 贡献

欢迎提交Pull Request。请确保:
1. 代码通过所有测试
2. 遵循现有代码风格
3. 更新相关文档

## 许可证

MIT
