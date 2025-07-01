import React from 'react';
import SchemaTree from './SchemaTree';
import { SchemaResult } from '../types';

export interface SQLEditorSidebarProps {
  databaseSchema: SchemaResult | { error: string } | null;
  loadingSchema: boolean;
  refreshSchema: () => void;
  onTreeNodeDoubleClick: (tableName: string) => void;
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
  fullscreen: boolean;
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
  onTreeNodeDoubleClick,
  onInsertTable,
  onInsertField,
  fullscreen,
  collapsed,
  onToggle,
}) => {
  return (
    <div className="sql-editor-sidebar">
      <div className="sidebar-content">
        <SchemaTree
          databaseSchema={databaseSchema}
          loadingSchema={loadingSchema}
          refreshSchema={refreshSchema}
          handleTreeNodeDoubleClick={onTreeNodeDoubleClick}
          handleInsertTable={onInsertTable}
          handleInsertField={onInsertField}
          fullscreen={fullscreen}
          collapsed={collapsed}
          toggleSider={onToggle}
        />
      </div>
    </div>
  );
};
