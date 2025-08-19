# VirtualTable 模块化重构

本目录包含了重构后的 VirtualTable 组件的模块化结构。

## 目录结构

```
VirtualTable/
├── index.tsx                 # 主要的 VirtualTable 组件
├── VirtualTable.module.less  # 样式文件
├── types/
│   └── index.ts             # TypeScript 类型定义
├── components/
│   ├── index.ts             # 组件导出文件
│   ├── ResizableTitle.tsx   # 可调整大小的表头组件
│   └── ColumnHeader.tsx     # 列头操作组件
├── hooks/
│   ├── index.ts             # Hooks 导出文件
│   ├── useScreenWidth.ts    # 屏幕宽度监听 Hook
│   ├── useExpandedRows.ts   # 展开行状态管理 Hook
│   └── useTableColumns.ts   # 表格列配置 Hook
└── utils/
    ├── index.ts             # 工具函数导出文件
    ├── columnUtils.ts       # 列宽计算相关工具函数
    ├── sortUtils.ts         # 排序相关工具函数
    └── dataUtils.ts         # 数据处理相关工具函数
```

## 重构内容

### 1. 类型定义 (`types/`)
- `VirtualTableProps`: 主组件的 props 类型
- `ColumnHeaderProps`: 列头组件的 props 类型
- `ResizableTitleProps`: 可调整标题组件的 props 类型
- `SortConfig`: 排序配置类型

### 2. 组件 (`components/`)
- `ResizableTitle`: 可拖拽调整列宽的表头组件
- `ColumnHeader`: 包含删除、移动操作的列头组件

### 3. 自定义 Hooks (`hooks/`)
- `useScreenWidth`: 监听屏幕宽度变化
- `useExpandedRows`: 管理表格展开行状态，支持数据更新时的状态保持
- `useTableColumns`: 管理表格列配置（已简化，主要逻辑移至主组件）

### 4. 工具函数 (`utils/`)
- `columnUtils.ts`: 
  - `getTextWidth`: 计算文本宽度
  - `getAutoColumnWidth`: 计算列的自适应宽度
- `sortUtils.ts`:
  - `isFieldSortable`: 检查字段是否可排序
  - `processSorterChange`: 处理排序器变化
- `dataUtils.ts`:
  - `extractSqlKeywords`: 从SQL语句提取关键词
  - `formatSearchKeywords`: 格式化搜索关键词
  - `generateRecordHash`: 生成记录哈希值用于状态保持

## 重构优势

1. **代码分离**: 将原来的963行代码分解为多个小模块，每个模块职责单一
2. **可维护性**: 相关功能集中在对应的模块中，便于维护和调试
3. **可复用性**: 组件和工具函数可以在其他地方复用
4. **类型安全**: 完善的 TypeScript 类型定义
5. **清晰结构**: 目录结构清晰，易于理解和扩展

## 使用方式

原有的使用方式保持不变：

```tsx
import VirtualTable from './VirtualTable';
```

内部会自动使用新的模块化结构。
