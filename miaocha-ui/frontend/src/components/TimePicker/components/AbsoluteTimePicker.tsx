import { useState } from 'react';
import { DatePicker, Button, Space } from 'antd';
import type { RangePickerProps } from 'antd/es/date-picker';
import { IAbsoluteTimePickerProps, ILogTimeSubmitParams } from '../types';

const { RangePicker } = DatePicker;

/**
 * 绝对时间选择组件
 */
const AbsoluteTimePicker: React.FC<IAbsoluteTimePickerProps> = ({ onSubmit }) => {
  const [absoluteOption, setAbsoluteOption] = useState<ILogTimeSubmitParams>();

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
      <RangePicker format="YYYY-MM-DD HH:mm:ss" showTime onChange={onRangeChange} />
      <Button disabled={!absoluteOption?.value} type="primary" onClick={handleSubmit}>
        确定
      </Button>
    </Space.Compact>
  );
};

export default AbsoluteTimePicker;
