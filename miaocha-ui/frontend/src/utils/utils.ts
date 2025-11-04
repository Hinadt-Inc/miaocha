export const colorPrimary = '#0038FF'; // 主题色

/**
 * 判断两个时间点之间的间隔是否超过一天（24小时）
 * @param startTime 开始时间字符串
 * @param endTime 结束时间字符串
 * @returns 超过24小时返回true，否则返回false
 */
export const isOverOneDay = (startTime: string, endTime: string): boolean => {
  const start = new Date(startTime).getTime(); // 开始时间转为毫秒
  const end = new Date(endTime).getTime(); // 结束时间转为毫秒
  return end - start > 24 * 60 * 60 * 1000; // 判断是否超过24小时
};

/**
 * 防抖函数，延迟执行传入的函数，如果在延迟时间内再次调用则重新计时
 * @param func 需要防抖的函数
 * @param wait 延迟时间（毫秒）
 * @param immediate 是否立即执行一次
 * @returns 防抖后的函数
 */

export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number,
  immediate: boolean = false,
): T & { cancel: () => void } {
  let timeout: any = null;
  let result: ReturnType<T> | undefined;

  const debounced = function (this: ThisParameterType<T>, ...args: Parameters<T>): ReturnType<T> | undefined {
    if (timeout) {
      clearTimeout(timeout);
    }

    if (immediate) {
      // 如果是立即执行模式
      const callNow = !timeout;
      timeout = setTimeout(() => {
        timeout = null;
      }, wait);

      if (callNow) {
        result = func.apply(this, args);
      }
    } else {
      // 常规模式：延迟执行
      timeout = setTimeout(() => {
        result = func.apply(this, args);
      }, wait);
    }

    return result;
  } as T & { cancel: () => void };

  // 添加取消功能
  debounced.cancel = function () {
    if (timeout) {
      clearTimeout(timeout);
      timeout = null;
    }
  };

  return debounced;
}

/**
 * 值类型枚举（用于通用排序）
 */
enum ValueType {
  Number = 'Number',
  English = 'English',
  Chinese = 'Chinese',
  Empty = 'Empty',
}

const NUMERIC_REG = /^-?\d+(?:\.\d+)?$/;
const hasChineseChar = (s: string) => /[\u4e00-\u9fff]/.test(s);
const hasEnglishLetter = (s: string) => /[A-Za-z]/.test(s);

const getTypeWeight = (type: ValueType): number => {
  switch (type) {
    case ValueType.Number:
      return 0; // 数字优先
    case ValueType.English:
      return 1; // 英文第二
    case ValueType.Chinese:
      return 2; // 中文第三
    case ValueType.Empty:
    default:
      return 3; // 空值最后
  }
};

/**
 * 获取值及类型（统一处理空值、数字、英文、中文）
 * @param v 原始值
 */
const getValueAndType = (v: unknown): { str: string; type: ValueType; num?: number } => {
  if (v === null || v === undefined) return { str: '', type: ValueType.Empty };

  if (typeof v === 'number') {
    if (Number.isNaN(v)) return { str: '', type: ValueType.Empty };
    return { str: String(v), type: ValueType.Number, num: v };
  }

  const s = String(v).trim();
  if (s.length === 0) return { str: '', type: ValueType.Empty };

  if (NUMERIC_REG.test(s)) {
    const n = Number(s);
    return Number.isNaN(n) ? { str: '', type: ValueType.Empty } : { str: s, type: ValueType.Number, num: n };
  }

  // 优先判断是否包含中文字符
  if (hasChineseChar(s)) {
    return { str: s, type: ValueType.Chinese };
  }

  // 只要包含英文字母则视为英文，其余符号也按英文归类处理
  if (hasEnglishLetter(s) || s.length > 0) {
    return { str: s, type: ValueType.English };
  }

  return { str: '', type: ValueType.Empty };
};

/**
 * 通用排序方法：按照权重进行排序 数字 > 英文 > 中文 > 空
 * 当类型相同：
 * - 数字：按数值大小排序
 * - 英文：按英文的字母顺序排序（不区分大小写）
 * - 中文：按中文本地化顺序排序
 * - 空：保持一致，认为相等
 *
 * @param prevRow 前一个行数据
 * @param nextRow 后一个行数据
 * @param key 列的字段 key
 * @returns 排序比较结果：负数表示 prevRow < nextRow，正数表示 prevRow > nextRow，0 表示相等
 */
export function generalSorter<T extends Record<string, any>>(prevRow: T, nextRow: T, key: string): number {
  const a = (prevRow as any)?.[key];
  const b = (nextRow as any)?.[key];

  const aInfo = getValueAndType(a);
  const bInfo = getValueAndType(b);

  const aWeight = getTypeWeight(aInfo.type);
  const bWeight = getTypeWeight(bInfo.type);

  // 先按类型权重排序
  if (aWeight !== bWeight) return aWeight - bWeight;

  // 类型相同，按各自类型规则排序
  switch (aInfo.type) {
    case ValueType.Number: {
      // 数字按数值大小排序
      const av = aInfo.num ?? Number(aInfo.str);
      const bv = bInfo.num ?? Number(bInfo.str);
      if (Number.isNaN(av) && Number.isNaN(bv)) return 0;
      if (Number.isNaN(av)) return 1;
      if (Number.isNaN(bv)) return -1;
      return av - bv;
    }

    case ValueType.English: {
      // 英文不区分大小写，按英文 locale 排序
      const as = aInfo.str.toLowerCase();
      const bs = bInfo.str.toLowerCase();
      const cmp = as.localeCompare(bs, 'en', { sensitivity: 'base', numeric: false });
      return cmp;
    }

    case ValueType.Chinese: {
      // 中文按中文 locale 排序
      const cmp = aInfo.str.localeCompare(bInfo.str, 'zh-CN', { sensitivity: 'base', numeric: false });
      return cmp;
    }

    case ValueType.Empty:
    default:
      return 0;
  }
}
