/**
 * 关键词搜索输入组件
 */

import React from 'react';
import { AutoComplete, Space } from 'antd';
import { PLACEHOLDERS } from '../constants';

interface IKeywordInputProps {
  value: string;
  onChange: (value: string) => void;
}

const KeywordInput: React.FC<IKeywordInputProps> = ({ value, onChange }) => {
  return (
    <Space.Compact style={{ width: '100%' }}>
      <AutoComplete
        allowClear
        placeholder={PLACEHOLDERS.KEYWORD}
        style={{ width: '100%' }}
        value={value}
        onChange={onChange}
        options={[]}
      />
    </Space.Compact>
  );
};

export default KeywordInput;
