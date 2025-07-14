# 统一错误处理系统

本项目集成了一套完整的错误处理和提示系统，提供了统一的错误展示和处理机制。

## 功能特性

- 🔧 **自动错误捕获**: 自动捕获 API 请求错误、未处理的 Promise 错误、全局 JS 错误等
- 🎨 **多种展示方式**: 支持 message、notification、静默处理等多种错误展示方式
- 📝 **错误分类**: 按网络、认证、业务、验证、权限、系统等类型分类处理
- 🎯 **智能映射**: 根据错误码自动映射到合适的错误类型和提示信息
- 🔄 **错误边界**: React ErrorBoundary 保护应用不会因为组件错误而崩溃
- 📊 **错误日志**: 自动记录错误日志，便于调试和监控
- ⚙️ **高度可配置**: 支持自定义错误处理逻辑和展示方式

## 使用方式

### 1. 基础用法 - 在组件中使用

```tsx
import { useErrorContext } from '../providers/ErrorProvider';

function MyComponent() {
  const { handleError, showSuccess, showWarning } = useErrorContext();

  const handleAsyncOperation = async () => {
    try {
      // 你的异步操作
      const result = await someApiCall();
      showSuccess('操作成功！');
    } catch (error) {
      // 错误会被自动处理并显示合适的提示
      handleError(error);
    }
  };

  return (
    <button onClick={handleAsyncOperation}>
      执行操作
    </button>
  );
}
```

### 2. 自定义错误处理

```tsx
import { useErrorContext } from '../providers/ErrorProvider';
import { ErrorType } from '../hooks/useErrorHandler';

function MyComponent() {
  const { handleError } = useErrorContext();

  const handleCustomError = () => {
    // 自定义错误配置
    handleError('这是一个自定义错误信息', {
      type: ErrorType.BUSINESS,
      showType: 'notification',
      duration: 5,
      action: () => {
        console.log('执行自定义操作');
      }
    });
  };

  return <button onClick={handleCustomError}>触发自定义错误</button>;
}
```

### 3. API 请求错误自动处理

所有通过 `request.ts` 发送的 API 请求错误都会被自动捕获和处理：

```tsx
import { get, post } from '../api/request';

async function fetchData() {
  try {
    // 如果请求失败，错误会被自动处理
    const data = await get('/api/users');
    return data;
  } catch (error) {
    // 错误已经被全局错误处理器处理了
    // 这里可以做一些特殊的错误处理逻辑
    console.log('请求失败，但用户已经看到错误提示了');
    throw error;
  }
}
```

## 错误类型和默认处理

### 错误类型枚举

```typescript
enum ErrorType {
  NETWORK = 'NETWORK',      // 网络错误
  AUTH = 'AUTH',            // 认证错误
  BUSINESS = 'BUSINESS',    // 业务错误
  VALIDATION = 'VALIDATION',// 验证错误
  PERMISSION = 'PERMISSION',// 权限错误
  SYSTEM = 'SYSTEM',        // 系统错误
}
```

### 常见错误码映射

| 错误码 | 错误类型 | 默认提示 | 展示方式 |
|--------|----------|----------|----------|
| 400 | VALIDATION | 请求参数有误，请检查输入信息 | message |
| 401 | AUTH | 登录状态已过期，即将跳转到登录页 | notification |
| 403 | PERMISSION | 权限不足，无法访问此资源 | notification |
| 404 | BUSINESS | 请求的资源不存在 | message |
| 500 | SYSTEM | 服务器内部错误，请联系管理员 | notification |
| ERR_NETWORK | NETWORK | 网络连接失败，请检查网络连接 | notification |
| ERR_TIMEOUT | NETWORK | 请求超时，请稍后重试 | message |

## 错误边界组件

错误边界会捕获组件渲染过程中的错误，防止整个应用崩溃：

```tsx
import ErrorBoundary from '../components/ErrorBoundary';

function App() {
  return (
    <ErrorBoundary
      fallback={<div>自定义错误页面</div>}
      onError={(error, errorInfo) => {
        // 自定义错误处理逻辑
        console.log('组件错误:', error, errorInfo);
      }}
    >
      <YourApp />
    </ErrorBoundary>
  );
}
```

## 配置和扩展

### 1. 添加新的错误码映射

在 `useErrorHandler.ts` 中的 `ERROR_CODE_MAPPING` 对象中添加新的映射：

```typescript
const ERROR_CODE_MAPPING: Record<string, ErrorConfig> = {
  // 现有映射...
  '4001': {
    type: ErrorType.BUSINESS,
    message: '用户不存在',
    showType: 'message',
  },
  // 更多自定义映射...
};
```

### 2. 自定义错误日志上报

修改 `logError` 函数以集成第三方错误监控服务：

```typescript
function logError(error: Error | string, config: ErrorConfig) {
  // 集成 Sentry
  // Sentry.captureException(error);
  
  // 集成自定义错误上报 API
  // errorReportingService.report({
  //   error: typeof error === 'string' ? error : error.message,
  //   config,
  //   // 更多上下文信息...
  // });
}
```

## 最佳实践

1. **统一使用错误处理**: 在所有异步操作中使用 `handleError` 来处理错误
2. **适当的错误类型**: 根据错误的性质选择合适的错误类型
3. **用户友好的提示**: 提供清晰、可理解的错误提示信息
4. **避免重复处理**: 不要在已经有全局错误处理的地方重复显示错误
5. **错误日志**: 保留足够的错误信息用于调试，但不要泄露敏感信息给用户

## 注意事项

- 所有的错误提示都会自动显示，无需手动调用 message 或 notification
- 401 错误会自动尝试刷新 token，失败后跳转到登录页
- 错误边界只在生产环境隐藏错误详情，开发环境会显示完整的错误信息
- 全局错误处理会阻止错误在控制台的默认显示，但仍会记录到日志中
