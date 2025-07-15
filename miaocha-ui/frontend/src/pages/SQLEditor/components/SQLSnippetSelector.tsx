import React from 'react';
import { Button, Dropdown } from 'antd';
import type { MenuProps } from 'antd';
import { useSQLSnippets } from '../hooks/useSQLSnippets';
import styles from '../SQLEditorPage.module.less';

export interface SQLSnippetSelectorProps {
  onSelect: (snippet: string) => void;
}

/**
 * SQL片段选择器组件
 * 提供常用SQL模板的快速插入功能
 */
export const SQLSnippetSelector: React.FC<SQLSnippetSelectorProps> = ({ onSelect }) => {
  const { sqlSnippets } = useSQLSnippets();

  const DORIS_URL = 'https://doris.apache.org/zh-CN/docs/sql-manual/basic-element/sql-data-types/data-type-overview';

  const menuItems: MenuProps['items'] = sqlSnippets.map((snippet) => ({
    key: snippet.label,
    label: (
      <div>
        <div>{snippet.label}</div>
        <div className={styles.snippetDescription}>{snippet.description}</div>
      </div>
    ),
    onClick: () => onSelect(snippet.insertText),
  }));

  return (
    <Button size="small" onClick={() => window.open(DORIS_URL, '_blank')}>
      帮助文档
    </Button>
    // <Dropdown menu={{ items: menuItems }} placement="bottomLeft" trigger={['click']}>
    //   <Button size="small">插入SQL模板</Button>
    // </Dropdown>
  );
};
