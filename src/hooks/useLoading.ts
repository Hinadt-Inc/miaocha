import { useState, useEffect, useCallback } from 'react';
import { useLoading as useContextLoading } from '../providers/LoadingProvider';

// 加载状态管理
let activeLoadingCount = 0;
const MAX_LOADING_TIME = 30000; // 最大加载时间，防止无限加载

// 使用全局上下文的loading hook
export const useGlobalLoading = (initialState: boolean = false) => {
  const { isLoading, startLoading, endLoading } = useContextLoading();
  const [localLoading, setLocalLoading] = useState(initialState);
  
  // 组件初始化时，如果有初始加载状态，则启动全局加载
  useEffect(() => {
    if (initialState) {
      startLoading();
      return () => endLoading();
    }
  }, [initialState, startLoading, endLoading]);
  
  const startLocalLoading = useCallback(() => {
    setLocalLoading(true);
    startLoading();
    
    // 安全措施：确保loading状态不会永久保持
    const timer = setTimeout(() => {
      endLocalLoading();
    }, MAX_LOADING_TIME);
    
    return () => clearTimeout(timer);
  }, [startLoading]);
  
  const endLocalLoading = useCallback(() => {
    setLocalLoading(false);
    endLoading();
  }, [endLoading]);
  
  // 组件卸载时自动结束loading
  useEffect(() => {
    return () => {
      if (localLoading) {
        endLocalLoading();
      }
    };
  }, [localLoading, endLocalLoading]);
  
  return { isLoading: localLoading, startLoading: startLocalLoading, endLoading: endLocalLoading };
};

// 全局加载状态控制
export const setGlobalLoadingActive = (active: boolean) => {
  if (active) {
    document.body.classList.add('loading-active');
  } else {
    document.body.classList.remove('loading-active');
  }
};

// 获取当前全局加载状态
export const isGlobalLoadingActive = () => {
  return document.body.classList.contains('loading-active');
};