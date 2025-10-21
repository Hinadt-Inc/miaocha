/**
 * SQL搜索输入组件
 */

import React from 'react';
import { AutoComplete, Space } from 'antd';
import { ILogColumnsResponse } from '../../types';
import { PLACEHOLDERS } from '../constants';

interface ISqlInputProps {
  value: string;
  onChange: (value: string) => void;
  columns?: ILogColumnsResponse[];
}

const SqlInput: React.FC<ISqlInputProps> = ({ value, onChange, columns = [] }) => {
  // 获取当前正在输入的词汇，用于字段匹配
  const getCurrentInputWord = (inputValue: string) => {
    if (!inputValue) return '';

    // 按空格分割，获取最后一个词汇
    const words = inputValue.split(/\s+/);
    const lastWord = words[words.length - 1] || '';

    // 如果最后一个词汇包含操作符，提取字段名部分
    const operatorMatch = /^([a-zA-Z_][a-zA-Z0-9_.]*)/.exec(lastWord);
    return operatorMatch ? operatorMatch[1] : lastWord;
  };

  // 根据当前输入的词汇筛选字段
  const currentWord = getCurrentInputWord(value);
  const filteredColumns = currentWord
    ? columns.filter(
        (column) => column.columnName?.toLowerCase().includes(currentWord.toLowerCase()),
      )
    : columns;

  const options = filteredColumns.map((item: ILogColumnsResponse) => ({
    value: (() => {
      // 智能替换/拼接逻辑
      if (!item.columnName) return value || '';

      // 如果当前输入为空，直接返回字段名
      if (!value) return item.columnName;

      // 如果当前有匹配的词汇，说明用户正在输入字段名，需要替换
      if (currentWord) {
        const words = value.split(/\s+/);
        words[words.length - 1] = item.columnName;
        return words.join(' ');
      }

      // 如果没有匹配词汇但输入以空格结尾，直接拼接
      if (value.endsWith(' ')) {
        return value + item.columnName;
      }

      // 否则在当前输入和字段名之间添加空格拼接
      return value + ' ' + item.columnName;
    })(),
    label: item.columnName,
  }));

  return (
    <Space.Compact style={{ width: '100%' }}>
      <AutoComplete
        allowClear
        options={options}
        placeholder={PLACEHOLDERS.SQL}
        style={{ width: '100%' }}
        value={value}
        onChange={onChange}
      />
    </Space.Compact>
  );
};

export default SqlInput;
