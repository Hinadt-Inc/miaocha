/**
 * 时间选择器组件
 */

import React, { Suspense, lazy } from 'react';
import { Button, Popover } from 'antd';
import SpinIndicator from '@/components/SpinIndicator';
import { ITimeOption } from '../../types';
import { ILogTimeSubmitParams } from '../types';

const TimePicker = lazy(() => import('@/components/TimePicker'));

interface ITimePickerWrapperProps {
  timeOption: ITimeOption;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (params: ILogTimeSubmitParams) => void;
  activeTab: string;
  setActiveTab: (tab: string) => void;
}

const TimePickerWrapper: React.FC<ITimePickerWrapperProps> = ({
  timeOption,
  open,
  onOpenChange,
  onSubmit,
  activeTab,
  setActiveTab,
}) => {
  return (
    <Popover
      arrow={true}
      content={
        <Suspense fallback={<SpinIndicator />}>
          <TimePicker
            activeTab={activeTab}
            currentTimeOption={timeOption}
            setActiveTab={setActiveTab}
            onSubmit={onSubmit}
          />
        </Suspense>
      }
      open={open}
      placement="bottomRight"
      trigger="click"
      onOpenChange={onOpenChange}
    >
      <Button color="primary" size="small" variant="link">
        {timeOption.label}
      </Button>
    </Popover>
  );
};

export default TimePickerWrapper;
