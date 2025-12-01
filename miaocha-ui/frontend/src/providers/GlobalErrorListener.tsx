// 全局错误提示监听
import { useEffect } from 'react';

import { App } from 'antd';

export default function GlobalErrorListener() {
  const { message } = App.useApp();

  useEffect(() => {
    const handleRejection = (evt: any) => {
      const content = evt?.detail?.reason?.message || evt?.reason?.message || '发生未知错误';
      message.error({ content, key: 'global-unhandled-rejection' });
    };

    window.addEventListener('unhandledrejection', handleRejection as EventListener);

    return () => {
      window.removeEventListener('unhandledrejection', handleRejection as EventListener);
    };
  }, [message]);

  return null;
}
