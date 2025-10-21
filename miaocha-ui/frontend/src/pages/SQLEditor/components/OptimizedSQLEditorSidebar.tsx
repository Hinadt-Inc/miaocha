import React from 'react';
import OptimizedSchemaTree from './OptimizedSchemaTree';
import { ExtendedSchemaResult } from '../types';
import styles from '../SQLEditorPage.module.less';

export interface OptimizedSQLEditorSidebarProps {
  databaseSchema: ExtendedSchemaResult | { error: string } | null;
  loadingSchema: boolean;
  loadingTables: Set<string>;
  refreshSchema: () => void;
  fetchTableSchema: (tableName: string) => Promise<any>;
  selectedSource?: string;
  onInsertTable: (
    tableName: string,
    columns?: ExtendedSchemaResult['tables'][0]['columns'],
  ) => void;
  onInsertField: (fieldName: string) => void;
  collapsed: boolean;
  onToggle: () => void;
}

/**
 * 优化的SQL编辑器侧边栏组件
 * 包含优化的数据库结构树和折叠功能
 */
export const OptimizedSQLEditorSidebar: React.FC<OptimizedSQLEditorSidebarProps> = ({
  databaseSchema,
  loadingSchema,
  loadingTables,
  refreshSchema,
  fetchTableSchema,
  selectedSource,
  onInsertTable,
  onInsertField,
  collapsed,
  onToggle,
}) => {
  return (
    <div className={styles.sqlEditorSidebar}>
      <div className={styles.sidebarContent}>
        <OptimizedSchemaTree
          collapsed={collapsed}
          databaseSchema={databaseSchema}
          fetchTableSchema={fetchTableSchema}
          handleInsertField={onInsertField}
          handleInsertTable={onInsertTable}
          loadingSchema={loadingSchema}
          loadingTables={loadingTables}
          refreshSchema={refreshSchema}
          selectedSource={selectedSource}
          toggleSider={onToggle}
        />
      </div>
    </div>
  );
};
