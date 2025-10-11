import { ProLayout } from '@ant-design/pro-components';
import { Link, Outlet, useLocation } from 'react-router-dom';
import Profile from '@/components/Profile';
import { useState, useEffect } from 'react';
import NProgress from 'nprogress';
import ErrorBoundary from '@/components/Error/ErrorBoundary';
import { colorPrimary } from '@/utils/utils';
import { getAuthorizedRoutes } from './routes';
import { useSelector } from 'react-redux';

NProgress.configure({
  showSpinner: false, // 是否显示右上角的转圈加载图标
});

// 获取本地存储的collapsed状态
const getStoredCollapsed = () => {
  try {
    const stored = localStorage.getItem('siderCollapsed');
    return stored ? JSON.parse(stored) : true;
  } catch (error) {
    console.error('读取collapsed状态失败:', error);
    return true;
  }
};

// 设置collapsed状态到本地存储
const setStoredCollapsed = (value: boolean) => {
  try {
    localStorage.setItem('siderCollapsed', JSON.stringify(value));
  } catch (error) {
    console.error('保存collapsed状态失败:', error);
  }
};

const App = () => {
  const [collapsed, setCollapsed] = useState(getStoredCollapsed());
  const location = useLocation();
  const userRole = useSelector((state: { user: IStoreUser }) => state.user.role);
  const [openKeys, setOpenKeys] = useState<string[]>([]);

  // 地址变化时启动进度条
  useEffect(() => {
    NProgress.start();

    // 在下一帧/微任务后结束，避免过长占用
    const stop = () => {
      NProgress.done();
    };
    // 用 requestAnimationFrame 更平滑，也可改为 setTimeout(() => NProgress.done(), 0)
    const raf = requestAnimationFrame(stop);
    return () => cancelAnimationFrame(raf);
  }, [location.pathname, location.search]);

  // 监听collapsed变化，更新本地存储
  const handleCollapse = (value: boolean) => {
    setCollapsed(value);
    setStoredCollapsed(value);
  };

  useEffect(() => {
    document.documentElement.style.setProperty('--primary-color', colorPrimary);
  }, []);

  // 初始化打开的菜单项
  useEffect(() => {
    // 如果是系统管理的子路径，自动展开系统管理菜单
    if (location.pathname.startsWith('/system/')) {
      setOpenKeys(['/system']);
    }
  }, [location.pathname]);

  // 处理菜单展开/收起
  const handleOpenChange = (keys: string[] | boolean) => {
    if (Array.isArray(keys)) {
      setOpenKeys(keys);
    }
  };

  return (
    <div className="app-container">
      <ProLayout
        title="秒查"
        logo="/logo.png"
        collapsed={collapsed}
        onCollapse={handleCollapse}
        breakpoint={false}
        siderWidth={200}
        location={{ pathname: location.pathname }}
        openKeys={openKeys}
        onOpenChange={handleOpenChange}
        fixSiderbar={true}
        menuProps={{
          theme: 'light',
          mode: 'inline',
        }}
        route={{
          path: '/',
          routes: getAuthorizedRoutes(userRole),
        }}
        menuItemRender={(item, dom) => <Link to={item.path ?? '/'}>{dom}</Link>}
        avatarProps={{
          render: () => <Profile collapsed={collapsed} />,
        }}
      >
        <ErrorBoundary
          key={location.pathname} // 路由切换自动复位
        >
          <Outlet />
        </ErrorBoundary>
      </ProLayout>
    </div>
  );
};

export default App;
