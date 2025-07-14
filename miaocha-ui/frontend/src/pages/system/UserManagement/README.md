# 用户管理模块

## 概述

用户管理模块采用模块化设计，将原本的单一文件拆分为多个子模块，提高代码的可维护性和可复用性。

## 目录结构

```
UserManagement/
├── components/                 # 组件目录
│   ├── UserPageHeader.tsx     # 页面头部组件（面包屑 + 搜索 + 操作按钮）
│   ├── UserFormModal.tsx      # 用户表单模态框
│   ├── PasswordModal.tsx      # 密码修改模态框
│   └── index.ts              # 组件统一导出
├── hooks/                     # 钩子函数目录
│   ├── useUserData.ts        # 用户数据管理钩子
│   ├── useTableConfig.tsx    # 表格配置钩子
│   ├── useUserActions.ts     # 用户操作钩子
│   └── index.ts              # 钩子统一导出
├── UserManagementPage.tsx     # 主页面组件
├── UserManagement.module.less # 样式文件
└── index.ts                  # 模块统一导出
```

## 模块职责

### 组件 (components/)

- **UserPageHeader**: 页面头部组件，包含面包屑导航、搜索框和操作按钮
- **UserFormModal**: 用户添加/编辑表单模态框
- **PasswordModal**: 密码修改模态框

### 钩子 (hooks/)

- **useUserData**: 管理用户数据的获取、搜索、加载状态等
- **useTableConfig**: 管理表格列配置、分页、排序、筛选等
- **useUserActions**: 管理用户相关操作（增删改、密码修改、权限分配等）

### 主页面

- **UserManagementPage**: 主页面组件，组合各个子组件和钩子

## 主要功能

1. **用户列表展示**: 支持分页、排序、筛选
2. **用户搜索**: 支持按昵称、邮箱、用户名搜索，带防抖功能
3. **用户管理**: 添加、编辑、删除用户
4. **密码管理**: 修改用户密码
5. **权限管理**: 用户模块权限分配
6. **状态管理**: 用户启用/禁用状态管理

## 重构优势

1. **模块化**: 代码按功能拆分，结构清晰
2. **可复用**: 组件和钩子可在其他页面复用
3. **可维护**: 职责单一，易于维护和测试
4. **类型安全**: TypeScript 类型定义完善
5. **性能优化**: 合理使用 React hooks，避免不必要的重渲染

## 使用方式

```tsx
import UserManagementPage from '@/pages/system/UserManagement';

// 在路由中使用
const UserManagement = lazy(() => import('@/pages/system/UserManagement'));
```

## 注意事项

1. 模块依赖全局错误处理系统 (`ErrorProvider`)
2. 需要配合 `withSystemAccess` 高阶组件使用，进行系统权限控制
3. 依赖 `ModulePermissionModal` 组件进行权限管理
