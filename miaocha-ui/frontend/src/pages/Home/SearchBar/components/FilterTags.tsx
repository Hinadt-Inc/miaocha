/**
 * 搜索条件过滤标签组件
 */

import React from 'react';

import { Space, Tag, Tooltip } from 'antd';

import { useHomeContext } from '../../context';
import { TAG_COLORS } from '../constants';

interface IFilterTagsProps {
  handleClickTag: (type: string, value: string) => void;
}

const FilterTags: React.FC<IFilterTagsProps> = ({ handleClickTag }) => {
  const { searchParams } = useHomeContext();
  const { startTime, endTime, keywords, whereSqls } = searchParams;

  return (
    <Space wrap>
      {/* 关键词标签 */}
      {(keywords || []).map((item: string) => (
        <Tooltip key={item} placement="topLeft" title={item}>
          <Tag
            closable
            color={TAG_COLORS.KEYWORD}
            onClick={() => handleClickTag('keywords', item)}
            onClose={() => handleClickTag('closeKeywords', item)}
          >
            <span className="tagContent">{item}</span>
          </Tag>
        </Tooltip>
      ))}

      {/* SQL条件标签 */}
      {[...new Set(whereSqls)].map((item: string) => (
        <Tooltip key={item} placement="topLeft" title={item}>
          <Tag
            closable
            color={TAG_COLORS.SQL}
            onClick={() => handleClickTag('sql', item)}
            onClose={() => handleClickTag('closeSql', item)}
          >
            <span className="tagContent">{item}</span>
          </Tag>
        </Tooltip>
      ))}

      {/* 时间范围标签 */}
      {startTime && endTime && (
        <Tag color={TAG_COLORS.TIME} onClick={() => handleClickTag('time', '')}>
          {startTime} ~ {endTime}
        </Tag>
      )}
    </Space>
  );
};

export default FilterTags;
