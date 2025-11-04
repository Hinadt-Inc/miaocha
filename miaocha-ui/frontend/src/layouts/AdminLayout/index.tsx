import React, { useEffect } from 'react';

import NProgress from 'nprogress';
import { useSelector } from 'react-redux';
import { Outlet, Navigate } from 'react-router-dom';

import { isAdmin } from '@/constants/roles';

const AdminLayout: React.FC = () => {
  const role = useSelector((state: { user: { role: string | null | undefined } }) => state.user.role);
  const hasToken = !!localStorage.getItem('accessToken');

  const loading = hasToken && (role === '' || role === null || role === undefined);

  // 用 NProgress 显示“加载中”
  useEffect(() => {
    if (loading) {
      NProgress.start();
    } else {
      NProgress.done();
    }
  }, [loading]);

  if (loading) {
    // 加载期间不渲染内容（避免误判），仅显示顶部进度条
    return null;
  }

  // 角色就绪后再判断权限
  if (!isAdmin(role || '')) {
    return <Navigate replace to="/403" />;
  }

  return <Outlet />;
};

export default AdminLayout;
