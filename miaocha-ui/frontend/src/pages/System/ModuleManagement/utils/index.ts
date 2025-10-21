import type { Module, ModuleData } from '../types';

/**
 * 转换模块数据为表格所需的格式
 */
export const transformModuleData = (modules: Module[]): ModuleData[] => {
  return modules.map((module) => ({
    ...module,
    key: module.id.toString(),
    users: module.users?.map((user) => ({
      ...user,
      role: (user as any).role || 'USER',
    })),
  }));
};

/**
 * 搜索模块数据
 */
export const searchModuleData = (modules: ModuleData[], searchText: string): ModuleData[] => {
  if (!searchText.trim()) {
    return modules;
  }

  const cleanValue = searchText.replace(/['"]/g, '');
  const searchTerms = cleanValue
    .toLowerCase()
    .split(/\s+/)
    .filter((term) => term);

  const matchesSearchTerms = (module: ModuleData) => {
    if (searchTerms.length === 0) return true;
    const moduleName = module.name?.toLowerCase() || '';
    const datasourceName = module.datasourceName?.toLowerCase() || '';
    const tableName = module.tableName?.toLowerCase() || '';
    return searchTerms.every((term) => `${moduleName} ${datasourceName} ${tableName}`.includes(term));
  };

  return modules.filter(matchesSearchTerms);
};
