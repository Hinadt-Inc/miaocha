
// 自定义比较函数类型
type CompareProps<T> = (prevProps: T, nextProps: T) => boolean;

/**
 * 通用纯组件包装器，对组件应用 React.memo 以减少不必要的重渲染
 * @param Component 要包装的组件
 * @param compareProps 自定义比较函数，默认为浅比较
 * @returns 包装后的纯组件
 */
export function withPureComponent<T extends object>(
  Component: React.ComponentType<T>,
  compareProps?: CompareProps<T>
): React.MemoExoticComponent<React.ComponentType<T>> {
  return React.memo(Component, compareProps);
}

/**
 * 简单的属性比较函数，用于比较特定的属性是否相等
 * @param keys 需要比较的属性名数组
 * @returns 比较函数
 */
export function createPropsComparator<T extends object>(keys: (keyof T)[]): CompareProps<T> {
  return (prevProps, nextProps) => {
    return keys.every(key => prevProps[key] === nextProps[key]);
  };
}

/**
 * 示例用法：
 * 
 * // 基本用法，使用默认的浅比较
 * const PureComponent = withPureComponent(MyComponent);
 * 
 * // 使用自定义比较函数
 * const comparator = createPropsComparator(['id', 'name']);
 * const PureComponent = withPureComponent(MyComponent, comparator);
 */