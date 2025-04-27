import { useState, useEffect, useCallback, useRef } from 'react';
import { useLoading as useContextLoading } from '../providers/LoadingProvider';

// 加载状态管理
const MAX_LOADING_TIME = 30000; // 最大加载时间，防止无限加载

// 使用全局上下文的loading hook
export const useGlobalLoading = (initialState: boolean = false) => {
  const { isLoading, startLoading, endLoading } = useContextLoading();
  const [localLoading, setLocalLoading] = useState(initialState);
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  
  // 组件初始化时，如果有初始加载状态，则启动全局加载
  useEffect(() => {
    if (initialState) {
      startLoading();
      return () => endLoading();
    }
  }, [initialState, startLoading, endLoading]);
  
  const startLocalLoading = useCallback((id?: string) => {
    setLocalLoading(true);
    startLoading(id);
    
    // 安全措施：确保loading状态不会永久保持
    // 清除之前的定时器，避免多次调用产生的重复定时器
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    
    timerRef.current = setTimeout(() => {
      setLocalLoading(false);
      endLoading(id);
      timerRef.current = null;
    }, MAX_LOADING_TIME);
  }, [startLoading, endLoading]);
  
  const endLocalLoading = useCallback((id?: string) => {
    setLocalLoading(false);
    endLoading(id);
    
    // 清除可能存在的定时器
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, [endLoading]);
  
  // 组件卸载时自动结束loading并清除定时器
  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
      if (localLoading) {
        endLoading();
      }
    };
  }, []); // 空依赖数组，确保清理函数只在组件卸载时执行一次
  
  return { isLoading: localLoading || isLoading, startLoading: startLocalLoading, endLoading: endLocalLoading };
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