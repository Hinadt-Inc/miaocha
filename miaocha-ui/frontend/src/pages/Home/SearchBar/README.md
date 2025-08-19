# SearchBar 模块化组件

## 概述

SearchBar 组件已经重构为模块化结构，提供更好的代码组织和可维护性。

## 文件结构

```
SearchBar/
├── index.tsx                 # 主组件文件
├── types.ts                  # 类型定义
├── constants.ts              # 常量配置
├── SearchBar.ts              # 模块导出文件
├── components/               # 子组件目录
│   ├── index.ts              # 组件导出文件
│   ├── FilterTags.tsx        # 过滤标签组件
│   ├── StatisticsInfo.tsx    # 统计信息组件
│   ├── KeywordInput.tsx      # 关键词输入组件
│   ├── SqlInput.tsx          # SQL输入组件
│   ├── TimePickerWrapper.tsx # 时间选择器包装组件
│   └── TimeGroupSelector.tsx # 时间分组选择器组件
├── hooks/                    # 自定义钩子目录
│   ├── index.ts              # 钩子导出文件
│   ├── useSearchInput.ts     # 搜索输入钩子
│   ├── useTimeState.ts       # 时间状态钩子
│   └── useSearchActions.ts   # 搜索操作钩子
└── styles/                   # 样式文件目录
    ├── index.ts              # 样式导出文件
    └── SearchBar.module.less # 组件样式文件
```

## 组件分解

### 主要组件
- **SearchBar/index.tsx**: 主要的SearchBar组件，整合所有子组件和钩子

### 子组件
- **FilterTags**: 显示搜索条件标签（关键词、SQL、时间）
- **StatisticsInfo**: 显示搜索结果统计信息
- **KeywordInput**: 关键词搜索输入框
- **SqlInput**: SQL查询输入框，支持字段自动补全
- **TimePickerWrapper**: 时间选择器包装组件
- **TimeGroupSelector**: 时间分组选择器

### 自定义钩子
- **useSearchInput**: 管理搜索输入状态
- **useTimeState**: 管理时间相关状态
- **useSearchActions**: 管理搜索操作逻辑

## 使用方式

```tsx
import SearchBar from './SearchBar';
// 或者
import SearchBar from './SearchBar/index';

// 使用组件
<SearchBar
  searchParams={searchParams}
  totalCount={totalCount}
  onSearch={handleSearch}
  // ... 其他props
/>
```

## 向后兼容性

原有的 `SearchBar.tsx` 文件已更新为导出新的模块化组件，确保现有代码无需修改即可继续使用。

## 优势

1. **模块化**: 组件拆分为更小的、可复用的子组件
2. **关注点分离**: 业务逻辑通过自定义钩子进行分离
3. **可维护性**: 代码结构更清晰，便于维护和扩展
4. **可测试性**: 每个子组件和钩子都可以独立测试
5. **复用性**: 子组件可以在其他地方复用

## 开发指南

- 添加新功能时，考虑是否应该创建新的子组件或钩子
- 保持组件的单一职责原则
- 使用TypeScript类型确保类型安全
- 在constants.ts中定义组件常量，避免硬编码
