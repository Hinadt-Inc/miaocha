import { FilterOperator, FieldDefinition } from '../types/logDataTypes';

const availableFields: FieldDefinition[] = [
  { name: 'timestamp', type: 'date' },
  { name: 'message', type: 'text' },
  { name: 'host', type: 'keyword' },
  { name: 'source', type: 'keyword' },
  { name: 'user_agent', type: 'text' },
  { name: 'status', type: 'number' },
  { name: 'bytes', type: 'number' },
  { name: 'response_time', type: 'number' },
  { name: 'ip', type: 'ip' },
  { name: 'method', type: 'keyword' },
  { name: 'path', type: 'keyword' },
  { name: 'referer', type: 'text' },
  { name: 'geo.country', type: 'keyword' },
  { name: 'geo.city', type: 'keyword' },
];

export const getFieldType = (fieldName: string): string => {
  const field = availableFields.find((f) => f.name === fieldName);
  return field?.type || 'keyword';
};
// 根据字段类型获取操作符选项
export const getOperatorsByFieldType = (fieldType?: string): { label: string; value: FilterOperator }[] => {
  const commonOperators: { label: string; value: FilterOperator }[] = [
    { label: '是', value: 'is' as const },
    { label: '不是', value: 'is_not' as const },
    { label: '存在', value: 'exists' as const },
    { label: '不存在', value: 'does_not_exist' as const },
  ];

  if (!fieldType) return commonOperators;

  switch (fieldType) {
    case 'text':
    case 'keyword':
      return [
        ...commonOperators,
        { label: '包含', value: 'contains' as const },
        { label: '不包含', value: 'does_not_contain' as const },
        { label: '是其中之一', value: 'is_one_of' as const },
        { label: '不是其中之一', value: 'is_not_one_of' as const },
      ];
    case 'number':
    case 'date':
      return [
        ...commonOperators,
        { label: '大于', value: 'greater_than' as const },
        { label: '小于', value: 'less_than' as const },
        { label: '在...之间', value: 'is_between' as const },
      ];
    default:
      return commonOperators;
  }
};

// 获取过滤器显示文本
export const getFilterDisplayText = (filter: {
  field: string;
  operator: FilterOperator;
  value: string | string[] | [number, number] | null;
}): string => {
  const { field, operator, value } = filter;

  switch (operator) {
    case 'is':
      return `${field}: ${value}`;
    case 'is_not':
      return `${field} 不是: ${value}`;
    case 'contains':
      return `${field} 包含: ${value}`;
    case 'does_not_contain':
      return `${field} 不包含: ${value}`;
    case 'exists':
      return `${field} 存在`;
    case 'does_not_exist':
      return `${field} 不存在`;
    case 'is_one_of':
      return `${field} 是: [${Array.isArray(value) ? value.join(', ') : value}]`;
    case 'is_not_one_of':
      return `${field} 不是: [${Array.isArray(value) ? value.join(', ') : value}]`;
    case 'greater_than':
      return `${field} > ${value}`;
    case 'less_than':
      return `${field} < ${value}`;
    case 'is_between':
      if (Array.isArray(value) && value.length === 2) {
        return `${field}: ${value[0]} 至 ${value[1]}`;
      }
      return `${field} 在范围内`;
    default:
      return `${field}: ${value}`;
  }
};

// 获取字段类型对应的图标颜色
export const getFieldTypeColor = (type: string): string => {
  const typeMap: Record<string, string> = {
    T: 'purple',
    S: 'blue',
    V: 'cyan',
    D: 'green',
    I: 'orange',
  };
  const firstChar = type.charAt(0).toUpperCase();
  return typeMap[firstChar] || 'default';
};

// 防抖函数
export const debounce = <F extends (...args: any[]) => any>(
  func: F,
  waitFor: number,
): ((...args: Parameters<F>) => void) => {
  let timeout: ReturnType<typeof setTimeout> | null = null;

  return (...args: Parameters<F>): void => {
    if (timeout !== null) {
      clearTimeout(timeout);
    }
    timeout = setTimeout(() => func(...args), waitFor);
  };
};

// 节流函数
export const throttle = <F extends (...args: any[]) => any>(
  func: F,
  waitFor: number,
): ((...args: Parameters<F>) => void) => {
  let lastTime = 0;

  return (...args: Parameters<F>): void => {
    const now = Date.now();
    if (now - lastTime >= waitFor) {
      func(...args);
      lastTime = now;
    }
  };
};

// 缓存函数结果
export const memoize = <F extends (...args: any[]) => any>(func: F): ((...args: Parameters<F>) => ReturnType<F>) => {
  const cache = new Map<string, ReturnType<F>>();

  return (...args: Parameters<F>): ReturnType<F> => {
    const key = JSON.stringify(args);
    if (cache.has(key)) {
      return cache.get(key)!;
    }

    const result = func(...args);
    cache.set(key, result);
    return result;
  };
};
