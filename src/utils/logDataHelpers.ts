import { LogData, FilterOperator, FieldDefinition } from '../types/logDataTypes';

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

// 生成模拟数据
export const generateMockData = (start: number, count: number): LogData[] => {
  return Array(count)
    .fill(null)
    .map((_, index) => {
      const actualIndex = start + index;
      return {
        key: actualIndex.toString(),
        timestamp: `2025-04-${(actualIndex % 14) + 1} ${actualIndex % 24}:${(actualIndex % 60).toString().padStart(2, '0')}:00`,
        message: `这是日志消息 ${actualIndex}`,
        host: `server-${actualIndex % 5}.example.com`,
        level: ['info', 'warning', 'error'][actualIndex % 3],
        source:
          actualIndex % 3 === 0 ? 'nginx' : actualIndex % 3 === 1 ? 'application' : 'database',
        status: actualIndex % 10 === 0 ? 500 : actualIndex % 5 === 0 ? 404 : 200,
        bytes: Math.floor(Math.random() * 10000),
        response_time: Math.round(Math.random() * 1000) / 10,
        ip: `192.168.1.${actualIndex % 256}`,
        method: actualIndex % 4 === 0 ? 'POST' : actualIndex % 3 === 0 ? 'PUT' : 'GET',
        path: index % 3 === 0 ? '/api/users' : index % 5 === 0 ? '/api/products' : '/api/orders',
        user_agent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
        referer: 'https://example.com',
        'geo.country': 'China',
        'geo.city':
          actualIndex % 3 === 0 ? 'Beijing' : actualIndex % 3 === 1 ? 'Shanghai' : 'Guangzhou',
      };
    });
};

// 根据字段类型获取操作符选项
export const getOperatorsByFieldType = (
  fieldType?: string,
): { label: string; value: FilterOperator }[] => {
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
  switch (type) {
    case 'TEXT':
      return 'purple';
    case 'keyword':
      return 'blue';
    case 'NUMBER':
      return 'cyan';
    case 'DATETIME':
      return 'green';
    case 'ip':
      return 'orange';
    default:
      return 'default';
  }
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
export const memoize = <F extends (...args: any[]) => any>(
  func: F,
): ((...args: Parameters<F>) => ReturnType<F>) => {
  const cache = new Map<string, ReturnType<F>>();

  return (...args: Parameters<F>): ReturnType<F> => {
    const key = JSON.stringify(args);
    if (cache.has(key)) {
      return cache.get(key) as ReturnType<F>;
    }

    const result = func(...args);
    cache.set(key, result);
    return result;
  };
};

// 优化大型数据集渲染的分片处理函数
export const chunkProcess = <T, R>(
  items: T[],
  processor: (item: T) => R,
  chunkSize: number = 100,
): Promise<R[]> => {
  return new Promise((resolve) => {
    const result: R[] = [];
    let index = 0;

    function processNextChunk() {
      const start = performance.now();

      while (index < items.length) {
        result.push(processor(items[index]));
        index++;

        // 如果处理了足够多的项或超过时间预算，中断操作并安排下一个时间片
        if (index % chunkSize === 0 || performance.now() - start > 16) {
          setTimeout(processNextChunk, 0);
          return;
        }
      }

      resolve(result);
    }

    processNextChunk();
  });
};

// 从对象数组中提取唯一值
export const extractUniqueValues = <T, K extends keyof T>(items: T[], key: K): T[K][] => {
  const uniqueValues = new Set<T[K]>();
  items.forEach((item) => uniqueValues.add(item[key]));
  return Array.from(uniqueValues);
};

// 生成模拟时间分布数据
export const generateMockDistributionData = (): Array<{ timePoint: string; count: number }> => {
  const data: Array<{ timePoint: string; count: number }> = [];

  // 使用固定日期，确保数据稳定
  const today = '2025-04-23';

  // 创建一些重复的时间点，模拟真实场景中的数据分布
  // 典型的时间序列数据，一小时生成几个数据点
  for (let hour = 0; hour < 24; hour++) {
    const hourStr = hour.toString().padStart(2, '0');

    // 为每小时生成1-3个样本，更接近真实场景
    const samplesInHour = Math.floor(Math.random() * 3) + 1;

    for (let i = 0; i < samplesInHour; i++) {
      // 生成随机分钟，使数据点在同一小时内有所不同
      const minute = Math.floor(Math.random() * 60)
        .toString()
        .padStart(2, '0');
      const formattedTime = `${today} ${hourStr}:${minute}:00`;
      const count = Math.floor(Math.random() * 40) + 5; // 随机生成5-45之间的数据数量

      data.push({
        timePoint: formattedTime,
        count,
      });
    }
  }

  // 确保数据按时间排序
  return data.sort((a, b) => new Date(a.timePoint).getTime() - new Date(b.timePoint).getTime());
};
