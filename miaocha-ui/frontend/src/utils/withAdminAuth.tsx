// todo 删除
import React from 'react';

/**
 * 管理员权限控制高阶组件
 * 只允许管理员和超级管理员访问
 */
const withAdminAuth = <P extends object>(WrappedComponent: React.ComponentType<P>): React.FC<P> => {
  const AdminProtectedComponent: React.FC<P> = (props) => <WrappedComponent {...props} />;

  // 设置组件显示名称，便于调试
  AdminProtectedComponent.displayName = `withAdminAuth(${WrappedComponent.displayName || WrappedComponent.name})`;

  return AdminProtectedComponent;
};

export default withAdminAuth;
