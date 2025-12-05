# 服务器管理模块

## 概述

服务器管理模块用于管理系统中的服务器信息，包括服务器的增删改查、连接测试等功能。

## 目录结构

```
MachineManagement/
├── MachineManagementPage.tsx    # 主页面组件
├── MachineManagement.module.less # 样式文件
├── index.ts                      # 模块入口
├── components/                   # 组件目录
│   ├── MachinePageHeader.tsx    # 页面头部组件
│   ├── MachineFormModal.tsx     # 机器表单模态框
│   └── index.ts                 # 组件入口
└── hooks/                       # Hook目录
    ├── useMachineData.ts        # 数据管理Hook
    ├── useMachineActions.ts     # 操作逻辑Hook
    ├── useTableConfig.tsx       # 表格配置Hook
    └── index.ts                 # Hook入口
```

## 组件说明

### MachineManagementPage
主页面组件，组合各个子组件和Hook，负责整体布局和数据流转。

### MachinePageHeader
页面头部组件，包含面包屑导航和操作按钮。

### MachineFormModal
通用的机器表单模态框，可用于新增和编辑机器信息。

## Hook说明

### useMachineData
负责机器数据的获取和管理：
- 机器列表数据状态
- 加载状态管理
- 数据获取和刷新功能

### useMachineActions
负责机器的各种操作逻辑：
- 新增机器
- 编辑机器
- 删除机器（使用Popconfirm确认）
- 连接测试
- 模态框状态管理

### useTableConfig
负责表格的配置：
- 表格列定义
- 分页配置
- 表格事件处理

## 使用方式

```tsx
import MachineManagementPage from '@/pages/system/MachineManagement';

// 在路由中使用
<Route path="/system/machine" component={MachineManagementPage} />
```

## 特性

- **模块化设计**: 按功能拆分为独立的组件和Hook
- **可复用性**: 组件设计支持在不同场景下复用
- **类型安全**: 完整的TypeScript类型定义
- **错误处理**: 集成全局错误处理机制
- **权限控制**: 通过withSystemAccess HOC进行权限验证

## 依赖

- React
- Ant Design
- @/api/machine
- @/types/machineTypes
- @/utils/withSystemAccess
