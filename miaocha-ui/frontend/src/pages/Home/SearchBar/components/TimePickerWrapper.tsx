/**
 * 时间选择器组件
 */

import React, { Suspense, lazy, useState, forwardRef, useImperativeHandle } from 'react';

import { Button, Popover } from 'antd';
import dayjs from 'dayjs';

import SpinIndicator from '@/components/SpinIndicator';
import { DATE_FORMAT_THOUSOND, QUICK_RANGES } from '@/components/TimePicker';

import { useHomeContext } from '../../context';
import { useDataInit } from '../../hooks/useDataInit';
import { ILogTimeSubmitParams } from '../types';

const TimePicker = lazy(() => import('@/components/TimePicker'));

/**
 * 暴露给父组件的方法
 */
export interface TimePickerWrapperRef {
  setVisible: (visible: boolean) => void;
}

const TimePickerWrapper = forwardRef<TimePickerWrapperRef>((_props, ref) => {
  const { updateSearchParams } = useHomeContext();
  const { fetchData } = useDataInit();

  const [visible, setVisible] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<string>('quick');
  const [timeOption, setTimeOption] = useState<ILogTimeSubmitParams>({
    value: 'last_15m',
    label: '最近15分钟',
    type: 'quick',
  });

  // 暴露方法给父组件
  useImperativeHandle(ref, () => ({
    setVisible,
  }));

  const handleSubmit = (timeOption: ILogTimeSubmitParams) => {
    const { range, type, value, startOption, endOption } = timeOption;

    const paramsWidthSearchParams = updateSearchParams({
      startTime: dayjs(range?.[0]).format(DATE_FORMAT_THOUSOND),
      endTime: dayjs(range?.[1]).format(DATE_FORMAT_THOUSOND),
      timeRange: value,
      timeType: type,
      ...(type === 'relative' &&
        startOption &&
        endOption && {
          relativeStartOption: timeOption.startOption,
          relativeEndOption: timeOption.endOption,
        }),
    });
    fetchData({ searchParams: paramsWidthSearchParams });
    setTimeOption(timeOption);
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
