# Monaco Editor CDN 加载问题解析

## 问题描述

在使用 `@monaco-editor/react` 时，即使配置了本地资源路径，仍然会有四个文件从CDN加载。

## 四个CDN文件详解

当Monaco Editor初始化时，通常会加载以下四个文件：

1. **loader.js** - Monaco编辑器的AMD模块加载器
2. **editor.main.js** - 编辑器核心功能模块  
3. **language支持文件** - 特定语言（如SQL）的语法高亮和智能提示
4. **worker文件** - 处理语法分析和智能提示的Web Worker

## 为什么会走CDN？

### 1. @monaco-editor/react 默认行为

`@monaco-editor/react` 库的 `loader` 默认配置会尝试从CDN加载Monaco资源，即使设置了：

```typescript
const config = {
  paths: { vs: '/monaco-editor/min/vs' },
  useCDN: false
};
```

### 2. loader.init() 的回退机制

当本地资源加载失败时，loader会自动回退到CDN加载，这是为了保证编辑器的可用性。

### 3. 配置不完整

某些配置可能不足以完全禁用CDN加载，特别是当缺少必要的本地资源文件时。

## 解决方案

### 方案1: 修改现有初始化 (已实现)

```typescript
// src/pages/SQLEditor/utils/monacoInit.ts
const initMonacoEditor = async (): Promise<typeof monaco | undefined> => {
  // 直接使用导入的monaco实例，跳过loader
  window.monaco = monaco;
  
  // 配置本地Workers
  self.MonacoEnvironment = { getWorker };
  
  return window.monaco;
};
```

### 方案2: 完全本地化组件 (推荐)

创建了 `LocalQueryEditor` 组件：

```typescript
// src/pages/SQLEditor/components/LocalQueryEditor.tsx
// 直接使用 monaco-editor，不依赖 @monaco-editor/react 的 loader
import * as monaco from 'monaco-editor';
```

## 验证方法

1. 打开浏览器开发者工具
2. 切换到 Network 标签页
3. 刷新页面并加载SQL编辑器
4. 检查是否还有从CDN加载的Monaco相关文件

## 优势对比

| 方案 | CDN依赖 | 加载速度 | 离线可用 | 配置复杂度 |
|------|---------|----------|----------|------------|
| 原方案 | 有 | 中等 | 否 | 中等 |
| 方案1 | 无 | 快 | 是 | 低 |
| 方案2 | 无 | 最快 | 是 | 低 |

## 推荐

使用 **方案2 (LocalQueryEditor)**，因为：
- 完全本地化，无CDN依赖
- 启动速度更快
- 代码更简洁
- 更好的离线支持
