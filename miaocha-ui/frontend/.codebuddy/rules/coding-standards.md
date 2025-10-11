# 编码规范

## TypeScript 规范
- 使用严格的 TypeScript 配置
- 所有组件和函数必须有明确的类型定义
- 优先使用 interface 而不是 type（除非需要联合类型）
- 使用泛型提高代码复用性
- 避免使用 any，使用 unknown 或具体类型

## React 组件规范
- 使用函数组件和 Hooks
- 组件名使用 PascalCase
- Props 接口以 Props 结尾（如 ButtonProps）
- 使用 React.memo 优化性能关键组件
- 自定义 Hooks 以 use 开头

## 文件组织规范
- 每个功能模块独立目录
- 组件文件使用 PascalCase.tsx
- 工具函数文件使用 camelCase.ts
- 类型定义文件命名为 types.ts 或 index.ts
- 常量文件命名为 constants.ts

## 样式规范
- 使用 Ant Design 组件库
- 自定义样式使用 CSS-in-JS (antd-style)
- 响应式设计使用 Ant Design 的栅格系统
- 颜色和间距使用设计系统 token

## 状态管理规范
- 全局状态使用 Redux Toolkit
- 组件内状态使用 useState
- 服务端状态使用 React Query
- 复杂逻辑使用 useReducer

## API 调用规范
- 使用 axios 进行 HTTP 请求
- API 函数统一放在 src/api/ 目录
- 使用 React Query 管理异步状态
- 统一的错误处理和加载状态

## 性能优化规范
- 使用 React.memo 避免不必要的重渲染
- 使用 useMemo 和 useCallback 优化计算和函数
- 大列表使用虚拟化技术
- 图片和资源懒加载
- 代码分割和动态导入