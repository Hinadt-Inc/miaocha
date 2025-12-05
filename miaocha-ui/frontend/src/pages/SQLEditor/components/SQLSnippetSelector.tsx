import React from 'react';

import { Button } from 'antd';

export interface SQLSnippetSelectorProps {
  onSelect: (snippet: string) => void;
}

/**
 * SQL片段选择器组件
 * 提供常用SQL模板的快速插入功能
 */
export const SQLSnippetSelector: React.FC<SQLSnippetSelectorProps> = () => {
  const DORIS_URL = 'https://doris.apache.org/zh-CN/docs/sql-manual/basic-element/sql-data-types/data-type-overview';

  return (
    <Button size="small" onClick={() => window.open(DORIS_URL, '_blank')}>
      帮助文档
    </Button>
  );
};
