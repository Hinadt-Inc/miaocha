# 数据源管理模块

## 概述

数据源管理模块采用模块化设计，将原本的单一文件拆分为多个子模块，提高代码的可维护性和可复用性。

## 目录结构

```
DataSourceManagement/
├── components/                          # 组件目录
│   ├── DataSourcePageHeader.tsx        # 页面头部组件（面包屑 + 搜索 + 操作按钮）
│   ├── DataSourceFormModal.tsx         # 数据源表单模态框
│   └── index.ts                        # 组件统一导出
├── hooks/                              # 钩子函数目录
│   ├── useDataSourceData.ts           # 数据源数据管理钩子
│   ├── useTableConfig.tsx             # 表格配置钩子
│   ├── useDataSourceActions.ts        # 数据源操作钩子
│   └── index.ts                       # 钩子统一导出
├── DataSourceManagementPage.tsx       # 主页面组件
├── DataSourceManagement.module.less   # 样式文件
├── README.md                          # 模块说明文档
└── index.ts                           # 模块统一导出
```

## 模块职责

### 组件 (components/)

- **DataSourcePageHeader**: 页面头部组件，包含面包屑导航、搜索框和操作按钮
- **DataSourceFormModal**: 数据源添加/编辑表单模态框，支持连接测试

### 钩子 (hooks/)

- **useDataSourceData**: 管理数据源数据的获取、搜索、分页、加载状态等
- **useTableConfig**: 管理表格列配置、操作按钮等
- **useDataSourceActions**: 管理数据源相关操作（增删改、连接测试等）

### 主页面

- **DataSourceManagementPage**: 主页面组件，组合各个子组件和钩子

## 主要功能

1. **数据源列表展示**: 支持分页、排序、筛选
2. **数据源搜索**: 支持按名称、JDBC地址、描述等关键词搜索，实时反馈
3. **数据源管理**: 添加、编辑、删除数据源
4. **连接测试**: 测试数据库连接配置是否正确
5. **状态管理**: 数据源的创建时间、更新时间、创建人等信息管理

## 重构优势

1. **模块化**: 代码按功能拆分，结构清晰
2. **可复用**: 组件和钩子可在其他页面复用
3. **可维护**: 职责单一，易于维护和测试
4. **类型安全**: TypeScript 类型定义完善
5. **性能优化**: 合理使用 React hooks，避免不必要的重渲染

## 使用方式

```tsx
import DataSourceManagementPage from '@/pages/system/DataSourceManagement';

// 在路由中使用
const DataSourceManagement = lazy(() => import('@/pages/system/DataSourceManagement'));
```

## 注意事项

1. 模块依赖全局错误处理系统 (`ErrorProvider`)
2. 需要配合 `withSystemAccess` 高阶组件使用，进行系统权限控制
3. 依赖 ProTable 组件提供表格功能
4. 支持 Doris 数据库类型，可扩展支持其他数据库类型
