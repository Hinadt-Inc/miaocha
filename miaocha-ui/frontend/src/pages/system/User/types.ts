// 单个模块权限项
export interface ModulePermissionListItem {
  id: number;
  userId: number;
  module: string;
  databaseName: string;
  datasourceId: number;
  datasourceName: string;
  queryConfig: Record<string, unknown> | null;
  createTime: string;
  updateTime: string;
  createUser: string;
  createUserName: string;
  updateUser: string;
  updateUserName: string;
}

// 列表
export interface UserListItem {
  id: number;
  nickname: string;
  email: string;
  role: string;
  status: number;
  username?: string;
  createTime?: string;
  updateTime?: string;
  modulePermissions?: ModulePermissionListItem[];
}

// 修改密码
export interface ChangePasswordPayload {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

// 修改密码参数
export interface ChangePasswordPayloadBySuperAdmin {
  oldPassword: string;
  newPassword: string;
}

// 创建用户
export interface CreateUserPayload {
  username: string;
  nickname: string;
  password: string;
  email: string;
  role: string;
  status: number;
}

// 修改用户
export interface UpdateUserPayload {
  id: number;
  nickname: string;
  email: string;
  role: string;
  status: number;
}

// 授权模块
export interface ModulePermissionResponse {
  success: boolean;
  message?: string;
}
