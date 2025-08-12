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
      trigger="click"
      open={open}
      onOpenChange={onOpenChange}
      placement="bottomRight"
      content={
        <Suspense fallback={<SpinIndicator />}>
          <TimePicker
            activeTab={activeTab}
            setActiveTab={setActiveTab}
            onSubmit={onSubmit}
            currentTimeOption={timeOption}
          />
        </Suspense>
      }
    >
      <Button color="primary" variant="link" size="small">
        {timeOption.label}
      </Button>
    </Popover>
  );
};

export default TimePickerWrapper;
