/**
 * 数据源权限类型
 */
export interface DatasourcePermission {
  datasourceId: number
  datasourceName: string
  databaseName: string
  tables: TablePermission[]
}

/**
 * 表权限类型
 */
export interface TablePermission {
  tableName: string
  permissionId: string | null
}

/**
 * 用户数据源权限查询参数
 */
export interface UserDatasourcePermissionQuery {
  userId: string
  datasourceId: string
}

/**
 * 表权限操作参数
 */
export interface TablePermissionOperation {
  userId: string
  datasourceId: string
  tableName: string
}
