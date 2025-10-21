# 模块管理页面 - ModuleManagement

## 概述
这是重构后的模块管理页面，采用模块化设计，将原本单一的大文件分割为多个小模块，提高了代码的可维护性和可读性。

## 文件结构

```
ModuleManagement/
├── components/                    # UI组件
│   ├── index.ts                  # 组件统一导出
│   ├── ModuleFormModal.tsx       # 模块表单模态框
│   ├── ModuleDetailModal.tsx     # 模块详情模态框
│   ├── DeleteConfirmModal.tsx    # 删除确认模态框
│   └── ModulePageHeader.tsx      # 页面头部组件
├── hooks/                        # 自定义Hooks
│   ├── index.ts                  # Hooks统一导出
│   ├── useModuleData.ts          # 数据管理Hook
│   ├── useTableConfig.tsx        # 表格配置Hook
│   └── useModuleActions.ts       # 操作管理Hook
├── types/                        # 类型定义
│   └── index.ts                  # 类型统一导出
├── utils/                        # 工具函数
│   └── index.ts                  # 数据转换工具
├── ModuleManagement.module.less  # 样式文件
├── ModuleManagementPage.tsx      # 主页面组件
└── index.ts                      # 模块统一导出
```

## 主要功能模块

### 1. 组件 (components/)

#### ModuleFormModal.tsx
- 负责模块的新增和编辑表单
- 集成数据源选择和表单验证
- 支持编辑模式的数据回填

#### ModuleDetailModal.tsx
- 显示模块的详细信息
- 包含Doris SQL的展示
- 采用描述列表格式展示

#### DeleteConfirmModal.tsx
- 模块删除确认对话框
- 支持选择是否同时删除Doris表数据
- 包含警告提示信息

#### ModulePageHeader.tsx
- 页面头部搜索和操作区域
- 包含面包屑导航
- 搜索框和操作按钮组

### 2. Hooks (hooks/)

#### useModuleData.ts
- 管理模块数据的获取和状态
- 处理搜索功能
- 统一的错误处理和消息提示

#### useTableConfig.tsx
- 表格列配置和分页设置
- 操作按钮的事件处理
- 响应式的表格配置

#### useModuleActions.ts
- 管理所有模态框的状态
- 处理模块的增删改操作
- SQL执行相关的逻辑

### 3. 类型定义 (types/)
- `Module`: 基础模块类型
- `ModuleData`: 表格展示用的数据类型
- `ModuleFormData`: 表单数据类型
- 各种模态框状态的类型定义

### 4. 工具函数 (utils/)
- `transformModuleData`: 数据转换函数
- `searchModuleData`: 搜索过滤函数

## 重构的优势

### 1. 关注点分离
- 数据逻辑与UI组件分离
- 状态管理与业务逻辑分离
- 类型定义与实现分离

### 2. 可复用性
- 组件可以在其他地方复用
- Hooks可以在类似页面中复用
- 工具函数可以独立测试和复用

### 3. 可维护性
- 每个文件职责单一，易于理解
- 修改某个功能时只需关注对应的文件
- 类型检查确保代码质量

### 4. 可测试性
- 每个Hook和工具函数都可以独立测试
- 组件的props明确，便于单元测试
- 业务逻辑从UI中抽离，便于测试

## 使用方式

原有的导入方式保持不变：
```typescript
import ModuleManagementPage from '@/pages/system/ModuleManagementPage';
```

内部实现已经完全模块化，但对外接口保持一致，确保不影响现有的路由和其他引用。

## 开发建议

1. **添加新功能**: 如需添加新的模态框或操作，建议在相应的文件夹下创建新文件
2. **修改样式**: 统一在 `ModuleManagement.module.less` 中管理样式
3. **类型扩展**: 在 `types/index.ts` 中添加新的类型定义
4. **业务逻辑**: 优先在Hooks中实现，保持组件的纯净性

这种模块化结构使得代码更加清晰，便于团队协作和后续维护。
