# 项目概览

## 项目信息
- **项目名称**: miaocha-ui
- **项目类型**: React + TypeScript 前端应用
- **主要功能**: 日志分析和查询系统
- **技术栈**: React 19, TypeScript, Vite, Ant Design, Redux Toolkit

## 核心功能模块
1. **日志查询与分析** (`src/pages/Home/`)
   - 实时日志搜索和过滤
   - 时间范围选择器
   - 日志数据可视化图表
   - 虚拟化表格展示

2. **SQL 编辑器** (`src/pages/SQLEditor/`)
   - Monaco Editor 集成
   - SQL 语法高亮和自动补全
   - 查询历史管理
   - 结果可视化

3. **系统管理** (`src/pages/system/`)
   - 数据源管理
   - Logstash 管理
   - 机器管理
   - 模块管理
   - 用户管理

4. **AI 助手** (`src/components/AIAssistant/`)
   - 智能日志分析
   - 自然语言查询
   - SSE 实时通信

## 架构特点
- 模块化组件设计
- 自定义 Hooks 封装业务逻辑
- Redux Toolkit 状态管理
- React Query 数据获取
- 虚拟化技术优化性能
- 响应式设计支持

## 开发规范
- 使用 TypeScript 严格类型检查
- ESLint + Prettier 代码格式化
- Husky + lint-staged 提交前检查
- 组件和工具函数分离
- 统一的错误处理和加载状态管理