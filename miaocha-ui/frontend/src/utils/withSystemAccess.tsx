import AuthCheck from '@/components/AuthCheck';

// 系统管理页面的包装器组件，用于添加访问控制
const withSystemAccess = <P extends object>(Component: React.ComponentType<P>): React.FC<P> => {
  const ComponentWithAccess = (props: P) => {
    return (
      <AuthCheck requiredRoles={['ADMIN', 'SUPER_ADMIN']}>
        <Component {...props} />
      </AuthCheck>
    );
  };

  // 保留原始组件的显示名称
  ComponentWithAccess.displayName = `withSystemAccess(${Component.displayName || Component.name || 'Component'})`;

  return ComponentWithAccess;
};

export default withSystemAccess;
