/**
 * 时间选择器组件
 */

import { Suspense, lazy, useState, forwardRef, useImperativeHandle, useMemo } from 'react';

import { Button, Popover } from 'antd';

import SpinIndicator from '@/components/SpinIndicator';

import { useHomeContext } from '../../context';
import { useDataInit } from '../../hooks/useDataInit';
import { parseTimeRange } from '../../utils';
import { ILogTimeSubmitParams } from '../types';

const TimePicker = lazy(() => import('@/components/TimePicker'));

/**
 * 暴露给父组件的方法
 */
export interface TimePickerWrapperRef {
  setVisible: (visible: boolean) => void;
}

const TimePickerWrapper = forwardRef<TimePickerWrapperRef>((_props, ref) => {
  const { searchParams, updateSearchParams } = useHomeContext();
  const { fetchData, refreshFieldDistributions } = useDataInit();

  const [visible, setVisible] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<string>('quick');
  const timeOption: ILogTimeSubmitParams = useMemo(() => {
    const { timeRange } = searchParams;
    const result = parseTimeRange(timeRange as string);
    const { type } = result || {};
    if (type) {
      setActiveTab(type as string);
    }
    return result;
  }, [searchParams.timeRange]); // 只依赖真正使用的 timeRange
  // 暴露方法给父组件
  useImperativeHandle(ref, () => ({
    setVisible,
  }));

  const handleSubmit = (timeOption: ILogTimeSubmitParams) => {
    const { range, value } = timeOption;

    const paramsWidthSearchParams = updateSearchParams({
      startTime: range?.[0],
      endTime: range?.[1],
      timeRange: value as any,
    });
    fetchData({ searchParams: paramsWidthSearchParams });
    refreshFieldDistributions(paramsWidthSearchParams);
    setVisible(false);
  };

  return (
    <Popover
      arrow={true}
      content={
        <Suspense fallback={<SpinIndicator />}>
          <TimePicker
            activeTab={activeTab}
            currentTimeOption={timeOption}
            setActiveTab={setActiveTab}
            onSubmit={handleSubmit}
          />
        </Suspense>
      }
      open={visible}
      placement="bottomRight"
      trigger="click"
      onOpenChange={setVisible}
    >
      <Button color="primary" size="small" variant="link">
        {timeOption.label}
      </Button>
    </Popover>
  );
});

export default TimePickerWrapper;
