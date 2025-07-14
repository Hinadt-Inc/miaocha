# 修复SQL编辑器"Maximum update depth exceeded"错误

## 问题分析

"Maximum update depth exceeded" 错误通常是由于组件在渲染过程中不断调用setState导致无限循环引起的。经过分析，发现以下几个问题：

## 修复的问题

### 1. useDatabaseSchema.ts 中的无限循环

**问题**：
- `useEffect` 依赖了 `fetchDatabaseSchema` 函数
- `fetchDatabaseSchema` 函数又依赖了 `initialSelectedSource`
- 导致每次渲染都重新创建函数，触发无限重新渲染

**修复**：
```typescript
// 修复前
const fetchDatabaseSchema = useCallback(
  async (sourceId?: string) => { ... },
  [initialSelectedSource], // 会导致循环
);

useEffect(() => {
  // ...
}, [initialSelectedSource, fetchDatabaseSchema]); // 依赖fetchDatabaseSchema导致循环

// 修复后
const fetchDatabaseSchema = useCallback(async (sourceId?: string) => {
  // ...
}, []); // 移除依赖，避免循环更新

useEffect(() => {
  // ...
}, [initialSelectedSource]); // 只依赖initialSelectedSource
```

### 2. useSQLEditorActions.ts 中的防抖函数问题

**问题**：
- `debounce`函数在`useCallback`内部，导致每次渲染都重新创建
- 依赖数组包含可能频繁变化的值

**修复**：
```typescript
// 修复前
const executeQuery = useCallback(
  debounce(() => { ... }, 300), // 每次都创建新的debounce函数
  [/* 大量依赖 */]
);

// 修复后
const executeQueryInternal = useCallback(() => { ... }, [/* 稳定依赖 */]);

const debouncedExecuteQuery = useMemo(
  () => debounce(executeQueryInternal, 300),
  [executeQueryInternal]
);

const executeQuery = useCallback(() => {
  debouncedExecuteQuery();
}, [debouncedExecuteQuery]);
```

### 3. QueryEditor.tsx 中的内容更新循环

**问题**：
- 编辑器内容变化触发`onChange`
- `onChange`更新`sqlQuery`状态
- `sqlQuery`变化触发`useEffect`更新编辑器内容
- 形成无限循环

**修复**：
```typescript
// 添加标志位避免循环更新
const isUpdatingFromState = useRef(false);

useEffect(() => {
  if (editorRef.current && editorRef.current.getValue() !== sqlQuery && !isUpdatingFromState.current) {
    isUpdatingFromState.current = true;
    editorRef.current.setValue(sqlQuery);
    setTimeout(() => {
      isUpdatingFromState.current = false;
    }, 50);
  }
}, [sqlQuery]);

const handleContentChange = () => {
  // 如果是程序更新内容，跳过onChange回调
  if (isUpdatingFromState.current) {
    return;
  }
  // ... 处理用户输入
};
```

### 4. 移除重复的编辑器内容监听

**问题**：
- 在`useSQLEditorActions.ts`的`handleEditorDidMount`中重复监听编辑器内容变化
- 与`QueryEditor.tsx`中的监听形成冲突

**修复**：
- 移除`handleEditorDidMount`中的重复监听
- 只在`QueryEditor.tsx`组件中统一处理内容变化

## 最佳实践总结

1. **避免在useCallback依赖中包含会变化的值**
2. **使用useMemo稳定昂贵的计算结果（如debounce函数）**
3. **避免重复的事件监听器**
4. **使用引用标志位避免程序更新与用户输入的循环**
5. **合理使用防抖来减少频繁的状态更新**

## 预期效果

修复后应该能够：
- 消除"Maximum update depth exceeded"错误
- 提高编辑器性能和响应速度
- 避免不必要的重新渲染
- 保持编辑器功能的稳定性
