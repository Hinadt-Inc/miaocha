/**
 * 用户角色常量定义
 */

// 管理员角色列表
export const ADMIN_ROLES = ['ADMIN', 'SUPER_ADMIN'] as const;

// 角色类型定义
export type AdminRole = (typeof ADMIN_ROLES)[number];

// 角色权限检查工具函数
export const isAdmin = (role: string | null): boolean => {
  return role !== null && ADMIN_ROLES.includes(role as AdminRole);
};
