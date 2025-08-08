import { useState, useEffect, useRef, useCallback } from 'react';
import { AutoRefreshState } from './types';
import { DEFAULT_CONFIG } from './constants';

/**
 * 自动刷新功能的自定义Hook
 * @param onRefresh 刷新回调函数
 * @param loading 是否正在加载
 * @returns 自动刷新相关的状态和方法
 */
export const useAutoRefresh = (onRefresh: () => void, loading?: boolean) => {
  // 状态管理
  const [state, setState] = useState<AutoRefreshState>({
    isAutoRefreshing: false,
    refreshInterval: 0,
    remainingTime: 0,
    lastRefreshTime: null,
    isPaused: false,
  });

  // 定时器引用
  const countdownRef = useRef<NodeJS.Timeout | null>(null);

  // 清理定时器
  const clearTimers = useCallback(() => {
    if (countdownRef.current) {
      clearInterval(countdownRef.current);
      countdownRef.current = null;
    }
  }, []);

  // 开始倒计时
  const startCountdown = useCallback(() => {
    if (state.refreshInterval <= 0 || loading || state.isPaused) return;
    
    
    
    // 如果剩余时间为0，重置为完整间隔
    if (state.remainingTime <= 0) {
      setState(prev => ({ ...prev, remainingTime: prev.refreshInterval }));
    }
    setState(prev => {
      // 如果不满足条件，直接返回原状态，不启动倒计时
      if (prev.refreshInterval <= 0 || loading || prev.isPaused) {
        return prev;
      }
      // 如果剩余时间为0，重置为完整间隔
      if (prev.remainingTime <= 0) {
        return { ...prev, remainingTime: prev.refreshInterval };
      }
      return prev;
    });
    // 清除之前的定时器
    clearTimers();
    
    countdownRef.current = setInterval(() => {
      setState(prev => {
        const newTime = prev.remainingTime - DEFAULT_CONFIG.COUNTDOWN_INTERVAL;
        if (newTime <= 0) {
          // 倒计时结束，执行刷新
          onRefresh();
          return {
            ...prev,
            remainingTime: prev.refreshInterval,
            lastRefreshTime: new Date(),
          };
        }
        return { ...prev, remainingTime: newTime };
      });
    }, DEFAULT_CONFIG.COUNTDOWN_INTERVAL);
  }, [state.refreshInterval, state.remainingTime, state.isPaused, loading, onRefresh, clearTimers]);

  // 开始自动刷新
  const startAutoRefresh = useCallback(() => {
    if (state.refreshInterval <= 0) return;
    
    setState(prev => ({
      ...prev,
      isAutoRefreshing: true,
      isPaused: false,
      lastRefreshTime: new Date(),
      remainingTime: prev.refreshInterval,
    }));
    
    // 如果不在loading状态，立即开始倒计时
    if (!loading) {
      setTimeout(startCountdown, 0);
    }
  }, [state.refreshInterval, loading, startCountdown]);

  // 停止自动刷新
  const stopAutoRefresh = useCallback(() => {
    setState(prev => ({
      ...prev,
      isAutoRefreshing: false,
      isPaused: false,
      remainingTime: 0,
    }));
    clearTimers();
  }, [clearTimers]);

  // 切换自动刷新状态
  const toggleAutoRefresh = useCallback(() => {
    if (state.isAutoRefreshing) {
      stopAutoRefresh();
    } else {
      startAutoRefresh();
    }
  }, [state.isAutoRefreshing, startAutoRefresh, stopAutoRefresh]);

  // 设置刷新间隔
  const setRefreshInterval = useCallback((value: number) => {
    setState(prev => ({ ...prev, refreshInterval: value }));
    
    if (value === 0) {
      // 如果选择关闭，停止自动刷新
      stopAutoRefresh();
    } else if (state.isAutoRefreshing) {
      // 如果正在自动刷新，重新开始
      clearTimers();
      setState(prev => ({ ...prev, remainingTime: value }));
      if (!loading) {
        setTimeout(startCountdown, 0);
      }
    }
  }, [state.isAutoRefreshing, loading, stopAutoRefresh, clearTimers, startCountdown]);

  // 手动刷新
  const handleManualRefresh = useCallback(() => {
    onRefresh();
    setState(prev => ({ ...prev, lastRefreshTime: new Date() }));
    
    // 如果正在自动刷新，重新开始倒计时
    if (state.isAutoRefreshing && state.refreshInterval > 0 && !loading) {
      clearTimers();
      setState(prev => ({ ...prev, remainingTime: prev.refreshInterval }));
      setTimeout(startCountdown, 0);
    }
  }, [onRefresh, state.isAutoRefreshing, state.refreshInterval, loading, clearTimers, startCountdown]);

  // 处理loading状态变化
  useEffect(() => {
    if (!state.isAutoRefreshing || state.refreshInterval <= 0) return;
    
    if (loading) {
      // 开始loading时暂停倒计时
      setState(prev => ({ ...prev, isPaused: true }));
      clearTimers();
    } else {
      // loading结束，恢复倒计时
      setState(prev => ({ ...prev, isPaused: false }));
      // 延迟一点时间再开始，确保状态稳定
      setTimeout(() => {
        if (state.isAutoRefreshing && state.refreshInterval > 0 && !loading) {
          startCountdown();
        }
      }, DEFAULT_CONFIG.RESTART_DELAY);
    }
  }, [loading, state.isAutoRefreshing, state.refreshInterval, clearTimers, startCountdown]);

  // 确保自动刷新的持续性
  useEffect(() => {
    if (state.isAutoRefreshing && state.refreshInterval > 0 && !loading && !state.isPaused && !countdownRef.current) {
      // 如果应该运行但没有活跃的定时器，重新启动
      startCountdown();
    }
  }, [state.isAutoRefreshing, state.refreshInterval, loading, state.isPaused, startCountdown]);

  // 组件卸载时清理定时器
  useEffect(() => {
    return () => {
      clearTimers();
    };
  }, [clearTimers]);

  return {
    ...state,
    toggleAutoRefresh,
    setRefreshInterval,
    handleManualRefresh,
  };
};
