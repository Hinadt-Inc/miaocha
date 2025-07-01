# SQL编辑器高度自动调整功能实现总结

## 功能概述

现在的SQL编辑器能够根据Splitter面板的尺寸变化自动调整编辑器高度，实现以下功能：

1. **自动高度调整**：当用户拖拽Splitter调整面板大小时，编辑器高度会自动跟随变化
2. **响应式设计**：窗口尺寸变化时，编辑器高度也会自动适应
3. **合理的边界限制**：设置了最小高度(200px)和最大高度(600px)的限制
4. **保留空间计算**：自动减去Card头部、工具栏等UI元素的高度

## 核心实现

### 1. useSplitterSize Hook (`/src/pages/SQLEditor/hooks/useSplitterSize.ts`)

这是核心的自定义Hook，主要功能：

```typescript
export const useSplitterSize = (options: UseSplitterSizeOptions = {}) => {
  // 使用ResizeObserver监听容器尺寸变化
  // 自动计算编辑器合适的高度
  // 返回containerRef, editorHeight, updateHeight
}
```

**关键特性：**
- 使用`ResizeObserver` API监听容器尺寸变化
- 自动计算可用高度（容器高度 - 保留高度）
- 提供5px的阈值来避免微小变化导致的频繁更新
- 支持手动触发高度重新计算

### 2. 集成到状态管理 (`useSQLEditorState.ts`)

```typescript
// 使用Splitter尺寸监听Hook来自动调整编辑器高度
const { containerRef: queryPanelRef, editorHeight, updateHeight } = useSplitterSize({
  reservedHeight: 120, // Card头部 + 工具栏 + 边距
  minHeight: 200,
  maxHeight: 600,
  initialHeight: 300
});
```

### 3. 主页面集成 (`SQLEditorPage.tsx`)

```tsx
<Splitter.Panel 
  defaultSize="35%" 
  min={200} 
  max="60%" 
  className="query-panel-container"
>
  <div ref={queryPanelRef} className="query-panel-wrapper">
    <SQLQueryPanel
      height={editorHeight}
      // ... 其他props
    />
  </div>
</Splitter.Panel>
```

## 工作流程

1. **初始化**：
   - 组件挂载时，`useSplitterSize` Hook创建ResizeObserver
   - 绑定到查询面板的容器元素
   - 设置初始高度为300px

2. **尺寸监听**：
   - 当用户拖拽Splitter改变面板大小时
   - ResizeObserver检测到容器尺寸变化
   - 自动计算新的编辑器高度

3. **高度计算**：
   ```typescript
   const availableHeight = containerHeight - reservedHeight;
   const newHeight = Math.max(minHeight, Math.min(maxHeight, availableHeight));
   ```

4. **更新应用**：
   - 新的高度通过state传递给QueryEditor组件
   - Monaco编辑器自动调整布局

## 配置参数

| 参数 | 默认值 | 说明 |
|------|-------|------|
| `reservedHeight` | 120px | 预留高度（Card头部、工具栏等） |
| `minHeight` | 200px | 编辑器最小高度 |
| `maxHeight` | 600px | 编辑器最大高度 |
| `initialHeight` | 300px | 初始高度 |

## 样式优化

添加了相应的CSS样式确保容器正确占用空间：

```less
.query-panel-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}
```

## 使用效果

- ✅ **响应式**：拖拽Splitter时编辑器高度实时调整
- ✅ **平滑**：高度变化平滑，无闪烁
- ✅ **智能**：自动避免过小或过大的高度
- ✅ **兼容**：与现有的Monaco编辑器功能完全兼容

## 测试方法

1. 启动开发服务器：`npm run dev`
2. 访问SQL编辑器页面
3. 拖拽垂直Splitter调整查询面板和结果面板的比例
4. 观察编辑器高度是否跟随面板尺寸自动调整
5. 调整浏览器窗口大小，检查响应式效果

现在编辑器的高度会智能地跟随Splitter的变化，为用户提供更好的编辑体验！
