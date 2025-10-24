// 全局错误提示监听
import { useEffect } from 'react';

import { App } from 'antd';

export default function GlobalErrorListener() {
  const { message } = App.useApp();

  useEffect(() => {
    const handleRejection = (evt: any) => {
      console.log('【全局异常】', evt);
      // evt?.detail?.reason?.message; // 自定义的错误，例如：@/src/api/request.ts 的错误
      // evt?.reason?.message; // js的错误、throw的错误
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
