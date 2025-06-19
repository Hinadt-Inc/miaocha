import React from 'react';
import { Button, Dropdown } from 'antd';

interface SqlSnippetItem {
  label: string;
  insertText: string;
  description: string;
}

interface SnippetSelectorProps {
  onSelect: (snippet: string) => void;
  snippets: SqlSnippetItem[];
}

/**
 * SQL代码片段选择器组件
 * 允许用户从下拉菜单中选择常用SQL代码片段
 */
const SnippetSelector: React.FC<SnippetSelectorProps> = ({ onSelect, snippets }) => (
  <Dropdown
    menu={{
      items: snippets.map((snippet) => ({
        key: snippet.label,
        label: snippet.label,
        onClick: () => onSelect(snippet.insertText),
      })),
    }}
    placement="bottomLeft"
  >
    <Button size="small" style={{ marginLeft: 8 }}>
      插入SQL模板
    </Button>
  </Dropdown>
);

export default SnippetSelector;
