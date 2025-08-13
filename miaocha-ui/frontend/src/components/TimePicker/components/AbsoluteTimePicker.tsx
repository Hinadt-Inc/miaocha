import { useState, useMemo } from 'react';
import { DatePicker, Button, Space } from 'antd';
import { IAbsoluteTimePickerProps, ILogTimeSubmitParams } from '../types';

const { RangePicker } = DatePicker;

/**
 * 绝对时间选择组件
 */
const AbsoluteTimePicker: React.FC<IAbsoluteTimePickerProps> = ({ onSubmit }) => {
  const [absoluteOption, setAbsoluteOption] = useState<ILogTimeSubmitParams>();

  const onRangeChange = (dates: any, dateStrings: string[]) => {
    if (dates) {
      setAbsoluteOption({
        label: dateStrings.join(' ~ '),
        value: dateStrings.join(' ~ '),
        range: dateStrings,
        type: 'absolute',
      });
    } else {
      setAbsoluteOption({} as any);
    }
  };

  const handleSubmit = () => {
    if (absoluteOption?.value) {
      onSubmit(absoluteOption);
    }
  };

  return useMemo(
    () => (
      <Space.Compact block>
        <RangePicker showTime format="YYYY-MM-DD HH:mm:ss" onChange={onRangeChange} />
        <Button type="primary" onClick={handleSubmit} disabled={!absoluteOption?.value}>
          确定
        </Button>
      </Space.Compact>
    ),
    [absoluteOption],
  );
};

export default AbsoluteTimePicker;
