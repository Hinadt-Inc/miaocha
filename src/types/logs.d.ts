export {};
declare global {
  //
  interface Module {
    moduleName: string;
    permissionId: number | null;
  }

  interface IModulesResponse {
    datasourceId: number;
    datasourceName: string;
    databaseName: string;
    modules: Module[];
  }

  // 参数
  interface ILogColumnsParams {
    datasourceId: number;
    module: string;
  }

  interface ILogColumnsResponse {
    dataType: string; // 数据类型
    columnName: string; // 列名
    columnType?: string; // 列类型
    columnComment?: string; // 列注释
    isPrimaryKey?: boolean; // 是否为主键
    isNullable?: boolean; // 是否可为空
    isFixed?: boolean; // 是否为固定列
    selected?: boolean; // 是否选中
  }

  interface ISearchLogsParams {
    datasourceId?: number;
    module?: string;
    keywords?: string[];
    whereSqls?: string[];
    startTime?: string;
    endTime?: string;
    timeRange?: string;
    timeGrouping?: string;
    pageSize?: number;
    offset?: number;
    fields?: string[];
  }

  interface ISearchLogsResponse {
    columns: string[]; // 列名列表
    errorMessage?: string;
    executionTimeMs: number; // 查询耗时（毫秒）
    rows: Record<string, unknown>[]; // 日志数据明细列表
    success: boolean;
    totalCount: number; // 日志总数
    fieldDistributions: IFieldDistributions[];
  }
}
