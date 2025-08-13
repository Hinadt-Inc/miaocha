# ExpandedRow 组件

展开行组件，用于在表格中展示详细的日志数据，支持表格和JSON两种展示方式。

## 📁 文件结构

```
ExpandedRow/
├── ExpandedRow.tsx             # 主组件文件
├── index.ts                    # 导出索引
├── index.module.less           # 样式文件
├── types.ts                    # 类型定义
├── constants.ts                # 常量定义
├── utils.ts                    # 工具函数
├── hooks.ts                    # 自定义hooks
└── README.md                   # 组件文档
```

## 🚀 使用方式

### 基本使用

```tsx
import ExpandedRow from '@/pages/Home/ExpandedRow';

const MyComponent = () => {
  const data = {
    field1: 'value1',
    field2: 'value2',
    log_time: '2024-01-01T12:00:00',
  };

  const keywords = ['keyword1', 'keyword2'];
  
  const moduleQueryConfig = {
    timeField: 'log_time',
    excludeFields: ['_key'],
  };

  return (
    <ExpandedRow
      data={data}
      keywords={keywords}
      moduleQueryConfig={moduleQueryConfig}
    />
  );
};
```

### 使用单独的hooks

```tsx
import { useTableColumns, useTableDataSource } from '@/pages/Home/ExpandedRow';

const CustomTable = ({ data, keywords, moduleQueryConfig }) => {
  const columns = useTableColumns(keywords);
  const dataSource = useTableDataSource(data, moduleQueryConfig);
  
  return (
    <Table
      columns={columns}
      dataSource={dataSource}
      pagination={false}
    />
  );
};
```

## 🔧 API 接口

### Props

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| data | `Record<string, any>` | ✅ | - | 展示的数据对象 |
| keywords | `string[]` | ✅ | - | 搜索关键词列表，用于高亮显示 |
| moduleQueryConfig | `IModuleQueryConfig` | ❌ | - | 模块查询配置 |

### IModuleQueryConfig

| 参数 | 类型 | 说明 |
|------|------|------|
| timeField | `string` | 时间字段名称 |
| excludeFields | `string[]` | 要排除的字段列表 |
| keywordFields | `any[]` | 关键词字段配置 |

## 🎯 功能特性

### 1. **双重展示模式**
- **Table模式**: 以表格形式展示字段和值
- **JSON模式**: 以JSON格式展示原始数据

### 2. **智能字段处理**
- 自动识别时间字段并格式化
- 支持排除特定字段
- 过滤内部key字段

### 3. **关键词高亮**
- 支持多关键词高亮
- 自动高亮匹配的文本内容

### 4. **模块化设计**
- 独立的hooks用于数据处理
- 可复用的工具函数
- 清晰的类型定义

## 🔨 工具函数

### formatTimeValue(value)
格式化时间值，将T替换为空格

### formatFieldValue(key, value, timeField)
根据字段类型格式化字段值

### transformDataToTableSource(data, moduleQueryConfig)
将数据对象转换为表格数据源

### filterDataByConfig(data, moduleQueryConfig)
根据配置过滤数据对象

## 🎨 样式定制

组件使用CSS Modules，可以通过修改`index.module.less`来定制样式：

```less
.expandedRow {
  // 自定义样式
  .ant-tabs-content {
    // 标签页内容样式
  }
  
  .field-title {
    // 字段标题样式
    font-weight: bold;
  }
}
```

## 📝 注意事项

1. **性能优化**: 组件使用`useMemo`进行数据缓存，避免不必要的重新计算
2. **类型安全**: 完整的TypeScript类型定义，确保类型安全
3. **可扩展性**: 模块化设计，便于功能扩展和维护
4. **兼容性**: 与现有代码保持兼容，可以无缝替换原组件
