import { useMemo } from 'react';
import { Tag } from 'antd';
import { IQuickTimePickerProps } from '../types';
import { QUICK_RANGES } from '../utils';

/**
 * 快速时间选择组件
 */
const QuickTimePicker: React.FC<IQuickTimePickerProps> = ({ selectedTag, onTagChange }) => {
  const quickRender = useMemo(() => {
    return (
      <>
        {Object.entries(QUICK_RANGES).map(([value, item]) => (
          <Tag.CheckableTag key={value} checked={selectedTag === value} onChange={() => onTagChange(value)}>
            {item.label}
          </Tag.CheckableTag>
        ))}
      </>
    );
  }, [selectedTag, onTagChange]);

  return quickRender;
};

export default QuickTimePicker;
