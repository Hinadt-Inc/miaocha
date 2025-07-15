import React, { createContext, useContext, useState, ReactNode, useCallback, useEffect } from 'react';

interface LoadingContextProps {
  isLoading: boolean;
  startLoading: (id?: string) => void;
  endLoading: (id?: string) => void;
}

// 创建加载状态上下文
const LoadingContext = createContext<LoadingContextProps>({
  isLoading: false,
  startLoading: () => {},
  endLoading: () => {},
});

// 使用Set保存当前活动的加载ID
const activeLoadingIds = new Set<string>();

interface LoadingProviderProps {
  children: ReactNode;
}

export const LoadingProvider: React.FC<LoadingProviderProps> = ({ children }) => {
  const [isLoading, setIsLoading] = useState(false);
  
  // 开始加载状态
  const startLoading = useCallback((id: string = 'global') => {
    activeLoadingIds.add(id);
    setIsLoading(true);
    // 添加标记到body元素，用于CSS选择器
    document.body.classList.add('loading-active');
  }, []);
  
  // 结束加载状态
  const endLoading = useCallback((id: string = 'global') => {
    activeLoadingIds.delete(id);
    
    // 只有当所有加载都完成时，才设置为非加载状态
    if (activeLoadingIds.size === 0) {
      setIsLoading(false);
      // 移除body元素上的标记
      document.body.classList.remove('loading-active');
    }
  }, []);
  
  // 确保组件卸载时清理加载状态
  useEffect(() => {
    return () => {
      if (activeLoadingIds.size > 0) {
        activeLoadingIds.clear();
        document.body.classList.remove('loading-active');
      }
    };
  }, []);
  
  return (
    <LoadingContext.Provider value={{ isLoading, startLoading, endLoading }}>
      {children}
    </LoadingContext.Provider>
  );
};

// 自定义Hook，用于在组件中方便地访问加载状态
export const useLoading = () => {
  const context = useContext(LoadingContext);
  
  if (context === undefined) {
    throw new Error('useLoading必须在LoadingProvider内部使用');
  }
  
  return context;
};