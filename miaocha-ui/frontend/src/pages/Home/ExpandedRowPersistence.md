# 表格展开行状态保持功能说明

## 问题描述

用户反馈：在右边表格展开一行，然后添加一个字段，请求完成后，所有行的详情都收起来了。

## 问题根因

1. **_key 生成机制**：每次数据请求时，记录的 `_key` 都会重新生成（使用 `Date.now() + index`）
2. **展开状态依赖 _key**：Antd Table 的 `expandedRowKeys` 依赖于记录的 `_key`
3. **数据更新导致状态丢失**：添加字段时会触发新的数据请求，`_key` 变化导致展开状态丢失

## 解决方案

### 核心思路
通过**内容匹配**的方式，在数据更新时恢复展开状态：

1. **记录展开行内容**：使用 `expandedRecordsRef` 记录每个展开行的完整数据
2. **生成稳定标识**：基于记录的关键字段（时间、主机、来源等）生成内容hash
3. **内容匹配恢复**：数据更新时，通过内容hash匹配找到对应记录，使用新的 `_key`
4. **智能清空策略**：只在重要搜索条件变化时清空展开状态，字段变化时保持状态

### 技术实现

#### 1. 状态管理
```typescript
const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);
const expandedRecordsRef = useRef<Map<React.Key, any>>(new Map());
```

#### 2. 记录展开行内容
```typescript
onExpand: (expanded, record) => {
  const key = record._key;
  if (expanded) {
    setExpandedRowKeys(prev => [...prev, key]);
    expandedRecordsRef.current.set(key, record); // 记录完整数据
  } else {
    setExpandedRowKeys(prev => prev.filter(k => k !== key));
    expandedRecordsRef.current.delete(key); // 清理引用
  }
}
```

#### 3. 内容匹配算法
```typescript
const generateRecordHash = (record: any) => {
  const timeField = moduleQueryConfig?.timeField || 'log_time';
  const identifyingFields = [timeField, 'host', 'source'];
  return identifyingFields
    .filter(field => record[field] !== undefined && record[field] !== null)
    .map(field => `${field}:${String(record[field])}`)
    .join('|');
};
```

#### 4. 状态恢复逻辑
```typescript
useEffect(() => {
  if (expandedRowKeys.length > 0 && data && data.length > 0) {
    // 为新数据生成hash映射
    const dataHashToKey = new Map<string, React.Key>();
    data.forEach(record => {
      const hash = generateRecordHash(record);
      dataHashToKey.set(hash, record._key);
    });

    // 匹配展开的记录
    const newExpandedKeys: React.Key[] = [];
    expandedRowKeys.forEach(oldKey => {
      const expandedRecord = expandedRecordsRef.current.get(oldKey);
      if (expandedRecord) {
        const recordHash = generateRecordHash(expandedRecord);
        const newKey = dataHashToKey.get(recordHash);
        if (newKey) {
          newExpandedKeys.push(newKey);
          // 更新引用
          expandedRecordsRef.current.set(newKey, data.find(item => item._key === newKey));
          expandedRecordsRef.current.delete(oldKey);
        }
      }
    });

    setExpandedRowKeys(newExpandedKeys);
  }
}, [data, expandedRowKeys, moduleQueryConfig]);
```

#### 5. 智能清空策略
```typescript
useEffect(() => {
  const prev = prevSearchParamsRef.current;
  const current = searchParams;
  
  // 只在重要搜索条件变化时清空展开状态
  const importantParamsChanged = 
    prev.startTime !== current.startTime ||
    prev.endTime !== current.endTime ||
    prev.module !== current.module ||
    prev.datasourceId !== current.datasourceId ||
    JSON.stringify(prev.whereSqls) !== JSON.stringify(current.whereSqls) ||
    JSON.stringify(prev.keywords) !== JSON.stringify(current.keywords) ||
    prev.timeRange !== current.timeRange;
  
  if (importantParamsChanged) {
    setExpandedRowKeys([]);
    expandedRecordsRef.current.clear();
  }
  
  prevSearchParamsRef.current = current;
}, [searchParams]);
```

## 功能特点

### ✅ 保持场景
- 添加/删除字段
- 列排序
- 分页加载更多
- 列宽调整

### ❌ 清空场景
- 更改时间范围
- 切换模块
- 修改搜索关键词
- 修改过滤条件

## 用户体验提升

1. **无感知状态保持**：用户在添加字段后，之前展开的行会自动保持展开状态
2. **智能状态管理**：只在必要时清空展开状态，避免不必要的用户操作丢失
3. **性能优化**：使用内容hash和Map结构，确保匹配算法高效执行
4. **稳定性保证**：即使在复杂的数据更新场景下，也能准确匹配和恢复状态

## 测试验证

### 场景一：添加字段保持展开状态
1. 展开某一行查看详情
2. 在侧边栏添加一个新字段
3. ✅ 验证：该行保持展开状态

### 场景二：删除字段保持展开状态
1. 展开某一行查看详情  
2. 删除一个已有字段
3. ✅ 验证：该行保持展开状态

### 场景三：时间范围变化清空状态
1. 展开某一行查看详情
2. 修改查询时间范围
3. ✅ 验证：展开状态被清空

这个解决方案能够有效解决用户反馈的问题，在保持良好用户体验的同时，确保在合适的场景下清空展开状态。
