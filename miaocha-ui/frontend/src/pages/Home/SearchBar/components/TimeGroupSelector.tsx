/**
 * 时间分组选择器组件
 */

import React, { useState } from 'react';

import { Button, Popover, Tag } from 'antd';

import { useHomeContext } from '../../context';
import { useDataInit } from '../../hooks/useDataInit';
import { TIME_GROUP } from '../../utils';

const TimeGroupSelector: React.FC = () => {
  const [visible, setVisible] = useState(false);
  const { searchParams, updateSearchParams } = useHomeContext();
  const { fetchData } = useDataInit();

  const onChange = (value: TimeGrouping) => {
    const paramsWidthSearchParams = updateSearchParams({ timeGrouping: value });
    setVisible(false);
    fetchData({ searchParams: paramsWidthSearchParams });
  };

  return (
    <Popover
      arrow={true}
      content={
        <>
          {Object.entries(TIME_GROUP).map(([value, item]) => (
            <Tag.CheckableTag
              key={value}
              checked={searchParams?.timeGrouping === value}
              onChange={() => onChange(value as TimeGrouping)}
            >
              {item}
            </Tag.CheckableTag>
          ))}
        </>
      }
      open={visible}
      placement="bottomRight"
      trigger="click"
      onOpenChange={setVisible}
    >
      <Button color="primary" size="small" variant="link">
        按{TIME_GROUP[searchParams?.timeGrouping ?? 'day']}分组
      </Button>
    </Popover>
  );
};

export default TimeGroupSelector;
