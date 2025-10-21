# Logstash管理模块

本模块负责Logstash进程的管理，包括创建、启动、停止、扩容、配置管理等功能。

## 目录结构

```
LogstashManagement/
├── index.ts                               # 模块入口
├── LogstashManagementPage.tsx             # 主页面组件
├── LogstashManagement.module.less         # 样式文件
├── components/                            # 组件目录
│   ├── index.ts                          # 组件导出
│   ├── LogstashPageHeader.tsx            # 页面头部组件
│   ├── TaskHistoryModal.tsx              # 任务历史模态框
│   ├── TaskDetailModal.tsx               # 任务详情模态框  
│   ├── LogstashDetailModal.tsx           # 进程详情模态框
│   ├── MachineTasksModal.tsx             # 机器任务模态框
│   ├── MachineTable.tsx                  # 机器列表表格
│   ├── ExpandedRowRenderer.tsx           # 表格展开行渲染器
│   ├── LogstashEditModal.tsx             # 进程编辑模态框
│   ├── LogstashMachineConfigModal.tsx    # 机器配置模态框
│   ├── LogstashMachineDetailModal.tsx    # 机器详情模态框
│   ├── LogstashScaleModal.tsx            # 扩容模态框
│   ├── LogstashLogTailModal.tsx          # 日志查看模态框
│   └── LogstashLogTailModal.module.less  # 日志模态框样式
└── hooks/                                # 自定义hooks
    ├── index.ts                          # hooks导出
    ├── useLogstashData.ts               # 数据管理hook
    ├── useTableConfig.tsx               # 表格配置hook
    ├── useLogstashActions.ts            # 主要操作hook
    └── useMachineActions.ts             # 机器操作hook
```

## 核心功能

### 1. 进程管理
- 创建Logstash进程
- 启动/停止进程
- 删除进程
- 查看进程详情
- 配置管理

### 2. 机器管理
- 启动/停止单个机器
- 重新初始化机器
- 强制停止机器
- 删除机器（缩容）
- 配置刷新
- 查看机器详情

### 3. 任务管理
- 查看任务历史
- 查看任务详情
- 查看机器任务

### 4. 扩容管理
- 添加机器
- 移除机器
- 自定义部署路径

### 5. 日志管理
- 实时日志查看
- 底部日志窗口

## 组件说明

### LogstashPageHeader
页面头部组件，包含面包屑导航和操作按钮。

### TaskHistoryModal
任务历史模态框，显示进程的所有历史任务。

### TaskDetailModal
任务详情模态框，显示单个任务的详细步骤信息。

### LogstashDetailModal
进程详情模态框，显示进程的完整配置和状态信息。

### MachineTasksModal
机器任务模态框，显示单个机器的任务详情。

### MachineTable
机器列表表格，作为主表格的展开行，显示进程下的所有机器。

## Hooks说明

### useLogstashData
负责数据获取和管理，包括：
- 进程列表数据
- 加载状态
- 数据刷新

### useTableConfig
负责表格配置，包括：
- 列配置
- 分页配置
- 表格事件处理

### useLogstashActions
负责主要操作逻辑，包括：
- 进程CRUD操作
- 模态框状态管理
- 操作响应处理

### useMachineActions
负责机器相关操作，包括：
- 机器生命周期管理
- 机器配置管理
- 机器任务查看

## 样式特性

- 响应式设计
- 统一的视觉风格
- 优化的表格展示
- 清晰的状态指示

## 注意事项

1. 所有的状态管理都通过hooks进行，保持组件的纯净性
2. 模态框状态统一管理，避免状态冲突
3. 错误处理和用户提示统一处理
4. 保持与原有功能的完全兼容
5. 维护良好的代码可读性和可维护性
