# 组件设计模式

## 组件结构模式
```typescript
// 标准组件结构
interface ComponentProps {
  // props 定义
}

const Component: React.FC<ComponentProps> = ({ prop1, prop2 }) => {
  // hooks
  // 事件处理函数
  // 渲染逻辑
  
  return (
    // JSX
  );
};

export default Component;
```

## 自定义 Hooks 模式
```typescript
// 业务逻辑封装
const useFeature = (params: FeatureParams) => {
  const [state, setState] = useState();
  
  // 业务逻辑
  
  return {
    state,
    actions: {
      action1,
      action2
    }
  };
};
```

## 高阶组件模式
- 使用 React.memo 包装纯组件
- 使用 forwardRef 传递 ref
- 使用 ErrorBoundary 包装错误边界

## 组合模式
- 使用 children 实现组件组合
- 使用 render props 模式
- 使用 Context 共享状态

## 表单处理模式
- 使用 Ant Design Form 组件
- 自定义表单验证规则
- 统一的表单提交处理

## 表格组件模式
- 使用虚拟化表格处理大数据
- 可配置的列定义
- 统一的排序和过滤逻辑
- 可扩展的行操作

## 模态框模式
- 使用 Ant Design Modal
- 统一的确认和取消处理
- 表单模态框的数据流

## 加载状态模式
- 统一的 Loading 组件
- 骨架屏占位
- 错误状态展示

## 路由组件模式
- 页面级组件放在 pages 目录
- 使用 React Router 进行路由管理
- 路由守卫和权限控制