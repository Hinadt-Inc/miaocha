/**
 * 搜索条件过滤标签组件
 */

import React from 'react';

import { Space, Tag, Tooltip } from 'antd';

import { ITimeOption } from '../../types';
import { TAG_COLORS } from '../constants';

interface IFilterTagsProps {
  keywords: string[];
  sqls: string[];
  timeOption: ITimeOption;
  onClickKeyword: (keyword: string) => void;
  onCloseKeyword: (keyword: string) => void;
  onClickSql: (sql: string) => void;
  onCloseSql: (sql: string) => void;
  onClickTime: () => void;
}

const FilterTags: React.FC<IFilterTagsProps> = ({
  keywords,
  sqls,
  timeOption,
  onClickKeyword,
  onCloseKeyword,
  onClickSql,
  onCloseSql,
  onClickTime,
}) => {
  const { range = [] } = timeOption;

  return (
    <Space wrap>
      {/* 关键词标签 */}
      {keywords.map((item: string) => (
        <Tooltip key={item} placement="topLeft" title={item}>
          <Tag
            closable
            color={TAG_COLORS.KEYWORD}
            onClick={() => onClickKeyword(item)}
            onClose={() => onCloseKeyword(item)}
          >
            <span className="tagContent">{item}</span>
          </Tag>
        </Tooltip>
      ))}

      {/* SQL条件标签 */}
      {[...new Set(sqls)].map((item: string) => (
        <Tooltip key={item} placement="topLeft" title={item}>
          <Tag closable color={TAG_COLORS.SQL} onClick={() => onClickSql(item)} onClose={() => onCloseSql(item)}>
            <span className="tagContent">{item}</span>
          </Tag>
        </Tooltip>
      ))}

      {/* 时间范围标签 */}
      {range.length === 2 && (
        <Tag color={TAG_COLORS.TIME} onClick={onClickTime}>
          {range[0]} ~ {range[1]}
        </Tag>
      )}
    </Space>
  );
};

export default FilterTags;
