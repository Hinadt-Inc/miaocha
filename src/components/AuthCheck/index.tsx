import { useEffect } from 'react';
import { useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { Result, Button } from 'antd';

interface AuthCheckProps {
  children: React.ReactNode;
  requiredRoles: string[];
}

const AuthCheck: React.FC<AuthCheckProps> = ({ children, requiredRoles }) => {
  const navigate = useNavigate();
  const { role, isLoggedIn } = useSelector((state: { user: IStoreUser }) => state.user);

  const hasPermission = role && requiredRoles.includes(role);

  useEffect(() => {
    // 如果用户未登录，重定向到登录页面
    if (!isLoggedIn) {
      navigate('/login');
    }
  }, [isLoggedIn, navigate]);

  if (!isLoggedIn) {
    // 如果用户未登录，渲染空内容（会被重定向到登录页面）
    return null;
  }

  if (!hasPermission) {
    // 如果没有权限，显示无权限页面
    return (
      <Result
        status="403"
        title="403"
        subTitle="抱歉，您没有权限访问此页面"
        extra={
          <Button type="primary" onClick={() => navigate('/')}>
            返回首页
          </Button>
        }
      />
    );
  }

  // 有权限，渲染子组件
  return <>{children}</>;
};

export default AuthCheck;
