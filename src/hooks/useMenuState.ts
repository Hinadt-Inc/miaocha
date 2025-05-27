import { useState, useEffect, useMemo } from 'react';
import { useLocation } from 'react-router-dom';

const useMenuState = (location: ReturnType<typeof useLocation>) => {
  const [openKeys, setOpenKeys] = useState<string[]>([]);

  // 计算当前选中的菜单项
  const selectedKeys = useMemo(() => {
    // 获取当前路径并规范化
    let pathname = location.pathname;
    if (pathname === '' || pathname === '/') {
      // 确保根路径一定会高亮
      return ['/'];
    }
    return [pathname];
  }, [location.pathname]);

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

  return { openKeys, selectedKeys, handleOpenChange };
};

export default useMenuState;
