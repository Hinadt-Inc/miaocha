/**
 * 时间分组选择器组件
 */

import React from 'react';

import { Button, Popover, Tag } from 'antd';

import { TIME_GROUP } from '../../utils';

interface ITimeGroupSelectorProps {
  timeGrouping: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onChange: (value: string) => void;
}

const TimeGroupSelector: React.FC<ITimeGroupSelectorProps> = ({ timeGrouping, open, onOpenChange, onChange }) => {
  return (
    <Popover
      arrow={true}
      content={
        <>
          {Object.entries(TIME_GROUP).map(([value, item]) => (
            <Tag.CheckableTag key={value} checked={timeGrouping === value} onChange={() => onChange(value)}>
              {item}
            </Tag.CheckableTag>
          ))}
        </>
      }
      open={open}
      placement="bottomRight"
      trigger="click"
      onOpenChange={onOpenChange}
    >
      <Button color="primary" size="small" variant="link">
        按{TIME_GROUP[timeGrouping]}分组
      </Button>
    </Popover>
  );
};

export default TimeGroupSelector;
