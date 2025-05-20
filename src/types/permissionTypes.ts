/**
 * 数据源权限类型
 */
export interface DatasourcePermission {
  datasourceId: number;
  datasourceName: string;
  databaseName: string;
  tables: TablePermission[];
  modules: TablePermission[];
  id: string;
}

/**
 * 表权限类型
 */
export interface TablePermission {
  moduleName: string;
  permissionId: string | null;
  tableName: string; // 新增tableName属性
  permissions: string[]; // 新增权限数
  id: string;
}

/**
 * 新的权限响应类型
 */
export interface PermissionResponse {
  id: number;
  userId: number;
  datasourceId: number;
  module: string;
  createTime: string;
  updateTime: string;
}

/**
 * 用户数据源权限查询参数
 */
export interface UserDatasourcePermissionQuery {
  userId: string;
  datasourceId: string;
}

/**
 * 表权限操作参数
 */
export interface TablePermissionOperation {
  userId: string;
  datasourceId: string;
  tableName: string;
}
