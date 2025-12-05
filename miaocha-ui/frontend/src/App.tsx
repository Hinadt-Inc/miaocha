import { useState, useEffect } from 'react';

import { ProLayout } from '@ant-design/pro-components';
import { ConfigProvider, message } from 'antd';
import NProgress from 'nprogress';
import { useSelector } from 'react-redux';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';

import ErrorBoundary from '@/components/Error/ErrorBoundary';
import Profile from '@/components/Profile';
import { onLogout, offLogout } from '@/utils/crossWindowSync';
import { colorPrimary } from '@/utils/utils';

import { getAuthorizedRoutes } from './routes';

NProgress.configure({
  showSpinner: false, // 是否显示右上角的转圈加载图标
});

// 获取本地存储的collapsed状态
const getStoredCollapsed = (): boolean => {
  try {
    const stored = localStorage.getItem('siderCollapsed');
    if (stored === 'true') return true;
    if (stored === 'false') return false;
    return true; // 未设置或值异常时的默认值
  } catch {
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
  const navigate = useNavigate();
  const userRole = useSelector((state: { user: IStoreUser }) => state.user.role);
  const [messageApi, contextHolder] = message.useMessage();
  window.messageApi = messageApi;
  const [openKeys, setOpenKeys] = useState<string[]>([]);

  // 监听跨窗口登出事件
  useEffect(() => {
    const handleLogout = () => {
      console.log('[App] 接收到跨窗口退出登录事件');
      // 跳转到登录页
      navigate('/login', { replace: true });
    };

    // 注册监听器
    onLogout(handleLogout);

    // 清理监听器
    return () => {
      offLogout(handleLogout);
    };
  }, [navigate]);

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
      <ConfigProvider
        theme={{
          // algorithm: antdTheme.darkAlgorithm, // 暗色算法
          token: {
            colorPrimary, // 影响主按钮、确定按钮等
            colorLink: colorPrimary, // 影响 Button type="link" 的文字颜色
            colorInfo: colorPrimary, // 影响 Info 提示框的背景色
          },
        }}
      >
        <ProLayout
          avatarProps={{
            render: () => <Profile collapsed={collapsed} />,
          }}
          breakpoint={false}
          collapsed={collapsed}
          fixSiderbar={true}
          location={{ pathname: location.pathname }}
          logo="/logo.png"
          menuItemRender={(item, dom) => <Link to={item.path ?? '/'}>{dom}</Link>}
          menuProps={{
            theme: 'light',
            mode: 'inline',
          }}
          openKeys={openKeys}
          route={{
            path: '/',
            routes: getAuthorizedRoutes(userRole),
          }}
          siderWidth={200}
          title="秒查"
          onCollapse={handleCollapse}
          onOpenChange={handleOpenChange}
        >
          <ErrorBoundary key={location.pathname}>
            {contextHolder}
            <Outlet />
          </ErrorBoundary>
        </ProLayout>
      </ConfigProvider>
    </div>
  );
};

export default App;
