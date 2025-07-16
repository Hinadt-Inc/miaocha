# handleInsertTable 方法修改验证

## 修改内容

### 问题描述
用户反馈：只有展开一次表名，获取了表字段，才可以触发 handleInsertTable 方法，希望随时都可以插入表名。

### 修改点

1. **useOptimizedSQLEditorActions.ts** - 修改 handleInsertTable 方法签名
   - 添加可选的 `_columns` 参数，保持向后兼容
   - 支持随时插入表名，无需预先加载列信息
   - 智能根据上下文决定插入内容（表名 vs SELECT * FROM 语句）

2. **VirtualizedSchemaTree.tsx** - 修改 handleInsertTableClick 方法
   - 移除对表列信息的强制依赖
   - 即使没有列信息也能插入表名
   - 安全处理 undefined 的列数据

### 代码逻辑

#### 修改前
```typescript
// 只有在找到表的列信息时才调用
if (table) {
  handleInsertTable(tableName, table.columns);
}
```

#### 修改后
```typescript
// 无论是否有列信息都能调用
if (databaseSchema && 'tables' in databaseSchema) {
  const table = databaseSchema.tables.find((t) => t.tableName === tableName);
  handleInsertTable(tableName, table?.columns);
} else {
  handleInsertTable(tableName, undefined);
}
```

### 智能插入逻辑

1. **在 FROM 子句中**: 只插入表名
2. **在其他位置**: 插入 `SELECT * FROM tableName`

这样用户可以：
- 直接点击表名进行插入，无需等待列加载
- 根据光标位置自动选择合适的插入内容
- 保持与原有功能的完全兼容性

### 向后兼容性

✅ 保持原有方法签名兼容  
✅ 不影响已有的展开表功能  
✅ 不改变现有的插入行为  
✅ 支持新的快速插入需求  

## 测试验证

用户现在可以：
1. ✅ 直接点击表名插入，无需展开
2. ✅ 展开表后点击仍然正常工作
3. ✅ 根据光标位置智能选择插入内容
4. ✅ 在任何时候都能触发 handleInsertTable
