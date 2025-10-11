/**
 * LogModule 相关的工具函数
 */

/**
 * 检查两个数据数组是否相同
 * @param prevRows 之前的数据
 * @param newRows 新的数据
 * @returns 是否相同
 */
export const isSameDataArray = (prevRows: any[], newRows: any[]): boolean => {
  if (prevRows.length !== newRows.length) {
    return false;
  }

  if (prevRows.length === 0 && newRows.length === 0) {
    return true;
  }

  return prevRows[0]?._key === newRows[0]?._key;
};

/**
 * 检查是否为重复数据
 * @param prevRows 之前的数据
 * @param newRows 新的数据
 * @returns 是否重复
 */
export const isDuplicateData = (prevRows: any[], newRows: any[]): boolean => {
  if (prevRows.length === 0 || newRows.length === 0) {
    return false;
  }

  const lastKey = prevRows[prevRows.length - 1]?._key;
  const firstNewKey = newRows[0]?._key;

  return lastKey === firstNewKey;
};

/**
 * 合并数据数组
 * @param prevRows 之前的数据
 * @param newRows 新的数据
 * @param offset 偏移量
 * @returns 合并后的数据
 */
export const mergeDataArrays = (prevRows: any[], newRows: any[], offset: number): any[] => {
  if (offset === 0) {
    // 首次加载或刷新，直接替换
    if (isSameDataArray(prevRows, newRows)) {
      return prevRows;
    }
    return newRows.length > 0 ? newRows : [];
  } else if (newRows.length > 0) {
    // 加载更多，避免重复拼接
    if (isDuplicateData(prevRows, newRows)) {
      // 已经拼接过，不再拼接
      return prevRows;
    }

    // 确保数据按正确顺序合并，保留第一页数据以支持浏览器搜索
    const mergedRows = [...prevRows, ...newRows];

    // 去重处理，基于_key字段
    const uniqueRows = mergedRows.filter((row, index, arr) => {
      return arr.findIndex((r) => r._key === row._key) === index;
    });

    return uniqueRows;
  } else {
    return prevRows;
  }
};
