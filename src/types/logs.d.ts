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
    columnName?: string;
    dataType?: string;
    selected?: boolean;
    columnComment?: string;
    isPrimaryKey?: boolean;
    isNullable?: boolean;
    isFixed?: boolean;
  }

  interface ISearchLogsParams {
    datasourceId?: number;
    module?: string;
    keywords?: string;
    whereSqls?: string;
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
    distribution: Record<string, unknown>[];
    distributionData?: Array<{
      timePoint: string;
      count: number;
    }>; // 日志时间分布数据，用于生成时间分布图
    errorMessage?: string;
    executionTimeMs: number; // 查询耗时（毫秒）
    fieldDistributions: Record<string, unknown>[]; // 字段数据分布统计信息，用于展示各字段的Top5值及占比
    records: Record<string, unknown>[];
    rows: Record<string, unknown>[]; // 日志数据明细列表
    success: boolean;
    totalCount: number; // 日志总数
  }
}
