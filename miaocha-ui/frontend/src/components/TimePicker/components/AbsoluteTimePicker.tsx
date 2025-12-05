import { useState, useEffect } from 'react';

import { DatePicker, Button, Space } from 'antd';
import type { RangePickerProps } from 'antd/es/date-picker';
import dayjs from 'dayjs';

import { IAbsoluteTimePickerProps, ILogTimeSubmitParams } from '../types';

const { RangePicker } = DatePicker;

/**
 * 绝对时间选择组件
 */
const AbsoluteTimePicker: React.FC<IAbsoluteTimePickerProps> = ({ onSubmit, currentTimeOption }) => {
  // 初始化时，如果有回显数据且类型为 absolute，则使用回显数据
  const [absoluteOption, setAbsoluteOption] = useState<ILogTimeSubmitParams>(() => {
    if (currentTimeOption?.type === 'absolute' && currentTimeOption.range) {
      return currentTimeOption;
    }
    return {};
  });

  // 当 currentTimeOption 变化时，同步更新状态
  useEffect(() => {
    if (currentTimeOption?.type === 'absolute' && currentTimeOption.range) {
      setAbsoluteOption(currentTimeOption);
    }
  }, [currentTimeOption]);

  const onRangeChange: RangePickerProps['onChange'] = (dates, dateStrings) => {
    if (dates) {
      setAbsoluteOption({
        label: dateStrings.join(' ~ '),
        value: dateStrings.join(' ~ '),
        range: dateStrings,
        type: 'absolute',
      });
    } else {
      setAbsoluteOption({});
    }
  };

  const handleSubmit = () => {
    if (absoluteOption?.value) {
      onSubmit(absoluteOption);
    }
  };

  return (
    <Space.Compact block>
      <RangePicker
        defaultValue={
          absoluteOption?.range && absoluteOption.range.length === 2
            ? [dayjs(absoluteOption.range[0]), dayjs(absoluteOption.range[1])]
            : undefined
        }
        format="YYYY-MM-DD HH:mm:ss"
        showTime
        value={
          absoluteOption?.range && absoluteOption.range.length === 2
            ? [dayjs(absoluteOption.range[0]), dayjs(absoluteOption.range[1])]
            : null
        }
        onChange={onRangeChange}
      />
      <Button disabled={!absoluteOption?.value} type="primary" onClick={handleSubmit}>
        确定
      </Button>
    </Space.Compact>
  );
};

export default AbsoluteTimePicker;
