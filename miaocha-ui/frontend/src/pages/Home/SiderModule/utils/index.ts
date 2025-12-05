/**
 * Sider模块相关的工具函数
 */

/**
 * 获取收藏的模块
 */
export const getFavoriteModule = (): string => {
  return localStorage.getItem('favoriteModule') || '';
};

/**
 * 设置收藏的模块
 */
export const setFavoriteModuleStorage = (module: string): void => {
  if (module) {
    localStorage.setItem('favoriteModule', module);
  } else {
    localStorage.removeItem('favoriteModule');
  }
};

/**
 * 切换收藏状态
 */
export const toggleFavoriteModule = (module: string, currentFavorite: string): string => {
  if (currentFavorite === module) {
    // 取消收藏
    setFavoriteModuleStorage('');
    return '';
  } else {
    // 设置收藏
    setFavoriteModuleStorage(module);
    return module;
  }
};

/**
 * 获取本地存储的激活字段
 */
export const getLocalActiveColumns = (): string[] => {
  return JSON.parse(localStorage.getItem('activeColumns') || '[]');
};

/**
 * 设置本地存储的激活字段
 */
export const setLocalActiveColumns = (columns: string[]): void => {
  localStorage.setItem('activeColumns', JSON.stringify(columns));
};

/**
 * 更新搜索参数到本地存储
 */
export const updateSearchParamsInStorage = (fields: string[]): void => {
  const savedSearchParams = localStorage.getItem('searchBarParams');
  if (savedSearchParams) {
    const params = JSON.parse(savedSearchParams);
    localStorage.setItem(
      'searchBarParams',
      JSON.stringify({
        ...params,
        fields,
      }),
    );
  }
};

/**
 * 清空搜索条件并保持字段
 */
export const clearSearchConditionsKeepFields = (fields: string[]): void => {
  const savedSearchParams = localStorage.getItem('searchBarParams');
  if (savedSearchParams) {
    const params = JSON.parse(savedSearchParams);
    localStorage.setItem(
      'searchBarParams',
      JSON.stringify({
        ...params,
        keywords: [],
        whereSqls: [],
        fields,
      }),
    );
  }
};

/**
 * 数组count求和
 */
export const sumArrayCount = (valueDistributions: IValueDistributions[]): number => {
  if (!valueDistributions || !Array.isArray(valueDistributions)) {
    return 0;
  }

  const counts = valueDistributions.map((item) => item.count);
  return counts.reduce((sum: number, item: string | number): number => {
    const num = typeof item === 'string' ? parseFloat(item) : item;
    return sum + (isNaN(num) ? 0 : num);
  }, 0);
};

/**
 * 生成查询条件的唯一标识符
 */
export const generateQueryConditionsKey = (conditions: {
  currentModule?: string;
  whereSqls?: string[];
  keywords?: string[];
  startTime?: string;
  endTime?: string;
  fields?: string[];
  columnName?: string;
  timestamp?: number;
}): string => {
  return JSON.stringify(conditions);
};

/**
 * 检查是否有分布数据
 */
export const hasDistributionData = (dist: IFieldDistributions): boolean => {
  return !!(
    dist &&
    ((dist.nonNullCount || 0) > 0 || (dist.totalCount || 0) > 0 || (dist.valueDistributions?.length || 0) > 0)
  );
};

/**
 * 清理本地存储中无效的字段
 */
export const cleanInvalidFieldsFromStorage = (validFields: string[]): void => {
  const savedSearchParams = localStorage.getItem('searchBarParams');
  if (savedSearchParams) {
    try {
      const params = JSON.parse(savedSearchParams);
      if (params.fields && Array.isArray(params.fields)) {
        const validStoredFields = params.fields.filter((field: string) => validFields.includes(field));
        if (validStoredFields.length !== params.fields.length) {
          console.warn('检测到本地存储中有无效字段，已自动清理');
          localStorage.setItem(
            'searchBarParams',
            JSON.stringify({
              ...params,
              fields: validStoredFields,
            }),
          );
        }
      }
    } catch (error) {
      console.error('清理本地存储字段时出错:', error);
    }
  }

  // 同时清理 activeColumns
  const activeColumns = getLocalActiveColumns();
  if (activeColumns.length > 0) {
    const validActiveColumns = activeColumns.filter((field) => validFields.includes(field));
    if (validActiveColumns.length !== activeColumns.length) {
      setLocalActiveColumns(validActiveColumns);
    }
  }
};
