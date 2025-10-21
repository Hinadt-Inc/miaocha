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
  tableName: string;
  permissions: string[];
  id: string;
  permissionId?: string;
  users?: {
    userId: number;
    nickname: string;
    email: string;
    role: string;
  }[];
}

/**
 * 用户权限详情
 */
export interface UserPermissionDetail {
  permissionId: number;
  userId: number;
  nickname: string;
  email: string;
  role: string;
  createTime: string;
  updateTime: string;
}

/**
 * 新的权限响应类型
 */
export interface PermissionResponse {
  datasourceId: number;
  datasourceName: string;
  module: string;
  users: UserPermissionDetail[];
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

/**
 * 模块响应类型 - 用于未授权模块列表
 */
export type IModulesResponse = string;

/**
 * 模块权限类型
 */
// export interface ModulePermission {
//   id: string;
//   userId: number;
//   datasourceId: number;
//   datasourceName: string;
//   databaseName: string;
//   module: string;
//   createTime: string;
//   createUser: string;
//   createUserName: string;
//   updateTime: string;
//   updateUser: string;
//   updateUserName: string;
//   users?: {
//     userId: number;
//     userName: string;
//   }[];
// }

export interface UserModulePermission {
  moduleId: string;
  moduleName?: string;
}
