# AutoRefresh 自动刷新模块

仿照Kibana风格的自动刷新功能模块，提供了完整的自动刷新解决方案。

## 📁 文件结构

```
AutoRefresh/
├── index.ts                 # 模块统一导出文件
├── AutoRefresh.tsx          # 主组件
├── index.module.less        # 样式文件
├── types.ts                 # TypeScript 类型定义
├── constants.ts             # 常量配置
├── utils.ts                 # 工具函数
├── useAutoRefresh.ts        # 自定义Hook
└── README.md               # 文档说明
```

## 🚀 快速开始

### 基本使用

```tsx
import AutoRefresh from './AutoRefresh';

const MyComponent = () => {
  const handleRefresh = () => {
    // 执行刷新逻辑
    console.log('刷新数据...');
  };

  return (
    <AutoRefresh
      onRefresh={handleRefresh}
      loading={false}
      disabled={false}
    />
  );
};
```

### 使用自定义Hook

```tsx
import { useAutoRefresh } from './AutoRefresh';

const MyComponent = () => {
  const {
    isAutoRefreshing,
    refreshInterval,
    remainingTime,
    toggleAutoRefresh,
    setRefreshInterval,
    handleManualRefresh,
  } = useAutoRefresh(() => {
    // 刷新逻辑
  });

  return (
    <div>
      <button onClick={toggleAutoRefresh}>
        {isAutoRefreshing ? '停止' : '开始'}自动刷新
      </button>
      <p>剩余时间: {remainingTime}ms</p>
    </div>
  );
};
```

## 📋 API 文档

### AutoRefresh 组件属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| onRefresh | `() => void` | - | 刷新回调函数（必需） |
| loading | `boolean` | `false` | 是否正在加载 |
| disabled | `boolean` | `false` | 是否禁用组件 |

### useAutoRefresh Hook

#### 参数
- `onRefresh: () => void` - 刷新回调函数
- `loading?: boolean` - 是否正在加载

#### 返回值
```tsx
{
  isAutoRefreshing: boolean;           // 是否正在自动刷新
  refreshInterval: number;             // 刷新间隔（毫秒）
  remainingTime: number;               // 剩余时间（毫秒）
  lastRefreshTime: Date | null;        // 上次刷新时间
  isPaused: boolean;                   // 是否暂停
  toggleAutoRefresh: () => void;       // 切换自动刷新状态
  setRefreshInterval: (value: number) => void; // 设置刷新间隔
  handleManualRefresh: () => void;     // 手动刷新
}
```

### 工具函数

#### formatRemainingTime
格式化剩余时间显示
```tsx
formatRemainingTime(ms: number, loading?: boolean): string
```

#### formatLastRefreshTime
格式化上次刷新时间
```tsx
formatLastRefreshTime(date: Date | null): string
```

#### calculateProgressPercent
计算进度百分比
```tsx
calculateProgressPercent(
  refreshInterval: number,
  remainingTime: number,
  isAutoRefreshing: boolean,
  loading?: boolean
): number
```

#### generateTooltipContent
生成Tooltip内容
```tsx
generateTooltipContent(
  isAutoRefreshing: boolean,
  refreshInterval: number,
  currentIntervalLabel: string,
  loading?: boolean,
  remainingTime?: number,
  lastRefreshTime?: Date | null
): string
```

## 🎨 样式定制

组件使用CSS Modules，可以通过覆盖样式类来定制外观：

```less
// 自定义样式
.autoRefresh {
  // 覆盖默认样式
  .refreshButton {
    color: #custom-color;
  }
}
```

## 🔧 配置选项

### 刷新间隔选项

在 `constants.ts` 中定义：

```tsx
export const REFRESH_INTERVALS = [
  { label: '关闭', value: 0 },
  { label: '5秒', value: 5000 },
  { label: '10秒', value: 10000 },
  { label: '30秒', value: 30000 },
  { label: '1分钟', value: 60000 },
  { label: '5分钟', value: 300000 },
  // ...更多选项
];
```

### 默认配置

```tsx
export const DEFAULT_CONFIG = {
  COUNTDOWN_INTERVAL: 1000, // 倒计时间隔（毫秒）
  RESTART_DELAY: 100,       // 重启延迟（毫秒）
};
```

## ✨ 特性

- 🎯 **仿Kibana设计**: 遵循Kibana的UI设计风格
- 🔄 **智能状态管理**: 自动处理loading状态和倒计时
- 📱 **响应式设计**: 适配不同屏幕尺寸
- 🧩 **模块化设计**: 清晰的文件结构和职责分离
- 🎨 **可定制样式**: 支持样式覆盖和主题定制
- 🔧 **TypeScript支持**: 完整的类型定义
- 🚀 **性能优化**: 使用useCallback和useMemo优化渲染

## 🐛 注意事项

1. 组件会在loading期间自动暂停倒计时
2. 手动刷新会重置自动刷新的倒计时
3. 组件卸载时会自动清理所有定时器
4. 修改刷新间隔时，如果正在自动刷新会立即应用新间隔
