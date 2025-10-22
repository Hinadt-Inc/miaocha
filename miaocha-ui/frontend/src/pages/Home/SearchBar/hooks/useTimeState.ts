import { useState, useCallback } from 'react';

import { ITimeOption, ILogSearchParams } from '@/pages/Home/types';
import { QUICK_RANGES } from '@/pages/Home/utils';

import { ITimeState, ILogTimeSubmitParams } from '../types';

export const useTimeState = (initialSearchParams: ILogSearchParams) => {
  // 初始化时间选项
  const initializeTimeOption = useCallback((): ITimeOption => {
    const { timeRange } = initialSearchParams;
    const isQuick = QUICK_RANGES[timeRange];

    if (!isQuick) {
      const defaultRange = QUICK_RANGES['last_15m'];
      return {
        value: 'last_15m',
        range: [defaultRange.from().format(defaultRange.format[0]), defaultRange.to().format(defaultRange.format[1])],
        ...defaultRange,
        type: 'quick',
      } as any;
    }

    const { from, to, format } = isQuick;
    return {
      value: timeRange,
      range: [from().format(format[0]), to().format(format[1])],
      ...QUICK_RANGES[timeRange],
      type: 'quick',
    } as any;
  }, [initialSearchParams]);

  const [timeState, setTimeState] = useState<ITimeState>({
    timeOption: initializeTimeOption(),
    timeGroup: 'auto',
    openTimeRange: false,
    openTimeGroup: false,
    activeTab: 'quick',
  });

  // 更新时间选项
  const setTimeOption = useCallback((option: ITimeOption | ((prev: ITimeOption) => ITimeOption)) => {
    setTimeState((prev) => ({
      ...prev,
      timeOption: typeof option === 'function' ? option(prev.timeOption) : option,
    }));
  }, []);

  // 更新时间分组
  const setTimeGroup = useCallback((group: string) => {
    setTimeState((prev) => ({
      ...prev,
      timeGroup: group,
    }));
  }, []);

  // 控制时间范围弹窗显隐
  const setOpenTimeRange = useCallback((open: boolean) => {
    setTimeState((prev) => ({
      ...prev,
      openTimeRange: open,
    }));
  }, []);

  // 控制时间分组弹窗显隐
  const setOpenTimeGroup = useCallback((open: boolean) => {
    setTimeState((prev) => ({
      ...prev,
      openTimeGroup: open,
    }));
  }, []);

  // 设置活动标签
  const setActiveTab = useCallback((tab: string) => {
    setTimeState((prev) => ({
      ...prev,
      activeTab: tab,
    }));
  }, []);

  // 提交时间范围
  const submitTime = useCallback(
    (params: ILogTimeSubmitParams) => {
      const timeOption: ITimeOption = {
        value: params.value || '',
        range: [params.range?.[0] || '', params.range?.[1] || ''],
        label: params.label || '',
        type: params.type || 'absolute',
        startOption: params.startOption,
        endOption: params.endOption,
      };
      setTimeOption(timeOption);
      setOpenTimeRange(false);
    },
    [setTimeOption, setOpenTimeRange],
  );

  return {
    timeState,
    setTimeOption,
    setTimeGroup,
    setOpenTimeRange,
    setOpenTimeGroup,
    setActiveTab,
    submitTime,
  };
};
