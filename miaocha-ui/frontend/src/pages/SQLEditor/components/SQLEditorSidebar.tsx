import React from 'react';
import VirtualizedSchemaTree from './VirtualizedSchemaTree';
import { SchemaResult } from '../types';
import styles from '../SQLEditorPage.module.less';

export interface SQLEditorSidebarProps {
  databaseSchema: SchemaResult | { error: string } | null;
  loadingSchema: boolean;
  refreshSchema: () => void;
  onInsertTable: (
    tableName: string,
    columns: {
      columnName: string;
      dataType: string;
      columnComment: string;
      isPrimaryKey: boolean;
      isNullable: boolean;
    }[],
  ) => void;
  onInsertField: (fieldName: string) => void;
  collapsed: boolean;
  onToggle: () => void;
}

/**
 * SQL编辑器侧边栏组件
 * 包含数据库结构树和折叠功能
 */
export const SQLEditorSidebar: React.FC<SQLEditorSidebarProps> = ({
  databaseSchema,
  loadingSchema,
  refreshSchema,
  onInsertTable,
  onInsertField,
  collapsed,
  onToggle,
}) => {
  return (
    <div className={styles.sqlEditorSidebar}>
      <div className={styles.sidebarContent}>
        <VirtualizedSchemaTree
          databaseSchema={databaseSchema}
          loadingSchema={loadingSchema}
          refreshSchema={refreshSchema}
          handleInsertTable={onInsertTable}
          handleInsertField={onInsertField}
          collapsed={collapsed}
          toggleSider={onToggle}
        />
      </div>
    </div>
  );
};
